package edu.cmu.cs.lti.salience.annotators;

import edu.cmu.cs.lti.script.type.Body;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NaiveBodyAnnotator extends AbstractLoggingAnnotator{
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        Body body;
        List<Body> bodies = new ArrayList<>(JCasUtil.select(jCas, Body.class));
        if (bodies.size() == 1){
            body = bodies.get(0);
            body.setBegin(0);
            body.setEnd(jCas.getDocumentText().length());
        }else{
            body = new Body(jCas);
            UimaAnnotationUtils.finishAnnotation(body, 0, jCas.getDocumentText().length(),COMPONENT_ID, 0, jCas);
        }
    }
}
