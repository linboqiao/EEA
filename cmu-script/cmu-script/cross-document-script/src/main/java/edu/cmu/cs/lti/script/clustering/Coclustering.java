package edu.cmu.cs.lti.script.clustering;

import edu.cmu.cs.lti.script.model.FeaturizedItem;
import edu.cmu.cs.lti.script.utils.CsvFeatureParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

public class Coclustering {

  public static void main(String[] args) throws IOException {
    List<String> targetFiles = FileUtils.readLines(new File("data/simpson_set"));

    final Set<String> targetFileNames = new HashSet<String>(targetFiles);

    File entityFeatureDir = new File("data/02_entity_features");
    File eventFeatureDir = new File("data/02_event_features");
    File linkingDir = new File("data/02_entity_event_links");

    FilenameFilter simpsonFilter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return targetFileNames.contains(FilenameUtils.getBaseName(name));
      }
    };

    File[] entityFiles = entityFeatureDir.listFiles(simpsonFilter);
    File[] eventFiles = eventFeatureDir.listFiles(simpsonFilter);
    File[] linkingFiles = linkingDir.listFiles(simpsonFilter);

    Arrays.sort(entityFiles);
    Arrays.sort(eventFiles);
    Arrays.sort(linkingFiles);

    CsvFeatureParser fp = new CsvFeatureParser();
    List<List<FeaturizedItem>> entityFeaturesByDoc = new ArrayList<List<FeaturizedItem>>();
    List<List<FeaturizedItem>> eventFeaturesByDoc = new ArrayList<List<FeaturizedItem>>();
    for (int i = 0; i < entityFiles.length; i++) {
      fp.intialize(linkingFiles[i], entityFiles[i], eventFiles[i]);
      entityFeaturesByDoc.add(fp.entityTable);
      eventFeaturesByDoc.add(fp.eventTable);
    }
  }
}
