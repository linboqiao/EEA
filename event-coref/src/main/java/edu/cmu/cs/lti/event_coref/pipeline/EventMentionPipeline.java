package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.annotator.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.annotators.OpenNlpChunker;
import edu.cmu.cs.lti.annotators.QuoteAnnotator;
import edu.cmu.cs.lti.annotators.WordNetBasedEntityAnnotator;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.BeamTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.CrfMentionTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.MentionSequenceAnnotator;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.emd.annotators.classification.RealisTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.postprocessors.MentionTypeSplitter;
import edu.cmu.cs.lti.emd.annotators.train.BeamBasedMentionTypeTrainer;
import edu.cmu.cs.lti.emd.annotators.train.MentionLevelEventMentionCrfTrainer;
import edu.cmu.cs.lti.emd.annotators.train.TokenLevelEventMentionCrfTrainer;
import edu.cmu.cs.lti.emd.pipeline.TrainingLooper;
import edu.cmu.cs.lti.event_coref.annotators.*;
import edu.cmu.cs.lti.event_coref.annotators.misc.DifferentTypeCorefCollector;
import edu.cmu.cs.lti.event_coref.annotators.misc.GoldRemover;
import edu.cmu.cs.lti.event_coref.annotators.postprocessors.MentionTypeAncClusterSplitter;
import edu.cmu.cs.lti.event_coref.annotators.prepare.ArgumentMerger;
import edu.cmu.cs.lti.event_coref.annotators.prepare.EnglishSrlArgumentExtractor;
import edu.cmu.cs.lti.event_coref.annotators.prepare.EventHeadWordAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.train.BeamBasedCorefTrainer;
import edu.cmu.cs.lti.event_coref.annotators.train.BeamJointTrainer;
import edu.cmu.cs.lti.event_coref.annotators.train.PaLatentTreeTrainer;
import edu.cmu.cs.lti.exceptions.ConfigurationException;
import edu.cmu.cs.lti.learning.train.RealisClassifierTrainer;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.pipeline.ProcessorWrapper;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.reader.RandomizedXmiCollectionReader;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
    final private String trainingWorkingDir;
    final private String testingWorkingDir;

    // The directory that stores all the awesome and not-awesome models.
    final private String generalModelDir;
    final private String eventModelDir;

    // Some conventions of processing data.
    final private String middleResults = "intermediate";
    final private String preprocessBase = "preprocessed";
    final private String evalBase = "eval";

    // When cross validation, we have auto generated suffixes for outputs. Let's make one for the full run too.
    final static String fullRunSuffix = "all";

    final private String language;

    private boolean useCharOffset;

    private String tokenDir;

    private String evalLogOutputDir;

    private String evalScript;

    private boolean doEval;

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
    private EventMentionPipeline(String typeSystemName, String language, boolean useCharOffset, String modelDir,
                                 String modelOutDir, String trainingWorkingDir, String testingWorkingDir) {
        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);
        this.generalModelDir = modelDir;
        this.eventModelDir = modelOutDir;

        this.trainingWorkingDir = trainingWorkingDir;
        this.testingWorkingDir = testingWorkingDir;

        if (trainingWorkingDir != null) {
            logger.info(String.format("Training directory will be %s.", trainingWorkingDir));
        }

        if (testingWorkingDir != null && new File(testingWorkingDir).exists()) {
            logger.info(String.format("Testing directory will be %s.", testingWorkingDir));
        }

        logger.info(String.format("Models can be found in %s.", modelOutDir));

        this.language = language;
        this.useCharOffset = useCharOffset;

    }


    /**
     * A constructor that only take the training directory.
     *
     * @param typeSystemName The type system to use.
     * @param config         Configuration file.
     * @param doEval
     */
    public EventMentionPipeline(String typeSystemName, Configuration config, boolean doEval) {
        this(typeSystemName, config.getOrElse("edu.cmu.cs.lti.language", "en"),
                config.getBoolean("edu.cmu.cs.lti.output.character.offset", false),
                config.get("edu.cmu.cs.lti.model.dir"),
                config.get("edu.cmu.cs.lti.model.event.dir"), config.get("edu.cmu.cs.lti.training.working.dir"),
                config.get("edu.cmu.cs.lti.test.working.dir")
        );
        this.tokenDir = config.get("edu.cmu.cs.lti.training.token_map.dir");
        this.evalLogOutputDir = config.get("edu.cmu.cs.lti.eval.log_dir");
        this.evalScript = config.get("edu.cmu.cs.lti.eval.script");
        this.doEval = doEval;
    }

    /**
     * Run cross validation on regression.
     *
     * @param config Configuration file.
     * @throws Exception
     */
    public void regression(Configuration config, CollectionReaderDescription... inputReaders) throws Exception {
        if (trainingWorkingDir != null) {
            prepare(config, inputReaders, trainingWorkingDir, false);
        }

        if (testingWorkingDir != null) {
            prepare(config, inputReaders, testingWorkingDir, false);
        }

        trainTest(config, true);
    }

    /**
     * Run preprocessing for training and testing directory.
     *
     * @param taskConfig The configuration file.
     * @throws IOException
     * @throws UIMAException
     */
    public void prepare(Configuration taskConfig, CollectionReaderDescription... readers) throws IOException,
            UIMAException, CpeDescriptorException, SAXException {
        boolean skipTrainPrepare = taskConfig.getBoolean("edu.cmu.cs.lti.train.skip.preprocess", false);
        boolean skipTestPrepare = taskConfig.getBoolean("edu.cmu.cs.lti.test.skip.preprocess", false);

        if (trainingWorkingDir != null) {
            prepare(taskConfig, readers, trainingWorkingDir, skipTrainPrepare);
        }

        if (testingWorkingDir != null) {
            prepare(taskConfig, readers, testingWorkingDir, skipTestPrepare);
        }
    }

    /**
     * Run major preprocessing steps for all the downstream tasks.
     *
     * @param taskConfig     The main configuration file.
     * @param workingDirPath The working directory where data are read from and written to.
     * @param skipIfExists   Skip preprocessing if data exists.
     * @throws UIMAException
     * @throws IOException
     */
    public void prepare(Configuration taskConfig, CollectionReaderDescription[] inputReaders, String workingDirPath,
                        boolean skipIfExists) throws
            UIMAException, IOException, CpeDescriptorException, SAXException {
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

        if (skipIfExists && preprocessDir.exists()) {
            logger.info("Preprocessed data exists, not running.");
            return;
        } else {
            logger.info(String.format("Starting pre-processing at %s.", workingDirPath));
        }

        List<String> preprocessorNames = validatePreprocessors(taskConfig.getList("edu.cmu.cs.lti.preprocessors"));

        final String semaforModelDirectory = generalModelDir + "/semafor_malt_model_20121129";
        final String fanseModelDirectory = generalModelDir + "/fanse_models";
        final String opennlpDirectory = generalModelDir + "/opennlp/en-chunker.bin";

        for (CollectionReaderDescription reader : inputReaders) {
            new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription[] preprocessors = new AnalysisEngineDescription[preprocessorNames.size()];

                    for (int i = 0; i < preprocessorNames.size(); i++) {
                        String name = preprocessorNames.get(i);
                        AnalysisEngineDescription processor;

                        if (name.equals("corenlp")) {
                            processor = AnalysisEngineFactory.createEngineDescription(
                                    StanfordCoreNlpAnnotator.class, typeSystemDescription,
                                    StanfordCoreNlpAnnotator.PARAM_LANGUAGE, language
                            );
                        } else if (name.equals("semafor")) {
                            processor = AnalysisEngineFactory.createEngineDescription(
                                    SemaforAnnotator.class, typeSystemDescription,
                                    SemaforAnnotator.SEMAFOR_MODEL_PATH, semaforModelDirectory);
                        } else if (name.equals("fanse")) {
                            processor = AnalysisEngineFactory.createEngineDescription(
                                    FanseAnnotator.class, typeSystemDescription,
                                    FanseAnnotator.PARAM_MODEL_BASE_DIR, fanseModelDirectory);
                        } else if (name.equals("opennlp")) {
                            processor = AnalysisEngineFactory.createEngineDescription(
                                    OpenNlpChunker.class, typeSystemDescription,
                                    OpenNlpChunker.PARAM_MODEL_PATH, opennlpDirectory);
                        } else if (name.equals("wordnetEntity")) {
                            processor = AnalysisEngineFactory.createEngineDescription(
                                    WordNetBasedEntityAnnotator.class, typeSystemDescription,
                                    WordNetBasedEntityAnnotator.PARAM_WN_PATH,
                                    FileUtils.joinPaths(taskConfig.get("edu.cmu.cs.lti.resource.dir"),
                                            taskConfig.get("edu.cmu.cs.lti.wndict.path"))
                            );
                        } else if (name.equals("quote")) {
                            processor = AnalysisEngineFactory.createEngineDescription(
                                    QuoteAnnotator.class, typeSystemDescription
                            );
                        } else if (name.equals("ArgumentMerger")) {
                            processor = AnalysisEngineFactory.createEngineDescription(ArgumentMerger.class,
                                    typeSystemDescription);
                        } else {
                            throw new ConfigurationException("Unknown preprocessor specified : " + name);
                        }

                        logger.info("Adding preprocessor " + name);

                        preprocessors[i] = processor;
                    }
                    return preprocessors;
                }
            }, workingDirPath, preprocessBase).runWithOutput();
        }
    }

    private List<String> validatePreprocessors(String[] preprocessorNames) {
        // Also retain the processing order.
        Set<String> uniqueNames = new LinkedHashSet<>();

        for (String name : preprocessorNames) {
            uniqueNames.add(name);
        }

        if (!uniqueNames.contains("corenlp")) {
            throw new ConfigurationException("Preprocessor [corenlp] is mandatory.");
        } else {
            uniqueNames.remove("corenlp");
        }

        List<String> processors = new ArrayList<>();

        processors.add("corenlp");

        for (String name : uniqueNames) {
            processors.add(name);
        }

        return processors;
    }

    private AnalysisEngineDescription getGoldAnnotator(boolean copyType, boolean copyRealis, boolean copyCluster,
                                                       boolean mergeSameSpan)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS,
                new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName},
                GoldStandardEventMentionAnnotator.PARAM_COPY_MENTION_TYPE, copyType,
                GoldStandardEventMentionAnnotator.PARAM_COPY_REALIS, copyRealis,
                GoldStandardEventMentionAnnotator.PARAM_COPY_CLUSTER, copyCluster,
                GoldStandardEventMentionAnnotator.PARAM_MERGE_SAME_SPAN, mergeSameSpan
        );
    }

    public CollectionReaderDescription annotateGoldMentions(CollectionReaderDescription reader, String mainDir,
                                                            String baseOutput, boolean copyType, boolean copyRealis,
                                                            boolean copyCluster, boolean mergeSameSpan, boolean skip)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        if (skip && new File(mainDir, baseOutput).exists()) {
            logger.info("Skipping gold annotator since exists.");
        } else {
            new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    logger.info(mainDir);
                    logger.info(baseOutput);

                    return new AnalysisEngineDescription[]{
                            getGoldAnnotator(copyType, copyRealis, copyCluster, mergeSameSpan)
                    };
                }
            }, mainDir, baseOutput).runWithOutput();
        }
        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, baseOutput);
    }

    public String trainSentLvType(Configuration config, CollectionReaderDescription trainingReader, String suffix,
                                  boolean usePaTraing, String lossType, int initialSeed, boolean skipTrain)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Starting training sentence level mention type model ...");

        String modelPath = FileUtils.joinPaths(eventModelDir, config.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir"),
                suffix);
        File modelFile = new File(modelPath);

        MutableInt trainingSeed = new MutableInt(initialSeed);

        if (usePaTraing) {
            logger.info("Use PA with loss : " + lossType);
        }

        // Only skip training when model directory exists.
        if (skipTrain && modelFile.exists()) {
            logger.info("Skipping mention type training, taking existing models.");
        } else {
            logger.info("Model file " + modelPath + " not exists, start training.");
            File cacheDir = new File(FileUtils.joinPaths(trainingWorkingDir,
                    config.get("edu.cmu.cs.lti.mention.cache.base")));

            AnalysisEngineDescription trainingEngine = AnalysisEngineFactory.createEngineDescription(
                    TokenLevelEventMentionCrfTrainer.class, typeSystemDescription,
                    TokenLevelEventMentionCrfTrainer.PARAM_GOLD_STANDARD_VIEW_NAME, UimaConst.goldViewName,
                    TokenLevelEventMentionCrfTrainer.PARAM_CONFIGURATION_FILE, config.getConfigFile(),
                    TokenLevelEventMentionCrfTrainer.PARAM_CACHE_DIRECTORY, cacheDir,
                    TokenLevelEventMentionCrfTrainer.PARAM_USE_PA_UPDATE, usePaTraing,
                    TokenLevelEventMentionCrfTrainer.PARAM_LOSS_TYPE, lossType
            );

            TrainingLooper mentionTypeTrainer = new TrainingLooper(config, modelPath,
                    trainingReader, trainingEngine) {
                @Override
                protected boolean loopActions() {
                    super.loopActions();
                    trainingSeed.add(2);
                    logger.debug("Update the training seed to " + trainingSeed.intValue());
                    trainingReader.setAttributeValue(RandomizedXmiCollectionReader.PARAM_SEED, trainingSeed.getValue());
                    return false;
                }

                @Override
                protected void finish() throws IOException {
                    TokenLevelEventMentionCrfTrainer.loopStopActions();
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    TokenLevelEventMentionCrfTrainer.saveModels(modelOutputDir, TokenLevelEventMentionCrfTrainer
                            .MODEL_NAME);
                }
            };
            mentionTypeTrainer.runLoopPipeline();
        }

        return modelPath;
    }

    public CollectionReaderDescription sentenceLevelMentionTagging(Configuration taskConfig,
                                                                   CollectionReaderDescription reader,
                                                                   String modelDir, String mainDir, String baseOutput,
                                                                   boolean skipTest)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        File outputFile = new File(mainDir, baseOutput);

        if (skipTest && outputFile.exists()) {
            logger.info("Skipping sent level tagging because output exists.");
            return CustomCollectionReaderFactory.createXmiReader(mainDir, baseOutput);
        } else {
            return new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription sentenceLevelTagger = AnalysisEngineFactory.createEngineDescription(
                            CrfMentionTypeAnnotator.class, typeSystemDescription,
                            CrfMentionTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                            CrfMentionTypeAnnotator.PARAM_CONFIG, taskConfig.getConfigFile().getPath()
                    );

                    return new AnalysisEngineDescription[]{sentenceLevelTagger};
                }
            }, mainDir, baseOutput).runWithOutput();
        }
    }


    public String trainBeamTypeModel(Configuration config, CollectionReaderDescription trainingReader, String suffix,
                                     boolean usePaTraing, String lossType, boolean useLaSO, boolean delayedLaso,
                                     int beamSize, int maxIter, Float aggressiveParameter, int initialSeed,
                                     boolean skipTrain)
            throws UIMAException, NoSuchMethodException, IOException, InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {
        logger.info("Start training beam based type model.");

        String modelPath = FileUtils.joinPaths(eventModelDir, config.get("edu.cmu.cs.lti.model.crf.mention.beam.dir"),
                suffix);

        File modelFile = new File(modelPath);

        if (usePaTraing) {
            logger.info(String.format("Will use PA with loss %s, delayed = %s.", lossType, delayedLaso));
        }


        if (skipTrain && modelFile.exists()) {
            logger.info("Skip Beam based type training.");
        } else {
            logger.info("Model file " + modelPath + " not exist, start training.");
            AnalysisEngineDescription trainer = AnalysisEngineFactory.createEngineDescription(
                    BeamBasedMentionTypeTrainer.class, typeSystemDescription,
                    BeamBasedMentionTypeTrainer.PARAM_GOLD_STANDARD_VIEW_NAME, UimaConst.goldViewName,
                    BeamBasedMentionTypeTrainer.PARAM_CONFIGURATION_FILE, config.getConfigFile(),
                    BeamBasedMentionTypeTrainer.PARAM_USE_PA_UPDATE, usePaTraing,
                    BeamBasedMentionTypeTrainer.PARAM_LOSS_TYPE, lossType,
                    BeamBasedMentionTypeTrainer.PARAM_DELAYED_LASO, delayedLaso,
                    BeamBasedMentionTypeTrainer.PARAM_BEAM_SIZE, beamSize,
                    BeamBasedMentionTypeTrainer.PARAM_USE_LASO, useLaSO,
                    BeamBasedMentionTypeTrainer.PARAM_AGGRESSIVE_PARAMETER, aggressiveParameter
            );

            MutableInt trainingSeed = new MutableInt(initialSeed);

            new TrainingLooper(modelPath, trainingReader, trainer, maxIter) {
                @Override
                protected boolean loopActions() {
                    super.loopActions();
                    trainingSeed.add(2);
                    logger.debug("Update the training seed to " + trainingSeed.intValue());
                    trainingReader.setAttributeValue(RandomizedXmiCollectionReader.PARAM_SEED, trainingSeed.getValue());
                    return false;
                }

                @Override
                protected void finish() throws IOException {
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    BeamBasedMentionTypeTrainer.saveModels(modelOutputDir);
                }
            }.runLoopPipeline();
        }
        return modelPath;
    }


    public CollectionReaderDescription beamMentionTagging(Configuration taskConfig, CollectionReaderDescription reader,
                                                          String modelDir, String mainDir, String baseOutput,
                                                          int beamSize, boolean skipTest)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        File outputFile = new File(mainDir, baseOutput);

        if (skipTest && outputFile.exists()) {
            logger.info("Skipping sent level tagging because output exists.");
            return CustomCollectionReaderFactory.createXmiReader(mainDir, baseOutput);
        } else {
            return new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription sentenceLevelTagger = AnalysisEngineFactory.createEngineDescription(
                            BeamTypeAnnotator.class, typeSystemDescription,
                            BeamTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                            BeamTypeAnnotator.PARAM_CONFIG, taskConfig.getConfigFile().getPath(),
                            BeamTypeAnnotator.PARAM_BEAM_SIZE, beamSize
                    );

                    return new AnalysisEngineDescription[]{sentenceLevelTagger};
                }
            }, mainDir, baseOutput).runWithOutput();
        }
    }


    public String trainDocLvType(Configuration config, CollectionReaderDescription trainingReader, String suffix,
                                 boolean skipTrain, int initialSeed)
            throws NoSuchMethodException, UIMAException, InstantiationException, IOException, ClassNotFoundException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Starting training document level mention type model ...");

        String modelPath = FileUtils.joinPaths(eventModelDir, config.get("edu.cmu.cs.lti.model.crf.mention.lv2.dir"),
                suffix);
        File modelFile = new File(modelPath);

        MutableInt trainingSeed = new MutableInt(initialSeed);

        // Only skip training when model directory exists.
        if (skipTrain && modelFile.exists()) {
            logger.info("Skipping second level mention type training, taking existing models.");
        } else {
            File cacheDir = new File(FileUtils.joinPaths(trainingWorkingDir, config.get("edu.cmu.cs.lti.mention.cache" +
                    ".base")));

            AnalysisEngineDescription trainingEngine = AnalysisEngineFactory.createEngineDescription(
                    MentionLevelEventMentionCrfTrainer.class, typeSystemDescription,
                    MentionLevelEventMentionCrfTrainer.PARAM_CONFIGURATION_FILE, config.getConfigFile(),
                    MentionLevelEventMentionCrfTrainer.PARAM_CACHE_DIRECTORY, cacheDir
            );

            TrainingLooper mentionTypeTrainer = new TrainingLooper(config, modelPath,
                    trainingReader, trainingEngine) {
                @Override
                protected boolean loopActions() {
                    super.loopActions();
                    return false;
                }

                @Override
                protected void finish() throws IOException {
                    MentionLevelEventMentionCrfTrainer.loopStopActions();
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    MentionLevelEventMentionCrfTrainer.saveModels(modelOutputDir, MentionLevelEventMentionCrfTrainer
                            .MODEL_NAME);
                }
            };
            mentionTypeTrainer.runLoopPipeline();
        }
        return modelPath;
    }

    public CollectionReaderDescription docLevelMentionTagging(Configuration taskConfig, CollectionReaderDescription
            reader, String modelDir, String mainDir, String baseOutput, boolean skipTest) throws UIMAException,
            IOException, CpeDescriptorException, SAXException {
        if (skipTest && new File(mainDir, baseOutput).exists()) {
            logger.info("Skipping doc level mention tagging because output exists.");
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, baseOutput);
        } else {
            return new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription documentLevelTagger = AnalysisEngineFactory.createEngineDescription(
                            MentionSequenceAnnotator.class, typeSystemDescription,
                            MentionSequenceAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                            MentionSequenceAnnotator.PARAM_CONFIG, taskConfig.getConfigFile().getPath()
                    );

                    return new AnalysisEngineDescription[]{documentLevelTagger};
                }
            }, mainDir, baseOutput).runWithOutput();
        }
    }

    public String trainRealisTypes(Configuration kbpConfig, CollectionReaderDescription trainingReader,
                                   String suffix, boolean skipTrain) throws Exception {
        String realisCvModelDir = FileUtils.joinPaths(eventModelDir, kbpConfig.get("edu.cmu.cs.lti.model.realis.dir")
                , suffix);

        if (skipTrain && new File(realisCvModelDir).exists()) {
            logger.info("Skipping realis training, taking existing models.");
        } else {
            RealisClassifierTrainer trainer = new RealisClassifierTrainer(typeSystemDescription, trainingReader,
                    kbpConfig);
            trainer.buildModels(realisCvModelDir);
        }

        return realisCvModelDir;
    }

    public CollectionReaderDescription realisAnnotation(Configuration taskConfig, CollectionReaderDescription reader,
                                                        String modelDir, String mainDir, String realisOutputBase,
                                                        boolean skipTest)
            throws IOException, UIMAException, CpeDescriptorException, SAXException {

        if (skipTest && new File(mainDir, realisOutputBase).exists()) {
            logger.info("Skipping realis detection because output exists.");
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, realisOutputBase);
        } else {
            return new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription realisAnnotator = AnalysisEngineFactory.createEngineDescription(
                            RealisTypeAnnotator.class, typeSystemDescription,
                            RealisTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                            RealisTypeAnnotator.PARAM_CONFIG_PATH, taskConfig.getConfigFile(),
                            RealisTypeAnnotator.PARAM_FEATURE_PACKAGE_NAME,
                            taskConfig.get("edu.cmu.cs.lti.feature.sentence.package.name")
                    );
                    return new AnalysisEngineDescription[]{realisAnnotator};
                }
            }, mainDir, realisOutputBase).runWithOutput();
        }
    }

    /**
     * Prepare dataset for training. It copy the annotations to the system mentions and annotate arguments.
     *
     * @param reader     The data to be prepared.
     * @param workingDir The working directory.
     * @param outputBase The base output directory.
     * @param seed       The random seed to scramble the training data.
     * @return A reader for the prepared dataset.
     * @throws UIMAException
     * @throws IOException
     */
    private CollectionReaderDescription prepareTraining(CollectionReaderDescription reader, String workingDir,
                                                        String outputBase, boolean mergeTypes, int seed)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {

        if (!new File(workingDir, outputBase).exists()) {
            new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription mentionAndCorefGoldAnnotator = getGoldAnnotator(true, true, true,
                            mergeTypes);
                    List<AnalysisEngineDescription> annotators = new ArrayList<>();
                    annotators.add(mentionAndCorefGoldAnnotator);
                    addCorefPreprocessors(annotators);
                    return annotators.toArray(new AnalysisEngineDescription[annotators.size()]);
                }
            }, workingDir, outputBase).runWithOutput();
        }

        return CustomCollectionReaderFactory.createRandomizedXmiReader(typeSystemDescription, workingDir, outputBase,
                seed);
    }

    private void addCorefPreprocessors(List<AnalysisEngineDescription> preAnnotators) throws
            ResourceInitializationException {
        AnalysisEngineDescription headWordExtractor = AnalysisEngineFactory.createEngineDescription(
                EventHeadWordAnnotator.class, typeSystemDescription
        );

        if (language.equals("zh")) {
            preAnnotators.add(headWordExtractor);
        } else {
            AnalysisEngineDescription argumentExtractor = AnalysisEngineFactory.createEngineDescription(
                    EnglishSrlArgumentExtractor.class, typeSystemDescription
            );
            preAnnotators.add(headWordExtractor);
            preAnnotators.add(argumentExtractor);
        }
    }


    public String trainBeamBasedCoref(Configuration config, CollectionReaderDescription trainingReader,
                                      String suffix, boolean skipTrain, int initialSeed, String modelDir,
                                      boolean mergeMention, boolean useLaSO, boolean delayedLaso, int beamSize) throws
            UIMAException, NoSuchMethodException, InstantiationException, IOException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        logger.info("Start beam based coreference training.");

        String cvModelDir = FileUtils.joinPaths(eventModelDir, modelDir, suffix);

        boolean modelExists = new File(cvModelDir).exists();
        if (skipTrain && modelExists) {
            logger.info("Skipping training, taking existing models.");
        } else {
            logger.info("Saving model directory at : " + cvModelDir);
            AnalysisEngineDescription corefEngine = AnalysisEngineFactory.createEngineDescription(
                    BeamBasedCorefTrainer.class, typeSystemDescription,
                    BeamBasedCorefTrainer.PARAM_CONFIGURATION_FILE, config.getConfigFile().getPath(),
                    BeamBasedCorefTrainer.PARAM_DELAYED_LASO, delayedLaso,
                    BeamBasedCorefTrainer.PARAM_MERGE_MENTION, mergeMention,
                    BeamBasedCorefTrainer.PARAM_BEAM_SIZE, beamSize,
                    BeamBasedCorefTrainer.PARAM_USE_LASO, useLaSO
            );

            TrainingLooper corefTrainer = new TrainingLooper(config, cvModelDir, trainingReader, corefEngine) {
                @Override
                protected boolean loopActions() {
                    super.loopActions();
                    return false;
                }

                @Override
                protected void finish() throws IOException {
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    BeamBasedCorefTrainer.saveModels(modelOutputDir);
                }
            };

            corefTrainer.runLoopPipeline();
        }
        return cvModelDir;
    }

    /**
     * Train the latent tree model coreference resolver.
     *
     * @param config         The configuration file.
     * @param trainingReader Reader for the training data.
     * @param suffix         The suffix for the model.
     * @param initialSeed    The initial seed for the reader.
     * @param skipTrain      Whether to skip the training if model file exists.
     * @return The trained model directory.
     */
    private String trainLatentTreeCoref(Configuration config, CollectionReaderDescription trainingReader,
                                        String suffix, String modelDir, int initialSeed, boolean skipTrain)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Start coreference training.");
        String cvModelDir = FileUtils.joinPaths(eventModelDir, modelDir, suffix);


        boolean modelExists = new File(cvModelDir).exists();
        if (skipTrain && modelExists) {
            logger.info("Skipping training, taking existing models.");
        } else {
            logger.info("Saving model directory at : " + cvModelDir);
            String cacheDir = FileUtils.joinPaths(trainingWorkingDir, config.get("edu.cmu.cs.lti.coref.cache.base"));
            AnalysisEngineDescription corefEngine = AnalysisEngineFactory.createEngineDescription(
                    PaLatentTreeTrainer.class, typeSystemDescription,
                    PaLatentTreeTrainer.PARAM_CONFIG_PATH, config.getConfigFile().getPath(),
                    PaLatentTreeTrainer.PARAM_CACHE_DIRECTORY, cacheDir
            );


            TrainingLooper corefTrainer = new TrainingLooper(config, cvModelDir, trainingReader, corefEngine) {
                @Override
                protected boolean loopActions() {
                    super.loopActions();
                    return false;
                }

                @Override
                protected void finish() throws IOException {
                    PaLatentTreeTrainer.finish();
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    PaLatentTreeTrainer.saveModels(modelOutputDir);
                }
            };
            corefTrainer.runLoopPipeline();
        }
        return cvModelDir;
    }

    private CollectionReaderDescription corefResolution(Configuration config, CollectionReaderDescription reader,
                                                        String modelDir, String mainDir, String outputBase,
                                                        boolean skipCorefTest)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        logger.info("Running coreference resolution, output at " + outputBase);
        if (skipCorefTest && new File(mainDir, outputBase).exists()) {
            logger.info("Skipping running coreference, using existing results.");
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, outputBase);
        } else {
            return new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription corefAnnotator = AnalysisEngineFactory.createEngineDescription(
                            EventCorefAnnotator.class, typeSystemDescription,
                            EventCorefAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                            EventCorefAnnotator.PARAM_CONFIG_PATH, config.getConfigFile()
                    );

                    AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                            MentionTypeSplitter.class, typeSystemDescription
                    );

                    List<AnalysisEngineDescription> annotators = new ArrayList<>();
                    addCorefPreprocessors(annotators);

                    annotators.add(mentionSplitter);
                    annotators.add(corefAnnotator);
                    return annotators.toArray(new AnalysisEngineDescription[annotators.size()]);
                }
            }, mainDir, outputBase).runWithOutput();
        }
    }

    private CollectionReaderDescription beamCorefResolution(Configuration config, CollectionReaderDescription reader,
                                                            String modelDir, String mainDir, String outputBase,
                                                            boolean mergeMentions, boolean skipCorefTest, int beamSize)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        logger.info("Running coreference resolution, output at " + outputBase);
        if (skipCorefTest && new File(mainDir, outputBase).exists()) {
            logger.info("Skipping running coreference, using existing results.");
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, outputBase);
        } else {
            return new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription corefAnnotator = AnalysisEngineFactory.createEngineDescription(
                            BeamEventCorefAnnotator.class, typeSystemDescription,
                            BeamEventCorefAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                            BeamEventCorefAnnotator.PARAM_CONFIG_PATH, config.getConfigFile(),
                            BeamEventCorefAnnotator.PARAM_MERGE_MENTION, mergeMentions,
                            BeamEventCorefAnnotator.PARAM_BEAM_SIZE, beamSize
                    );

                    AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                            MentionTypeSplitter.class, typeSystemDescription
                    );

                    List<AnalysisEngineDescription> annotators = new ArrayList<>();
                    addCorefPreprocessors(annotators);
                    annotators.add(mentionSplitter);
                    annotators.add(corefAnnotator);
                    return annotators.toArray(new AnalysisEngineDescription[annotators.size()]);
                }
            }, mainDir, outputBase).runWithOutput();
        }
    }

    public CollectionReaderDescription ddMentionDetectionAndCoreference(
            Configuration config, CollectionReaderDescription reader, String mentionModelDir, String latentTreeDir,
            File corefRuleFile, String mainDir, String outputBase, boolean skipJointTest)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        logger.info("Running joint mention detection and coreference, output at " + outputBase);

        if (skipJointTest && new File(mainDir, outputBase).exists()) {
            logger.info("Skipping running Joint, using existing results.");
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, outputBase);
        } else {
            new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription ddDecoder = AnalysisEngineFactory.createEngineDescription(
                            DDEventTypeCorefAnnotator.class, typeSystemDescription,
                            DDEventTypeCorefAnnotator.PARAM_CONFIG_PATH, config.getConfigFile(),
                            DDEventTypeCorefAnnotator.PARAM_MENTION_MODEL_DIRECTORY, mentionModelDir,
                            DDEventTypeCorefAnnotator.PARAM_COREF_MODEL_DIRECTORY, latentTreeDir,
                            DDEventTypeCorefAnnotator.PARAM_COREF_RULE_FILE, corefRuleFile
                    );

                    AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                            MentionTypeAncClusterSplitter.class, typeSystemDescription,
                            MentionTypeAncClusterSplitter.PARAM_COREF_RULE_FILE, corefRuleFile
                    );

                    List<AnalysisEngineDescription> annotators = new ArrayList<>();
                    addCorefPreprocessors(annotators);
                    annotators.add(ddDecoder);
                    annotators.add(mentionSplitter);

                    return annotators.toArray(new AnalysisEngineDescription[annotators.size()]);
                }
            }, mainDir, outputBase).runWithOutput();
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, outputBase);
        }
    }

    private void testAndEvalJoint(Configuration config, CollectionReaderDescription devReader, String modelDir,
                                  String modelSuffix, String modelName, int beamSize, String realisModelDir,
                                  boolean skipTest, String processOutputDir, String subEvalDir, String goldStandard,
                                  String outputSuffix, String sliceSuffix)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        String runName = "joint_span_coref_" + modelName + modelSuffix + "_" + outputSuffix;
        CollectionReaderDescription results = beamJointSpanCoref(config, devReader, modelDir + modelSuffix,
                realisModelDir, trainingWorkingDir, FileUtils.joinPaths(middleResults, sliceSuffix, runName),
                beamSize, true, skipTest);
        String jointTbf = FileUtils.joinPaths(processOutputDir, runName + "_" + sliceSuffix + ".tbf");
        writeResults(results, jointTbf, "joint");
        logger.info("Evaluating with " + goldStandard + " and " + jointTbf);
        eval(goldStandard, jointTbf, subEvalDir, runName, sliceSuffix);
    }

    private void evalJointModels(String processOutputDir, String subEvalDir, String goldStandard, String modelName,
                                 String sliceSuffix) throws IOException, InterruptedException {
        int[] iters = {3, 6, 9, 15};

        for (int numIteration : iters) {
            String modelSuffix = "_iter" + numIteration;
            String runName = "joint_span_coref_" + modelName + modelSuffix + "_train";
            String jointTbf = FileUtils.joinPaths(processOutputDir, runName + "_" + sliceSuffix + ".tbf");
            eval(goldStandard, jointTbf, subEvalDir, runName, sliceSuffix);
        }
    }

    public String trainJointSpanModel(Configuration config, CollectionReaderDescription trainReader,
                                      CollectionReaderDescription devReader, String mentionLossType, String modelName,
                                      boolean skipTrain, boolean skipTest, int maxIter, String realisModelDir,
                                      String modelDir, int beamSize, boolean useLaSo, boolean corefTrainingConstraint,
                                      int constraintType, String processOutputDir, String trainGold, String devGold,
                                      String subEvalDir, String sliceSuffix)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Start beam based joint training.");
        String cvModelDir = FileUtils.joinPaths(eventModelDir, modelDir, modelName);

        File cacheDir = new File(FileUtils.joinPaths(trainingWorkingDir,
                config.get("edu.cmu.cs.lti.joint.cache.base")));

        boolean modelExists = new File(cvModelDir).exists();

        if (skipTrain && modelExists) {
            logger.info("Skipping beam joint training, taking existing models.");
        } else {
            logger.info("Saving model directory at : " + cvModelDir);

            AnalysisEngineDescription trainEngine = AnalysisEngineFactory.createEngineDescription(
                    BeamJointTrainer.class, typeSystemDescription,
                    BeamJointTrainer.PARAM_CONFIG_PATH, config.getConfigFile().getPath(),
                    BeamJointTrainer.PARAM_REALIS_MODEL_DIRECTORY, realisModelDir,
                    BeamJointTrainer.PARAM_MENTION_LOSS_TYPE, mentionLossType,
                    BeamJointTrainer.PARAM_BEAM_SIZE, beamSize,
                    BeamJointTrainer.PARAM_USE_LASO, useLaSo,
                    BeamJointTrainer.PARAM_USE_WARM_START, false,
                    BeamJointTrainer.PARAM_ADD_COREFERNCE_CONSTRAINT, corefTrainingConstraint,
                    BeamJointTrainer.PARAM_STRATEGY_TYPE, constraintType,
                    BeamJointTrainer.PARAM_CACHE_DIR, cacheDir
            );

            TrainingLooper trainer = new TrainingLooper(cvModelDir, trainReader, trainEngine, maxIter) {
                @Override
                protected boolean loopActions() {
                    boolean modelSaved = super.loopActions();
                    BeamJointTrainer.loopAction();

                    if (modelSaved) {
//                        try {
//                            if (numIteration > 1) {
//                                testAndEvalJoint(config, trainReader, cvModelDir, "_iter" + numIteration, modelName,
//                                        beamSize, realisModelDir, skipTest, processOutputDir, subEvalDir, trainGold,
//                                        "train", sliceSuffix);
//                            }
//                        } catch (SAXException | InterruptedException | IOException | CpeDescriptorException |
//                                UIMAException e) {
//                            e.printStackTrace();
//                        }

                        if (numIteration > 5 && numIteration % 3 == 0) {
                            try {
                                testAndEvalJoint(config, devReader, cvModelDir, "_iter" + numIteration, modelName,
                                        beamSize, realisModelDir, skipTest, processOutputDir, subEvalDir, devGold,
                                        "dev", sliceSuffix);
                            } catch (SAXException | InterruptedException | IOException | CpeDescriptorException |
                                    UIMAException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return modelSaved;
                }

                @Override
                protected void finish() throws IOException {
                    BeamJointTrainer.finish();
                    try {
                        // Test using the final model.
                        testAndEvalJoint(config, devReader, cvModelDir, "", modelName, beamSize,
                                realisModelDir, skipTest, processOutputDir, subEvalDir, devGold, "dev", sliceSuffix);
                    } catch (SAXException | InterruptedException | IOException | CpeDescriptorException |
                            UIMAException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    BeamJointTrainer.saveModels(modelOutputDir);
                }
            };

            trainer.setNumberIterToSave(1);

            trainer.runLoopPipeline();

            logger.info("Joint training finished ...");
        }
        return cvModelDir;
    }


    private CollectionReaderDescription beamJointSpanCoref(Configuration config, CollectionReaderDescription reader,
                                                           String modelDir, String realisDir, String mainDir,
                                                           String outputBase, int beamSize, boolean useLaso,
                                                           boolean skipTest)
            throws UIMAException, SAXException, CpeDescriptorException, IOException {
        logger.info("Running joint beam mention detection and coreference, output at " + outputBase);

        if (skipTest && new File(mainDir, outputBase).exists()) {
            logger.info("Skipping running Joint beam, using existing results.");
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, outputBase);
        } else {
            new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription goldRemover = AnalysisEngineFactory.createEngineDescription(
                            GoldRemover.class, typeSystemDescription);

                    AnalysisEngineDescription jointDecoder = AnalysisEngineFactory.createEngineDescription(
                            JointMentionCorefAnnotator.class, typeSystemDescription,
                            JointMentionCorefAnnotator.PARAM_CONFIG_PATH, config.getConfigFile(),
                            JointMentionCorefAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                            JointMentionCorefAnnotator.PARAM_REALIS_MODEL_DIRECTORY, realisDir,
                            JointMentionCorefAnnotator.PARAM_BEAM_SIZE, beamSize,
                            JointMentionCorefAnnotator.PARAM_USE_LASO, useLaso
                    );

                    List<AnalysisEngineDescription> annotators = new ArrayList<>();
                    addCorefPreprocessors(annotators);
                    annotators.add(goldRemover);
                    annotators.add(jointDecoder);

                    return annotators.toArray(new AnalysisEngineDescription[annotators.size()]);
                }
            }, mainDir, outputBase).runWithOutput();
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, outputBase);
        }
    }

    private void eval(String gold, String system, String subDir, String runName, String suffix) throws IOException,
            InterruptedException {
//        ${eval_script} -g ${gold_standard} -s ${sys_out} -t ${base_dir}/tkn -d ${log_dir}/${sys_name}/cv_$i.cmp \
//        -o ${log_dir}/${sys_name}/cv_$i.scores -c ${log_dir}/${sys_name}/coref_out_$i

        String evalOutDir = FileUtils.joinPaths(evalLogOutputDir, subDir, runName);
        logger.info("Evaluating, saving results to " + evalOutDir);
        logger.info("Gold file is " + gold);
        logger.info("System file is " + system);

        ProcessBuilder pb = new ProcessBuilder("python", evalScript, "-g", gold, "-s", system, "-t", tokenDir,
                "-d", FileUtils.joinPaths(evalOutDir, suffix + ".cmp"),
                "-o", FileUtils.joinPaths(evalOutDir, suffix + ".scores"),
                "-c", FileUtils.joinPaths(evalOutDir, suffix + ".coref_out")
        ).redirectErrorStream(true);
        Process p = pb.start();

        // We can let it run at background.
//        p.waitFor();
//        logger.info("Evaluation done.");
    }

    public void writeResults(CollectionReaderDescription processedResultReader, String tbfOutput, String systemId)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
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
                        TbfStyleEventWriter.PARAM_GOLD_TOKEN_COMPONENT_ID, TbfEventDataReader.COMPONENT_ID,
                        TbfStyleEventWriter.PARAM_USE_CHARACTER_OFFSET, useCharOffset
                );
                return new AnalysisEngineDescription[]{resultWriter};
            }
        }).run();
    }

//
//    public void writeAndEval(CollectionReaderDescription processedResultReader, String tbfOutput, String systemId,
//                             String gold, String subDir, String suffix)
//            throws UIMAException, IOException, CpeDescriptorException, SAXException, InterruptedException {
//        logger.info("Writing results to " + tbfOutput);
//
//        new BasicPipeline(new ProcessorWrapper() {
//            @Override
//            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
//                return processedResultReader;
//            }
//
//            @Override
//            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
//
//                AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
//                        TbfStyleEventWriter.class, typeSystemDescription,
//                        TbfStyleEventWriter.PARAM_OUTPUT_PATH, tbfOutput,
//                        TbfStyleEventWriter.PARAM_SYSTEM_ID, systemId,
//                        TbfStyleEventWriter.PARAM_GOLD_TOKEN_COMPONENT_ID, TbfEventDataReader.COMPONENT_ID,
//                        TbfStyleEventWriter.PARAM_USE_CHARACTER_OFFSET, useCharOffset
//                );
//                return new AnalysisEngineDescription[]{resultWriter};
//            }
//        }).run();
//
//        eval(gold, tbfOutput, subDir, systemId, suffix);
//    }

    public void writeGold(CollectionReaderDescription reader, String goldTbfOutput) throws
            UIMAException, IOException, CpeDescriptorException, SAXException {
        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription goldCopier = getGoldAnnotator(true, true, true, false);
                AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                        TbfStyleEventWriter.class, typeSystemDescription,
                        TbfStyleEventWriter.PARAM_OUTPUT_PATH, goldTbfOutput,
                        TbfStyleEventWriter.PARAM_SYSTEM_ID, "gold",
                        TbfStyleEventWriter.PARAM_GOLD_TOKEN_COMPONENT_ID, TbfEventDataReader.COMPONENT_ID,
                        TbfStyleEventWriter.PARAM_USE_CHARACTER_OFFSET, useCharOffset
                );
                return new AnalysisEngineDescription[]{goldCopier, resultWriter};
            }
        }).run();
    }

    private void collectCorefConstraint(CollectionReaderDescription reader, File corefRuleFile)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        // Get type rules.
        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription collector = AnalysisEngineFactory.createEngineDescription(
                        DifferentTypeCorefCollector.class, typeSystemDescription,
                        DifferentTypeCorefCollector.PARAM_COREFERENCE_ALLOWED_TYPES, corefRuleFile
                );
                return new AnalysisEngineDescription[]{collector};
            }
        }).run();
    }

    public void runVanilla(Configuration taskConfig) throws Exception {
        boolean skipCorefTrain = taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptrain", false);
        boolean skipTypeTrain = taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);
        boolean skipRealisTrain = taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain", false);

        boolean skipTypeTest = taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptest", false);
        boolean skipRealisTest = taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptest", false);
        boolean skipCorefTest = taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptest", false);

        int seed = taskConfig.getInt("edu.cmu.cs.lti.random.seed", 17);

        CollectionReaderDescription trainReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, preprocessBase);

        CollectionReaderDescription testReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, testingWorkingDir, preprocessBase);

        String processDir = FileUtils.joinPaths(testingWorkingDir, evalBase, "full_run");

        CollectionReaderDescription trainingData = prepareTraining(trainReader,
                trainingWorkingDir, FileUtils.joinPaths(middleResults, fullRunSuffix, "prepared_training"), false,
                seed);

        // Train realis model.
        String realisModelDir = trainRealisTypes(taskConfig, trainingData, fullRunSuffix, skipRealisTrain);

        String vanillaSentCrfModel = trainSentLvType(taskConfig, trainingData, fullRunSuffix, false, "hamming", seed,
                skipTypeTrain);

        // Train coref model.
        String treeCorefModel = trainLatentTreeCoref(taskConfig, trainingData, fullRunSuffix, taskConfig.get("edu.cmu" +
                ".cs.lti.model.event.latent_tree"), seed, skipCorefTrain
        );

        // Run the vanilla model.
        runOnly(taskConfig, testReader, vanillaSentCrfModel, realisModelDir, treeCorefModel, fullRunSuffix,
                "vanillaMention", processDir, skipTypeTest, skipRealisTest, skipCorefTest);
    }

    public void trainTest(Configuration taskConfig, boolean runAll) throws Exception {
        int seed = taskConfig.getInt("edu.cmu.cs.lti.random.seed", 17);

        CollectionReaderDescription trainingReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, preprocessBase);

        CollectionReaderDescription testDataReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, testingWorkingDir, preprocessBase);

        String evalDir = FileUtils.joinPaths(testingWorkingDir, evalBase, "full_run");

        experiment(taskConfig, fullRunSuffix, trainingReader, testDataReader, evalDir, seed, runAll);
        jointExperiment(taskConfig, fullRunSuffix, trainingReader, testDataReader, evalDir, seed, runAll);

    }

    /**
     * Conduct cross validation  on various tasks.
     *
     * @param taskConfig
     * @throws Exception
     */
    public void crossValidation(Configuration taskConfig) throws Exception {
        int numSplit = taskConfig.getInt("edu.cmu.cs.lti.cv.split", 5);
        int seed = taskConfig.getInt("edu.cmu.cs.lti.random.seed", 17);

        String crossEvalDir = FileUtils.joinPaths(trainingWorkingDir, evalBase, "coref_cv");
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(crossEvalDir);

        logger.info(String.format("Will conduct %d fold cross validation.", numSplit));
        for (int slice = 0; slice < numSplit; slice++) {
            logger.info("Starting CV split " + slice);

            String sliceSuffix = "split_" + slice;

            CollectionReaderDescription trainingSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, trainingWorkingDir, preprocessBase, false, seed, numSplit, slice);
            CollectionReaderDescription devSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, trainingWorkingDir, preprocessBase, true, seed, numSplit, slice);

//            experiment(taskConfig, sliceSuffix, trainingSliceReader, devSliceReader, crossEvalDir, seed, false);
            jointExperiment(taskConfig, sliceSuffix, trainingSliceReader, devSliceReader, crossEvalDir, seed, false);
        }
    }

    private String runOnly(Configuration taskConfig, CollectionReaderDescription reader,
                           String typeModel, String realisModel, String corefModel,
                           String sliceSuffix, String runName, String outputDir,
                           boolean skipType, boolean skipRealis, boolean skipCoref)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        logger.info("Running plain models.");
        logger.info(String.format("Type model is %s, Realis Model is %s, Coref Model is %s.", typeModel, realisModel,
                typeModel));

        String annotatedOutput = FileUtils.joinPaths(middleResults, sliceSuffix, runName);

        CollectionReaderDescription mentionOutput = sentenceLevelMentionTagging(taskConfig, reader, typeModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "mention"), skipType);

        CollectionReaderDescription realisOutput = realisAnnotation(taskConfig, mentionOutput, realisModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "realis"), skipType && skipRealis);

        CollectionReaderDescription corefSentMentions = corefResolution(taskConfig,
                realisOutput, corefModel, trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "coref"),
                skipType && skipRealis && skipCoref);

        String tbfOutput = FileUtils.joinPaths(outputDir, runName + "_" + sliceSuffix + ".tbf");
        writeResults(corefSentMentions, tbfOutput, runName);

        return tbfOutput;
    }

    private String runPlainModels(Configuration taskConfig, CollectionReaderDescription reader,
                                  String typeModel, String realisModel, String corefModel,
                                  String sliceSuffix, String runName, String outputDir,
                                  String subEval, String gold,
                                  boolean skipType, boolean skipRealis, boolean skipCoref)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        logger.info("Running plain models.");
        logger.info(String.format("Type model is %s, Realis Model is %s, Coref Model is %s.", typeModel, realisModel,
                typeModel));

        String annotatedOutput = FileUtils.joinPaths(middleResults, sliceSuffix, runName);

        CollectionReaderDescription mentionOutput = sentenceLevelMentionTagging(taskConfig, reader, typeModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "mention"), skipType);

        CollectionReaderDescription realisOutput = realisAnnotation(taskConfig, mentionOutput, realisModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "realis"), skipType && skipRealis);

        CollectionReaderDescription corefSentMentions = corefResolution(taskConfig,
                realisOutput, corefModel, trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "coref"),
                skipType && skipRealis && skipCoref);

        String tbfOutput = FileUtils.joinPaths(outputDir, runName + "_" + sliceSuffix + ".tbf");
        writeResults(corefSentMentions, tbfOutput, runName);

        eval(gold, tbfOutput, subEval, runName, sliceSuffix);

        return tbfOutput;
    }

    private String runBeamMentions(Configuration taskConfig, CollectionReaderDescription reader,
                                   String typeModel, String realisModel, String corefModel,
                                   String sliceSuffix, String runName, String processDir, int beamSize,
                                   String subEval, String gold,
                                   boolean skipType, boolean skipRealis, boolean skipCoref) throws SAXException,
            UIMAException, CpeDescriptorException, IOException, InterruptedException {
        logger.info("Running models with beam based mentions.");

        logger.info(String.format("Type model is %s, Realis Model is %s, Coref Model is %s.", typeModel, realisModel,
                typeModel));

        String annotatedOutput = FileUtils.joinPaths(middleResults, sliceSuffix, runName);

        CollectionReaderDescription mentionOutput = beamMentionTagging(taskConfig, reader, typeModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "mention"), beamSize, skipType);

        CollectionReaderDescription realisOutput = realisAnnotation(taskConfig, mentionOutput, realisModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "realis"), skipType && skipRealis);

        CollectionReaderDescription corefSentMentions = corefResolution(taskConfig,
                realisOutput, corefModel, trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "coref"),
                skipType && skipRealis && skipCoref);

        String tbfOutput = FileUtils.joinPaths(processDir, runName + "_" + sliceSuffix + ".tbf");
        writeResults(corefSentMentions, tbfOutput, runName);

        eval(gold, tbfOutput, subEval, runName, sliceSuffix);

        return tbfOutput;
    }

    private String runBeamAll(Configuration taskConfig, CollectionReaderDescription reader,
                              String typeModel, String realisModel, String corefModel,
                              String sliceSuffix, String runName, String evalDir, int mentionBeamSize,
                              int corefBeamSize, String subEval, String gold,
                              boolean skipType, boolean skipRealis, boolean skipCoref)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        logger.info("Running models with beam based mentions and coreference.");

        logger.info(String.format("Type model is %s, Realis Model is %s, Coref Model is %s.", typeModel, realisModel,
                corefModel));

        String annotatedOutput = FileUtils.joinPaths(middleResults, sliceSuffix, runName);

        CollectionReaderDescription mentionOutput = beamMentionTagging(taskConfig, reader, typeModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "mention"), mentionBeamSize, skipType);

        CollectionReaderDescription realisOutput = realisAnnotation(taskConfig, mentionOutput, realisModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "realis"), skipType && skipRealis);

        CollectionReaderDescription beamCorefOutput = beamCorefResolution(taskConfig, realisOutput,
                corefModel, trainingWorkingDir, FileUtils.joinPaths(middleResults, sliceSuffix, "coref"),
                skipType && skipRealis && skipCoref, false, corefBeamSize);

        String tbfOutput = FileUtils.joinPaths(evalDir, runName + "_" + sliceSuffix + ".tbf");
        writeResults(beamCorefOutput, tbfOutput, runName);

        eval(gold, tbfOutput, subEval, runName, sliceSuffix);

        return tbfOutput;
    }

    private void jointExperiment(Configuration taskConfig, String sliceSuffix, CollectionReaderDescription trainReader,
                                 CollectionReaderDescription testReader, String processOutDir, int seed, boolean runAll
    ) throws Exception {
        boolean skipRealisTrain = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain", false);
        boolean skipJointTrain = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptrain", false);
        boolean skipJointTest = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptest", false);
        int jointBeamSize = taskConfig.getInt("edu.cmu.cs.lti.joint.beam.size", 5);

        String[] lossTypes = taskConfig.getList("edu.cmu.cs.lti.mention.loss_types");

        String subEvalDir = sliceSuffix.equals(fullRunSuffix) ? "final" : "cv";
        int jointMaxIter = taskConfig.getInt("edu.cmu.cs.lti.perceptron.joint.maxiter", 30);

        // TODO remember to set back training dir.
//        CollectionReaderDescription trainingData = prepareTraining(trainReader, trainingWorkingDir,
//                FileUtils.joinPaths(middleResults, sliceSuffix, "prepared_training"), false, seed);
        CollectionReaderDescription trainingData = prepareTraining(trainReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "prepared_training"), false, seed);

        // Produce gold standard tbf for evaluation.
        String devGold = FileUtils.joinPaths(processOutDir, "gold_dev_" + sliceSuffix + ".tbf");
        writeGold(testReader, devGold);

        String trainGold = FileUtils.joinPaths(processOutDir, "gold_train_" + sliceSuffix + ".tbf");
        writeGold(trainingData, trainGold);

        // Train realis model.
        String realisModelDir = trainRealisTypes(taskConfig, trainingData, sliceSuffix, skipRealisTrain);

        for (int i = 0; i < lossTypes.length; i++) {
            String lossType = lossTypes[i];

            for (int strategy = 1; strategy <= 1; strategy++) {
                String constrainModelSuffix = String.format("%s_loss=%s_beamSize=%d_con=%d", sliceSuffix,
                        lossType, jointBeamSize, strategy);
                trainJointSpanModel(taskConfig, trainingData, testReader, lossType, constrainModelSuffix,
                        skipJointTrain, skipJointTest, jointMaxIter, realisModelDir,
                        taskConfig.get("edu.cmu.cs.lti.model.joint.span.dir"), jointBeamSize, true, true, strategy,
                        processOutDir, trainGold, devGold, subEvalDir, sliceSuffix);
//                evalJointModels(processOutDir, subEvalDir, trainGold, constrainModelSuffix, sliceSuffix);
            }

//            String modelSuffix = String.format("%s_loss=%s_beamSize=%d", sliceSuffix, lossType, jointBeamSize);
//
//            trainJointSpanModel(taskConfig, trainingData, testReader, lossType, modelSuffix,
//                    skipJointTrain, skipJointTest, jointMaxIter, realisModelDir,
//                    taskConfig.get("edu.cmu.cs.lti.model.joint.span.dir"), jointBeamSize, true, false, 0,
//                    processOutDir, trainGold, devGold, subEvalDir, sliceSuffix);
//
//            evalJointModels(processOutDir, subEvalDir, trainGold, modelSuffix, sliceSuffix);
        }
    }

    private CollectionReaderDescription removeGold(CollectionReaderDescription trainingData, String mainDir, String
            baseDir) throws SAXException, UIMAException, CpeDescriptorException, IOException {

        return new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return trainingData;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription goldRemover = AnalysisEngineFactory.createEngineDescription(
                        GoldRemover.class, typeSystemDescription);

                return new AnalysisEngineDescription[]{goldRemover};
            }
        }, mainDir, baseDir).runWithOutput();
    }

    private void experiment(Configuration taskConfig, String sliceSuffix, CollectionReaderDescription trainReader,
                            CollectionReaderDescription testReader, String processOutDir, int seed, boolean runAll
    ) throws Exception {
        boolean skipCorefTrain = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptrain", false);
        boolean skipTypeTrain = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);
        boolean skipRealisTrain = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain", false);

        boolean skipTypeTest = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptest", false);
        boolean skipRealisTest = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptest", false);
        boolean skipCorefTest = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptest", false);

        int mentionBeamSize = taskConfig.getInt("edu.cmu.cs.lti.mention.beam.size", 5);
        int corefBeamSize = taskConfig.getInt("edu.cmu.cs.lti.coref.beam.size", 5);

        String[] lossTypes = taskConfig.getList("edu.cmu.cs.lti.mention.loss_types");

        String subEvalDir = sliceSuffix.equals(fullRunSuffix) ? "final" : "cv";
        int maxIteration = taskConfig.getInt("edu.cmu.cs.lti.perceptron.maxiter", 15);
        int delayedMaxIter = taskConfig.getInt("edu.cmu.cs.lti.perceptron.delayed.maxiter", 24);

        CollectionReaderDescription trainingData = prepareTraining(trainReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "prepared_training"), false, seed);

        // Train realis model.
        String realisModelDir = trainRealisTypes(taskConfig, trainingData, sliceSuffix, skipRealisTrain);

//        // TODO debug stuff:
//        boolean smallTrainDebug = true;
//        if (smallTrainDebug) {
//            // TODO remember to set back training dir.
//            CollectionReaderDescription smallTrainingData = prepareTraining(trainReader, trainingWorkingDir,
//                    FileUtils.joinPaths(middleResults, sliceSuffix, "prepared_training_test"), false, seed);
//
//            CollectionReaderDescription trainingDataWithoutGold = removeGold(smallTrainingData, trainingWorkingDir,
//                    FileUtils.joinPaths(middleResults, sliceSuffix, "training_without_gold"));
//
//            CollectionReaderDescription goldMentionWithTrain = annotateGoldMentions(trainingDataWithoutGold,
//                    trainingWorkingDir,
//                    FileUtils.joinPaths(middleResults, sliceSuffix, "gold_mentions_on_small_train"), true, true,
// false,
//                    true, true
//                    /* copy type, realis, not coref, merge types, skip*/);
//
//            String smallTrainGold = FileUtils.joinPaths(processOutDir, "gold_train_" + sliceSuffix + "_small.tbf");
//            writeGold(smallTrainingData, smallTrainGold);
//
//            // The vanilla crf model.
//            String smallSentModel = trainSentLvType(taskConfig, smallTrainingData, sliceSuffix + "_small", false,
//                    "hamming",
//                    seed, skipTypeTrain
//            );
//
//            // The vanilla coref model.
//            String smallTreeModel = trainLatentTreeCoref(taskConfig, smallTrainingData, sliceSuffix,
//                    taskConfig.get("edu.cmu.cs.lti.model.event.latent_tree"), seed, skipCorefTrain
//            );
//
//            runPlainModels(taskConfig, trainingDataWithoutGold, smallSentModel, realisModelDir, smallTreeModel,
//                    sliceSuffix, "vanillaMention_ON_train_small", processOutDir, subEvalDir, smallTrainGold,
//                    skipTypeTest, skipRealisTest, skipCorefTest);
//
//            CollectionReaderDescription corefGoldTypeRealisOnSmallTrain = corefResolution(taskConfig,
//                    goldMentionWithTrain, smallTreeModel, trainingWorkingDir,
//                    FileUtils.joinPaths(middleResults, sliceSuffix, "coref_gold_type+realis_on_small_train"),
//                    skipCorefTest);
//            String vanillaCorefOnGoldMentionsOnSmallTrain = FileUtils.joinPaths(processOutDir,
//                    "gold_type_realis_vanilla_coref_on_small_train_" + sliceSuffix + ".tbf");
//            writeResults(corefGoldTypeRealisOnSmallTrain, vanillaCorefOnGoldMentionsOnSmallTrain, "tree_coref");
//            eval(smallTrainGold, vanillaCorefOnGoldMentionsOnSmallTrain, subEvalDir,
//                    "gold_type_realis_vanilla_coref_on_small_train", sliceSuffix);
//            DebugUtils.pause();
//        }


        // Produce gold standard tbf for evaluation.
        String devGold = FileUtils.joinPaths(processOutDir, "gold_dev_" + sliceSuffix + ".tbf");
        writeGold(testReader, devGold);

        String trainGold = FileUtils.joinPaths(processOutDir, "gold_train_" + sliceSuffix + ".tbf");
        writeGold(trainingData, devGold);


        // The vanilla crf model.
        String vanillaSentCrfModel = trainSentLvType(taskConfig, trainingData, sliceSuffix, false, "hamming", seed,
                skipTypeTrain
        );

        // The vanilla coref model.
        String treeCorefModel = trainLatentTreeCoref(taskConfig, trainingData, sliceSuffix,
                taskConfig.get("edu.cmu.cs.lti.model.event.latent_tree"), seed, skipCorefTrain
        );


        // Train beamed based coref model.
        Map<String, String> beamCorefModels = new HashMap<>();
        Map<String, Integer> modelBeamSize = new HashMap<>();

        int[] sizes = {5};
        boolean[] useLaSO = {true}; // When using local features, whether to use LaSO is equivalent.
        boolean[] delayed = {true, false};
        boolean[] merged = {true, false};

        for (int size : sizes) {
            for (boolean laso : useLaSO) {
                for (boolean d : delayed) {
                    if (!laso && !d) {
                        // When not using LaSO, delayed doesn't matter.
                        continue;
                    }
                    for (boolean m : merged) {
                        String name = String.format("coref_laso=%s_delayed=%s_merged=%s_beamSize=%d", laso, d, m,
                                size);
                        String modelName = sliceSuffix + "_" + name;
                        String beamCoref = trainBeamBasedCoref(taskConfig, trainingData, modelName, skipCorefTrain,
                                seed, taskConfig.get("edu.cmu.cs.lti.model.event.latent_tree.beam"), m, laso, d,
                                size);
                        beamCorefModels.put(name, beamCoref);
                        modelBeamSize.put(name, size);
                    }
                }
            }
        }

        String[] paSentMentionModels = new String[lossTypes.length];
        String[] paBeamLaSOMentionModels = new String[lossTypes.length];
        String[] delayedPaBeamMentionModels = new String[lossTypes.length];
//        String[] paBeamVanillaMentionModels = new String[lossTypes.length];

        // Train beam based crf models.
        for (int i = 0; i < lossTypes.length; i++) {
            String lossType = lossTypes[i];

            // PA, LaSO, delayed.
            delayedPaBeamMentionModels[i] = trainBeamTypeModel(taskConfig, trainingData,
                    sliceSuffix + "_delayed_" + lossType, true, lossType, true, true, mentionBeamSize, delayedMaxIter,
                    null, seed, skipTypeTrain);

            // PA, LaSO, but not delayed.
            paBeamLaSOMentionModels[i] = trainBeamTypeModel(taskConfig, trainingData,
                    sliceSuffix + "_laso_" + lossType, true, lossType, true, false, mentionBeamSize, maxIteration,
                    null, seed, skipTypeTrain);

            // PA, no beam search.
            paSentMentionModels[i] = trainSentLvType(taskConfig, trainingData, sliceSuffix + "_" + lossType,
                    true, lossType, seed, skipTypeTrain);

            //            // PA, not laso, not delayed.
//            paBeamVanillaMentionModels[i] = trainBeamTypeModel(taskConfig, trainingData,
//                    sliceSuffix + "_vanilla_" + lossType, true, lossType, false, false, mentionBeamSize,
// maxIteration,
//                    null, seed, skipTypeTrain);
        }


        // A beam search model that do not use PA loss.
        String noPaVanillaBeamMentionModel = trainBeamTypeModel(taskConfig, trainingData, sliceSuffix + "_vanilla",
                false, "hamming", false, false, mentionBeamSize, maxIteration, null, seed, skipTypeTrain);
        String noPaLaSoBeamMentionModel = trainBeamTypeModel(taskConfig, trainingData, sliceSuffix + "_laso",
                false, "hamming", true, false, mentionBeamSize, maxIteration, null, seed, skipTypeTrain);

        // Produce gold mention (type + realis) detection.
        CollectionReaderDescription goldMentionAll = annotateGoldMentions(testReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "gold_mentions"), true, true, false, true, true
                    /* copy type, realis, not coref, merge types, skip*/);

        // Gold mention types.
        CollectionReaderDescription goldMentionTypes = annotateGoldMentions(testReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "gold_type"), true, false, false, true, true
                    /* copy type, not realis, not coref, merge types, skip*/);

        // Run realis on gold types.
        CollectionReaderDescription goldTypeSystemRealis = realisAnnotation(taskConfig, goldMentionTypes,
                realisModelDir, trainingWorkingDir, FileUtils.joinPaths(middleResults, sliceSuffix,
                        "gold_system_realis"), skipRealisTest);

        // Coreference with gold components.
        CollectionReaderDescription corefGoldTypeRealis = corefResolution(taskConfig, goldMentionAll,
                treeCorefModel, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "coref_gold_type+realis"), skipCorefTest);

        String vanillaCorefOnGoldMentions = FileUtils.joinPaths(processOutDir,
                "gold_type_realis_vanilla_coref_" + sliceSuffix + ".tbf");
        writeResults(corefGoldTypeRealis, vanillaCorefOnGoldMentions, "tree_coref");
        eval(devGold, vanillaCorefOnGoldMentions, subEvalDir, "gold_type_realis_vanilla_coref", sliceSuffix);

        CollectionReaderDescription corefGoldType = corefResolution(taskConfig, goldTypeSystemRealis,
                treeCorefModel, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "vanilla_coref_gold_type"), skipCorefTest);

        String vanillaCorefOnGoldType = FileUtils.joinPaths(processOutDir, "gold_type_coref_" + sliceSuffix + ".tbf");
        writeResults(corefGoldType, vanillaCorefOnGoldType, "gold_types");
        eval(devGold, vanillaCorefOnGoldType, subEvalDir, "gold_type_vanilla_coref", sliceSuffix);

        // Beam coreference with gold components.
        for (Map.Entry<String, String> beamCorefModelWithName : beamCorefModels.entrySet()) {
            String coreModelName = beamCorefModelWithName.getKey();
            String beamCorefModel = beamCorefModelWithName.getValue();
            int beamSize = modelBeamSize.get(coreModelName);

            String outputName = "gold_type_" + coreModelName;
            CollectionReaderDescription goldBeamCorefResult = beamCorefResolution(taskConfig, goldMentionAll,
                    beamCorefModel, trainingWorkingDir, FileUtils.joinPaths(middleResults, sliceSuffix, outputName),
                    skipCorefTest, false, beamSize);
            String goldBeamCorefOut = FileUtils.joinPaths(processOutDir, outputName + "_" + sliceSuffix + ".tbf");
            writeResults(goldBeamCorefResult, goldBeamCorefOut, "tree_coref");
            eval(devGold, goldBeamCorefOut, subEvalDir, outputName, sliceSuffix);
        }

        // Run the vanilla model.
        runPlainModels(taskConfig, testReader, vanillaSentCrfModel, realisModelDir, treeCorefModel,
                sliceSuffix, "vanillaMention", processOutDir, subEvalDir, devGold,
                skipTypeTest, skipRealisTest, skipCorefTest);

        runBeamMentions(taskConfig, testReader, noPaVanillaBeamMentionModel, realisModelDir, treeCorefModel,
                sliceSuffix, "noPaVanillaBeamMention", processOutDir, mentionBeamSize,
                subEvalDir, devGold, skipTypeTest, skipRealisTest, skipCorefTest);

        runBeamMentions(taskConfig, testReader, noPaLaSoBeamMentionModel, realisModelDir, treeCorefModel, sliceSuffix,
                "noPaLasoBeamMention", processOutDir, mentionBeamSize, subEvalDir, devGold,
                skipTypeTest, skipRealisTest, skipCorefTest);

        // Run the sent models.
        for (int i = 0; i < lossTypes.length; i++) {
            String lossType = lossTypes[i];
            String paModel = paSentMentionModels[i];
            runPlainModels(taskConfig, testReader, paModel, realisModelDir, treeCorefModel, sliceSuffix,
                    "paMention_" + lossType, processOutDir, subEvalDir, devGold,
                    skipTypeTest, skipRealisTest, skipCorefTest);

            String laSoBeamModel = paBeamLaSOMentionModels[i];
//            String vanillaBeamModel = paBeamVanillaMentionModels[i];
            runBeamMentions(taskConfig, testReader, laSoBeamModel, realisModelDir, treeCorefModel, sliceSuffix,
                    "laSoBeamMention_" + lossType, processOutDir, mentionBeamSize, subEvalDir, devGold,
                    skipTypeTest, skipRealisTest, skipCorefTest);

//            runBeamMentions(taskConfig, testReader, vanillaBeamModel, realisModelDir, treeCorefModel, sliceSuffix,
//                    "vanillaBeamMention_" + lossType, processOutDir, mentionBeamSize, subEvalDir, devGold,
//                    skipTypeTest, skipRealisTest, skipCorefTest);

            String delayedBeamModel = delayedPaBeamMentionModels[i];
            runBeamMentions(taskConfig, testReader, delayedBeamModel, realisModelDir, treeCorefModel, sliceSuffix,
                    "delayedBeamMention_" + lossType, processOutDir, mentionBeamSize, subEvalDir, devGold,
                    skipTypeTest, skipRealisTest, skipCorefTest);

            for (Map.Entry<String, String> beamCorefModelWithName : beamCorefModels.entrySet()) {
                String coreModelName = beamCorefModelWithName.getKey();
                String beamCorefModel = beamCorefModelWithName.getValue();
//                runBeamAll(taskConfig, testReader, vanillaBeamModel, realisModelDir, beamCorefModel, sliceSuffix,
//                        "vanillaBeamMention_" + lossType + "_" + coreModelName, processOutDir, mentionBeamSize,
//                        corefBeamSize, subEvalDir, devGold, skipTypeTest, skipRealisTest, skipCorefTest);
                runBeamAll(taskConfig, testReader, laSoBeamModel, realisModelDir, beamCorefModel, sliceSuffix,
                        "laSoBeamMention_" + lossType + "_" + coreModelName, processOutDir, mentionBeamSize,
                        corefBeamSize, subEvalDir, devGold, skipTypeTest, skipRealisTest, skipCorefTest);

                runBeamAll(taskConfig, testReader, delayedBeamModel, realisModelDir, beamCorefModel, sliceSuffix,
                        "delayedBeamMention_" + lossType + "_" + coreModelName, processOutDir, mentionBeamSize,
                        corefBeamSize, subEvalDir, devGold, skipTypeTest, skipRealisTest, skipCorefTest);
            }
        }
    }
}
