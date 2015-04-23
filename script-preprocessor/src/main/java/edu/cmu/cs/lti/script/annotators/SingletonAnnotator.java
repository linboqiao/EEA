/**
 * 
 */
package edu.cmu.cs.lti.script.annotators;

import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.Collection;

/**
 * For each mention, if there is not high level cluster, then assign it with a entity
 * 
 * @author zhengzhongliu
 * 
 */
public class SingletonAnnotator extends AbstractLoggingAnnotator {

  public static String COMPONENT_ID = SingletonAnnotator.class.getSimpleName();

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.apache.uima.jcas.JCas)
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
      logger.info(progressInfo(aJCas));
    int id = 0;
    Collection<EventMention> eventMentions = JCasUtil.select(aJCas, EventMention.class);
    for (EventMention mention : eventMentions) {
      if (mention.getReferringEvent() == null) {
        Event event = new Event(aJCas);
        event.setEventMentions(new FSArray(aJCas, 1));
        event.setEventMentions(0, mention);
        mention.setReferringEvent(event);
        UimaAnnotationUtils.finishTop(event, COMPONENT_ID, null, aJCas);
      }
      mention.setId(Integer.toString(id));
      id++;
    }

    UimaAnnotationUtils.assignAnnotationIds(eventMentions);
    UimaAnnotationUtils.assignTopIds(JCasUtil.select(aJCas, Event.class));

    Collection<EntityMention> entityMentions = JCasUtil.select(aJCas, EntityMention.class);
    for (EntityMention enm : entityMentions) {
      if (enm.getReferingEntity() == null) {
        Entity entity = new Entity(aJCas);
        entity.setEntityMentions(new FSArray(aJCas, 1));
        entity.setEntityMentions(0, enm);
        enm.setReferingEntity(entity);
        entity.setRepresentativeMention(enm);
        UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, null, aJCas);
      }
    }

    UimaAnnotationUtils.assignAnnotationIds(entityMentions);
    UimaAnnotationUtils.assignTopIds(JCasUtil.select(aJCas, Entity.class));

  }
}
