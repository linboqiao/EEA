package edu.cmu.cs.lti.cds.annotators.patches;

import edu.cmu.cs.lti.script.type.StanfordTreeAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/30/14
 * Time: 4:21 PM
 */
public class TreeLeafFixer extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        int id = 0;
        for (StanfordTreeAnnotation tree : JCasUtil.select(aJCas, StanfordTreeAnnotation.class)) {
            tree.setId(Integer.toString(id++));
            tree.setIsLeaf(tree.getChildren().size() == 0);
        }
    }
}
