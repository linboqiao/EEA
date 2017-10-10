package edu.cmu.cs.lti.script.annotators.argument;

import edu.cmu.cs.lti.script.type.SemaforAnnotationSet;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/26/16
 * Time: 6:49 PM
 *
 * @author Zhengzhong Liu
 */
public class CompatibleArgumentMiner extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }


    private void getEvents(JCas aJCas) {
        // Use semafor to get these.
        JCasUtil.select(aJCas, SemaforAnnotationSet.class);
    }

}
