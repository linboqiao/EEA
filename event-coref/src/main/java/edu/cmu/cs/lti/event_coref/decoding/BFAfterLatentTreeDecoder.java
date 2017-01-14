package edu.cmu.cs.lti.event_coref.decoding;

import com.google.common.collect.ListMultimap;
import edu.cmu.cs.lti.learning.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/11/17
 * Time: 1:34 PM
 *
 * @author Zhengzhong Liu
 */
public class BFAfterLatentTreeDecoder extends LatentTreeDecoder {
    private final int trainingStrategy;

    public BFAfterLatentTreeDecoder(int trainingStrategy) {
        logger.info("After link decoding strategy is " + trainingStrategy);
        this.trainingStrategy = trainingStrategy;
    }

    @Override
    public MentionSubGraph decode(MentionGraph mentionGraph, List<MentionCandidate> candidates,
                                  GraphWeightVector weights, boolean getGoldTree) {
        if (getGoldTree) {
            return getGoldTree(mentionGraph, candidates);
        } else {
            return systemDecode(mentionGraph, candidates, weights);
        }
    }

    private MentionSubGraph getGoldTree(MentionGraph mentionGraph, List<MentionCandidate> candidates) {
        MentionSubGraph goldTree = new MentionSubGraph(mentionGraph);
        if (trainingStrategy == 0) {
            getOriginalTree(mentionGraph, goldTree);
        } else if (trainingStrategy == 1) {
            getFullTree(mentionGraph, goldTree, candidates);
        } else if (trainingStrategy == 2) {
            getMinimumTree(mentionGraph, goldTree);
        }
        return goldTree;
    }

    private void getOriginalTree(MentionGraph mentionGraph, MentionSubGraph goldTree) {

    }

    private void getFullTree(MentionGraph mentionGraph, MentionSubGraph goldTree, List<MentionCandidate> candidates) {
        if (!mentionGraph.getResolvedRelations().containsKey(EdgeType.After)) {
            return;
        }

        ListMultimap<Integer, Integer> afterAdjacentMap = mentionGraph.getResolvedRelations().get(EdgeType.After);

        for (Map.Entry<Integer, Collection<Integer>> afterLinks : afterAdjacentMap.asMap().entrySet()) {
            int from = afterLinks.getKey();

            for (Integer to : afterLinks.getValue()) {
                MentionGraphEdge edge;
                EdgeDirection direction;
                if (from > to) {
                    edge = mentionGraph.getMentionGraphEdge(from, to);
                    direction = EdgeDirection.Forward;
                } else {
                    edge = mentionGraph.getMentionGraphEdge(to, from);
                    direction = EdgeDirection.Backword;
                }
                logger.info("Adding edge from " + from + " to " + to);
                logger.info("Adding edge from " + candidates.get(from-1).getHeadWord().getCoveredText() + " to " + candidates.get(to-1).getHeadWord().getCoveredText());
                goldTree.addUnlabelledEdge(candidates, edge, EdgeType.After, direction);
            }
        }
    }

    private void getMinimumTree(MentionGraph mentionGraph, MentionSubGraph goldTree) {
    }

    private MentionSubGraph systemDecode(MentionGraph mentionGraph, List<MentionCandidate> mentionCandidates,
                                         GraphWeightVector weights) {
        MentionSubGraph bestFirstTree = new MentionSubGraph(mentionGraph);

        for (int curr = 1; curr < mentionGraph.numNodes(); curr++) {
            int currentCandidateId = MentionGraph.getCandidateIndex(curr);
            MentionCandidate candidate = mentionCandidates.get(currentCandidateId);

            double rootScore = Double.NEGATIVE_INFINITY;

            Map<MentionGraphEdge, Double> forwardLinks = new HashMap<>();
            Map<MentionGraphEdge, Double> backwardLinks = new HashMap<>();

            for (int ant = 0; ant < curr; ant++) {
                MentionGraphEdge edge = mentionGraph.getMentionGraphEdge(curr, ant);
                edge.extractNodeAgnosticFeatures(mentionCandidates);

                int antMentionId = MentionGraph.getCandidateIndex(ant);
                MentionKey antMention = mentionGraph.isRoot(ant) ?
                        MentionKey.rootKey() : mentionCandidates.get(antMentionId).asKey();

                double forwardScore = 0;
                double backwardScore = 0;

                int numKeyPairs = 0;

                for (NodeKey currentKey : candidate.asKey()) {
                    for (NodeKey antKey : antMention) {
                        LabelledMentionGraphEdge forwardEdge = edge.getLabelledEdge(mentionCandidates,
                                antKey, currentKey);
                        numKeyPairs++;

                        if (mentionGraph.isRoot(ant)) {
                            // We don't need backward, forward two edges.
                            rootScore += forwardEdge.scoreEdge(EdgeType.After_Root, weights);
                        } else {
                            LabelledMentionGraphEdge backwardEdge = edge.getLabelledEdge(mentionCandidates,
                                    currentKey, antKey);
                            forwardScore += forwardEdge.scoreEdge(EdgeType.After, weights);
                            backwardScore += backwardEdge.scoreEdge(EdgeType.After, weights);
                        }
                    }
                }

                forwardScore /= numKeyPairs;
                backwardScore /= numKeyPairs;
                rootScore /= numKeyPairs;

                if (forwardScore > backwardScore && forwardScore > rootScore) {
                    // Has a forward link.
                    forwardLinks.put(edge, forwardScore);
                    logger.info("Adding forward link " + edge);
                } else if (backwardScore > forwardScore & backwardScore > rootScore) {
                    // Has a backward link.
                    backwardLinks.put(edge, backwardScore);
                    logger.info("Adding backward link " + edge);
                }
            }

            for (Map.Entry<MentionGraphEdge, Double> forwardLink : forwardLinks.entrySet()) {
                bestFirstTree.addUnlabelledEdge(mentionCandidates, forwardLink.getKey(), EdgeType.After,
                        EdgeDirection.Forward);
            }

            for (Map.Entry<MentionGraphEdge, Double> backwardLink : backwardLinks.entrySet()) {
                bestFirstTree.addUnlabelledEdge(mentionCandidates, backwardLink.getKey(), EdgeType.After,
                        EdgeDirection.Backword);
            }
        }
        return bestFirstTree;
    }
}
