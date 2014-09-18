package edu.stanford.nlp.jcoref.docclustering;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//import edu.stanford.nlp.jcoref.EECBDocument;
//import edu.stanford.nlp.jcoref.EECBReader;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.stats.TwoDimensionalCounter;

/** Implementation of Mihai's Document Clustering algorithm (A Hybrid Unsupervised Approach for Document Clustering)
 * 
 * @author heeyoung
 *
 */
public class DocumentClustering {

  private static final double THRES_CONVERGE = 0.01;
  private static final boolean VERBOSE = true;
  private static boolean converged = true;   // skip EM for speed if true

  public static class Document {
    public String docID;
    Annotation doc;
    SimilarityVector docTfIdfVector;
    SimilarityVector docVector;

    public Document(String docID, String text) {
      this.docID = docID;
      this.doc = new Annotation(text);
      docVector = new SimilarityVector(text);
      docTfIdfVector = new SimilarityVector(text);
    }
    public Document(String docID, Annotation annotation) {
      this.docID = docID;
      this.doc = annotation;
      this.docVector = new SimilarityVector(doc, false);
      this.docTfIdfVector = new SimilarityVector(doc, false);
    }
    @Override
    public String toString() {
      return "Doc "+docID;
    }
  }
  public static class Cluster {
    int clusterID;
    public Set<Document> docs;
    SimilarityVector centroid;
    double withinDist;

    public Cluster(int id, Document d) {
      clusterID = id;
      docs = new HashSet<Document>();
      docs.add(d);
      centroid = new SimilarityVector(Counters.getCopy(d.docTfIdfVector.vector));
      withinDist = 0;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Cluster ").append(clusterID).append(" -> {");
      for(Document d : docs){
        sb.append(d.docID).append(", ");
      }
      sb.append("}");
      return sb.toString();
    }

    public static void calculateWithinDist(Cluster c) {
      c.withinDist = 0;
      for(Document d : c.docs) {
        c.withinDist += Math.pow(1 - SimilarityVector.getCosineSimilarity(d.docTfIdfVector, c.centroid), 2);
      }
    }
  }

  public static Map<Integer, Cluster> getInitialModel(Collection<Document> docs) {
    return HierarchicalClustering.getBestModel(docs);
  }

  // main API entry point
  public static Map<Integer, Cluster> getDocumentClusters(Collection<Document> docs) {
    Map<Integer, Cluster> initialModel = getInitialModel(docs);
    Map<Integer, Cluster> finalModel = initialModel;
    int totalDocSize = docs.size();
    Set<String> vocabulary = new HashSet<String>();

    // P(category|theta)
    Counter<Integer> prob_c = new ClassicCounter<Integer>();

    // P(word|category) -> counter<category, word>
    TwoDimensionalCounter<Integer, String> logProb_w_c = new TwoDimensionalCounter<Integer, String>();

    // P(category|document) -> counter<DocID, category>
    TwoDimensionalCounter<String, Integer> prob_c_d = new TwoDimensionalCounter<String, Integer>();

    // parameter initialization
    initialize(initialModel, prob_c_d, vocabulary);

    if(VERBOSE) {
      for(Cluster c : initialModel.values()) {
        System.err.println(c.clusterID + ": "+c.docs.size());
      }
      for(Cluster c : initialModel.values()) {
        System.err.println(c);
      }
      System.err.println("========================================================================");
    }
    // EM algorithm
    int i=0;
    while(!converged) {

      // M-step
      calculateProbCategory(prob_c, prob_c_d, initialModel, totalDocSize);
      calculateProbWordGivenCategory(prob_c, logProb_w_c, prob_c_d, docs, vocabulary);

      // E-step
      converged = calculateProbCategoryGivenDoc(prob_c, logProb_w_c, prob_c_d, docs);

      finalModel = getBestCategories(prob_c_d, docs);

      if(VERBOSE) {
        for(Cluster c : finalModel.values()) {
          System.err.println(c);
        }
        System.err.println((i++) + " ========================================================================");
      }
    }

    return finalModel;
  }

  private static Map<Integer, Cluster> getBestCategories(TwoDimensionalCounter<String, Integer> prob_c_d, Collection<Document> docs) {
    Map<Integer, Cluster> bestModel = new HashMap<Integer, Cluster>();
    for(Document doc : docs){
      String docID = doc.docID;
      Counter<Integer> probCategoryOneDoc = prob_c_d.getCounter(docID);
      int category = Counters.argmax(probCategoryOneDoc);
      if(!bestModel.containsKey(category)) bestModel.put(category, new Cluster(category, doc));
      else bestModel.get(category).docs.add(doc);
    }
    return bestModel;
  }

  private static void printBestCategoryForDoc(TwoDimensionalCounter<String, Integer> prob_c_d) {
    for(String docID : prob_c_d.firstKeySet()) {
      Counter<Integer> probCategoryOneDoc = prob_c_d.getCounter(docID);
      System.err.println(docID + " -> "+Counters.argmax(probCategoryOneDoc));
    }
  }

  private static void initialize(
      Map<Integer, Cluster> initialModel,
      TwoDimensionalCounter<String, Integer> prob_c_d,
      Set<String> vocabulary) {

    for(int clusterID : initialModel.keySet()) {
      for(Document d : initialModel.get(clusterID).docs) {
        prob_c_d.setCount(d.docID, clusterID, 1);
        vocabulary.addAll(d.docVector.vector.keySet());
      }
    }
  }
  private static void calculateProbCategory(
      Counter<Integer> prob_c,
      TwoDimensionalCounter<String, Integer> prob_c_d,
      Map<Integer, Cluster> initialModel, int totalDocSize) {

    Counter<Integer> prob_c_sumD = new ClassicCounter<Integer>();
    for(String docID : prob_c_d.firstKeySet()) {
      prob_c_sumD.addAll(prob_c_d.getCounter(docID));
    }
    for(int clusterID : initialModel.keySet()) {
      prob_c.setCount(clusterID, (1.0+prob_c_sumD.getCount(clusterID))/(initialModel.size()+totalDocSize));
    }
  }

  private static void calculateProbWordGivenCategory(
      Counter<Integer> prob_c,
      TwoDimensionalCounter<Integer, String> logProb_w_c,
      TwoDimensionalCounter<String, Integer> prob_c_d,
      Collection<Document> docs, Set<String> vocabulary) {
    for(int clusterID : prob_c.keySet()) {
      Counter<String> logProb_w_ci = logProb_w_c.getCounter(clusterID);
      for(String word : vocabulary) {
        logProb_w_ci.setCount(word, 1);
        for(Document doc : docs) {
          logProb_w_ci.incrementCount(word, doc.docVector.vector.getCount(word)*prob_c_d.getCount(doc.docID, clusterID));
        }
      }
      Counters.logInPlace(logProb_w_ci);
      Counters.logNormalizeInPlace(logProb_w_ci);
    }
  }
  private static boolean calculateProbCategoryGivenDoc(
      Counter<Integer> prob_c,
      TwoDimensionalCounter<Integer, String> logProb_w_c,
      TwoDimensionalCounter<String, Integer> prob_c_d,
      Collection<Document> docs) {
    double diff = 0;
    boolean decisionChanged = false;

    for(Document doc : docs) {
      Counter<Integer> oldProb_c_di = prob_c_d.getCounter(doc.docID);
      int previousDecision = Counters.argmax(oldProb_c_di);
      Counter<Integer> newProb_c_di = new ClassicCounter<Integer>();

      for(int clusterID : prob_c.keySet()) {
        newProb_c_di.setCount(clusterID, Math.log(prob_c.getCount(clusterID)));
        for(String word : doc.docVector.vector.keySet()) {
          newProb_c_di.incrementCount(clusterID, logProb_w_c.getCount(clusterID, word));
        }
      }
      Counters.logNormalizeInPlace(newProb_c_di);
      Counters.expInPlace(newProb_c_di);

      // TODO
      // calculate diff
      Counters.subtractInPlace(oldProb_c_di, newProb_c_di);
      diff += Counters.L2Norm(oldProb_c_di);
      if(VERBOSE) System.err.println("diff: "+diff);

      if(previousDecision != Counters.argmax(newProb_c_di)) decisionChanged = true;
      prob_c_d.setCounter(doc.docID, (ClassicCounter<Integer>) newProb_c_di);
    }
    if(diff/docs.size() < THRES_CONVERGE && !decisionChanged) return true;
    return false;
  }
  
//  // wrapper for JCBDocuments
//  public static Map<Integer, Cluster> getDocumentClusters(Map<String, EECBDocument> docs) {
//    Set<Document> docsForClustering = new HashSet<Document>();
//    for(EECBDocument doc : docs.values()){
//      Document d = new Document(doc.docID, doc.annotation);
//      docsForClustering.add(d);
//    }
//    Map<Integer, Cluster> docClusters = getDocumentClusters(docsForClustering);
//    return docClusters;
//  }

//  /** return gold document clusters: for debug/analysis purpose */
//  public static Map<Integer, Cluster> getGoldDocumentClusters(Map<String, EECBDocument> docs){
//    Map<Integer, Cluster> docClusters = new HashMap<Integer, Cluster>();
//    for(EECBDocument doc : docs.values()){
//      Document d = new Document(doc.docID, doc.annotation);
//      int clusterID = Integer.parseInt(doc.docID.split("-")[0]);
//      if(docClusters.containsKey(clusterID)) {
//        docClusters.get(clusterID).docs.add(d);
//      } else {
//        docClusters.put(clusterID, new Cluster(clusterID++, d));
//      }
//    }
//    return docClusters;
//  }

//  public static void main(String[] args) {
//    String jcbPath = "/scr/heeyoung/corpus/coref/jcoref/jcb_v0.2/";
//    EECBReader jr = new EECBReader(jcbPath);
//    Map<String, EECBDocument> inputDocs = jr.readInputFiles("SRL_PATH_HERE");
//        Map<Integer, Cluster> clusters = getDocumentClusters(inputDocs);
////    Map<Integer, Cluster> clusters = getGoldDocumentClusters(inputDocs);
//
//    System.err.println();
//
//    //    String jcbPath = "/scr/heeyoung/corpus/coref/jcoref/jcb_v0.1/";
//    //    JCBReader jcbReader = new JCBReader(jcbPath);
//    //    Set<Document> jcbDocs = new HashSet<Document>();
//    //
//    //    JCBDocument doc;
//    //    while((doc = jcbReader.nextDoc())!=null) {
//    //      Document d = new Document(doc.docID, doc.annotation);
//    //      jcbDocs.add(d);
//    //    }
//    //    Map<Integer, Cluster> finalClusters = getDocumentClusters(jcbDocs);
//    //
//    //    System.err.println();
//  }
}
