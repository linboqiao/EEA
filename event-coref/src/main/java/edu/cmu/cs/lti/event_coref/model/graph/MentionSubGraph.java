package edu.cmu.cs.lti.event_coref.model.graph;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge.EdgeType;
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
    //The edge index in the list, indexed first by the current index, then the antecedent index.
    private Integer[][] edgeIndexes;

    private List<SubGraphEdge> edges;

    private int numNodes;

    // The subgraph score under current feature.
    private double score;

    private Map<EdgeType, int[][]> edgeAdjacentList;

    private int[][] corefChains;

    private MentionGraph parentGraph;

    /**
     * Initialize a mention subgraph with all the super graph's node. The edges are empty.
     *
     * @param parentGraph The parent graph of this subgraph.
     */
    public MentionSubGraph(MentionGraph parentGraph) {
        edgeIndexes = new Integer[parentGraph.numNodes()][parentGraph.numNodes()];
        numNodes = parentGraph.numNodes();
        edges = new ArrayList<>();
        this.parentGraph = parentGraph;
    }


    public Integer getEdgeIndex(int govIdx, int depIdx) {
        return edgeIndexes[govIdx][depIdx];
    }

    public void addEdge(MentionGraphEdge mentionGraphEdge, EdgeType newType) {
        edges.add(new SubGraphEdge(mentionGraphEdge, newType));
        int edgeIndex = edges.size() - 1;
        edgeIndexes[mentionGraphEdge.depIdx][mentionGraphEdge.govIdx] = edgeIndex;
    }

    public SubGraphEdge getEdge(int depIdx, int govIdx) {
        Integer edgeIndex = edgeIndexes[depIdx][govIdx];
        if (edgeIndex == null) {
            return null;
        }
        return edges.get(edgeIndex);
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Map<EdgeType, int[][]> getEdgeAdjacentList() {
        return edgeAdjacentList;
    }

    public int[][] getCorefChains() {
        return corefChains;
    }

    // TODO compute the hamming
    public double getLoss(MentionSubGraph goldGraph) {
        //1. sort edges by dep


        //2. check difference in gov
        // apply 1.5 to root error
        // apply 1 to link error


        return 0;
    }

    public GraphFeatureVector getDelta(MentionSubGraph otherGraph) {
        GraphFeatureVector deltaFeatureVector = new GraphFeatureVector(parentGraph.getClassAlphabet(), parentGraph
                .getFeatureAlphabet());

        Set<SubGraphEdge> otherEdgeMatched = new HashSet<>();

        // This can be make efficient by reading of the arcs.
        for (SubGraphEdge edge : edges) {
            SubGraphEdge otherEdge = otherGraph.getEdge(edge.getDep(), edge.getGov());

            if (otherEdge == null) {
                // Other graph does not contain this edge, add all features to the delta.
                deltaFeatureVector.extend(edge.getEdgeFeatures(), edge.getEdgeType().name());
            } else if (!otherEdge.getEdgeType().equals(edge.getEdgeType())) {
                // Add edge features.
                deltaFeatureVector.extend(edge.getEdgeFeatures(), edge.getEdgeType().name());
                // Add the negation of the other features.
                deltaFeatureVector.extend(otherEdge.getEdgeFeatures().negation(), otherEdge.getEdgeType().name());
                otherEdgeMatched.add(otherEdge);
            }
        }

        for (SubGraphEdge edge : otherGraph.edges) {
            if (!otherEdgeMatched.contains(edge)) {
                deltaFeatureVector.extend(edge.getEdgeFeatures().negation(), edge.getEdgeType().name());
            }
        }
        return deltaFeatureVector;
    }

    /**
     * Convert the tree to transitive and equivalence resolved graph
     */
    public void resolveTree() {
        List<Set<Integer>> clusters = new ArrayList<>();

        ArrayListMultimap<EdgeType, Pair<Integer, Integer>> allRelations = ArrayListMultimap.create();
        ArrayListMultimap<EdgeType, Pair<Integer, Integer>> generalizedRelations = ArrayListMultimap.create();

        for (SubGraphEdge edge : edges) {
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

        edgeAdjacentList = GraphUtils.resolveRelations(generalizedRelations, group2Clusters, numNodes);
        corefChains = GraphUtils.createSortedCorefChains(group2Clusters);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SubGraph:\n");
        for (Integer[] edgeIndex : edgeIndexes) {
            for (Integer index : edgeIndex) {
                if (index != null) {
                    if (edges.get(index) != null) {
                        sb.append("\t").append(edges.get(index).toString()).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }
}
