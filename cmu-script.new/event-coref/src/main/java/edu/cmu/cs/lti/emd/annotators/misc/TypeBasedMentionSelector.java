package edu.cmu.cs.lti.emd.annotators.misc;

import edu.cmu.cs.lti.uima.util.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/3/16
 * Time: 8:56 PM
 *
 * @author Zhengzhong Liu
 */
public class TypeBasedMentionSelector extends AbstractAnnotator {
    public static final String PARAM_SELECTED_MENTION_TYPE_FILE = "selectedMentionTypeFile";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ConfigurationParameter(name = PARAM_SELECTED_MENTION_TYPE_FILE)
    private File selectedMentionTypeFile;

    Set<String> selectedTypes;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        selectedTypes = new HashSet<>();
        try {
            for (String line : FileUtils.readLines(selectedMentionTypeFile)) {
                String t = MentionTypeUtils.canonicalize(line);
                selectedTypes.add(t);
                logger.info("Selected " + t);
            }
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Set<EventMention> mentionToDelete = new HashSet<>();
        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            String t = MentionTypeUtils.canonicalize(mention.getEventType());
            if (!selectedTypes.contains(t)) {
                mentionToDelete.add(mention);
            }
        }

        List<Event> eventsToDelete = new ArrayList<>();
        for (Event event : JCasUtil.select(aJCas, Event.class)) {
            List<EventMention> filteredMentions = new ArrayList<>();
            for (EventMention eventMention : FSCollectionFactory.create(event.getEventMentions(), EventMention.class)) {
                if (!mentionToDelete.contains(eventMention)) {
                    filteredMentions.add(eventMention);
                }
            }

            if (filteredMentions.size() > 0) {
                event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, filteredMentions));
            } else {
                eventsToDelete.add(event);
            }
        }

        for (EventMention eventMention : mentionToDelete) {
            eventMention.removeFromIndexes(aJCas);
        }

        for (Event event : eventsToDelete) {
            event.removeFromIndexes(aJCas);
        }
    }
}
