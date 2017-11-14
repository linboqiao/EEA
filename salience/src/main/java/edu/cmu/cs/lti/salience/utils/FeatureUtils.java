package edu.cmu.cs.lti.salience.utils;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

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
    public static final String sparsePrefix = "Sparse";

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

    public static List<SimpleInstance> getEventInstances(ArticleComponent component,
                                                         int[] eventSaliency, LookupTable.SimCalculator simCalculator) {
        List<SimpleInstance> instances = new ArrayList<>();

        Map<EventMention, Integer> mentionSentenceIds = new HashMap<>();
        int sentIndex = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.selectCovered(StanfordCorenlpSentence.class, component)) {
            for (EventMention eventMention : JCasUtil.selectCovered(EventMention.class, sentence)) {
                mentionSentenceIds.put(eventMention, sentIndex);
            }
            sentIndex++;
        }

        TObjectIntMap<String> tokenCount = new TObjectIntHashMap<>();
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, component)) {
            tokenCount.adjustOrPutValue(SalienceUtils.getCanonicalToken(token), 1, 1);
        }

        int index = 0;

        List<EventMention> allEventMentions = JCasUtil.selectCovered(EventMention.class, component);

        for (EventMention eventMention : allEventMentions) {
            SimpleInstance instance = new SimpleInstance();

            instance.label = eventSaliency[index];
            instance.instanceName = Integer.toString(index);

            double votingScore = 0;
            for (EventMention evm : allEventMentions) {
                if (evm != eventMention) {
                    votingScore += simCalculator.getSimilarity(eventMention.getCoveredText(), evm.getCoveredText());
                }
            }

            // Normalize by length.
            if (allEventMentions.size() > 1) {
                votingScore /= (allEventMentions.size() - 1);
            }

            StanfordCorenlpToken targetWord = UimaNlpUtils.findHeadFromStanfordAnnotation(eventMention);
            String targetLex = targetWord == null ? TextUtils.asTokenized(eventMention) :
                    SalienceUtils.getCanonicalToken(targetWord);
            instance.featureMap.put(lexicalPrefix + "Head_" + targetLex, 1.0);
            instance.featureMap.put("SentenceLoc", (double) mentionSentenceIds.getOrDefault(eventMention, 0));
            instance.featureMap.put(sparsePrefix + "FrameName_" + eventMention.getFrameName(), 1.0);
            instance.featureMap.put("HeadCount", (double) tokenCount.get(targetLex));
            instance.featureMap.put("EmbeddingVoting", votingScore);

            index++;

            instances.add(instance);
        }
        return instances;
    }

    public static List<SimpleInstance> getKbInstances(JCas aJCas, Set<String> entitySalience,
                                                      LookupTable.SimCalculator simCalculator) {
        List<SimpleInstance> instances = new ArrayList<>();

        Map<ComponentAnnotation, Integer> sentenceIds = new HashMap<>();
        SalienceUtils.MergedClusters clusters = SalienceUtils.getBodyCorefeEntities(aJCas, sentenceIds);

        Body body = JCasUtil.selectSingle(aJCas, Body.class);

        TObjectIntMap<String> tokenCount = new TObjectIntHashMap<>();
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, body)) {
            tokenCount.adjustOrPutValue(SalienceUtils.getCanonicalToken(token), 1, 1);
        }

        Collection<StanfordCorenlpSentence> sentences = JCasUtil.selectCovered(StanfordCorenlpSentence.class, body);

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

            // Normalize by length.
            if (allKbs.size() > 1) {
                votingScore /= (allKbs.size() - 1);
            }

            int isNamedEntity = 0;

            for (Integer entityId : kbEntry.getValue()) {
                List<ComponentAnnotation> mentions = clusters.getClusters().get(entityId);
                for (ComponentAnnotation mention : mentions) {
                    int sentenceAppear;
                    if (sentenceIds.containsKey(mention)) {
                        sentenceAppear = sentenceIds.get(mention);
                    } else {
                        sentenceAppear = sentences.size();
                    }

                    if (sentenceAppear <= firstLoc) {
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

            int salience = entitySalience.contains(kbid) ? 1 : 0;
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
