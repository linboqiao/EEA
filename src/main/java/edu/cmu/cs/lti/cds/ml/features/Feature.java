package edu.cmu.cs.lti.cds.ml.features;

import edu.cmu.cs.lti.cds.model.ChainElement;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/30/14
 * Time: 5:36 PM
 */
public abstract class Feature {
    public abstract Map<String, Double> getFeature(ChainElement elementLeft, ChainElement elementRight);

    public abstract boolean isLexicalized();
}
