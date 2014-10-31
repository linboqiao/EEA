/**
 * 
 */
package edu.cmu.cs.lti.cds.annotators.writers;

import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author zhengzhongliu
 * 
 */
public class DocumentLevelEventWriter extends AbstractCsvWriterAnalysisEngine {

  private Iterator<Event> iter;

  private TokenAlignmentHelper tHelper;

  private String docId;

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine#getHeader()
   */
  @Override
  protected String[] getHeader() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine#prepare(org.apache.uima.jcas.
   * JCas)
   */
  @Override
  protected void prepare(JCas aJCas) {
    Collection<Event> events = JCasUtil.select(aJCas, Event.class);
    iter = events.iterator();
    setSeperator('\t');

    docId = UimaConvenience.getShortDocumentNameWithOffset(aJCas);
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine#hasNextRow()
   */
  @Override
  protected boolean hasNextRow() {
    return iter.hasNext();
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine#getNextCsvRow()
   */
  @Override
  protected String[] getNextCsvRow() {
    List<String> eventSurfaces = new ArrayList<String>();

    Event event = iter.next();

    eventSurfaces.add(docId + "_" + event.getId());

    TObjectIntHashMap<String> headwordCount = new TObjectIntHashMap<String>();
    for (int i = 0; i < event.getEventMentions().size(); i++) {
      EventMention mention = event.getEventMentions(i);
      Word headWord = mention.getHeadWord();
      headwordCount.adjustOrPutValue(UimaNlpUtils.getLemmatizedAnnotation(headWord), 1, 1);
    }

    for (TObjectIntIterator<String> iter = headwordCount.iterator(); iter.hasNext();) {
      iter.advance();
      eventSurfaces.add(iter.key() + ":" + iter.value());
    }

    return eventSurfaces.toArray(new String[eventSurfaces.size()]);
  }
}
