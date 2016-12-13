package edu.cmu.cs.lti.after.train;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionSpan;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.MentionUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Train a pairwise based after linking model.
 * <p>
 * Date: 12/12/16
 * Time: 3:35 PM
 *
 * @author Zhengzhong Liu
 */
public class PairwiseAfterTrainer extends AbstractLoggingAnnotator {
    private PairFeatureExtractor extractor;


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Collection<EventMentionSpan> mentionSpans = JCasUtil.select(aJCas, EventMentionSpan.class);
        List<EventMention> allMentions = MentionUtils.clearDuplicates(
                new ArrayList<>(JCasUtil.select(aJCas, EventMention.class))
        );

        MentionGraph mentionGraph = getMentionGraph(aJCas, allMentions);
    }

    private MentionGraph getMentionGraph(JCas aJCas, List<EventMention> allMentions){
        int eventIdx = 0;
        for (Event event : JCasUtil.select(aJCas, Event.class)) {
            event.setIndex(eventIdx++);
        }
        extractor.initWorkspace(aJCas);

        Map<Integer, Integer> mentionId2EventId = MentionUtils.groupEventClusters(allMentions);

        List<MentionCandidate> candidates = MentionUtils.createCandidates(aJCas, allMentions);

        // Each candidate can correspond to multiple nodes.
        SetMultimap<Integer, Integer> candidate2SplitNodes = HashMultimap.create();
        // A gold mention has a one to one mapping to a node in current case.
        TIntIntMap mention2SplitNodes = new TIntIntHashMap();
        for (int i = 0; i < allMentions.size(); i++) {
            candidate2SplitNodes.put(i, i);
            mention2SplitNodes.put(i, i);
        }

        Map<Pair<Integer, Integer>, String> relations = MentionUtils.indexRelations(aJCas, mention2SplitNodes,
                allMentions);

        List<String> mentionTypes = allMentions.stream().map(EventMention::getEventType).collect(Collectors.toList());

        // A mention graph represent all the mentions and contains features among them.
        return new MentionGraph(candidates, candidate2SplitNodes, mentionTypes, mentionId2EventId,
                relations, extractor, true);
    }
}
