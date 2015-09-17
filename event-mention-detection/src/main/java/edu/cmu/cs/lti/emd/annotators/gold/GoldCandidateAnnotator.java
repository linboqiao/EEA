package edu.cmu.cs.lti.emd.annotators.gold;

import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/14/15
 * Time: 5:03 PM
 *
 * @author Zhengzhong Liu
 */
public class GoldCandidateAnnotator extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldStandardView = JCasUtil.getView(aJCas, goldStandardViewName, false);

        for (EventMention mention : JCasUtil.select(goldStandardView, EventMention.class)) {
            CandidateEventMention systemMention = new CandidateEventMention(aJCas,
                    mention.getBegin(), mention.getEnd());
            systemMention.setPredictedType(mention.getEventType());

            if (!systemMention.getCoveredText().trim().equals("")) {
                UimaAnnotationUtils.finishAnnotation(systemMention, COMPONENT_ID, mention.getId(), aJCas);
            } else {
                systemMention.removeFromIndexes();
            }
        }
    }
}
