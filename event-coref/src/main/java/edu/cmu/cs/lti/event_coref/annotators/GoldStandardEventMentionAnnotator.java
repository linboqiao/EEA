package edu.cmu.cs.lti.event_coref.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.uima.util.MentionTypeUtils;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.DiscontinuousComponentAnnotation;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
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

    public static final String PARAM_MERGE_SAME_SPAN = "mergeSameSpan";

    @ConfigurationParameter(name = PARAM_TARGET_VIEWS)
    private String[] targetViewNames;

    @ConfigurationParameter(name = PARAM_COPY_MENTION_TYPE, defaultValue = "false")
    private boolean copyMentionType;

    @ConfigurationParameter(name = PARAM_COPY_REALIS, defaultValue = "false")
    private boolean copyRealis;

    @ConfigurationParameter(name = PARAM_COPY_CLUSTER, defaultValue = "false")
    private boolean copyCluster;

    @ConfigurationParameter(name = PARAM_MERGE_SAME_SPAN, defaultValue = "false")
    private boolean mergeTypes;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        final JCas goldStandard = JCasUtil.getView(aJCas, goldStandardViewName, false);

        for (String targetViewName : targetViewNames) {
            JCas targetView = JCasUtil.getView(aJCas, targetViewName, false);


            Map<EventMention, EventMention> from2toMentionMap = mergeTypes ?
                    copyMentionsWithMerge(goldStandard, targetView) : copyMentions(goldStandard, targetView);

            if (copyCluster) {
                copyEvents(goldStandard, targetView, from2toMentionMap);
            }
        }
    }

    private Map<EventMention, EventMention> copyMentions(JCas fromView, JCas toView) {
        // Delete the mentions from the target view first.
        for (EventMention mention : UimaConvenience.getAnnotationList(toView, EventMention.class)) {
            mention.removeFromIndexes();
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

    /**
     * Copy the mentions so that double tagged event mentions are merged.
     *
     * @param fromView
     * @param toView
     * @return
     */
    private Map<EventMention, EventMention> copyMentionsWithMerge(JCas fromView, JCas toView) {
        // Delete the mentions first.
        for (EventMention mention : UimaConvenience.getAnnotationList(toView, EventMention.class)) {
            mention.removeFromIndexes();
        }

        // Here we copy multiple mentions into one, so multiple "from" can be mapped to one mention.
        Map<EventMention, EventMention> from2toMentionMap = new HashMap<>();

        ArrayListMultimap<Span, EventMention> spanToMentions = ArrayListMultimap.create();

        // Here is what we do about double tagging for sequence training.
        // We merge all mentions with same span to one single mention.
        // 1. If it is multi-type tagging, that means we are creating a new type by joining the types.
        // 2. If it is a syntactic multi tagging, that means we discard one of them.
        for (EventMention goldMention : JCasUtil.select(fromView, EventMention.class)) {
            if (validate(goldMention, toView)) {
                Span mentionSpan = Span.of(goldMention.getBegin(), goldMention.getEnd());
                spanToMentions.put(mentionSpan, goldMention);
            }
        }

        for (Map.Entry<Span, Collection<EventMention>> shareSpanMentions : spanToMentions.asMap().entrySet()) {
            Collection<EventMention> allSharedMentions = shareSpanMentions.getValue();

            // Create a merged type.
            TObjectIntMap<String> allTypes = new TObjectIntHashMap<>();
            EventMention aSharedMention = null;
            for (EventMention mention : allSharedMentions) {
                allTypes.adjustOrPutValue(mention.getEventType(), 1, 1);
//                logger.info(mention.getEventType());
                if (aSharedMention == null) {
                    aSharedMention = mention;
                }
            }

            String jointType = MentionTypeUtils.joinMultipleTypes(allTypes.keySet());

            // TODO investigate this, if it is correct here we don't need to filter later.
            int repeatCount = 0;
            for (TObjectIntIterator<String> iter = allTypes.iterator(); iter.hasNext(); ) {
                iter.advance();
//                logger.info(iter.key() + " " + iter.value());
                if (iter.value() > repeatCount) {
                    if (repeatCount != 0) {
                        logger.warn("Repeat count of different types are different!");
                    }
                    repeatCount = iter.value();
                }
            }

            EventMention copiedMention = copyMention(toView, aSharedMention, jointType);
            // Record the number of times of multi tagging of the same type on the same span
            // the multi sense counts are not counted here.
            // We set this to (repeat count - 1) to record only additional mentions.
            copiedMention.setMultiTag(repeatCount - 1);

            // Multiple mentions can be mapped to one copied mention.
            for (EventMention mention : allSharedMentions) {
                from2toMentionMap.put(mention, copiedMention);
            }
        }
        return from2toMentionMap;
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
            }else{
                // If the gold standard didn't provide a realis type, we make it up.
                sourceMention.setRealisType("Actual");
                targetMention.setRealisType("Actual");
            }
        }
        UimaAnnotationUtils.finishAnnotation(targetMention, COMPONENT_ID, sourceMention.getId(), toView);

        return targetMention;
    }

    private boolean validate(EventMention goldMention, JCas targetView) {
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