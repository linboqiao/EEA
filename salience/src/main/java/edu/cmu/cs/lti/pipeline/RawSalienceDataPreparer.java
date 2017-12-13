package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.collection_reader.AnnotatedNytReader;
import edu.cmu.cs.lti.salience.annotators.MultiFormatSalienceDataWriter;
import edu.cmu.cs.lti.salience.annotators.TagmeStyleJSONReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
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
public class RawSalienceDataPreparer {
    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String inputJson = argv[1];
        String jsonOutput = argv[2];
        String xmiOutput = argv[3];
        String embeddingPath = argv[4];
        String trainingSplitFile = argv[5];
        String testSplitFile = argv[6];
        String devSplitFile = argv[7];

        // This reader can read Semantic scholar data.
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TagmeStyleJSONReader.class, typeSystemDescription,
                TagmeStyleJSONReader.PARAM_INPUT_JSON, inputJson,
                TagmeStyleJSONReader.PARAM_ABSTRACT_FIELD, "title",
                TagmeStyleJSONReader.PARAM_BODY_FIELDS, new String[]{"paperAbstract"}
        );

        AnalysisEngineDescription stanfordAnalyzer = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_LANGUAGE, "en",
                StanfordCoreNlpAnnotator.PARAM_KEEP_QUIET, true,
                StanfordCoreNlpAnnotator.PARAM_WHITESPACE_TOKENIZE, true,
                StanfordCoreNlpAnnotator.PARAM_ADDITIONAL_VIEWS, AnnotatedNytReader.ABSTRACT_VIEW_NAME,
                StanfordCoreNlpAnnotator.MULTI_THREAD, true,
                StanfordCoreNlpAnnotator.PARAM_PARSER_MAXLEN, 40
        );

        AnalysisEngineDescription parsedWriter = CustomAnalysisEngineFactory.createGzippedXmiWriter(
                workingDir, "parsed");

        AnalysisEngineDescription jsonWriter = AnalysisEngineFactory.createEngineDescription(
                MultiFormatSalienceDataWriter.class, typeSystemDescription,
                MultiFormatSalienceDataWriter.PARAM_OUTPUT_DIR, new File(workingDir, jsonOutput),
                MultiFormatSalienceDataWriter.PARAM_TRAIN_SPLIT, trainingSplitFile,
                MultiFormatSalienceDataWriter.PARAM_TEST_SPLIT, testSplitFile,
                MultiFormatSalienceDataWriter.PARAM_DEV_SPLIT, devSplitFile,
                MultiFormatSalienceDataWriter.PARAM_OUTPUT_PREFIX, "salience",
                MultiFormatSalienceDataWriter.MULTI_THREAD, true,
                MultiFormatSalienceDataWriter.PARAM_JOINT_EMBEDDING, embeddingPath,
                MultiFormatSalienceDataWriter.PARAM_WRITE_EVENT, true
        );

//        new BasicPipeline(reader, true, true, 7, workingDir, xmiOutput, true, stanfordAnalyzer, parsedWriter,
//                jsonWriter).run();

        SimplePipeline.runPipeline(reader, stanfordAnalyzer, parsedWriter, jsonWriter);
    }
}
