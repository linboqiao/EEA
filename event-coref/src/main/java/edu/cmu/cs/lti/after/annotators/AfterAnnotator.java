package edu.cmu.cs.lti.after.annotators;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/12/16
 * Time: 3:49 PM
 *
 * @author Zhengzhong Liu
 */
public class AfterAnnotator extends AbstractLoggingAnnotator{

    public static final String PARAM_MODEL_DIRECTORY = "modelDirectory";
    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    private File modelDirectory;

    public static final String PARAM_CONFIG = "configuration";
    @ConfigurationParameter(name = PARAM_CONFIG)
    private Configuration config;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }
}
