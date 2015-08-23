package edu.cmu.cs.lti.emd.pipeline;

import edu.cmu.cs.lti.emd.annotators.twostep.CandidateEventMentionDetector;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/30/15
 * Time: 7:07 PM
 */
public class CandidateEventMentionDetectorRunner {
    static String className = CandidateEventMentionDetectorRunner.class.getSimpleName();

    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");

        String paramBaseInputDirName = args[0];// "split_train";
        String outputBase = args[1]; //"test_data";

        // Parameters for the writer
        String paramParentInputDir = "event-mention-detection/data/Event-mention-detection-2014";

        String paramTypeSystemDescriptor = "TypeSystem";

        String semLinkDataPath = "data/resources/SemLink_1.2.2c";

        String wordnetDataPath = "data/resources/wnDict";

        String frameDataPath = "data/resources/fndata-1.5/frame";


        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, new File(paramParentInputDir, paramBaseInputDirName).getCanonicalPath(), false);

        AnalysisEngineDescription detector = AnalysisEngineFactory.createEngineDescription(
                CandidateEventMentionDetector.class, typeSystemDescription,
                CandidateEventMentionDetector.PARAM_FRAME_DATA_PATH, frameDataPath,
                CandidateEventMentionDetector.PARAM_GOLD_STANDARD_VIEW_NAME, "goldStandard",
                CandidateEventMentionDetector.PARAM_USEFUL_FRAME_DIR, UsefulFrameDetectorRunner.dataOutputDir,
                CandidateEventMentionDetector.PARAM_SEM_LINK_PATH, semLinkDataPath,
                CandidateEventMentionDetector.PARAM_WORDNET_PATH, wordnetDataPath,
                CandidateEventMentionDetector.PARAM_FOR_TRAINING, true
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(paramParentInputDir, outputBase, 1, null);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, detector, writer);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Done");
    }
}