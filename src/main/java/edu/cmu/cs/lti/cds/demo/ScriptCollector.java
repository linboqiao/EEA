package edu.cmu.cs.lti.cds.demo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.util.Hash;
import org.apache.uima.jcas.JCas;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.StringUtils;
import gnu.trove.map.hash.TObjectIntHashMap;

public class ScriptCollector {
  static Table<String, String, TObjectIntHashMap<String>> event2EntityLinks = HashBasedTable
          .create();

  static Map<String, String> eventId2Surface = new HashMap<String, String>();

  static Map<String, String> entityId2Surface = new HashMap<String, String>();

  public static void addObservation(JCas aJCas, Event event, Entity entity, String role) {
    String docId = UimaConvenience.getShortDocumentNameWithOffset(aJCas);
    String eventId = docId + "_" + event.getId();
    String entityId = docId + "_" + entity.getId();

    String evmStr = event.getEventMentions(0).getHeadWord().getCoveredText().toLowerCase();
    eventId2Surface.put(eventId, evmStr);

    entityId2Surface.put(entityId,
            StringUtils.text2CsvField(entity.getEntityMentions(0).getCoveredText()));

    if (event2EntityLinks.contains(eventId, entityId)) {
      event2EntityLinks.get(eventId, entityId).adjustOrPutValue(role, 1, 1);
    } else {
      TObjectIntHashMap<String> roleMap = new TObjectIntHashMap<String>();
      roleMap.put(role, 1);
      event2EntityLinks.put(eventId, entityId, roleMap);
    }
  }

  public static void writeObservations(PrintStream out) {
    List<String> eventIds = new ArrayList<String>(event2EntityLinks.rowKeySet());
    Collections.sort(eventIds);

    for (String eventId : eventIds) {
      out.println("Event: " + eventId2Surface.get(eventId));
      Map<String, TObjectIntHashMap<String>> row = event2EntityLinks.row(eventId);
      for (String entityId : row.keySet()) {
        out.println("  -- " + row.get(entityId) + " :\t " + entityId2Surface.get(entityId));
      }
    }
  }

}
