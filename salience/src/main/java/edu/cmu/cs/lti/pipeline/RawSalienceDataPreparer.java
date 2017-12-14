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
        String xmiOutput = argv[2];

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

        new BasicPipeline(reader, true, true, 7, workingDir, xmiOutput, true, stanfordAnalyzer).run();
    }
}
