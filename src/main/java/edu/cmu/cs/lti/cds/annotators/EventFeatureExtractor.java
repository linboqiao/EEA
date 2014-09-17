package edu.cmu.cs.lti.cds.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import com.google.common.collect.ArrayListMultimap;

import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine;

public class EventFeatureExtractor extends AbstractCsvWriterAnalysisEngine {

  List<Event> allEvents = new ArrayList<Event>();

  ArrayListMultimap<Event, Sentence> evm2Sents;

  int eventIndex = 0;

  @Override
  protected String[] getHeader() {
    return null;
  }

  @Override
  protected void prepare(JCas aJCas) {
    evm2Sents = ArrayListMultimap.create();
    for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
      for (EventMention evm : JCasUtil.selectCovered(EventMention.class, sent)) {
        evm2Sents.put(evm.getReferringEvent(), sent);
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
    for (Event evm : evm2Sents.keySet()) {
      
    }

    return features.toArray(new String[features.size()]);
  }
}
