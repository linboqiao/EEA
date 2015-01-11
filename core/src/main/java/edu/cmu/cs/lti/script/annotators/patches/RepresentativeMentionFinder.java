/**
 * 
 */
package edu.cmu.cs.lti.script.annotators.patches;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;

/**
 * @author zhengzhongliu
 * 
 */
public class RepresentativeMentionFinder extends JCasAnnotator_ImplBase {

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.apache.uima.jcas.JCas)
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
      EntityMention firstProperNounMention = null;
      EntityMention firstCommonNounMention = null;
      EntityMention firstPronounMention = null;

      for (EntityMention mention : FSCollectionFactory.create(entity.getEntityMentions(),
              EntityMention.class)) {
        if (mention.getHead().getPos().startsWith("NNP")) {
          firstProperNounMention = mention;
          break;
        } else if (mention.getHead().getPos().startsWith("NN")) {
          if (firstCommonNounMention == null)
            firstCommonNounMention = mention;
        } else {
          if (firstPronounMention == null)
            firstPronounMention = mention;
        }
      }

      if (firstProperNounMention != null) {
        entity.setRepresentativeMention(firstProperNounMention);
      } else if (firstCommonNounMention != null) {
        entity.setRepresentativeMention(firstCommonNounMention);
      } else {
        entity.setRepresentativeMention(firstPronounMention);
      }
    }
  }
}
