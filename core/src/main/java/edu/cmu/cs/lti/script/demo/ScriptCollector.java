package edu.cmu.cs.lti.script.demo;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.StringUtils;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;

import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

public class ScriptCollector {
  static Table<String, String, TObjectIntHashMap<String>> event2EntityLinks = HashBasedTable
          .create();

  static Map<String, String> eventId2Surface = new HashMap<String, String>();

  static Map<String, String> entityId2Surface = new HashMap<String, String>();

  public static void addObservation(JCas aJCas, Event event, Entity entity) {
    String docId = UimaConvenience.getShortDocumentNameWithOffset(aJCas);
    String eventId = docId + "_" + event.getId();
    String centerEntityId = docId + "_" + entity.getId();

    String evmStr = event.getEventMentions(0).getHeadWord().getCoveredText().toLowerCase();
    eventId2Surface.put(eventId, evmStr);
    String entityStr = entity.getEntityMentions(0).getCoveredText();
    entityId2Surface.put(centerEntityId, entityStr);

    for (int i = 0; i < event.getEventMentions().size(); i++) {
      EventMention mention = event.getEventMentions(i);

      for (EventMentionArgumentLink argumentLink : FSCollectionFactory.create(
              mention.getArguments(), EventMentionArgumentLink.class)) {
        String surface = StringUtils.text2CsvField(argumentLink.getArgument().getCoveredText());
        String entityId = docId + "_" + argumentLink.getArgument().getReferingEntity().getId();
        String role = argumentLink.getArgumentRole();
        if (centerEntityId.equals(entityId)) {
          surface = "##" + surface + "##";
        }

        entityId2Surface.put(entityId, surface);

        if (event2EntityLinks.contains(eventId, entityId)) {
          event2EntityLinks.get(eventId, entityId).adjustOrPutValue(role, 1, 1);
        } else {
          TObjectIntHashMap<String> roleMap = new TObjectIntHashMap<String>();
          roleMap.put(role, 1);
          event2EntityLinks.put(eventId, entityId, roleMap);
        }
      }
    }
  }

  public static void writeObservations(PrintStream out) {
    List<String> eventIds = new ArrayList<String>(event2EntityLinks.rowKeySet());
    Collections.sort(eventIds);

    List<Entry<String, String>> surfaceSorted = new ArrayList<Entry<String, String>>(
            eventId2Surface.entrySet());
    Collections.sort(surfaceSorted, new Comparator<Map.Entry<String, String>>() {
      public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
        return (o2.getValue()).compareTo(o1.getValue());
      }
    });

    for (Map.Entry<String, String> idSurface : surfaceSorted) {
      out.print(idSurface.getValue());
      Map<String, TObjectIntHashMap<String>> row = event2EntityLinks.row(idSurface.getKey());
      for (String entityId : row.keySet()) {
        out.print("\t" + row.get(entityId) + " :\t " + entityId2Surface.get(entityId)+ "\t");
      }
      out.println("\n");
    }
  }
}
