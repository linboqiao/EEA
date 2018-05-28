package edu.cmu.cs.lti.collection_reader;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.NoiseTextFormatter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Read rich ERE style data.
 * Date: 1/25/16
 * Time: 5:32 PM
 *
 * @author Zhengzhong Liu
 */
public class EreCorpusReader extends AbstractCollectionReader {
    public static final String PARAM_SOURCE_TEXT_DIR = "sourceTextDir";
    @ConfigurationParameter(name = PARAM_SOURCE_TEXT_DIR)
    private File sourceTextDir;

    public static final String PARAM_ERE_ANNOTATION_DIR = "ereAnnotationDir";
    @ConfigurationParameter(name = PARAM_ERE_ANNOTATION_DIR)
    private File ereAnnotationDir;

    public static final String PARAM_SOURCE_EXT = "sourceSuffix";
    @ConfigurationParameter(name = PARAM_SOURCE_EXT)
    private String sourceExt;

    public static final String PARAM_ERE_ANNOTATION_EXT = "ereSourceSuffix";
    @ConfigurationParameter(name = PARAM_ERE_ANNOTATION_EXT)
    private String ereExt;

    private List<Pair<File, File>> sourceAndAnnotationFiles;

    public static final String COMPONENT_ID = LDCXmlCollectionReader.class.getSimpleName();

    private int fileIndex;

    private DocumentBuilder documentBuilder;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        sourceAndAnnotationFiles = new ArrayList<>();

        Collection<File> sourceFiles = FileUtils.listFiles(sourceTextDir, new String[]{sourceExt}, false);

        for (File sourceFile : sourceFiles) {
            File ereFile = new File(ereAnnotationDir, sourceFile.getName().replaceAll(sourceExt + "$", ereExt));

            if (ereFile.exists()) {
                sourceAndAnnotationFiles.add(Pair.of(sourceFile, ereFile));
            } else {
                sourceAndAnnotationFiles.add(Pair.of(sourceFile, null));
            }
        }

        logger.info(String.format("%d files are going to be read.", sourceAndAnnotationFiles.size()));
        fileIndex = 0;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        try {
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        File sourceFile = sourceAndAnnotationFiles.get(fileIndex).getKey();
        File ereFile = sourceAndAnnotationFiles.get(fileIndex).getValue();
        fileIndex++;

        UimaAnnotationUtils.setSourceDocumentInformation(jCas, sourceFile.toURI().toURL().toString(),
                (int) sourceFile.length(), 0, true);
        String sourceFileStr = FileUtils.readFileToString(sourceFile);
        String documentText = new NoiseTextFormatter(sourceFileStr).cleanForum().multiNewLineBreaker("zh").getText();

        Article article = new Article(jCas);
        UimaAnnotationUtils.finishAnnotation(article, 0, documentText.length(), COMPONENT_ID, 0, jCas);
        article.setArticleName(StringUtils.removeEnd(sourceFile.getName(), "." + sourceExt));
        article.setLanguage(language);

        if (sourceFileStr.length() != documentText.length()) {
            throw new CollectionException(new Exception(String.format(
                    "Length difference after cleaned, before : %d, " + "after : %d",
                    sourceFileStr.length(), documentText.length())));
        }

        if (inputViewName != null) {
            try {
                JCas inputView = ViewCreatorAnnotator.createViewSafely(jCas, inputViewName);
                inputView.setDocumentText(sourceFileStr);
            } catch (AnalysisEngineProcessException e) {
                throw new CollectionException(e);
            }
        }

        JCas goldView = null;
        try {
            goldView = jCas.createView(goldStandardViewName);
        } catch (CASException e) {
            throw new CollectionException(e);
        }

        jCas.setDocumentText(documentText);
        goldView.setDocumentText(documentText);

        if (ereFile != null) {
            try {
                annotateGoldStandard(goldView, ereFile);
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }
    }

    private void annotateGoldStandard(JCas goldView, File ereFile) throws IOException, SAXException {
        Document document = documentBuilder.parse(ereFile);
        Map<String, EntityMention> id2EntityMention = new HashMap<>();
        annotateEntity(goldView, document, id2EntityMention);
        annotateFillers(goldView, document, id2EntityMention);
        annotateRelations(goldView, document, id2EntityMention);
        annotateEvents(goldView, document, id2EntityMention);
    }

    private void annotateEvents(JCas view, Document document, Map<String, EntityMention> id2EntityMention) {
        NodeList hopperNodes = document.getElementsByTagName("hopper");

        for (int hopperNodeIndex = 0; hopperNodeIndex < hopperNodes.getLength(); hopperNodeIndex++) {
            Node hopperNode = hopperNodes.item(hopperNodeIndex);
            String hopperId = getAttribute(hopperNode, "id");

            List<EventMention> mentionCluster = new ArrayList<>();
            for (Node eventMentionNode : getSubNodes(hopperNode, "event_mention")) {
                String mentionId = getAttribute(eventMentionNode, "id");
                String mentionType = getAttribute(eventMentionNode, "type");
                String mentionSubType = getAttribute(eventMentionNode, "subtype");
                String mergedType = mentionType + "_" + mentionSubType;
                String realisStatus = getAttribute(eventMentionNode, "realis");

                Node triggerNode = getSubNode(eventMentionNode, "trigger");
                int triggerStart = Integer.parseInt(getAttribute(triggerNode, "offset"));
                int triggerLength = Integer.parseInt(getAttribute(triggerNode, "length"));
                int triggerEnd = triggerStart + triggerLength;

                if (validateAnnotation(view, triggerStart, triggerEnd, triggerNode.getTextContent())) {
                    EventMention mention = new EventMention(view, triggerStart, triggerEnd);
                    mention.setEventType(mergedType);
                    mention.setRealisType(realisStatus);

                    List<Node> argNodes = getSubNodes(eventMentionNode, "em_arg");
                    for (Node argNode : argNodes) {
//                        String argEntityId = getAttribute(argNode, "entity_id");
                        String argEntityMentionId = getAttribute(argNode, "entity_mention_id");
                        if (argEntityMentionId == null) {
                            argEntityMentionId = getAttribute(argNode, "filler_id");
                        }

                        String argRole = getAttribute(argNode, "role");
                        String realis = getAttribute(argNode, "realis");

                        EventMentionArgumentLink link = new EventMentionArgumentLink(view);
                        link.setArgumentRole(argRole);
                        EntityMention argEntityMention = id2EntityMention.get(argEntityMentionId);
                        link.setArgument(argEntityMention);
                        link.setEventMention(mention);
                        link.setRealis(realis);
                        UimaAnnotationUtils.finishTop(link, COMPONENT_ID, 0, view);
                    }

                    UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, mentionId, view);
                    mentionCluster.add(mention);
                }
            }

            Event event = new Event(view);
            event.setEventMentions(FSCollectionFactory.createFSArray(view, mentionCluster));
            UimaAnnotationUtils.finishTop(event, COMPONENT_ID, hopperId, view);
        }
    }

    private void annotateRelations(JCas view, Document document, Map<String, EntityMention> id2EntityMention) {
        NodeList relationNodes = document.getElementsByTagName("relation");

        for (int relationNodeIndex = 0; relationNodeIndex < relationNodes.getLength(); relationNodeIndex++) {
            Node relationNode = relationNodes.item(relationNodeIndex);
            String relationId = getAttribute(relationNode, "id");
            String relationType = getAttribute(relationNode, "type");
            String relationSubType = getAttribute(relationNode, "subtype");

            String mergedType = relationType + "_" + relationSubType;

            Node relationMentionNode = getSubNode(relationNode, "relation_mention");

            // Assume relation have only 2 argument.
            Node relationArg1 = getSubNode(relationMentionNode, "rel_arg1");
            Node relationArg2 = getSubNode(relationMentionNode, "rel_arg2");

            String arg1ElementId = getAttribute(relationArg1, "entity_mention_id");


            String arg2ElementId = getAttribute(relationArg2, "entity_mention_id");

            EntityMentionRelation mentionRelation = new EntityMentionRelation(view);
            mentionRelation.setHead(id2EntityMention.get(arg1ElementId));
            mentionRelation.setChild(id2EntityMention.get(arg2ElementId));
            mentionRelation.setRelationType(mergedType);
            UimaAnnotationUtils.finishTop(mentionRelation, COMPONENT_ID, relationId, view);
        }
    }

    private void annotateFillers(JCas view, Document document, Map<String, EntityMention> id2EntityMention) {
        NodeList fillerNodes = document.getElementsByTagName("filler");

        for (int fillerNodeIndex = 0; fillerNodeIndex < fillerNodes.getLength(); fillerNodeIndex++) {
            Node fillerNode = fillerNodes.item(fillerNodeIndex);
            String fillerId = getAttribute(fillerNode, "id");
            int offset = Integer.parseInt(getAttribute(fillerNode, "offset"));
            int length = Integer.parseInt(getAttribute(fillerNode, "length"));
            int end = offset + length;
            String type = getAttribute(fillerNode, "type");

            if (validateAnnotation(view, offset, end, fillerNode.getTextContent())) {
                EntityMention filler = new EntityMention(view);
                filler.setEntityType(type);
                filler.setId(fillerId);
                UimaAnnotationUtils.finishAnnotation(filler, COMPONENT_ID, fillerId, view);

                id2EntityMention.put(fillerId, filler);
            }
        }
    }

    private List<EntityMention> annotateEntity(JCas view, Document document,
                                               Map<String, EntityMention> id2EntityMention) {
        NodeList entityNodes = document.getElementsByTagName("entity");

        List<EntityMention> entityMentions = new ArrayList<>();
        for (int entityIndex = 0; entityIndex < entityNodes.getLength(); entityIndex++) {
            Node entityNode = entityNodes.item(entityIndex);
            String entityId = getAttribute(entityNode, "id");
            String entityType = getAttribute(entityNode, "type");

            List<Node> entityMentionNodes = getSubNodes(entityNode, "entity_mention");

            for (Node entityMentionNode : entityMentionNodes) {
                String entityMentionId = getAttribute(entityMentionNode, "id");
                int entityMentionStart = Integer.parseInt(getAttribute(entityMentionNode, "offset"));
                int entityMentionLength = Integer.parseInt(getAttribute(entityMentionNode, "length"));
                int entityMentionEnd = entityMentionStart + entityMentionLength;

                String mentionText = "";

                Node mentionTextNode = getSubNode(entityMentionNode, "mention_text");
                if (mentionTextNode != null) {
                    mentionText = mentionTextNode.getNodeValue();
                }

                if (validateAnnotation(view, entityMentionStart, entityMentionEnd, mentionText)) {
                    EntityMention mention = new EntityMention(view, entityMentionStart, entityMentionEnd);
                    mention.setEntityType(entityType);
                    id2EntityMention.put(entityMentionId, mention);
                    entityMentions.add(mention);
                    UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, entityMentionId, view);
                }
            }

            Entity entity = new Entity(view);
            entity.setEntityMentions(FSCollectionFactory.createFSArray(view, entityMentions));
            entity.setEntityType(entityType);
            UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, entityId, view);
        }
        return entityMentions;
    }

    private String getAttribute(Node node, String attributeName) {
        NamedNodeMap entityMentionAttributes = node.getAttributes();
        Node attributeNode = entityMentionAttributes.getNamedItem(attributeName);

        if (attributeNode == null) {
            return null;
        }

        return attributeNode.getNodeValue();
    }

    private Node getSubNode(Node node, String subNodeName) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node subNode = node.getChildNodes().item(i);
            if (subNode.getNodeName().equals(subNodeName)) {
                return subNode;
            }
        }
        return null;
    }

    private List<Node> getSubNodes(Node node, String subNodeName) {
        List<Node> subnodes = new ArrayList<>();
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node subnode = node.getChildNodes().item(i);
            if (subnode.getNodeName().equals(subNodeName)) {
                subnodes.add(subnode);
            }
        }
        return subnodes;
    }

    private boolean validateAnnotation(JCas view, int start, int end, String expectedText) {
        if (view.getDocumentText().substring(start, end).equals(expectedText)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return fileIndex < sourceAndAnnotationFiles.size();
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(fileIndex, sourceAndAnnotationFiles.size(), Progress.ENTITIES)};
    }

//    public static void main(String[] argv) {
//        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(EreCorpusReader.class,
//                typeSystemDescription,
//                EreCorpusReader.PARAM_ERE_ANNOTATION_DIR, annotationDir,
//                EreCorpusReader.PARAM_SOURCE_TEXT_DIR, sourceDir,
//                EreCorpusReader.PARAM_ERE_ANNOTATION_EXT, "rich_ere.xml",
//                EreCorpusReader.PARAM_SOURCE_EXT, "mp.txt",
//                EreCorpusReader.PARAM_LANGUAGE, "zh");
//    }
}
