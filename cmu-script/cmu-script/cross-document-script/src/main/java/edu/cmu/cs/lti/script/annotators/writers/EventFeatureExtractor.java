package edu.cmu.cs.lti.script.annotators.writers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
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
    eventIndex = 0;
  }

  @Override
  protected boolean hasNextRow() {
    return eventIndex < allEvents.size();
  }

  @Override
  protected String[] getNextCsvRow() {
    List<String> features = new ArrayList<String>();

    // we start with a trivial single mention event, but we should still treat it as a list
    Event event = allEvents.get(eventIndex++);
    features.add(event.getId());

    TObjectIntHashMap<String> mentionSurfaceCount = new TObjectIntHashMap<String>();
    TObjectIntHashMap<String> lemmaCount = new TObjectIntHashMap<String>();
    TObjectIntHashMap<String> headwordCount = new TObjectIntHashMap<String>();
    for (int i = 0; i < event.getEventMentions().size(); i++) {
      EventMention mention = event.getEventMentions(i);
      Word headWord = mention.getHeadWord();
      String mentionSurface = UimaNlpUtils.getLemmatizedAnnotation(mention);
      mentionSurfaceCount.adjustOrPutValue(mentionSurface, 1, 1);
      headwordCount.adjustOrPutValue(headWord.getCoveredText(), 1, 1);
    }

    for (Sentence sent : evm2Sents.get(event)) {
      for (StanfordCorenlpToken word : JCasUtil.selectCovered(StanfordCorenlpToken.class, sent)) {
        if (word.getPos().startsWith("N") || word.getPos().startsWith("V")
                || word.getPos().startsWith("J")) {
          lemmaCount.adjustOrPutValue(word.getLemma(), 1, 1);
        }
      }
    }

    for (TObjectIntIterator<String> iter = headwordCount.iterator(); iter.hasNext();) {
      iter.advance();
      features.add("H:" + iter.key() + ":" + iter.value());
    }
    for (TObjectIntIterator<String> iter = mentionSurfaceCount.iterator(); iter.hasNext();) {
      iter.advance();
      features.add("M:" + iter.key() + ":" + iter.value());
    }

    for (TObjectIntIterator<String> iter = lemmaCount.iterator(); iter.hasNext();) {
      iter.advance();
      features.add("C:" + StringUtils.text2CsvField(iter.key()) + ":" + iter.value());
    }

    return features.toArray(new String[features.size()]);
  }
}
