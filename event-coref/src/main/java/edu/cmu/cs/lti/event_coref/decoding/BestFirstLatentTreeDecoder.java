package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.LabelledMentionGraphEdge;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.utils.CubicLagrangian;
import edu.cmu.cs.lti.utils.MathUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

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
    public MentionSubGraph decode(MentionGraph mentionGraph, List<MentionCandidate> mentionCandidates,
                                  GraphWeightVector weights, PairFeatureExtractor extractor,
                                  CubicLagrangian u, CubicLagrangian v) {
        MentionSubGraph bestFirstTree = new MentionSubGraph(mentionGraph);

        for (int curr = 1; curr < mentionGraph.numNodes(); curr++) {
            Pair<LabelledMentionGraphEdge, EdgeType> bestEdge = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            int currentCandidateId = MentionGraph.getCandidateIndex(curr);

            MentionCandidate candidate = mentionCandidates.get(currentCandidateId);

            for (NodeKey currentKey : candidate.asKey()) {
                for (int ant = 0; ant < curr; ant++) {
                    int antMentionId = MentionGraph.getCandidateIndex(ant);

                    MentionKey antMention = mentionGraph.isRoot(ant) ?
                            MentionKey.rootKey() : mentionCandidates.get(antMentionId).asKey();

                    for (NodeKey antKey : antMention) {

                        LabelledMentionGraphEdge mentionGraphEdge = mentionGraph.getMentionGraphEdge(curr, ant)
                                .getLabelledEdge(mentionCandidates, antKey, currentKey);

                        Pair<EdgeType, Double> bestLabelScore = mentionGraphEdge.getBestLabelScore(weights);
                        double score = bestLabelScore.getRight();
                        EdgeType edgeType = bestLabelScore.getLeft();

                        if (Double.isNaN(score)) {
                            logger.error("Link score is NaN : " + mentionGraphEdge.toString());
                            throw new RuntimeException("Link score is NaN, there might be errors in weights.");
                        }

                        // We have a special root node at the begin, so we minus one to get the original sequence index.
                        double lagrangianPenalty = 0;
                        if (ant > 0) {
                            lagrangianPenalty =
                                    u.getSumOverTVariable(curr - 1, ant - 1) + v.getSumOverTVariable(curr - 1, ant - 1);
                        }

                        score += lagrangianPenalty;

                        if (MathUtils.sureLarger(score, bestScore)) {
                            bestEdge = Pair.of(mentionGraphEdge, edgeType);

//                    logger.info(mentionGraphEdge.getFeatureVector().readableString());

//                    logger.info("Best edge is " + mentionGraphEdge.toString());
//                    logger.info("Best edge type is " + edgeType);
//                    logger.info("Best score is " + score + " sure larger than current best " + bestScore);

                            bestScore = score;
                        } else if (MathUtils.almostEqual(score, bestScore)) {
                            // When tie, we break tie using the distance. Since the distance to root is considered 0, we
                            // never override a ROOT link decision.
                            if (bestEdge == null || !bestEdge.getValue().equals(EdgeType.Root)) {
                                bestEdge = Pair.of(mentionGraphEdge, edgeType);
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
                }

                bestFirstTree.addEdge(bestEdge.getLeft(), bestEdge.getRight());
            }
        }
        return bestFirstTree;
    }
}
