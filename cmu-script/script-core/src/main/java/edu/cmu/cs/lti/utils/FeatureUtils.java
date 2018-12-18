package edu.cmu.cs.lti.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/19/18
 * Time: 9:25 PM
 *
 * @author Zhengzhong Liu
 */
public class FeatureUtils {
    public static class SimpleInstance {
        public String instanceName;

        public Map<String, Double> featureMap;

        public int label;

        public SimpleInstance() {
            featureMap = new HashMap<>();
        }

        public Map<String, Double> getFeatureMap() {
            return featureMap;
        }

        public int getLabel() {
            return label;
        }

        public String getInstanceName() {
            return instanceName;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(instanceName).append("\t").append(label);
            featureMap.keySet().stream().sorted().forEach(f ->
                    sb.append("\t").append(String.format("%s:%.5f", f, featureMap.get(f)))
            );
            return sb.toString();
        }
    }
}
