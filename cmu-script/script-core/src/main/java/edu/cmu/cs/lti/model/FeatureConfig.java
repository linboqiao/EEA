package edu.cmu.cs.lti.model;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/28/15
 * Time: 9:45 PM
 */
public class FeatureConfig {

    public enum FeatureType {
        binary, numeric, nominal
    }

    public final double defaultValue;

    public final String featureName;

    public final FeatureType type;

    public FeatureConfig(String featureName, FeatureType type, double defaultValue) {
        this.featureName = featureName;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public FeatureConfig(String featureName, FeatureType type) {
        this.featureName = featureName;
        this.type = type;
        this.defaultValue = 0;
    }
}
