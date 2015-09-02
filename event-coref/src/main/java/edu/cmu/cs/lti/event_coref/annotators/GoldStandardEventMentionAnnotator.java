package edu.cmu.cs.lti.event_coref.annotators;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.DiscontinuousComponentAnnotation;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/20/15
 * Time: 2:45 PM
 * <p>
 * Annotate event mentions based on Gold Standard, while this is useful
 * for training, it is can also be used in some evaluation case when
 * gold standard mentions are given
 *
 * @author Zhengzhong Liu
 */
public class GoldStandardEventMentionAnnotator extends AbstractAnnotator {

    public static final String COMPONENT_ID = GoldStandardEventMentionAnnotator.class.getSimpleName();

    public static final String PARAM_TARGET_VIEWS = "targetViewNames";

    @ConfigurationParameter(name = PARAM_TARGET_VIEWS)
    private String[] targetViewNames;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        final JCas goldStandard = JCasUtil.getView(aJCas, goldStandardViewName, false);
        for (String targetViewName : targetViewNames) {
            JCas targetView = JCasUtil.getView(aJCas, targetViewName, false);
            copyMentions(goldStandard, targetView);
            copyEvents(goldStandard, targetView);
        }
    }

    private void copyMentions(JCas fromView, JCas toView) {
        for (EventMention goldMention : JCasUtil.select(fromView, EventMention.class)) {
            EventMention systemMention = new EventMention(toView, goldMention.getBegin(), goldMention.getEnd());
            copyRegions(toView, goldMention, systemMention);
            systemMention.setRealisType(goldMention.getRealisType());
            systemMention.setEventType(goldMention.getEventType());
            UimaAnnotationUtils.finishAnnotation(systemMention, COMPONENT_ID, goldMention.getId(), toView);
        }
    }

    private void copyEvents(JCas from, JCas to) {
        Map<Span, EventMention> fromMentions = new HashMap<>();

        Map<EventMention, EventMention> from2toMention = new HashMap<>();

        for (EventMention fromMention : JCasUtil.select(from, EventMention.class)) {
            fromMentions.put(new Span(fromMention.getBegin(), fromMention.getEnd()), fromMention);
        }

        for (EventMention toMention : JCasUtil.select(to, EventMention.class)) {
            from2toMention.put(fromMentions.get(new Span(toMention.getBegin(), toMention.getEnd())), toMention);
        }

        for (Event event : JCasUtil.select(from, Event.class)) {
            Event copiedEvent = new Event(to);
            int fromMentionLength = event.getEventMentions().size();
            copiedEvent.setEventMentions(new FSArray(to, fromMentionLength));

            for (int i = 0; i < fromMentionLength; i++) {
                EventMention toMention = from2toMention.get(event.getEventMentions(i));
                copiedEvent.setEventMentions(i, toMention);
                toMention.setReferringEvent(copiedEvent);
            }
            copiedEvent.setEventIndex(event.getEventIndex());

            UimaAnnotationUtils.finishTop(copiedEvent, COMPONENT_ID, event.getId(), to);
        }
    }

    private void copyRegions(JCas toView, DiscontinuousComponentAnnotation from, DiscontinuousComponentAnnotation to) {
        to.setYangRegions(new FSArray(toView, from.getYangRegions().size()));
        for (int i = 0; i < from.getYangRegions().size(); i++) {
            to.setYangRegions(i, from.getYangRegions(i));
        }

        to.setYinRegions(new FSArray(toView, from.getYinRegions().size()));
        for (int i = 0; i < from.getYinRegions().size(); i++) {
            to.setYinRegions(i, from.getYinRegions(i));
        }
    }
}