package edu.cmu.cs.lti.learning.model.graph;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import edu.cmu.cs.lti.learning.model.*;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
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

    private Table<Integer, Integer, SubGraphEdge> edgeTable;

    private int numNodes;

    // The subgraph score under current feature.
    private double score;

    // The parent graph of this subgraph.
    private MentionGraph parentGraph;

    // Store edges other than the cluster edges, as adjacent list.
    private Map<EdgeType, ListMultimap<Pair<Integer, String>, Pair<Integer, String>>> resolvedRelations;

    // Store coreference chains.
//    private int[][] corefChains;

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
        subgraph.score = score;
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

    public void addEdge(LabelledMentionGraphEdge labelledMentionGraphEdge, EdgeType newType) {
        int dep = labelledMentionGraphEdge.getDep();
        int gov = labelledMentionGraphEdge.getGov();
        SubGraphEdge newEdge = new SubGraphEdge(labelledMentionGraphEdge, newType);
        edgeTable.put(dep, gov, newEdge);
        if (gov != 0) {
            // We consider root to be special, which does not add distance to the tree.
            totalDistance += dep - gov;

            int govNode = newEdge.getGov();
            int depNode = newEdge.getDep();

            NodeKey govKey = newEdge.getGovKey();
            NodeKey depKey = newEdge.getDepKey();

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

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Map<EdgeType, ListMultimap<Pair<Integer, String>, Pair<Integer, String>>> getResolvedRelations() {
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

        for (Map.Entry<Integer, Map<Integer, SubGraphEdge>> depEdgeEntry : referenceGraph.edgeTable.rowMap()
                .entrySet()) {
            int depIdx = depEdgeEntry.getKey();

            // Both of these are unique maps, because we only allow one edge between two nodes.
            Map<Integer, SubGraphEdge> referenceEdgesFromDep = depEdgeEntry.getValue();
            Map<Integer, SubGraphEdge> thisEdgesFromDep = edgeTable.row(depIdx);


            // Normally we require each node must link to something, but for non-mentions, it waste too much
            // computation, so we allow some edges to be empty.
            if (referenceEdgesFromDep.isEmpty()) {
                for (Map.Entry<Integer, SubGraphEdge> thisEdge : thisEdgesFromDep.entrySet()) {
                    if (thisEdge.getValue().getEdgeType() != EdgeType.Root) {
                        loss += 1;
                    }
                }
            } else if (thisEdgesFromDep.isEmpty()) {
                for (Map.Entry<Integer, SubGraphEdge> referenceEdge : referenceEdgesFromDep.entrySet()) {
                    if (referenceEdge.getValue().getEdgeType() != EdgeType.Root) {
                        loss += 1;
                    }
                }
            } else {
                SubGraphEdge referenceEdge = referenceEdgesFromDep.entrySet().iterator().next().getValue();
                SubGraphEdge thisEdge = thisEdgesFromDep.entrySet().iterator().next().getValue();

                if (referenceEdge.getGov() != thisEdge.getGov()) {
//                logger.info("Loss because different antecedent : " + thisEdge + " vs " + referenceEdge);
                    if (thisEdge.getEdgeType() == EdgeType.Root) {
                        loss += 1.5;
                    } else {
                        loss += 1;
                    }
                } else {
                    if (referenceEdge.getEdgeType() != thisEdge.getEdgeType()) {
                        // NOTE: this should not happen when we only have one type other than root, because types
                        // will be

                        // deterministic.
//                    logger.info("Loss because different type : " + thisEdge + " vs " + referenceEdge);
                        loss += 1;
                    } else if (!referenceEdge.getDepKey().getMentionType().equals(thisEdge.getDepKey().getMentionType
                            ())) {
                        loss += 0.5;
//                    logger.info("Loss because different dep key type : " + thisEdge + " vs " + referenceEdge);
                    } else if (!referenceEdge.getGovKey().getMentionType().equals(thisEdge.getGovKey().getMentionType
                            ())) {
                        loss += 0.5;
//                    logger.info("Loss because different gov key type : " + thisEdge + " vs " + referenceEdge);
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

        // For edges in this subgraph.
        for (SubGraphEdge edge : edgeTable.values()) {
            deltaFeatureVector.extend(edge.getEdgeFeatures(), edge.getEdgeType().name());
        }

        // For edges in the other subgraph.
        for (SubGraphEdge edge : otherGraph.edgeTable.values()) {
            deltaFeatureVector.extend(edge.getEdgeFeatures().negation(), edge.getEdgeType().name());
        }
        return deltaFeatureVector;
    }

    /**
     * Convert the tree to transitive and equivalence resolved graph
     */
    public void resolveCoreference() {
        resolveCoreference(numNodes);
    }

    /**
     * Convert the tree to transitive and equivalence resolved graph
     */
    public void resolveCoreference(int untilNode) {
//        List<Set<Integer>> clusters = new ArrayList<>();
        SetMultimap<EdgeType, Pair<Integer, Integer>> allRelations = HashMultimap.create();
        SetMultimap<EdgeType, Pair<Integer, Integer>> interRelations = HashMultimap.create();

//        List<Set<Pair<Integer, String>>> typedClusters = new ArrayList<>();
        SetMultimap<Integer, Pair<Integer, String>> indexedTypedClusters = HashMultimap.create();
        SetMultimap<EdgeType, Pair<Pair<Integer, String>, Pair<Integer, String>>> allTypedRelations =
                HashMultimap.create();

        // Collect stuff from the edgeTable, until the limit.
        int typedClusterId = 0;
        for (SubGraphEdge edge : edgeTable.values()) {
            EdgeType type = edge.getEdgeType();

            int govNode = edge.getGov();
            int depNode = edge.getDep();

            NodeKey depKey = edge.getDepKey();
            NodeKey govKey = edge.getGovKey();

            Pair<Integer, String> typedGovNode = Pair.of(govNode, govKey.getMentionType());

            Pair<Integer, String> typedDepNode = Pair.of(depNode, depKey.getMentionType());

            if (govNode > untilNode || depNode > untilNode) {
                // Don't break here since we are not sure that the edges are sorted.
                continue;
            }

            if (type.equals(EdgeType.Root)) {
                // If this link to root, start a new cluster.
                indexedTypedClusters.put(typedClusterId, typedDepNode);
                typedClusterId++;
            } else if (type.equals(EdgeType.Coreference)) {
                // Add the node to one of the existing cluster.
                for (Integer eventId : indexedTypedClusters.keySet()) {
                    Set<Pair<Integer, String>> typedCluster = indexedTypedClusters.get(eventId);
                    if (typedCluster.contains(typedGovNode)) {
                        typedCluster.add(typedDepNode);
                        break;
                    }
                }
            } else {
                // For all other relation types, simply record them first.
                logger.info(String.format("Adding relation %s between %s and %s", type, govNode, depNode));
                allRelations.put(type, Pair.of(govNode, depNode));
                allTypedRelations.put(type, Pair.of(typedGovNode, typedDepNode));
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
        resolvedRelations = GraphUtils.resolveRelations(interRelations, indexedTypedClusters);
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
        this.resolveCoreference(until);

        // Variable indicating whether the coreference clusters are matched.
        boolean corefMatch = Arrays.deepEquals(this.getCorefChains(), this.parentGraph.getNodeCorefChains());

        // Variable indicating whether the other mention links are matched.
        boolean linkMatch = true;
        for (Map.Entry<EdgeType, ListMultimap<Pair<Integer, String>, Pair<Integer, String>>> predictEdgesWithType :
                this.getResolvedRelations().entrySet()) {
            ListMultimap<Pair<Integer, String>, Pair<Integer, String>> actualEdges =
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
//        return graphMatch(referentGraph, numNodes);
    }

//    /**
//     * Whether the subgraph matches the gold standard until a certain node.
//     *
//     * @return
//     */
//    public boolean graphMatch(MentionSubGraph referentGraph, int until) {
//        this.resolveCoreference(until);
//        referentGraph.resolveCoreference(until);
//
//        // Variable indicating whether the coreference clusters are matched.
//        boolean corefMatch = Arrays.deepEquals(this.getCorefChains(), referentGraph.getCorefChains());
//
//        // Variable indicating whether the other mention links are matched.
//        boolean linkMatch = true;
//        for (Map.Entry<EdgeType, ListMultimap<Pair<Integer, String>, Pair<Integer, String>>> predictEdgesWithType :
//                this.getResolvedRelations().entrySet()) {
//            ListMultimap<Pair<Integer, String>, Pair<Integer, String>> actualEdges = referentGraph
//                    .getResolvedRelations().get(predictEdgesWithType.getKey());
//            ListMultimap<Pair<Integer, String>, Pair<Integer, String>> predictedEdges = predictEdgesWithType
// .getValue();
//
//            if (!actualEdges.equals(predictedEdges)) {
//                linkMatch = false;
//            }
//        }
//        return corefMatch && linkMatch;
//    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SubGraph (non root edges only) of distance %d:\n", totalDistance));

        for (SubGraphEdge edge : edgeTable.values()) {
            if (!edge.getEdgeType().equals(EdgeType.Root)) {
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
}
