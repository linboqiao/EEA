package edu.cmu.cs.lti.learning.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/24/16
 * Time: 4:05 PM
 *
 * @author Zhengzhong Liu
 */
public class ClusterBuilder<T extends Comparable<T>> {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    private Map<T, Integer> clusterAssignments;

    private List<TreeSet<T>> clusters;

    private Set<T> elements;

    public ClusterBuilder() {
        clusterAssignments = new HashMap<>();
        clusters = new ArrayList<>();
        elements = new HashSet<>();
    }

    public ClusterBuilder<T> makeCopy() {
        ClusterBuilder<T> builder = new ClusterBuilder<>();

        builder.elements.addAll(elements);

        for (TreeSet<T> cluster : clusters) {
            TreeSet<T> clusterCopy = new TreeSet<>();
            clusterCopy.addAll(cluster);
            builder.clusters.add(clusterCopy);
        }

        builder.clusterAssignments.putAll(clusterAssignments);

        return builder;
    }


    private void addElement(T element) {
        if (!elements.contains(element)) {
            elements.add(element);
            int newClusterIndex = clusters.size();
            clusterAssignments.put(element, newClusterIndex);

            TreeSet<T> cluster = new TreeSet<>();
            cluster.add(element);
            clusters.add(cluster);

//            logger.info("Added element " + element + " assigned to cluster " + newClusterIndex);
//            logger.info("New singleton cluster " + clusters.get(newClusterIndex));
        }
    }

    public void addLink(T element1, T element2) {
        addElement(element1);
        addElement(element2);

//        logger.info("Cluster assignment is now.");
//        logger.info(clusterAssignments.toString());

//        logger.info("Clusters are now");
//        for (TreeSet<T> cluster : clusters) {
//            logger.info(cluster.toString());
//        }

        int clusterId1 = clusterAssignments.get(element1);
        int clusterId2 = clusterAssignments.get(element2);

        TreeSet<T> cluster1 = clusters.get(clusterId1);
        TreeSet<T> cluster2 = clusters.get(clusterId2);

//        logger.info("Adding link " + element1 + " " + element2);
//        logger.info("Element 1 is in " + clusterId1 + " which is " + cluster1);
//        logger.info("Element 2 is in " + clusterId2 + " which is " + cluster2);

        remove(clusters, clusterId1, clusterId2);

        TreeSet<T> mergedSet = new TreeSet<>();
        mergedSet.addAll(cluster1);
        mergedSet.addAll(cluster2);

//        logger.info("Merged cluster is " + mergedSet);

        int mergedClusterId = insert(clusters, mergedSet);

//        for (T t : mergedSet) {
//            clusterAssignments.put(t, mergedClusterId);
//        }

        for (int i = mergedClusterId; i < clusters.size(); i++) {
            TreeSet<T> cluster = clusters.get(i);
            for (T t : cluster) {
                clusterAssignments.put(t, i);
            }
        }

//        logger.info("Updated clusters are : ");
//        for (TreeSet<T> cluster : clusters) {
//            logger.info(cluster.toString());
//        }

//        logger.info(clusterAssignments.toString());
//        logger.info("After update, cluster size is " + clusters.size());
    }

    private int insert(List<TreeSet<T>> clusters, TreeSet<T> newCluster) {
        T newFirst = newCluster.first();

        int insertPos = 0;
        for (TreeSet<T> c : clusters) {
            T currentFirst = c.first();

            int compare = newFirst.compareTo(currentFirst);

            if (compare < 0) {
                break;
            }

            insertPos++;
        }

        clusters.add(insertPos, newCluster);

        return insertPos;
    }

    private void remove(List<TreeSet<T>> clusters, int... clusterIds) {
        Arrays.sort(clusterIds);

//        logger.info("All clusters are:");
//        for (TreeSet<T> cluster : clusters) {
//            logger.info(cluster.toString());
//        }

        for (int i = clusterIds.length - 1; i >= 0; i--) {
            int clusterId = clusterIds[i];
//            logger.info("removing cluster " + clusterId);
//            logger.info("Which is " + clusters.get(clusterId));
            clusters.remove(clusterId);
//            logger.info("After removing");
//            for (TreeSet<T> cluster : clusters) {
//                logger.info(cluster.toString());
//            }
        }
    }

    public boolean match(ClusterBuilder another) {
        return clusterMatch(another.clusters);
    }

    private boolean clusterMatch(List<TreeSet<T>> thatClusters) {
        if (this.clusters.size() != thatClusters.size()) {
            return false;
        }

        for (int i = 0; i < this.clusters.size(); i++) {
            TreeSet<T> thisCluster = this.clusters.get(i);
            TreeSet<T> thatCluster = thatClusters.get(i);

            if (thisCluster.size() != thatCluster.size()) {
                return false;
            }

            Iterator<T> thisIter = thisCluster.iterator();
            Iterator<T> thatIter = thatCluster.iterator();

            while (thisIter.hasNext()) {
                T thisElement = thisIter.next();
                T thatElement = thatIter.next();

                if (!thisElement.equals(thatElement)) {
                    return false;
                }
            }
        }
        return true;
    }


    public List<TreeSet<T>> getClusters() {
        return clusters;
    }

    public Set<T> getElements() {
        return elements;
    }

    public static void main(String[] args) {
        ClusterBuilder<Integer> builder = new ClusterBuilder<>();
        builder.addLink(96, 145);
        builder.addLink(101, 147);
        builder.addLink(147, 241);
        builder.addLink(43, 399);
        builder.addLink(96, 441);
        builder.addLink(441, 443);
    }
}
