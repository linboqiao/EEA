package edu.cmu.cs.lti.cds.clustering;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import edu.cmu.cs.lti.io.GigawordTextReader;
import edu.stanford.nlp.jcoref.docclustering.DocumentClustering;
import edu.stanford.nlp.jcoref.docclustering.DocumentClustering.Cluster;
import edu.stanford.nlp.jcoref.docclustering.DocumentClustering.Document;

public class EMDocumentClusterer {
  public static void main(String[] args) throws FileNotFoundException, IOException {
    GigawordTextReader reader = new GigawordTextReader("data/01_plain_text");
    System.out.println("Reading documents");
    List<Document> documents = reader.getDocumentForClustering();
    System.out.println("Document read, starting clustering");
    Map<Integer, Cluster> clusters = DocumentClustering.getDocumentClusters(documents);
    System.out.println("Clustering done");

    File outputFile = new File("data/clustering_out");

    List<String> results = new ArrayList<String>();
    for (Entry<Integer, Cluster> cluster : clusters.entrySet()) {
      StringBuilder b = new StringBuilder();
      b.append(Integer.toString(cluster.getKey()));
      Cluster c = cluster.getValue();
      for (Document d : c.docs) {
        b.append(" " + d.docID);
      }
      results.add(b.toString());
    }

    FileUtils.writeLines(outputFile, results);

  }
}
