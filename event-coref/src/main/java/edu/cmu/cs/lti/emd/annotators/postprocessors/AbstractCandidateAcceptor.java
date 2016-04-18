package edu.cmu.cs.lti.emd.annotators.postprocessors;

import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/1/15
 * Time: 5:04 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AbstractCandidateAcceptor extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<CandidateEventMention> candidates = UimaConvenience.getAnnotationList(aJCas, CandidateEventMention.class);

        candidates.stream().filter(this::accept).forEach
                (candidate -> {
                    EventMention mention = new EventMention(aJCas);
                    mention.setBegin(candidate.getBegin());
                    mention.setEnd(candidate.getEnd());
                    mention.setEventType(candidate.getPredictedType());
                    mention.setRealisType(candidate.getPredictedRealis());
                    UimaAnnotationUtils.finishAnnotation(mention, candidate.getBegin(), candidate.getEnd(),
                            COMPONENT_ID, 0, aJCas);
                });
        candidates.forEach(edu.cmu.cs.lti.script.type.CandidateEventMention::removeFromIndexes);
    }

    protected abstract boolean accept(CandidateEventMention candidate);
}
