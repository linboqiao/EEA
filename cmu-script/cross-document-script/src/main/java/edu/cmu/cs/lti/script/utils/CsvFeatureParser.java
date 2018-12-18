package edu.cmu.cs.lti.script.utils;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.script.model.EntityEventLink;
import edu.cmu.cs.lti.script.model.FeaturizedEntity;
import edu.cmu.cs.lti.script.model.FeaturizedEvent;
import edu.cmu.cs.lti.script.model.FeaturizedItem;
import edu.cmu.cs.lti.uima.util.CsvFactory;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvFeatureParser {

  public Table<FeaturizedEvent, FeaturizedEntity, EntityEventLink> linkings;

  public List<FeaturizedItem> entityTable;

  public List<FeaturizedItem> eventTable;

  public enum ItemType {
    Event, Entity
  }

  private List<FeaturizedItem> getFeatures(File file, ItemType type) throws IOException {
    CSVReader reader = CsvFactory.getCSVReader(file);

    String docId = FilenameUtils.getBaseName(file.getName());

    String[] nextLine;
    List<FeaturizedItem> featureTables = new ArrayList<FeaturizedItem>();
    while ((nextLine = reader.readNext()) != null) {
      Table<String, String, Integer> featureTable = HashBasedTable.create();
      if (nextLine.length > 1) {
        int id = Integer.parseInt(nextLine[0]);

        for (String entry : nextLine) {
          String[] f = entry.split(":");
          if (f.length == 3) {
            featureTable.put(f[0], f[1], Integer.parseInt(f[2]));
          }
        }
        FeaturizedItem item;
        if (type == ItemType.Entity) {
          item = new FeaturizedEntity(docId, id, featureTable);
        } else {
          item = new FeaturizedEvent(docId, id, featureTable);
        }
        featureTables.add(item);
      }
    }
    return featureTables;
  }

  public void intialize(File linkFile, File entityFile, File eventFile) throws IOException {
    linkings = HashBasedTable.create();

    String docId = FilenameUtils.getBaseName(linkFile.getName());
    String entityDocId = FilenameUtils.getBaseName(entityFile.getName());
    String evmDocId = FilenameUtils.getBaseName(eventFile.getName());

    if (!docId.equals(entityDocId) && !entityDocId.equals(evmDocId)) {
      System.err.println("Feature files not aligned");
    }

    System.out.println("Procesing " + entityDocId);

    TIntObjectHashMap<FeaturizedEntity> entityById = new TIntObjectHashMap<FeaturizedEntity>();

    TIntObjectHashMap<FeaturizedEvent> eventById = new TIntObjectHashMap<FeaturizedEvent>();

    entityTable = getFeatures(entityFile, ItemType.Entity);
    eventTable = getFeatures(eventFile, ItemType.Event);

    for (FeaturizedItem entity : entityTable) {
      entityById.put(entity.itemId, (FeaturizedEntity) entity);
    }

    for (FeaturizedItem event : eventTable) {
      eventById.put(event.itemId, (FeaturizedEvent) event);
    }

    CSVReader reader = CsvFactory.getCSVReader(linkFile);

    String[] nextLine;
    while ((nextLine = reader.readNext()) != null) {
      if (nextLine.length != 4) {
        System.err.println("Link file borken");
      } else {
        int eventId = Integer.parseInt(nextLine[0]);
        int entityId = Integer.parseInt(nextLine[1]);
        System.out.println(eventId + " " + entityId);
        FeaturizedEntity entity = entityById.get(entityId);
        FeaturizedEvent event = eventById.get(eventId);
        linkings.put(event, entity,
                new EntityEventLink(nextLine[2], Double.parseDouble(nextLine[3])));
      }
    }
  }
}
