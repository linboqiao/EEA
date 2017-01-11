package edu.cmu.cs.lti.event_coref.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;
import java.util.*;

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
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PARAM_TARGET_VIEWS = "targetViewNames";

    public static final String PARAM_COPY_MENTION_TYPE = "copyMentionType";

    public static final String PARAM_COPY_REALIS = "copyRealis";

    public static final String PARAM_COPY_CLUSTER = "copyCluster";

    public static final String PARAM_COPY_RELATIONS = "copyRelations";

    @ConfigurationParameter(name = PARAM_TARGET_VIEWS)
    private String[] targetViewNames;

    @ConfigurationParameter(name = PARAM_COPY_MENTION_TYPE, defaultValue = "false")
    private boolean copyMentionType;

    @ConfigurationParameter(name = PARAM_COPY_REALIS, defaultValue = "false")
    private boolean copyRealis;

    @ConfigurationParameter(name = PARAM_COPY_CLUSTER, defaultValue = "false")
    private boolean copyCluster;

//    @ConfigurationParameter(name = PARAM_MERGE_SAME_SPAN, defaultValue = "false")
//    private boolean mergeTypes;

    @ConfigurationParameter(name = PARAM_COPY_RELATIONS, defaultValue = "false")
    private boolean copyRelations;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        final JCas goldStandard = JCasUtil.getView(aJCas, goldStandardViewName, false);

        for (String targetViewName : targetViewNames) {
            JCas targetView = JCasUtil.getView(aJCas, targetViewName, false);


            Map<EventMention, EventMention> from2toMentionMap = copyMentions(goldStandard, targetView);
            Map<EventMentionSpan, EventMentionSpan> from2toSpanMap = copyMentionSpans(goldStandard, targetView,
                    from2toMentionMap);

            if (copyCluster) {
                copyEvents(goldStandard, targetView, from2toMentionMap);
            }

            if (copyRelations) {
                copyRelations(goldStandard, targetView, from2toSpanMap);
            }
        }
    }

    private void copyRelations(JCas fromView, JCas toView, Map<EventMentionSpan, EventMentionSpan> from2toSpanMap) {
        for (EventMentionSpanRelation relation : UimaConvenience.getAnnotationList(toView,
                EventMentionSpanRelation.class)) {
            relation.removeFromIndexes();
        }

        Collection<EventMentionSpanRelation> sourceRelations = JCasUtil.select(fromView,
                EventMentionSpanRelation.class);

        ArrayListMultimap<EventMentionSpan, EventMentionSpanRelation> headRelations = ArrayListMultimap.create();
        ArrayListMultimap<EventMentionSpan, EventMentionSpanRelation> childRelations = ArrayListMultimap.create();

        for (EventMentionSpanRelation relation : JCasUtil.select(fromView, EventMentionSpanRelation.class)) {
            EventMentionSpan copiedHead = from2toSpanMap.get(relation.getHead());
            EventMentionSpan copiedChild = from2toSpanMap.get(relation.getChild());

            EventMentionSpanRelation copiedRelation = new EventMentionSpanRelation(toView);

            copiedRelation.setHead(copiedHead);
            copiedRelation.setChild(copiedChild);
            copiedRelation.setRelationType(relation.getRelationType());

            headRelations.put(copiedChild, copiedRelation);
            childRelations.put(copiedHead, copiedRelation);

            UimaAnnotationUtils.finishTop(copiedRelation, COMPONENT_ID, relation.getId(), toView);
        }

        for (EventMentionSpan child : headRelations.keySet()) {
            child.setHeadEventRelations(FSCollectionFactory.createFSList(toView, headRelations.get(child)));
        }

        for (EventMentionSpan head : childRelations.keySet()) {
            head.setChildEventRelations(FSCollectionFactory.createFSList(toView, childRelations.get(head)));
        }
    }

    private Map<EventMention, EventMention> copyMentions(JCas fromView, JCas toView) {
        // Delete the mentions from the target view first.
        for (EventMention mention : UimaConvenience.getAnnotationList(toView, EventMention.class)) {
            mention.removeFromIndexes();
        }

        for (EventMentionSpan eventMentionSpan : UimaConvenience.getAnnotationList(toView, EventMentionSpan.class)) {
            eventMentionSpan.removeFromIndexes();
        }

        Map<EventMention, EventMention> from2toMentionMap = new HashMap<>();

        for (EventMention goldMention : JCasUtil.select(fromView, EventMention.class)) {
            if (validate(goldMention, toView)) {
                EventMention copiedMention = copyMention(toView, goldMention, goldMention.getEventType());
                from2toMentionMap.put(goldMention, copiedMention);
            }
        }


        return from2toMentionMap;
    }

    private Map<EventMentionSpan, EventMentionSpan> copyMentionSpans(JCas fromView, JCas toView,
                                                                     Map<EventMention, EventMention>
                                                                             from2toMentionMap) {
        for (EventMentionSpan eventMentionSpan : UimaConvenience.getAnnotationList(toView, EventMentionSpan.class)) {
            eventMentionSpan.removeFromIndexes();
        }

        Map<EventMentionSpan, EventMentionSpan> from2toSpanMap = new HashMap<>();

        for (EventMentionSpan goldMention : JCasUtil.select(fromView, EventMentionSpan.class)) {
            if (validate(goldMention, toView)) {
                EventMentionSpan copiedMention = copyMentionSpan(toView, goldMention, from2toMentionMap);
                from2toSpanMap.put(goldMention, copiedMention);
            }
        }

        return from2toSpanMap;
    }


    private EventMention copyMention(JCas toView, EventMention sourceMention, String mentionType) {
        EventMention targetMention = new EventMention(toView, sourceMention.getBegin(), sourceMention.getEnd());

        copyRegions(toView, sourceMention, targetMention);
        if (copyMentionType) {
            targetMention.setEventType(mentionType);
        }

        if (copyRealis) {
            if (sourceMention.getRealisType() != null) {
                targetMention.setRealisType(sourceMention.getRealisType());
            } else {
                // If the gold standard didn't provide a realis type, we make it up.
                sourceMention.setRealisType("Actual");
                targetMention.setRealisType("Actual");
            }
        }
        UimaAnnotationUtils.finishAnnotation(targetMention, COMPONENT_ID, sourceMention.getId(), toView);

        return targetMention;
    }

    private EventMentionSpan copyMentionSpan(JCas toView, EventMentionSpan sourceSpan,
                                             Map<EventMention, EventMention> from2toMentionMap) {
        EventMentionSpan ems = new EventMentionSpan(toView, sourceSpan.getBegin(), sourceSpan.getEnd());

        List<EventMention> targetMentions = new ArrayList<>();

        Word headWord = null;
        for (EventMention srcMention : FSCollectionFactory.create(sourceSpan.getEventMentions(), EventMention.class)) {
            EventMention targetMention = from2toMentionMap.get(srcMention);
            targetMentions.add(targetMention);
            headWord = targetMention.getHeadWord();
        }

        ems.setEventMentions(FSCollectionFactory.createFSList(toView, targetMentions));
        ems.setEventType(sourceSpan.getEventType());
        ems.setRealisType(sourceSpan.getRealisType());
        ems.setHeadWord(headWord);

        UimaAnnotationUtils.finishAnnotation(ems, COMPONENT_ID, sourceSpan.getId(), toView);
        return ems;
    }


    private boolean validate(ComponentAnnotation goldMention, JCas targetView) {
        if (targetView.getDocumentText().substring(goldMention.getBegin(), goldMention.getEnd()).trim().equals("")) {
            return false;
        }
        return true;
    }

    private void copyEvents(JCas fromView, JCas toView, Map<EventMention, EventMention> from2toMentions) {
        // Delete events first.
        for (Event event : UimaConvenience.getAnnotationList(toView, Event.class)) {
            event.removeFromIndexes();
        }

        for (Event event : JCasUtil.select(fromView, Event.class)) {
            Event copiedEvent = new Event(toView);
            int fromMentionLength = event.getEventMentions().size();

            List<EventMention> copiedMentions = new ArrayList<>();

            for (int i = 0; i < fromMentionLength; i++) {
                EventMention toMention = from2toMentions.get(event.getEventMentions(i));
                if (toMention != null) {
                    copiedMentions.add(toMention);
                    toMention.setReferringEvent(copiedEvent);
                }
            }

            copiedEvent.setEventMentions(FSCollectionFactory.createFSArray(toView, copiedMentions));
            UimaAnnotationUtils.finishTop(copiedEvent, COMPONENT_ID, event.getId(), toView);
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