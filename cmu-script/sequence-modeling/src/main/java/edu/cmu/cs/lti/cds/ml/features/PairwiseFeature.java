package edu.cmu.cs.lti.cds.ml.features;

import edu.cmu.cs.lti.script.model.ContextElement;
import org.apache.uima.jcas.JCas;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/30/14
 * Time: 5:36 PM
 */
public abstract class PairwiseFeature extends Feature {
    public void prepare(JCas aJCas) {

    }

    public abstract Map<String, Double> getFeature(ContextElement elementLeft, ContextElement elementRight, int skip);

    public boolean isLexicalized() {
        return true;
    }
}