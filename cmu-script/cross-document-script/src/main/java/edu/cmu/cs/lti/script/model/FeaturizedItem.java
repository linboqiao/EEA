package edu.cmu.cs.lti.script.model;

import com.google.common.collect.Table;

public abstract class FeaturizedItem {
  public Table<String, String, Integer> featureTable;

  public String docId;

  public int itemId;

  public FeaturizedItem(String docId, int itemId, Table<String, String, Integer> featureTable) {
    this.docId = docId;
    this.itemId = itemId;
    this.featureTable = featureTable;
  }
}
