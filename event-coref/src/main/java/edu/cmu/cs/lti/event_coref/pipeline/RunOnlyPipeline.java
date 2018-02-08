package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.collection_reader.LDCXmlCollectionReader;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/20/16
 * Time: 6:23 PM
 *
 * @author Zhengzhong Liu
 */
public class RunOnlyPipeline {

    public static void main(String argv[]) throws Exception {
        if (argv.length < 2) {
            System.err.println("First argument is the settings; Second argument is the input data directory.");
        }

        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);

        Configuration kbpConfig = new Configuration(argv[0]);

        String inputPath = argv[1];
        //"data/mention/LDC/LDC2015E77_TAC_KBP_2015_English_Cold_Start_Evaluation_Source_Corpus_V2.0/data/";

        String outputPath = argv[2];
        //"../data/project_data/cmu-script/mention/kbp/chinese/Chinese_Coled_Start_LDC2016E63"

//        CollectionReaderDescription reader = ldcReader(typeSystemDescription, inputPath, kbpConfig);
        CollectionReaderDescription reader = textReader(typeSystemDescription, inputPath, kbpConfig);

        // Now prepare the real pipeline.
        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, kbpConfig);

        boolean skipTestPrepare = kbpConfig.getBoolean("edu.cmu.cs.lti.test.skip.preprocess", false);
        pipeline.prepareData(kbpConfig, outputPath, skipTestPrepare, reader);

        pipeline.runVanilla(kbpConfig, outputPath);
    }

    private static CollectionReaderDescription ldcReader(TypeSystemDescription typeSystemDescription, String inputPath,
                                                         Configuration kbpConfig) throws ResourceInitializationException {
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
                                                          Configuration kbpConfig) throws ResourceInitializationException {
        return    CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class, typeSystemDescription,
                PlainTextCollectionReader.PARAM_INPUTDIR, inputPath,
                PlainTextCollectionReader.PARAM_LANGUAGE, kbpConfig.get("edu.cmu.cs.lti.language"),
                PlainTextCollectionReader.PARAM_DO_NOISE_FILTER, true
        );
    }
}
