package edu.cmu.cs.lti.io;

import edu.cmu.cs.lti.collection_reader.EreCorpusReader;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/3/16
 * Time: 9:18 AM
 *
 * @author Zhengzhong Liu
 */
public class EventDataReader {
    public static CollectionReaderDescription getReader(Configuration datasetConfig,
                                                        TypeSystemDescription typeSystemDescription)
            throws ResourceInitializationException {
        String datasetFormat = datasetConfig.get("edu.cmu.cs.lti.data.format").toLowerCase();
        switch (datasetFormat) {
            case "ere":
                return getEreReader(datasetConfig, typeSystemDescription);
            case "tbf":
                return getTbfReader(datasetConfig, typeSystemDescription);
            default:
                throw new IllegalArgumentException(String.format("Unknown dataset format %s.", datasetFormat));
        }
    }

    private static CollectionReaderDescription getEreReader(Configuration datasetConfig,
                                                            TypeSystemDescription typeSystemDescription)
            throws ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(EreCorpusReader.class, typeSystemDescription,
                EreCorpusReader.PARAM_ERE_ANNOTATION_DIR, datasetConfig.get("edu.cmu.cs.lti.data.annotation.path"),
                EreCorpusReader.PARAM_SOURCE_TEXT_DIR, datasetConfig.get("edu.cmu.cs.lti.data.source.path"),
                EreCorpusReader.PARAM_ERE_ANNOTATION_EXT, datasetConfig.get("edu.cmu.cs.lti.data.annotation.extension"),
                EreCorpusReader.PARAM_SOURCE_EXT, datasetConfig.get("edu.cmu.cs.lti.data.source.extension"),
                EreCorpusReader.PARAM_LANGUAGE, "zh");
    }

    private static CollectionReaderDescription getTbfReader(Configuration datasetConfig,
                                                            TypeSystemDescription typeSystemDescription)
            throws ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(
                TbfEventDataReader.class, typeSystemDescription,
                TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, datasetConfig.get("edu.cmu.cs.lti.data.gold.tbf"),
                TbfEventDataReader.PARAM_SOURCE_EXT, datasetConfig.get("edu.cmu.cs.lti.data.source.extension"),
                TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY, datasetConfig.get("edu.cmu.cs.lti.data.source.path"),
                TbfEventDataReader.PARAM_TOKEN_DIRECTORY, datasetConfig.get("edu.cmu.cs.lti.test.token_map.dir"),
                TbfEventDataReader.PARAM_TOKEN_EXT, datasetConfig.get("edu.cmu.cs.lti.data.token.extension"),
                TbfEventDataReader.PARAM_LANGUAGE, datasetConfig.get("edu.cmu.cs.lti.data.language"),
                TbfEventDataReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
        );
    }
}
