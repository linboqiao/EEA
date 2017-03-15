package edu.cmu.cs.lti.learning.model.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/5/15
 * Time: 4:41 PM
 *
 * @author Zhengzhong Liu
 */
public class GraphUtils {
    private static final Logger logger = LoggerFactory.getLogger(GraphUtils.class.getName());

    public static <T extends Comparable> Map<T, List<T>> transitiveClosure(Collection<Pair<T, T>> relations) {
        Map<T, List<T>> closureGraph = new HashMap<>();

        Set<T> vertexSet = new HashSet<>();
        for (Pair<T, T> relation : relations) {
            vertexSet.add(relation.getKey());
            vertexSet.add(relation.getValue());
        }

        List<T> sortedVertex = new ArrayList<>(vertexSet);
        Collections.sort(sortedVertex);

        Map<T, Integer> indices = new HashMap<>();

        int index = 0;
        for (T t : sortedVertex) {
            indices.put(t, index);
            index++;
        }

        int numVertex = sortedVertex.size();

        boolean[][] reach = new boolean[numVertex][numVertex];

        for (Pair<T, T> relation : relations) {
            reach[indices.get(relation.getKey())][indices.get(relation.getValue())] = true;
        }

        for (int k = 0; k < numVertex; k++) {
            for (int i = 0; i < numVertex; i++) {
                for (int j = 0; j < numVertex; j++) {
                    reach[i][j] = reach[i][j] || (reach[i][k] && reach[k][j]);
                }
            }
        }

        for (int i = 0; i < numVertex; i++) {
            T gov = sortedVertex.get(i);
            List<T> deps = new ArrayList<>();
            for (int j = 0; j < numVertex; j++) {
                if (reach[i][j]) {
                    T dep = sortedVertex.get(j);
                    deps.add(dep);
                }
            }

            if (!deps.isEmpty()) {
                closureGraph.put(gov, deps);
            }
        }

        return closureGraph;
    }

    public static <T extends Comparable> Map<T, List<T>> transitiveReduction(Map<T, List<T>> closureGraph) {

        Map<T, List<T>> reducedGraph = new HashMap<>();

        Set<Pair<T, T>> indirectLinks = new HashSet<>();

        for (Map.Entry<T, List<T>> row : closureGraph.entrySet()) {
            T from = row.getKey();
            for (T to : row.getValue()) {
                if (closureGraph.containsKey(to)) {
                    // Show that the "to" node can be reached from another way, thus "indirect link".
                    for (T k : closureGraph.get(to)) {
                        indirectLinks.add(Pair.of(from, k));
                    }
                }
            }
        }

        for (Map.Entry<T, List<T>> row : closureGraph.entrySet()) {
            T from = row.getKey();
            List<T> toList = new ArrayList<>();
            for (T to : row.getValue()) {
                if (!indirectLinks.contains(Pair.of(from, to))) {
                    toList.add(to);
                }
            }
            Collections.sort(toList);

            reducedGraph.put(from, toList);
        }
        return reducedGraph;
    }

    /**
     * Propagate the relations between with among coreference clusters.
     *
     * @param clusterAdjacent Adjacent list for the clusters.
     * @param group2Clusters  Map from each cluster to the mentions.
     * @param <M>             Type representing mention.
     * @param <E>             Type representing mention cluster.
     * @return
     */
    public static <E, M extends Comparable> ArrayListMultimap<M, M> resolveEquivalence(
            Map<E, List<E>> clusterAdjacent, Multimap<E, M> group2Clusters) {
        ArrayListMultimap<M, M> mentionAdjacent = ArrayListMultimap.create();
        ArrayListMultimap<M, M> sortedMentionAdjacent = ArrayListMultimap.create();

        for (Map.Entry<E, List<E>> adjacent : clusterAdjacent.entrySet()) {
            Collection<M> govCluster = group2Clusters.get(adjacent.getKey());

            for (E dep : adjacent.getValue()) {
                Collection<M> depCluster = group2Clusters.get(dep);
                for (M govMention : govCluster) {
                    for (M depMention : depCluster) {
                        mentionAdjacent.put(govMention, depMention);
                    }
                }
            }
        }

        for (Map.Entry<M, Collection<M>> mentionRow : mentionAdjacent.asMap().entrySet()) {
            M mention = mentionRow.getKey();
            List<M> adjacentElements = new ArrayList<>(mentionRow.getValue());
            Collections.sort(adjacentElements);
            sortedMentionAdjacent.putAll(mention, adjacentElements);
        }
        return sortedMentionAdjacent;
    }


    /**
     * From list of clusters, grouped by whatever key T, convert to sorted coref chains.
     *
     * @param group2Clusters Map from the cluster Id to elements in the cluster.
     * @param <E>            The type representing the cluster id.
     * @param <M>            The type representing the cluster element.
     * @return List of non-singleton coreference chains sorted by event mention id.
     */
    public static <E, M extends Comparable> List<M>[] createSortedCorefChains(Multimap<E, M> group2Clusters) {
        SortedMap<M, List<M>> chainsSortedByHead = new TreeMap<>();
        for (Map.Entry<E, Collection<M>> entry : group2Clusters.asMap().entrySet()) {
            List<M> chainList = new ArrayList<>(entry.getValue());
            if (chainList.size() > 1) {
                Collections.sort(chainList);
                M firstElement = chainList.get(0);
                chainsSortedByHead.put(firstElement, chainList);
            }
//            logger.info("Chain list is " + chainList);
        }

        int clusterId = 0;
        List<M>[] corefChains = new List[chainsSortedByHead.size()];
        for (Map.Entry<M, List<M>> entry : chainsSortedByHead.entrySet()) {
            corefChains[clusterId++] = entry.getValue();
        }
        return corefChains;
    }

    /**
     * Propagate all transitive and equivalence relations to all the nodes.
     *
     * @param <T>                   The type representing a cluster of nodes (e.g. event for event mentions)
     * @param interClusterRelations Relations in between clusters.
     * @param clusters              Nodes contained in the cluster.
     * @return The resolved graph as adjacent list, stored separated for each edge type.
     */
    public static <T extends Comparable> Map<EdgeType, Set<Pair<T, T>>> resolveRelations(
            Multimap<EdgeType, Pair<Integer, Integer>> interClusterRelations, Multimap<Integer, T> clusters) {
        Map<EdgeType, Set<Pair<T, T>>> allRelations = new HashMap<>();

        for (Map.Entry<EdgeType, Collection<Pair<Integer, Integer>>> relationsByType : interClusterRelations
                .asMap().entrySet()) {
            // Resolve transitive.
            Map<Integer, List<Integer>> eventClosure = GraphUtils.transitiveClosure(relationsByType
                    .getValue());
            // Resolve equivalence.
            ArrayListMultimap<T, T> mentionAdjacentArray =
                    GraphUtils.resolveEquivalence(eventClosure, clusters);

            Set<Pair<T, T>> relations = new HashSet<>();
            for (Map.Entry<T, Collection<T>> mentionAdjacent : mentionAdjacentArray.asMap().entrySet()) {
                for (T toMention : mentionAdjacent.getValue()) {
                    relations.add(Pair.of(mentionAdjacent.getKey(), toMention));
                }
            }
            allRelations.put(relationsByType.getKey(), relations);
        }


        return allRelations;
    }
}