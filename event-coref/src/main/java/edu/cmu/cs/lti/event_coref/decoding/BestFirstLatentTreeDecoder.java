package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.learning.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.*;
import edu.cmu.cs.lti.utils.MathUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            int currentCandidateId = MentionGraph.getCandidateIndex(curr);
            MentionCandidate candidate = mentionCandidates.get(currentCandidateId);

            for (int ant = 0; ant < curr; ant++) {
                //  Compute the averaged after link score between ant and curr.
                double averagedAfterScore = 0;
                int afterPairs = 0;

                MentionGraphEdge edge = mentionGraph.getMentionGraphEdge(curr, ant);
                edge.extractNodeAgnosticFeatures(mentionCandidates);

                int antMentionId = MentionGraph.getCandidateIndex(ant);
                MentionKey antMention = mentionGraph.isRoot(ant) ?
                        MentionKey.rootKey() : mentionCandidates.get(antMentionId).asKey();

                Map<Pair<LabelledMentionGraphEdge, EdgeType>, Double> bestEdges = new HashMap<>();

                for (NodeKey currentKey : candidate.asKey()) {
                    Pair<LabelledMentionGraphEdge, EdgeType> bestEdge = null;
                    double bestScore = Double.NEGATIVE_INFINITY;

                    for (NodeKey antKey : antMention) {
                        LabelledMentionGraphEdge labelledEdge = edge.getLabelledEdge(mentionCandidates,
                                antKey, currentKey);

                        Map<EdgeType, Double> labelScores;

                        if (getGoldTree) {
                            labelScores = new HashMap<>();
                            if (labelledEdge.hasActualEdgeType()) {
                                double score = labelledEdge.getCorrectLabelScore(weights);
                                labelScores.put(labelledEdge.getActualEdgeType(), score);
                            }
                        } else {
                            labelScores = labelledEdge.getBestLabelScore(weights);
                        }

                        for (Map.Entry<EdgeType, Double> labelScore : labelScores.entrySet()) {
                            EdgeType edgeType = labelScore.getKey();
                            double score = labelScore.getValue();

                            // TODO: maybe control such thing during update.
                            if (Double.isNaN(score)) {
                                logger.error("Link score is NaN : " + labelledEdge.toString());
                                throw new RuntimeException("Link score is NaN, there might be errors in weights.");
                            }

                            switch (edgeType) {
                                case After:
                                    averagedAfterScore += score;
                                    afterPairs++;
                                    break;
                                default:
                                    if (MathUtils.sureLarger(score, bestScore)) {
                                        bestScore = score;
                                        bestEdge = Pair.of(labelledEdge, edgeType);
                                    } else if (MathUtils.almostEqual(score, bestScore)) {
                                        // When tie, we break tie using the distance. Since the distance to root is
                                        // considered 0, we never override a ROOT link decision.
                                        if (bestEdge == null || !bestEdge.getValue().equals(EdgeType.Root)) {
                                            bestEdge = Pair.of(labelledEdge, edgeType);
                                            bestScore = score;
                                        }
                                    }
                            }
                        }
                    }
                    bestEdges.put(bestEdge, bestScore);
                }

                //Now compute the averaged after link score between the two candidates.
                if (afterPairs > 0) {
                    averagedAfterScore /= afterPairs;
                } else {
                    averagedAfterScore = Double.NEGATIVE_INFINITY;
                }

                boolean afterOverwrite = true;
                for (Map.Entry<Pair<LabelledMentionGraphEdge, EdgeType>, Double> bestEdge : bestEdges.entrySet()) {
                    double score = bestEdge.getValue();
                    if (MathUtils.sureLarger(score, averagedAfterScore) ||
                            MathUtils.almostEqual(score, averagedAfterScore)) {
                        afterOverwrite = false;
                    }
                }

                if (afterOverwrite) {
                    bestFirstTree.addUnlabelledEdge(edge, EdgeType.After);
                } else {
                    bestEdges.forEach((e, s) -> bestFirstTree.addLabelledEdge(e.getKey(), e.getValue()));
                }
            }
        }

        return bestFirstTree;
    }
}
