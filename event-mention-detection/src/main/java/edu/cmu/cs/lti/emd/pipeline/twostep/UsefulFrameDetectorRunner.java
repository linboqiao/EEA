package edu.cmu.cs.lti.emd.pipeline.twostep;

import edu.cmu.cs.lti.emd.annotators.UsefulFramDetector;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
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
public class UsefulFrameDetectorRunner {
    static String className = UsefulFrameDetectorRunner.class.getSimpleName();

    public static String dataOutputDir = "event-mention-detection/data/Event-mention-detection-2014/useful_frames";

    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");

        // Parameters for the writer
        String paramParentInputDir = "event-mention-detection/data/Event-mention-detection-2014";
        String paramBaseInputDirName = "split_train";

        String paramTypeSystemDescriptor = "TypeSystem";

        String semLinkDataPath = "data/resources/SemLink_1.2.2c";

        String wordnetDataPath = "data/resources/wnDict";

        String frameDataPath = "data/resources/fndata-1.5/frame";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, new File(paramParentInputDir, paramBaseInputDirName).getCanonicalPath(), false);

        AnalysisEngineDescription detector = AnalysisEngineFactory.createEngineDescription(
                UsefulFramDetector.class, typeSystemDescription,
                UsefulFramDetector.PARAM_FRAME_DATA_PATH, frameDataPath,
                UsefulFramDetector.PARAM_GOLD_STANDARD_VIEW_NAME, "goldStandard",
                UsefulFramDetector.PARAM_OUTPUT_DIR, dataOutputDir,
                UsefulFramDetector.PARAM_SEM_LINK_PATH, semLinkDataPath,
                UsefulFramDetector.PARAM_WORDNET_PATH, wordnetDataPath);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, detector);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
