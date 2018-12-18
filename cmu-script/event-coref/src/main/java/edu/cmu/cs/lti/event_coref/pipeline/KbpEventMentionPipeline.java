package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.utils.Configuration;

import java.io.File;

/**
 * Run all the experiments of the KBP 2015 event track tasks, including train/test and 5-fold cross validation.
 *
 * @author Zhengzhong Liu
 */
public class KbpEventMentionPipeline {
    public static void main(String argv[]) throws Exception {
        if (argv.length < 1) {
            System.err.println("Please provide one argument for the settings file.");
        }

        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        Configuration kbpConfig = new Configuration(argv[0]);

        // Now prepare the real pipeline.
        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, kbpConfig);

        pipeline.prepareData(kbpConfig);

        if (kbpConfig.getBoolean("edu.cmu.cs.lti.test", false)) {
            pipeline.trainAndTest(kbpConfig, false);
        }

        if (kbpConfig.getBoolean("edu.cmu.cs.lti.development", false)) {
            pipeline.crossValidation(kbpConfig);
        }
    }

    public static void moveData(Configuration config, String outputModelDir, String testingWorkingDir, int suffix) {
        File corefModelDir = new File(outputModelDir, config.get("edu.cmu.cs.lti.model.event.latent_tree"));
        File evalDir = new File(new File(testingWorkingDir, "eval"), "full_run");

        rename(corefModelDir, suffix);
        rename(evalDir, suffix);
    }

    public static void rename(File d, int suffix) {
        String newPath = d.getPath() + "_seed_" + suffix;
        System.out.println("Backup results:");
        System.out.println(d.getPath());
        System.out.println("->");
        System.out.println(newPath);

        d.renameTo(new File(newPath));
    }
}
