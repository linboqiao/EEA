package edu.cmu.cs.lti.script.annotators.patches;

import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;

public class DuplicatedMentionRemover extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    Table<Integer, Integer, EntityMention> mentionByBeginEnd = HashBasedTable.create();

    List<EntityMention> allMentions = UimaConvenience.getAnnotationList(aJCas, EntityMention.class);

    for (EntityMention mention : allMentions) {
      int b = mention.getBegin();
      int e = mention.getEnd();
      if (mentionByBeginEnd.contains(b, e)) {
        EntityMention duplicateMention = mentionByBeginEnd.get(b, e);
        EntityMention remainingOne = selectMentionToDelete(aJCas, mention, duplicateMention);
        mentionByBeginEnd.put(b, e, remainingOne);
      } else {
        mentionByBeginEnd.put(b, e, mention);
      }
    }
  }

  private EntityMention selectMentionToDelete(JCas aJCas, EntityMention m1, EntityMention m2) {
    int s1 = getClusterSize(m1);
    int s2 = getClusterSize(m2);
    if (s1 >= s2) {
      UimaAnnotationUtils.removeEntityMention(aJCas, m2);
      return m1;
    } else {
      UimaAnnotationUtils.removeEntityMention(aJCas, m1);
      return m2;
    }
  }

  private int getClusterSize(EntityMention m1) {
    return m1.getReferingEntity().getEntityMentions().size();
  }

}
