package edu.cmu.cs.lti.event_coref.annotators;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.DebugUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/15/18
 * Time: 5:37 PM
 *
 * @author Zhengzhong Liu
 */
public class DebugAnnotator extends AbstractLoggingAnnotator{
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(aJCas.getDocumentText());

        DebugUtils.pause();
    }
}
