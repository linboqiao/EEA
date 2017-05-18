package edu.cmu.cs.lti.after.annotators;

import edu.cmu.cs.lti.learning.model.graph.GraphUtils;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.pipeline.ProcessorWrapper;
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
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A baseline that copy temporal with predicted cluster.
 *
 * @author Zhengzhong Liu
 */
public class SelectedTemporalBaseline extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<EventMentionRelation> originalRelations = UimaConvenience.getAnnotationList(aJCas,
                EventMentionRelation.class);

        List<EventMention> mentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));

        List<Set<EventMention>> clusters = createScriptCluster(mentions, originalRelations);

        for (EventMentionRelation originalRelation : originalRelations) {
            originalRelation.removeFromIndexes();
        }

        for (MentionLink link : JCasUtil.select(aJCas, MentionLink.class)) {
            for (Set<EventMention> cluster : clusters) {
                if (cluster.contains(link.getSource()) && cluster.contains(link.getTarget())) {
                    createLink(aJCas, link.getSource(), link.getTarget(), link.getRelationType());
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

    private List<Set<EventMention>> createScriptCluster(List<EventMention> mentions,
                                                        List<EventMentionRelation> relations) {
        List<Pair<EventMention, EventMention>> plainRelations = new ArrayList<>();
        for (EventMentionRelation relation : relations) {
            EventMention from = relation.getHead();
            EventMention to = relation.getChild();
            plainRelations.add(Pair.of(from, to));
        }
        return GraphUtils.createCluster(mentions, plainRelations);
    }

    public static CollectionReaderDescription run(CollectionReaderDescription reader,
                                                  TypeSystemDescription typeSystemDescription,
                                                  String mainDir, String baseOutput)
            throws SAXException, UIMAException, CpeDescriptorException, IOException {
        return new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription annotator = AnalysisEngineFactory.createEngineDescription(
                        SelectedTemporalBaseline.class, typeSystemDescription);
                return new AnalysisEngineDescription[]{annotator};
            }
        }, mainDir, baseOutput).runWithOutput();
    }
}
