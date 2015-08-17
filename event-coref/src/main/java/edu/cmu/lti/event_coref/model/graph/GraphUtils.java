package edu.cmu.lti.event_coref.model.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.primitives.Ints;
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
            T gov = r.getKey();
            T dep = r.getValue();
            transitiveResolvedRelations.put(gov, dep);
            reverseRelations.put(dep, gov);
            for (T govHead : reverseRelations.get(gov)) {
                transitiveResolvedRelations.put(govHead, dep);
            }
        }
        return transitiveResolvedRelations;
    }

    public static <T> int[][] resolveEquivalence(ArrayListMultimap<T, T> transitiveResolvedAdjacentGroup, ArrayListMultimap<T, Integer> group2Clusters, int numNodes) {
        ArrayListMultimap<Integer, Integer> transitiveResolvedAdjacentMentions = ArrayListMultimap.create();

        int[][] mentionAdjacentArray = new int[numNodes][];

        for (Map.Entry<T, T> relationEventPair : transitiveResolvedAdjacentGroup.entries()) {
            List<Integer> govCluster = group2Clusters.get(relationEventPair.getKey());
            List<Integer> depCluster = group2Clusters.get(relationEventPair.getValue());

            for (int govMentionId : govCluster) {
                for (int depMentionId : depCluster) {
                    transitiveResolvedAdjacentMentions.put(govMentionId, depMentionId);
                }
            }
        }

        for (Map.Entry<Integer, Collection<Integer>> mentionRow : transitiveResolvedAdjacentMentions.asMap().entrySet()) {
            int mentionId = mentionRow.getKey();
            int[] adjacentMentions = Ints.toArray(mentionRow.getValue());
            Arrays.sort(adjacentMentions);
            mentionAdjacentArray[mentionId] = adjacentMentions;
        }

        return mentionAdjacentArray;
    }


    /**
     * From list of clusters, grouped by whatever key T, convert to sorted coref chains
     *
     * @param group2Clusters
     * @param <T>
     * @return
     */
    public static <T> int[][] createSortedCorefChains(ArrayListMultimap<T, Integer> group2Clusters) {
        SortedMap<Integer, int[]> chainsSortedByHead = new TreeMap<>();
        for (Map.Entry<T, Collection<Integer>> entry : group2Clusters.asMap().entrySet()) {
            Collection<Integer> chainList = entry.getValue();
            if (chainList.size() > 1) {
                int[] chainArr = Ints.toArray(chainList);
                Arrays.sort(chainArr);
                Integer headId = chainArr[0];
                chainsSortedByHead.put(headId, chainArr);
            }
        }

        int clusterId = 0;
        int[][] corefChains = new int[chainsSortedByHead.size()][];
        for (Map.Entry<Integer, int[]> entry : chainsSortedByHead.entrySet()) {
            corefChains[clusterId++] = entry.getValue();
        }
        return corefChains;
    }

    public static <T> Map<Edge.EdgeType, int[][]> resolveRelations(ArrayListMultimap<Edge.EdgeType, Pair<T, T>> generalizedRelations, ArrayListMultimap<T, Integer> clusters, int numNodes) {
        Map<Edge.EdgeType, int[][]> edgeAdjacentList = new HashMap<>();

        for (Map.Entry<Edge.EdgeType, Collection<Pair<T, T>>> relationsByType : generalizedRelations.asMap().entrySet()) {
            //resolve transitive
            ArrayListMultimap<T, T> transitiveResolvedAdjacentEvents = GraphUtils.linkTransitiveRelations(relationsByType.getValue());
            //resolve equivalence
            int[][] mentionAdjacentArray = GraphUtils.resolveEquivalence(transitiveResolvedAdjacentEvents, clusters, numNodes);
            edgeAdjacentList.put(relationsByType.getKey(), mentionAdjacentArray);
        }
        return edgeAdjacentList;
    }
}