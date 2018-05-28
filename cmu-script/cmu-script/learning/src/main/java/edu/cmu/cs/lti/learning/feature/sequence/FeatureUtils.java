package edu.cmu.cs.lti.learning.feature.sequence;

import com.google.common.base.Joiner;
import org.javatuples.Pair;

import java.util.Arrays;

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
     *
     * @param featureTypeAndName
     * @return
     */
    public static String formatFeatureName(Pair<String, String> featureTypeAndName) {
        return formatFeatureName(featureTypeAndName.getValue0(), featureTypeAndName.getValue1());
    }

    public static String formatFeatureName(String featureType, String featureName) {
        return String.format("%s::%s", featureType, featureName);
    }

    public static String sortedJoin(String... components) {
        Arrays.sort(components);
        return Joiner.on(":").join(components);
    }

    public static final String FEATURE_SPEC_SPITTER = "\n###\n";

    public static String[] splitFeatureSpec(String featureSpecs) {
        return featureSpecs.split(FEATURE_SPEC_SPITTER);
    }

    public static String joinFeatureSpec(String... featureSpecs) {
        return Joiner.on(FEATURE_SPEC_SPITTER).join(featureSpecs);
    }

}
