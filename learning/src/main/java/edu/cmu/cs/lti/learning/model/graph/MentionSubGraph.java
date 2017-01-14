package edu.cmu.cs.lti.learning.model.graph;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.utils.DebugUtils;
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
    private Map<EdgeType, ListMultimap<Integer, Integer>> resolvedRelations;

    // Store typed coreference chains, each list represent a chain, where the elements are the
    // <mention id, mention type> representing the mentions in the chain.
    private List<Pair<Integer, String>>[] typedCorefChains;

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

    public void addUnlabelledEdge(List<MentionCandidate> candidates, MentionGraphEdge edge,
                                  EdgeType newType, EdgeDirection direction) {
        SubGraphEdge newEdge = addEdge(edge);
        newEdge.setUnlabelledType(newType, direction);

        for (LabelledMentionGraphEdge labelledEdge : edge.getAllLabelledEdges(candidates,
                direction == EdgeDirection.Backword)) {
            newEdge.addLabelledEdge(labelledEdge, newType);
        }
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

            clusterBuilder.addLink(Pair.of(govNode, govKey.getMentionType()),
                    Pair.of(depNode, depKey.getMentionType()));
        }
    }

    public SubGraphEdge getEdge(int depIdx, int govIdx) {
        return edgeTable.get(depIdx, govIdx);
    }

    public int getNumEdges() {
        return edgeTable.size();
    }

    public Map<EdgeType, ListMultimap<Integer, Integer>> getResolvedRelations() {
        return resolvedRelations;
    }

    public List<Pair<Integer, String>>[] getCorefChains() {
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

        for (Map.Entry<Integer, Map<Integer, SubGraphEdge>> govners : referenceGraph.edgeTable.rowMap().entrySet()) {
            int depIdx = govners.getKey();

            Collection<SubGraphEdge> referentGovners = govners.getValue().values();
            Collection<SubGraphEdge> targetGovners = edgeTable.row(depIdx).values();

            double unlabelledLoss = computeUnlabelledLoss(referentGovners, targetGovners);
            double labelledLoss = computeLabelledLoss(referentGovners, targetGovners);

            loss += unlabelledLoss;
            loss += labelledLoss;
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
                    if (targetGovEdge.getLabelledType(labelledTargetEdge) == EdgeType.Coref_Root) {
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

    private double computeUnlabelledLoss(Collection<SubGraphEdge> referentGovEdges,
                                         Collection<SubGraphEdge> targetGovEdges) {
        double loss = 0;
        if (referentGovEdges.isEmpty()) {
            // Referent node is not connect to anything.
            for (SubGraphEdge targetGovEdge : targetGovEdges) {
                if (targetGovEdge.hasUnlabelledType()) {
                    loss++;
                }
            }
        } else if (targetGovEdges.isEmpty()) {
            for (SubGraphEdge referentGovEdge : referentGovEdges) {
                if (referentGovEdge.hasUnlabelledType()) {
                    loss++;
                }
            }
        } else {
            // Because we have a tree, then the edge collection must be single.
            SubGraphEdge referentEdge = referentGovEdges.iterator().next();
            SubGraphEdge targetGovEdge = targetGovEdges.iterator().next();

            if (referentEdge.getGov() != targetGovEdge.getGov()) {
                // First, compare the gov index.
                if (targetGovEdge.getUnlabelledType() == EdgeType.Coref_Root) {
                    loss += 1.5;
                } else {
                    loss += 1;
                }
            } else {
                // Next, check type.
                if (referentEdge.getUnlabelledType() != targetGovEdge.getUnlabelledType()) {
                    loss += 1;
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

        // For edges in this subgraph.
        for (SubGraphEdge edge : edgeTable.values()) {
            for (LabelledMentionGraphEdge labelledEdge : edge.getAllLabelledEdge()) {
                logger.info("Adding features from:");
                logger.info(labelledEdge.toString());
                String classNameForFeature = edge.hasUnlabelledType() ?
                        edge.getUnlabelledType().name() : edge.getLabelledType(labelledEdge).name();

                // TODO we saw nulls, probably due to labelled edges are directional
                if (edge.getEdgeFeatures() == null){
                    logger.info("Features for " + labelledEdge + " is null.");
                    DebugUtils.pause();
                }

                deltaFeatureVector.extend(edge.getEdgeFeatures(), classNameForFeature);
                deltaFeatureVector.extend(labelledEdge.getFeatureVector(), classNameForFeature);
            }
        }

        // For edges in the other subgraph.
        for (SubGraphEdge edge : otherGraph.edgeTable.values()) {
            for (LabelledMentionGraphEdge labelledEdge : edge.getAllLabelledEdge()) {
                String classNameForFeature = edge.hasUnlabelledType() ?
                        edge.getUnlabelledType().name() : edge.getLabelledType(labelledEdge).name();
                deltaFeatureVector.extend(edge.getEdgeFeatures(), classNameForFeature);
                deltaFeatureVector.extend(labelledEdge.getFeatureVector().negation(), classNameForFeature);
            }
        }
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
        SetMultimap<EdgeType, Pair<Integer, Integer>> allRelations = HashMultimap.create();
        SetMultimap<EdgeType, Pair<Integer, Integer>> interRelations = HashMultimap.create();

        SetMultimap<Integer, Pair<Integer, String>> indexedTypedClusters = HashMultimap.create();
        SetMultimap<Integer, Integer> relaxedClusters = HashMultimap.create();

        SetMultimap<EdgeType, Pair<Pair<Integer, String>, Pair<Integer, String>>> allTypedRelations =
                HashMultimap.create();

        // Collect stuff from the edgeTable, until the limit.
        int typedClusterId = 0;

        List<SubGraphEdge> allEdges = new ArrayList<>(edgeTable.values());

        allEdges.sort((o1, o2) -> new CompareToBuilder().append(o1.getDep(), o2.getDep())
                .append(o1.getGov(), o2.getGov()).build());

        for (SubGraphEdge edge : allEdges) {
            int govNode = edge.getGov();
            int depNode = edge.getDep();

            for (LabelledMentionGraphEdge labelledEdge : edge.getAllLabelledEdge()) {
                EdgeType type = edge.getLabelledType(labelledEdge);

                NodeKey depKey = labelledEdge.getDepKey();
                NodeKey govKey = labelledEdge.getGovKey();

                Pair<Integer, String> typedGovNode = Pair.of(govNode, govKey.getMentionType());

                Pair<Integer, String> typedDepNode = Pair.of(depNode, depKey.getMentionType());

                int govCandidateId = govKey.getCandidateIndex();
                int depCandidateId = depKey.getCandidateIndex();

                if (govNode > untilNode || depNode > untilNode) {
                    // Don't break here since we are not sure that the edges are sorted.
                    continue;
                }

                if (type.equals(EdgeType.Coref_Root)) {
                    // If this link to root, start a new cluster.
                    indexedTypedClusters.put(typedClusterId, typedDepNode);
                    relaxedClusters.put(typedClusterId, depKey.getCandidateIndex());
                } else if (type.equals(EdgeType.Coreference)) {
                    // Add the node to one of the existing cluster.
                    for (Integer eventId : indexedTypedClusters.keySet()) {
                        Set<Pair<Integer, String>> typedCluster = indexedTypedClusters.get(eventId);
                        if (typedCluster.contains(typedGovNode)) {
                            typedCluster.add(typedDepNode);
                            break;
                        }
                    }

                    for (Integer eventId : relaxedClusters.keySet()) {
                        Set<Integer> cluster = relaxedClusters.get(eventId);
                        if (cluster.contains(govCandidateId)) {
                            cluster.add(depCandidateId);
                            break;
                        }
                    }
                } else {
                    allRelations.put(type, Pair.of(govNode, depNode));
                    allTypedRelations.put(type, Pair.of(typedGovNode, typedDepNode));
                }
            }
        }

        TObjectIntMap<Pair<Integer, String>> node2ClusterId = new TObjectIntHashMap<>();

        for (Map.Entry<Integer, Collection<Pair<Integer, String>>> cluster : indexedTypedClusters.asMap().entrySet()) {
            for (Pair<Integer, String> element : cluster.getValue()) {
                node2ClusterId.put(element, cluster.getKey());
            }
        }

        // Propagate relations within cluster.
        for (Map.Entry<EdgeType, Pair<Integer, Integer>> relation : allRelations.entries()) {
            int govNode = relation.getValue().getLeft();
            int depNode = relation.getValue().getRight();
            interRelations.put(relation.getKey(),
                    Pair.of(node2ClusterId.get(govNode), node2ClusterId.get(depNode)));
        }

        // Create links for nodes in clusters.
        resolvedRelations = GraphUtils.resolveRelations(interRelations, relaxedClusters);
        // Create a coreference chain.
        typedCorefChains = GraphUtils.createSortedCorefChains(indexedTypedClusters);
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
        boolean linkMatch = true;
        for (Map.Entry<EdgeType, ListMultimap<Integer, Integer>> predictEdgesWithType :
                this.getResolvedRelations().entrySet()) {
            ListMultimap<Integer, Integer> actualEdges =
                    this.parentGraph.getResolvedRelations().get(predictEdgesWithType.getKey());
            linkMatch = predictEdgesWithType.getValue().equals(actualEdges);
        }
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

        logger.info("Updating weights by: ");
        logger.info(delta.readableNodeVector());

        if (l2Sqaure != 0) {
            double tau = loss / l2Sqaure;
            weights.updateWeightsBy(delta, tau);
            weights.updateAverageWeights();
        }
        return loss;
    }
}
