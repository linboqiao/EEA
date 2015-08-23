package edu.cmu.cs.lti.event_coref.model.graph;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.feature.VectorUtils;
import edu.cmu.cs.lti.event_coref.model.graph.Edge.EdgeType;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/3/15
 * Time: 2:16 PM
 *
 * @author Zhengzhong Liu
 */
public class SubGraph {
    //The edge index in the list, indexed by its gov idx and dep idx
    private Integer[][] edgeIndexes;

    private int numNodes;

    private List<SubGraphEdge> edges;

    private List<TObjectDoubleMap<String>> allEdgeFeatures;

    public EnumMap<EdgeType, TObjectDoubleMap<String>> getAllLabelledFeatures() {
        return allLabelledFeatures;
    }

    private EnumMap<EdgeType, TObjectDoubleMap<String>> allLabelledFeatures;

    //the subgraph score under current feature
    private double score;

    private Map<EdgeType, int[][]> edgeAdjacentList;

    private int[][] corefChains;


    public SubGraph(int numNodes) {
        edgeIndexes = new Integer[numNodes][numNodes];
        allEdgeFeatures = new ArrayList<>();
        edges = new ArrayList<>();
        allLabelledFeatures = new EnumMap<>(EdgeType.class);
        this.numNodes = numNodes;
    }

    public Integer getEdgeIndex(int govIdx, int depIdx) {
        return edgeIndexes[govIdx][depIdx];
    }

    public SubGraphEdge getEdge(int edgeIndex) {
        return edges.get(edgeIndex);
    }

    public SubGraphEdge getEdge(int govIdx, int depIdx) {
        Integer edgeIndex = getEdgeIndex(govIdx, depIdx);
        if (edgeIndex != null) {
            return edges.get(edgeIndex);
        } else {
            return null;
        }
    }

    public boolean hasEdge(int govIdx, int depIdx) {
        return getEdgeIndex(govIdx, depIdx) != null;
    }

    public TObjectDoubleMap<String> getEdgeFeatures(int govIdx, int depIdx) {
        if (hasEdge(govIdx, depIdx)) {
            return getEdgeFeatures(getEdgeIndex(govIdx, depIdx));
        } else {
            return null;
        }
    }

    public TObjectDoubleMap<String> getEdgeFeatures(int edgeIndex) {
        return allEdgeFeatures.get(edgeIndex);
    }

    public void addEdge(Edge newEdge, EdgeType newType) {
        edges.add(new SubGraphEdge(newEdge.govIdx, newEdge.depIdx, newType));
        allEdgeFeatures.add(newEdge.getArcFeatures());

        if (allLabelledFeatures.containsKey(newType)) {
            VectorUtils.sumInPlace(allLabelledFeatures.get(newType), newEdge.getLabelledFeatures().get(newType));
        } else {
            allLabelledFeatures.put(newType, new TObjectDoubleHashMap<>(newEdge.getLabelledFeatures().get(newType)));
        }

//        System.out.println("After adding edge " + newEdge + " using type " + newType);
//        System.out.println(allLabelledFeatures);

        int edgeIndex = edges.size() - 1;
        edgeIndexes[newEdge.govIdx][newEdge.depIdx] = edgeIndex;
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

    public double getLoss(SubGraph goldGraph) {
        //1. sort edges by dep


        //2. check difference in gov
        // apply 1.5 to root error
        // apply 1 to link error

        return 0;
    }

    public StructDelta getDelta(SubGraph otherGraph) {
        TObjectDoubleMap<String> unlabelledFeaturesDelta = getEdgeFeatureDeltas(otherGraph);
        EnumMap<EdgeType, TObjectDoubleMap<String>> typedFeaturesDelta = getTypedFeatureDeltas(otherGraph);
        return new StructDelta(unlabelledFeaturesDelta, typedFeaturesDelta);
    }

    public TObjectDoubleMap<String> getEdgeFeatureDeltas(SubGraph otherGraph) {
        TObjectDoubleMap<String> unlabelledFeaturesDelta = new TObjectDoubleHashMap<>();

        //compare edge features, we only need to see which edges are not contained in both set
        //if two graph contains the same edge, the features will cancelled each other.

        edges.stream().filter(edge -> !otherGraph.hasEdge(edge.govIdx, edge.depIdx)).forEach(edge -> {
            int edgeIndex = this.getEdgeIndex(edge.govIdx, edge.depIdx);
            TObjectDoubleMap<String> edgeFeatures = this.getEdgeFeatures(edgeIndex);
            VectorUtils.sumInPlace(unlabelledFeaturesDelta, this.getEdgeFeatures(edgeIndex));
            System.out.println("Edge feature in this graph " + edgeFeatures);
        });

        otherGraph.edges.stream().filter(edge -> !this.hasEdge(edge.govIdx, edge.depIdx)).forEach(edge -> {
            int edgeIndex = otherGraph.getEdgeIndex(edge.govIdx, edge.depIdx);
            TObjectDoubleMap<String> edgeFeatures = otherGraph.getEdgeFeatures(edgeIndex);
            VectorUtils.minusInplace(unlabelledFeaturesDelta, edgeFeatures);
            System.out.println("Edge feature in the other graph " + edgeFeatures);
        });
        return unlabelledFeaturesDelta;
    }

    public EnumMap<EdgeType, TObjectDoubleMap<String>> getTypedFeatureDeltas(SubGraph otherGraph) {
        //compare typed features
        EnumMap<EdgeType, TObjectDoubleMap<String>> typedFeaturesDelta = new EnumMap<>(EdgeType.class);

        EnumMap<EdgeType, TObjectDoubleMap<String>> allOtherGraphFeatures = otherGraph.getAllLabelledFeatures();

        System.out.println("Labelled Features in the other graph");
        System.out.println(allOtherGraphFeatures);

        System.out.println("Labelled Features in this graph");
        System.out.println(allLabelledFeatures);

        allLabelledFeatures.entrySet().forEach(
                entry -> {
                    EdgeType type = entry.getKey();
                    TObjectDoubleMap<String> otherFeatures = allOtherGraphFeatures.get(type);
                    TObjectDoubleMap<String> thisFeatures = entry.getValue();
                    TObjectDoubleMap<String> resultFeatures = otherFeatures != null ? VectorUtils.minus(thisFeatures, otherFeatures) : new TObjectDoubleHashMap<>(thisFeatures);

//                    System.out.println("Difference in " + type);
//                    System.out.println("This :");
//                    System.out.println(thisFeatures);
//                    System.out.println("Other :");
//                    System.out.println(otherFeatures);
//                    System.out.println("Result :");
//                    System.out.println(resultFeatures);

                    if (!resultFeatures.isEmpty()) {
                        typedFeaturesDelta.put(type, resultFeatures);
                    }
                }
        );

        allOtherGraphFeatures.entrySet().forEach(
                entry -> {
                    EdgeType type = entry.getKey();
                    if (!allLabelledFeatures.containsKey(type)) {
                        typedFeaturesDelta.put(type, VectorUtils.minus(new TObjectDoubleHashMap<>(), entry.getValue()));
                    }
                }
        );

        return typedFeaturesDelta;
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
            int govNode = edge.govIdx;
            int depNode = edge.depIdx;

            //The following can resolve transitive closure under the left-to-right decoding
            if (type.equals(EdgeType.Root)) {
                Set<Integer> newCluster = new HashSet<>();
                newCluster.add(depNode);
                clusters.add(newCluster);
            } else if (type.equals(EdgeType.Coreference)) {
                for (Set<Integer> cluster : clusters) {
                    if (cluster.contains(govNode)) {
                        cluster.add(depNode);
                        break;
                    }
                }
            } else {
                allRelations.put(type, Pair.of(govNode, depNode));
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

        for (Map.Entry<EdgeType, Pair<Integer, Integer>> relation : allRelations.entries()) {
            int govNode = relation.getValue().getKey();
            int depNode = relation.getValue().getValue();
            generalizedRelations.put(relation.getKey(), Pair.of(node2ClusterId.get(govNode), node2ClusterId.get(depNode)));
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
