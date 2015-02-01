/**
 * 
 */
package edu.cmu.cs.lti.script.annotators.writers;

import edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalsysisEngine;
import org.apache.uima.jcas.JCas;

/**
 * @author zhengzhongliu
 * 
 */
public class DocumentTextPrinter extends AbstractCustomizedTextWriterAnalsysisEngine {

  /*
   * (non-Javadoc)
   * 
   * @see
   * edu.edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalsysisEngine#getTextToPrint(org
   * .apache.uima.jcas.JCas)
   */
  @Override
  public String getTextToPrint(JCas aJCas) {
    return aJCas.getDocumentText();
  }

}
