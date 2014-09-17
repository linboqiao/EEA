/**
 * 
 */
package edu.cmu.cs.lti.cds.annotators;

import java.util.Arrays;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;

/**
 * Initialize each event as a singleton cluster that contains only one event mentions
 * 
 * @author zhengzhongliu
 * 
 */
public class SingletonEventAnnotator extends JCasAnnotator_ImplBase {

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.apache.uima.jcas.JCas)
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
      Event event = new Event(aJCas);
      event.setEventMentions(FSCollectionFactory.createFSList(aJCas, Arrays.asList(mention)));
      event.setId(mention.getId());
    }
  }

}
