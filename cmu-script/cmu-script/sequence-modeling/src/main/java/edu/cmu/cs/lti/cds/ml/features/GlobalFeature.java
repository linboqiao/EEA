package edu.cmu.cs.lti.cds.ml.features;

import edu.cmu.cs.lti.script.model.ContextElement;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/22/15
 * Time: 4:28 PM
 */
public abstract class GlobalFeature extends Feature {
    protected List<ContextElement> elements;

    public void preprocessChain(List<ContextElement> elements) {
        this.elements = elements;
    }

    public abstract Map<String, Double> getFeature(ContextElement targetElement, int targetIndex);

    public boolean isLexicalized() {
        return false;
    }
}
