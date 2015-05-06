package edu.cmu.lti.event_coref.pipeline;

import edu.cmu.cs.lti.annotator.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.annotators.OpenNlpChunker;
import edu.cmu.cs.lti.collection_reader.BratEventGoldStandardAnnotator;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.AbstractProcessorBuilder;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.lti.event_coref.annotators.ArgumentExtractor;
import edu.cmu.lti.event_coref.annotators.GoldStandardEventMentionAnnotator;
import edu.cmu.lti.event_coref.annotators.InputTextCleaner;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;

/**
 * A pipeline structure that create mentions first and then create coref links
 * <p/>
 * Created with IntelliJ IDEA.
 * Date: 4/15/15
 * Time: 5:20 PM
 *
 * @author Zhengzhong Liu
 */
public class CorefPipeline {
    public void prepareEventMentions(String typeSystemName, final String parentDir, final String modelPath, final String xmiOutputBase) throws UIMAException, IOException {
        final TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);

        final String sourceTextDir = parentDir + "/LDC2014E121/source";
        final String tokenOffsetDir = parentDir + "/LDC2014E121/token_offset";
        final String annotationDir = parentDir + "/LDC2014E121/annotation";

        final String semaforModelDirectory = modelPath + "/semafor_malt_model_20121129";
        final String fanseModelDirectory = modelPath + "/fanse_models";
        final String opennlpDirectory = modelPath + "/opennlp/en-chunker.bin";


        BasicPipeline pipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return CollectionReaderFactory.createReaderDescription(
                        PlainTextCollectionReader.class,
                        PlainTextCollectionReader.PARAM_INPUTDIR, sourceTextDir,
                        PlainTextCollectionReader.PARAM_TEXT_SUFFIX, BratEventGoldStandardAnnotator.defaultTextFileNameSuffix,
                        PlainTextCollectionReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName);
            }

            @Override
            public AnalysisEngineDescription[] buildPreprocessors() throws ResourceInitializationException {
                return new AnalysisEngineDescription[0];
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription cleaner = AnalysisEngineFactory.createEngineDescription(
                        InputTextCleaner.class, typeSystemDescription,
                        InputTextCleaner.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
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

                AnalysisEngineDescription opennlp = AnalysisEngineFactory.createEngineDescription(
                        OpenNlpChunker.class, typeSystemDescription,
                        OpenNlpChunker.PARAM_MODEL_PATH, opennlpDirectory);

                AnalysisEngineDescription eventMentionAnnotator = AnalysisEngineFactory.createEngineDescription(
                        GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                        GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS, new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName}
                );
                AnalysisEngineDescription argumentExtractor = AnalysisEngineFactory.createEngineDescription(
                        ArgumentExtractor.class, typeSystemDescription
                );
                return new AnalysisEngineDescription[]{cleaner, goldStandard, stanfordAnalyzer, semaforAnalyzer, fanseParser, opennlp, eventMentionAnnotator, argumentExtractor};
            }

            @Override
            public AnalysisEngineDescription[] buildPostProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(parentDir, xmiOutputBase);
                return new AnalysisEngineDescription[]{xmiWriter};
            }
        });
        pipeline.run();
    }

    public static void main(String[] argv) throws UIMAException, IOException {
        CorefPipeline pipeline = new CorefPipeline();
        String typeSystemName = "TypeSystem";
        String parentDir = argv[0];
        String xmiOutputBase = "argument_extracted";
        String modelPath = argv[1];
        pipeline.prepareEventMentions(typeSystemName, parentDir, modelPath, xmiOutputBase);
    }
}