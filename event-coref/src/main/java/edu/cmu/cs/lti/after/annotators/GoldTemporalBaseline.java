package edu.cmu.cs.lti.after.annotators;

import edu.cmu.cs.lti.learning.model.graph.GraphUtils;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.timeml.MentionLink;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.*;

/**
 * A baseline that copy temporal with predicted cluster
 *
 * @author Zhengzhong Liu
 */
public class GoldTemporalBaseline extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<Set<EventMention>> clusters = createGoldStandardScriptCluster(aJCas);

        Collection<MentionLink> mentionLinks = JCasUtil.select(aJCas, MentionLink.class);

//        logger.info("Number of non singleton clusters: " + clusters.size());
//        logger.info("Number of mention links: " + mentionLinks.size());

        for (MentionLink link : mentionLinks) {
            for (Set<EventMention> cluster : clusters) {
                if (cluster.contains(link.getSource()) && cluster.contains(link.getTarget())) {
                    if (link.getSource() != link.getTarget()) {
                        createLink(aJCas, link.getSource(), link.getTarget(), link.getRelationType());
                    }
//                    logger.info("Creating link based on gold standard " + link.getSource().getCoveredText() + " " +
//                            link.getTarget().getCoveredText() + " " + link.getRelationType());
                }
            }
        }
    }

    private void createLink(JCas aJCas, EventMention from, EventMention to, String timemlType) {
        if (timemlType.equals("AFTER")) {
            createAfterLink(aJCas, to, from);
        } else if (timemlType.equals("BEFORE")) {
            createAfterLink(aJCas, from, to);
        }
    }

    private void createAfterLink(JCas aJCas, EventMention from, EventMention to) {
        EventMentionRelation afterLink = new EventMentionRelation(aJCas);
        afterLink.setHead(from);
        afterLink.setChild(to);
        afterLink.setRelationType("After");
        UimaAnnotationUtils.finishTop(afterLink, COMPONENT_ID, 0, aJCas);
    }

    private List<Set<EventMention>> createGoldStandardScriptCluster(JCas aJCas) {
        List<EventMentionRelation> originalRelations = UimaConvenience.getAnnotationList(aJCas,
                EventMentionRelation.class);

        for (EventMentionRelation originalRelation : originalRelations) {
            originalRelation.removeFromIndexes();
        }

        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, aJCas);

        List<EventMention> normalMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));
        List<EventMention> goldMentions = new ArrayList<>(JCasUtil.select(goldView, EventMention.class));

        Map<EventMention, EventMention> gold2Normal = ensureMentionMapping(normalMentions, goldMentions);

        List<Pair<EventMention, EventMention>> plainRelations = new ArrayList<>();

        for (EventMentionRelation relation : JCasUtil.select(goldView, EventMentionRelation.class)) {
            EventMention from = relation.getHead();
            EventMention to = relation.getChild();
            plainRelations.add(Pair.of(gold2Normal.get(from), gold2Normal.get(to)));
        }

        return GraphUtils.createCluster(normalMentions, plainRelations);
    }

    private Map<EventMention, EventMention> ensureMentionMapping(List<EventMention> normalMentions,
                                                                 List<EventMention> goldMentions) {
        Map<EventMention, EventMention> gold2Normal = new HashMap<>();

        if (normalMentions.size() != goldMentions.size()) {
            logger.error("Different mentions from gold standard, cannot use this baseline!");
        }

        for (int i = 0; i < normalMentions.size(); i++) {
            if (normalMentions.get(i).getBegin() != goldMentions.get(i).getBegin()) {
                logger.error("Different mentions from gold standard, cannot use this baseline!");
            } else {
                gold2Normal.put(goldMentions.get(i), normalMentions.get(i));
            }
        }
        return gold2Normal;
    }

    public static CollectionReaderDescription run(CollectionReaderDescription reader,
                                                  TypeSystemDescription typeSystemDescription,
                                                  String mainDir, String baseOutput)
            throws SAXException, UIMAException, CpeDescriptorException, IOException {
        AnalysisEngineDescription annotator = AnalysisEngineFactory.createEngineDescription(
                GoldTemporalBaseline.class, typeSystemDescription);
        return new BasicPipeline(reader, mainDir, baseOutput, annotator).run().getOutput();
    }
}
