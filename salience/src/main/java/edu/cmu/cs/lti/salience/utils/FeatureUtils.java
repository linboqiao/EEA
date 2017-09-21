package edu.cmu.cs.lti.salience.utils;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/20/17
 * Time: 4:54 PM
 *
 * @author Zhengzhong Liu
 */
public class FeatureUtils {
    public static final String lexicalPrefix = "Lexical";

    public static class SimpleInstance {
        String instanceName;

        Map<String, Double> featureMap;

        int label;

        public SimpleInstance() {
            featureMap = new HashMap<>();
        }

        public Map<String, Double> getFeatureMap() {
            return featureMap;
        }

        public int getLabel() {
            return label;
        }

        public String getInstanceName() {
            return instanceName;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(instanceName).append("\t").append(label);
            featureMap.keySet().stream().sorted().forEach(f ->
                    sb.append("\t").append(String.format("%s:%.5f", f, featureMap.get(f)))
            );
            return sb.toString();
        }

        public List<String> getFeatureNames() {
            List<String> allFeatures = new ArrayList<>();
            List<String> lexicalFeatures = new ArrayList<>();
            featureMap.keySet().stream().sorted().forEach(f -> {
                if (!f.startsWith("Lexical")) {
                    allFeatures.add(f);
                } else {
                    String lexicalType = f.split("_")[0];
                    lexicalFeatures.add(lexicalType);
                }
            });
            allFeatures.addAll(lexicalFeatures);
            return allFeatures;
        }
    }


    private static Map<Word, Integer> loadSentenceIds(Collection<StanfordCorenlpSentence> sentences) {
        Map<Word, Integer> sentenceIds = new HashMap<>();
        int sentenceIndex = 0;
        for (StanfordCorenlpSentence sentence : sentences) {
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence)) {
                sentenceIds.put(token, sentenceIndex);
            }
            sentenceIndex++;
        }
        return sentenceIds;
    }

    public static List<SimpleInstance> getKbInstances(JCas aJCas, LookupTable.SimCalculator simCalculator) {
        List<SimpleInstance> instances = new ArrayList<>();

        Set<String> abstractEntities = SalienceUtils.getAbstractEntities(aJCas);

        SalienceUtils.MergedClusters clusters = SalienceUtils.getBodyCorefeEntities(aJCas);
        Body body = JCasUtil.selectSingle(aJCas, Body.class);

        TObjectIntMap<String> tokenCount = new TObjectIntHashMap<>();
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, body)) {
            tokenCount.adjustOrPutValue(SalienceUtils.getCanonicalToken(token), 1, 1);
        }

        Collection<StanfordCorenlpSentence> sentences = JCasUtil.selectCovered(StanfordCorenlpSentence.class, body);
        Map<Word, Integer> sentenceIds = loadSentenceIds(sentences);

        Set<String> allKbs = clusters.kbidEntities.keySet();
        for (Map.Entry<String, Collection<Integer>> kbEntry : clusters.kbidEntities.asMap().entrySet()) {
            SimpleInstance instance = new SimpleInstance();

            int firstLoc = sentences.size();
            int numMention = 0;
            ComponentAnnotation firstMention = null;
            String kbid = kbEntry.getKey();

            double votingScore = 0;
            for (String kb : allKbs) {
                if (!kb.equals(kbid)) {
                    votingScore += simCalculator.getSimilarity(kbid, kb);
                }
            }

            int isNamedEntity = 0;
            for (Integer entityId : kbEntry.getValue()) {
                List<ComponentAnnotation> mentions = clusters.getClusters().get(entityId);
                for (ComponentAnnotation mention : mentions) {
                    StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
                    int sentenceAppear = sentenceIds.get(head);
                    if (sentenceAppear < firstLoc) {
                        firstLoc = sentenceAppear;
                        firstMention = mention;
                    }

                    if (mention instanceof StanfordEntityMention) {
                        StanfordEntityMention stanfordMention = (StanfordEntityMention) mention;
                        if (stanfordMention.getEntityType() != null) {
                            if (!stanfordMention.getEntityType().equals("DATE") &&
                                    !stanfordMention.getEntityType().equals("NUMBER")) {
                                isNamedEntity = 1;
                            }
                        }
                    }
                }
                numMention += mentions.size();
            }

            int salience = abstractEntities.contains(kbid) ? 1 : 0;
            StanfordCorenlpToken firstHeadWord = UimaNlpUtils.findHeadFromStanfordAnnotation(firstMention);
            String firstHeadLex = SalienceUtils.getCanonicalToken(firstHeadWord);

            instance.instanceName = kbid;
            instance.label = salience;
            instance.featureMap.put("EmbeddingVoting", votingScore);
            instance.featureMap.put("IsNamedEntity", (double) isNamedEntity);
            instance.featureMap.put("FirstLoc", (double) firstLoc);
            instance.featureMap.put("MentionsCount", (double) bucket(numMention));
            instance.featureMap.put("HeadCount", (double) bucket(tokenCount.get(firstHeadLex)));
            instance.featureMap.put(lexicalPrefix + "Head_" + firstHeadLex, 1.0);
            instances.add(instance);
        }
        return instances;
    }

    private static int bucket(int number) {
        return (int) Math.round(Math.log(10 * (number + 1)));
    }
}
