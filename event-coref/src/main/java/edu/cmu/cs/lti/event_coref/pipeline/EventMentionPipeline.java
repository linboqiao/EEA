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
import edu.cmu.cs.lti.emd.annotators.structure.ArgumentExtractor;
import edu.cmu.cs.lti.emd.pipeline.CrfMentionTrainingLooper;
import edu.cmu.cs.lti.event_coref.annotators.EventCorefAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.GoldStandardEventMentionAnnotator;
import edu.cmu.cs.lti.learning.train.RealisClassifierTrainer;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.pipeline.ProcessorWrapper;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.utils.Configuration;
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
 * A pipeline structure for KBP 2015 event task of both Mention Detection and Coreference.
 * This pipeline should be easily adapted to a general pipeline by replacing the preprocessors.
 * <p>
 * Date: 8/16/15
 * Time: 4:21 PM
 *
 * @author Zhengzhong Liu
 */
public class EventMentionPipeline {
    final TypeSystemDescription typeSystemDescription;

    // Output directory.
    final String trainingWorkingDir;
    final String testingWorkingDir;

    // The directory that stores all the awesome and not-awesome models.
    final String modelDir;
    final String outputModelDir;

    // Some conventions of processing data.
    final String middleResults = "intermediate";
    final String preprocessBase = "preprocessed";
    final String evalBase = "eval";

    // When cross validation, we have auto generated suffixes for outputs. Let's make one for the full run too.
    final String fullRunSuffix = "all";

    protected final Logger logger = LoggerFactory.getLogger(getClass());


    /**
     * A constructor that take both the training and testing directory.
     *
     * @param typeSystemName     The type system to use.
     * @param modelDir           The models directory saving other NLP tools model.
     * @param modelOutDir        The models directory saving the output models.
     * @param trainingWorkingDir The main working directory of the training data.
     * @param testingWorkingDir  The main working directory of the testing data.
     */
    public EventMentionPipeline(String typeSystemName, String modelDir, String modelOutDir,
                                String trainingWorkingDir, String testingWorkingDir) {
        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);
        this.modelDir = modelDir;
        this.outputModelDir = modelOutDir;

        this.trainingWorkingDir = trainingWorkingDir;
        this.testingWorkingDir = testingWorkingDir;

        logger.info(String.format("Training directory will be %s.", trainingWorkingDir));
        if (testingWorkingDir != null && new File(testingWorkingDir).exists()) {
            logger.info(String.format("Testing directory will be %s.", testingWorkingDir));
        }

        logger.info(String.format("Models can be found in %s.", modelOutDir));
    }

    /**
     * A constructor that only take the training directory.
     *
     * @param typeSystemName The type system to use.
     * @param modelDir       The models directory saving other NLP tools model.
     * @param modelOutDir    The models directory saving the output models.
     * @param trainingDir    The main working directory of the training data.
     */
    public EventMentionPipeline(String typeSystemName, String modelDir, String modelOutDir, String trainingDir) {
        this(typeSystemName, modelDir, modelOutDir, trainingDir, null);
    }

    /**
     * Run cross validation on regression.
     *
     * @param config Configuration file.
     * @throws Exception
     */
    public void regression(Configuration config) throws Exception {
        String regressionDir = config.get("edu.cmu.cs.lti.regression.dir");

        prepare(config, trainingWorkingDir,
                joinPaths(regressionDir, "train", "train.tbf"),
                joinPaths(regressionDir, "train", "source"),
                joinPaths(regressionDir, "train", "tkn")
        );

        prepare(config, testingWorkingDir,
                joinPaths(regressionDir, "test", "test.tbf"),
                joinPaths(regressionDir, "test", "source"),
                joinPaths(regressionDir, "test", "tkn")
        );

        trainAll(config, false, false, false);
        test(config);
    }

    /**
     * Run preprocessing for training and testing directory.
     *
     * @param taskConfig The configuration file.
     * @throws IOException
     * @throws UIMAException
     */
    public void prepare(Configuration taskConfig) throws IOException, UIMAException {
        prepare(taskConfig, trainingWorkingDir,
                taskConfig.get("edu.cmu.cs.lti.training.gold.tbf"),
                taskConfig.get("edu.cmu.cs.lti.training.source_text.dir"),
                taskConfig.get("edu.cmu.cs.lti.training.token_map.dir"));

        if (testingWorkingDir != null) {
            prepare(taskConfig, testingWorkingDir,
                    // Simply do not supply this if the gold standard for testing doesn't exists.
                    taskConfig.get("edu.cmu.cs.lti.test.gold.tbf"),
                    taskConfig.get("edu.cmu.cs.lti.test.source_text.dir"),
                    taskConfig.get("edu.cmu.cs.lti.test.token_map.dir"));
        }
    }

    /**
     * Run major preprocessing steps for all the downstream tasks.
     *
     * @param taskConfig       The main configuration file.
     * @param workingDirPath   The working directory where data are read from and written to.
     * @param goldStandardPath The gold standard file in tbf format.
     * @param plainTextPath    The directory that stores the plain text.
     * @param tokenMapPath     The directory that stores the token maps.
     * @throws UIMAException
     * @throws IOException
     */
    public void prepare(Configuration taskConfig, String workingDirPath, String goldStandardPath,
                        String plainTextPath, String tokenMapPath) throws
            UIMAException, IOException {
        if (workingDirPath == null) {
            logger.info("Working directory not provided, not running");
            return;
        }

        File workingDir = new File(workingDirPath);
        if (!workingDir.exists()) {
            logger.info("Created directory for preprocessing : " + workingDirPath);
            workingDir.mkdirs();
        }

        File preprocessDir = new File(workingDirPath, preprocessBase);

        if (preprocessDir.exists()) {
            logger.info("Preprocessed data exists, not running.");
            return;
        } else {
            logger.info(String.format("Starting pre-processing at %s.", workingDirPath));
        }

        final String semaforModelDirectory = modelDir + "/semafor_malt_model_20121129";
        final String fanseModelDirectory = modelDir + "/fanse_models";
        final String opennlpDirectory = modelDir + "/opennlp/en-chunker.bin";

        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return CollectionReaderFactory.createReaderDescription(
                        TbfEventDataReader.class, typeSystemDescription,
                        TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, goldStandardPath,
                        TbfEventDataReader.PARAM_SOURCE_EXT, ".txt",
                        TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY, plainTextPath,
                        TbfEventDataReader.PARAM_TOKEN_DIRECTORY, tokenMapPath,
                        TbfEventDataReader.PARAM_TOKEN_EXT, ".tab",
                        TbfEventDataReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
                );
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
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
                        WordNetBasedEntityAnnotator.PARAM_WN_PATH,
                        taskConfig.get("edu.cmu.cs.lti.wndict.path")
                );

                return new AnalysisEngineDescription[]{
                        stanfordAnalyzer, semaforAnalyzer, fanseParser, opennlp, quoteAnnotator, wordNetEntityAnnotator
                };
            }
        }).runWithOutput(workingDirPath, preprocessBase);
    }

    public String trainMentionTypeLv1(Configuration config, CollectionReaderDescription trainingReader, String suffix,
                                      boolean skipTrain) throws UIMAException, IOException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        logger.info("Starting Training ...");

        String modelPath = joinPaths(outputModelDir, config.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir"), suffix);
        File modelFile = new File(modelPath);

        // Only skip training when model directory exists.
        if (skipTrain && modelFile.exists()) {
            logger.info("Skipping mention type training, taking existing models.");
        } else {
            File cacheDir = new File(joinPaths(trainingWorkingDir, config.get("edu.cmu.cs.lti.mention.cache.base")));

            CrfMentionTrainingLooper mentionTypeTrainer = new CrfMentionTrainingLooper(config, modelPath,
                    typeSystemDescription, trainingReader, cacheDir);
            mentionTypeTrainer.runLoopPipeline();
        }

        return modelPath;
    }

    private AnalysisEngineDescription getGoldAnnotator(boolean copyType, boolean copyRealis, boolean copyCluster)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS,
                new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName},
                GoldStandardEventMentionAnnotator.PARAM_COPY_MENTION_TYPE, copyType,
                GoldStandardEventMentionAnnotator.PARAM_COPY_REALIS, copyRealis,
                GoldStandardEventMentionAnnotator.PARAM_COPY_CLUSTER, copyCluster
        );
    }

    public CollectionReaderDescription goldMentionAnnotator(CollectionReaderDescription reader, String mainDir,
                                                            String baseOutput, boolean copyType, boolean copyRealis,
                                                            boolean copyCluster) throws UIMAException, IOException {
        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                return new AnalysisEngineDescription[]{getGoldAnnotator(copyType, copyRealis, copyCluster)};
            }
        }).runWithOutput(mainDir, baseOutput);
        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, baseOutput);
    }

    public CollectionReaderDescription crfMentionDetection(Configuration taskConfig, CollectionReaderDescription reader,
                                                           String modelDir, String mainDir, String baseOutput)
            throws UIMAException, IOException {

        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription crfLevel1Annotator = AnalysisEngineFactory.createEngineDescription(
                        CrfMentionTypeAnnotator.class, typeSystemDescription,
                        CrfMentionTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                        CrfMentionTypeAnnotator.PARAM_CONFIG, taskConfig.getConfigFile().getPath()
                );

                AnalysisEngineDescription everythingAcceptor = AnalysisEngineFactory.createEngineDescription(
                        AllCandidateAcceptor.class, typeSystemDescription
                );
                return new AnalysisEngineDescription[]{crfLevel1Annotator, everythingAcceptor};
            }
        }).runWithOutput(mainDir, baseOutput);

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, baseOutput);
    }

    public String trainRealisTypes(Configuration kbpConfig, CollectionReaderDescription trainingReader,
                                   String suffix, boolean skipTrain) throws Exception {
        String realisCvModelDir = joinPaths(outputModelDir, kbpConfig.get("edu.cmu.cs.lti.model.realis.dir"), suffix);

        if (skipTrain && new File(realisCvModelDir).exists()) {
            logger.info("Skipping realis training, taking existing models.");
        } else {
            RealisClassifierTrainer trainer = new RealisClassifierTrainer(typeSystemDescription, trainingReader,
                    kbpConfig);
            trainer.buildModels(realisCvModelDir);
        }

        return realisCvModelDir;
    }

    public CollectionReaderDescription realisAnnotation(Configuration config, CollectionReaderDescription reader,
                                                        String modelDir, String mainDir, String realisOutputBase)
            throws IOException, UIMAException {
        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription realisAnnotator = AnalysisEngineFactory.createEngineDescription(
                        RealisTypeAnnotator.class, typeSystemDescription,
                        RealisTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                        RealisTypeAnnotator.PARAM_CONFIG_PATH, config.getConfigFile(),
                        RealisTypeAnnotator.PARAM_FEATURE_PACKAGE_NAME,
                        config.get("edu.cmu.cs.lti.feature.sentence.package.name")
                );
                return new AnalysisEngineDescription[]{realisAnnotator};
            }
        }).runWithOutput(mainDir, realisOutputBase);

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, realisOutputBase);
    }

    private CollectionReaderDescription prepareCorefTraining(CollectionReaderDescription reader, String workingDir,
                                                             String outputBase, int seed)
            throws UIMAException, IOException {
        if (!new File(workingDir, outputBase).exists()) {
            new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription mentionAndCorefGoldAnnotator = getGoldAnnotator(true, true, true);
                    AnalysisEngineDescription argumentExtractor = AnalysisEngineFactory.createEngineDescription(
                            ArgumentExtractor.class, typeSystemDescription
                    );
                    return new AnalysisEngineDescription[]{mentionAndCorefGoldAnnotator, argumentExtractor};
                }
            }).runWithOutput(workingDir, outputBase);
        }

        return CustomCollectionReaderFactory.createRandomizedXmiReader(typeSystemDescription, workingDir, outputBase,
                seed);
    }

    /**
     * Train the latent tree model coreference resolver.
     *
     * @param config         The configuration file.
     * @param trainingReader Reader for the training data.
     * @param suffix         The suffix for the model.
     * @return The trained model directory.
     */
    private String trainLatentTreeCoref(Configuration config, CollectionReaderDescription trainingReader,
                                        String suffix, boolean skipTrain) throws UIMAException, IOException {
        logger.info("Start coreference training.");
        String cvModelDir = joinPaths(outputModelDir, config.get("edu.cmu.cs.lti.model.event.latent_tree"), suffix);
        int seed = config.getInt("edu.cmu.cs.lti.random.seed", 17);

        boolean modelExists = new File(cvModelDir).exists();

        if (skipTrain && modelExists) {
            logger.info("Skipping coreference training, taking existing models.");
        } else {
            logger.info("Prepare data for coreference training");
            CollectionReaderDescription trainingAnnotatedReader = prepareCorefTraining(trainingReader,
                    trainingWorkingDir, joinPaths(middleResults, suffix, "coref_training"), seed);
            logger.info("Saving model directory at : " + cvModelDir);

            String cacheDir = edu.cmu.cs.lti.utils.FileUtils.joinPaths(trainingWorkingDir,
                    config.get("edu.cmu.cs.lti.coref.cache.base"));

            LatentTreeTrainingLooper corefTrainer = new LatentTreeTrainingLooper(config, cvModelDir, cacheDir,
                    typeSystemDescription, trainingAnnotatedReader);
            corefTrainer.runLoopPipeline();
            logger.info("Coreference training finished ...");
        }
        return cvModelDir;
    }

    private CollectionReaderDescription corefResolution(Configuration config, CollectionReaderDescription reader,
                                                        String modelDir, String mainDir, String outputBase,
                                                        boolean useAverage) throws UIMAException, IOException {
        logger.info("Running coreference resolution, output at " + outputBase);
        logger.info("Runing with average ? " + useAverage);

        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription argumentExtractor = AnalysisEngineFactory.createEngineDescription(
                        ArgumentExtractor.class, typeSystemDescription
                );

                AnalysisEngineDescription corefAnnotator = AnalysisEngineFactory.createEngineDescription(
                        EventCorefAnnotator.class, typeSystemDescription,
                        EventCorefAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                        EventCorefAnnotator.PARAM_CONFIG_PATH, config.getConfigFile(),
                        EventCorefAnnotator.PARAM_USE_AVERAGE, useAverage
                );
                return new AnalysisEngineDescription[]{argumentExtractor, corefAnnotator};
            }
        }).runWithOutput(mainDir, outputBase);

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, outputBase);
    }

    // TODO joint inference of mention and detection.
    public void joinMentionDetectionAndCoreference() {

    }

    public void writeResults(CollectionReaderDescription processedResultReader, String tbfOutput, String systemId)
            throws UIMAException, IOException {
        logger.info("Writing results to " + tbfOutput);

        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return processedResultReader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                        TbfStyleEventWriter.class, typeSystemDescription,
                        TbfStyleEventWriter.PARAM_OUTPUT_PATH, tbfOutput,
                        TbfStyleEventWriter.PARAM_SYSTEM_ID, systemId,
                        TbfStyleEventWriter.PARAM_GOLD_TOKEN_COMPONENT_ID, TbfEventDataReader.COMPONENT_ID
                );

                return new AnalysisEngineDescription[]{resultWriter};
            }
        }).run();
    }

    public void writeGold(CollectionReaderDescription reader, String goldTbfOutput) throws UIMAException, IOException {
        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription goldCopier = getGoldAnnotator(true, true, true);
                AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                        TbfStyleEventWriter.class, typeSystemDescription,
                        TbfStyleEventWriter.PARAM_OUTPUT_PATH, goldTbfOutput,
                        TbfStyleEventWriter.PARAM_SYSTEM_ID, "gold"
                );
                return new AnalysisEngineDescription[]{goldCopier, resultWriter};
            }
        }).run();
    }

    public void trainAll(Configuration config, boolean skipTypeTrain, boolean skipRealisTrain, boolean skipCorefTrain)
            throws Exception {
        logger.info("Staring training a full model on all data in " + preprocessBase);
        CollectionReaderDescription trainingReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, preprocessBase);
        trainMentionTypeLv1(config, trainingReader, fullRunSuffix, skipTypeTrain);
        trainRealisTypes(config, trainingReader, fullRunSuffix, skipRealisTrain);
        trainLatentTreeCoref(config, trainingReader, fullRunSuffix, skipCorefTrain);
        logger.info("All training done.");
    }

    /**
     * Run a test, with all the intermediate results retained.
     *
     * @param testConfig The test configuration file.
     * @throws UIMAException
     * @throws IOException
     */
    public void test(Configuration testConfig) throws UIMAException, IOException {
        CollectionReaderDescription testDataReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, testingWorkingDir, preprocessBase);

        String crfTypeModel = joinPaths(outputModelDir,
                testConfig.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir"), fullRunSuffix);
        String mentionLv1Output = joinPaths(middleResults, fullRunSuffix, "mention_lv1");

        logger.info("Going to run mention type on [" + testingWorkingDir + "/"
                + preprocessBase + "], output will be at " + mentionLv1Output);
        CollectionReaderDescription lv1OutputReader = crfMentionDetection(testConfig, testDataReader, crfTypeModel,
                testingWorkingDir, mentionLv1Output);

        // Run realis on Lv1 crf mentions.
        String realisModel = joinPaths(outputModelDir,
                testConfig.get("edu.cmu.cs.lti.model.realis.dir"), fullRunSuffix);
        String lv1RealisOutput = joinPaths(middleResults, fullRunSuffix, "lv1_realis");

        logger.info("Going to run realis classifier on " + mentionLv1Output + " output will be at " + lv1RealisOutput);
        CollectionReaderDescription lv1MentionRealisResults = realisAnnotation(testConfig, lv1OutputReader,
                realisModel, testingWorkingDir, lv1RealisOutput);

        // Run gold standard mention detection.
        String goldMentionOutput = joinPaths(middleResults, fullRunSuffix, "gold");
        CollectionReaderDescription goldMentions = goldMentionAnnotator(testDataReader, testingWorkingDir,
                goldMentionOutput, true, true, false /* copy type, realis, not coref*/);

        // Run coreference.
        String corefModel = joinPaths(outputModelDir,
                testConfig.get("edu.cmu.cs.lti.model.event.latent_tree"), fullRunSuffix);
        String fullResultOutput = joinPaths(middleResults, fullRunSuffix, "lv1_realis_coref");
        String goldCorefOutput = joinPaths(middleResults, fullRunSuffix, "gold_coref");

        logger.info("Going to run coreference on [" + lv1RealisOutput + "], output will be at " + fullResultOutput);
        CollectionReaderDescription fullResults = corefResolution(testConfig, lv1MentionRealisResults,
                corefModel, testingWorkingDir, fullResultOutput, true);
        CollectionReaderDescription goldBasedCoref = corefResolution(testConfig, goldMentions, corefModel,
                testingWorkingDir, goldCorefOutput, true);

        // Output final result.
        String evalDir = joinPaths(testingWorkingDir, evalBase, "full_run");
        writeResults(fullResults, joinPaths(evalDir, "lv1_realis_coref_" + fullRunSuffix + ".tbf"), "sys-coref");
        writeResults(goldBasedCoref, joinPaths(evalDir, "gold_coref_" + fullRunSuffix + ".tbf"), "gold-coref");
    }

    /**
     * Run a test from end to end. That is, assuming all models given, run test starting from raw text, and doesn't
     * produce intermediate output.
     *
     * @param reader     Collection reader for the input dataset.
     * @param testConfig Configuration file for the test.
     */
    public void endToEndTest(Configuration testConfig, CollectionReaderDescription reader)
            throws UIMAException, IOException {
        String crfTypeModel = joinPaths(outputModelDir,
                testConfig.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir"), fullRunSuffix);

        String realisModel = joinPaths(outputModelDir,
                testConfig.get("edu.cmu.cs.lti.model.realis.dir"), fullRunSuffix);

        String corefModel = joinPaths(outputModelDir,
                testConfig.get("edu.cmu.cs.lti.model.event.latent_tree"), fullRunSuffix);

        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription crfLevel1Annotator = AnalysisEngineFactory.createEngineDescription(
                        CrfMentionTypeAnnotator.class, typeSystemDescription,
                        CrfMentionTypeAnnotator.PARAM_MODEL_DIRECTORY, crfTypeModel,
                        CrfMentionTypeAnnotator.PARAM_CONFIG, testConfig.getConfigFile()
                );

                AnalysisEngineDescription everythingAcceptor = AnalysisEngineFactory.createEngineDescription(
                        AllCandidateAcceptor.class, typeSystemDescription
                );

                AnalysisEngineDescription realisAnnotator = AnalysisEngineFactory.createEngineDescription(
                        RealisTypeAnnotator.class, typeSystemDescription,
                        RealisTypeAnnotator.PARAM_MODEL_DIRECTORY, realisModel,
                        RealisTypeAnnotator.PARAM_CONFIG_PATH, testConfig.getConfigFile(),
                        RealisTypeAnnotator.PARAM_FEATURE_PACKAGE_NAME,
                        testConfig.get("edu.cmu.cs.lti.feature.sentence.package.name")
                );

                AnalysisEngineDescription argumentExtractor = AnalysisEngineFactory.createEngineDescription(
                        ArgumentExtractor.class, typeSystemDescription
                );

                AnalysisEngineDescription corefAnnotator = AnalysisEngineFactory.createEngineDescription(
                        EventCorefAnnotator.class, typeSystemDescription,
                        EventCorefAnnotator.PARAM_MODEL_DIRECTORY, corefModel,
                        EventCorefAnnotator.PARAM_CONFIG_PATH, testConfig.getConfigFile(),
                        EventCorefAnnotator.PARAM_USE_AVERAGE, true
                );
                return new AnalysisEngineDescription[]{crfLevel1Annotator, everythingAcceptor, realisAnnotator,
                        argumentExtractor, corefAnnotator};
            }
        }).run();
    }

    private String joinPaths(String... segments) {
        return edu.cmu.cs.lti.utils.FileUtils.joinPaths(segments);
    }

    public void crossValidation(Configuration taskConfig) throws Exception {
        int numSplit = taskConfig.getInt("edu.cmu.cs.lti.cv.split", 5);
        int seed = taskConfig.getInt("edu.cmu.cs.lti.random.seed", 17);

        String corefEval = joinPaths(trainingWorkingDir, evalBase, "tree_coref");
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(corefEval);

        boolean skipCorefTrain = taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptrain", false);
        boolean skipTypeTrain = taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);
        boolean skipRealisTrain = taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain=true", false);

        for (int slice = 0; slice < numSplit; slice++) {
            logger.info("Starting CV split " + slice);

            String sliceSuffix = "split_" + slice;

            CollectionReaderDescription trainingSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, trainingWorkingDir, preprocessBase, false, seed, slice);
            CollectionReaderDescription devSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, trainingWorkingDir, preprocessBase, true, seed, slice);

            // Train lv1 of the mention type model.
            String crfTypeModelDir = trainMentionTypeLv1(taskConfig, trainingSliceReader, sliceSuffix, skipTypeTrain);

            // Train realis model.
            String realisModelDir = trainRealisTypes(taskConfig, trainingSliceReader, sliceSuffix, skipRealisTrain);

            // Train coref model.
            String treeCorefModel = trainLatentTreeCoref(taskConfig, trainingSliceReader, sliceSuffix, skipCorefTrain);

            // Mention types from the crf model.
            CollectionReaderDescription lv1OutputReader = crfMentionDetection(taskConfig, devSliceReader,
                    crfTypeModelDir, trainingWorkingDir, joinPaths(middleResults, sliceSuffix, "mention_lv1"));

            // Gold mention types.
            CollectionReaderDescription goldMentionTypes = goldMentionAnnotator(devSliceReader, trainingWorkingDir,
                    joinPaths(middleResults, sliceSuffix, "gold_type"), true, false, false);

            // Run realis on gold types.
            CollectionReaderDescription goldMentionRealis = realisAnnotation(taskConfig, goldMentionTypes,
                    realisModelDir, trainingWorkingDir, joinPaths(middleResults, sliceSuffix, "gold_realis"));

            // Run realis on Lv1 crf mentions.
            CollectionReaderDescription lv1MentionRealis = realisAnnotation(taskConfig, lv1OutputReader,
                    realisModelDir, trainingWorkingDir, joinPaths(middleResults, sliceSuffix, "lv1_realis"));

            // Run gold mention detection.
            CollectionReaderDescription goldMentionAll = goldMentionAnnotator(devSliceReader, trainingWorkingDir,
                    joinPaths(middleResults, sliceSuffix, "gold_mentions"), true, true, false);

            // Run coreference on dev data.
            String coref_gold = joinPaths(middleResults, sliceSuffix, "coref_gold_mention");
            String coref_gold_no_aver = joinPaths(middleResults, sliceSuffix, "coref_gold_mention_no_aver");
            String coref_system = joinPaths(middleResults, sliceSuffix, "coref_system_mention");
            String coref_system_no_aver = joinPaths(middleResults, sliceSuffix, "coref_system_mention_no_aver");

            CollectionReaderDescription corefOnGold = corefResolution(taskConfig, goldMentionAll,
                    treeCorefModel, trainingWorkingDir, coref_gold, true);
            CollectionReaderDescription corefOnGoldNoAver = corefResolution(taskConfig, goldMentionAll,
                    treeCorefModel, trainingWorkingDir, coref_gold_no_aver, false);
            CollectionReaderDescription corefSystem = corefResolution(taskConfig, lv1MentionRealis,
                    treeCorefModel, trainingWorkingDir, coref_system, true);
            CollectionReaderDescription corefSystemNoAver = corefResolution(taskConfig, lv1MentionRealis,
                    treeCorefModel, trainingWorkingDir, coref_system_no_aver, false);

            // Output final result.
            String mentionEval = joinPaths(trainingWorkingDir, evalBase, "mention_only");
            writeResults(goldMentionRealis,
                    joinPaths(mentionEval, "gold_mention_realis_" + sliceSuffix + ".tbf"), "gold_types"
            );

            // Produce gold mentions for easy evaluation.
            writeGold(devSliceReader, joinPaths(mentionEval, "gold_" + sliceSuffix + ".tbf"));

            writeResults(corefOnGold, joinPaths(corefEval, "gold_coref_" + sliceSuffix + ".tbf"), "tree_coref");
            writeResults(corefOnGoldNoAver, joinPaths(corefEval, "gold_coref_f_" + sliceSuffix + ".tbf"), "tree_coref");

            writeResults(corefSystem, joinPaths(corefEval, "lv1_coref_" + sliceSuffix + ".tbf"), "tree_coref");
            writeResults(corefSystemNoAver, joinPaths(corefEval, "lv1_coref_f_" + sliceSuffix + ".tbf"), "tree_coref");

            // Produce gold coreference for easy evaluation.
            writeGold(devSliceReader, joinPaths(corefEval, "gold_" + sliceSuffix + ".tbf"));

            // TODO temp return for running only one cross validation, for the matter of fast experiments.
            return;
        }
    }
}
