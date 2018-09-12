/**
 * 
 */
package edu.cmu.cs.lti.script.annotators.writers;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhengzhongliu
 * 
 */
public class EventEntityLinkProducer extends AbstractCsvWriterAnalysisEngine {

  int index = 0;

  private List<Triple<Event, Entity, Pair<String, Integer>>> allLinks;

  private String docId;

  /*
   * (non-Javadoc)
   * 
   * @see edu.edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine#getHeader()
   */
  @Override
  protected String[] getHeader() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * edu.edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine#prepare(org.apache.uima.jcas.
   * JCas)
   */
  @Override
  protected void prepare(JCas aJCas) {
    setSeparator('\t');
    docId = UimaConvenience.getShortDocumentNameWithOffset(aJCas);

    Table<Event, Entity, TObjectIntHashMap<String>> event2EntityLinks = HashBasedTable.create();
    allLinks = new ArrayList<Triple<Event, Entity, Pair<String, Integer>>>();

    for (Event event : JCasUtil.select(aJCas, Event.class)) {
      for (int i = 0; i < event.getEventMentions().size(); i++) {
        EventMention evm = event.getEventMentions(i);
        for (EventMentionArgumentLink argument : FSCollectionFactory.create(evm.getArguments(),
                EventMentionArgumentLink.class)) {
          EntityMention en = argument.getArgument();
          Entity entity = en.getReferingEntity();
          String role = argument.getArgumentRole();

          if (event == null) {
            System.out.println("Event is null");
          }

          if (entity == null) {
            System.err.println("Entity is null");
          }

          if (event2EntityLinks.contains(event, entity)) {
            event2EntityLinks.get(event, entity).adjustOrPutValue(role, 1, 1);
          } else {
            TObjectIntHashMap<String> roleMap = new TObjectIntHashMap<String>();
            roleMap.put(role, 1);
            event2EntityLinks.put(event, entity, roleMap);
          }
        }
      }
    }

    for (Cell<Event, Entity, TObjectIntHashMap<String>> cell : event2EntityLinks.cellSet()) {
      TObjectIntHashMap<String> roleMap = cell.getValue();
      for (String roleName : roleMap.keySet()) {
        allLinks.add(Triple.of(cell.getRowKey(), cell.getColumnKey(),
                Pair.of(roleName, roleMap.get(roleName))));
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine#hasNextRow()
   */
  @Override
  protected boolean hasNextRow() {
    return index < allLinks.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine#getNextCsvRow()
   */
  @Override
  protected String[] getNextCsvRow() {
    Triple<Event, Entity, Pair<String, Integer>> link = allLinks.get(index);
    String[] linkRow = new String[4];

    linkRow[0] = docId + "_" + link.getLeft().getId();
    linkRow[1] = docId + "_" + link.getMiddle().getId();
    linkRow[2] = link.getRight().getLeft();
    linkRow[3] = Integer.toString(link.getRight().getRight());

    index++;
    return linkRow;
  }
}
