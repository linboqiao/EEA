package edu.cmu.cs.lti.emd.annotators;

import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/30/15
 * Time: 12:03 PM
 */
public class CandidateEventMentionDetector extends AbstractLoggingAnnotator {


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }

    public List<CandidateEventMention> semaforMentionFinder(JCas aJCas, Set<String> targetFrames) {
        List<CandidateEventMention> candidates = new ArrayList<>();
        return candidates;
    }


    public List<CandidateEventMention> fanseMentionFinder(JCas aJCas, Set<String> targetFrames) {
        List<CandidateEventMention> candidates = new ArrayList<>();
        return candidates;
    }

    public List<CandidateEventMention> frameLookupMentionFinder(JCas aJCas, Set<String> targetFrames) {
        List<CandidateEventMention> candidates = new ArrayList<>();
        return candidates;
    }

    public List<CandidateEventMention> brownClusteringMentionFinder(JCas aJCas, Set<String> targetFrames) {
        List<CandidateEventMention> candidates = new ArrayList<>();
        return candidates;
    }

}
