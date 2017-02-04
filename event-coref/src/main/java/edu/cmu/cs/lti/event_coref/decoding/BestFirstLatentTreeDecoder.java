package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.learning.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.learning.model.NodeKey;
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
    public MentionSubGraph decode(MentionGraph mentionGraph, List<MentionCandidate> candidates,
                                  GraphWeightVector weights, boolean getGoldTree) {
        MentionSubGraph bestFirstTree = new MentionSubGraph(mentionGraph);

        for (int curr = 1; curr < mentionGraph.numNodes(); curr++) {
            MentionKey currentKeys = candidates.get(MentionGraph.getCandidateIndex(curr)).asKey();

            MentionGraphEdge rootEdge = mentionGraph.getEdge(0, curr);
            rootEdge.extractNodeAgnosticFeatures(candidates);

            for (NodeKey currentKey : currentKeys) {
                LabelledMentionGraphEdge labelledRootEdge = rootEdge.getLabelledEdge(candidates,
                        NodeKey.getRootKey(EdgeType.Coreference), currentKey);
                double rootScore = labelledRootEdge.getRootScore(weights);

                Pair<LabelledMentionGraphEdge, EdgeType> bestEdge = null;
                double bestScore = Double.NEGATIVE_INFINITY;
                for (int ant = 1; ant < curr; ant++) {
                    MentionKey antKeys = candidates.get(MentionGraph.getCandidateIndex(ant)).asKey();
                    MentionGraphEdge edge = mentionGraph.getEdge(ant, curr);
                    edge.extractNodeAgnosticFeatures(candidates);

                    if (getGoldTree) {
                        if (!edge.hasRealLabelledEdge()) {
                            continue;
                        }
                    }

                    for (NodeKey antKey : antKeys) {
                        LabelledMentionGraphEdge labelledEdge = edge.getLabelledEdge(candidates, antKey, currentKey);
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
                            bestScore = score;
                        } else if (MathUtils.almostEqual(score, bestScore)) {
                            // When tie, we break tie using the distance. Since the distance to root is considered 0, we
                            // never override a ROOT link decision.
                            if (bestEdge == null) {
                                bestEdge = Pair.of(labelledEdge, edgeType);
                                bestScore = score;
                            }
                        }
                    }
                }

                if (MathUtils.sureLarger(bestScore, rootScore)) {
//                logger.info(String.format("Edge score %.2f is higher than root score %.2f.", bestScore, rootScore));
//                logger.info(bestEdge.toString());
                    bestFirstTree.addLabelledEdge(bestEdge.getLeft(), bestEdge.getRight());
                } else {
                    bestFirstTree.addLabelledEdge(labelledRootEdge, EdgeType.Root);
                }
            }
        }
        return bestFirstTree;
    }
}
