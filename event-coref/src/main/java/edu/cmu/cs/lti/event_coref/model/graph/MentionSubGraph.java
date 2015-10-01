package edu.cmu.cs.lti.event_coref.model.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge.EdgeType;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.GraphFeatureVector;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.javatuples.Pair;

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
//    //The edge index in the list, indexed first by the dep node, then the gov node index.
//    private Integer[][] edgeIndexes;
//
//    // Store the edges of this graph.
//    private List<SubGraphEdge> edges;

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

    public void addEdge(MentionGraphEdge mentionGraphEdge, EdgeType newType) {
        edgeTable.put(mentionGraphEdge.depIdx, mentionGraphEdge.govIdx, new SubGraphEdge(mentionGraphEdge, newType));
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
        double loss = 0;
        double prec = 0;
        double recall = 0;

        int nonRootCorrect = 0;

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
//                System.out.println("Loss because different antecedent : " + thisEdge + " vs " + referenceEdge);
                if (thisEdge.getEdgeType() == EdgeType.Root){
                    loss += 1.5;
                }else{
                    loss += 1;
                }
            } else {
                if (referenceEdge.getEdgeType() != thisEdge.getEdgeType()) {
                    // NOTE: this should not happen when we only have one type other than root, because types will be
                    // deterministic.
//                    System.out.println("Loss because different type : " + thisEdge + " vs " + referenceEdge);
                    loss += 1;
                }else{

                }
            }
        }
        return loss;
    }

    /**
     * Compute the difference of features on this graph against another subgraph.
     *
     * @param otherGraph The other subgraph.
     * @param extractor  The feature extracor.
     * @return The delta of the features on these graphs.
     */
    public GraphFeatureVector getDelta(MentionSubGraph otherGraph, PairFeatureExtractor extractor) {
        if (parentGraph != otherGraph.parentGraph) {
            throw new IllegalArgumentException("To compute delta, both graph should have the same parent.");
        }

        GraphFeatureVector deltaFeatureVector = extractor.newGraphFeatureVector();

        // For edges in this subgraph.
        for (SubGraphEdge edge : edgeTable.values()) {
            deltaFeatureVector.extend(edge.getEdgeFeatures(extractor), edge.getEdgeType().name());
        }

        // For edges in the other subgraph.
        for (SubGraphEdge edge : otherGraph.edgeTable.values()) {
            deltaFeatureVector.extend(edge.getEdgeFeatures(extractor).negation(), edge.getEdgeType().name());
        }
        return deltaFeatureVector;
    }

//    /**
//     * Compute the difference of features on this graph against another subgraph.
//     *
//     * @param otherGraph The other subgraph.
//     * @param extractor  The feature extracor.
//     * @return The delta of the features on these graphs.
//     */
//    public GraphFeatureVector getDelta(MentionSubGraph otherGraph, PairFeatureExtractor extractor) {
//        if (parentGraph != otherGraph.parentGraph) {
//            throw new IllegalArgumentException("To compute delta, both graph should have the same parent.");
//        }
//
//        GraphFeatureVector deltaFeatureVector = extractor.newGraphFeatureVector();
//
//        Set<SubGraphEdge> otherEdgeMatched = new HashSet<>();
//
//        // For edges in this subgraph.
//        for (SubGraphEdge edge : edgeTable.values()) {
//            SubGraphEdge otherEdge = otherGraph.getEdge(edge.getDep(), edge.getGov());
//
//            // For edges in this subgraph but not in the other subgraph.
//            if (otherEdge == null) {
//                // Other graph does not contain this edge, add all features to the delta.
//                deltaFeatureVector.extend(edge.getEdgeFeatures(extractor), edge.getEdgeType().name());
//
////                System.out.println("Edge missed from prediction : " + edge);
////                System.out.println(edge.getEdgeFeatures(extractor).readableString());
//            } else if (!otherEdge.getEdgeType().equals(edge.getEdgeType())) {
//                // Add edge features.
//                deltaFeatureVector.extend(edge.getEdgeFeatures(extractor), edge.getEdgeType().name());
//                // Add the negation of the other features.
//                deltaFeatureVector.extend(otherEdge.getEdgeFeatures(extractor).negation(),
//                        otherEdge.getEdgeType().name());
//                otherEdgeMatched.add(otherEdge);
//
////                System.out.println("Edge difference in type : " + otherEdge.getEdgeType() + " " + edge
// .getEdgeType());
////                System.out.println(edge.getEdgeFeatures(extractor).readableString());
//            }
//        }
//
//        // For edges in the other subgraph but not in this.
//        for (SubGraphEdge edge : otherGraph.edgeTable.values()) {
//            if (!otherEdgeMatched.contains(edge)) {
////                System.out.println("Edge additional from prediction : " + edge);
////                System.out.println(edge.getEdgeFeatures(extractor).readableString());
//                deltaFeatureVector.extend(edge.getEdgeFeatures(extractor).negation(), edge.getEdgeType().name());
//            }
//        }
//        return deltaFeatureVector;
//    }

    /**
     * Convert the tree to transitive and equivalence resolved graph
     */
    public void resolveTree() {
        List<Set<Integer>> clusters = new ArrayList<>();

        ArrayListMultimap<EdgeType, Pair<Integer, Integer>> allRelations = ArrayListMultimap.create();
        ArrayListMultimap<EdgeType, Pair<Integer, Integer>> generalizedRelations = ArrayListMultimap.create();

        for (SubGraphEdge edge : edgeTable.values()) {
            EdgeType type = edge.getEdgeType();
            int govNode = edge.getGov();
            int depNode = edge.getDep();

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
            generalizedRelations.put(relation.getKey(),
                    Pair.with(node2ClusterId.get(govNode), node2ClusterId.get(depNode)));
        }

        resolvedRelations = GraphUtils.resolveRelations(generalizedRelations, group2Clusters, numNodes);
        corefChains = GraphUtils.createSortedCorefChains(group2Clusters);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SubGraph:\n");

        for (SubGraphEdge edge : edgeTable.values()) {
            sb.append("\t").append(edge.toString()).append("\n");
        }

        return sb.toString();
    }
}
