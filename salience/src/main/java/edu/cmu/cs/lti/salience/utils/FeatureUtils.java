package edu.cmu.cs.lti.salience.utils;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

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
    }

    public static List<SimpleInstance> getEventInstances(ArticleComponent component,
                                                         List<SimpleInstance> entityInstances,
                                                         int[] eventSaliency, LookupTable.SimCalculator simCalculator) {
        Map<String, SimpleInstance> instanceByKbId = new HashMap<>();
        for (SimpleInstance entityInstance : entityInstances) {
            instanceByKbId.put(entityInstance.instanceName, entityInstance);
        }

        Map<StanfordCorenlpToken, String> kbidByHead = new HashMap<>();
        for (GroundedEntity groundedEntity : JCasUtil.selectCovered(GroundedEntity.class, component)) {
            StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(groundedEntity);
            if (head != null) {
                kbidByHead.put(head, groundedEntity.getKnowledgeBaseId());
            }
        }

        List<SimpleInstance> instances = new ArrayList<>();

        Map<EventMention, Integer> mentionSentenceIds = new HashMap<>();
        ArrayListMultimap<EventMention, SimpleInstance> sameSentInstances = ArrayListMultimap.create();
        ArrayListMultimap<EventMention, SimpleInstance> argumentInstances = ArrayListMultimap.create();

        int sentIndex = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.selectCovered(StanfordCorenlpSentence.class, component)) {
            List<GroundedEntity> groundedEntities = JCasUtil.selectCovered(GroundedEntity.class, sentence);

            for (EventMention eventMention : JCasUtil.selectCovered(EventMention.class, sentence)) {
                // Record the sentence id of each event mention.
                mentionSentenceIds.put(eventMention, sentIndex);

                // Find the entities in each sentence for each event.
                for (GroundedEntity groundedEntity : groundedEntities) {
                    SimpleInstance entityInstance = instanceByKbId.get(groundedEntity.getKnowledgeBaseId());
                    if (entityInstance != null) {
                        sameSentInstances.put(eventMention, entityInstance);
                    }
                }

                // Find the argument entity for each event.
                FSList arguments = eventMention.getArguments();
                if (arguments != null) {
                    for (EventMentionArgumentLink argument : FSCollectionFactory.create(arguments,
                            EventMentionArgumentLink.class)) {
                        EntityMention argumentMention = argument.getArgument();
                        String entityKbId = kbidByHead.get(argumentMention.getHead());
                        if (entityKbId != null) {
                            SimpleInstance entityInstance = instanceByKbId.get(entityKbId);
                            if (entityInstance != null) {
                                argumentInstances.put(eventMention, entityInstance);
                            }
                        }
                    }
                }
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
            SimpleInstance eventInstance = new SimpleInstance();

            eventInstance.label = eventSaliency[index];
            eventInstance.instanceName = Integer.toString(index);

            String curr = SalienceUtils.getCanonicalToken(UimaNlpUtils.findHeadFromStanfordAnnotation(eventMention));

            // Calculate the average voting from the other event to this event.
            double eventVotingScore = 0;
            for (EventMention evm : allEventMentions) {
                if (evm != eventMention) {
                    String other = SalienceUtils.getCanonicalToken(UimaNlpUtils.findHeadFromStanfordAnnotation(evm));
                    eventVotingScore += simCalculator.getSimilarity(curr, other);
                }
            }

            // Normalize by length.
            if (allEventMentions.size() > 1) {
                eventVotingScore /= (allEventMentions.size() - 1);
            }

            StanfordCorenlpToken targetWord = UimaNlpUtils.findHeadFromStanfordAnnotation(eventMention);
            String targetLex = targetWord == null ? TextUtils.asTokenized(eventMention) :
                    SalienceUtils.getCanonicalToken(targetWord);
            int sentLoc = mentionSentenceIds.getOrDefault(eventMention, 0);
            if (sentLoc > 10) {
                sentLoc = 10;
            }

            // Calculate the average voting from the entity to this event.
            double entityVotingScore = 0;
            for (String kb : instanceByKbId.keySet()) {
                entityVotingScore += simCalculator.getSimilarity(curr, kb);
            }

            if (instanceByKbId.keySet().size() > 0) {
                entityVotingScore /= instanceByKbId.keySet().size();
            }

            eventInstance.featureMap.put(lexicalPrefix + "Head_" + targetLex, 1.0);
            eventInstance.featureMap.put("SentenceLoc", (double) sentLoc);
            eventInstance.featureMap.put(sparsePrefix + "FrameName_" + eventMention.getFrameName(), 1.0);
            eventInstance.featureMap.put("HeadCount", (double) tokenCount.get(targetLex));
            eventInstance.featureMap.put("EventEmbeddingVoting", eventVotingScore);
            eventInstance.featureMap.put("EntityEmbeddingVoting", entityVotingScore);

            // Add entity instances related to the event to features.
            List<SimpleInstance> sentEntities = sameSentInstances.containsKey(eventMention) ?
                    sameSentInstances.get(eventMention) : new ArrayList<>();
            List<SimpleInstance> argumentEntities = argumentInstances.containsKey(eventMention) ?
                    argumentInstances.get(eventMention) : new ArrayList<>();

            addEntityInstanceFeatures(eventInstance, sentEntities, "EntitySameSent");
            addEntityInstanceFeatures(eventInstance, argumentEntities, "EntityArguments");

            index++;

            instances.add(eventInstance);
        }
        return instances;
    }

    private static void addEntityInstanceFeatures(SimpleInstance instance, List<SimpleInstance> entityInstances,
                                                  String featureBase) {
        String[] entityFeatureNames = new String[]{"EmbeddingVoting", "MentionsCount", "HeadCount"};

        for (String featureName : entityFeatureNames) {
            double maxValue = 0;
            double averValue = 0;
            double minValue = Double.MAX_VALUE;

            if (entityInstances.size() > 0) {
                for (SimpleInstance entityInstance : entityInstances) {
                    double featureValue = entityInstance.featureMap.get(featureName);
                    if (featureValue > maxValue) {
                        maxValue = featureValue;
                    }
                    if (featureValue < minValue) {
                        minValue = featureValue;
                    }
                    averValue += featureValue;
                }
                averValue /= entityInstances.size();
            } else {
                minValue = 0;
            }

            instance.featureMap.put(String.format("%s_%s_max", featureBase, featureName), maxValue);
            instance.featureMap.put(String.format("%s_%s_aver", featureBase, featureName), averValue);
            instance.featureMap.put(String.format("%s_%s_min", featureBase, featureName), minValue);
        }
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

            if (firstLoc > 10) {
                firstLoc = 10;
            }

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
        return bucket(number, 1);
    }

    private static int bucket(int number, int k) {
        return (int) Math.round(Math.log(k * (number + 1)));
    }
}
