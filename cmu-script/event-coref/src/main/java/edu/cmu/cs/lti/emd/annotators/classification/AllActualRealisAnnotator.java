package edu.cmu.cs.lti.emd.annotators.classification;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/3/16
 * Time: 12:04 AM
 *
 * @author Zhengzhong Liu
 */
public class AllActualRealisAnnotator extends AbstractAnnotator{
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for (EventMention eventMention : JCasUtil.select(aJCas, EventMention.class)) {
            eventMention.setRealisType("Actual");
        }
    }
}
