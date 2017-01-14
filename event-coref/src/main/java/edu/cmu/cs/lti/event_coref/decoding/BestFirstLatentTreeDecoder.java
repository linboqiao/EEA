package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.learning.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.graph.*;
import edu.cmu.cs.lti.utils.MathUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Decode the tree edges using a latent tree decoder.
 *
 * @author Zhengzhong Liu
 */
public class BestFirstLatentTreeDecoder extends LatentTreeDecoder {
    public BestFirstLatentTreeDecoder() {
        super();
    }

    @Override
    public MentionSubGraph decode(MentionGraph mentionGraph, List<MentionCandidate> mentionCandidates,
                                  GraphWeightVector weights, boolean getGoldTree) {
        MentionSubGraph bestFirstTree = new MentionSubGraph(mentionGraph);

        for (int curr = 1; curr < mentionGraph.numNodes(); curr++) {
            Pair<LabelledMentionGraphEdge, EdgeType> bestEdge = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (int ant = 0; ant < curr; ant++) {
                MentionGraphEdge edge = mentionGraph.getMentionGraphEdge(curr, ant);

                if (getGoldTree) {
                    if (!edge.hasRealLabelledEdge()) {
                        continue;
                    }
                }

                for (LabelledMentionGraphEdge labelledEdge : edge.getAllLabelledEdges(mentionCandidates)) {
                    double score;
                    EdgeType edgeType;

                    if (getGoldTree) {
                        if (labelledEdge.hasActualEdgeType()) {
                            score = labelledEdge.getCorrectLabelScore(weights);
                            edgeType = labelledEdge.getActualEdgeType();
                        } else {
                            continue;
                        }
                    } else {
                        Pair<EdgeType, Double> bestLabelScore = labelledEdge.getBestLabelScore(weights);
                        score = bestLabelScore.getValue();
                        edgeType = bestLabelScore.getKey();
                    }

                    if (Double.isNaN(score)) {
                        logger.error("Link score is NaN : " + labelledEdge.toString());
                        throw new RuntimeException("Link score is NaN, there might be errors in weights.");
                    }

                    if (MathUtils.sureLarger(score, bestScore)) {
                        bestEdge = Pair.of(labelledEdge, edgeType);

//                    logger.info(mentionGraphEdge.getFeatureVector().readableString());

//                    logger.info("Best edge is " + mentionGraphEdge.toString());
//                    logger.info("Best edge type is " + edgeType);
//                    logger.info("Best score is " + score + " sure larger than current best " + bestScore);

                        bestScore = score;
                    } else if (MathUtils.almostEqual(score, bestScore)) {
                        // When tie, we break tie using the distance. Since the distance to root is considered 0, we
                        // never override a ROOT link decision.
                        if (bestEdge == null || !bestEdge.getValue().equals(EdgeType.Coref_Root)) {
                            bestEdge = Pair.of(labelledEdge, edgeType);
                            bestScore = score;

//                        logger.info("Tie break winner edge is between " + mentionGraphEdge.toString());
//                        logger.info("Tie break winner edge type is " + edgeType);
//                        logger.info("Tie break winner score is " + bestScore);
                        }

//                    logger.info("Discarded edge is between " + mentionGraphEdge.toString());
//                    logger.info("Discarded edge type is " + edgeType);
//                    logger.info("Discarded score is " + score);
                    }
                }

                bestFirstTree.addLabelledEdge(bestEdge.getLeft(), bestEdge.getRight());
            }
        }
        return bestFirstTree;
    }
}
