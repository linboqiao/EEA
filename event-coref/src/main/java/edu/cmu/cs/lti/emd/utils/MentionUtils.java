package edu.cmu.cs.lti.emd.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/4/16
 * Time: 2:02 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionUtils {
    private static final Logger logger = LoggerFactory.getLogger(MentionUtils.class);

    /**
     * Convert event mentions into mention candidates.
     *
     * @param aJCas
     * @param mentions
     * @return
     */
    public static List<MentionCandidate> createCandidates(JCas aJCas, List<EventMention> mentions) {
        int sentIndex = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentence.setIndex(sentIndex++);
        }

        Map<EventMention, Collection<StanfordCorenlpSentence>> mention2Sentence = JCasUtil.indexCovering(
                aJCas, EventMention.class, StanfordCorenlpSentence.class);

        return IntStream.range(0, mentions.size()).mapToObj(i -> {
            EventMention mention = mentions.get(i);
            MentionCandidate candidate = new MentionCandidate(mention.getBegin(), mention.getEnd(),
                    Iterables.getFirst(mention2Sentence.get(mention), null), mention.getHeadWord(), i);
            candidate.setRealis(mention.getRealisType());
            candidate.setMentionType(mention.getEventType());
            return candidate;
        }).collect(Collectors.toList());
    }

    /**
     * Convert event mentions into mention candidates.
     *
     * @param aJCas
     * @param mentions
     * @return
     */
    public static List<MentionCandidate> createMergedCandidates(JCas aJCas, List<EventMention> mentions) {
        int sentIndex = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentence.setIndex(sentIndex++);
        }

        Map<EventMention, Collection<StanfordCorenlpSentence>> mention2Sentence = JCasUtil.indexCovering(
                aJCas, EventMention.class, StanfordCorenlpSentence.class);


        ArrayListMultimap<Span, EventMention> span2Mentions = ArrayListMultimap.create();

        for (int i = 0; i < mentions.size(); i++) {
            EventMention mention = mentions.get(i);
            span2Mentions.put(Span.of(mention.getBegin(), mention.getEnd()), mention);
        }

        List<MentionCandidate> candidates = new ArrayList<>();
//        int candidateIndex = 0;
        MutableInt candidateIndex = new MutableInt();

        span2Mentions.keySet().stream().sorted(Collections.reverseOrder()).forEach(span ->{
            Set<String> allTypes = span2Mentions.get(span).stream().map(EventMention::getEventType)
                    .collect(Collectors.toSet());
            String type = MentionTypeUtils.joinMultipleTypes(allTypes);

            EventMention mention = Iterables.get(span2Mentions.get(span), 0);
            String realis = mention.getRealisType();
            MentionCandidate candidate = new MentionCandidate(mention.getBegin(), mention.getEnd(),
                    Iterables.get(mention2Sentence.get(mention), 0), mention.getHeadWord(), candidateIndex.getValue());
            candidate.setMentionType(type);
            candidate.setRealis(realis);
            candidateIndex.increment();
            candidates.add(candidate);
        });
//
//        for (Map.Entry<Span, Collection<EventMention>> s2m : span2Mentions.asMap().entrySet()) {
//            Set<String> allTypes = s2m.getValue().stream().map(EventMention::getEventType).collect(Collectors.toSet());
//            String type = MentionTypeUtils.joinMultipleTypes(allTypes);
//
//            EventMention mention = Iterables.get(s2m.getValue(), 0);
//            String realis = mention.getRealisType();
//            MentionCandidate candidate = new MentionCandidate(mention.getBegin(), mention.getEnd(),
//                    Iterables.get(mention2Sentence.get(mention), 0), mention.getHeadWord(), candidateIndex);
//            candidate.setMentionType(type);
//            candidate.setRealis(realis);
//            candidateIndex++;
//            candidates.add(candidate);
//        }

        return candidates;
    }

    /**
     * Keep only one mention when multiple ones have the same span and types.
     *
     * @param mentions The original mentions.
     * @return The filtered result.
     */
    public static List<EventMention> clearDuplicates(List<EventMention> mentions) {
        ArrayListMultimap<Quartet<Integer, Integer, String, String>, EventMention> counter = ArrayListMultimap.create();
        for (EventMention mention : mentions) {
            counter.put(new Quartet<>(mention.getBegin(), mention.getEnd(), mention.getEventType(),
                    mention.getEpistemicStatus()), mention);
        }

        Set<EventMention> removalMentions = new HashSet<>();

        for (Map.Entry<Quartet<Integer, Integer, String, String>, Collection<EventMention>> item :
                counter.asMap().entrySet()) {
            Collection<EventMention> duplicates = item.getValue();

            EventMention keptMention = null;

            for (EventMention duplicate : duplicates) {
                if (duplicate.getReferringEvent() != null) {
                    keptMention = duplicate;
                    break;
                }
            }

            if (keptMention == null) {
                keptMention = Iterables.getFirst(duplicates, null);
            }

            for (EventMention duplicate : duplicates) {
                if (duplicate != keptMention) {
                    removalMentions.add(duplicate);
                }
            }
        }


        List<EventMention> filteredMentions = new ArrayList<>();
        for (EventMention mention : mentions) {
            if (!removalMentions.contains(mention)) {
                filteredMentions.add(mention);
            }
        }

        return filteredMentions;
    }

    /**
     * Create event candidates from tokens. This will not create any realis type and event type, they should be later
     * included.
     *
     * @param aJCas
     * @param tokens
     * @return
     */
    public static List<MentionCandidate> createCandidatesFromTokens(JCas aJCas,
                                                                    List<StanfordCorenlpToken> tokens) {
        Map<StanfordCorenlpToken, Collection<StanfordCorenlpSentence>> mention2Sentence = JCasUtil.indexCovering(
                aJCas, StanfordCorenlpToken.class, StanfordCorenlpSentence.class);

        return IntStream.range(0, tokens.size()).mapToObj(i -> {
            StanfordCorenlpToken token = tokens.get(i);
            return new MentionCandidate(token.getBegin(), token.getEnd(),
                    Iterables.getFirst(mention2Sentence.get(token), null), token, i);
        }).collect(Collectors.toList());
    }

    /**
     * @return Map from the mention to the event index it refers.
     */
    public static Map<Integer, Integer> groupEventClusters(List<EventMention> mentions) {
        Map<Integer, Integer> mentionId2EventId = new HashMap<>();
        int eventIndex = 0;

        Map<Event, Integer> eventIndices = new HashMap<>();

        for (int i = 0; i < mentions.size(); i++) {
            Event referringEvent = mentions.get(i).getReferringEvent();
            if (referringEvent == null) {
                mentionId2EventId.put(i, eventIndex);
                eventIndex++;
            } else if (eventIndices.containsKey(referringEvent)) {
                Integer referringIndex = eventIndices.get(referringEvent);
                mentionId2EventId.put(i, referringIndex);
            } else {
                mentionId2EventId.put(i, eventIndex);
                eventIndices.put(referringEvent, eventIndex);
                eventIndex++;
            }
        }
        return mentionId2EventId;
    }

    public static Map<Pair<Integer, Integer>, String> indexRelations(JCas aJCas, TIntIntMap mention2SplitNodes,
                                                                     List<EventMention> allMentions) {
        Map<Pair<Integer, Integer>, String> relations = new HashMap<>();


        TObjectIntMap<EventMention> mentionIds = new TObjectIntHashMap<>();
        for (int i = 0; i < allMentions.size(); i++) {
            EventMention mention = allMentions.get(i);
            mentionIds.put(mention, i);
        }

        for (EventMentionRelation relation : JCasUtil.select(aJCas, EventMentionRelation.class)) {
            int headMention = mentionIds.get(relation.getHead());
            int childMention = mentionIds.get(relation.getChild());

            relations.put(Pair.of(mention2SplitNodes.get(headMention), mention2SplitNodes.get(childMention)),
                    relation.getRelationType());
        }

        return relations;
    }


    public static int processCandidates(List<EventMention> mentions, List<MentionCandidate> goldCandidates,
                                        SetMultimap<Integer, Integer> candidate2Split,
                                        TIntIntMap mention2SplitCandidate, List<String> splitCandidateTypes) {
        SetMultimap<Span, Integer> span2Mentions = HashMultimap.create();

        for (int i = 0; i < mentions.size(); i++) {
            span2Mentions.put(Span.of(mentions.get(i).getBegin(), mentions.get(i).getEnd()), i);
        }

        int splitCandidateId = 0;
        for (int candidateIndex = 0; candidateIndex < goldCandidates.size(); candidateIndex++) {
            MentionCandidate candidate = goldCandidates.get(candidateIndex);
            Span candidateSpan = Span.of(candidate.getBegin(), candidate.getEnd());
            if (span2Mentions.containsKey(candidateSpan)) {
                Set<Integer> correspondingMentions = span2Mentions.get(candidateSpan);
                String mentionType = MentionTypeUtils.joinMultipleTypes(correspondingMentions.stream()
                        .map(mentions::get).map(EventMention::getEventType).collect(Collectors.toList()));
                candidate.setMentionType(mentionType);

                for (Integer mentionId : correspondingMentions) {
                    EventMention mention = mentions.get(mentionId);
                    candidate.setRealis(mention.getRealisType());
                    candidate2Split.put(candidateIndex, splitCandidateId);
                    splitCandidateTypes.add(mention.getEventType());
                    mention2SplitCandidate.put(mentionId, splitCandidateId);
                    splitCandidateId++;
                }
            } else {
                candidate.setMentionType(ClassAlphabet.noneOfTheAboveClass);
                candidate.setRealis(ClassAlphabet.noneOfTheAboveClass);
                splitCandidateTypes.add(ClassAlphabet.noneOfTheAboveClass);
                candidate2Split.put(candidateIndex, splitCandidateId);
                splitCandidateId++;
            }
        }

        return splitCandidateId;
    }

    public static Map<Integer, Integer> mapCandidate2Events(int numCandidates, TIntIntMap mention2Candidate,
                                                            Map<Integer, Integer> mention2event) {
        Map<Integer, Integer> candidate2Events = new HashMap<>();

        final MutableInt maxEventId = new MutableInt(0);
        mention2Candidate.forEachEntry((mentionId, candidateId) -> {
            int eventId = mention2event.get(mentionId);
            candidate2Events.put(candidateId, eventId);
            if (eventId > maxEventId.getValue()) {
                maxEventId.setValue(eventId);
            }
            return true;
        });


        for (int i = 0; i < numCandidates; i++) {
            if (!candidate2Events.containsKey(i)) {
                maxEventId.increment();
                candidate2Events.put(i, maxEventId.getValue());
            }
        }

        return candidate2Events;
    }
}
