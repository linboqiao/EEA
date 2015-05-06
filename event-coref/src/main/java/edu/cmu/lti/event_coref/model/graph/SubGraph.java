package edu.cmu.lti.event_coref.model.graph;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.feature.VectorUtils;
import edu.cmu.lti.event_coref.model.graph.Edge.EdgeType;
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

        for (Map.Entry<EdgeType, TObjectDoubleMap<String>> entry
                : newEdge.getLabelledFeatures().entrySet()) {
            if (allLabelledFeatures.containsKey(entry.getKey())) {
                VectorUtils.sumInPlace(allLabelledFeatures.get(entry.getKey()), entry.getValue());
            } else {
                allLabelledFeatures.put(entry.getKey(), entry.getValue());
            }
        }
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

        //compare edge features
        for (SubGraphEdge edge : edges) {
            if (!otherGraph.hasEdge(edge.govIdx, edge.depIdx)) {
                int edgeIndex = this.getEdgeIndex(edge.govIdx, edge.depIdx);
                VectorUtils.sumInPlace(unlabelledFeaturesDelta, this.getEdgeFeatures(edgeIndex));
            }
        }

        for (SubGraphEdge edge : otherGraph.edges) {
            if (!this.hasEdge(edge.govIdx, edge.depIdx)) {
                int edgeIndex = otherGraph.getEdgeIndex(edge.govIdx, edge.depIdx);
                VectorUtils.minusInplace(unlabelledFeaturesDelta, otherGraph.getEdgeFeatures(edgeIndex));
            }
        }
        return unlabelledFeaturesDelta;
    }

    public EnumMap<EdgeType, TObjectDoubleMap<String>> getTypedFeatureDeltas(SubGraph otherGraph) {
        //compare typed features
        EnumMap<EdgeType, TObjectDoubleMap<String>> typedFeaturesDelta = new EnumMap<>(EdgeType.class);

        EnumMap<EdgeType, TObjectDoubleMap<String>> allOtherGraphFeatures = otherGraph.getAllLabelledFeatures();
        for (EnumMap.Entry<EdgeType, TObjectDoubleMap<String>> entry : allLabelledFeatures.entrySet()) {
            EdgeType type = entry.getKey();
            TObjectDoubleMap<String> thisFeatuers = entry.getValue();
            TObjectDoubleMap<String> otherFeatures = allOtherGraphFeatures.get(type);
            TObjectDoubleMap<String> resultFeatures = VectorUtils.minus(thisFeatuers, otherFeatures);
            typedFeaturesDelta.put(type, resultFeatures);
        }
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
            if (type.equals(EdgeType.Root)) {
                Set<Integer> newCluster = new HashSet<>();
                newCluster.add(depNode);
                clusters.add(newCluster);
            } else if (type.equals(EdgeType.Coreference)) {
                for (Set<Integer> cluster : clusters) {
                    if (cluster.contains(depNode) || cluster.contains(govNode)) {
                        cluster.add(depNode);
                        cluster.add(govNode);
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
}
