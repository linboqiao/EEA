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
        if (argv.length < 3) {
            System.err.println("First argument is the settings; Second argument is the input data directory; Third is" +
                    " the main output directory.");
        }

        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);

        Configuration kbpConfig = new Configuration(argv[0]);

        String inputPath = argv[1];
        //"data/mention/LDC/LDC2015E77_TAC_KBP_2015_English_Cold_Start_Evaluation_Source_Corpus_V2.0/data/";

        String workingDir = argv[2];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                LDCXmlCollectionReader.class, typeSystemDescription,
                LDCXmlCollectionReader.PARAM_DATA_PATH, inputPath
        );

//        String testingWorkingDir = kbpConfig.get("edu.cmu.cs.lti.test.working.dir");
        String modelOutputDir = kbpConfig.get("edu.cmu.cs.lti.model.output.dir");
        String modelPath = kbpConfig.get("edu.cmu.cs.lti.model.dir");

        // Now prepare the real pipeline.
        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, modelPath, modelOutputDir,
                null, workingDir);

        boolean skipLv1Test = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptest", false);
        boolean skipLv2Test = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_type.lv2.skiptest", false);
        boolean skipRealisTest = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptest", false);
        boolean skipCorefTest = kbpConfig.getBoolean("edu.cmu.cs.lti.coref.skiptest", false);
        boolean skipJointTest = kbpConfig.getBoolean("edu.cmu.cs.lti.joint.skiptest", false);

        pipeline.prepare(kbpConfig, reader);
        pipeline.test(kbpConfig, skipLv1Test, skipLv2Test, skipRealisTest, skipCorefTest, skipJointTest);
    }
}
