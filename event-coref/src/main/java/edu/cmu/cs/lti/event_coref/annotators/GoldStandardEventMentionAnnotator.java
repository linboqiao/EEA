package edu.cmu.cs.lti.event_coref.annotators;

import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.script.type.DiscontinuousComponentAnnotation;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;
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
            Map<EventMention, EventMention> from2toMentionMap = copyMentions(goldStandard, targetView);
            copyEvents(goldStandard, targetView, from2toMentionMap);
        }
    }

    private Map<EventMention, EventMention> copyMentions(JCas fromView, JCas toView) {
        Map<EventMention, EventMention> from2toMentionMap = new HashMap<>();
        for (EventMention goldMention : JCasUtil.select(fromView, EventMention.class)) {
            EventMention systemMention = new EventMention(toView, goldMention.getBegin(), goldMention.getEnd());
            copyRegions(toView, goldMention, systemMention);
            systemMention.setRealisType(goldMention.getRealisType());
            systemMention.setEventType(goldMention.getEventType());
            UimaAnnotationUtils.finishAnnotation(systemMention, COMPONENT_ID, goldMention.getId(), toView);
            from2toMentionMap.put(goldMention, systemMention);
        }
        return from2toMentionMap;
    }

    private void copyEvents(JCas from, JCas to, Map<EventMention, EventMention> from2toMentionMap) {
        for (Event event : JCasUtil.select(from, Event.class)) {
            Event copiedEvent = new Event(to);
            int fromMentionLength = event.getEventMentions().size();
            copiedEvent.setEventMentions(new FSArray(to, fromMentionLength));

            for (int i = 0; i < fromMentionLength; i++) {
                EventMention toMention = from2toMentionMap.get(event.getEventMentions(i));
                copiedEvent.setEventMentions(i, toMention);
                toMention.setReferringEvent(copiedEvent);
            }
            copiedEvent.setEventIndex(event.getEventIndex());

            UimaAnnotationUtils.finishTop(copiedEvent, COMPONENT_ID, event.getId(), to);
        }
    }

    private void copyRegions(JCas toView, DiscontinuousComponentAnnotation from, DiscontinuousComponentAnnotation to) {
        if (from.getRegions() != null) {
            to.setRegions(new FSArray(toView, from.getRegions().size()));
            for (int i = 0; i < from.getRegions().size(); i++) {
                to.setRegions(i, from.getRegions(i));
            }
        }
    }

    public static void main(String[] argv) throws UIMAException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(
                "TaskEventMentionDetectionTypeSystem");

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
                "data/mention/kbp/LDC2015E73", "preprocessed");

        AnalysisEngineDescription goldCopier = AnalysisEngineFactory.createEngineDescription(
                GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS, new String[]{CAS.NAME_DEFAULT_SOFA}
        );

        AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                TbfStyleEventWriter.class, typeSystemDescription,
                TbfStyleEventWriter.PARAM_OUTPUT_PATH, "test.tbf",
                TbfStyleEventWriter.PARAM_SYSTEM_ID, "gold"
        );

        SimplePipeline.runPipeline(reader, goldCopier, resultWriter);
    }
}