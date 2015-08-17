package edu.cmu.lti.event_coref.pipeline;

import edu.cmu.cs.lti.annotator.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.annotators.OpenNlpChunker;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.AbstractProcessorBuilder;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.lti.event_coref.annotators.InputTextCleaner;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;

/**
 * A pipeline structure for KBP 2015 event task of both Mention Detection and Coref.
 * This pipeline should be easily adapted to a general pipeline by replacing the preprocessors.
 * <p>
 * Date: 8/16/15
 * Time: 4:21 PM
 *
 * @author Zhengzhong Liu
 */
public class KBP2015EventTaskPipeline {
    final TypeSystemDescription typeSystemDescription;

    // Input data.
    final String goldStandardFilePath;
    final String plainTextDataDir;
    final String tokenMapDir;

    // Output directory.
    final String mainOutputDir;

    // Models.
    final String modelDir;

    public KBP2015EventTaskPipeline(String typeSystemName, String goldStandardFilePath, String plainTextDataDir,
                                    String tokenMapDir, String modelDir, String mainOutputDir) {
        this.typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);
        this.goldStandardFilePath = goldStandardFilePath;
        this.plainTextDataDir = plainTextDataDir;
        this.tokenMapDir = tokenMapDir;
        this.modelDir = modelDir;
        this.mainOutputDir = mainOutputDir;
    }

    public void process(String xmiOutputBase) throws UIMAException, IOException {
        final String semaforModelDirectory = modelDir + "/semafor_malt_model_20121129";
        final String fanseModelDirectory = modelDir + "/fanse_models";
        final String opennlpDirectory = modelDir + "/opennlp/en-chunker.bin";

        BasicPipeline pipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return CollectionReaderFactory.createReaderDescription(
                        TbfEventDataReader.class, typeSystemDescription,
                        TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, goldStandardFilePath,
                        TbfEventDataReader.PARAM_SOURCE_EXT, ".txt",
                        TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY, plainTextDataDir,
                        TbfEventDataReader.PARAM_TOKEN_DIRECTORY, tokenMapDir,
                        TbfEventDataReader.PARAM_TOKEN_EXT, ".tab"
                );
            }

            @Override
            public AnalysisEngineDescription[] buildPreprocessors() throws ResourceInitializationException {
                AnalysisEngineDescription cleaner = AnalysisEngineFactory.createEngineDescription(
                        InputTextCleaner.class, typeSystemDescription,
                        InputTextCleaner.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
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

                return new AnalysisEngineDescription[]{
                        cleaner, stanfordAnalyzer, semaforAnalyzer, fanseParser, opennlp
                };
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                return new AnalysisEngineDescription[0];
            }

            @Override
            public AnalysisEngineDescription[] buildPostProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(mainOutputDir,
                        xmiOutputBase);
                return new AnalysisEngineDescription[]{xmiWriter};
            }
        });

        pipeline.run();
    }

    // TODO train relevant models.
    public void train() {

    }

    // TODO calling mention detection only.
    public void mentionDetection() {

    }

    // TODO calling coreference only.
    public void coreference() {

    }

    // TODO joint inference of mention and detection.
    public void joinMentionDetectionAndCoreference() {

    }

    public static void main(String argv[]) throws UIMAException, IOException {
        Configuration kbpConfig = new Configuration("settings/kbp.properties");
        Configuration commonConfig = new Configuration("settings/common.properties");

        String modelPath = commonConfig.get("edu.cmu.cs.lti.model.dir");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");


        String parenOutputtDir = kbpConfig.get("edu.cmu.cs.lti.output.parent.dir");
        String goldTbf = kbpConfig.get("edu.cmu.cs.lti.gold.tbf");
        String sourceDir = kbpConfig.get("edu.cmu.cs.lti.source_text.dir");
        String tokenDir = kbpConfig.get("edu.cmu.cs.lti.token_map.dir");

        KBP2015EventTaskPipeline pipeline = new KBP2015EventTaskPipeline(typeSystemName, goldTbf, sourceDir,
                tokenDir, modelPath, parenOutputtDir);

        String xmiOutputBase = "argument_extracted";
        pipeline.process(xmiOutputBase);
    }
}
