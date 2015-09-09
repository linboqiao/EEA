package edu.cmu.cs.lti.emd.learn.feature;

import org.javatuples.Pair;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 11:49 PM
 *
 * @author Zhengzhong Liu
 */
public class FeatureUtils {
    /**
     * Formate the feature name by combining the type and name.
     * @param featureTypeAndName
     * @return
     */
    public static String formatFeatureName(Pair<String, String> featureTypeAndName) {
        return formatFeatureName(featureTypeAndName.getValue0(), featureTypeAndName.getValue1());
    }

    public static String formatFeatureName(String featureType, String featureName) {
        return String.format("%s::%s", featureType, featureName);
    }
}
