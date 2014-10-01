package edu.cmu.cs.lti.cds.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.lti.cds.clustering.EntityClusterManager;
import edu.cmu.cs.lti.script.type.Entity;

public class EntityCluster {
  private Set<String> entityIds = new LinkedHashSet<String>();

  private Date firstSeen;

  private Date lastSeen;

  private int maxUnseenDate = 10;

  // Set of representative mentions for cluster
  private List<String> mentionHeads = new ArrayList<String>();

  private String clusterType;

  public EntityCluster(String entityId, Entity entity, Date date, String clusterType) {
    String headMentionStr = EntityClusterManager.getRepresentativeStr(entity);
    // System.out.println("Adding head " + mentionHead);
    this.mentionHeads.add(headMentionStr);
    this.clusterType = clusterType;
    this.entityIds.add(entityId);
    this.firstSeen = date;
    this.lastSeen = date;
  }

  public String getClusterType() {
    return clusterType;
  }

  public Set<String> getEntityIds() {
    return entityIds;
  }

  public List<String> getMentionHeads() {
    return mentionHeads;
  }

  public void addNewEntity(String id, Date lastSeen, String mentionHead) {
    this.entityIds.add(id);
    if (lastSeen != null) {
      this.lastSeen = lastSeen;
    }
    this.mentionHeads.add(mentionHead);
    System.out.println("Adding new entity to cluster: " + mentionHead);
  }

  public boolean checkExpire(Date currentDate) {
    if (lastSeen == null) {
      return true;
    }
    long diff = currentDate.getTime() - lastSeen.getTime();
    if (diff / (24 * 60 * 60 * 1000) > maxUnseenDate) {
      return true;
    }
    return false;
  }

  public Date getFirstSeen() {
    return firstSeen;
  }

  public Date getLastSeen() {
    return lastSeen;
  }
}
