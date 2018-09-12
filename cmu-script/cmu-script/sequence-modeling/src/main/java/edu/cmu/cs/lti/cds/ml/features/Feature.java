package edu.cmu.cs.lti.cds.ml.features;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/22/15
 * Time: 4:29 PM
 */
public abstract class Feature {
    protected boolean testMode = false;

    public abstract boolean isLexicalized();

    public void setTestMode() {
        testMode = true;
    }
}
