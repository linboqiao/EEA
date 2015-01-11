package stanford.nlp.jcoref.docclustering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;

public class TfIdf {

  private static final double IDF_UNKNOWN = 0.1; // little bigger than the score of df=1

  public Map<String, Double> idfScore = new HashMap<String, Double>();

  private static final String defaultLemmaIdfFile = "lemma_idf";

  private static final String defaultIdfFile = "idf";

  public TfIdf() {
    loadIdfScore(defaultLemmaIdfFile);
  }

  public TfIdf(boolean lemmaIdf) {
    if (lemmaIdf)
      loadIdfScore(defaultLemmaIdfFile);
    else
      loadIdfScore(defaultIdfFile);
  }

  /** load IDF score */
  private void loadIdfScore(String file) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(
              IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(file)));
      while (reader.ready()) {
        String[] split = reader.readLine().split("\t");
        if (split.length < 4)
          continue;
        idfScore.put(split[0], Double.parseDouble(split[3]));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeIgnoringExceptions(reader);
    }
  }

  /** for now, log scale tf used */
  public static Counter<String> tfVector(SimilarityVector v) {
    return Counters.tfLogScale(v.vector, Math.E);
  }

  public static void multiplyIdf(SimilarityVector v, TfIdf tfIdf) {
    for (String term : v.vector.keySet()) {
      double idf = tfIdf.idfScore.containsKey(term) ? tfIdf.idfScore.get(term) : IDF_UNKNOWN;
      v.vector.setCount(term, v.vector.getCount(term) * idf);
    }
  }

  public static void applyTfIdf(SimilarityVector v, TfIdf tfIdf) {
    v.vector = tfVector(v);
    multiplyIdf(v, tfIdf);
  }

}
