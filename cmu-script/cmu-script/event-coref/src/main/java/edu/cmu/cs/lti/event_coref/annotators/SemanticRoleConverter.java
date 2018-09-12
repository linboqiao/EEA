package edu.cmu.cs.lti.event_coref.annotators;

import edu.cmu.cs.lti.ling.FrameDataReader;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/12/15
 * Time: 11:37 PM
 *
 * @author Zhengzhong Liu
 */
public class SemanticRoleConverter extends AbstractLoggingAnnotator {
    public static final String PARAM_FRAME_MAPPING_PATH = "frameMappingPath";

    @ConfigurationParameter(name = PARAM_FRAME_MAPPING_PATH)
    private String frameMappingPath;

    private String defaultVn2FnPath = "vn-fn/VN-FNRoleMapping.txt";

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        FrameDataReader.getFN2VNFrameMap(new File(frameMappingPath, defaultVn2FnPath).getAbsolutePath(), false);
    }


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }
}
