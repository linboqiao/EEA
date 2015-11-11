package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.EventMentionTypeClassPrinter;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

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
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        Configuration kbpConfig = new Configuration(argv[0]);
        String trainingWorkingDir = kbpConfig.get("edu.cmu.cs.lti.training.working.dir");
        String testingWorkingDir = kbpConfig.get("edu.cmu.cs.lti.test.working.dir");
        String modelOutputDir = kbpConfig.get("edu.cmu.cs.lti.model.output.dir");
        String modelPath = kbpConfig.get("edu.cmu.cs.lti.model.dir");

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TbfEventDataReader.class, typeSystemDescription,
                TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, kbpConfig.get("edu.cmu.cs.lti.training.gold.tbf"),
                TbfEventDataReader.PARAM_SOURCE_EXT, ".txt",
                TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY,
                kbpConfig.get("edu.cmu.cs.lti.training.source_text.dir"),
                TbfEventDataReader.PARAM_TOKEN_DIRECTORY, kbpConfig.get("edu.cmu.cs.lti.training.token_map.dir"),
                TbfEventDataReader.PARAM_TOKEN_EXT, ".tab",
                TbfEventDataReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
        );

        AnalysisEngineDescription classPrinter = AnalysisEngineFactory.createEngineDescription(
                EventMentionTypeClassPrinter.class, typeSystemDescription,
                EventMentionTypeClassPrinter.CLASS_OUTPUT_PATH,
                edu.cmu.cs.lti.utils.FileUtils.joinPaths(
                        kbpConfig.get("edu.cmu.cs.lti.training.working.dir"), "mention_types.txt")
        );

        // Create the classes first.
        SimplePipeline.runPipeline(reader, classPrinter);

        // Now prepare the real pipeline.
        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, modelPath, modelOutputDir,
                trainingWorkingDir, testingWorkingDir);

        boolean skipTypeTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);
        boolean skipRealisTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain=true", false);
        boolean skipCorefTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.coref.skiptrain", false);

        pipeline.prepare(kbpConfig);
//        pipeline.trainAll(kbpConfig, skipTypeTrain, skipRealisTrain, skipCorefTrain);
//        pipeline.test(kbpConfig);
        pipeline.crossValidation(kbpConfig);
    }
}
