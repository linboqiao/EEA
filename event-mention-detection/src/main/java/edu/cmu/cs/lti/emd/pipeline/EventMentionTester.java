package edu.cmu.cs.lti.emd.pipeline;

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

        String paramInputDir = "event-mention-detection/data/Event-mention-detection-2014";
        String testBaseDir = "test_data";
        String paramTypeSystemDescriptor = "TypeSystem";
        String semLinkDataPath = "data/resources/SemLink_1.2.2c";

        String modelPath = new File(paramInputDir, "models").getCanonicalPath();

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(paramInputDir, testBaseDir, 1, false);
        AnalysisEngineDescription ana = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionCandidateFeatureGenerator.class, typeSystemDescription,
                EventMentionCandidateFeatureGenerator.PARAM_SEM_LINK_DIR, semLinkDataPath,
                EventMentionCandidateFeatureGenerator.PARAM_IS_TRAINING, false,
                EventMentionCandidateFeatureGenerator.PARAM_MODEL_FOLDER, modelPath
        );
        SimplePipeline.runPipeline(reader, ana);
    }
}