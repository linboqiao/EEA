package edu.cmu.cs.lti.event_coref.annotators;

import edu.cmu.cs.lti.model.Span;
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
 * Date: 5/11/15
 * Time: 3:04 PM
 *
 * @author Zhengzhong Liu
 */
public class GoldStandardEventAnnotator extends AbstractAnnotator {

    public static final String COMPONENT_ID = GoldStandardEventAnnotator.class.getSimpleName();

    public static final String PARAM_TARGET_VIEWS = "targetViewNames";

    @ConfigurationParameter(name = PARAM_TARGET_VIEWS)
    private String[] targetViewNamess;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, false);
        for (String targetViewName : targetViewNamess) {
            JCas targetView = JCasUtil.getView(aJCas, targetViewName, false);
            copyEvents(goldView, targetView);
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
}
