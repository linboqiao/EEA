package edu.cmu.cs.lti.learning.feature;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.utils.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/12/15
 * Time: 3:27 PM
 *
 * @author Zhengzhong Liu
 */
public class FeatureSpecParser {
    public static String FEATURE_FUNCTION_PACKAGE_KEY = "feature_function.package";

    public static String FEATURE_FUNCTION_NAME_KEY = "feature_function.names";

    private String featureFunctionPackageName;

    public FeatureSpecParser(String featureFunctionPackageName) {
        this.featureFunctionPackageName = featureFunctionPackageName;
    }

    public Configuration parseFeatureFunctionSpecs(String rawFeatureSpecs) {
        Configuration featureSpec = new Configuration();
        featureSpec.add(FEATURE_FUNCTION_PACKAGE_KEY, featureFunctionPackageName);
        List<String> allFunctions = new ArrayList<>();

        if (rawFeatureSpecs != null && !rawFeatureSpecs.isEmpty()) {
            for (String rawFeatureSpec : rawFeatureSpecs.split(";")) {
                String[] featureSpecKeyValue = rawFeatureSpec.split("\\s+", 2);
                String featureFunctionName = featureSpecKeyValue[0];
                allFunctions.add(featureFunctionName);

                if (featureSpecKeyValue.length == 2) {
                    parseFeatureTemplateSpec(featureSpecKeyValue[1]).entrySet().forEach(entry -> featureSpec.add(
                            featureFunctionName + "." + entry.getKey(), entry.getValue()));
                }
            }
        }

        featureSpec.add(FEATURE_FUNCTION_NAME_KEY, Joiner.on(",").join(allFunctions));

        return featureSpec;
    }

    public Map<String, String> parseFeatureTemplateSpec(String templateSpecStr) {
        Map<String, String> featureTemplateSpec = new HashMap<>();
        for (String s : templateSpecStr.split(":")) {
            String[] keyVal = s.split("=", 2);
            featureTemplateSpec.put(keyVal[0], keyVal[1]);
        }
        return featureTemplateSpec;
    }
}