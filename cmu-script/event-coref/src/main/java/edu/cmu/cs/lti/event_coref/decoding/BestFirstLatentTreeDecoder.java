package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.learning.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.*;
import edu.cmu.cs.lti.utils.MathUtils;

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
        if (getGoldTree) {
            return goldDecode(mentionGraph, candidates, weights);
        } else {
            return systemDecode(mentionGraph, candidates, weights);
        }
    }

    private MentionSubGraph systemDecode(MentionGraph mentionGraph, List<MentionCandidate> candidates,
                                         GraphWeightVector weights) {
        MentionSubGraph bestFirstTree = new MentionSubGraph(mentionGraph);

        for (int curr = 1; curr < mentionGraph.numNodes(); curr++) {
            MentionKey currentKeys = candidates.get(MentionGraph.getCandidateIndex(curr)).asKey();

            MentionGraphEdge rootEdge = mentionGraph.getEdge(0, curr);
            rootEdge.extractNodeAgnosticFeatures(candidates);

            for (NodeKey currentKey : currentKeys) {
                LabelledMentionGraphEdge labelledRootEdge = rootEdge.getLabelledEdge(candidates,
                        NodeKey.getRootKey(EdgeType.Coreference), currentKey);

                double rootScore = labelledRootEdge.getRootScore(weights);

                LabelledMentionGraphEdge bestEdge = null;
                double bestScore = Double.NEGATIVE_INFINITY;

                for (int ant = 1; ant < curr; ant++) {
                    MentionKey antKeys = candidates.get(MentionGraph.getCandidateIndex(ant)).asKey();
                    MentionGraphEdge edge = mentionGraph.getEdge(ant, curr);
                    edge.extractNodeAgnosticFeatures(candidates);

                    for (NodeKey antKey : antKeys) {
                        LabelledMentionGraphEdge labelledEdge = edge.getLabelledEdge(candidates, antKey, currentKey);
//                        Pair<EdgeType, Double> bestLabelScore = labelledEdge.getBestLabelScore(weights);

                        double corefScore = labelledEdge.scoreEdge(EdgeType.Coreference, weights);

//                        double score = bestLabelScore.getValue();
//                        EdgeType edgeType = bestLabelScore.getKey();

                        if (Double.isNaN(corefScore)) {
                            logger.error("Link score is NaN : " + labelledEdge.toString());
                            throw new RuntimeException("Link score is NaN, there might be errors in weights.");
                        }

                        if (MathUtils.sureLarger(corefScore, bestScore)) {
                            bestEdge = labelledEdge;
                            bestScore = corefScore;
                        } else if (MathUtils.almostEqual(corefScore, bestScore)) {
                            // When tie, we break tie using the distance: we always favor closer antecedents.
                            bestEdge = labelledEdge;
                            bestScore = corefScore;
                        }
                    }
                }

                if (MathUtils.sureLarger(bestScore, rootScore)) {
                    bestFirstTree.addLabelledEdge(bestEdge, EdgeType.Coreference);
                } else {
                    bestFirstTree.addLabelledEdge(labelledRootEdge, EdgeType.Root);
                }
            }
        }
        return bestFirstTree;
    }

    private MentionSubGraph goldDecode(MentionGraph mentionGraph, List<MentionCandidate> candidates,
                                       GraphWeightVector weights) {
        MentionSubGraph bestFirstTree = new MentionSubGraph(mentionGraph);

        for (int curr = 1; curr < mentionGraph.numNodes(); curr++) {
            MentionKey currentKeys = candidates.get(MentionGraph.getCandidateIndex(curr)).asKey();

            MentionGraphEdge rootEdge = mentionGraph.getEdge(0, curr);
            rootEdge.extractNodeAgnosticFeatures(candidates);

            for (NodeKey currentKey : currentKeys) {
                LabelledMentionGraphEdge labelledRootEdge = rootEdge.getLabelledEdge(candidates,
                        NodeKey.getRootKey(EdgeType.Coreference), currentKey);

                LabelledMentionGraphEdge bestEdge = null;
                double bestScore = Double.NEGATIVE_INFINITY;

                for (int ant = 1; ant < curr; ant++) {
                    MentionKey antKeys = candidates.get(MentionGraph.getCandidateIndex(ant)).asKey();
                    MentionGraphEdge edge = mentionGraph.getEdge(ant, curr);
                    edge.extractNodeAgnosticFeatures(candidates);

                    for (NodeKey antKey : antKeys) {
                        LabelledMentionGraphEdge labelledEdge = edge.getLabelledEdge(candidates, antKey, currentKey);

                        if (labelledEdge.hasActualEdgeType() &&
                                labelledEdge.getActualEdgeType().equals(EdgeType.Coreference)) {

                            double score = labelledEdge.getCorrectLabelScore(weights);

                            if (Double.isNaN(score)) {
                                logger.error("Link score is NaN : " + labelledEdge.toString());
                                throw new RuntimeException("Link score is NaN, there might be errors in weights.");
                            }

                            if (MathUtils.sureLarger(score, bestScore)) {
                                bestEdge = labelledEdge;
                                bestScore = score;
                            }
                            // TODO: Edit 1, do not break tie using distance.
//                            else if (MathUtils.almostEqual(score, bestScore)) {
//                                // When tie, we break tie using the distance. We always favor closer antecedents.
//                                bestEdge = labelledEdge;
//                                bestScore = score;
//                            }
                        }
                    }
                }

                if (bestEdge != null) {
                    bestFirstTree.addLabelledEdge(bestEdge, EdgeType.Coreference);
                } else {
                    bestFirstTree.addLabelledEdge(labelledRootEdge, EdgeType.Root);
                }
            }
        }
        return bestFirstTree;
    }

}
