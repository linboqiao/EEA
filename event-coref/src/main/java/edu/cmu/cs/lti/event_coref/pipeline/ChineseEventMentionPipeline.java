package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.utils.Configuration;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/16
 * Time: 5:23 PM
 *
 * @author Zhengzhong Liu
 */
public class ChineseEventMentionPipeline {
    public static void main(String argv[]) throws Exception {
        if (argv.length < 1) {
            System.err.println("Please provide one argument for the settings file.");
        }

        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        Configuration kbpConfig = new Configuration(argv[0]);

        // Now prepare the real pipeline.
        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, kbpConfig);

        pipeline.prepare(kbpConfig);

//        pipeline.tryAnnotator(kbpConfig);

//        pipeline.computeStats();

        if (kbpConfig.getBoolean("edu.cmu.cs.lti.development", false)) {
            pipeline.crossValidation(kbpConfig);
        }

        if (kbpConfig.getBoolean("edu.cmu.cs.lti.test", false)) {
            pipeline.trainTest(kbpConfig, false);
        }
    }
}
