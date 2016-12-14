package edu.cmu.cs.lti.utils;

import com.google.common.collect.*;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.MentionTypeUtils;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.FSCollectionFactory;
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

    public static MentionGraph createMentionGraph(JCas aJCas, List<MentionCandidate> candidates,
                                                  PairFeatureExtractor extractor, boolean isTraining) {
        // Currently, we still could not handle cases where mentions have the same attributes and spans.
        List<EventMention> allMentions = MentionUtils.clearDuplicates(
                new ArrayList<>(JCasUtil.select(aJCas, EventMention.class))
        );

        // Create singleton events if not existed yet.
        createSingleEvents(aJCas, allMentions);

        TObjectIntMap<EventMention> mentionIndices = new TObjectIntHashMap<>();
        for (int i = 0; i < allMentions.size(); i++) {
            EventMention mention = allMentions.get(i);
            mention.setIndex(i);
            mentionIndices.put(mention, i);
        }

        List<EventMentionSpan> mentionSpans = new ArrayList<>(JCasUtil.select(aJCas, EventMentionSpan.class));

        int eventIdx = 0;
        int[] mentionId2EventId = new int[allMentions.size()];

        for (Event event : JCasUtil.select(aJCas, Event.class)) {
            event.setIndex(eventIdx++);
            // Here we only store non-singletons.
            int clusterSize = event.getEventMentions().size();
            if (clusterSize > 1) {
                for (int i = 0; i < event.getEventMentions().size(); i++) {
                    EventMention mention = event.getEventMentions(i);
                    mentionId2EventId[mention.getIndex()] = event.getIndex();
                }
            }
        }

        // Create some mapping for creating the event graph.
        int[][] candidate2Mentions = new int[candidates.size()][];
        String[] mentionTypes = new String[allMentions.size()];

        TObjectIntMap<EventMentionSpan> mentionIds = new TObjectIntHashMap<>();
        for (int i = 0; i < mentionSpans.size(); i++) {
            // i is the candidate id, a.k.a, the mention span id.
            EventMentionSpan ems = mentionSpans.get(i);
            mentionIds.put(ems, i);

            Collection<EventMention> mentions = FSCollectionFactory.create(ems.getEventMentions(), EventMention.class);

            candidate2Mentions[i] = new int[mentions.size()];

            int j = 0;
            for (EventMention mention : mentions) {
                int mentionIndex = mention.getIndex();
                candidate2Mentions[i][j] = mentionIndex;
                mentionTypes[mentionIndex] = mention.getEventType();
                j++;
            }
        }

        Table<Integer, Integer, String> relations = indexSpanRelations(aJCas, mentionIds);

        return new MentionGraph(candidates, candidate2Mentions, mentionTypes, mentionId2EventId,
                relations, extractor, isTraining);
    }

    private static void createSingleEvents(JCas aJCas, List<EventMention> mentions) {
        for (int i = 0; i < mentions.size(); i++) {
            EventMention mention = mentions.get(i);

            if (mention.getReferringEvent() == null) {
                Event event = new Event(aJCas);
                event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, Collections.singletonList(mention)));
                mention.setReferringEvent(event);
                UimaAnnotationUtils.finishTop(event, "Singleton", 0, aJCas);
            }
        }
    }

    public static List<MentionCandidate> getSpanBasedCandidates(JCas aJCas) {
        int sentIndex = 0;
        List<EventMentionSpan> mentionSpans = new ArrayList<>(JCasUtil.select(aJCas, EventMentionSpan.class));

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentence.setIndex(sentIndex++);
        }

        Map<EventMentionSpan, Collection<StanfordCorenlpSentence>> mention2Sentence = JCasUtil.indexCovering(
                aJCas, EventMentionSpan.class, StanfordCorenlpSentence.class);

        List<MentionCandidate> allCandidates = new ArrayList<>();

        for (int i = 0; i < mentionSpans.size(); i++) {
            EventMentionSpan eventMentionSpan = mentionSpans.get(i);
            MentionCandidate candidate = new MentionCandidate(eventMentionSpan.getBegin(), eventMentionSpan.getEnd(),
                    Iterables.getFirst(mention2Sentence.get(eventMentionSpan), null),
                    eventMentionSpan.getHeadWord(), i
            );
            candidate.setRealis(eventMentionSpan.getRealisType());
            candidate.setMentionType(eventMentionSpan.getEventType());
            allCandidates.add(candidate);
        }

        return allCandidates;
    }


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

        span2Mentions.keySet().stream().sorted(Collections.reverseOrder()).forEach(span -> {
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
    public static int[] groupEventClusters(List<EventMention> mentions) {
        int[] mentionId2EventId = new int[mentions.size()];
        int eventIndex = 0;

        Map<Event, Integer> eventIndices = new HashMap<>();

        for (int i = 0; i < mentions.size(); i++) {
            Event referringEvent = mentions.get(i).getReferringEvent();
            if (referringEvent == null) {
                mentionId2EventId[i] = eventIndex;
                eventIndex++;
            } else if (eventIndices.containsKey(referringEvent)) {
                Integer referringIndex = eventIndices.get(referringEvent);
                mentionId2EventId[i] = referringIndex;
            } else {
                mentionId2EventId[i] = eventIndex;
                eventIndices.put(referringEvent, eventIndex);
                eventIndex++;
            }
        }
        return mentionId2EventId;
    }

    public static Table<Integer, Integer, String> indexSpanRelations(JCas aJCas,
                                                                     TObjectIntMap<EventMentionSpan> spanIds) {
        Table<Integer, Integer, String> relations = HashBasedTable.create();

        for (EventMentionSpanRelation relation : JCasUtil.select(aJCas, EventMentionSpanRelation.class)) {
            int head = spanIds.get(relation.getHead());
            int child = spanIds.get(relation.getChild());
            relations.put(head, child, relation.getRelationType());
        }

        return relations;
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
        SetMultimap<Word, Integer> head2Mentions = HashMultimap.create();

        for (int i = 0; i < mentions.size(); i++) {
//            span2Mentions.put(Span.of(mentions.get(i).getBegin(), mentions.get(i).getEnd()), i);
            head2Mentions.put(mentions.get(i).getHeadWord(), i);
        }

        int splitCandidateId = 0;
        for (int candidateIndex = 0; candidateIndex < goldCandidates.size(); candidateIndex++) {
            MentionCandidate candidate = goldCandidates.get(candidateIndex);
            Word candidateHead = candidate.getHeadWord();
//            Span candidateSpan = Span.of(candidate.getBegin(), candidate.getEnd());
            if (head2Mentions.containsKey(candidateHead)) {
                Set<Integer> correspondingMentions = head2Mentions.get(candidateHead);

                // The type is done by joining multiple types on the same span.
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
