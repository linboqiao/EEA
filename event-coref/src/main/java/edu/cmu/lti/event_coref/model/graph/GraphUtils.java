package edu.cmu.lti.event_coref.model.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.primitives.Ints;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/5/15
 * Time: 4:41 PM
 *
 * @author Zhengzhong Liu
 */
public class GraphUtils {
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


    public static <T> int[][] createSortedCorefChains(ArrayListMultimap<T, Integer> group2Clusters) {
        int[][] corefChains = new int[group2Clusters.size()][];

        SortedMap<Integer, int[]> chainsSortedByHead = new TreeMap<>();
        for (Map.Entry<T, Collection<Integer>> entry : group2Clusters.asMap().entrySet()) {
            Collection<Integer> chainList = entry.getValue();
            int[] chainArr = Ints.toArray(chainList);
            Arrays.sort(chainArr);
            Integer headId = chainArr[0];
            chainsSortedByHead.put(headId, chainArr);
        }

        int clusterId = 0;
        for (Map.Entry<Integer, int[]> entry : chainsSortedByHead.entrySet()) {
            if (entry.getValue().length > 1) {
                //singleton are not uniquely stored
                corefChains[clusterId++] = entry.getValue();
            }
        }
        return corefChains;
    }

    public static <T> Map<Edge.EdgeType, int[][]> resolveRelations(ArrayListMultimap<Edge.EdgeType, Pair<T, T>> generalizedRelations, ArrayListMultimap<T, Integer> event2Clusters, int numNodes) {
        Map<Edge.EdgeType, int[][]> edgeAdjacentList = new HashMap<>();
        for (Edge.EdgeType type : Edge.EdgeType.values()) {
            if (!type.equals(Edge.EdgeType.Root)) {
                //Root type do not need to be modelled explicitly here
                edgeAdjacentList.put(type, new int[numNodes][]);
            }
        }

        for (Map.Entry<Edge.EdgeType, Collection<Pair<T, T>>> relationsByType : generalizedRelations.asMap().entrySet()) {
            //resolve transitive
            ArrayListMultimap<T, T> transitiveResolvedAdjacentEvents = GraphUtils.linkTransitiveRelations(relationsByType.getValue());
            //resolve equivalence
            int[][] mentionAdjacentArray = GraphUtils.resolveEquivalence(transitiveResolvedAdjacentEvents, event2Clusters, numNodes);
            edgeAdjacentList.put(relationsByType.getKey(), mentionAdjacentArray);
        }
        return edgeAdjacentList;
    }

}