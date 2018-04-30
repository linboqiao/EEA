package edu.cmu.cs.lti.script.utils;

import edu.cmu.cs.lti.script.type.*;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/19/18
 * Time: 9:19 PM
 *
 * @author Zhengzhong Liu
 */
public class ImplicitFeaturesExtractor {
    private static Set<String> animacyTypes = new HashSet<>();
    private static Set<String> namedTypes = new HashSet<>();
    private static String[] grammarRoleCategories = {"Subject", "Adjunct", "Verb_Argument", "Noun_Argument", "Other"};

    {
        animacyTypes.add("PERSON");
        namedTypes.add("Location");
        namedTypes.add("PERSON");
        namedTypes.add("ORGANIZATION");
    }

    private static boolean inConjunction(Word word) {
        FSList headDepsFS = word.getHeadDependencyRelations();
        FSList childDepsFS = word.getChildDependencyRelations();

        boolean inConjunction = false;

        if (headDepsFS != null) {
            for (Dependency dependency : FSCollectionFactory.create(headDepsFS, Dependency.class)) {
                if (dependency.getDependencyType().startsWith("conj")) {
                    inConjunction = true;
                }
            }
        }

        if (childDepsFS != null) {
            for (Dependency dependency : FSCollectionFactory.create(childDepsFS, Dependency.class)) {
                if (dependency.getDependencyType().startsWith("conj")) {
                    inConjunction = true;
                }
            }
        }

        return inConjunction;
    }

    private static String grammarRole(Word word) {
        FSList headDepsFS = word.getHeadDependencyRelations();

        String relationType = "Other";

        if (headDepsFS != null) {
            for (Dependency dependency : FSCollectionFactory.create(headDepsFS, Dependency.class)) {
                String depType = dependency.getDependencyType();
                switch (depType) {
                    case "subj":
                        relationType = "Subject";
                        break;
                    case "pobj": // object of a preposition.
                    case "advmod":// adverb modifier.
                    case "tmod":// temporal modifier.
                    case "npadvmod": //noun phrase as adverbial modifier
                        relationType = "Adjunct";
                        break;
                    case "dobj":
                    case "iobj":
                    case "ccomp": // clausal complement.
                    case "acomp": // adjectival complement.
                        relationType = "Verb_Argument";
                        break;
                    case "rcmod": // relative clause.
                    case "appos": // appositional.
                    case "poss": // possession modifier.
                    case "nn": // noun compound modifier.
                    case "amod": // adjectival modifier.
                        relationType = "Noun_Argument";
                        break;
                    default:
                        break;
                }
            }
        }

        return relationType;
    }

    public static Map<Word, SortedMap<String, Double>> getArgumentFeatures(JCas aJCas, Set<Word> targets) {
        Map<Word, SortedMap<String, Double>> allFeatures = new HashMap<>();
        for (Word target : targets) {
            allFeatures.put(target, new TreeMap<>());
        }

        TObjectIntMap<String> tokenCount = new TObjectIntHashMap<>();
        for (StanfordCorenlpToken token : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
            tokenCount.adjustOrPutValue(lemma(token), 1, 1);
        }

        Map<Word, Integer> tokenSentIndex = new HashMap<>();

        // Record the position of a token in sentence (begin, middle or end).
        Map<Word, Integer> tokenSentPosition = new HashMap<>();
        Set<Word> firstTokens = new HashSet<>();
        Set<Word> lastTokens = new HashSet<>();
        Map<Word, String> grammarRoles = new HashMap<>();
        int sentIndex = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            List<Word> words = JCasUtil.selectCovered(Word.class, sentence);
            double sentLength = (double) words.size();
            double beginBoundary = sentLength / 3;
            double middleBoundary = sentLength / 3 * 2;

            int tokenIndex = 0;
            for (Word word : words) {
                tokenSentIndex.put(word, sentIndex++);

                double relativePosition = 1.0 * tokenIndex / sentLength;
                if (relativePosition < beginBoundary) {
                    tokenSentPosition.put(word, 0);
                } else if (relativePosition < middleBoundary) {
                    tokenSentPosition.put(word, 1);
                } else {
                    tokenSentPosition.put(word, 2);
                }

                if (tokenIndex == 0) {
                    firstTokens.add(word);
                }

                if (tokenIndex == sentLength) {
                    lastTokens.add(word);
                }

                grammarRoles.put(word, grammarRole(word));

                tokenIndex++;
            }
        }

        for (EntityMention entityMention : JCasUtil.select(aJCas, EntityMention.class)) {
            Word entityHead = entityMention.getHead();
            if (allFeatures.containsKey(entityHead)) {
                Entity entity = entityMention.getReferingEntity();
                int clusterSize = entity.getEntityMentions().size();

                int nominalCount = 0;
                int namedCount = 0;
                int pronominalCount = 0;

                int beginCount = 0;
                int middleCount = 0;
                int endCount = 0;
                int firstCount = 0;
                int lastCount = 0;

                TObjectIntMap<String> grammarRoleCounts = new TObjectIntHashMap<>();

                // Set first location to the last sentence.
                int firstLoc = sentIndex;

                for (EntityMention mention : FSCollectionFactory.create(entity.getEntityMentions(), EntityMention
                        .class)) {
                    Word head = mention.getHead();
                    String pos = head.getPos();

                    int mentionLoc = tokenSentIndex.get(mention.getHead());

                    if (mentionLoc < firstLoc) {
                        firstLoc = mentionLoc;
                    }


                    if (pos.equals("NNP")) {
                        namedCount += 1;
                    } else if (pos.startsWith("PR")) {
                        pronominalCount += 1;
                    } else {
                        if (mention.getEntityType() != null && namedTypes.contains(mention.getEntityType())) {
                            namedCount += 1;
                        }
                        nominalCount += 1;
                    }

                    switch (tokenSentPosition.get(head)) {
                        case 0:
                            beginCount += 1;
                            break;
                        case 1:
                            middleCount += 1;
                            break;
                        case 2:
                            endCount += 1;
                    }

                    grammarRoleCounts.adjustOrPutValue(grammarRoles.get(head), 1, 1);

                    if (firstTokens.contains(head)) {
                        firstCount += 1;
                    }

                    if (lastTokens.contains(head)) {
                        lastCount += 1;
                    }
                }

                Map<String, Double> thisFeatures = allFeatures.get(entityHead);

                // Add salience features.
                thisFeatures.put("Salience_NominalCount", bucket(nominalCount));
                thisFeatures.put("Salience_NamedCount", bucket(namedCount));
                thisFeatures.put("Salience_PronominalCount", bucket(pronominalCount));
                thisFeatures.put("Salience_MentionCounts", bucket(clusterSize));

                thisFeatures.put("Salience_FirstLoc", bucket(firstLoc));

                // Add life span features.
                // Positions
                thisFeatures.put("Life_sentence_position=begin", bucket(beginCount));
                thisFeatures.put("Life_sentence_position=middle", bucket(middleCount));
                thisFeatures.put("Life_sentence_position=end", bucket(endCount));

                thisFeatures.put("Life_sentence_position=first", bucket(firstCount));
                thisFeatures.put("Life_sentence_position=last", bucket(lastCount));

                // Grammar roles.
                for (String roleName : grammarRoleCategories) {
                    thisFeatures.put("Life_GrammarRole_" + roleName, bucket(grammarRoleCounts.get(roleName)));
                }
            }
        }

        // Head count feature.
        for (Word target : targets) {
            SortedMap<String, Double> headFeatures = allFeatures.get(target);
            if (headFeatures.isEmpty()) {
                // The target is not part of a mention.
                String pos = target.getPos();

                int nominalCount = 0;
                int namedCount = 0;
                int pronominalCount = 0;

                if (pos.equals("NNP")) {
                    namedCount = 1;
                } else if (pos.startsWith("PR")) {
                    pronominalCount = 1;
                } else {
                    nominalCount = 1;
                }

                headFeatures.put("Salience_NominalCount", bucket(nominalCount));
                headFeatures.put("Salience_NamedCount", bucket(namedCount));
                headFeatures.put("Salience_PronominalCount", bucket(pronominalCount));
                headFeatures.put("Salience_MentionCounts", bucket(1));
            }

            headFeatures.put("Salience_HeadCount", bucket(tokenCount.get(lemma(target))));
            headFeatures.put("Salience_FirstLoc", bucket(tokenSentIndex.get(target)));
        }

        return allFeatures;
    }

    private static String lemma(Word word) {
        return word.getLemma().toLowerCase();
    }


    private static double bucket(int number) {
        // Don't bucket here.
        return number;
//        return bucketK(number, Math.exp(1.0));
    }

    private static double bucketK(int number, double k) {
        return (double) Math.round(Math.log(k * (number + 1)));
    }
}
