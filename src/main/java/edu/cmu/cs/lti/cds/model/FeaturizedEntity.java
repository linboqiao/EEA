package edu.cmu.cs.lti.cds.model;

import com.google.common.collect.Table;

public class FeaturizedEntity extends FeaturizedItem {

  public FeaturizedEntity(String docId, int itemId, Table<String, String, Integer> featureTable) {
    super(docId, itemId, featureTable);
    // TODO Auto-generated constructor stub
  }

}
