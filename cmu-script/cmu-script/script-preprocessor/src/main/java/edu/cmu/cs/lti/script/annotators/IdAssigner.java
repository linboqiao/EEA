package edu.cmu.cs.lti.script.annotators;

import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.ComponentTOP;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/20/14
 * Time: 11:57 PM
 */
public class IdAssigner extends AbstractLoggingAnnotator {

    public static final String PARAM_TOP_NAMES_TO_ASSIGN = "topNames";

    public static final String PARAM_ANNOTATION_NAMES_TO_ASSIGN = "annotationNames";
    private String[] topsToAssign;
    private String[] annosToAssign;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        topsToAssign = (String[]) aContext.getConfigParameterValue(PARAM_TOP_NAMES_TO_ASSIGN);
        annosToAssign = (String[]) aContext.getConfigParameterValue(PARAM_ANNOTATION_NAMES_TO_ASSIGN);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

        if (topsToAssign != null)
            for (String topName : topsToAssign) {
                try {
                    Class<? extends ComponentTOP> clazz = Class.forName(topName).asSubclass(ComponentTOP.class);
                    UimaAnnotationUtils.assignTopIds(JCasUtil.select(aJCas, clazz));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        if (annosToAssign != null)
            for (String annoName : annosToAssign) {
                try {
                    Class<? extends ComponentAnnotation> clazz = Class.forName(annoName).asSubclass(ComponentAnnotation.class);
                    UimaAnnotationUtils.assignAnnotationIds(JCasUtil.select(aJCas, clazz));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
    }


}
