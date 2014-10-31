package edu.cmu.cs.lti.cds.annotators.writers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import com.google.common.collect.ArrayListMultimap;

import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

public class DocumentLevelEntityWriter extends AbstractCsvWriterAnalysisEngine {

  Map<Entity, Set<Sentence>> entity2Sents;

  ArrayListMultimap<Sentence, String> sent2Lemmas;

  private Iterator<Entity> iter;

  private String docId;

  @Override
  protected String[] getHeader() {
    return null;
  }

  @Override
  protected void prepare(JCas aJCas) {
    List<Entity> allEntities = new ArrayList<Entity>(JCasUtil.select(aJCas, Entity.class));
    iter = allEntities.iterator();
    docId = UimaConvenience.getShortDocumentNameWithOffset(aJCas);
    setSeperator('\t');
  }

  @Override
  protected boolean hasNextRow() {
    return iter.hasNext();
  }

  @Override
  protected String[] getNextCsvRow() {
    List<String> features = new ArrayList<String>();
    Entity en = iter.next();
    features.add(docId + "_" + en.getId());

    TObjectIntHashMap<String> mentionTypeCount = new TObjectIntHashMap<String>();
    TObjectIntHashMap<String> mentionHeadWordCount = new TObjectIntHashMap<String>();

    // mention related features
    for (int i = 0; i < en.getEntityMentions().size(); i++) {
      EntityMention mention = en.getEntityMentions(i);
      if (mention.getEntityType() != null) {
        mentionTypeCount.adjustOrPutValue(mention.getEntityType(), 1, 1);
      }

      // do not use pronoun for mention surface feature
      if (mention.getHead().getPos().startsWith("N") || mention.getHead().getPos().startsWith("V")) {
        mentionHeadWordCount.adjustOrPutValue(mention.getHead().getLemma(), 1, 1);
      }
    }

    if (mentionHeadWordCount.size() == 0) {
      en.setInformative(false);
    } else {
      for (TObjectIntIterator<String> iter = mentionHeadWordCount.iterator(); iter.hasNext();) {
        iter.advance();
        features.add("H:" + iter.key() + ":" + iter.value());
      }

      for (TObjectIntIterator<String> iter = mentionTypeCount.iterator(); iter.hasNext();) {
        iter.advance();
        features.add("T:" + iter.key() + ":" + iter.value());
      }
    }

    return features.toArray(new String[features.size()]);
  }
}