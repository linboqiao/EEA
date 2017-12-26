package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.salience.annotators.MultiFormatSalienceDataWriter;
import edu.cmu.cs.lti.script.annotators.FrameBasedEventDetector;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
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
public class SalienceDataPreparer {
    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String inputDir = argv[1];
        String jsonOutput = argv[2];
        String xmiOutput = argv[3];
        String trainingSplitFile = argv[4];
        String testSplitFile = argv[5];
        String devSplitFile = argv[6];
        String embeddingPath = argv[7];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, inputDir,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz",
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true
        );

        AnalysisEngineDescription detector = AnalysisEngineFactory.createEngineDescription(
                FrameBasedEventDetector.class, typeSystemDescription,
                FrameBasedEventDetector.PARAM_FRAME_RELATION, "../data/resources/fndata-1.7/frRelation.xml"
        );

        AnalysisEngineDescription jsonWriter = AnalysisEngineFactory.createEngineDescription(
                MultiFormatSalienceDataWriter.class, typeSystemDescription,
                MultiFormatSalienceDataWriter.PARAM_OUTPUT_DIR, new File(workingDir, jsonOutput),
                MultiFormatSalienceDataWriter.PARAM_TRAIN_SPLIT, trainingSplitFile,
                MultiFormatSalienceDataWriter.PARAM_TEST_SPLIT, testSplitFile,
                MultiFormatSalienceDataWriter.PARAM_DEV_SPLIT, devSplitFile,
                MultiFormatSalienceDataWriter.PARAM_OUTPUT_PREFIX, "nyt_salience",
                MultiFormatSalienceDataWriter.MULTI_THREAD, true,
                MultiFormatSalienceDataWriter.PARAM_JOINT_EMBEDDING, embeddingPath,
                MultiFormatSalienceDataWriter.PARAM_WRITE_EVENT, true
        );

//        StepBasedDirGzippedXmiWriter.dirSegFunction = IOUtils::indexBasedSegFunc;

//        new BasicPipeline(reader, true, true, 7, workingDir, xmiOutput, true, detector, jsonWriter).run();
        SimplePipeline.runPipeline(reader, detector, jsonWriter);
    }
}
