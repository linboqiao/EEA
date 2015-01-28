package edu.cmu.cs.lti.emd.annotators;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/27/15
 * Time: 2:19 PM
 */
public class EventMentionCandidateAnnotator extends AbstractLoggingAnnotator {

    public static String ANNOTATOR_COMPONENT_ID = EventMentionCandidateAnnotator.class.getSimpleName();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);
    }

    private void semaforCandidateFinder(JCas aJCas) {
        for (SemaforAnnotationSet annoSet : JCasUtil.select(aJCas, SemaforAnnotationSet.class)) {
            String frameName = annoSet.getFrameName();

            SemaforLabel targetLabel = null;

            List<SemaforLabel> frameElements = new ArrayList<SemaforLabel>();

            for (SemaforLayer layer : FSCollectionFactory.create(annoSet.getLayers(), SemaforLayer.class)) {
                String layerName = layer.getName();
                if (layerName.equals("Target")) {// Target that invoke the frame
                    targetLabel = layer.getLabels(0);
                } else if (layerName.equals("FE")) {// Frame element
                    FSArray elements = layer.getLabels();
                    if (elements != null) {
                        for (SemaforLabel element : FSCollectionFactory.create(elements, SemaforLabel.class)) {
                            frameElements.add(element);
                        }
                    }
                }
            }

            if (targetLabel != null) {
                List<SingleEventFeature> allFeatures = new ArrayList<>();

                Word labelHeadWord = UimaNlpUtils.findHeadFromTreeAnnotation(aJCas, targetLabel);

                if (labelHeadWord != null) {
                    EventMention evm = new EventMention(aJCas);
                    evm.setBegin(targetLabel.getBegin());
                    evm.setEnd(targetLabel.getEnd());
                    evm.addToIndexes();
                    evm.setComponentId(ANNOTATOR_COMPONENT_ID);


                }
            }
        }
    }

    private void fanseCandidateFinder(JCas aJCas) {

    }
}
