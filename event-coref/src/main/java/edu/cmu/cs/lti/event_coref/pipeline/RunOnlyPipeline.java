package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.collection_reader.LDCXmlCollectionReader;
import edu.cmu.cs.lti.io.JsonRichEventWriter;
import edu.cmu.cs.lti.script.annotators.FrameBasedEventDetector;
import edu.cmu.cs.lti.script.annotators.VerbBasedEventDetector;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.DispatchReader;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/20/16
 * Time: 6:23 PM
 *
 * @author Zhengzhong Liu
 */
public class RunOnlyPipeline {
    private static final Logger logger = LoggerFactory.getLogger(RunOnlyPipeline.class);

    public static void main(String argv[]) throws Exception {
        if (argv.length < 3) {
            System.err.println("Args: [setting] [input] [output] [run name] ([simple event] [format])");
        }

        String inputPath = argv[1];
        //"data/mention/LDC/LDC2015E77_TAC_KBP_2015_English_Cold_Start_Evaluation_Source_Corpus_V2.0/data/";

        String outputPath = argv[2];
        //"../data/project_data/cmu-script/mention/kbp/chinese/Chinese_Coled_Start_LDC2016E63"

        String runName = argv[3];

        boolean simpleEvent = false;
        if (argv.length >= 5) {
            if (argv[4].equals("simple")) {
                simpleEvent = true;
                System.out.println("Using simple event extractor.");
            }
        }

        String readerType = "txt";
        if (argv.length >= 6) {
            readerType = argv[5];
        }

        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);

        Configuration kbpConfig = new Configuration(argv[0]);
        kbpConfig.set("edu.cmu.cs.lti.experiment.name", runName);

        logger.info("Reader type is: " + readerType);

        CollectionReaderDescription reader = DispatchReader.getReader(typeSystemDescription, inputPath, readerType,
                kbpConfig);

        // Now prepare the real pipeline.
        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, kbpConfig);

        boolean skipTestPrepare = kbpConfig.getBoolean("edu.cmu.cs.lti.test.skip.preprocess", false);
        pipeline.prepareData(kbpConfig, outputPath, skipTestPrepare, reader);

        CollectionReaderDescription results;
        if (simpleEvent) {
            AnalysisEngineDescription verbEvents = AnalysisEngineFactory.createEngineDescription(
                    VerbBasedEventDetector.class, typeSystemDescription
            );

            AnalysisEngineDescription frameEvents = AnalysisEngineFactory.createEngineDescription(
                    FrameBasedEventDetector.class, typeSystemDescription,
                    FrameBasedEventDetector.PARAM_FRAME_RELATION, "../data/resources/fndata-1.7/frRelation.xml",
                    FrameBasedEventDetector.PARAM_IGNORE_BARE_FRAME, true
            );

            AnalysisEngineDescription[] engines = new AnalysisEngineDescription[]{
                    verbEvents, frameEvents
            };

            results = pipeline.runOnMentions(kbpConfig, outputPath, engines, "SimpleEvents");
        } else {
            results = pipeline.runVanilla(kbpConfig, outputPath);
        }

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                JsonRichEventWriter.class, typeSystemDescription,
                JsonRichEventWriter.PARAM_OUTPUT_DIR, FileUtils.joinPaths(outputPath, "rich", runName)
        );

        SimplePipeline.runPipeline(results, writer);
    }

    private static CollectionReaderDescription ldcReader(TypeSystemDescription typeSystemDescription, String inputPath,
                                                         Configuration kbpConfig) throws
            ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(
                LDCXmlCollectionReader.class, typeSystemDescription,
                LDCXmlCollectionReader.PARAM_DATA_PATH, inputPath,
                LDCXmlCollectionReader.PARAM_BASE_NAME_FILE_FILTER,
                kbpConfig.get("edu.cmu.cs.lti.file.basename.filter"),
                LDCXmlCollectionReader.PARAM_BASE_NAME_IGNORES,
                kbpConfig.get("edu.cmu.cs.lti.file.basename.ignores.preprocess"),
                LDCXmlCollectionReader.PARAM_LANGUAGE,
                kbpConfig.get("edu.cmu.cs.lti.language"),
                LDCXmlCollectionReader.PARAM_RECURSIVE, true
        );
    }

    private static CollectionReaderDescription textReader(TypeSystemDescription typeSystemDescription, String inputPath,
                                                          Configuration kbpConfig) throws
            ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class, typeSystemDescription,
                PlainTextCollectionReader.PARAM_INPUTDIR, inputPath,
                PlainTextCollectionReader.PARAM_LANGUAGE, kbpConfig.get("edu.cmu.cs.lti.language"),
                PlainTextCollectionReader.PARAM_DO_NOISE_FILTER, true
        );
    }
}
