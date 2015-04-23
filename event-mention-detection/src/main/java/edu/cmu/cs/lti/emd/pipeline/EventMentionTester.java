package edu.cmu.cs.lti.emd.pipeline;

import edu.cmu.cs.lti.emd.annotators.EvaluationResultWriter;
import edu.cmu.cs.lti.emd.annotators.EventMentionTypeLearner;
import edu.cmu.cs.lti.emd.eval.EventMentionEvalRunner;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.pipeline.SimplePipeline;

import java.io.File;


public class EventMentionTester {
    private static String className = EventMentionTester.class.getSimpleName();

    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");

        String paramInputDir = "event-mention-detection/data/Event-mention-detection-2014";
        String paramTypeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";
        String semLinkDataPath = "data/resources/SemLink_1.2.2c";
        String brownClusteringDataPath = "data/resources/TDT5_BrownWC.txt";
        String wordnetDataPath = "data/resources/wnDict";

        String modelBase = args[0]; // models_train_split
        String modelName = args[1]; // "weka.classifiers.functions.SMO" "weka.classifiers.trees.RandomForest"

        if (args.length >= 4) {
            String realisModelBase = args[2];
            String realisModelName = args[3];
            String realisModelPath = new File(paramInputDir, realisModelBase).getCanonicalPath();
        }

        String modelPath = new File(paramInputDir, modelBase).getCanonicalPath();

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);


        CollectionReaderDescription dev_reader = CustomCollectionReaderFactory.createXmiReader(paramInputDir, "dev_data", 1, false);
//        CollectionReaderDescription test_reader = CustomCollectionReaderFactory.createXmiReader(paramInputDir, "test_data", 1, false);
//        CollectionReaderDescription final_reader = CustomCollectionReaderFactory.createXmiReader(paramInputDir + "/test", "submission_data", 1, false);

        AnalysisEngineDescription mention = AnalysisEngineFactory.createEngineDescription(
                EventMentionTypeLearner.class, typeSystemDescription,
                EventMentionTypeLearner.PARAM_SEM_LINK_DIR, semLinkDataPath,
                EventMentionTypeLearner.PARAM_IS_TRAINING, false,
                EventMentionTypeLearner.PARAM_MODEL_FOLDER, modelPath,
                EventMentionTypeLearner.PARAM_MODEL_NAME_FOR_TEST, modelName,
                EventMentionTypeLearner.PARAM_ONLINE_TEST, true,
                EventMentionTypeLearner.PARAM_TRAINING_DATASET_PATH, new File(paramInputDir, modelBase + "/training.arff").getCanonicalPath(),
                EventMentionTypeLearner.PARAM_BROWN_CLUSTERING_PATH, brownClusteringDataPath,
                EventMentionTypeLearner.PARAM_WORDNET_PATH, wordnetDataPath
        );

//        AnalysisEngineDescription realis = CustomAnalysisEngineFactory.createAnalysisEngine(
//                EventMentionRealisLearner.class, typeSystemDescription,
//                EventMentionRealisLearner.PARAM_SEM_LINK_DIR, semLinkDataPath,
//                EventMentionRealisLearner.PARAM_IS_TRAINING, false,
//                EventMentionRealisLearner.PARAM_MODEL_FOLDER, realisModelPath,
//                EventMentionRealisLearner.PARAM_MODEL_NAME_FOR_TEST, realisModelName,
//                EventMentionRealisLearner.PARAM_ONLINE_TEST, true,
//                EventMentionRealisLearner.PARAM_TRAINING_DATASET_PATH, new File(paramInputDir, realisModelBase + "/training.arff").getCanonicalPath(),
//                EventMentionRealisLearner.PARAM_BROWN_CLUSTERING_PATH, brownClusteringDataPath
//        );

        AnalysisEngineDescription devResults = AnalysisEngineFactory.createEngineDescription(EvaluationResultWriter.class, typeSystemDescription,
                EvaluationResultWriter.PARAM_OUTPUT_PATH, paramInputDir + "/results/temp_dev_prediction.tbf");
        SimplePipeline.runPipeline(dev_reader, mention, devResults);

//        AnalysisEngineDescription testResults = CustomAnalysisEngineFactory.createAnalysisEngine(EvaluationResultWriter.class, typeSystemDescription,
//                EvaluationResultWriter.PARAM_OUTPUT_PATH, paramInputDir + "/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/test_prediction.tbf");
//        SimplePipeline.runPipeline(test_reader, mention, testResults);

//        AnalysisEngineDescription finalResults = CustomAnalysisEngineFactory.createAnalysisEngine(EvaluationResultWriter.class, typeSystemDescription,
//                EvaluationResultWriter.PARAM_OUTPUT_PATH, paramInputDir + "/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/CMU-TWO-STEP.tbf");
//        SimplePipeline.runPipeline(final_reader, mention, realis, finalResults);

        EventMentionEvalRunner runner = new EventMentionEvalRunner();

        runner.runEval(
                "/Users/zhengzhongliu/Documents/projects/EvmEval/scorer_v1.2.py",
                "event-mention-detection/data/Event-mention-detection-2014/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/tbf/all_dev_gold.tbf",
                "event-mention-detection/data/Event-mention-detection-2014/results/temp_dev_prediction.tbf",
                "event-mention-detection/data/Event-mention-detection-2014/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/data/token_offset",
                "event-mention-detection/data/Event-mention-detection-2014/eval_out");

        System.out.println(modelBase + "\t" + runner.getMicroTypeAccuracy());


        System.err.println(className + " finished");
    }
}