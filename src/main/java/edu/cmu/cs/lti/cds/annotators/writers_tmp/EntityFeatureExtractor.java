package edu.cmu.cs.lti.cds.annotators.writers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import com.google.common.collect.ArrayListMultimap;

import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.io.writer.AbstractCsvWriterAnalysisEngine;
import edu.cmu.cs.lti.utils.StringUtils;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

public class EntityFeatureExtractor extends AbstractCsvWriterAnalysisEngine {
  List<Entity> allEntities;

  int entityIndex;

  // ArrayListMultimap<Entity, Sentence> entity2Sents;

  Map<Entity, Set<Sentence>> entity2Sents;

  ArrayListMultimap<Sentence, String> sent2Lemmas;

  @Override
  protected String[] getHeader() {
    return null;
  }

  @Override
  protected void prepare(JCas aJCas) {
    entity2Sents = new HashMap<Entity, Set<Sentence>>();
    entityIndex = 0;

    for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
      for (EntityMention mention : JCasUtil.selectCovered(EntityMention.class, sent)) {
        Entity en = mention.getReferingEntity();
        if (entity2Sents.containsKey(en)) {
          entity2Sents.get(en).add(sent);
        } else {
          Set<Sentence> sents = new HashSet<Sentence>();
          sents.add(sent);
          entity2Sents.put(en, sents);
        }
      }
    }

    allEntities = new ArrayList<Entity>(JCasUtil.select(aJCas, Entity.class));
  }

  @Override
  protected boolean hasNextRow() {
    return entityIndex < allEntities.size();
  }

  @Override
  protected String[] getNextCsvRow() {
    List<String> features = new ArrayList<String>();
    Entity en = allEntities.get(entityIndex++);
    features.add(en.getId());

    TObjectIntHashMap<String> mentionTypeCount = new TObjectIntHashMap<String>();
    TObjectIntHashMap<String> mentionSurfaceCount = new TObjectIntHashMap<String>();
    TObjectIntHashMap<String> lemmaCount = new TObjectIntHashMap<String>();
    TObjectIntHashMap<String> mentionHeadWordCount = new TObjectIntHashMap<String>();

    // mention related features
    for (int i = 0; i < en.getEntityMentions().size(); i++) {
      EntityMention mention = en.getEntityMentions(i);
      if (mention.getEntityType() != null) {
        mentionTypeCount.adjustOrPutValue(mention.getEntityType(), 1, 1);
      }

      // do not use pronoun for mention surface feature
      if (!mention.getHead().getPos().startsWith("PR")) {
        String mentionSurface = StringUtils.text2CsvField(mention.getCoveredText()).toLowerCase();
        mentionSurfaceCount.adjustOrPutValue(mentionSurface, 1, 1);
        mentionHeadWordCount.adjustOrPutValue(mention.getHead().getLemma(), 1, 1);
      }
    }

    // contextual features
    for (Sentence sent : entity2Sents.get(en)) {
      for (StanfordCorenlpToken word : JCasUtil.selectCovered(StanfordCorenlpToken.class, sent)) {
        if (word.getPos().startsWith("N") || word.getPos().startsWith("V")
                || word.getPos().startsWith("J")) {
          lemmaCount.adjustOrPutValue(word.getLemma(), 1, 1);
        }
      }
    }

    for (TObjectIntIterator<String> iter = mentionTypeCount.iterator(); iter.hasNext();) {
      iter.advance();
      features.add("T:" + iter.key() + ":" + iter.value());
    }

    for (TObjectIntIterator<String> iter = lemmaCount.iterator(); iter.hasNext();) {
      iter.advance();
      features.add("C:" + StringUtils.text2CsvField(iter.key()) + ":" + iter.value());
    }

    for (TObjectIntIterator<String> iter = mentionSurfaceCount.iterator(); iter.hasNext();) {
      iter.advance();
      features.add("M:" + iter.key() + ":" + iter.value());
    }

    for (TObjectIntIterator<String> iter = mentionHeadWordCount.iterator(); iter.hasNext();) {
      iter.advance();
      features.add("H:" + iter.key() + ":" + iter.value());
    }

    return features.toArray(new String[features.size()]);
  }
}