/**
 *
 */
package edu.cmu.cs.lti.collection_reader;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.javatuples.Pair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Zhengzhong Liu, Hector
 *         <p/>
 *         This analysis engine work with AceDataCollectionReader and annotate the golden standard
 *         from the .apf file to JCas
 */
public class AceDataGoldenAnnotator extends AbstractLoggingAnnotator {
    private final String ANNOTATOR_COMPONENT_ID = "GoldStandard_ACE2005";

    public static final String PARAM_GOLD_STANDARD_VIEW_NAME = "goldStandard";

    @ConfigurationParameter(name = PARAM_GOLD_STANDARD_VIEW_NAME)
    String goldStandardViewName;

    Pattern ampPattern;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        ampPattern = Pattern.compile(Pattern.quote("&amp;"));
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

        try {
            JCas goldStandardView = aJCas.getView(goldStandardViewName);

            String documentText = goldStandardView.getDocumentText();

            SourceDocumentInformation info = JCasUtil
                    .selectSingle(aJCas, SourceDocumentInformation.class);

            SAXBuilder builder = new SAXBuilder();
            builder.setDTDHandler(null);

            URI apfUri = new URI(info.getUri());

            Document doc = builder.build(getAPFFile(new File(apfUri.getPath())));

            Element apfSource = doc.getRootElement();
            Element apfDocument = apfSource.getChild("document");
            List<Element> apfEntities = apfDocument.getChildren("entity");
            List<Element> apfEvents = apfDocument.getChildren("event");

            Pair<Map<String, Entity>, Map<String, EntityMention>> aceIds = annotateEntities(apfEntities,
                    goldStandardView, documentText);

            Map<String, Entity> id2Entities = aceIds.getValue0();
            Map<String, EntityMention> id2EntityMentions = aceIds.getValue1();

            JCasUtil.select(goldStandardView, EntityMention.class);

            annotateEvents(apfEvents, goldStandardView, documentText, id2Entities, id2EntityMentions);
        } catch (JDOMException | IOException | URISyntaxException | CASException e) {
            e.printStackTrace();
        }
    }

    private <T extends ComponentTOP> void setComponentId(T anno) {
        anno.setComponentId(ANNOTATOR_COMPONENT_ID);
    }

    private <T extends ComponentAnnotation> void setComponentId(T anno) {
        anno.setComponentId(ANNOTATOR_COMPONENT_ID);
    }

    private void annotateEvents(List<Element> apfEvents, JCas aJCas, String documentText,
                                Map<String, Entity> id2Entities, Map<String, EntityMention> id2EntityMentions) {
        for (Element apfEvent : apfEvents) {
            Event event = new Event(aJCas);
            event.setEventType(apfEvent.getAttributeValue("TYPE"));
            event.setEventSubtype(apfEvent.getAttributeValue("SUBTYPE"));
            event.setId(apfEvent.getAttributeValue("ID"));
            event.setModality(apfEvent.getAttributeValue("MODALITY"));
            event.setPolarity(apfEvent.getAttributeValue("POLARITY"));
            event.setGenericity(apfEvent.getAttributeValue("GENERICITY"));
            event.setTense(apfEvent.getAttributeValue("TENSE"));
            setComponentId(event);
            event.addToIndexes();

            // annotate arguments
            List<Element> eventArguments = apfEvent.getChildren("event_argument");
            List<EventArgumentLink> argumentLinks = new ArrayList<>();

            for (Element argument : eventArguments) {
                String argumentId = argument.getAttributeValue("REFID");

                EventArgumentLink argumentLink = new EventArgumentLink(aJCas);
                argumentLink.setEvent(event);
                argumentLink.setEntity(id2Entities.get(argumentId));
                argumentLink.setArgumentRole(argument.getAttributeValue("ROLE"));
                setComponentId(argumentLink);
                argumentLink.addToIndexes();
                argumentLinks.add(argumentLink);
            }

            event.setArguments(FSCollectionFactory.createFSList(aJCas, argumentLinks));

            // annotate event mentions
            List<EventMention> mentions = new ArrayList<EventMention>();
            List<Element> eventMentions = apfEvent.getChildren("event_mention");
            for (Element eventMention : eventMentions) {
                Element mentionExtentNode = eventMention.getChild("extent").getChild("charseq");
                int mentionExtentStart = Integer.parseInt(mentionExtentNode.getAttributeValue("START"));
                int mentionExtentEnd = Integer.parseInt(mentionExtentNode.getAttributeValue("END"));

                EventMentionContext evmExtent = new EventMentionContext(aJCas);
                evmExtent.setBegin(mentionExtentStart);
                evmExtent.setEnd(mentionExtentEnd + 1);
                setComponentId(evmExtent);
                evmExtent.addToIndexes();

                // note that we annotate event mention with the anchor, then attach things to it
                Element mentionAnchor = eventMention.getChild("anchor").getChild("charseq");
                int anchorStart = Integer.parseInt(mentionAnchor.getAttributeValue("START"));
                int anchorEnd = Integer.parseInt(mentionAnchor.getAttributeValue("END"));

                EventMention mention = new EventMention(aJCas, anchorStart, anchorEnd + 1);
                mention.setMentionContext(evmExtent);
                mention.setId(eventMention.getAttributeValue("ID"));

                List<EventMentionArgumentLink> evmArgumentLinks = new ArrayList<EventMentionArgumentLink>();
                for (Element evmArugment : eventMention.getChildren("event_mention_argument")) {
                    String argumentId = evmArugment.getAttributeValue("REFID");
                    EventMentionArgumentLink argumentLink = new EventMentionArgumentLink(aJCas);
                    argumentLink.setEventMention(mention);
                    EntityMention argumentEm = id2EntityMentions.get(argumentId);
                    argumentLink.setArgument(argumentEm);
                    argumentLink.setArgumentRole(evmArugment.getAttributeValue("ROLE"));
                    setComponentId(argumentLink);
                    argumentLink.addToIndexes();
                    evmArgumentLinks.add(argumentLink);
                }

                mention.setArguments(FSCollectionFactory.createFSList(aJCas, evmArgumentLinks));
                setComponentId(mention);
                mention.addToIndexes();
                mentions.add(mention);
            }

            for (EventMention mention : mentions) {
                mention.setReferringEvent(event);
                //annotate mention with the full type
                mention.setEventType(event.getEventType() + "_" + event.getEventSubtype());
            }

            event.setEventMentions(UimaConvenience.makeFsArray(mentions, aJCas));
        }
    }

    private Pair<Map<String, Entity>, Map<String, EntityMention>> annotateEntities(
            List<Element> apfEntities, JCas aJCas, String documentText) {
        Map<String, Entity> id2Entity = new HashMap<String, Entity>();
        Map<String, EntityMention> id2EntityMention = new HashMap<String, EntityMention>();

        for (Element apfEntity : apfEntities) {
            Entity namedEntity = new Entity(aJCas);
            namedEntity.setEntityType(apfEntity.getAttributeValue("TYPE"));
            namedEntity.setEntitySubtype(apfEntity.getAttributeValue("SUBTYPE"));
            namedEntity.setEntityClass(apfEntity.getAttributeValue("CLASS"));
            String entityId = apfEntity.getAttributeValue("ID");
            namedEntity.setAnnotationId(entityId);
            setComponentId(namedEntity);
            namedEntity.addToIndexes();

            // annotate mentions
            List<EntityMention> mentions = new ArrayList<EntityMention>();
            List<Element> entityMentions = apfEntity.getChildren("entity_mention");
            for (int j = 0; j < entityMentions.size(); j++) {
                Element entityMention = entityMentions.get(j);
                int start = Integer.parseInt(entityMention.getChild("extent").getChild("charseq")
                        .getAttributeValue("START"));
                int end = Integer.parseInt(entityMention.getChild("extent").getChild("charseq")
                        .getAttributeValue("END"));
                String givenText = entityMention.getChild("extent").getChild("charseq").getText();
                String parsedText = documentText.substring(start, end + 1);
                Matcher ampMatcher = ampPattern.matcher(parsedText);
                parsedText = ampMatcher.replaceAll("&");

                EntityMention mention = new EntityMention(aJCas, start, end + 1);
                mention.setEntityType(entityMention.getAttributeValue("TYPE"));
                String mentionAceId = entityMention.getAttributeValue("ID");
                mention.setId(mentionAceId);

                int headStart = Integer.parseInt(entityMention.getChild("head").getChild("charseq")
                        .getAttributeValue("START"));
                int headEnd = Integer.parseInt(entityMention.getChild("head").getChild("charseq")
                        .getAttributeValue("END"));

                mention.setHeadAnnotation(new ComponentAnnotation(aJCas, headStart, headEnd + 1));
                setComponentId(mention);
                mention.addToIndexes();
                mentions.add(mention);

                id2EntityMention.put(mentionAceId, mention);

                givenText = givenText.replaceAll("\\s+", " ");
                parsedText = givenText.replaceAll("\\s+", " ");
            }


            namedEntity.setEntityMentions(UimaConvenience.makeFsArray(mentions, aJCas));
            id2Entity.put(entityId, namedEntity);
        }

        return new Pair<Map<String, Entity>, Map<String, EntityMention>>(id2Entity, id2EntityMention);
    }

    private File getAPFFile(File sgmFile) {
        String apfFileName = sgmFile.getPath();
        apfFileName = sgmFile.getPath().substring(0, apfFileName.length() - 3) + "apf.xml";
        if (new File(apfFileName).exists())
            return new File(apfFileName);

        apfFileName = sgmFile.getPath();
        apfFileName = sgmFile.getPath().substring(0, apfFileName.length() - 3) + "entities.apf.xml";
        if (new File(apfFileName).exists())
            return new File(apfFileName);

        apfFileName = sgmFile.getPath();
        apfFileName = sgmFile.getPath().substring(0, apfFileName.length() - 3) + "mentions.apf.xml";
        if (new File(apfFileName).exists())
            return new File(apfFileName);

        return null;
    }
}
