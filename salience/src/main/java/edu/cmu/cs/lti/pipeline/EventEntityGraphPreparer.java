package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.salience.annotators.EventEntityGraphWriter;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/14/17
 * Time: 10:04 AM
 *
 * @author Zhengzhong Liu
 */
public class EventEntityGraphPreparer {
    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String inputDir = argv[1];
        String output = argv[2];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, inputDir,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz",
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true
        );

        AnalysisEngineDescription jsonWriter = AnalysisEngineFactory.createEngineDescription(
                EventEntityGraphWriter.class, typeSystemDescription,
                EventEntityGraphWriter.PARAM_OUTPUT_DIR, new File(workingDir, output)
        );

        new BasicPipeline(reader, true, true, 7, jsonWriter).run();
    }
}
