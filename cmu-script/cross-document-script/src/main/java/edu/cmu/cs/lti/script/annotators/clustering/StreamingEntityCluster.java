/**
 *
 */
package edu.cmu.cs.lti.script.annotators.clustering;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.script.clustering.EntityClusterManager;
import edu.cmu.cs.lti.script.model.FeatureTable;
import edu.cmu.cs.lti.script.runners.StreamingEntityClusteringRunner;
import edu.cmu.cs.lti.script.solr.SolrIndexReader;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.FileUtils;
import edu.cmu.cs.lti.utils.StringUtils;
import edu.cmu.cs.lti.utils.TimeUtils;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author zhengzhongliu
 */
public class StreamingEntityCluster extends AbstractLoggingAnnotator {
    SolrIndexReader reader;

    EntityClusterManager manager;

    double cluster_threshold = 0.5;

    enum FeatureType {
        CONTEXT, SURFACE
    }

    double[] featureWeights = {0.2, 0.8};

    // assign a low IDF to OOV word
    int defaultDocumentFrequency = 1000000;

    @Override
    public void initialize(final UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        try {
            reader = new SolrIndexReader(
                    "/Users/zhengzhongliu/tools/solr-4.7.0/example/solr/collection1/data/index");

            File clusterOut = new File("data/03_entity_clusters");

            File clusterIndexing = new File("data/03_cluster_index");

            FileUtils.ensureDirectory(clusterIndexing);
            FileUtils.ensureDirectory(clusterOut);
            manager = new EntityClusterManager(clusterOut, clusterIndexing, logger);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        startProcessInfo(aJCas);

        String articleName = UimaConvenience.getShortDocumentNameWithOffset(aJCas);

        String datedArticleName = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();


        int numExistingClusters = manager.numOfCluster();

//        System.out.println("Number of clusters " + numExistingClusters);

        String[] datedNameFields = datedArticleName.split("_");
        String dateStr = "";
        Date date = null;
        if (datedNameFields.length == 3) {
            dateStr = datedNameFields[2].substring(0, 8);
            try {
                date = TimeUtils.dateFormat.parse(dateStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

//        logger.info("Article date " + date);

        boolean dateChanged = false;

        if (StreamingEntityClusteringRunner.date != null) {
            if (!StreamingEntityClusteringRunner.date.equals(date)) {
                dateChanged = true;
            }
        }

        StreamingEntityClusteringRunner.date = date;

        if (dateChanged) {
            manager.flushClusters(date);
        }


        Table<EntityMention, EventMention, String> argumentTable = HashBasedTable.create();

//        for (EventMentionArgumentLink argument : JCasUtil.select(aJCas, EventMentionArgumentLink.class)) {
//            argumentTable.put(argument.getArgument(), argument.getEventMention(), argument.getArgumentRole());
//        }

        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            TObjectIntHashMap<String> mentionTypeCount = new TObjectIntHashMap<String>();

//            ArrayListMultimap<String, EventMention> relatedEventMentions = ArrayListMultimap.create();

            for (int i = 0; i < entity.getEntityMentions().size(); i++) {
                EntityMention mention = entity.getEntityMentions(i);
                if (mention.getEntityType() != null) {
                    mentionTypeCount.adjustOrPutValue(mention.getEntityType(), 1, 1);
                }

//                if (argumentTable.containsRow(mention)) {
//                    for (Entry<EventMention, String> headEventMention : argumentTable.row(mention).entrySet()) {
//                        relatedEventMentions.put(headEventMention.getValue(), headEventMention.getKey());
//                    }
//                }
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
//                System.out.println("Streaming in new entity : " + EntityClusterManager.getRepresentativeStr(entity)
//                        + " entity id: " + entity.getId());

                TIntObjectHashMap<FeatureTable> candidateClusterFeatures = manager
                        .getCandidateClusterFeatures(majorityType, entity, numExistingClusters);

                FeatureTable features = getFeatureVector(aJCas, dateStr, entity);

                int bestClusterId = -1;
                if (candidateClusterFeatures != null) {
                    // System.out.println("Number of candidates : " + candidateClusterFeatures.size());
                    bestClusterId = rankClusters(candidateClusterFeatures, features);
                    // System.out.println("Selected " + bestClusterId);
                }

                if (bestClusterId == -1) {
                    manager.createNewCluster(date, features, majorityType, entity, articleName);
                } else {
                    manager.addToExistingCluster(bestClusterId, date, features, majorityType, entity, articleName);
                }
            }
        }

    }

    /**
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
//            System.out.println("Similarity with " + clusterId + " is " + sim);
        }

        if (maxSim > cluster_threshold) {
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

    private FeatureTable getFeatureVector(JCas aJCas, String dateStr, Entity entity) {
        // create feature table
        FeatureTable features = new FeatureTable();

        // context features
        fillContextFeature(features, aJCas, entity);

        // surface features
        fillMentionSurfaceFeature(features, aJCas, entity);

        return features;
    }

    private void fillMentionSurfaceFeature(FeatureTable features, JCas aJCas, Entity entity) {
        String headMentionStr = EntityClusterManager.getRepresentativeStr(entity);

        TObjectIntHashMap<String> skipBigramCounts = new TObjectIntHashMap<String>();
        // System.out.println("head mention " + headMentionStr);
        for (String skipBigram : StringUtils.characterSkipBigram(headMentionStr)) {
            // System.out.println("sb " + skipBigram);
            skipBigramCounts.adjustOrPutValue(skipBigram, 1, 1);
        }

        for (String sb : skipBigramCounts.keySet()) {
            features.addFeature(FeatureType.SURFACE.name(), sb, skipBigramCounts.get(sb));
        }
    }

    private int getMinSentenceDistance(int currentSid, Collection<Integer> occurringSentenceIds) {
        int leftMin = -1;

        for (int occuringSid : occurringSentenceIds) {
            if (currentSid == occuringSid) {
                return 0;
            } else if (currentSid > occuringSid) {
                leftMin = currentSid - occuringSid;
            } else {
                int rightMin = occuringSid - currentSid;
                return rightMin < leftMin ? rightMin : leftMin;
            }
        }

        return leftMin;
    }

    private void fillContextFeature(FeatureTable features, JCas aJCas, Entity entity) {
        Set<StanfordCorenlpToken> mentionWords = new HashSet<StanfordCorenlpToken>();
        for (int i = 0; i < entity.getEntityMentions().size(); i++) {
            EntityMention mention = entity.getEntityMentions(i);
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
                mentionWords.add(token);
                break; // just take first token
            }
        }

        TObjectIntHashMap<StanfordCorenlpToken> mentionRemovedContext = new TObjectIntHashMap<StanfordCorenlpToken>();
        LinkedHashSet<Integer> occurringSentenceIds = new LinkedHashSet<Integer>();

        Collection<Sentence> sentences = JCasUtil.select(aJCas, Sentence.class);
        int numSents = sentences.size();
        for (Sentence sent : sentences) {
            int sid = Integer.parseInt(sent.getId());
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, sent)) {
                if (mentionWords.contains(token)) {
                    occurringSentenceIds.add(sid);
                } else {
                    mentionRemovedContext.put(token, sid);
                }
            }
        }

        TObjectDoubleHashMap<String> weightedTfs = new TObjectDoubleHashMap<String>();
        for (StanfordCorenlpToken token : mentionRemovedContext.keySet()) {
            String t = token.getCoveredText();
            int dist = getMinSentenceDistance(mentionRemovedContext.get(token), occurringSentenceIds);
            double distReverse = dist * 1.0 / numSents;
            double w = 1.0 - (distReverse);
            weightedTfs.adjustOrPutValue(t, w, w);
        }

        for (String term : weightedTfs.keySet()) {
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

            double wtf = weightedTfs.get(term);

            features.addFeature(FeatureType.CONTEXT.name(), term, wtf * idf);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        try {
            manager.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}