package edu.cmu.cs.lti.event_coref.annotators.prepare;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 2/3/16
 * Time: 8:59 PM
 *
 * @author Zhengzhong Liu
 */
public class EventHeadWordAnnotator extends AbstractAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            StanfordCorenlpToken headWord = UimaNlpUtils.findHeadFromAnnotation(mention);
            mention.setHeadWord(headWord);
        }
    }
}
