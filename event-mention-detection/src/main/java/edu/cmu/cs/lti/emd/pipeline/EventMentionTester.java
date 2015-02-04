package edu.cmu.cs.lti.emd.pipeline;

import edu.cmu.cs.lti.emd.annotators.EvaluationResultWriter;
import edu.cmu.cs.lti.emd.annotators.EventMentionCandidateFeatureGenerator;
import edu.cmu.cs.lti.emd.annotators.EventMentionRealisFeatureGenerator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;


public class EventMentionTester {
    private static String className = EventMentionTester.class.getSimpleName();

    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");

        String modelBase = args[0]; // models_train_split

        String modelName = args[1]; // "weka.classifiers.functions.SMO" "weka.classifiers.trees.RandomForest"

        String realisModelBase = args[2];

        String realisModelName = args[3];


        String paramInputDir = "event-mention-detection/data/Event-mention-detection-2014";
        String paramTypeSystemDescriptor = "TypeSystem";
        String semLinkDataPath = "data/resources/SemLink_1.2.2c";
        String brownClusteringDataPath = "data/resources/TDT5_BrownWC.txt";
        String wordnetDataPath = "data/resources/wnDict";


        String modelPath = new File(paramInputDir, modelBase).getCanonicalPath();

        String realisModelPath = new File(paramInputDir, realisModelBase).getCanonicalPath();

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription final_reader = CustomCollectionReaderFactory.createXmiReader(paramInputDir + "/test", "submission_data", 1, false);

//        CollectionReaderDescription dev_reader = CustomCollectionReaderFactory.createXmiReader(paramInputDir, "dev_data", 1, false);
//        CollectionReaderDescription test_reader = CustomCollectionReaderFactory.createXmiReader(paramInputDir, "test_data", 1, false);

        AnalysisEngineDescription ana = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionCandidateFeatureGenerator.class, typeSystemDescription,
                EventMentionCandidateFeatureGenerator.PARAM_SEM_LINK_DIR, semLinkDataPath,
                EventMentionCandidateFeatureGenerator.PARAM_IS_TRAINING, false,
                EventMentionCandidateFeatureGenerator.PARAM_MODEL_FOLDER, modelPath,
                EventMentionCandidateFeatureGenerator.PARAM_MODEL_NAME_FOR_TEST, modelName,
                EventMentionCandidateFeatureGenerator.PARAM_ONLINE_TEST, true,
                EventMentionCandidateFeatureGenerator.PARAM_TRAINING_DATASET_PATH, new File(paramInputDir, modelBase + "/training.arff").getCanonicalPath(),
                EventMentionCandidateFeatureGenerator.PARAM_BROWN_CLUSTERING_PATH, brownClusteringDataPath,
                EventMentionCandidateFeatureGenerator.PARAM_WORDNET_PATH, wordnetDataPath
        );


        AnalysisEngineDescription realis = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionRealisFeatureGenerator.class, typeSystemDescription,
                EventMentionRealisFeatureGenerator.PARAM_SEM_LINK_DIR, semLinkDataPath,
                EventMentionRealisFeatureGenerator.PARAM_IS_TRAINING, false,
                EventMentionRealisFeatureGenerator.PARAM_MODEL_FOLDER, realisModelPath,
                EventMentionRealisFeatureGenerator.PARAM_MODEL_NAME_FOR_TEST, realisModelName,
                EventMentionRealisFeatureGenerator.PARAM_ONLINE_TEST, true,
                EventMentionRealisFeatureGenerator.PARAM_TRAINING_DATASET_PATH, new File(paramInputDir, realisModelBase + "/training.arff").getCanonicalPath(),
                EventMentionRealisFeatureGenerator.PARAM_BROWN_CLUSTERING_PATH, brownClusteringDataPath
        );

//        AnalysisEngineDescription devResults = CustomAnalysisEngineFactory.createAnalysisEngine(EvaluationResultWriter.class, typeSystemDescription,
//                EvaluationResultWriter.PARAM_OUTPUT_PATH, paramInputDir + "/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/dev_prediction.tbf");
//
//        AnalysisEngineDescription testResults = CustomAnalysisEngineFactory.createAnalysisEngine(EvaluationResultWriter.class, typeSystemDescription,
//                EvaluationResultWriter.PARAM_OUTPUT_PATH, paramInputDir + "/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/test_prediction.tbf");

//        AnalysisEngineDescription devOutputPrediction = CustomAnalysisEngineFactory.createXmiWriter(paramInputDir, "dev_predicted", 2, null);
//
//        AnalysisEngineDescription testOutputPrediction = CustomAnalysisEngineFactory.createXmiWriter(paramInputDir, "test_predicted", 2, null);

//        SimplePipeline.runPipeline(dev_reader, ana, realis, devResults, devOutputPrediction);
//
//        SimplePipeline.runPipeline(test_reader, ana, realis, testResults, testOutputPrediction);

        AnalysisEngineDescription finalResults = CustomAnalysisEngineFactory.createAnalysisEngine(EvaluationResultWriter.class, typeSystemDescription,
                EvaluationResultWriter.PARAM_OUTPUT_PATH, paramInputDir + "/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/CMU-TWO-STEP.tbf");
        AnalysisEngineDescription finalOutputPrediction = CustomAnalysisEngineFactory.createXmiWriter(paramInputDir + "/test", "final_predicted", 2, null);
        SimplePipeline.runPipeline(final_reader, ana, realis, finalResults, finalOutputPrediction);

        System.err.println(className + " finished");
    }
}