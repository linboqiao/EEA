package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.collection_reader.EreCorpusReader;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

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

        // Now prepare the real pipeline.
        EventMentionPipeline pipeline = new EventMentionPipeline(typeSystemName, kbpConfig, true);

        pipeline.prepare(kbpConfig);

//        pipeline.tryAnnotator(kbpConfig);

        if (kbpConfig.getBoolean("edu.cmu.cs.lti.development", false)) {
            pipeline.crossValidation(kbpConfig);
        }

        if (kbpConfig.getBoolean("edu.cmu.cs.lti.test", false)) {
            pipeline.trainTest(kbpConfig, true);
        }
    }

    private static CollectionReaderDescription getEreReader(TypeSystemDescription typeSystemDescription, String
            sourceDir, String annotationDir, String ereExt, String sourceExt) throws
            ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(EreCorpusReader.class, typeSystemDescription,
                EreCorpusReader.PARAM_ERE_ANNOTATION_DIR, annotationDir,
                EreCorpusReader.PARAM_SOURCE_TEXT_DIR, sourceDir,
                EreCorpusReader.PARAM_ERE_ANNOTATION_EXT, ereExt,
                EreCorpusReader.PARAM_SOURCE_EXT, sourceExt,
                EreCorpusReader.PARAM_LANGUAGE, "zh");
    }
}
