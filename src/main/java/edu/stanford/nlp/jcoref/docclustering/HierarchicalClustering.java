package edu.stanford.nlp.jcoref.docclustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import edu.stanford.nlp.jcoref.EECBDocument;
//import edu.stanford.nlp.jcoref.EECBReader;
import edu.stanford.nlp.jcoref.docclustering.DocumentClustering.Cluster;
import edu.stanford.nlp.jcoref.docclustering.DocumentClustering.Document;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.util.Pair;

public class HierarchicalClustering {

  TfIdf tfIdf;

  Map<String, Document> docs;

  Map<Integer, Cluster> clusters = new HashMap<Integer, Cluster>();

  Map<Integer, Cluster> clustersForTracingMerge = new HashMap<Integer, Cluster>();

  List<Pair<Integer, Integer>> mergeOrder = new ArrayList<Pair<Integer, Integer>>();

  /**
   * merge count -> modelscore: modelScore.getCount(1) gives score for a model with 1 merging (1 two
   * documents cluster)
   */
  Counter<Integer> modelScore = new ClassicCounter<Integer>();

  /** maintain distances between clusters */
  Counter<Pair<Integer, Integer>> distBetweenClusters = new ClassicCounter<Pair<Integer, Integer>>();

  SimilarityVector metaCentroid;

  /** maximum number of clusters */
  private static final int MAX_CLUSTER = 100;

  private static final boolean VERBOSE = true;

  public HierarchicalClustering() {
    tfIdf = new TfIdf(false);
    docs = new HashMap<String, Document>();
  }

  public HierarchicalClustering(Collection<Document> docs) {
    this();
    for (Document doc : docs) {
      TfIdf.applyTfIdf(doc.docTfIdfVector, this.tfIdf);
      this.docs.put(doc.docID, doc);
    }
  }

  public void initialize() {
    int clusterID = 1;
    for (String docID : docs.keySet()) {
      clusters.put(clusterID, new Cluster(clusterID, docs.get(docID)));
      clustersForTracingMerge.put(clusterID, new Cluster(clusterID, docs.get(docID)));
      clusterID++;
    }
    for (Cluster c1 : clusters.values()) {
      for (Cluster c2 : clusters.values()) {
        if (c1.clusterID < c2.clusterID) {
          distBetweenClusters.incrementCount(
                  new Pair<Integer, Integer>(c1.clusterID, c2.clusterID), distUPGMA(c1, c2));
        }
      }
    }

    int docCount = docs.size();
    metaCentroid = new SimilarityVector();
    for (Document d : docs.values()) {
      Counters.addInPlace(metaCentroid.vector, d.docTfIdfVector.vector);
    }
    Counters.multiplyInPlace(metaCentroid.vector, 1.0 / docCount);
  }

  public static double distUPGMA(Cluster c1, Cluster c2) {
    double sum = 0;
    for (Document d1 : c1.docs) {
      for (Document d2 : c2.docs) {
        sum += 1 - SimilarityVector.getCosineSimilarity(d1.docTfIdfVector, d2.docTfIdfVector);
      }
    }

    return sum / (c1.docs.size() * c2.docs.size());
  }

  /** merge two clusters: merge them into the cluster with smaller id */
  public Cluster mergeClusters(Map<Integer, Cluster> clusters, Cluster c1, Cluster c2) {
    SimilarityVector newCentroid = new SimilarityVector();
    Counters.addInPlace(newCentroid.vector,
            Counters.multiplyInPlace(c1.centroid.vector, c1.docs.size()));
    Counters.addInPlace(newCentroid.vector,
            Counters.multiplyInPlace(c2.centroid.vector, c2.docs.size()));
    Counters.multiplyInPlace(newCentroid.vector, 1.0 / (c1.docs.size() + c2.docs.size()));

    Cluster to;
    Cluster from;
    if (c1.clusterID < c2.clusterID) {
      to = c1;
      from = c2;
    } else {
      to = c2;
      from = c1;
    }

    for (Integer cID : clusters.keySet()) {
      if (cID == c1.clusterID || cID == c2.clusterID)
        continue;
      Pair<Integer, Integer> old1 = (cID < c1.clusterID) ? new Pair<Integer, Integer>(cID,
              c1.clusterID) : new Pair<Integer, Integer>(c1.clusterID, cID);
      Pair<Integer, Integer> old2 = (cID < c2.clusterID) ? new Pair<Integer, Integer>(cID,
              c2.clusterID) : new Pair<Integer, Integer>(c2.clusterID, cID);
      Pair<Integer, Integer> newPair = (cID < to.clusterID) ? new Pair<Integer, Integer>(cID,
              to.clusterID) : new Pair<Integer, Integer>(to.clusterID, cID);
      Pair<Integer, Integer> removePair = (cID < from.clusterID) ? new Pair<Integer, Integer>(cID,
              from.clusterID) : new Pair<Integer, Integer>(from.clusterID, cID);

      double newDist = (distBetweenClusters.getCount(old1) * c1.docs.size() + distBetweenClusters
              .getCount(old2) * c2.docs.size())
              / (c1.docs.size() + c2.docs.size());
      distBetweenClusters.setCount(newPair, newDist);
      distBetweenClusters.remove(removePair);
    }

    to.docs.addAll(from.docs);
    clusters.remove(from.clusterID);
    Pair<Integer, Integer> mergePair = new Pair<Integer, Integer>(to.clusterID, from.clusterID);
    distBetweenClusters.remove(mergePair);
    mergeOrder.add(mergePair);
    to.centroid = newCentroid;
    Cluster.calculateWithinDist(to);
    return to;
  }

  public void calculateModelScores() {
    double B = 0;
    double W = 0;
    for (Cluster c : clustersForTracingMerge.values()) {
      B += Math.pow(1 - SimilarityVector.getCosineSimilarity(c.centroid, metaCentroid), 2);
    }
    Pair<Integer, Integer> closest = null;
    while ((closest = Counters.argmin(distBetweenClusters)) != null) {
      Cluster c1 = clustersForTracingMerge.get(closest.first());
      Cluster c2 = clustersForTracingMerge.get(closest.second());
      B -= c1.docs.size()
              * Math.pow(1 - SimilarityVector.getCosineSimilarity(c1.centroid, metaCentroid), 2);
      B -= c2.docs.size()
              * Math.pow(1 - SimilarityVector.getCosineSimilarity(c2.centroid, metaCentroid), 2);
      W -= c1.withinDist;
      W -= c2.withinDist;

      Cluster newCluster = mergeClusters(clustersForTracingMerge, c1, c2);

      B += newCluster.docs.size()
              * Math.pow(
                      1 - SimilarityVector.getCosineSimilarity(newCluster.centroid, metaCentroid),
                      2);
      W += newCluster.withinDist;

      double C = B * (docs.size() - clustersForTracingMerge.size())
              / (W * (clustersForTracingMerge.size() - 1));
      modelScore.incrementCount(mergeOrder.size(), C);
    }

  }

  public Map<Integer, Cluster> getBestModel() {
    if (VERBOSE) {
      for (int i = 1; i < modelScore.size(); i++) {
        System.err.println("merge " + i + ", score: " + modelScore.getCount(i));
      }
    }

    int totalMerge = modelScore.size();
    modelScore.remove(modelScore.size());
    for (int i = 1; i < totalMerge - MAX_CLUSTER || i < totalMerge / 2; i++) {
      modelScore.remove(i);
    }

    int mergeCount = Counters.argmax(modelScore);
    for (int c = 0; c < mergeCount; c++) {
      Cluster c1 = clusters.get(mergeOrder.get(c).first());
      Cluster c2 = clusters.get(mergeOrder.get(c).second());
      mergeClusters(clusters, c1, c2);
    }
    return clusters;
  }

  public static Map<Integer, Cluster> getBestModel(Collection<Document> docs) {
    HierarchicalClustering hc = new HierarchicalClustering(docs);
    if (VERBOSE) {
      System.err.println("HAC prepared");
    }
    hc.initialize();
    if (VERBOSE) {
      System.err.println("HAC initialzed,calcuating model scores");
    }
    hc.calculateModelScores();
    if (VERBOSE) {
      System.err.println("Done");
    }
    return hc.getBestModel();
  }

  // public static void main(String[] args) {
  // String jcbPath = "/scr/heeyoung/corpus/coref/jcoref/jcb_v0.1/";
  // EECBReader jcbReader = new EECBReader(jcbPath);
  // Set<Document> jcbDocs = new HashSet<Document>();
  //
  // EECBDocument doc;
  // while((doc = jcbReader.nextDoc())!=null) {
  // Document d = new Document(doc.docID, doc.annotation);
  // jcbDocs.add(d);
  // }
  // HierarchicalClustering hc = new HierarchicalClustering(jcbDocs);
  // hc.initialize();
  // hc.calculateModelScores();
  // for(int i = 1 ; i < hc.modelScore.size() ; i++) {
  // System.err.println(i+" -> "+hc.modelScore.getCount(i));
  // }
  // Map<Integer, Cluster> bestModel = hc.getBestModel();
  // for(Cluster c : bestModel.values()) {
  // System.err.println(c);
  // }
  // }
}
