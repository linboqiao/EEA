package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.annotator.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.annotators.OpenNlpChunker;
import edu.cmu.cs.lti.annotators.QuoteAnnotator;
import edu.cmu.cs.lti.annotators.WordNetBasedEntityAnnotator;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.emd.annotators.acceptors.AllCandidateAcceptor;
import edu.cmu.cs.lti.event_coref.annotators.ArgumentExtractor;
import edu.cmu.cs.lti.event_coref.annotators.GoldStandardEventMentionAnnotator;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.AbstractProcessorBuilder;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
import java.io.IOException;

/**
 * A pipeline structure that create mentions first and then create coref links
 * <p>
 * Created with IntelliJ IDEA.
 * Date: 4/15/15
 * Time: 5:20 PM
 *
 * @author Zhengzhong Liu
 */
public class CorefPipeline {
    final TypeSystemDescription typeSystemDescription;

    // Input data.
    final String goldStandardFilePath;
    final String plainTextDataDir;
    final String tokenMapDir;

    // Output directory.
    final String workingDir;
    // Models.
    final String modelDir;
    // The task configuration.
    final Configuration taskConfig;
    // Base directory to store intermediate results during prediction (i.e. temporary mention detection, realis
    // detection)
    final String middleResults = "intermediate";

    // For outputs with cross validation, we have a auto generated suffix for it. We use this for the case where we do
    // it on all.
    final String fullRunSuffix = "all";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public CorefPipeline(String typeSystemName, String modelDir, Configuration taskConfig) {
        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);

        this.goldStandardFilePath = taskConfig.get("edu.cmu.cs.lti.gold.tbf");
        this.plainTextDataDir = taskConfig.get("edu.cmu.cs.lti.source_text.dir");
        this.tokenMapDir = taskConfig.get("edu.cmu.cs.lti.token_map.dir");
        this.workingDir = taskConfig.get("edu.cmu.cs.lti.working.dir");
        this.taskConfig = taskConfig;
        this.modelDir = modelDir;
        logger.info(String.format("Reading gold tbf from %s , token from %s, source from %s", goldStandardFilePath,
                tokenMapDir, plainTextDataDir));
        logger.info(String.format("Main output can be found at %s.", workingDir));
    }

    public void prepareEventMentions(String preprocessBase) throws UIMAException, IOException {
        final String semaforModelDirectory = modelDir + "/semafor_malt_model_20121129";
        final String fanseModelDirectory = modelDir + "/fanse_models";
        final String opennlpDirectory = modelDir + "/opennlp/en-chunker.bin";

        logger.info("Running preparation, from " + plainTextDataDir + ", results will be found at " + preprocessBase);

        BasicPipeline pipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return CollectionReaderFactory.createReaderDescription(
                        TbfEventDataReader.class, typeSystemDescription,
                        TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, goldStandardFilePath,
                        TbfEventDataReader.PARAM_SOURCE_EXT, ".txt",
                        TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY, plainTextDataDir,
                        TbfEventDataReader.PARAM_TOKEN_DIRECTORY, tokenMapDir,
                        TbfEventDataReader.PARAM_TOKEN_EXT, ".tab",
                        TbfEventDataReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
                );
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws
                    ResourceInitializationException {
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

                AnalysisEngineDescription argumentExtractor = AnalysisEngineFactory.createEngineDescription(
                        ArgumentExtractor.class, typeSystemDescription
                );
                AnalysisEngineDescription quoteAnnotator = AnalysisEngineFactory.createEngineDescription(
                        QuoteAnnotator.class, typeSystemDescription
                );

                AnalysisEngineDescription wordNetEntityAnnotator = AnalysisEngineFactory.createEngineDescription(
                        WordNetBasedEntityAnnotator.class, typeSystemDescription,
                        WordNetBasedEntityAnnotator.PARAM_JOB_TITLE_LIST, taskConfig.get("edu.cmu.cs.lti" +
                                ".profession_list"),
                        WordNetBasedEntityAnnotator.PARAM_WN_PATH, taskConfig.get("edu.cmu.cs.lti.wndict.path")
                );

                // NOTE: run a mention annotator here, this could be a gold annotator, or a trained annotator.
                AnalysisEngineDescription eventMentionAnnotator = AnalysisEngineFactory.createEngineDescription(
                        GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                        GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS, new String[]{CAS.NAME_DEFAULT_SOFA,
                                UimaConst.inputViewName}
                );

                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                        preprocessBase);

                return new AnalysisEngineDescription[]{stanfordAnalyzer, semaforAnalyzer,
                        fanseParser, opennlp, argumentExtractor, quoteAnnotator,
                        wordNetEntityAnnotator, eventMentionAnnotator, xmiWriter};
            }
        }, typeSystemDescription);
        pipeline.run();
    }

    /**
     * Train the latent tree model coreference resolver.
     *
     * @param taskConfig     The configuration file.
     * @param trainingReader Reader for the training data.
     * @param suffix         The suffix for the model.
     * @return The trained model directory.
     */
    private String trainLatentTreeCoref(Configuration taskConfig, CollectionReaderDescription trainingReader, String
            suffix) throws UIMAException, IOException {
        logger.info("Start coreference training.");
        String cvModelDir = taskConfig.get("edu.cmu.cs.lti.model.event_coref.latent_tree") + suffix;

        logger.info("Saving model directory at : " + cvModelDir);
        LatentTreeTrainingLooper corefTrainer = new LatentTreeTrainingLooper(taskConfig, cvModelDir,
                typeSystemDescription, trainingReader);
        corefTrainer.runLoopPipeline();
        logger.info("Coreference training finished ...");
        return cvModelDir;
    }

    private CollectionReaderDescription corefResolution(CollectionReaderDescription reader, String modelDir, String
            baseOutput) throws ResourceInitializationException {
        //TODO finish this.
        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, baseOutput);
    }

    public void writeResults(CollectionReaderDescription processedResultReader, String tbfOutput, String systemId)
            throws UIMAException, IOException {
        logger.info("Writing results to " + tbfOutput);

        AnalysisEngineDescription everythingAcceptor = AnalysisEngineFactory.createEngineDescription(
                AllCandidateAcceptor.class, typeSystemDescription
        );

        BasicPipeline systemPipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return processedResultReader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                        TbfStyleEventWriter.class, typeSystemDescription,
                        TbfStyleEventWriter.PARAM_OUTPUT_PATH, tbfOutput,
                        TbfStyleEventWriter.PARAM_SYSTEM_ID, systemId
                );

                return new AnalysisEngineDescription[]{everythingAcceptor, resultWriter};
            }
        }, typeSystemDescription);
        systemPipeline.run();
    }

    public void crossValidation(String inputBaseDir) throws UIMAException, IOException {
        int numSplit = taskConfig.getInt("edu.cmu.cs.lti.cv.split", 5);
        int seed = taskConfig.getInt("edu.cmu.cs.lti.cv.seed", 17);
        String evalPath = taskConfig.get("edu.cmu.cs.lti.eval.base");

        File goldCorefEval = new File(new File(workingDir, evalPath), "gold_type_coref");
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(goldCorefEval);

        for (int slice = 0; slice < numSplit; slice++) {
            String sliceSuffix = "split_" + slice;

            CollectionReaderDescription trainingSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, workingDir, inputBaseDir, false, seed, slice);
            CollectionReaderDescription devSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, workingDir, inputBaseDir, true, seed, slice);

            // Train the tree coreference model on the training data.
            String treeCorefModel = trainLatentTreeCoref(taskConfig, trainingSliceReader, sliceSuffix);

            // Run coreference on dev data.
            String corefOutput = FileUtils.joinPaths(middleResults, sliceSuffix, "treeCoref");
            CollectionReaderDescription corefResultReader = corefResolution(devSliceReader, treeCorefModel,
                    corefOutput);

            // Output final result.
            writeResults(corefResultReader,
                    FileUtils.joinPaths(workingDir, evalPath, "treeCoref", "lv1_mention_realis" + sliceSuffix + ".tbf"),
                    "treeCoref"
            );
        }
    }

    public static void main(String[] argv) throws UIMAException, IOException {
        if (argv.length < 1) {
            System.err.println("Please provide one argument for the settings file.");
        }

        Configuration taskConfig = new Configuration(argv[0]);
        Configuration commonConfig = new Configuration("settings/common.properties");

        String modelPath = commonConfig.get("edu.cmu.cs.lti.model.dir");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        CorefPipeline pipeline = new CorefPipeline(typeSystemName, modelPath, taskConfig);

        String preprocesseBase = "preprocessed";
//        pipeline.prepareEventMentions(preprocesseBase);
        pipeline.crossValidation(preprocesseBase);
    }
}