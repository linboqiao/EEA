package edu.cmu.cs.lti.emd.annotators;

import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import weka.classifiers.Classifier;
import weka.core.SerializationHelper;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/1/15
 * Time: 2:55 PM
 */
public class EventMentionPredictor extends AbstractLoggingAnnotator {
    public static final String PARAM_MODEL_PATH = "modelPath";

    @ConfigurationParameter(name = PARAM_MODEL_PATH)
    private String modelPath;

    private Classifier classifier;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        try {
            classifier = (Classifier) SerializationHelper.read(modelPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for (CandidateEventMention mention : JCasUtil.select(aJCas, CandidateEventMention.class)) {

        }


    }
}
