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
import edu.cmu.cs.lti.emd.annotators.gold.GoldCandidateAnnotator;
import edu.cmu.cs.lti.emd.pipeline.CrfMentionTrainingLooper;
import edu.cmu.cs.lti.event_coref.annotators.ArgumentExtractor;
import edu.cmu.cs.lti.event_coref.annotators.EventCorefAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.GoldStandardEventMentionAnnotator;
import edu.cmu.cs.lti.learning.train.RealisClassifierTrainer;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.AbstractProcessorBuilder;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.commons.io.FileUtils;
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

    // Output directory.
    final String workingDir;

    // Models.
    final String modelDir;

    // Base directory to store intermediate results during prediction (i.e. temporary mention detection, realis
    // detection)
    final String middleResults = "intermediate";

    // For outputs with cross validation, we have a auto generated suffix for it. We use this for the case where we do
    // it on all.
    final String fullRunSuffix = "all";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public KBP2015EventTaskPipeline(String typeSystemName, String goldStandardFilePath, String plainTextDataDir,
                                    String tokenMapDir, String modelDir, String workingDir) {
        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);
        this.goldStandardFilePath = goldStandardFilePath;
        this.plainTextDataDir = plainTextDataDir;
        this.tokenMapDir = tokenMapDir;
        this.modelDir = modelDir;
        this.workingDir = workingDir;

        logger.info(String.format("Reading gold tbf from %s , token from %s, source from %s", goldStandardFilePath,
                tokenMapDir, plainTextDataDir));
        logger.info(String.format("Main output can be found at %s.", workingDir));
    }

    public void prepare(Configuration taskConfig, String preprocessOutputBase) throws UIMAException, IOException {
        File preprocessDir = new File(workingDir, preprocessOutputBase);

        if (preprocessDir.exists()) {
            logger.info("Preprocessed data exists, not running.");
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
                        preprocessOutputBase);

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

        String modelPath = kbpConfig.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir") + suffix;
        File modelFile = new File(modelPath);
        boolean skipTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);

        // Only skip training when model directory exists.
        if (!modelFile.exists() || !skipTrain) {
            File classFile = kbpConfig.getFile("edu.cmu.cs.lti.mention.classes.path");
            File cacheDir = new File(kbpConfig.get("edu.cmu.cs.lti.mention.cache.dir") + suffix);
            String[] classes = FileUtils.readLines(classFile).stream().map(l -> l.split("\t"))
                    .filter(p -> p.length >= 1).map(p -> p[0]).toArray(String[]::new);

            for (String c : classes) {
                logger.info("Register class " + c);
            }

            logger.info("Saving model directory at " + modelPath);

            CrfMentionTrainingLooper mentionTypeTrainer = new CrfMentionTrainingLooper(classes, kbpConfig, modelPath,
                    cacheDir, typeSystemDescription, trainingReader);
            mentionTypeTrainer.runLoopPipeline();
        } else {
            logger.info("Skipping training");
        }

        return modelPath;
    }

    public CollectionReaderDescription goldCandidateAnnotation(CollectionReaderDescription reader, String baseOutput)
            throws UIMAException, IOException {
        BasicPipeline pipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription goldMentionTypeAnnotator = AnalysisEngineFactory.createEngineDescription(
                        GoldCandidateAnnotator.class, typeSystemDescription
                );
                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                        baseOutput);
                return new AnalysisEngineDescription[]{goldMentionTypeAnnotator, xmiWriter};
            }
        }, typeSystemDescription);

        pipeline.run();

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, baseOutput);
    }

    private CollectionReaderDescription annotateGoldCoref(CollectionReaderDescription reader, String outputBase)
            throws UIMAException, IOException {
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
                                    new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName}
                            );
                    AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                            outputBase);
                    return new AnalysisEngineDescription[]{mentionAndCorefGoldAnnotator, xmiWriter};
                }
            }, typeSystemDescription);

            pipeline.run();
        }

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, outputBase);
    }

    public CollectionReaderDescription mentionDetection(CollectionReaderDescription reader, String modelDir, String
            baseOutput, Configuration config) throws UIMAException, IOException {
        // TODO: Static variable is not so nice here, it can actually be passed in.
        CrfMentionTypeAnnotator.config = config;

        BasicPipeline systemPipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription crfLevel1Annotator = AnalysisEngineFactory.createEngineDescription(
                        CrfMentionTypeAnnotator.class, typeSystemDescription,
                        CrfMentionTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir
                );

                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                        baseOutput);

                return new AnalysisEngineDescription[]{crfLevel1Annotator, xmiWriter};
            }
        }, typeSystemDescription);

        systemPipeline.run();

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, baseOutput);
    }


    public String trainRealisTypes(Configuration kbpConfig, CollectionReaderDescription trainingReader, String
            suffix) throws Exception {
        RealisClassifierTrainer trainer = new RealisClassifierTrainer(typeSystemDescription, trainingReader, kbpConfig);
        String realisCvModelDir = kbpConfig.get("edu.cmu.cs.lti.model.realis.dir") + suffix;
        trainer.buildModels(realisCvModelDir);

        return realisCvModelDir;
    }

    public CollectionReaderDescription realisAnnotation(Configuration config, CollectionReaderDescription reader,
                                                        String modelDir, String realisOutputBase)
            throws IOException, UIMAException {
        BasicPipeline pipeline = new BasicPipeline(new AbstractProcessorBuilder() {
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
        }, typeSystemDescription);

        pipeline.run();

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, realisOutputBase);
    }

    private CollectionReaderDescription prepareCorefTraining(CollectionReaderDescription reader,
                                                             String outputBase, int seed)
            throws UIMAException, IOException {
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
                                    new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName}
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

        return CustomCollectionReaderFactory.createRandomizedXmiReader(
                typeSystemDescription, workingDir, outputBase, seed);
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
        int seed = taskConfig.getInt("edu.cmu.cs.lti.cv.seed", 17);

        boolean skipTrain = taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptrain", false);
        boolean modelExists = new File(cvModelDir).exists();

        if (skipTrain && modelExists) {
            logger.info("Skipping coreference training ...");
        } else {
            logger.info("Prepare data for coreference training");
            String trainingDataOutput = edu.cmu.cs.lti.utils.FileUtils.joinPaths(middleResults, suffix,
                    "coref_training");
            CollectionReaderDescription trainingAnnotatedReader = prepareCorefTraining(trainingReader,
                    trainingDataOutput, seed);

            logger.info("Saving model directory at : " + cvModelDir);

            LatentTreeTrainingLooper corefTrainer = new LatentTreeTrainingLooper(taskConfig, cvModelDir,
                    typeSystemDescription, trainingAnnotatedReader);
            corefTrainer.runLoopPipeline();
            logger.info("Coreference training finished ...");
        }

        return cvModelDir;
    }

    private CollectionReaderDescription corefResolution(CollectionReaderDescription reader, Configuration config,
                                                        String modelDir, String outputBase, boolean useAverage)
            throws UIMAException, IOException {
        logger.info("Running coreference resolution, output at " + outputBase);

        BasicPipeline testPipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription goldMentionsAnnotator = AnalysisEngineFactory
                        .createEngineDescription(
                                GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                                GoldStandardEventMentionAnnotator.PARAM_COPY_MENTION_ONLY, true,
                                GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS,
                                new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName}
                        );
                AnalysisEngineDescription argumentExtractor = AnalysisEngineFactory.createEngineDescription(
                        ArgumentExtractor.class, typeSystemDescription
                );

                AnalysisEngineDescription corefAnnotator = AnalysisEngineFactory.createEngineDescription(
                        EventCorefAnnotator.class, typeSystemDescription,
                        EventCorefAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                        EventCorefAnnotator.PARAM_CONFIG_PATH, config.getConfigFile(),
                        EventCorefAnnotator.PARAM_USE_AVERAGE, useAverage
                );
                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                        outputBase);
                return new AnalysisEngineDescription[]{goldMentionsAnnotator, argumentExtractor, corefAnnotator,
                        xmiWriter};
            }
        }, typeSystemDescription);

        testPipeline.run();

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, outputBase);
    }

    // TODO joint inference of mention and detection.
    public void joinMentionDetectionAndCoreference() {

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

    public void writeGold(CollectionReaderDescription reader, String goldTbfOutput) throws UIMAException, IOException {
        BasicPipeline goldPipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription goldCopier = AnalysisEngineFactory.createEngineDescription(
                        GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                        GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS, new String[]{CAS.NAME_DEFAULT_SOFA}
                );

                AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                        TbfStyleEventWriter.class, typeSystemDescription,
                        TbfStyleEventWriter.PARAM_OUTPUT_PATH, goldTbfOutput,
                        TbfStyleEventWriter.PARAM_SYSTEM_ID, "gold"
                );

                return new AnalysisEngineDescription[]{goldCopier, resultWriter};
            }
        }, typeSystemDescription);

        goldPipeline.run();
    }

    public void trainAll(Configuration kbpConfig, String inputBaseDir) throws Exception {
        logger.info("Staring training a full model on all data in " + inputBaseDir);
        CollectionReaderDescription trainingReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, workingDir, inputBaseDir);
        trainMentionTypeLv1(kbpConfig, trainingReader, fullRunSuffix);
        trainRealisTypes(kbpConfig, trainingReader, fullRunSuffix);
        trainLatentTreeCoref(kbpConfig, trainingReader, fullRunSuffix);
        logger.info("All training done.");
    }

    public void test(Configuration testConfig, String inputBaseDir) throws UIMAException, IOException {
        CollectionReaderDescription testDataReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, workingDir, inputBaseDir);

        String sliceSuffix = fullRunSuffix;

        String crfTypeModelDir = testConfig.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir") + sliceSuffix;
        String mentionLv1Output = middleResults + "/" + sliceSuffix + "/mention_lv1";

        logger.info("Going to run mention type on [" + workingDir + "/" + inputBaseDir + "], output will be at " +
                mentionLv1Output);
        CollectionReaderDescription lv1OutputReader = mentionDetection(testDataReader, crfTypeModelDir,
                mentionLv1Output, testConfig);

        // Run realis on Lv1 crf mentions.
        String realisModelDir = testConfig.get("edu.cmu.cs.lti.model.realis.dir") + sliceSuffix;
        String lv1RealisOutput = middleResults + "/" + sliceSuffix + "/lv1_realis";

        logger.info("Going to run realis classifier on " + mentionLv1Output + " output will be at " + lv1RealisOutput);
        CollectionReaderDescription lv1MentionRealisResults = realisAnnotation(testConfig, lv1OutputReader,
                realisModelDir, lv1RealisOutput);

        // Output final result.
        String evalPath = testConfig.get("edu.cmu.cs.lti.eval.base");
        File typeLv1Eval = new File(new File(workingDir, evalPath), "lv1_types");
        writeResults(lv1MentionRealisResults,
                new File(typeLv1Eval, "lv1_mention_realis" + sliceSuffix + ".tbf").getAbsolutePath(),
                "CMU-LTI"
        );
    }

    public void crossValidation(Configuration taskConfig, String inputBaseDir) throws Exception {
        int numSplit = taskConfig.getInt("edu.cmu.cs.lti.cv.split", 5);
        int seed = taskConfig.getInt("edu.cmu.cs.lti.cv.seed", 17);
        String evalPath = taskConfig.get("edu.cmu.cs.lti.eval.base");

        File typeLv1Eval = new File(new File(workingDir, evalPath), "lv1_types");
        File goldMentionEval = new File(new File(workingDir, evalPath), "gold_types");
        File corefEval = edu.cmu.cs.lti.utils.FileUtils.joinPathsAsFile(workingDir, evalPath, "tree_coref");
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(typeLv1Eval);
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(goldMentionEval);

        for (int slice = 0; slice < numSplit; slice++) {
            logger.info("Starting CV split " + slice);

            String sliceSuffix = "split_" + slice;

            CollectionReaderDescription trainingSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, workingDir, inputBaseDir, false, seed, slice);
            CollectionReaderDescription devSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, workingDir, inputBaseDir, true, seed, slice);

//            // Train lv1 of the mention type model.
//            String crfTypeModelDir = trainMentionTypeLv1(taskConfig, trainingSliceReader, sliceSuffix);
//
//            // Train realis model.
//            String realisModelDir = trainRealisTypes(taskConfig, trainingSliceReader, sliceSuffix);
//            String goldBasedRealisOutput = middleResults + "/" + sliceSuffix + "/gold_realis";
//            String lv1RealisOutput = middleResults + "/" + sliceSuffix + "/lv1_realis";

            // Train coref model.
            String treeCorefModel = trainLatentTreeCoref(taskConfig, trainingSliceReader, sliceSuffix);

//            // Mentions from the crf model.
//            String mentionLv1Output = middleResults + "/" + sliceSuffix + "/mention_lv1";
//            CollectionReaderDescription lv1OutputReader = mentionDetection(devSliceReader, crfTypeModelDir,
//                    mentionLv1Output, taskConfig);

//            // Gold mentions.
//            String goldMentionOutput = middleResults + "/" + sliceSuffix + "/gold_type";
//            CollectionReaderDescription goldCandidateReader = goldCandidateAnnotation(devSliceReader,
//                    goldMentionOutput);

//            // Run realis on gold mentions.
//            CollectionReaderDescription goldMentionRealisResults = realisAnnotation(taskConfig, goldCandidateReader,
//                    realisModelDir, goldBasedRealisOutput);
//
//            // Run realis on Lv1 crf mentions.
//            CollectionReaderDescription lv1MentionRealisResults = realisAnnotation(taskConfig, lv1OutputReader,
//                    realisModelDir, lv1RealisOutput);

            // Run coreference on dev data.
            String corefOutput = edu.cmu.cs.lti.utils.FileUtils.joinPaths(middleResults, sliceSuffix, "tree_coref");
            CollectionReaderDescription corefResultsWithAverage = corefResolution(devSliceReader, taskConfig,
                    treeCorefModel, corefOutput, true);
            CollectionReaderDescription corefResultsNoAverage = corefResolution(devSliceReader, taskConfig,
                    treeCorefModel, corefOutput, false);

//            // Output final result.
//            writeResults(lv1MentionRealisResults,
//                    new File(typeLv1Eval, "lv1_mention_realis" + sliceSuffix + ".tbf").getPath(), "crf_lv1_types"
//            );
//
//            writeResults(goldMentionRealisResults,
//                    new File(goldMentionEval, "gold_mention_realis" + sliceSuffix + ".tbf").getPath(), "gold_types"
//            );

            writeResults(corefResultsWithAverage,
                    new File(corefEval, "coref_" + sliceSuffix + ".tbf").getPath(), "tree_coref"
            );

            writeResults(corefResultsNoAverage,
                    new File(corefEval, "coref_no_aver_" + sliceSuffix + ".tbf").getPath(), "tree_coref"
            );

            // Produce gold coreference.
            String goldCorefTbf = new File(corefEval, "gold_" + sliceSuffix + ".tbf").getPath();
            writeGold(devSliceReader, goldCorefTbf);

            // Produce gold mentions.
            String goldTbf = new File(typeLv1Eval, "gold" + sliceSuffix + ".tbf").getPath();
            writeGold(devSliceReader, goldTbf);
        }
    }

    public static void main(String argv[]) throws Exception {
        if (argv.length < 1) {
            System.err.println("Please provide one argument for the settings file.");
        }

        Configuration kbpConfig = new Configuration(argv[0]);
        Configuration commonConfig = new Configuration("settings/common.properties");

        String modelPath = commonConfig.get("edu.cmu.cs.lti.model.dir");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        String workingDir = kbpConfig.get("edu.cmu.cs.lti.working.dir");
        String goldTbf = kbpConfig.get("edu.cmu.cs.lti.gold.tbf");
        String sourceDir = kbpConfig.get("edu.cmu.cs.lti.source_text.dir");
        String tokenDir = kbpConfig.get("edu.cmu.cs.lti.token_map.dir");

        KBP2015EventTaskPipeline pipeline = new KBP2015EventTaskPipeline(typeSystemName, goldTbf, sourceDir,
                tokenDir, modelPath, workingDir);

        String preprocessBase = "preprocessed";
//        pipeline.prepare(kbpConfig, preprocessBase);
        pipeline.crossValidation(kbpConfig, preprocessBase);
        pipeline.trainAll(kbpConfig, preprocessBase);
//        pipeline.test(kbpConfig, preprocessBase);
    }
}
