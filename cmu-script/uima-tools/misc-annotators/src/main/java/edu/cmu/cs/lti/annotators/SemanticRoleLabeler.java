package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

/**
 * Extract semantic roles of various kinds form multiple sources. Results will be stored in general FrameTrigger types.
 *
 * @author Zhengzhong Liu
 */
public class SemanticRoleLabeler extends AbstractLoggingAnnotator{

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        
    }
}
