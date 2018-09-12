package edu.cmu.cs.lti.event_coref.annotators.stats;

import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.model.FScore;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.helper.HashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/7/16
 * Time: 2:55 PM
 *
 * @author Zhengzhong Liu
 */
public class TypeHeuristicAccuracy extends AbstractLoggingAnnotator {

    private int sentenceCheckLimit = 5;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, false);

        Map<EventMention, Integer> mentionSentenceIndex = new HashMap<>();

        int sentIndex = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sentence)) {
                mentionSentenceIndex.put(mention, sentIndex);
            }
            sentIndex++;
        }

        Set<Pair<EventMention, EventMention>> corefArcs = new HashSet<>();

        for (Event event : JCasUtil.select(aJCas, Event.class)) {
            for (int i = 0; i < event.getEventMentions().size() - 1; i++) {
                EventMention mi = event.getEventMentions(i);
                for (int j = i + 1; j < event.getEventMentions().size(); j++) {
                    EventMention mj = event.getEventMentions(j);
                    corefArcs.add(Pair.of(mi, mj));
                    corefArcs.add(Pair.of(mj, mi));
                }
            }
        }

        List<FScore> fScoreList = new ArrayList<>();

        for (int i = 0; i <= sentenceCheckLimit; i++) {
            FScore fScore = checkDistanceAccuracy(mentionSentenceIndex, corefArcs, sentenceCheckLimit);
            fScoreList.add(fScore);
        }
    }

    private FScore checkDistanceAccuracy(Map<EventMention, Integer> mentionSentenceIndex, Set<Pair<EventMention,
            EventMention>> corefArcs, int threshold) {
        FScore fScore = new FScore(corefArcs.size() / 2);

        for (Map.Entry<EventMention, Integer> mentionIWithIndex : mentionSentenceIndex.entrySet()) {
            for (Map.Entry<EventMention, Integer> mentionJWithIndex : mentionSentenceIndex.entrySet()) {
                int sentI = mentionIWithIndex.getValue();
                int sentJ = mentionIWithIndex.getValue();

                if (Math.abs(sentI - sentJ) <= threshold) {
                    EventMention mi = mentionIWithIndex.getKey();
                    EventMention mj = mentionJWithIndex.getKey();
                    if (mi.getEventType().equals(mj.getEventType())) {
                        if (corefArcs.contains(Pair.of(mi, mj))) {
                            fScore.addTp();
                        } else {
                            fScore.addFp();
                        }
                    }
                }
            }
        }
        return fScore;
    }
}
