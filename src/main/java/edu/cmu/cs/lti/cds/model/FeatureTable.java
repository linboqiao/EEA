package edu.cmu.cs.lti.cds.model;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

public class FeatureTable {

  private Table<String, String, Double> featureTable = HashBasedTable.create();

  private int size = 0;

  public void addFeature(String featureType, String featureName, double featureValue) {
    featureTable.put(featureType, featureName, featureValue);
    size++;
  }

  public Table<String, String, Double> getFeatures() {
    return featureTable;
  }

  public int getSize() {
    return size;
  }

  public void mergeWith(FeatureTable tableToMerge) {
    int additionalSize = tableToMerge.getSize();

    for (Cell<String, String, Double> cell : tableToMerge.getFeatures().cellSet()) {
      String row = cell.getRowKey();
      String col = cell.getColumnKey();
      double fVal = cell.getValue();
      if (featureTable.contains(row, col)) {
        featureTable.put(row, col, (fVal * additionalSize + featureTable.get(row, col) * size)
                / (size + additionalSize));
      } else {
        featureTable.put(row, col, fVal);
      }
    }

    size += additionalSize;
  }
}