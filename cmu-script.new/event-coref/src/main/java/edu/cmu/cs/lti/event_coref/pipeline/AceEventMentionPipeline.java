package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.utils.Configuration;

/**
 * Run all the experiments of the KBP 2015 event track tasks, including train/test and 5-fold cross validation.
 *
 * @author Zhengzhong Liu
 */
public class AceEventMentionPipeline {
    public static void main(String argv[]) throws Exception {
        if (argv.length < 1) {
            System.err.println("Please provide one argument for the settings file.");
        }

        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        Configuration taskConfig = new Configuration(argv[0]);

        // Now prepare the real pipeline.
        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, taskConfig);

        pipeline.prepareData(taskConfig);

        if (taskConfig.getBoolean("edu.cmu.cs.lti.development", false)) {
            pipeline.trainAndDev(taskConfig, false);
        }

        if (taskConfig.getBoolean("edu.cmu.cs.lti.test", false)) {
            pipeline.trainAndTest(taskConfig, false);
        }
    }
}
