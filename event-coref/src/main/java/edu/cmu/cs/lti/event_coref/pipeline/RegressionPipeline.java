package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.utils.Configuration;

/**
 * Run regression on a toy dataset, ensure must component works as expected.
 *
 * @author Zhengzhong Liu
 */
public class RegressionPipeline {
    public static void main(String argv[]) throws Exception {
        if (argv.length < 1) {
            System.err.println("Please provide one argument for the settings file.");
        }

        Configuration commonConfig = new Configuration("settings/common.properties");
        String modelPath = commonConfig.get("edu.cmu.cs.lti.model.dir");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        Configuration config = new Configuration(argv[0]);
        String trainingWorkingDir = config.get("edu.cmu.cs.lti.regression.working.dir");
        String modelOutputDir = config.get("edu.cmu.cs.lti.regression.model.output.dir");

        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName,
                modelPath, modelOutputDir, trainingWorkingDir);

        pipeline.regression(config);
    }
}
