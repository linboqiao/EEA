package stanford.nlp.jcoref.docclustering;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import stanford.nlp.jcoref.JDictionaries;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.util.CoreMap;

public class SimilarityVector implements Serializable {

  private static final long serialVersionUID = -9201222565921102128L;

  public Counter<String> vector;
  public static final Set<String> stopWords = new HashSet<String>(Arrays.asList(
      ".", ",", "%", "`", "``", "'", "''", "&", "$", "!", "&", "#", "@",
      "a", "an", "the", "of", "at", "on", "upon", "in", "to", "from",
      "out", "as", "so", "such", "or", "and", "those", "this", "these", "that",
      "for", ",", "is", "was", "am", "are", "'s", "been", "were", "be"));

  public SimilarityVector() {
    vector = new ClassicCounter<String>();
  }

  public SimilarityVector(Counter<String> v) {
    this.vector = v;
  }

  public SimilarityVector(String text) {
    this();
    for(String w : text.toLowerCase().split("\\s+")) {
      if(w.endsWith(".")) w = w.substring(0, w.length()-1);
      vector.incrementCount(w);
    }
  }

  /** tokenized text */
  public SimilarityVector(Annotation doc, boolean lemmaIdf) {
    this();
    List<CoreMap> sentences = doc.get(SentencesAnnotation.class);
    for(CoreMap sentence : sentences) {
      for(CoreLabel w : sentence.get(TokensAnnotation.class)) {
        if(isStopWord(w)) continue;
        if(lemmaIdf) vector.incrementCount(w.get(LemmaAnnotation.class));
        else vector.incrementCount(w.get(TextAnnotation.class).toLowerCase());
      }
    }
  }

  @Override
  public String toString(){
    return vector.toString();
  }

  /** remove function word, punctuation, numbers etc */
  private boolean isStopWord(CoreLabel w) {
    String word = (w.containsKey(LemmaAnnotation.class))? w.get(LemmaAnnotation.class) : w.get(TextAnnotation.class);
    if(stopWords.contains(word) || isNumeric(word)) return true;
    else return false;
  }
  public static boolean isNumeric(String str){
    return str.matches("-?\\d+(.\\d+)?");
  }
  
  public static SimilarityVector get1stOrderSimilarityVector(List<CoreLabel> words, JDictionaries dict) {
    SimilarityVector sv = new SimilarityVector();
    for(CoreLabel cl : words) {
      String word = cl.get(LemmaAnnotation.class).toLowerCase();
      sv.vector.incrementCount(word);
    }
    return sv;
  }
  public static SimilarityVector get1stOrderTfIdfSentenceVector(List<CoreLabel> words, JDictionaries dict){
    SimilarityVector sv = get1stOrderSimilarityVector(words, dict);
    TfIdf.applyTfIdf(sv, dict.tfIdf);
    return sv;
  }

  public static SimilarityVector get2ndOrderSimilarityVector(List<CoreLabel> words, JDictionaries dict){
    Map<String, Set<String>> thesaurus;
    SimilarityVector sv = new SimilarityVector();
    for(CoreLabel cl : words) {
      String word = cl.get(LemmaAnnotation.class).toLowerCase();
      String pos = cl.get(PartOfSpeechAnnotation.class);
      if(stopWords.contains(word) || pos.startsWith("PRP")) continue; 
      sv.vector.incrementCount(word);

      if(pos.startsWith("N")) thesaurus = dict.thesaurusNoun;
      else if(pos.startsWith("V")) thesaurus = dict.thesaurusVerb;
      else thesaurus = dict.thesaurusAdj;

      if(thesaurus.containsKey(word)) {
        for(String syn : thesaurus.get(word)) {
          sv.vector.incrementCount(syn);
        }
      }
    }
    return sv;
  }
  public static SimilarityVector get2ndOrderTfIdfSentenceVector(List<CoreLabel> words, JDictionaries dict){
    SimilarityVector sv = get2ndOrderSimilarityVector(words, dict);
    TfIdf.applyTfIdf(sv, dict.tfIdf);
    return sv;
  }

  public static double getCosineSimilarity(SimilarityVector v1, SimilarityVector v2) {
    return Counters.cosine(v1.vector, v2.vector);
  }
}


