package edu.cmu.cs.lti.learning.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.script.type.CharacterAnnotation;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/27/15
 * Time: 4:49 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionTypeUtils {
    public static String TYPE_NAME_JOINER = " ; ";

    private static Map<String, String[]> typeSplitCache = new HashMap<>();

//    /**
//     * The same span might be annotated as multiple types, we join them together.
//     *
//     * @param mentions All the mentions of interested.
//     * @return From the mention span to the type.
//     */
//    public static TreeMap<Span, String> mergeMentionTypes(Collection<EventMention> mentions) {
//        ArrayListMultimap<Span, String> mergedMentionTypes = ArrayListMultimap.create();
//        for (EventMention mention : mentions) {
//            mergedMentionTypes.put(Span.of(mention.getBegin(), mention.getEnd()), mention.getEventType());
//        }
//
//        TreeMap<Span, String> mentionWithMergedTypes = new TreeMap<>(Collections.reverseOrder());
//
//        for (Span span : mergedMentionTypes.keySet()) {
//            mentionWithMergedTypes.put(span, joinMultipleTypes(mergedMentionTypes.get(span)));
//        }
//
//        return mentionWithMergedTypes;
//    }

    public static Map<StanfordCorenlpToken, String> getTokenTypes(JCas aJCas, Sentence sentence) {
        ArrayListMultimap<StanfordCorenlpToken, String> unmergedTypes = ArrayListMultimap.create();

        List<EventMention> mentions = JCasUtil.selectCovered(aJCas, EventMention.class, sentence);

        Map<CharacterAnnotation, Collection<StanfordCorenlpToken>> characterCovering = JCasUtil.indexCovering(aJCas,
                CharacterAnnotation.class, StanfordCorenlpToken.class);

        for (EventMention mention : mentions) {
            String type = mention.getEventType();

            List<CharacterAnnotation> containedCharacters = JCasUtil.selectCovered(aJCas, CharacterAnnotation.class,
                    mention);

            if (containedCharacters.size() != 0) {
                for (CharacterAnnotation containedCharacter : containedCharacters) {
                    for (StanfordCorenlpToken token : characterCovering.get(containedCharacter)) {
                        unmergedTypes.put(token, type);
                    }
                }
            } else {
                List<StanfordCorenlpToken> containedTokens = JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class,
                        mention);
                for (StanfordCorenlpToken token : containedTokens) {
                    unmergedTypes.put(token, type);
                }
            }
        }

        Map<StanfordCorenlpToken, String> tokenTypes = new HashMap<>();
        for (Map.Entry<StanfordCorenlpToken, Collection<String>> unmergedType : unmergedTypes.asMap().entrySet()) {
            tokenTypes.put(unmergedType.getKey(), joinMultipleTypes(unmergedType.getValue()));
        }

        return tokenTypes;
    }

    public static String joinMultipleTypes(Iterable<String> types) {
        TreeSet<String> uniqueSortedTypes = Sets.newTreeSet(types);
        return Joiner.on(TYPE_NAME_JOINER).join(uniqueSortedTypes);
    }

    public static String[] splitToMultipleTypes(String joinedType) {
        if (typeSplitCache.containsKey(joinedType)) {
            return typeSplitCache.get(joinedType);
        } else {
            String[] t = joinedType.split(TYPE_NAME_JOINER);
            typeSplitCache.put(joinedType, t);
            return t;
        }
    }

    public static Map<Integer, Set<Integer>> treatTypesAsEquivalent(ClassAlphabet alphabet, List<String>
            additionalRules) {
        Map<Integer, Set<Integer>> type2EquivalentTypes = new HashMap<>();

        for (String foundRule : additionalRules) {
            String[] lr = foundRule.trim().split("\t");
            if (lr.length == 2) {
                addToEquivalentSet(type2EquivalentTypes, alphabet.getClassIndex(lr[0]), alphabet.getClassIndex(lr[1]));
            }
        }

        for (int i = 0; i < alphabet.size(); i++) {
            addToEquivalentSet(type2EquivalentTypes, i, i);

            String classNameI = alphabet.getClassName(i);
            for (int j = i + 1; j < alphabet.size(); j++) {
                String classNameJ = alphabet.getClassName(j);
                if (partialEquivalence(classNameI, classNameJ)) {
//                    System.out.println("Adding " + classNameI + " " + classNameJ);
                    addToEquivalentSet(type2EquivalentTypes, i, j);
                }
            }
        }

        return type2EquivalentTypes;
    }

    private static void addToEquivalentSet(Map<Integer, Set<Integer>> type2EquivalentTypes, int i, int j) {
        Set<Integer> equivalentSet;

        if (type2EquivalentTypes.containsKey(i)) {
            equivalentSet = type2EquivalentTypes.get(i);
        } else if (type2EquivalentTypes.containsKey(j)) {
            equivalentSet = type2EquivalentTypes.get(j);
        } else {
            equivalentSet = new HashSet<>();
            type2EquivalentTypes.put(i, equivalentSet);
            type2EquivalentTypes.put(j, equivalentSet);
        }
        equivalentSet.add(i);
        equivalentSet.add(j);
    }

    public static ArrayListMultimap<Integer, Integer> findAllowedCorefTypes(ClassAlphabet alphabet, List<String>
            additionalRules) {
        ArrayListMultimap<Integer, Integer> allowedLists = ArrayListMultimap.create();

        for (int i = 0; i < alphabet.size(); i++) {
            String classNameI = alphabet.getClassName(i);
            for (int j = i + 1; j < alphabet.size(); j++) {
                String classNameJ = alphabet.getClassName(j);
                if (partialEquivalence(classNameI, classNameJ)) {
                    allowedLists.put(i, j);
                    allowedLists.put(j, i);
                }
            }
        }

        for (int i = 0; i < alphabet.size(); i++) {
            allowedLists.put(i, i);
        }

        for (String foundRule : additionalRules) {
            String[] lr = foundRule.trim().split("\t");
            if (lr.length == 2) {
                allowedLists.put(alphabet.getClassIndex(lr[0]), alphabet.getClassIndex(lr[1]));
                allowedLists.put(alphabet.getClassIndex(lr[1]), alphabet.getClassIndex(lr[0]));
            }
        }

        return allowedLists;
    }

    public static boolean partialEquivalence(String type1, String type2) {
        Set<String> type1Parts = new HashSet<>(Arrays.asList(MentionTypeUtils.splitToMultipleTypes(type1)));
        Set<String> type2Parts = new HashSet<>(Arrays.asList(MentionTypeUtils.splitToMultipleTypes(type2)));
        type1Parts.retainAll(type2Parts);

        return type1Parts.size() > 0;
    }

    public static String canonicalize(String line) {
        return line.trim().replaceAll("\\s", "").toLowerCase().replaceAll("\\p{Punct}+", "");
    }

}
