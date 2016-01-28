package edu.cmu.cs.lti.emd.annotators.postprocessors;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.emd.utils.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * During processing all double taggings are merged, we need to separate them.
 *
 * @author Zhengzhong Liu
 */
public class MentionTypeSplitter extends AbstractLoggingAnnotator {
    public static final String PARAM_COREF_RULE_FILE = "corefRulesFile";

    @ConfigurationParameter(name = PARAM_COREF_RULE_FILE)
    private File corefRules;

    ArrayListMultimap<String, String> allowedCorefs;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        allowedCorefs = ArrayListMultimap.create();

        try {
            for (String line : FileUtils.readLines(corefRules)) {
                String[] lr = line.trim().split(" ");
                if (lr.length > 2) {
                    allowedCorefs.put(lr[0], lr[1]);
                    allowedCorefs.put(lr[1], lr[0]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<EventMention> originalMentions = UimaConvenience.getAnnotationList(aJCas, EventMention.class);
        List<Event> originalEvents = UimaConvenience.getAnnotationList(aJCas, Event.class);

        ArrayListMultimap<EventMention, EventMention> splitMap = ArrayListMultimap.create();

        // Copy mentions first.
        for (EventMention candidate : originalMentions) {
            String[] predictedTypes = MentionTypeUtils.splitToTmultipleTypes(candidate.getEventType());
            // Split each stored mention by the syntactic count and the type.
            for (int syntacticType = 0; syntacticType < candidate.getMultiTag() + 1; syntacticType++) {
                for (String predictedType : predictedTypes) {
                    EventMention mention = new EventMention(aJCas);
                    mention.setBegin(candidate.getBegin());
                    mention.setEnd(candidate.getEnd());
                    mention.setEventType(predictedType);
                    mention.setRealisType(candidate.getRealisType());
                    UimaAnnotationUtils.finishAnnotation(mention, candidate.getBegin(), candidate.getEnd(),
                            COMPONENT_ID, 0, aJCas);

                    // Because we cannot allow two mentions with the same span to be in the same cluster, we will
                    // need to pick one, for now, we simply pick the first one.
                    if (syntacticType == 0) {
                        splitMap.put(candidate, mention);
                    }
                }
            }
        }

        // Group the copied mentions with the original clusters.
        ArrayListMultimap<Event, EventMention> predictedClusters = ArrayListMultimap.create();
        for (Map.Entry<EventMention, Collection<EventMention>> predicted2Splitted : splitMap.asMap().entrySet()) {
            Event predictedEvent = predicted2Splitted.getKey().getReferringEvent();
            if (predictedEvent != null) {
                predictedClusters.putAll(predicted2Splitted.getKey().getReferringEvent(),
                        predicted2Splitted.getValue());
            }
        }

        // Split the original clusters with event type.
        for (Map.Entry<Event, Collection<EventMention>> cluster : predictedClusters.asMap().entrySet()) {
            ArrayListMultimap<Integer, EventMention> subclusters = splitClusterByType(cluster.getValue());
//            logger.info("Cluster contains " + cluster.getValue().size() + " mentions , splitted as " + subclusters
// .keySet().size());

            int eventId = 0;
//            logger.info("Event " + eventId);
            for (Map.Entry<Integer, Collection<EventMention>> subcluster : subclusters.asMap().entrySet()) {
                Event event = new Event(aJCas);
                event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, subcluster.getValue()));
                for (EventMention mention : subcluster.getValue()) {
                    mention.setReferringEvent(event);
//                    logger.info(mention.getCoveredText() + " " + mention.getBegin() + " " + mention.getEnd());
                }
                UimaAnnotationUtils.finishTop(event, COMPONENT_ID, eventId++, aJCas);
            }

//            if (subclusters.keySet().size() > 1){
//                DebugUtils.pause();
//            }
        }


        for (EventMention originalMention : originalMentions) {
            originalMention.removeFromIndexes();
        }

        for (Event originalEvent : originalEvents) {
            originalEvent.removeFromIndexes();
        }
    }

    private ArrayListMultimap<Integer, EventMention> splitClusterByType(Collection<EventMention> cluster) {
        ArrayListMultimap<Integer, EventMention> subclusters = ArrayListMultimap.create();

        int nextId = 0;
        Map<String, Integer> typeId = new HashMap<>();

        for (EventMention mention : cluster) {
            String type = mention.getEventType();
            int id;

            if (typeId.containsKey(type)) {
                id = typeId.get(type);
            } else {
                id = findIdInCompatibles(type, typeId);
                if (id == -1) {
                    id = nextId;
                    nextId++;
                }
            }
            typeId.put(type, id);
            subclusters.put(id, mention);
        }

        return subclusters;
    }

    private int findIdInCompatibles(String type, Map<String, Integer> typeId) {
        List<String> compatibleTypes = allowedCorefs.get(type);
        for (String compatibleType : compatibleTypes) {
            if (typeId.containsKey(compatibleType)) {
                return typeId.get(compatibleType);
            }
        }
        return -1;
    }
}
