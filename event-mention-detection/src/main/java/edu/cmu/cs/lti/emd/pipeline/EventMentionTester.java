package edu.cmu.cs.lti.emd.pipeline;

import edu.cmu.cs.lti.emd.annotators.EvaluationResultWriter;
import edu.cmu.cs.lti.emd.annotators.EventMentionCandidateFeatureGenerator;
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

        String modelBase = args[0]; // train_split

        String testBaseDir = args[1]; // "dev_data"

        String modelName = args[2]; // "weka.classifiers.functions.SMO"

        String paramInputDir = "event-mention-detection/data/Event-mention-detection-2014";
        String paramTypeSystemDescriptor = "TypeSystem";
        String semLinkDataPath = "data/resources/SemLink_1.2.2c";
        String brownClusteringDataPath = "data/resources/TDT5_BrownWC.txt";

        String resultXmiOutput = "processed";

        String modelPath = new File(paramInputDir, modelBase).getCanonicalPath();

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(paramInputDir, testBaseDir, 1, false);
        AnalysisEngineDescription ana = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionCandidateFeatureGenerator.class, typeSystemDescription,
                EventMentionCandidateFeatureGenerator.PARAM_SEM_LINK_DIR, semLinkDataPath,
                EventMentionCandidateFeatureGenerator.PARAM_IS_TRAINING, false,
                EventMentionCandidateFeatureGenerator.PARAM_MODEL_FOLDER, modelPath,
                EventMentionCandidateFeatureGenerator.PARAM_MODEL_NAME_FOR_TEST, modelName,
                EventMentionCandidateFeatureGenerator.PARAM_ONLINE_TEST, true,
                EventMentionCandidateFeatureGenerator.PARAM_TRAINING_DATASET_PATH, new File(paramInputDir, modelBase + "/training.arff").getCanonicalPath(),
                EventMentionCandidateFeatureGenerator.PARAM_BROWN_CLUSTERING_PATH, brownClusteringDataPath
        );

        AnalysisEngineDescription results = CustomAnalysisEngineFactory.createAnalysisEngine(EvaluationResultWriter.class, typeSystemDescription,
                EvaluationResultWriter.PARAM_OUTPUT_PATH, "dev_prediction.tbf");

        AnalysisEngineDescription outputPrediction = CustomAnalysisEngineFactory.createXmiWriter(paramInputDir, resultXmiOutput, 2, null);

        SimplePipeline.runPipeline(reader, ana, results, outputPrediction);
        System.err.println(className + " finished");

    }
}