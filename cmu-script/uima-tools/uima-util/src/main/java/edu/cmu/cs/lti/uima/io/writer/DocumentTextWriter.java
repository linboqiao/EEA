/**
 * 
 */
package edu.cmu.cs.lti.uima.io.writer;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * A simple writer to print document text.
 * 
 * @author Jun Araki
 */
public class DocumentTextWriter extends AbstractCustomizedTextWriterAnalysisEngine {

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    try {
      super.initialize(context);
    } catch (ResourceInitializationException e) {
      throw new ResourceInitializationException(e);
    }
  }

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    try {
      super.process(aJCas);
    } catch (AnalysisEngineProcessException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  public String getTextToPrint(JCas aJCas) {
    return aJCas.getDocumentText();
  }

}