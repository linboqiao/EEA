package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.File;

/**
 * Run all the experiments of the KBP 2015 event track tasks, including train/test and 5-fold cross validation.
 *
 * @author Zhengzhong Liu
 */
public class KbpEnglishEventMentionPipeline {
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
            pipeline.trainTest(kbpConfig, false, kbpConfig.getBoolean("edu.cmu.cs.lti.test.has_gold", false));
        }

        if (kbpConfig.getBoolean("edu.cmu.cs.lti.development", false)) {
            pipeline.crossValidation(kbpConfig);
        }

//        for (int i = 1; i < 23; i++) {
//            pipeline.trainAll(kbpConfig, skipTypeTrain, skipLv2TypeTrain, skipRealisTrain, skipCorefTrain, i);
//            pipeline.test(kbpConfig, skipLv1Test, skipLv2Test, skipRealisTest, skipCorefTest, skipJointTest);
//            moveData(kbpConfig, modelOutputDir, testingWorkingDir, i);
//        }
    }

    private static CollectionReaderDescription getTbfReader(TypeSystemDescription typeSystemDescription, String
            goldStandardPath, String plainTextPath, String tokenMapPath, String language) throws
            ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(
                TbfEventDataReader.class, typeSystemDescription,
                TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, goldStandardPath,
                TbfEventDataReader.PARAM_SOURCE_EXT, ".txt",
                TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY, plainTextPath,
                TbfEventDataReader.PARAM_TOKEN_DIRECTORY, tokenMapPath,
                TbfEventDataReader.PARAM_TOKEN_EXT, ".tab",
                TbfEventDataReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName,
                TbfEventDataReader.PARAM_LANGUAGE, language
        );
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
