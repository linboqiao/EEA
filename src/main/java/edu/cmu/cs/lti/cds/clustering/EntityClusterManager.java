package edu.cmu.cs.lti.cds.clustering;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.cmu.cs.lti.cds.model.EntityCluster;
import edu.cmu.cs.lti.cds.model.FeatureTable;
import gnu.trove.map.hash.TIntObjectHashMap;

public class EntityClusterManager {
  public enum entityType {
    ORGANIZATION, LOCATION, PERSON
  }

  Table<String, String, List<Integer>> typeSpecifiedMentionHead2Cluster = HashBasedTable.create();

  private List<EntityCluster> clusters = new ArrayList<EntityCluster>();

  private List<FeatureTable> featureTables = new ArrayList<FeatureTable>();

  public int numOfCluster() {
    return clusters.size();
  }

  public TIntObjectHashMap<FeatureTable> getCandidateClusterFeatures(String entityType,
          String mentionHead) {
    TIntObjectHashMap<FeatureTable> candidateClusters = new TIntObjectHashMap<FeatureTable>();
    List<Integer> candidateIds = typeSpecifiedMentionHead2Cluster.get(entityType, mentionHead);
    if (candidateIds != null) {
      for (Integer clusterId : candidateIds) {
        candidateClusters.put(clusterId, getClusterFeature(clusterId));
      }
    }

    return candidateClusters;
  }

  public EntityCluster getCluster(int id) {
    return clusters.get(id);
  }

  public FeatureTable getClusterFeature(int id) {
    return featureTables.get(id);
  }

  public int getNumClusters() {
    return clusters.size();
  }

  public void createNewCluster(String entityId, Date date, FeatureTable features,
          String clusterType, String mentionHead) {
    EntityCluster cluster = new EntityCluster();
    cluster.addNewEntity(entityId, date);
    clusters.add(cluster);
    featureTables.add(features);
    addClusterHeadByType(clusterType, mentionHead, clusters.size() - 1);
    System.out.println("Creating new cluster [" + mentionHead + "], # cluster is : "
            + clusters.size());
  }

  private void addClusterHeadByType(String clusterType, String mentionHead, int clusterId) {
    if (typeSpecifiedMentionHead2Cluster.contains(clusterType, mentionHead)) {
      typeSpecifiedMentionHead2Cluster.get(clusterType, mentionHead).add(clusterId);
    } else {
      List<Integer> newClusterIds = new ArrayList<Integer>();
      newClusterIds.add(clusterId);
      typeSpecifiedMentionHead2Cluster.put(clusterType, mentionHead, newClusterIds);
    }
  }

  public void addToExistingCluster(int clusterId, String entityId, Date date,
          FeatureTable newEntityFeature) {
    getCluster(clusterId).addNewEntity(entityId, date);
    getClusterFeature(clusterId).mergeWith(newEntityFeature);

    System.out.println("Adding entity " + entityId + " to cluster " + clusterId);
  }
}