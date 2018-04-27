package edu.cmu.cs.lti.learning.model.decoding;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 2/3/17
 * Time: 8:26 PM
 *
 * @author Zhengzhong Liu
 */
public class IncrementalLinkBuilder<T extends Comparable<T>> {
    private TObjectIntMap<T> node2Cluster;

    private Table<T, T, EdgeType> links;

    // A reach map.
    // If a node B is reachable from A of type T, then there will be an element B, A in the multimap under T.
    private Map<EdgeType, SetMultimap<Integer, Integer>> reachMapByType;

    private Map<T, EdgeType> latterNodesByType;

    private double score;

    public IncrementalLinkBuilder(TObjectIntMap<T> node2Cluster) {
        this.node2Cluster = node2Cluster;
        links = HashBasedTable.create();
        reachMapByType = new HashMap<>();
        latterNodesByType = new HashMap<>();
        score = 0;
    }

    public boolean addLink(T gov, T dep, EdgeType type, double score) {
        boolean success = checkLink(gov, dep, type);

        if (success) {
            this.score += score;
            storeLink(gov, dep, type);
        }

        return success;
    }

    public boolean checkLink(T gov, T dep, EdgeType type) {
        return checkCycle(gov, dep, type);
    }

    private void storeLink(T gov, T dep, EdgeType type) {
        links.put(gov, dep, type);
        if (!EdgeType.isRootType(type)) {
            if (gov.compareTo(dep) > 0) {
                latterNodesByType.put(gov, type);
            } else {
                latterNodesByType.put(dep, type);
            }
        }
        maintainReachMatrix(gov, dep, type);
    }

    private void maintainReachMatrix(T gov, T dep, EdgeType type) {
        SetMultimap<Integer, Integer> reachMap;

        if (reachMapByType.containsKey(type)) {
            reachMap = reachMapByType.get(type);
        } else {
            reachMap = HashMultimap.create();
            reachMapByType.put(type, reachMap);
        }

        int govCluster = node2Cluster.get(gov);
        int depCluster = node2Cluster.get(dep);

        // The dep is directly reachable from gov.
        reachMap.put(depCluster, govCluster);

        // Clusters that can reach gov can also reach dep.
        for (Integer transitiveGov : reachMap.get(govCluster)) {
            reachMap.put(depCluster, transitiveGov);
        }
    }

    private boolean checkCycle(T gov, T dep, EdgeType type) {
        int govCluster = node2Cluster.get(gov);
        int depCluster = node2Cluster.get(dep);

        if (govCluster == depCluster) {
            return false;
        }

        if (reachMapByType.containsKey(type)) {
            SetMultimap<Integer, Integer> reachMap = reachMapByType.get(type);
            if (reachMap.containsKey(govCluster)) {
                if (reachMap.get(govCluster).contains(depCluster)) {
                    // This means the dep cluster is reachable from gov, so adding this link means a cycle.
                    return false;
                }
            }
        }

        return true;
    }

    public double getScore() {
        return score;
    }

    public Table<T, T, EdgeType> getLinks() {
        return links;
    }

    public boolean linkToPrevious(T node, EdgeType type) {
        return latterNodesByType.containsKey(node) && latterNodesByType.get(node) == type;
    }


    public static void main(String[] argv) {
        TObjectIntMap<String> clusters = new TObjectIntHashMap<>();

        clusters.put("Attack_A", 0);
        clusters.put("Attack_B", 0);

        clusters.put("Die_A", 1);
        clusters.put("Die_B", 1);

        clusters.put("Injure_A", 2);
        clusters.put("Injure_B", 2);
        clusters.put("Injure_C", 2);

        clusters.put("Hospital_A", 3);
        clusters.put("Hospital_B", 3);
        clusters.put("Hospital_C", 3);

        IncrementalLinkBuilder<String> linkBuilder = new IncrementalLinkBuilder<>(clusters);

        EdgeType t = EdgeType.After;

        List<Pair<String, String>> linksToAdd = new ArrayList<>();

        linksToAdd.add(Pair.of("Attack_A", "Die_A"));
        linksToAdd.add(Pair.of("Attack_B", "Injure_B"));
        linksToAdd.add(Pair.of("Injure_C", "Hospital_B"));
        linksToAdd.add(Pair.of("Hospital_C", "Attack_B"));

        for (Pair<String, String> link : linksToAdd) {
            System.out.println("Adding " + link);
            System.out.println("Can you success ? "
                    + linkBuilder.checkLink(link.getKey(), link.getValue(), EdgeType.After));
            boolean success = linkBuilder.addLink(link.getKey(), link.getValue(), EdgeType.After, 1);
            System.out.println("Actually " + success);
        }
    }
}
