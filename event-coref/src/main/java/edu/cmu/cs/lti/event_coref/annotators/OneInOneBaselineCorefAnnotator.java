package edu.cmu.cs.lti.event_coref.annotators;

import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/8/15
 * Time: 6:02 PM
 *
 * @author Zhengzhong Liu
 */
public class OneInOneBaselineCorefAnnotator extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        int i = 0;
        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            Event singleton = new Event(aJCas);
            singleton.setEventMentions(new FSArray(aJCas, 1));
            singleton.setEventMentions(0, mention);
            UimaAnnotationUtils.finishTop(singleton, COMPONENT_ID, i++, aJCas);
        }
    }
}