package edu.cmu.cs.lti.emd.annotators.gold;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

/**
 * Copy gold standard mention trigger (nuggets)
 * Created with IntelliJ IDEA.
 * Date: 4/15/15
 * Time: 5:09 PM
 *
 * @author Zhengzhong Liu
 */
public class GoldMentionCopier extends AbstractLoggingAnnotator {
    public static final String componentId = GoldMentionCopier.class.getSimpleName();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldStandardView = JCasUtil.getView(aJCas, goldStandardViewName, false);

        for (EventMention mention : JCasUtil.select(goldStandardView, EventMention.class)) {
            EventMention systemMention = new EventMention(aJCas, mention.getBegin(), mention.getEnd());
            systemMention.setEventType(mention.getEventType());

            if (!systemMention.getCoveredText().trim().equals("")) {
                UimaAnnotationUtils.finishAnnotation(systemMention, componentId, mention.getId(), aJCas);
            } else {
                systemMention.removeFromIndexes();
            }

        }
    }
}
