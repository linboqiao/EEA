package edu.cmu.lti.event_coref.pipeline;

import edu.cmu.cs.lti.annotator.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.collection_reader.BratEventGoldStandardAnnotator;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.lti.event_coref.annotators.ForumTextCleaner;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/13/15
 * Time: 2:27 PM
 *
 * @author Zhengzhong Liu
 */
public class Preprocessor {
    private static final Logger logger = LoggerFactory.getLogger(Preprocessor.class);

    public static void main(String[] args) throws ResourceInitializationException {
        String parentDir = args[0]; //"event-coref/data/brat_event";
        String modelPath = args[1];

        String sourceTextDir = parentDir + "/LDC2014E121/source";
        String tokenOffsetDir = parentDir + "/LDC2014E121/token_offset";
        String annotationDir = parentDir + "/LDC2014E121/annotation";
        String outputBaseDir = "sentence_parsed";// "sentence_parsed";

        String semaforModelDirectory = modelPath + "/semafor_malt_model_20121129";
        String fanseModelDirectory = modelPath + "/fanse_models";

        String paramTypeSystemDescriptor = "TypeSystem";

        String inputViewName = "original";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class,
                PlainTextCollectionReader.PARAM_INPUTDIR, sourceTextDir,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, BratEventGoldStandardAnnotator.defaultTextFileNameSuffix,
                PlainTextCollectionReader.PARAM_INPUT_VIEW_NAME, inputViewName);

        AnalysisEngineDescription cleaner = AnalysisEngineFactory.createEngineDescription(
                ForumTextCleaner.class, typeSystemDescription,
                ForumTextCleaner.PARAM_INPUT_VIEW_NAME, inputViewName
        );

        AnalysisEngineDescription goldStandard = AnalysisEngineFactory.createEngineDescription(
                BratEventGoldStandardAnnotator.class, typeSystemDescription,
                BratEventGoldStandardAnnotator.PARAM_ANNOTATION_DIR, annotationDir,
                BratEventGoldStandardAnnotator.PARAM_TOKENIZATION_MAP_DIR, tokenOffsetDir
        );
        AnalysisEngineDescription stanfordAnalyzer = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_USE_SUTIME, true);

        AnalysisEngineDescription semaforAnalyzer = AnalysisEngineFactory.createEngineDescription(
                SemaforAnnotator.class, typeSystemDescription,
                SemaforAnnotator.SEMAFOR_MODEL_PATH, semaforModelDirectory);

        AnalysisEngineDescription fanseParser = AnalysisEngineFactory.createEngineDescription(
                FanseAnnotator.class, typeSystemDescription, FanseAnnotator.PARAM_MODEL_BASE_DIR,
                fanseModelDirectory);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                parentDir, outputBaseDir, null, null
        );

        try {
//            SimplePipeline.runPipeline(reader, cleaner,  goldStandard, writer);
            SimplePipeline.runPipeline(reader, cleaner, goldStandard, stanfordAnalyzer, semaforAnalyzer, fanseParser, writer);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

}
