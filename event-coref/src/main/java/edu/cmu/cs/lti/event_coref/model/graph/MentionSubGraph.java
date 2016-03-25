package edu.cmu.cs.lti.event_coref.model.graph;

import com.google.common.collect.*;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge.EdgeType;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.GraphFeatureVector;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.javatuples.Pair;
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
    private Map<EdgeType, int[][]> resolvedRelations;

    // Store coreference chains.
    private int[][] corefChains;

    /**
     * Initialize a mention subgraph with all the super graph's node. The edges are empty.
     *
     * @param parentGraph The parent graph of this subgraph.
     */
    public MentionSubGraph(MentionGraph parentGraph) {
        edgeTable = HashBasedTable.create();
        numNodes = parentGraph.numNodes();
        this.parentGraph = parentGraph;
    }

    public MentionSubGraph makeCopy() {
        MentionSubGraph subgraph = new MentionSubGraph(parentGraph);
        subgraph.score = score;
        subgraph.numNodes = numNodes;

        for (Table.Cell<Integer, Integer, SubGraphEdge> cell : edgeTable.cellSet()) {
            subgraph.edgeTable.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
        }

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
    }

    public void addEdge(LabelledMentionGraphEdge labelledMentionGraphEdge, EdgeType newType) {
        edgeTable.put(labelledMentionGraphEdge.getDep(), labelledMentionGraphEdge.getGov(),
                new SubGraphEdge(labelledMentionGraphEdge, newType));
    }

    public SubGraphEdge getEdge(int depIdx, int govIdx) {
        return edgeTable.get(depIdx, govIdx);
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Map<EdgeType, int[][]> getResolvedRelations() {
        return resolvedRelations;
    }

    public int[][] getCorefChains() {
        return corefChains;
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

            // Both of these are unique maps, because we only allow one edge between two nodes, and we require each
            // node must link to something.
            Map<Integer, SubGraphEdge> referenceEdgesFromDep = depEdgeEntry.getValue();
            Map<Integer, SubGraphEdge> thisEdgesFromDep = edgeTable.row(depIdx);

            SubGraphEdge referenceEdge = referenceEdgesFromDep.entrySet().iterator().next().getValue();
            SubGraphEdge thisEdge = thisEdgesFromDep.entrySet().iterator().next().getValue();

            if (referenceEdge.getGov() != thisEdge.getGov()) {
//                logger.debug("Loss because different antecedent : " + thisEdge + " vs " + referenceEdge);
                if (thisEdge.getEdgeType() == EdgeType.Root) {
                    loss += 1.5;
                } else {
                    loss += 1;
                }
            } else {
                if (referenceEdge.getEdgeType() != thisEdge.getEdgeType()) {
                    // NOTE: this should not happen when we only have one type other than root, because types will be
                    // deterministic.
//                    logger.debug("Loss because different type : " + thisEdge + " vs " + referenceEdge);
                    loss += 1;
                } else if (!referenceEdge.getDepKey().getMentionType().equals(thisEdge.getDepKey().getMentionType())) {
                    loss += 0.5;
//                    logger.debug("Loss because different dep key type : " + thisEdge + " vs " + referenceEdge);
                } else if (!referenceEdge.getGovKey().getMentionType().equals(thisEdge.getGovKey().getMentionType())) {
                    loss += 0.5;
//                    logger.debug("Loss because different gov key type : " + thisEdge + " vs " + referenceEdge);
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
        List<Set<Integer>> clusters = new ArrayList<>();

        SetMultimap<EdgeType, Pair<Integer, Integer>> allRelations = HashMultimap.create();
        SetMultimap<EdgeType, Pair<Integer, Integer>> interRelations = HashMultimap.create();

        // Collect stuff from the edgeTable, until the limit
        for (SubGraphEdge edge : edgeTable.values()) {
            EdgeType type = edge.getEdgeType();
            int govNode = edge.getGov();
            int depNode = edge.getDep();

            if (govNode > untilNode || depNode > untilNode) {
                continue;
            }

            if (type.equals(EdgeType.Root)) {
                // If this link to root, start a new cluster.
                Set<Integer> newCluster = new HashSet<>();
                newCluster.add(depNode);
                clusters.add(newCluster);
            } else if (type.equals(EdgeType.Coreference)) {
                // Add the node to one of the existing node.
                for (Set<Integer> cluster : clusters) {
                    if (cluster.contains(govNode)) {
                        cluster.add(depNode);
                        break;
                    }
                }
            } else {
                // For all other relation types, remember them first.
                allRelations.put(type, Pair.with(govNode, depNode));
            }
        }

        ArrayListMultimap<Integer, Integer> group2Clusters = ArrayListMultimap.create();
        TIntIntMap node2ClusterId = new TIntIntHashMap();
        int clusterId = 0;
        for (Set<Integer> cluster : clusters) {
            group2Clusters.putAll(clusterId, cluster);
            for (int clusterNode : cluster) {
                node2ClusterId.put(clusterNode, clusterId);
            }
            clusterId++;
        }

        // Propagate relations within cluster.
        for (Map.Entry<EdgeType, Pair<Integer, Integer>> relation : allRelations.entries()) {
            int govNode = relation.getValue().getValue0();
            int depNode = relation.getValue().getValue1();
            interRelations.put(relation.getKey(),
                    Pair.with(node2ClusterId.get(govNode), node2ClusterId.get(depNode)));
        }

        // Create links for nodes in clusters.
        resolvedRelations = GraphUtils.resolveRelations(interRelations, group2Clusters, numNodes);
        // Create a coreference chain.
        corefChains = GraphUtils.createSortedCorefChains(group2Clusters);
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
        for (Map.Entry<MentionGraphEdge.EdgeType, int[][]> predictEdgesWithType : this.getResolvedRelations()
                .entrySet()) {
            int[][] actualEdges = this.parentGraph.getResolvedRelations().get(predictEdgesWithType.getKey());
            if (!Arrays.deepEquals(predictEdgesWithType.getValue(), actualEdges)) {
                linkMatch = false;
            }
        }
        return corefMatch && linkMatch;
    }

    /**
     * Whether the subgraph matches the gold standard.
     *
     * @return
     */
    public boolean graphMatch(MentionSubGraph referentGraph) {
        return graphMatch(referentGraph, numNodes);
    }

    /**
     * Whether the subgraph matches the gold standard until a certain node.
     *
     * @return
     */
    public boolean graphMatch(MentionSubGraph referentGraph, int until) {
        this.resolveCoreference(until);
        referentGraph.resolveCoreference(until);

        // Variable indicating whether the coreference clusters are matched.
        boolean corefMatch = Arrays.deepEquals(this.getCorefChains(), referentGraph.getCorefChains());

        // Variable indicating whether the other mention links are matched.
        boolean linkMatch = true;
        for (Map.Entry<MentionGraphEdge.EdgeType, int[][]> predictEdgesWithType : this.getResolvedRelations()
                .entrySet()) {
            int[][] actualEdges = referentGraph.getResolvedRelations().get(predictEdgesWithType.getKey());
            if (!Arrays.deepEquals(predictEdgesWithType.getValue(), actualEdges)) {
                linkMatch = false;
            }
        }
        return corefMatch && linkMatch;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SubGraph (non root edges only):\n");

        for (SubGraphEdge edge : edgeTable.values()) {
            if (!edge.getEdgeType().equals(EdgeType.Root)) {
                sb.append("\t").append(edge.toString()).append("\n");
            }
        }

        return sb.toString();
    }
}
