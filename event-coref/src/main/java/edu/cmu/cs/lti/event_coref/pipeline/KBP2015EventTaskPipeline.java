package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.annotator.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.annotators.OpenNlpChunker;
import edu.cmu.cs.lti.annotators.QuoteAnnotator;
import edu.cmu.cs.lti.annotators.WordNetBasedEntityAnnotator;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.emd.annotators.acceptors.AllCandidateAcceptor;
import edu.cmu.cs.lti.emd.annotators.classification.RealisTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.crf.CrfMentionTypeAnnotator;
import edu.cmu.cs.lti.emd.pipeline.CrfMentionTrainingLooper;
import edu.cmu.cs.lti.event_coref.annotators.*;
import edu.cmu.cs.lti.learning.train.RealisClassifierTrainer;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.AbstractProcessorBuilder;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.AnalysisComponent;
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
import java.lang.reflect.InvocationTargetException;

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
    // Main working directory.
    final String workingDir;
    // Models.
    final String modelDir;

    // Base directory to store preprocessed data.
    final String preprocessBase = "preprocessed";

    // Base directory to store intermediate results during prediction.
    final String middleResults = "intermediate";

    // For outputs with cross validation, we have a auto generated suffix for it. We use this for the case where we do
    // it on all.
    final String fullRunSuffix = "_all";

    final Configuration taskConfig;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public KBP2015EventTaskPipeline(String typeSystemName, String modelDir, Configuration taskConfig) {
        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);
        this.modelDir = modelDir;
        this.taskConfig = taskConfig;

        this.workingDir = taskConfig.get("edu.cmu.cs.lti.working.dir");
        this.goldStandardFilePath = taskConfig.get("edu.cmu.cs.lti.gold.tbf");
        this.plainTextDataDir = taskConfig.get("edu.cmu.cs.lti.source_text.dir");
        this.tokenMapDir = taskConfig.get("edu.cmu.cs.lti.token_map.dir");

        logger.info(String.format("Reading gold tbf from %s , token from %s, source from %s", goldStandardFilePath,
                tokenMapDir, plainTextDataDir));
        logger.info(String.format("Main output can be found at %s.", workingDir));
    }

    public void prepare() throws UIMAException, IOException {
        boolean invalidateExisting = taskConfig.getBoolean("edu.cmu.cs.lti.preprocess.invalidate", false);
        if (!invalidateExisting && new File(workingDir, preprocessBase).exists()) {
            logger.info("Preprocessed data already exists, will not rerun.");
            return;
        }

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
                        TbfEventDataReader.PARAM_TOKEN_EXT, ".tab",
                        TbfEventDataReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
                );
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
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

                AnalysisEngineDescription quoteAnnotator = AnalysisEngineFactory.createEngineDescription(
                        QuoteAnnotator.class, typeSystemDescription
                );

                AnalysisEngineDescription wordNetEntityAnnotator = AnalysisEngineFactory.createEngineDescription(
                        WordNetBasedEntityAnnotator.class, typeSystemDescription,
                        WordNetBasedEntityAnnotator.PARAM_JOB_TITLE_LIST,
                        taskConfig.get("edu.cmu.cs.lti.profession_list"),
                        WordNetBasedEntityAnnotator.PARAM_WN_PATH,
                        taskConfig.get("edu.cmu.cs.lti.wndict.path")
                );

                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                        preprocessBase);

                return new AnalysisEngineDescription[]{
                        stanfordAnalyzer, semaforAnalyzer, fanseParser, opennlp, quoteAnnotator,
                        wordNetEntityAnnotator, xmiWriter
                };
            }
        }, typeSystemDescription);

        pipeline.run();
    }

    public String trainMentionTypeLv1(Configuration kbpConfig, CollectionReaderDescription trainingReader,
                                      String suffix) throws UIMAException, IOException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        logger.info("Starting Training ...");

        String cvModelDir = new File(kbpConfig.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir"), suffix).getPath();
        boolean skipTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);
        if (!skipTrain) {
            logger.info("Saving model directory at " + cvModelDir);
            File goldCache = new File(kbpConfig.get("edu.cmu.cs.lti.mention.cache.dir"), suffix);
            CrfMentionTrainingLooper mentionTypeTrainer = new CrfMentionTrainingLooper(kbpConfig, cvModelDir, goldCache,
                    typeSystemDescription, trainingReader);
            mentionTypeTrainer.runLoopPipeline();
        } else {
            logger.info("Skipping training");
        }

        return cvModelDir;
    }

    public CollectionReaderDescription lv1MentionDetection(CollectionReaderDescription reader, String modelDir, String
            baseOutput, Configuration config) throws UIMAException, IOException {
        BasicPipeline systemPipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription crfLevel1Annotator = AnalysisEngineFactory.createEngineDescription(
                        CrfMentionTypeAnnotator.class, typeSystemDescription,
                        CrfMentionTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                        CrfMentionTypeAnnotator.PARAM_CONFIGURATION_PATH, config.getConfigFile()
                );

                AnalysisEngineDescription everythingAcceptor = AnalysisEngineFactory.createEngineDescription(
                        AllCandidateAcceptor.class, typeSystemDescription
                );

                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                        baseOutput);

                return new AnalysisEngineDescription[]{crfLevel1Annotator, everythingAcceptor, xmiWriter};
            }
        }, typeSystemDescription);

        systemPipeline.run();

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, baseOutput);
    }

    public String trainRealisTypes(Configuration kbpConfig, CollectionReaderDescription trainingReader, String
            suffix) throws Exception {
        RealisClassifierTrainer trainer = new RealisClassifierTrainer(typeSystemDescription, trainingReader, kbpConfig);
        String realisCvModelDir = new File(kbpConfig.get("edu.cmu.cs.lti.model.realis.dir"), suffix).getPath();
        trainer.buildModels(realisCvModelDir);

        return realisCvModelDir;
    }

    public CollectionReaderDescription realisAnnotation(Configuration config, CollectionReaderDescription reader,
                                                        String modelDir, String realisOutputBase)
            throws IOException, UIMAException {
        new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription realisAnnotator = AnalysisEngineFactory.createEngineDescription(
                        RealisTypeAnnotator.class, typeSystemDescription,
                        RealisTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                        RealisTypeAnnotator.PARAM_CONFIG_PATH, config.getConfigFile(),
                        RealisTypeAnnotator.PARAM_FEATURE_PACKAGE_NAME,
                        config.get("edu.cmu.cs.lti.feature.sentence.package.name")
                );
                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                        realisOutputBase);
                return new AnalysisEngineDescription[]{realisAnnotator, xmiWriter};
            }
        }, typeSystemDescription).run();

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, realisOutputBase);
    }

    public CollectionReaderDescription annotateGold(CollectionReaderDescription reader, String baseOutput,
                                                    boolean type, boolean realis, boolean coref)
            throws UIMAException, IOException {
        new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription goldMentionTypeAnnotator = AnalysisEngineFactory.createEngineDescription(
                        GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                        GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS,
                        new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName},
                        GoldStandardEventMentionAnnotator.PARAM_COPY_TYPE, type,
                        GoldStandardEventMentionAnnotator.PARAM_COPY_REALIS, realis,
                        GoldStandardEventMentionAnnotator.PARAM_COPY_COREFERENCE, coref
                );
                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                        baseOutput);
                return new AnalysisEngineDescription[]{goldMentionTypeAnnotator, xmiWriter};
            }
        }, typeSystemDescription).run();

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, baseOutput);
    }

    private CollectionReaderDescription prepareCorefTraining(CollectionReaderDescription reader, String outputBase)
            throws UIMAException, IOException {
        // The preparation is only done for the first time.
        if (!new File(workingDir, outputBase).exists()) {
            BasicPipeline pipeline = new BasicPipeline(new AbstractProcessorBuilder() {
                @Override
                public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription mentionAndCorefGoldAnnotator = AnalysisEngineFactory
                            .createEngineDescription(
                                    GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                                    GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS,
                                    new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName},
                                    GoldStandardEventMentionAnnotator.PARAM_COPY_TYPE, true,
                                    GoldStandardEventMentionAnnotator.PARAM_COPY_REALIS, true,
                                    GoldStandardEventMentionAnnotator.PARAM_COPY_COREFERENCE, true
                            );
                    AnalysisEngineDescription argumentExtractor = AnalysisEngineFactory.createEngineDescription(
                            ArgumentExtractor.class, typeSystemDescription
                    );

                    AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                            outputBase);
                    return new AnalysisEngineDescription[]{mentionAndCorefGoldAnnotator, argumentExtractor, xmiWriter};
                }
            }, typeSystemDescription);

            pipeline.run();
        }

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, outputBase);
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
        String trainingDataOutput = edu.cmu.cs.lti.utils.FileUtils.joinPaths(middleResults, suffix, "coref_training");
        CollectionReaderDescription trainingAnnotatedReader = prepareCorefTraining(trainingReader, trainingDataOutput);
        logger.info("Start coreference training.");
        String modelDir = new File(taskConfig.get("edu.cmu.cs.lti.model.event_coref.latent_tree"), suffix).getPath();

        logger.info("Saving model directory at : " + modelDir);
        LatentTreeTrainingLooper corefTrainer = new LatentTreeTrainingLooper(taskConfig, modelDir,
                typeSystemDescription, trainingAnnotatedReader);
        corefTrainer.runLoopPipeline();
        logger.info("Coreference training finished ...");
        return modelDir;
    }

    private CollectionReaderDescription corefResolution(CollectionReaderDescription reader, Configuration config,
                                                        String modelDir, String outputBase)
            throws UIMAException, IOException {
        logger.info("Running coreference resolution, output at " + outputBase);

        BasicPipeline testPipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription argumentExtractor = AnalysisEngineFactory.createEngineDescription(
                        ArgumentExtractor.class, typeSystemDescription
                );

                AnalysisEngineDescription corefAnnotator = AnalysisEngineFactory.createEngineDescription(
                        EventCorefAnnotator.class, typeSystemDescription,
                        EventCorefAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                        EventCorefAnnotator.PARAM_CONFIG_PATH, config.getConfigFile()
                );
                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                        outputBase);
                return new AnalysisEngineDescription[]{argumentExtractor, corefAnnotator, xmiWriter};
            }
        }, typeSystemDescription);

        testPipeline.run();

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, outputBase);
    }

    // TODO joint inference of mention and detection.
    public void joinMentionDetectionAndCoreference() {

    }

    private void writeResults(CollectionReaderDescription processedResultReader,
                              String baseOutputDir, String systemId, String suffix)
            throws UIMAException, IOException {
        String tbfOutputPath = FileUtils.joinPaths(baseOutputDir, systemId, systemId + "_" + suffix + ".tbf");

        logger.info("Writing results to " + baseOutputDir);

        BasicPipeline systemPipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return processedResultReader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                        TbfStyleEventWriter.class, typeSystemDescription,
                        TbfStyleEventWriter.PARAM_OUTPUT_PATH, tbfOutputPath,
                        TbfStyleEventWriter.PARAM_SYSTEM_ID, systemId
                );

                return new AnalysisEngineDescription[]{resultWriter};
            }
        }, typeSystemDescription);
        systemPipeline.run();
    }

    public void runBaselines(CollectionReaderDescription reader, String baselineTbfOutput,
                             Class<? extends AnalysisComponent> baselineComponentClass)
            throws UIMAException, IOException {
        new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription baseline = AnalysisEngineFactory.createEngineDescription(
                        baselineComponentClass, typeSystemDescription
                );

                AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                        TbfStyleEventWriter.class, typeSystemDescription,
                        TbfStyleEventWriter.PARAM_OUTPUT_PATH, baselineTbfOutput,
                        TbfStyleEventWriter.PARAM_SYSTEM_ID, "gold"
                );

                return new AnalysisEngineDescription[]{baseline, resultWriter};
            }
        }, typeSystemDescription).run();
    }

    public void annotateAndWriteGold(CollectionReaderDescription reader, String goldTbfOutput)
            throws UIMAException, IOException {
        new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription goldCopier = AnalysisEngineFactory.createEngineDescription(
                        GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                        GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS, new String[]{CAS.NAME_DEFAULT_SOFA},
                        GoldStandardEventMentionAnnotator.PARAM_COPY_REALIS, true,
                        GoldStandardEventMentionAnnotator.PARAM_COPY_TYPE, true,
                        GoldStandardEventMentionAnnotator.PARAM_COPY_COREFERENCE, true
                );

                AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                        TbfStyleEventWriter.class, typeSystemDescription,
                        TbfStyleEventWriter.PARAM_OUTPUT_PATH, goldTbfOutput,
                        TbfStyleEventWriter.PARAM_SYSTEM_ID, "gold"
                );

                return new AnalysisEngineDescription[]{goldCopier, resultWriter};
            }
        }, typeSystemDescription).run();
    }

    public void trainAll() throws Exception {
        logger.info("Staring training a full model on all data in " + preprocessBase);
        CollectionReaderDescription trainingReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, workingDir, preprocessBase);
        trainMentionTypeLv1(taskConfig, trainingReader, fullRunSuffix);
        trainRealisTypes(taskConfig, trainingReader, fullRunSuffix);
        trainLatentTreeCoref(taskConfig, trainingReader, fullRunSuffix);
        logger.info("All training done.");
    }

    public void testAll() throws UIMAException, IOException {
        CollectionReaderDescription testDataReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, workingDir, preprocessBase);

        String crfTypeModelDir = taskConfig.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir") + fullRunSuffix;
        String mentionLv1Output = middleResults + "/" + fullRunSuffix + "/mention_lv1";

        logger.info("Going to run mention type on [" + workingDir + "/" + preprocessBase + "], output will be at " +
                mentionLv1Output);
        CollectionReaderDescription lv1Output = lv1MentionDetection(testDataReader, crfTypeModelDir,
                mentionLv1Output, taskConfig);

        // Run realis on Lv1 crf mentions.
        String realisModelDir = taskConfig.get("edu.cmu.cs.lti.model.realis.dir") + fullRunSuffix;
        String lv1RealisOutput = middleResults + "/" + fullRunSuffix + "/lv1_realis";
        logger.info("Going to run realis classifier on " + mentionLv1Output + " output will be at " + lv1RealisOutput);
        CollectionReaderDescription lv1MentionRealisResults = realisAnnotation(taskConfig, lv1Output,
                realisModelDir, lv1RealisOutput);

        // Output final result.
        String evalPath = taskConfig.get("edu.cmu.cs.lti.eval.base");
        File typeLv1Eval = new File(new File(workingDir, evalPath), "lv1_types");
        writeResults(lv1MentionRealisResults,
                new File(typeLv1Eval, "lv1_mention_realis" + fullRunSuffix + ".tbf").getAbsolutePath(),
                "CMU-LTI-Run1", fullRunSuffix
        );
    }

    public void crossValidation() throws Exception {
        int numSplit = taskConfig.getInt("edu.cmu.cs.lti.cv.split", 5);
        int seed = taskConfig.getInt("edu.cmu.cs.lti.cv.seed", 17);
        String evalBase = taskConfig.get("edu.cmu.cs.lti.eval.base");

        String evalPath = new File(workingDir, evalBase).getPath();

        for (int slice = 0; slice < numSplit; slice++) {
            String sliceSuffix = "split_" + slice;

//            File evalPath = edu.cmu.cs.lti.utils.FileUtils.joinPathsAsFile(workingDir, evalBase, sliceSuffix);
            edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(evalPath);

            CollectionReaderDescription trainingSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, workingDir, preprocessBase, false, seed, slice);
            CollectionReaderDescription devSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, workingDir, preprocessBase, true, seed, slice);

            // Training Part:

//            // Train lv1 of the mention type model.
//            String crfTypeModelPath = trainMentionTypeLv1(taskConfig, trainingSliceReader, sliceSuffix);
//
//            // Train realis model.
//            String realisModelPath = trainRealisTypes(taskConfig, trainingSliceReader, sliceSuffix);

            // Train coreference model.
            String treeCorefModelPath = trainLatentTreeCoref(taskConfig, trainingSliceReader, sliceSuffix);

            // Testing Part:

            // Gold mentions : type.
//            CollectionReaderDescription goldMentionTypesOutput = annotateGold(devSliceReader,
//                    middleResults + "/" + sliceSuffix + "/gold_type", true, false, false);

//            // Mentions from the crf model.
//            CollectionReaderDescription lv1Output = lv1MentionDetection(devSliceReader, crfTypeModelPath,
//                    middleResults + "/" + sliceSuffix + "/mention_lv1", taskConfig);

            // Gold mentions : type and realis.
            CollectionReaderDescription goldMentionTypeRealisOutput = annotateGold(devSliceReader,
                    middleResults + "/" + sliceSuffix + "/gold_mention", true, true, false);


//            // Run realis on gold mentions.
//            CollectionReaderDescription goldTypeSystemRealisResults = realisAnnotation(taskConfig,
//                    goldMentionTypesOutput, realisModelPath, middleResults + "/" + sliceSuffix + "/gold_realis");
//
//            // Run realis on Lv1 crf mentions.
//            CollectionReaderDescription lv1MentionRealisResults = realisAnnotation(taskConfig, lv1Output,
//                    realisModelPath, middleResults + "/" + sliceSuffix + "/lv1_realis");

            // Run coreference on gold mention type and gold realis.
            CollectionReaderDescription goldMentionCorefResults = corefResolution(goldMentionTypeRealisOutput,
                    taskConfig, treeCorefModelPath, edu.cmu.cs.lti.utils.FileUtils.joinPaths(middleResults, sliceSuffix,
                            "goldType_goldRealis_treeCoref"));

//            // Run coreference on gold mention type and predicted realis.
//            CollectionReaderDescription goldTypeSystemRealisCorefResults = corefResolution
// (goldTypeSystemRealisResults,
//                    taskConfig, treeCorefModelPath, edu.cmu.cs.lti.utils.FileUtils.joinPaths(middleResults,
// sliceSuffix,
//                            "goldType_sysRealis_treeCoref"));
//
//            // Run coreference on predicted mentions.
//            CollectionReaderDescription systemMentionCorefResults = corefResolution(lv1MentionRealisResults,
// taskConfig,
//                    treeCorefModelPath, edu.cmu.cs.lti.utils.FileUtils.joinPaths(middleResults, sliceSuffix,
//                            "lv1Type_sysRealis_treeCoref"));

            // Output final result.
            writeResults(goldMentionCorefResults, evalPath, "goldType_goldRealis_treeCoref", sliceSuffix);
//            writeResults(goldTypeSystemRealisCorefResults, evalPath, "goldType_sysRealis_treeCoref", sliceSuffix);
//            writeResults(systemMentionCorefResults, evalPath, "lv1Type_sysRealis_treeCoref", sliceSuffix);

            // Run and write coreference baselines.
//            runBaselines(goldMentionTypeRealisOutput, new File(evalPath, "OneInOne_" + sliceSuffix + ".tbf").getPath(),
//                    OneInOneBaselineCorefAnnotator.class);
//            runBaselines(goldMentionTypeRealisOutput, new File(evalPath, "AllInOne_" + sliceSuffix + ".tbf").getPath(),
//                    AllInOneBaselineCorefAnnotator.class);
//            // Write gold standard.
//            annotateAndWriteGold(devSliceReader, new File(evalPath, "gold" + sliceSuffix + ".tbf").getPath());

            break;
        }
    }

    public static void main(String argv[]) throws Exception {
        if (argv.length < 1) {
            System.err.println("Please provide one argument for the settings file.");
        }

        Configuration trainingConfig = new Configuration(argv[0]);

        Configuration commonConfig = new Configuration("settings/common.properties");
        String modelPath = commonConfig.get("edu.cmu.cs.lti.model.dir");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        KBP2015EventTaskPipeline trainingPipeline = new KBP2015EventTaskPipeline(typeSystemName, modelPath,
                trainingConfig);

        trainingPipeline.prepare();
        trainingPipeline.crossValidation();
//        trainingPipeline.trainAll();

        if (argv.length >= 2) {
            Configuration testConfig = new Configuration(argv[1]);
            KBP2015EventTaskPipeline testingPipeline = new KBP2015EventTaskPipeline(typeSystemName, modelPath,
                    testConfig);
            testingPipeline.prepare();
            testingPipeline.testAll();
        }
    }
}