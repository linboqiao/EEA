package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge;
import edu.cmu.cs.lti.event_coref.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
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
    @Override
    public MentionSubGraph decode(MentionGraph mentionGraph, FeatureAlphabet featureAlphabet, ClassAlphabet
            classAlphabet, GraphWeightVector weights, PairFeatureExtractor extractor) {
        MentionSubGraph bestFirstTree = new MentionSubGraph(mentionGraph);

        logger.debug("Decoding with best first.");
        for (int curr = 1; curr < mentionGraph.numNodes(); curr++) {
            Pair<MentionGraphEdge, MentionGraphEdge.EdgeType> bestEdge = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int ant = 0; ant < curr; ant++) {
                MentionGraphEdge mentionGraphEdge = mentionGraph.getMentionGraphEdges()[curr][ant];
                Pair<MentionGraphEdge.EdgeType, Double> bestLabelScore = mentionGraphEdge.getBestLabelScore(weights,
                        extractor);
                double score = bestLabelScore.getValue1();
                MentionGraphEdge.EdgeType label = bestLabelScore.getValue0();

                if (bestEdge == null) {
                    bestEdge = Pair.with(mentionGraphEdge, label);
                    bestScore = score;
                } else if (score > bestScore) {
                    bestEdge = Pair.with(mentionGraphEdge, label);
                    bestScore = score;
                }
            }
            bestFirstTree.addEdge(bestEdge.getValue0(), bestEdge.getValue1());
        }
        return bestFirstTree;
    }
}
