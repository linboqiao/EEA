package edu.cmu.cs.lti.emd.annotators.lexical;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

/**
 * Print out simplest form of the triples of the mentions from gold standard.
 *
 * @author Zhengzhong Liu
 */
public class MentionTriplePrinter extends AbstractLoggingAnnotator {

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }
}
