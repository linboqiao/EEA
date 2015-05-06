package edu.cmu.lti.event_coref.annotators;

import edu.cmu.cs.lti.script.type.DiscontinuousComponentAnnotation;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/20/15
 * Time: 2:45 PM
 * <p/>
 * Annotate event mentions based on Gold Standard, while this is useful
 * for training, it is can also be used in some evaluation case when
 * gold standard mentions are given
 *
 * @author Zhengzhong Liu
 */
public class GoldStandardEventMentionAnnotator extends AbstractAnnotator {

    public static final String COMPONENT_ID = GoldStandardEventMentionAnnotator.class.getSimpleName();

    public static final String PARAM_TARGET_VIEWS = "targetViewNamess";

    @ConfigurationParameter(name = PARAM_TARGET_VIEWS)
    private String[] targetViewNamess;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        final JCas goldStandard = JCasUtil.getView(aJCas, goldStandardViewName, false);
        for (String targetViewName : targetViewNamess) {
            JCas targetView = JCasUtil.getView(aJCas, targetViewName, false);
            copyMentions(goldStandard, targetView);
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