package edu.cmu.cs.lti.cds.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class EntityCluster {
  Set<String> entityIds = new HashSet<String>();

  Date lastSeen = null;

  public void addNewEntity(String id, Date lastSeen) {
    this.entityIds.add(id);
    if (lastSeen != null) {
      this.lastSeen = lastSeen;
    }
  }

  public boolean checkExpire(Date currentDate) {
    if (lastSeen == null) {
      return true;
    }
    long diff = currentDate.getTime() - lastSeen.getTime();
    if (diff / (24 * 60 * 60 * 1000) > 10) {
      return true;
    }
    return false;
  }
}
