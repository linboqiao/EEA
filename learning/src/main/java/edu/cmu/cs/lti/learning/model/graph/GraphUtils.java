package edu.cmu.cs.lti.learning.model.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
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

    /**
     * Given a list of relations, produce a adjacent list of transitive resolved relations
     *
     * @param relations List of binary relations
     * @param <T>       The class of the relation argument
     * @return Transitive resolved relations, represented as adjacent map
     */
    public static <T> ArrayListMultimap<T, T> linkTransitiveRelations(Collection<Pair<T, T>> relations) {
        ArrayListMultimap<T, T> transitiveResolvedRelations = ArrayListMultimap.create();

        ArrayListMultimap<T, T> reverseRelations = ArrayListMultimap.create();

        for (Pair<T, T> r : relations) {
            T gov = r.getLeft();
            T dep = r.getRight();
            transitiveResolvedRelations.put(gov, dep);
            reverseRelations.put(dep, gov);
            for (T govHead : reverseRelations.get(gov)) {
                transitiveResolvedRelations.put(govHead, dep);
            }
        }
        return transitiveResolvedRelations;
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
            Multimap<E, E> clusterAdjacent, Multimap<E, M> group2Clusters) {
        ArrayListMultimap<M, M> mentionAdjacent = ArrayListMultimap.create();
        ArrayListMultimap<M, M> sortedMentionAdjacent = ArrayListMultimap.create();

        for (Map.Entry<E, E> relationEventPair : clusterAdjacent.entries()) {
            Collection<M> govCluster = group2Clusters.get(relationEventPair.getKey());
            Collection<M> depCluster = group2Clusters.get(relationEventPair.getValue());

            for (M govMention : govCluster) {
                for (M depMention : depCluster) {
                    mentionAdjacent.put(govMention, depMention);
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
    public static <T extends Comparable> Map<EdgeType, ListMultimap<T, T>> resolveRelations(
            Multimap<EdgeType, Pair<Integer, Integer>> interClusterRelations, Multimap<Integer, T> clusters) {
        Map<EdgeType, ListMultimap<T, T>> relationAdjacentLists = new HashMap<>();

        for (Map.Entry<EdgeType, Collection<Pair<Integer, Integer>>> relationsByType : interClusterRelations
                .asMap().entrySet()) {
            // Resolve transitive.
            ArrayListMultimap<Integer, Integer> eventRelationAdjacentList =
                    GraphUtils.linkTransitiveRelations(relationsByType.getValue());
            // Resolve equivalence.
            ArrayListMultimap<T, T> mentionAdjacentArray =
                    GraphUtils.resolveEquivalence(eventRelationAdjacentList, clusters);
            relationAdjacentLists.put(relationsByType.getKey(), mentionAdjacentArray);
        }
        return relationAdjacentLists;
    }
}