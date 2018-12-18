package edu.cmu.cs.lti.event_coref.decoding;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.decoding.IncrementalLinkBuilder;
import edu.cmu.cs.lti.learning.model.graph.*;
import edu.cmu.cs.lti.utils.MathUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
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

    private List<EdgeType> typeSet;

    public static boolean debug = false;

    public static void startDebug() {
        debug = true;
    }

    public static void stopDebug() {
        debug = false;
    }

    public BFAfterLatentTreeDecoder() {
        this(0);
    }

    public BFAfterLatentTreeDecoder(int trainingStrategy) {
        logger.info("After link gold standard decoding strategy is " + trainingStrategy);
        this.trainingStrategy = trainingStrategy;

        typeSet = new ArrayList<>();
        typeSet.add(EdgeType.After);
        typeSet.add(EdgeType.Subevent);
    }

    @Override
    public MentionSubGraph decode(MentionGraph mentionGraph, List<MentionCandidate> candidates,
                                  GraphWeightVector weights, boolean getGoldTree) {
        if (getGoldTree) {
            return getGoldTree(mentionGraph, candidates, weights);
        } else {
            SetMultimap<Integer, NodeKey> clusters = mentionGraph.getEvent2NodeKeys();
            TObjectIntMap<NodeKey> node2Cluster = new TObjectIntHashMap<>();

            for (Map.Entry<Integer, Collection<NodeKey>> event2Cluster : clusters.asMap().entrySet()) {
                for (NodeKey nodeKey : event2Cluster.getValue()) {
                    node2Cluster.put(nodeKey, event2Cluster.getKey());
                }
            }

            return systemDecode(mentionGraph, candidates, weights, node2Cluster);
        }
    }

    private MentionSubGraph getGoldTree(MentionGraph mentionGraph, List<MentionCandidate> candidates,
                                        GraphWeightVector weights) {
        MentionSubGraph goldTree = new MentionSubGraph(mentionGraph);
        if (trainingStrategy == 0) {
            getMinimumTree(mentionGraph, goldTree, candidates, weights);
        } else if (trainingStrategy == 1) {
            getFullTree(mentionGraph, goldTree, candidates);
        }
        return goldTree;
    }

    private void getFullTree(MentionGraph mentionGraph, MentionSubGraph goldTree, List<MentionCandidate> candidates) {

        for (EdgeType targetType : typeSet) {
            if (!mentionGraph.getReducedEventRelations().containsKey(targetType)) {
                continue;
            }
            Set<Pair<NodeKey, NodeKey>> allLinks = mentionGraph.getResolvedRelations().get(targetType);

            Set<NodeKey> linkedNodes = new HashSet<>();

            for (Pair<NodeKey, NodeKey> link : allLinks) {
                NodeKey fromNode = link.getKey();
                NodeKey toNode = link.getValue();
                MentionGraphEdge edge = mentionGraph.getEdge(toNode.getNodeIndex(), fromNode.getNodeIndex());
                edge.extractNodeAgnosticFeatures(candidates);
                LabelledMentionGraphEdge labelledEdge = edge.getLabelledEdge(candidates, fromNode, toNode);
                goldTree.addLabelledEdge(labelledEdge, targetType);

                // The latter node is considered as linked to the former one.
                linkedNodes.add(fromNode.compareTo(toNode) > 0 ? fromNode : toNode);
            }

            for (MentionCandidate candidate : candidates) {
                for (NodeKey node : candidate.asKey()) {
                    // No previous node link to it. And it does not link to previous nodes. We link to ROOT.
                    if (!linkedNodes.contains(node)) {
                        MentionGraphEdge edge = mentionGraph.getEdge(0, node.getNodeIndex());
                        LabelledMentionGraphEdge rootEdge = edge.getLabelledEdge(candidates,
                                NodeKey.getRootKey(targetType), node);
                        goldTree.addLabelledEdge(rootEdge, EdgeType.Root);
                    }
                }
            }
        }
    }

    private void getMinimumTree(MentionGraph mentionGraph, MentionSubGraph goldTree, List<MentionCandidate> candidates,
                                GraphWeightVector weights) {
        EdgeType[] targetTypes = {EdgeType.After, EdgeType.Subevent};

        for (EdgeType targetType : targetTypes) {
            Set<NodeKey> linkedNodes = new HashSet<>();

            Map<Integer, List<Integer>> reducedEventGraph = mentionGraph.getReducedEventRelations().get(targetType);
            if (reducedEventGraph != null) {
                for (Map.Entry<Integer, List<Integer>> adjacentList : reducedEventGraph.entrySet()) {
                    int from = adjacentList.getKey();
                    for (Integer to : adjacentList.getValue()) {
                        // Take the best pair of mentions from two events.
                        Pair<Double, LabelledMentionGraphEdge> bestLink = getBestMentionPair(mentionGraph, from, to,
                                candidates, weights, targetType);

                        NodeKey fromKey = bestLink.getValue().getGovKey();
                        NodeKey toKey = bestLink.getValue().getDepKey();

                        linkedNodes.add(fromKey.compareTo(toKey) > 0 ? fromKey : toKey);
                        goldTree.addLabelledEdge(bestLink.getValue(), targetType);
                    }
                }
            }

            // Now link all other links to root.
            for (MentionCandidate candidate : candidates) {
                for (NodeKey nodeKey : candidate.asKey()) {
                    if (!linkedNodes.contains(nodeKey)) {
                        LabelledMentionGraphEdge rootEdge =
                                mentionGraph.getLabelledEdge(candidates, NodeKey.getRootKey(targetType), nodeKey);
                        goldTree.addLabelledEdge(rootEdge, EdgeType.Root);
                    }
                }
            }
        }
    }

    private Pair<Double, LabelledMentionGraphEdge> getBestMentionPair(MentionGraph mentionGraph, int from, int to,
                                                                      List<MentionCandidate> candidates,
                                                                      GraphWeightVector weights, EdgeType type) {

        double bestScore = Double.NEGATIVE_INFINITY;
        LabelledMentionGraphEdge bestEdge = null;

        for (NodeKey fromKey : mentionGraph.getEvent2NodeKeys().get(from)) {
            for (NodeKey toKey : mentionGraph.getEvent2NodeKeys().get(to)) {
                MentionGraphEdge edge = mentionGraph.getEdge(fromKey.getNodeIndex(), toKey.getNodeIndex());
                edge.extractNodeAgnosticFeatures(candidates);
                LabelledMentionGraphEdge labelledEdge = edge.getLabelledEdge(candidates, fromKey, toKey);
                double score = labelledEdge.scoreEdge(type, weights);

                if (score > bestScore) {
                    bestScore = score;
                    bestEdge = labelledEdge;
                }
            }
        }

        return Pair.of(bestScore, bestEdge);
    }

    private MentionSubGraph systemDecode(MentionGraph mentionGraph, List<MentionCandidate> candidates,
                                         GraphWeightVector weights,
                                         TObjectIntMap<NodeKey> node2Cluster) {
        MentionSubGraph bestFirstTree = new MentionSubGraph(mentionGraph);

        IncrementalLinkBuilder<NodeKey> linkBuilder = new IncrementalLinkBuilder<>(node2Cluster);

        for (int curr = 1; curr < mentionGraph.numNodes(); curr++) {
            MentionGraphEdge rootEdge = mentionGraph.getEdge(0, curr);
            rootEdge.extractNodeAgnosticFeatures(candidates);

            for (NodeKey descendant : candidates.get(MentionGraph.getCandidateIndex(curr)).asKey()) {
                Map<EdgeType, LabelledMentionGraphEdge> rootEdgeByType = new HashMap<>();
                Map<EdgeType, Double> rootScoreByType = new HashMap<>();

                for (EdgeType edgeType : typeSet) {
                    LabelledMentionGraphEdge labelledRootEdge = rootEdge.getLabelledEdge(candidates,
                            NodeKey.getRootKey(edgeType), descendant);
                    rootEdgeByType.put(edgeType, labelledRootEdge);
                    double rootScore = labelledRootEdge.getRootScore(weights);
                    rootScoreByType.put(edgeType, rootScore);
                }

                for (int ant = 1; ant < curr; ant++) {
                    MentionGraphEdge edge = mentionGraph.getEdge(ant, curr);
                    edge.extractNodeAgnosticFeatures(candidates);

                    for (NodeKey antecedent : edge.getAntKey(candidates)) {
                        LabelledMentionGraphEdge forwardEdge = edge.getLabelledEdge(candidates, antecedent, descendant);
                        LabelledMentionGraphEdge backwardEdge = edge.getLabelledEdge(candidates, descendant,
                                antecedent);

                        for (EdgeType edgeType : typeSet) {
                            boolean forwardOK = linkBuilder.checkLink(antecedent, descendant, edgeType);
                            boolean backwardOK = linkBuilder.checkLink(descendant, antecedent, edgeType);

                            double forwardScore = forwardEdge.scoreEdge(edgeType, weights);
                            double backwardScore = backwardEdge.scoreEdge(edgeType, weights);
                            double rootScore = rootScoreByType.get(edgeType);

                            boolean tryForward = false;
                            boolean tryBackward = false;

                            if (backwardOK) {
                                if (!forwardOK) {
                                    // Only backward OK, we have no choice.
                                    tryBackward = true;
                                } else {
                                    // Both backward and forward do not conflict, pick the higher score.
                                    if (MathUtils.sureLarger(backwardScore, forwardScore)) {
                                        tryBackward = true;
                                    } else {
                                        tryForward = true;
                                    }
                                }
                            } else {
                                if (forwardOK) {
                                    // Only forward OK, we have no choice.
                                    tryForward = true;
                                }
                                // The last branch means both are not OK, we do not try anything.
                            }

                            if (tryBackward) {
                                if (MathUtils.sureLarger(backwardScore, rootScore)) {
                                    linkBuilder.addLink(descendant, antecedent, edgeType, backwardScore);
                                }
                            } else if (tryForward) {
                                if (MathUtils.sureLarger(forwardScore, rootScore)) {
                                    linkBuilder.addLink(antecedent, descendant, edgeType, forwardScore);
                                }
                            }
                        }
                    }
                }

                for (EdgeType edgeType : typeSet) {
                    if (!linkBuilder.linkToPrevious(descendant, edgeType)) {
                        bestFirstTree.addLabelledEdge(rootEdgeByType.get(edgeType), EdgeType.Root);
                    }
                }
            }

            for (Table.Cell<NodeKey, NodeKey, EdgeType> link : linkBuilder.getLinks().cellSet()) {
                LabelledMentionGraphEdge edge = mentionGraph
                        .getLabelledEdge(candidates, link.getRowKey(), link.getColumnKey());
                bestFirstTree.addLabelledEdge(edge, link.getValue());
            }


        }
        return bestFirstTree;
    }
}
