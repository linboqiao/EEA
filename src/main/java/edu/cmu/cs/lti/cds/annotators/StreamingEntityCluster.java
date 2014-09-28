/**
 * 
 */
package edu.cmu.cs.lti.cds.annotators;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.cs.lti.cds.clustering.EntityClusterManager;
import edu.cmu.cs.lti.cds.model.FeatureTable;
import edu.cmu.cs.lti.cds.solr.SolrIndexReader;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.StringUtils;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * @author zhengzhongliu
 * 
 */
public class StreamingEntityCluster extends JCasAnnotator_ImplBase {
  SolrIndexReader reader;

  EntityClusterManager manager = new EntityClusterManager();

  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmdd");

  double cluster_threshold = 0.5;

  enum FeatureType {
    CONTEXT, SURFACE
  }

  double[] featureWeights = { 0.2, 0.8 };

  // assign a low IDF to OOV word
  int defaultDocumentFrequency = 1000000;

  @Override
  public void initialize(final UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    try {
      reader = new SolrIndexReader(
              "/Users/zhengzhongliu/tools/solr-4.7.0/example/solr/collection1/data/index");
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    }
  }

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    String articleName = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();
    System.out.println("Processing " + UimaConvenience.getShortDocumentNameWithOffset(aJCas)
            + " _ " + articleName);
    String[] articleNamefields = articleName.split("_");
    String dateStr = "";
    Date date = null;
    if (articleNamefields.length == 3) {
      dateStr = articleNamefields[2].substring(0, 8);
      try {
        date = dateFormat.parse(dateStr);
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }

    for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
      TObjectIntHashMap<String> mentionTypeCount = new TObjectIntHashMap<String>();

      for (int i = 0; i < entity.getEntityMentions().size(); i++) {
        EntityMention mention = entity.getEntityMentions(i);
        if (mention.getEntityType() != null) {
          mentionTypeCount.adjustOrPutValue(mention.getEntityType(), 1, 1);
        }
      }

      String majorityType = "UNKONWN";
      int typeNum = 0;

      for (String type : mentionTypeCount.keySet()) {
        int typeCount = mentionTypeCount.get(type);
        if (typeCount > typeNum) {
          majorityType = type;
          typeNum = typeCount;
        }
      }

      if (majorityType.equals(EntityClusterManager.entityType.ORGANIZATION.name())
      // || majorityType.equals(EntityClusterManager.entityType.LOCATION.name())
              || majorityType.equals(EntityClusterManager.entityType.PERSON.name())) {

        String headMentionStr = entity.getRepresentativeMention().getCoveredText()
                .replace("\n", "");

        String entityId = articleName + "_" + entity.getId();
        TIntObjectHashMap<FeatureTable> candidateClusterFeatures = manager
                .getCandidateClusterFeatures(majorityType, headMentionStr);

        FeatureTable features = getFeatureVector(aJCas, dateStr, headMentionStr);

        int bestClusterId = -1;
        if (candidateClusterFeatures != null) {
          // System.out.println("Number of candidates : " + candidateClusterFeatures.size());
          bestClusterId = rankClusters(candidateClusterFeatures, features);
          // System.out.println("Selected " + bestClusterId);
        }

        if (bestClusterId == -1) {
          manager.createNewCluster(entityId, date, features, majorityType, headMentionStr);
        } else {
          manager.addToExistingCluster(bestClusterId, entityId, date, features);
        }
      }
    }
  }

  /**
   * 
   * @param candidateClusters
   * @return The rank of the selected cluster or -1 as not found
   */
  private int rankClusters(TIntObjectHashMap<FeatureTable> candidateClusters, FeatureTable features) {
    double maxSim = 0;
    int bestClusterId = -1;
    for (int clusterId : candidateClusters.keys()) {
      double sim = clusterSimilarity(candidateClusters.get(clusterId), features);
      if (sim > maxSim) {
        maxSim = sim;
        bestClusterId = clusterId;
      }
      // System.out.println("Similarity with " + clusterId + " is " + sim);
    }

    if (bestClusterId > cluster_threshold) {
      return bestClusterId;
    } else {
      return -1;
    }
  }

  private double clusterSimilarity(FeatureTable t1, FeatureTable t2) {
    double weightedSum = 0;
    for (int i = 0; i < FeatureType.values().length; i++) {
      String ft = FeatureType.values()[i].name();
      double weight = featureWeights[i];
      Map<String, Double> featureMap1 = t1.getFeatures().row(ft);
      Map<String, Double> featureMap2 = t2.getFeatures().row(ft);

      if (ft.equals(FeatureType.CONTEXT.name())) {
        double cos = cosine(featureMap1, featureMap2);
        weightedSum += weight * cos;
        // System.out.println("Cos " + cos);
      } else if (ft.equals(FeatureType.SURFACE.name())) {
        double dice = StringUtils.strDice(featureMap1, featureMap2);
        weightedSum += weight * dice;
        // System.out.println("Dice " + dice);
      }
    }

    return weightedSum;
  }

  private double cosine(Map<String, Double> featureMap1, Map<String, Double> featureMap2) {
    double dotProd = 0;

    double vecLength1 = 0;
    for (Entry<String, Double> feature1 : featureMap1.entrySet()) {
      String featureName = feature1.getKey();
      double val1 = feature1.getValue();
      if (featureMap2.containsKey(featureName)) {
        double val2 = featureMap2.get(featureName);
        dotProd += val1 * val2;
      }
      vecLength1 += val1 * val1;
    }

    // System.out.println("Dot prod " + dotProd);

    double vecLength2 = 0;
    for (Entry<String, Double> feature2 : featureMap2.entrySet()) {
      double val2 = feature2.getValue();
      vecLength2 += val2 * val2;
    }
    if (vecLength1 == 0 || vecLength2 == 0) {
      return 0;
    } else {
      return dotProd / Math.sqrt((vecLength1 * vecLength2));
    }
  }

  private FeatureTable getFeatureVector(JCas aJCas, String dateStr, String headMentionStr) {
    // create feature table
    FeatureTable features = new FeatureTable();

    Collection<StanfordCorenlpToken> fullContext = JCasUtil.select(aJCas,
            StanfordCorenlpToken.class);
    TObjectIntHashMap<String> tfCounts = new TObjectIntHashMap<String>();
    for (StanfordCorenlpToken token : fullContext) {
      String t = token.getCoveredText();
      tfCounts.adjustOrPutValue(t, 1, 1);
    }

    for (String term : tfCounts.keySet()) {
      int df = defaultDocumentFrequency;
      try {
        df = reader.getDocumentFrequency(term);
      } catch (IOException e) {
        e.printStackTrace();
      }

      double idf = 0;
      if (df > 0) {
        idf = 1.0 / df;
      } else {
        idf = 1.0 / defaultDocumentFrequency;
      }

      int tf = tfCounts.get(term);

      // System.out.println("tf is " + tf + " idf is " + idf);

      features.addFeature(FeatureType.CONTEXT.name(), term, tf * idf);
    }

    TObjectIntHashMap<String> skipBigramCounts = new TObjectIntHashMap<String>();
    // System.out.println("head mention " + headMentionStr);
    for (String skipBigram : StringUtils.characterSkipBigram(headMentionStr)) {
      // System.out.println("sb " + skipBigram);
      skipBigramCounts.adjustOrPutValue(skipBigram, 1, 1);
    }

    for (String sb : skipBigramCounts.keySet()) {
      features.addFeature(FeatureType.SURFACE.name(), sb, skipBigramCounts.get(sb));
    }

    return features;
  }
}