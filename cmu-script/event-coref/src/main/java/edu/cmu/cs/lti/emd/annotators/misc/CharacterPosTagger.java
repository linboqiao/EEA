package edu.cmu.cs.lti.emd.annotators.misc;

import edu.cmu.cs.lti.script.type.CharacterAnnotation;
import edu.cmu.cs.lti.script.type.ZparTreeAnnotation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/2/16
 * Time: 2:16 PM
 *
 * @author Zhengzhong Liu
 */
public class CharacterPosTagger extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for (CharacterAnnotation character : JCasUtil.select(aJCas, CharacterAnnotation.class)) {
            for (ZparTreeAnnotation tree : JCasUtil.selectCovered(ZparTreeAnnotation.class, character)) {
                if (!tree.getIsLeaf()) {
                    character.setPos(tree.getPennTreeLabel());
                }
            }
        }
    }
}
