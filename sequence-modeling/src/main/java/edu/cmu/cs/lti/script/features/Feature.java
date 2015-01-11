package edu.cmu.cs.lti.script.features;

import edu.cmu.cs.lti.script.model.ContextElement;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/30/14
 * Time: 5:36 PM
 */
public abstract class Feature {
    public abstract Map<String, Double> getFeature(ContextElement elementLeft, ContextElement elementRight, int skip);

    public abstract boolean isLexicalized();
}
