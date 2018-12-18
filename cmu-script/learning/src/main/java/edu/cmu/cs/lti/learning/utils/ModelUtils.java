package edu.cmu.cs.lti.learning.utils;

import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/3/16
 * Time: 10:28 PM
 *
 * @author Zhengzhong Liu
 */
public class ModelUtils {
    public static final String finalModelSuffix = "all";

    public static String getTestModelFile(String base, Configuration config) {
        return getTestModelFile(base, config, finalModelSuffix);
    }

    public static String getTestModelFile(String modelDir, Configuration config, String suffix) {
        String modelChoice = config.get("edu.cmu.cs.lti.model.test.choice");
        String modelName = modelChoice == null ? suffix : suffix + "_" + modelChoice;
        return FileUtils.joinPaths(modelDir, config.get("edu.cmu.cs.lti.model.name"),
                config.get("edu.cmu.cs.lti.model.type"), modelName);
    }

    public static String getTrainModelPath(String modelDir, Configuration config, String suffix) {
        return getTrainModelPath(modelDir, config, suffix, null);
    }

    public static String getTrainModelPath(String modelDir, Configuration config, String suffix, String modelChoice) {
        String modelName = modelChoice == null ? suffix : suffix + "_" + modelChoice;
        return FileUtils.joinPaths(modelDir, config.get("edu.cmu.cs.lti.model.name"),
                config.get("edu.cmu.cs.lti.model.type"), modelName);
    }
}
