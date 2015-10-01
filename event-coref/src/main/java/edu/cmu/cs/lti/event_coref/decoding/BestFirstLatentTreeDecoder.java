package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge;
import edu.cmu.cs.lti.event_coref.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import org.javatuples.Pair;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/26/15
 * Time: 6:23 PM
 *
 * @author Zhengzhong Liu
 */
public class BestFirstLatentTreeDecoder extends LatentTreeDecoder {

    public BestFirstLatentTreeDecoder() {
        super();
    }

    @Override
    public MentionSubGraph decode(MentionGraph mentionGraph, GraphWeightVector weights, PairFeatureExtractor
            extractor) {
        MentionSubGraph bestFirstTree = new MentionSubGraph(mentionGraph);

        for (int curr = 1; curr < mentionGraph.numNodes(); curr++) {
            Pair<MentionGraphEdge, MentionGraphEdge.EdgeType> bestEdge = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int ant = 0; ant < curr; ant++) {
                MentionGraphEdge mentionGraphEdge = mentionGraph.getMentionGraphEdge(curr, ant);
                Pair<MentionGraphEdge.EdgeType, Double> bestLabelScore =
                        mentionGraphEdge.getBestLabelScore(weights, extractor);
                double score = bestLabelScore.getValue1();
                MentionGraphEdge.EdgeType label = bestLabelScore.getValue0();

                if (score > bestScore) {
                    bestEdge = Pair.with(mentionGraphEdge, label);
                    bestScore = score;
//                    if (!label.equals(MentionGraphEdge.EdgeType.Root)) {
//                        logger.debug(String.format(
//                                "Best edge type is %s, anaphora is [%s] , antecedent is [%s], the score is %s",
//                                label.name(), mentionGraph.getNode(curr), mentionGraph.getNode(ant), bestScore));
//                    }
                }
            }
            bestFirstTree.addEdge(bestEdge.getValue0(), bestEdge.getValue1());
        }
        return bestFirstTree;
    }
}
