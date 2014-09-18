package edu.cmu.cs.lti.cds.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import com.google.common.collect.ArrayListMultimap;

import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.StringUtils;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

public class EventFeatureExtractor extends AbstractCsvWriterAnalysisEngine {

  List<Event> allEvents = new ArrayList<Event>();

  Map<Event, Set<Sentence>> evm2Sents;

  int eventIndex = 0;

  @Override
  protected String[] getHeader() {
    return null;
  }

  @Override
  protected void prepare(JCas aJCas) {
    evm2Sents = new HashMap<Event, Set<Sentence>>();
    for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
      for (EventMention evm : JCasUtil.selectCovered(EventMention.class, sent)) {
        Event event = evm.getReferringEvent();
        if (evm2Sents.containsKey(event)) {
          evm2Sents.get(event).add(sent);
        } else {
          Set<Sentence> sents = new HashSet<Sentence>();
          sents.add(sent);
          evm2Sents.put(event, sents);
        }
      }
    }

    allEvents = new ArrayList<Event>(JCasUtil.select(aJCas, Event.class));
  }

  @Override
  protected boolean hasNextRow() {
    return eventIndex < allEvents.size();
  }

  @Override
  protected String[] getNextCsvRow() {
    List<String> features = new ArrayList<String>();

    Event event = allEvents.get(eventIndex++);
    features.add(event.getId());

    TObjectIntHashMap<String> mentionSurfaceCount = new TObjectIntHashMap<String>();
    TObjectIntHashMap<String> lemmaCount = new TObjectIntHashMap<String>();
    for (int i = 0; i < event.getEventMentions().size(); i++) {
      EventMention mention = event.getEventMentions(i);
      String mentionSurface = UimaNlpUtils.getLemmatizedAnnotation(mention);
      mentionSurfaceCount.adjustOrPutValue(mentionSurface, 1, 1);
    }

    for (Sentence sent : evm2Sents.get(event)) {
      for (StanfordCorenlpToken word : JCasUtil.selectCovered(StanfordCorenlpToken.class, sent)) {
        if (word.getPos().startsWith("N") || word.getPos().startsWith("V")
                || word.getPos().startsWith("J")) {
          lemmaCount.adjustOrPutValue(word.getLemma().toLowerCase(), 1, 1);
        }
      }
    }

    for (TObjectIntIterator<String> iter = lemmaCount.iterator(); iter.hasNext();) {
      iter.advance();
      features.add("C:" + StringUtils.text2CsvField(iter.key()) + ":" + iter.value());
    }

    for (TObjectIntIterator<String> iter = mentionSurfaceCount.iterator(); iter.hasNext();) {
      iter.advance();
      features.add("M:" + iter.key() + ":" + iter.value());
    }

    return features.toArray(new String[features.size()]);
  }
}
