package edu.cmu.cs.lti.emd.pipeline.twostep;

import edu.cmu.cs.lti.annotator.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

/**
 * This pipeline runs FanseAnnotator.
 */
public class EventMentionDataPreprocessor {
    private static String className = EventMentionDataPreprocessor.class.getSimpleName();

    public static void main(String[] args) throws UIMAException {
        System.out.println(className + " started...");
        String paramInputDir = args[0];
//        "event-mention-detection/data/Event-mention-detection-2014" +
//                "/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/data/";

        String paramParentOutputDir = args[1]; //"event-mention-detection/data/Event-mention-detection-2014"
        String paramBaseOutputDirName = "semafor_processed";

        String goldStandardFilePath = args.length > 2 ? paramInputDir + args[2] : null; // "converted.tbf"

        String sourceDataPath = paramInputDir + "source";
        String tokenDataPath = paramInputDir + "token_offset";

        String paramTypeSystemDescriptor = "TypeSystem";

        String semaforModelDirectory = "../models/semafor_malt_model_20121129";
        String fanseModelDirectory = "../models/fanse_models";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TbfEventDataReader.class, typeSystemDescription,
                TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, goldStandardFilePath,
                TbfEventDataReader.PARAM_SOURCE_EXT, ".tkn.txt",
                TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY, sourceDataPath,
                TbfEventDataReader.PARAM_TOKEN_DIRECTORY, tokenDataPath,
                TbfEventDataReader.PARAM_TOKEN_EXT, ".txt.tab"
        );

        AnalysisEngineDescription stanfordAnalyzer = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_USE_SUTIME, true);

        AnalysisEngineDescription semaforAnalyzer = AnalysisEngineFactory.createEngineDescription(
                SemaforAnnotator.class, typeSystemDescription,
                SemaforAnnotator.SEMAFOR_MODEL_PATH, semaforModelDirectory);

        AnalysisEngineDescription fanseParser = AnalysisEngineFactory.createEngineDescription(
                FanseAnnotator.class, typeSystemDescription, FanseAnnotator.PARAM_MODEL_BASE_DIR,
                fanseModelDirectory);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, 0,
                null);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, stanfordAnalyzer, fanseParser, semaforAnalyzer, writer);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println(className + " successfully completed.");
    }

}
