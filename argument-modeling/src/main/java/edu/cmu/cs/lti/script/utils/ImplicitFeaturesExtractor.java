package edu.cmu.cs.lti.script.utils;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
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

        namedTypes.add("LOCATION");
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

    public static Map<Entity, SortedMap<String, Double>> getArgumentFeatures(JCas aJCas) {
        TObjectIntMap<String> tokenCount = new TObjectIntHashMap<>();
        for (StanfordCorenlpToken token : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
            tokenCount.adjustOrPutValue(lemma(token), 1, 1);
        }

        Map<Word, Integer> tokenSentIndex = new HashMap<>();

        // Record the position of a token in sentence (begin, middle or end).
        Map<StanfordCorenlpToken, Integer> tokenSentPosition = new HashMap<>();
        Set<StanfordCorenlpToken> firstTokens = new HashSet<>();
        Set<StanfordCorenlpToken> lastTokens = new HashSet<>();
        Map<StanfordCorenlpToken, String> grammarRoles = new HashMap<>();

        int sentIndex = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            List<StanfordCorenlpToken> words = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
            double sentLength = (double) words.size();
            double beginBoundary = sentLength / 3;
            double middleBoundary = sentLength / 3 * 2;

            int tokenIndex = 0;
            for (StanfordCorenlpToken word : words) {
                tokenSentIndex.put(word, sentIndex);

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

            sentIndex++;
        }

        Map<Entity, SortedMap<String, Double>> entityFeatures = new HashMap<>();
        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
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

            int firstLoc = sentIndex;

            EntityMention representEnt = entity.getRepresentativeMention();

            for (EntityMention mention : FSCollectionFactory.create(entity.getEntityMentions(), EntityMention.class)) {
                Word mentionHead = mention.getHead();
                StanfordCorenlpToken mentionFirst = UimaConvenience.selectCoveredFirst(mention,
                        StanfordCorenlpToken.class);
                StanfordCorenlpToken mentionlast = UimaConvenience.selectCoveredLast(mention,
                        StanfordCorenlpToken.class);
                String pos = mentionHead.getPos();

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

                switch (tokenSentPosition.get(mentionFirst)) {
                    case 0:
                        beginCount += 1;
                        break;
                    case 1:
                        middleCount += 1;
                        break;
                    case 2:
                        endCount += 1;
                }

                grammarRoleCounts.adjustOrPutValue(grammarRoles.get(mentionHead), 1, 1);

                if (firstTokens.contains(mentionFirst)) {
                    firstCount += 1;
                }

                if (lastTokens.contains(mentionlast)) {
                    lastCount += 1;
                }
            }

            SortedMap<String, Double> thisFeatures = new TreeMap<>();

            // Add salience features.
            thisFeatures.put("Salience_NominalCount", bucket(nominalCount));
            thisFeatures.put("Salience_NamedCount", bucket(namedCount));
            thisFeatures.put("Salience_PronominalCount", bucket(pronominalCount));
            thisFeatures.put("Salience_MentionCounts", bucket(clusterSize));

            thisFeatures.put("Salience_HeadCounts", bucket(tokenCount.get(representEnt.getHead())));

            thisFeatures.put("Salience_FirstLoc", bucket(firstLoc));

            // Add life span features.
            // Positions
            thisFeatures.put("Life_sentence_position=begin", bucket(beginCount));
            thisFeatures.put("Life_sentence_position=middle", bucket(middleCount));
            thisFeatures.put("Life_sentence_position=end", bucket(endCount));

            thisFeatures.put("Life_sentence_position=first", bucket(firstCount));
            thisFeatures.put("Life_sentence_position=last", bucket(lastCount));

            entityFeatures.put(entity, thisFeatures);
        }

        return entityFeatures;
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
