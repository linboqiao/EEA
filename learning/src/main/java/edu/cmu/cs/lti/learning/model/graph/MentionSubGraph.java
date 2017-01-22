package edu.cmu.cs.lti.learning.model.graph;

import com.google.common.base.Joiner;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.model.*;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/3/15
 * Time: 2:16 PM
 * <p>
 * A subgraph defines nodes and edges taken from its parent graph (super graph).
 *
 * @author Zhengzhong Liu
 */
public class MentionSubGraph {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    // Dep, Gov, Edge
    private Table<Integer, Integer, SubGraphEdge> edgeTable;

    private int numNodes;

    // The parent graph of this subgraph.
    private MentionGraph parentGraph;

    // Store edges other than the cluster edges, as adjacent list.
    private Map<EdgeType, Map<NodeKey, List<NodeKey>>> resolvedRelations;

    // Store typed coreference chains, each list represent a chain, where the elements are the
    // <mention id, mention type> representing the mentions in the chain.
    private List<NodeKey>[] typedCorefChains;

    private int totalDistance = 0;

    private ClusterBuilder<Pair<Integer, String>> clusterBuilder;

    /**
     * Initialize a mention subgraph with all the super graph's node. The edges are empty.
     *
     * @param parentGraph The parent graph of this subgraph.
     */
    public MentionSubGraph(MentionGraph parentGraph) {
        edgeTable = HashBasedTable.create();
        numNodes = parentGraph.numNodes();
        this.parentGraph = parentGraph;
        clusterBuilder = new ClusterBuilder<>();
    }

    public MentionSubGraph makeCopy() {
        MentionSubGraph subgraph = new MentionSubGraph(parentGraph);
        subgraph.numNodes = numNodes;
        subgraph.totalDistance = totalDistance;

        for (Table.Cell<Integer, Integer, SubGraphEdge> cell : edgeTable.cellSet()) {
            subgraph.edgeTable.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
        }
        subgraph.clusterBuilder = clusterBuilder.makeCopy();
        return subgraph;
    }

    /**
     * Initialize a mention subgraph with the first {@code numNodes} nodes.
     *
     * @param parentGraph
     * @param numNodes
     */
    public MentionSubGraph(MentionGraph parentGraph, int numNodes) {
        edgeTable = HashBasedTable.create();
        this.numNodes = numNodes;
        this.parentGraph = parentGraph;
        clusterBuilder = new ClusterBuilder<>();
    }

    private SubGraphEdge addEdge(MentionGraphEdge edge) {
        int dep = edge.getDep();
        int gov = edge.getGov();

        SubGraphEdge newEdge;
        if (edgeTable.contains(dep, gov)) {
            newEdge = edgeTable.get(dep, gov);
        } else {
            newEdge = new SubGraphEdge(edge);
            edgeTable.put(dep, gov, newEdge);
        }
        return newEdge;
    }

    public void addLabelledEdge(LabelledMentionGraphEdge labelledMentionGraphEdge, EdgeType newType) {
        int dep = labelledMentionGraphEdge.getDep();
        int gov = labelledMentionGraphEdge.getGov();

        SubGraphEdge newEdge = addEdge(labelledMentionGraphEdge.getHostingEdge());
        newEdge.addLabelledEdge(labelledMentionGraphEdge, newType);

        if (gov != 0) {
            // We consider root to be special, which does not add distance to the tree.
            totalDistance += dep - gov;

            int govNode = newEdge.getGov();
            int depNode = newEdge.getDep();

            NodeKey govKey = labelledMentionGraphEdge.getGovKey();
            NodeKey depKey = labelledMentionGraphEdge.getDepKey();

            if (newType == EdgeType.Coreference) {
                clusterBuilder.addLink(Pair.of(govNode, govKey.getMentionType()),
                        Pair.of(depNode, depKey.getMentionType()));
            }
        }
    }

    public SubGraphEdge getEdge(int depIdx, int govIdx) {
        return edgeTable.get(depIdx, govIdx);
    }

    public int getNumEdges() {
        return edgeTable.size();
    }

    public Map<EdgeType, Map<NodeKey, List<NodeKey>>> getResolvedRelations() {
        return resolvedRelations;
    }

    public List<NodeKey>[] getCorefChains() {
        return typedCorefChains;
    }

    /**
     * Compute the loss with regard to the reference graph.
     *
     * @param referenceGraph The graph to be used as a reference for the loss
     * @return The loss of the graph (almost hamming)
     */
    public double getLoss(MentionSubGraph referenceGraph) {
        if (parentGraph != referenceGraph.parentGraph) {
            throw new IllegalArgumentException("To compute loss, both graph should have the same parent.");
        }

        double loss = 0;

        Set<Pair<Integer, Integer>> checkedEdges = new HashSet<>();
        for (Map.Entry<Integer, Map<Integer, SubGraphEdge>> govners : referenceGraph.edgeTable.rowMap().entrySet()) {
            int depIdx = govners.getKey();
            for (Map.Entry<Integer, SubGraphEdge> govLink : govners.getValue().entrySet()) {
                int govIdx = govLink.getKey();
                SubGraphEdge referentEdge = govLink.getValue();

                checkedEdges.add(Pair.of(depIdx, govIdx));

                if (edgeTable.contains(depIdx, govIdx)) {
                    SubGraphEdge thisEdge = edgeTable.get(depIdx, govIdx);
                    loss += compareEdge(referentEdge, thisEdge);
                } else {
                    loss += referentEdge.numLabelledNonRootLinks();
                }
            }
        }

        for (Table.Cell<Integer, Integer, SubGraphEdge> targetEdge : edgeTable.cellSet()) {
            SubGraphEdge subgraphEdge = targetEdge.getValue();
            if (!checkedEdges.contains(Pair.of(targetEdge.getRowKey(), targetEdge.getColumnKey()))) {
                for (LabelledMentionGraphEdge labelledEdge : subgraphEdge.getAllLabelledEdge()) {
                    if (subgraphEdge.getLabelledType(labelledEdge) == EdgeType.Root) {
                        // Erroneous root attachment.
                        loss += 1.5;
                    } else {
                        loss += 1;
                    }
                }
            }
        }

        return loss;
    }

    private double compareEdge(SubGraphEdge referentEdge, SubGraphEdge targetEdge) {
        double loss = 0;
        for (LabelledMentionGraphEdge referentLabelledEdge : referentEdge.getAllLabelledEdge()) {
            if (targetEdge.getLabelledType(referentLabelledEdge) == null) {
                loss += 1;
            } else if (targetEdge.getLabelledType(referentLabelledEdge) !=
                    referentEdge.getLabelledType(referentLabelledEdge)) {
                loss += 1;
            }
        }
        return loss;
    }

    private double computeLabelledLoss(Collection<SubGraphEdge> referentGovEdges, Collection<SubGraphEdge>
            targetGovEdges) {
        double loss = 0;
        if (referentGovEdges.isEmpty()) {
            // Referent node is not connect to anything.
            for (SubGraphEdge targetGovEdge : targetGovEdges) {
                loss += targetGovEdge.numLabelledNonRootLinks();
            }
        } else if (targetGovEdges.isEmpty()) {
            for (SubGraphEdge referentGovEdge : referentGovEdges) {
                loss += referentGovEdge.numLabelledNonRootLinks();
            }
        } else {
            // Because we have a tree, then the edge collection must be single.
            SubGraphEdge referentEdge = referentGovEdges.iterator().next();
            SubGraphEdge targetGovEdge = targetGovEdges.iterator().next();

            if (referentEdge.getGov() != targetGovEdge.getGov()) {
                // First, compare the gov index.
                for (LabelledMentionGraphEdge labelledTargetEdge : targetGovEdge.getAllLabelledEdge()) {
                    if (targetGovEdge.getLabelledType(labelledTargetEdge) == EdgeType.Root) {
                        loss += 1.5;
                    } else {
                        loss += 1;
                    }
                }
            } else {
                // Next, check type.
                for (LabelledMentionGraphEdge labelledTargetEdge : targetGovEdge.getAllLabelledEdge()) {
                    EdgeType targetType = targetGovEdge.getLabelledType(labelledTargetEdge);
                    EdgeType referentType = referentEdge.getLabelledType(labelledTargetEdge);
                    if (targetType != referentType) {
                        loss += 1;
                    }
                }
            }
        }
        return loss;
    }

    /**
     * Compute the difference of features on this graph against another subgraph.
     *
     * @param otherGraph      The other subgraph.
     * @param classAlphabet   The class labels alphabet.
     * @param featureAlphabet The feature alphabet.
     * @return The delta of the features on these graphs.
     */
    public GraphFeatureVector getDelta(MentionSubGraph otherGraph, ClassAlphabet classAlphabet,
                                       FeatureAlphabet featureAlphabet) {
        if (parentGraph != otherGraph.parentGraph) {
            throw new IllegalArgumentException("To compute delta, both graph should have the same parent.");
        }

        GraphFeatureVector deltaFeatureVector = new GraphFeatureVector(classAlphabet, featureAlphabet);

        TObjectIntMap<Pair<LabelledMentionGraphEdge, String>> edgeFeatureInclusions = new TObjectIntHashMap<>();

        // For edges in this subgraph.
        for (SubGraphEdge edge : edgeTable.values()) {
            for (LabelledMentionGraphEdge labelledEdge : edge.getAllLabelledEdge()) {
                String classNameForFeature = edge.getLabelledType(labelledEdge).name();
//                deltaFeatureVector.extend(edge.getEdgeFeatures(), classNameForFeature);
//                deltaFeatureVector.extend(labelledEdge.getFeatureVector(), classNameForFeature);
                edgeFeatureInclusions.adjustOrPutValue(Pair.of(labelledEdge, classNameForFeature), 1, 1);
            }
        }

        // For edges in the other subgraph.
        for (SubGraphEdge edge : otherGraph.edgeTable.values()) {
            for (LabelledMentionGraphEdge labelledEdge : edge.getAllLabelledEdge()) {
                String classNameForFeature = edge.getLabelledType(labelledEdge).name();
//                deltaFeatureVector.extend(edge.getEdgeFeatures(), classNameForFeature, -1);
//                deltaFeatureVector.extend(labelledEdge.getFeatureVector(), classNameForFeature, -1);
                edgeFeatureInclusions.adjustOrPutValue(Pair.of(labelledEdge, classNameForFeature), -1, -1);
            }
        }

        edgeFeatureInclusions.forEachEntry((edgeWithType, multiplier) -> {
            LabelledMentionGraphEdge edge = edgeWithType.getKey();
            String className = edgeWithType.getValue();

            if (multiplier != 0) {
//                logger.info(String.format("Adding features with multiplier %d  for type %s from:", multiplier,
//                        className));
//                logger.info(edge.toString());
                deltaFeatureVector.extend(edge.getFeatureVector(), className, multiplier);
                deltaFeatureVector.extend(edge.getHostingEdge().getNodeAgnosticFeatures(), className, multiplier);
            }
            return true;
        });

        return deltaFeatureVector;
    }


    /**
     * Convert the tree to transitive and equivalence resolved graph
     */
    public void resolveGraph() {
        resolveGraph(numNodes);
    }

    /**
     * Convert the tree to transitive and equivalence resolved graph
     */
    public void resolveGraph(int untilNode) {
        SetMultimap<Integer, NodeKey> keyClusters = HashMultimap.create();
        SetMultimap<EdgeType, Pair<NodeKey, NodeKey>> keyRelations = HashMultimap.create();

        // Collect stuff from the edgeTable, until the limit.
        int clusterId = 0;

        List<SubGraphEdge> allEdges = new ArrayList<>(edgeTable.values());

        allEdges.sort((o1, o2) -> new CompareToBuilder().append(o1.getDep(), o2.getDep())
                .append(o1.getGov(), o2.getGov()).build());

        if (numNodes > 1){
            // First add first node to ROOT.
            clusterId ++;
        }

        for (SubGraphEdge edge : allEdges) {
            int govNode = edge.getGov();
            int depNode = edge.getDep();

            for (LabelledMentionGraphEdge labelledEdge : edge.getAllLabelledEdge()) {
                EdgeType type = edge.getLabelledType(labelledEdge);

                NodeKey depKey = labelledEdge.getDepKey();
                NodeKey govKey = labelledEdge.getGovKey();

                if (govNode > untilNode || depNode > untilNode) {
                    // Don't break here since we are not sure that the edges are sorted.
                    continue;
                }

                if (type.equals(EdgeType.Root)) {
                    // If this link to root, start a new cluster.
                    keyClusters.put(clusterId, depKey);
                    clusterId++;
                } else if (type.equals(EdgeType.Coreference)) {
                    // Add the node to one of the existing cluster.
                    for (Integer eventId : keyClusters.keys()) {
                        Set<NodeKey> nodeCluster = keyClusters.get(eventId);
                        if (nodeCluster.contains(govKey)) {
                            nodeCluster.add(depKey);
                            break;
                        }
                    }
                } else {
                    keyRelations.put(type, Pair.of(govKey, depKey));
                }
            }
        }

        TObjectIntMap<NodeKey> node2ClusterId = new TObjectIntHashMap<>();
        for (Map.Entry<Integer, Collection<NodeKey>> cluster : keyClusters.asMap().entrySet()) {
            for (NodeKey element : cluster.getValue()) {
                node2ClusterId.put(element, cluster.getKey());
            }
        }

        SetMultimap<EdgeType, Pair<Integer, Integer>> interRelations = HashMultimap.create();
        // Propagate relations within cluster.
        for (Map.Entry<EdgeType, Pair<NodeKey, NodeKey>> relation :
                keyRelations.entries()) {
            NodeKey govNode = relation.getValue().getLeft();
            NodeKey depNode = relation.getValue().getRight();
            interRelations.put(relation.getKey(),
                    Pair.of(node2ClusterId.get(govNode), node2ClusterId.get(depNode)));
        }

        // Create links for nodes in clusters.
        resolvedRelations = GraphUtils.resolveRelations(interRelations, keyClusters);
        // Create a coreference chain.
        typedCorefChains = GraphUtils.createSortedCorefChains(keyClusters);
    }

    /**
     * Whether the subgraph matches the gold standard.
     *
     * @return
     */
    public boolean graphMatch() {
        return graphMatch(numNodes);
    }

    /**
     * Whether the subgraph matches the gold standard graph until a particular node.
     *
     * @return
     */
    public boolean graphMatch(int until) {
        this.resolveGraph(until);

        // Variable indicating whether the coreference clusters are matched.
        boolean corefMatch = Arrays.deepEquals(this.getCorefChains(), this.parentGraph.getNodeCorefChains());

        // Variable indicating whether the other mention links are matched.
        boolean linkMatch = this.getResolvedRelations().equals(this.parentGraph.getResolvedRelations());

        return corefMatch && linkMatch;
    }

    /**
     * Whether the subgraph matches another given subgraph.
     *
     * @return
     */
    public boolean graphMatch(MentionSubGraph referentGraph) {
        return this.clusterBuilder.match(referentGraph.clusterBuilder);
    }

    public String fullTree() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SubGraph of distance %d:\n", totalDistance));

        for (SubGraphEdge edge : edgeTable.values()) {
            sb.append("\t").append(edge.toString()).append("\n");
        }

        sb.append("Cluster view:\n");
        for (TreeSet<Pair<Integer, String>> elements : clusterBuilder.getClusters()) {
            sb.append(Joiner.on("\t").join(elements)).append("\n");
        }

        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SubGraph (non root edges only) of distance %d:\n", totalDistance));

        for (SubGraphEdge edge : edgeTable.values()) {
            if (edge.hasUnlabelledType() || edge.numLabelledNonRootLinks() > 0) {
                sb.append("\t").append(edge.toString()).append("\n");
            }
        }

        sb.append("Cluster view:\n");
        for (TreeSet<Pair<Integer, String>> elements : clusterBuilder.getClusters()) {
            sb.append(Joiner.on("\t").join(elements)).append("\n");
        }

        return sb.toString();
    }

    public int getTotalDistance() {
        return totalDistance;
    }

    /**
     * Update the weights by the difference of the predicted tree and the gold latent tree using Passive-Aggressive
     * algorithm.
     *
     * @param referentTree The gold latent tree.
     */
    public double paUpdate(MentionSubGraph referentTree, GraphWeightVector weights) {
        GraphFeatureVector delta = referentTree.getDelta(this, weights.getClassAlphabet(),
                weights.getFeatureAlphabet());

        double loss = this.getLoss(referentTree);
        double l2Sqaure = delta.getFeatureSquare();

//        logger.info("Updating weights by: ");
//        logger.info(delta.readableNodeVector());

        if (l2Sqaure != 0) {
            double tau = loss / l2Sqaure;
            weights.updateWeightsBy(delta, tau);
            weights.updateAverageWeights();
        }
        return loss;
    }
}
