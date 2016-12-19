package edu.cmu.cs.lti.after.train;

import com.google.common.collect.ListMultimap;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.DebugUtils;
import edu.cmu.cs.lti.utils.MentionUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Train a pairwise based after linking model.
 * <p>
 * Date: 12/12/16
 * Time: 3:35 PM
 *
 * @author Zhengzhong Liu
 */
public class PairwiseAfterTrainer extends AbstractLoggingAnnotator {
    private PairFeatureExtractor extractor;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);

        List<MentionCandidate> candidates = MentionUtils.getSpanBasedCandidates(aJCas);
        MentionGraph mentionGraph = MentionUtils.createMentionGraph(aJCas, candidates, extractor, false);

        Map<EdgeType, ListMultimap<Integer, Integer>> spanRelations = mentionGraph.getResolvedRelations();

        for (EdgeType edgeType : spanRelations.keySet()) {
            logger.info("Showing edges of type : " + edgeType);
            for (Map.Entry<Integer, Collection<Integer>> relations : spanRelations.get(edgeType).asMap().entrySet()) {
                int fromNodeCandidateId = MentionGraph.getCandidateIndex(relations.getKey());
                for (int toNodeId : relations.getValue()) {
                    int toNodeCandidateId = MentionGraph.getCandidateIndex(toNodeId);
                    logger.info(String.format("Relation from [%s] to [%s].",
                            candidates.get(fromNodeCandidateId), candidates.get(toNodeCandidateId)));
                }
            }
        }

        DebugUtils.pause();
    }
}
