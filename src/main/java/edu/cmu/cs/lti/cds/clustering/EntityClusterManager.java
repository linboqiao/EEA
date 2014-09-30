package edu.cmu.cs.lti.cds.clustering;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.cmu.cs.lti.cds.model.EntityCluster;
import edu.cmu.cs.lti.cds.model.FeatureTable;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.StringUtils;
import edu.cmu.cs.lti.utils.TimeUtils;
import gnu.trove.map.hash.TIntObjectHashMap;

public class EntityClusterManager {
  public enum entityType {
    ORGANIZATION, LOCATION, PERSON
  }

  private Table<String, String, Set<Integer>> typeSpecifiedRepresentativeEntity2Cluster = HashBasedTable
          .create();

  private Map<Integer, EntityCluster> clusters = new LinkedHashMap<Integer, EntityCluster>();

  private Map<Integer, FeatureTable> featureTables = new HashMap<Integer, FeatureTable>();

  private int numOfCluster;

  private File dumpingFolder;

  public EntityClusterManager(File dumpingFolder) {
    if (dumpingFolder.isDirectory()) {
      this.dumpingFolder = dumpingFolder;
    } else {
      throw new IllegalArgumentException("Please provide a directory to store cluster information");
    }
  }

  public int numOfCluster() {
    return numOfCluster;
  }

  public String getRepresentativeStr(Entity entity) {
    return entity.getRepresentativeMention().getCoveredText().replace("\n", "");
  }

  public String getUniqueEntityId(String articleName, Entity entity) {
    return articleName + "_" + entity.getId();
  }

  public TIntObjectHashMap<FeatureTable> getCandidateClusterFeatures(String entityType,
          Entity entity, int clusterIdLimit) {
    // String headMentionStr = getRepresentativeStr(entity);

    TIntObjectHashMap<FeatureTable> candidateClusters = new TIntObjectHashMap<FeatureTable>();

    for (String entityWord : getEntityMentionWords(entity)) {
      Set<Integer> candidateIds = typeSpecifiedRepresentativeEntity2Cluster.get(entityType,
              entityWord);
      if (candidateIds != null) {
        for (int clusterId : candidateIds) {
          if (clusterId < clusterIdLimit && !candidateClusters.contains(clusterId)) {
            candidateClusters.put(clusterId, getClusterFeature(clusterId));
            // System.out.println("Found candidate " + clusterId + " by using " + entityWord);
          }
        }
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

  public void createNewCluster(Date date, FeatureTable features, String clusterType, Entity entity,
          String articleName) {
    String headMentionStr = getRepresentativeStr(entity);
    // System.out.println("Observe new entity: [" + headMentionStr + "], type is " + clusterType);
    String entityId = getUniqueEntityId(articleName, entity);
    EntityCluster cluster = new EntityCluster(entityId, date, headMentionStr, clusterType);
    int clusterId = numOfCluster;
    clusters.put(clusterId, cluster);
    featureTables.put(clusterId, features);
    for (String meaningfulWord : getEntityMentionWords(entity)) {
      hashClusterByTypeAndWord(clusterType, meaningfulWord, clusterId);
    }
    numOfCluster++;
    // System.out.println("Creating new cluster [" + headMentionStr + "], # cluster is : "
    // + clusters.size());
  }

  private Set<String> getEntityMentionWords(Entity entity) {
    Set<String> meaningFulHeadWords = new HashSet<String>();
    for (int i = 0; i < entity.getEntityMentions().size(); i++) {
      EntityMention mention = entity.getEntityMentions(i);
      Word head = mention.getHead();
      if (head.getPos().startsWith("NN")) {
        meaningFulHeadWords.add(head.getCoveredText());
      }
    }
    return meaningFulHeadWords;
  }

  private void hashClusterByTypeAndWord(String clusterType, String clusterHead, int clusterId) {
    if (typeSpecifiedRepresentativeEntity2Cluster.contains(clusterType, clusterHead)) {
      typeSpecifiedRepresentativeEntity2Cluster.get(clusterType, clusterHead).add(clusterId);
    } else {
      Set<Integer> newClusterIds = new HashSet<Integer>();
      newClusterIds.add(clusterId);
      typeSpecifiedRepresentativeEntity2Cluster.put(clusterType, clusterHead, newClusterIds);
    }
  }

  public void addToExistingCluster(int clusterId, Date date, FeatureTable newEntityFeature,
          String clusterType, Entity entity, String articleName) {
    String entityId = getUniqueEntityId(articleName, entity);
    String mentionHead = getRepresentativeStr(entity);
    getCluster(clusterId).addNewEntity(entityId, date, mentionHead);
    getClusterFeature(clusterId).mergeWith(newEntityFeature);
    // ensure new cluster is added at the end
    reinsertCluster(clusterId);
    // System.out.println("Adding to existing cluster: [" + mentionHead + "], " + clusterId);
  }

  public void reinsertCluster(int clusterId) {
    EntityCluster existingCluster = getCluster(clusterId);
    clusters.remove(clusterId);
    clusters.put(clusterId, existingCluster);
  }

  public void flushClusters(Date date) {
    Iterator<Entry<Integer, EntityCluster>> iter = clusters.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<Integer, EntityCluster> nextEntry = iter.next();
      int cid = nextEntry.getKey();
      EntityCluster cluster = nextEntry.getValue();
      if (cluster.checkExpire(date)) {
        dropCluster(cid);
      } else {
        break;
      }
    }
  }

  public void dropCluster(int clusterId) {
    System.out.println(clusterId + " is dropped");
    EntityCluster cluster = clusters.get(clusterId);
    dumpCluster(clusterId, cluster);
    String ct = cluster.getClusterType();
    List<String> mhs = cluster.getMentionHeads();
    clusters.remove(clusterId);
    featureTables.remove(clusterId);

    for (String mh : mhs) {
      Set<Integer> clustersHashed = typeSpecifiedRepresentativeEntity2Cluster.get(ct, mh);
      clustersHashed.remove(clusterId);
    }
  }

  public void dumpCluster(int clusterId, EntityCluster cluster) {
    File outputFile = new File(dumpingFolder, clusterId + ".cls");
    List<String> lines = new ArrayList<String>();

    String headStr = StringUtils.spaceJoiner.join(cluster.getMentionHeads());
    String idStr = StringUtils.spaceJoiner.join(cluster.getEntityIds());
    String lifeSpan = TimeUtils.dateFormat.format(cluster.getFirstSeen()) + " "
            + TimeUtils.dateFormat.format(cluster.getLastSeen());

    lines.add(lifeSpan);
    lines.add(headStr);
    lines.add(idStr);

    try {
      FileUtils.writeLines(outputFile, lines);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void finish() {
    for (Entry<Integer, EntityCluster> clusterEntry : clusters.entrySet()) {
      dumpCluster(clusterEntry.getKey(), clusterEntry.getValue());
    }
  }
}