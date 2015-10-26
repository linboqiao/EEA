package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.utils.Configuration;

/**
 * Run all the experiments of the KBP 2015 event track tasks, including train/test and 5-fold cross validation.
 *
 * @author Zhengzhong Liu
 */
public class KBP2015EventTaskPipeline {
    public static void main(String argv[]) throws Exception {
        if (argv.length < 1) {
            System.err.println("Please provide one argument for the settings file.");
        }

        Configuration commonConfig = new Configuration("settings/common.properties");
        String modelPath = commonConfig.get("edu.cmu.cs.lti.model.dir");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        Configuration kbpConfig = new Configuration(argv[0]);
        String trainingWorkingDir = kbpConfig.get("edu.cmu.cs.lti.training.working.dir");
        String testingWorkingDir = kbpConfig.get("edu.cmu.cs.lti.test.working.dir");
        String modelOutputDir = kbpConfig.get("edu.cmu.cs.lti.model.output.dir");

        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, modelPath, modelOutputDir,
                trainingWorkingDir, testingWorkingDir);

        boolean skipTypeTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);
        boolean skipRealisTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain=true", false);
        boolean skipCorefTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.coref.skiptrain", false);

        pipeline.prepare(kbpConfig);
        pipeline.trainAll(kbpConfig, skipTypeTrain, skipRealisTrain, skipCorefTrain);
        pipeline.test(kbpConfig);
        pipeline.crossValidation(kbpConfig);
    }
}
