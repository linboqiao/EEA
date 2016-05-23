package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.collection_reader.LDCXmlCollectionReader;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
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

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                LDCXmlCollectionReader.class, typeSystemDescription,
                LDCXmlCollectionReader.PARAM_DATA_PATH, inputPath
        );

        // Now prepare the real pipeline.
        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, kbpConfig, false);

        pipeline.prepare(kbpConfig, reader);
        pipeline.runVanilla(kbpConfig);
    }
}
