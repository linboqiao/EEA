package edu.cmu.cs.lti.emd.stat;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/18/16
 * Time: 4:38 PM
 *
 * @author Zhengzhong Liu
 */
public class EventMentionSurfaceCorverageReport extends AbstractLoggingAnnotator{
    public final static String PARAM_EVENT_MENTION_SURFACE = "eventMentionSurface";
    @ConfigurationParameter(name= PARAM_EVENT_MENTION_SURFACE)
    File outputPath;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }
}
