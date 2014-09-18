package edu.cmu.cs.lti.utils;

import java.io.File;
import java.io.IOException;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import au.com.bytecode.opencsv.CSVReader;
import edu.cmu.cs.lti.uima.util.CsvFactory;

public class CsvFeatureParser {
  public CsvFeatureParser() {
    
  }

  public Table<String, String, Integer> getFeatures(File file) throws IOException {
    CSVReader reader = CsvFactory.getCSVReader(file);
    String[] nextLine;
    Table<String, String, Integer> featureTable = HashBasedTable.create();
    while ((nextLine = reader.readNext()) != null) {
      for (String entry : nextLine) {
        String[] f = entry.split(":");
        if (f.length == 3) {
          featureTable.put(f[0], f[1], Integer.parseInt(f[2]));
        } else {
          System.err.println("Incorrect feature : " + entry);
        }
      }
    }
    return featureTable;
  }
  
}
