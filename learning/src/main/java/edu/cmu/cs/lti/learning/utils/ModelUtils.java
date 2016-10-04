package edu.cmu.cs.lti.learning.utils;

import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/3/16
 * Time: 10:28 PM
 *
 * @author Zhengzhong Liu
 */
public class ModelUtils {
    public static File getModelPath(String base, Configuration config) {
        return FileUtils.joinPathsAsFile(base,
                config.get("edu.cmu.cs.lti.model.experiment.name"), config.get("edu.cmu.cs.lti.model.type"),
                config.get("edu.cmu.cs.lti.model.test.choice"));
    }
}
