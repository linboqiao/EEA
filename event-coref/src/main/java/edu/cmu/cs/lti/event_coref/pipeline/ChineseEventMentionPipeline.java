package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.collection_reader.EreCorpusReader;
import edu.cmu.cs.lti.emd.annotators.EventMentionTypeClassPrinter;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/16
 * Time: 5:23 PM
 *
 * @author Zhengzhong Liu
 */
public class ChineseEventMentionPipeline {
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

        CollectionReaderDescription trainReader = getEreReader(typeSystemDescription,
                kbpConfig.get("edu.cmu.cs.lti.training.source_text.dir"),
                kbpConfig.get("edu.cmu.cs.lti.training.ere.annotation"));

        AnalysisEngineDescription classPrinter = AnalysisEngineFactory.createEngineDescription(
                EventMentionTypeClassPrinter.class, typeSystemDescription,
                EventMentionTypeClassPrinter.CLASS_OUTPUT_PATH,
                edu.cmu.cs.lti.utils.FileUtils.joinPaths(
                        kbpConfig.get("edu.cmu.cs.lti.training.working.dir"), "mention_types.txt")
        );

        // Create the classes first.
        SimplePipeline.runPipeline(trainReader, classPrinter);

        // Now prepare the real pipeline.
        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, modelPath, modelOutputDir,
                trainingWorkingDir, testingWorkingDir);

        boolean skipTypeTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);
        boolean skipLv2TypeTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_type.lv2.skiptrain", false);
        boolean skipRealisTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain", false);
        boolean skipCorefTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.coref.skiptrain", false);


        boolean skipLv1Test = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptest", false);
        boolean skipLv2Test = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_type.lv2.skiptest", false);
        boolean skipRealisTest = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptest", false);
        boolean skipCorefTest = kbpConfig.getBoolean("edu.cmu.cs.lti.coref.skiptest", false);
        boolean skipJointTest = kbpConfig.getBoolean("edu.cmu.cs.lti.joint.skiptest", false);

        pipeline.prepare(kbpConfig, trainReader, null /*test data not exist now.*/);
//        pipeline.crossValidation(kbpConfig);
    }

    private static CollectionReaderDescription getEreReader(TypeSystemDescription typeSystemDescription, String
            sourceDir, String annotationDir) throws ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(EreCorpusReader.class, typeSystemDescription,
                EreCorpusReader.PARAM_ERE_ANNOTATION_DIR, annotationDir,
                EreCorpusReader.PARAM_SOURCE_TEXT_DIR, sourceDir,
                EreCorpusReader.PARAM_ERE_ANNOTATION_EXT, "rich_ere.xml",
                EreCorpusReader.PARAM_SOURCE_EXT, "mp.txt",
                EreCorpusReader.PARAM_LANGUAGE, "zh");
    }
}
