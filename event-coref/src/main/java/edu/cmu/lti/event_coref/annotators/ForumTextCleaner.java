package edu.cmu.lti.event_coref.annotators;

import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.util.NoiseTextFormatter;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/21/15
 * Time: 12:38 AM
 *
 * @author Zhengzhong Liu
 */
public class ForumTextCleaner extends AbstractAnnotator {
    public static final String PARAM_INPUT_VIEW_NAME = "InputViewName";

    @ConfigurationParameter(name = PARAM_INPUT_VIEW_NAME)
    private String inputViewName;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas inputView = ViewCreatorAnnotator.createViewSafely(aJCas, inputViewName);
        String originalText = inputView.getDocumentText();
        aJCas.setDocumentText(NoiseTextFormatter.cleanForum(originalText));
    }
}
