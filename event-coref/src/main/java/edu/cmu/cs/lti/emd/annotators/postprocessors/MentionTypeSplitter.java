package edu.cmu.cs.lti.emd.annotators.postprocessors;

import edu.cmu.cs.lti.uima.util.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.apache.commons.beanutils.BeanUtils.copyProperty;


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
            String originComponent = candidate.getComponentId();

            String[] predictedTypes = MentionTypeUtils.splitToMultipleTypes(candidate.getEventType());
            // Split each stored mention by the duplicated type count.
            for (int duplicateTagCount = 0; duplicateTagCount < candidate.getMultiTag() + 1; duplicateTagCount++) {
                for (String predictedType : predictedTypes) {
                    EventMention mention = new EventMention(aJCas);
                    try {
                        copyProperties(mention, candidate);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        logger.warn("");
                    }

                    // Copy the regions explicitly.
                    mention.setRegions(duplicateRegionFS(aJCas, candidate));

                    // Create new FSLists so that we won't get warnings.
                    mention.setArguments(duplicateArgumentFS(aJCas, candidate));

                    // Mention type should be set explicitly.
                    mention.setEventType(predictedType);

                    UimaAnnotationUtils.finishAnnotation(mention, candidate.getBegin(), candidate.getEnd(),
                            originComponent, 0, aJCas);
                }
            }
        }

        for (EventMention originalMention : originalMentions) {
            originalMention.removeFromIndexes();
        }
    }

    private void copyProperties(Object dest, Object orig) throws InvocationTargetException, IllegalAccessException {
        PropertyDescriptor[] origDescriptors =
                BeanUtilsBean.getInstance().getPropertyUtils().getPropertyDescriptors(orig);
        for (int i = 0; i < origDescriptors.length; i++) {
            String name = origDescriptors[i].getName();
            if ("class".equals(name)) {
                continue; // No point in trying to set an object's class.
            }

            if ("regions".equals(name)) {
                continue; // We ignore regions because sometimes the setter might not be found.
            }

            if (BeanUtilsBean.getInstance().getPropertyUtils().isReadable(orig, name) &&
                    BeanUtilsBean.getInstance().getPropertyUtils().isWriteable(dest, name)) {
                try {
                    Object value = BeanUtilsBean.getInstance().getPropertyUtils().getSimpleProperty(orig, name);
                    copyProperty(dest, name, value);
                } catch (NoSuchMethodException e) {
                    // Should not happen.
                }
            }
        }
    }

    private FSArray duplicateRegionFS(JCas aJCas, EventMention mention) {
        FSArray regionsFS = mention.getRegions();

        if (regionsFS == null) {
            return null;
        }

        return FSCollectionFactory.createFSArray(aJCas, FSCollectionFactory.create(regionsFS, Annotation.class));
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
