package edu.cmu.cs.lti.after.train;

import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionSpan;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.MentionUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

        List<MentionCandidate> candidates = MentionUtils.getSpanBasedCandidates(aJCas);
        MentionGraph mentionGraph = MentionUtils.createMentionGraph(aJCas, candidates, extractor, false);
    }
}
