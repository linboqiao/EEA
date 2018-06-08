package edu.cmu.cs.lti.utils;

import edu.cmu.cs.lti.collection_reader.LDCXmlCollectionReader;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/21/18
 * Time: 5:42 PM
 *
 * @author Zhengzhong Liu
 */
public class DispatchReader {
    public static CollectionReaderDescription getReader(TypeSystemDescription typeSystemDescription,
                                                        String inputPath, Configuration config) throws
            ResourceInitializationException {
        return getReader(typeSystemDescription, inputPath, null, config);
    }

    public static CollectionReaderDescription getReader(TypeSystemDescription typeSystemDescription,
                                                        String inputPath, String type, Configuration config) throws
            ResourceInitializationException {

        if (type == null) {
            return textReader(typeSystemDescription, inputPath, config);
        }

        type = type.toLowerCase();

        switch (type) {
            case "xmi":
                return xmiReader(typeSystemDescription, inputPath);
            case "ldc":
                return ldcReader(typeSystemDescription, inputPath, config);
            case "txt":
            default:
                return textReader(typeSystemDescription, inputPath, config);
        }

    }

    private static CollectionReaderDescription xmiReader(TypeSystemDescription typeSystemDescription, String
            inputPath) throws ResourceInitializationException {
        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, inputPath);
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
                PlainTextCollectionReader.PARAM_DO_NOISE_FILTER, true,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, kbpConfig.get("edu.cmu.cs.lti.input_suffix")
        );
    }
}
