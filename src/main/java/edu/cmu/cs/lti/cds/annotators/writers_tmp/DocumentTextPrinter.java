/**
 * 
 */
package edu.cmu.cs.lti.cds.annotators.writers;

import org.apache.uima.jcas.JCas;

import edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalsysisEngine;

/**
 * @author zhengzhongliu
 * 
 */
public class DocumentTextPrinter extends AbstractCustomizedTextWriterAnalsysisEngine {

  /*
   * (non-Javadoc)
   * 
   * @see
   * edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalsysisEngine#getTextToPrint(org
   * .apache.uima.jcas.JCas)
   */
  @Override
  public String getTextToPrint(JCas aJCas) {
    return aJCas.getDocumentText();
  }

}
