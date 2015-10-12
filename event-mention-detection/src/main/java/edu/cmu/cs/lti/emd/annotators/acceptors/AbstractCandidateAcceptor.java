package edu.cmu.cs.lti.emd.annotators.acceptors;

import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/1/15
 * Time: 5:04 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AbstractCandidateAcceptor extends AbstractLoggingAnnotator {

    public static final String PARAM_REMOVE_ACCEPT_MENTIONS = "removeAccepts";

    @ConfigurationParameter(name = PARAM_REMOVE_ACCEPT_MENTIONS, defaultValue = "false")
    private boolean removeAccepted;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Collection<CandidateEventMention> candidates = JCasUtil.select(aJCas, CandidateEventMention.class);

        candidates.stream().filter(this::accept).forEach(candidate -> {
            EventMention mention = new EventMention(aJCas);
            mention.setBegin(candidate.getBegin());
            mention.setEnd(candidate.getEnd());
            mention.setEventType(candidate.getPredictedType());
            mention.setRealisType(candidate.getPredictedRealis());
            UimaAnnotationUtils.finishAnnotation(mention, candidate.getBegin(), candidate.getEnd(),
                    COMPONENT_ID, 0, aJCas);
            if (removeAccepted) {
                candidate.removeFromIndexes();
            }
        });
    }

    protected abstract boolean accept(CandidateEventMention candidate);
}
