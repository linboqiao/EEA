package edu.cmu.cs.lti.event_coref.decoding;

import edu.cmu.cs.lti.learning.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.*;
import edu.cmu.cs.lti.utils.MathUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

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

        Map<NodeKey, List<NodeKey>> afterAdjacentMap = mentionGraph.getResolvedRelations().get(EdgeType.After);

        Set<NodeKey> linkedNodes = new HashSet<>();

        for (MentionCandidate candidate : candidates) {
            for (NodeKey fromNode : candidate.asKey()) {
                if (afterAdjacentMap.containsKey(fromNode)) {
                    for (NodeKey toNode : afterAdjacentMap.get(fromNode)) {
                        MentionGraphEdge edge = mentionGraph.getEdge(fromNode.getNodeIndex(), toNode.getNodeIndex());
                        LabelledMentionGraphEdge labelledEdge = edge.getLabelledEdge(candidates, fromNode, toNode);

                        goldTree.addLabelledEdge(labelledEdge, EdgeType.After);

                        // The latter node is considered as linked to the former one.
                        if (fromNode.compareTo(toNode) > 0) {
                            linkedNodes.add(fromNode);
                        } else {
                            linkedNodes.add(toNode);
                        }
                    }
                }

                // No previous node link to it. And it does not link to previous nodes. We link to ROOT.
                if (!linkedNodes.contains(fromNode)) {
                    MentionGraphEdge edge = mentionGraph.getEdge(fromNode.getNodeIndex(), 0);
                    LabelledMentionGraphEdge rootEdge = edge.getLabelledEdge(candidates, NodeKey.rootKey(), fromNode);
                    goldTree.addLabelledEdge(rootEdge, EdgeType.Root);
                }
            }
        }
    }

    private void getMinimumTree(MentionGraph mentionGraph, MentionSubGraph goldTree) {
    }

    private MentionSubGraph systemDecode(MentionGraph mentionGraph, List<MentionCandidate> candidates,
                                         GraphWeightVector weights) {
        MentionSubGraph bestFirstTree = new MentionSubGraph(mentionGraph);

        for (int curr = 1; curr < mentionGraph.numNodes(); curr++) {
            Map<LabelledMentionGraphEdge, Pair<EdgeType, Double>> forwardLinks = new HashMap<>();
            Map<LabelledMentionGraphEdge, Pair<EdgeType, Double>> backwardLinks = new HashMap<>();

            MentionGraphEdge rootEdge = mentionGraph.getEdge(curr, 0);
            rootEdge.extractNodeAgnosticFeatures(candidates);
            LabelledMentionGraphEdge labelledRootEdge = rootEdge.getAllLabelledEdges(candidates).get(0);

            double rootScore = labelledRootEdge.getRootScore(weights);

            for (int ant = 1; ant < curr; ant++) {
                MentionGraphEdge edge = mentionGraph.getEdge(curr, ant);
                edge.extractNodeAgnosticFeatures(candidates);

                for (NodeKey antecedent : edge.getAntKey(candidates)) {
                    for (NodeKey precedent : edge.getPrecKey(candidates)) {
                        LabelledMentionGraphEdge forwardEdge = edge.getLabelledEdge(candidates, antecedent, precedent);
                        LabelledMentionGraphEdge backwardEdge = edge.getLabelledEdge(candidates, precedent, antecedent);

                        Pair<EdgeType, Double> bestForwardScore = forwardEdge.getBestLabelScore(weights);
                        Pair<EdgeType, Double> bestBackwardScore = backwardEdge.getBestLabelScore(weights);

                        double forwardScore = bestForwardScore.getRight();
                        double backwardScore = bestBackwardScore.getRight();

                        if (MathUtils.sureLarger(backwardScore, forwardScore)) {
                            if (MathUtils.sureLarger(backwardScore, rootScore)) {
                                backwardLinks.put(backwardEdge, bestBackwardScore);
                            }
                        } else {
                            if (MathUtils.sureLarger(forwardScore, rootScore)) {
                                forwardLinks.put(forwardEdge, bestForwardScore);
                            }
                        }

                    }
                }
            }

            if (forwardLinks.isEmpty() && backwardLinks.isEmpty()) {
                bestFirstTree.addLabelledEdge(labelledRootEdge, EdgeType.Root);
            }
            forwardLinks.forEach((link, typeScore) -> bestFirstTree.addLabelledEdge(link, typeScore.getLeft()));
            backwardLinks.forEach((link, typeScore) -> bestFirstTree.addLabelledEdge(link, typeScore.getLeft()));
        }
        return bestFirstTree;
    }
}
