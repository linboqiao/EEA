package edu.cmu.cs.lti.emd.utils;

import com.google.common.collect.Iterables;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.script.type.*;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.javatuples.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/4/16
 * Time: 2:02 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionUtils {

    /**
     * Convert event mentions into mention candidates.
     *
     * @param aJCas
     * @param mentions
     * @return
     */
    public static List<MentionCandidate> createCandidates(JCas aJCas, Collection<EventMention> mentions, TIntIntMap
            mention2Candidate) {
        Map<EventMention, Collection<StanfordCorenlpSentence>> mention2Sentence = JCasUtil.indexCovering(
                aJCas, EventMention.class, StanfordCorenlpSentence.class);

        for (int i = 0; i < mentions.size(); i++) {
            mention2Candidate.put(i, i);
        }

        return mentions.stream().map(mention -> {
            MentionCandidate candidate = new MentionCandidate(mention.getBegin(), mention.getEnd(),
                    Iterables.getFirst(mention2Sentence.get(mention), null), mention.getHeadWord());
            candidate.setRealis(mention.getRealisType());
            candidate.setMentionType(mention.getEventType());
            return candidate;
        }).collect(Collectors.toList());
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
                                                                    Collection<StanfordCorenlpToken> tokens) {
        Map<StanfordCorenlpToken, Collection<StanfordCorenlpSentence>> mention2Sentence = JCasUtil.indexCovering(
                aJCas, StanfordCorenlpToken.class, StanfordCorenlpSentence.class);

        return tokens.stream().map(token -> new MentionCandidate(token.getBegin(), token.getEnd(),
                Iterables.getFirst(mention2Sentence.get(token), null), token)).collect(Collectors.toList());
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

    public static Map<Pair<Integer, Integer>, String> indexRelations(JCas aJCas, TIntIntMap mention2Candidate,
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

            relations.put(Pair.with(mention2Candidate.get(headMention), mention2Candidate.get(childMention)),
                    relation.getRelationType());
        }

        return relations;
    }
}
