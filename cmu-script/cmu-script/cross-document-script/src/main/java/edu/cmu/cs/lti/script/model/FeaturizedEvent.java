package edu.cmu.cs.lti.script.model;

import com.google.common.collect.Table;

public class FeaturizedEvent extends FeaturizedItem {

  public FeaturizedEvent(String docId, int itemId, Table<String, String, Integer> featureTable) {
    super(docId, itemId, featureTable);
  }

}
