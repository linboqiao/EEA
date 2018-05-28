package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run regression on a toy dataset, ensure must component works as expected.
 *
 * @author Zhengzhong Liu
 */
public class RegressionPipeline {
    private static final Logger logger = LoggerFactory.getLogger(RegressionPipeline.class);

    public static void main(String argv[]) throws Exception {
        if (argv.length < 1) {
            System.err.println("Please provide one argument for the settings file.");
        }

        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        Configuration config = new Configuration(argv[0]);
        String regressionDir = config.get("edu.cmu.cs.lti.regression.dir");
        boolean runReference = config.getBoolean("edu.cmu.cs.lti.regression.reference_mode", false);
        String modelPath = config.get("edu.cmu.cs.lti.model.dir");

        String base = runReference ? "reference_run" : "regression_run";

        logger.info(String.format("Regression directory is [%s], reference base is [%s].", regressionDir, base));

        String trainingWorkingDir = FileUtils.joinPaths(regressionDir, base, "train");
        String testWorkingDir = FileUtils.joinPaths(regressionDir, base, "test");
        String modelOutputDir = FileUtils.joinPaths(config.get("edu.cmu.cs.lti.regression.model.output.dir"), base);

        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, config);

//        pipeline.regression(config);
    }
}
