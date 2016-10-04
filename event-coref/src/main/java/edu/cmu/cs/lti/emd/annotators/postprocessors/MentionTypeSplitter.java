package edu.cmu.cs.lti.emd.annotators.postprocessors;

import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.lang.reflect.InvocationTargetException;
import java.util.List;


/**
 * During processing all double taggings are merged, we need to separate them. This do not handle coreference so
 * coreference should be done after it, or handle the splitting itself.
 *
 * @author Zhengzhong Liu
 */
public class MentionTypeSplitter extends AbstractLoggingAnnotator {

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<EventMention> originalMentions = UimaConvenience.getAnnotationList(aJCas, EventMention.class);

        // Copy mentions.
        for (EventMention candidate : originalMentions) {
            String[] predictedTypes = MentionTypeUtils.splitToMultipleTypes(candidate.getEventType());
            // Split each stored mention by the duplicated type count.
            for (int duplicateTagCount = 0; duplicateTagCount < candidate.getMultiTag() + 1; duplicateTagCount++) {
                for (String predictedType : predictedTypes) {
                    EventMention mention = new EventMention(aJCas);
//                    mention.setBegin(candidate.getBegin());
//                    mention.setEnd(candidate.getEnd());
//                    mention.setRealisType(candidate.getRealisType());
//                    mention.setHeadWord(candidate.getHeadWord());
//                    mention.setArguments(candidate.getArguments());
                    try {
                        BeanUtils.copyProperties(mention, candidate);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    // Create new FSLists so that we won't get warnings.
                    mention.setArguments(duplicateArgumentFS(aJCas, candidate));

                    // Mention type should be set explicitly.
                    mention.setEventType(predictedType);
                    UimaAnnotationUtils.finishAnnotation(mention, candidate.getBegin(), candidate.getEnd(),
                            COMPONENT_ID, 0, aJCas);
                }
            }
        }

        for (EventMention originalMention : originalMentions) {
            originalMention.removeFromIndexes();
        }
    }

    private FSList duplicateArgumentFS(JCas aJCas, EventMention mention) {
        FSList argumentsFS = mention.getArguments();

        if (argumentsFS == null) {
            return null;
        }

        return FSCollectionFactory.createFSList(aJCas, FSCollectionFactory.create(argumentsFS,
                EventMentionArgumentLink.class));
    }
}
