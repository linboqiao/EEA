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
import edu.cmu.cs.lti.event_coref.annotators.postprocessors.MentionTypeAncClusterSplitter;
import edu.cmu.cs.lti.event_coref.annotators.prepare.ArgumentMerger;
import edu.cmu.cs.lti.event_coref.annotators.prepare.EnglishSrlArgumentExtractor;
import edu.cmu.cs.lti.event_coref.annotators.prepare.EventHeadWordAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.train.BeamBasedCorefTrainer;
import edu.cmu.cs.lti.event_coref.annotators.train.DelayedLaSOJointTrainer;
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
    public EventMentionPipeline(String typeSystemName, String language, boolean useCharOffset, String modelDir,
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
     */
    public EventMentionPipeline(String typeSystemName, Configuration config) {
        this(typeSystemName, config.getOrElse("edu.cmu.cs.lti.language", "en"),
                config.getBoolean("edu.cmu.cs.lti.output.character.offset", false),
                config.get("edu.cmu.cs.lti.model.dir"),
                config.get("edu.cmu.cs.lti.model.event.dir"), config.get("edu.cmu.cs.lti.training.working.dir"),
                config.get("edu.cmu.cs.lti.test.working.dir"));
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

    public CollectionReaderDescription goldMentionAnnotator(CollectionReaderDescription reader, String mainDir,
                                                            String baseOutput, boolean copyType, boolean copyRealis,
                                                            boolean copyCluster, boolean mergeSameSpan)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
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
        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, baseOutput);
    }

    public String trainSentLvType(Configuration config, CollectionReaderDescription trainingReader, String suffix,
                                  boolean skipTrain, boolean usePaTraing, String lossType, int initialSeed)
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
                protected void loopActions() {
                    super.loopActions();
                    trainingSeed.add(2);
                    logger.debug("Update the training seed to " + trainingSeed.intValue());
                    trainingReader.setAttributeValue(RandomizedXmiCollectionReader.PARAM_SEED, trainingSeed.getValue());
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
                                     boolean skipTrain, boolean usePaTraing, String lossType, boolean delayedLaso,
                                     int initialSeed) throws UIMAException, NoSuchMethodException, IOException,
            InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
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
                    BeamBasedMentionTypeTrainer.PARAM_DELAYED_LASO, delayedLaso
            );

            MutableInt trainingSeed = new MutableInt(initialSeed);

            new TrainingLooper(config, modelPath,
                    trainingReader, trainer) {
                @Override
                protected void loopActions() {
                    super.loopActions();
                    trainingSeed.add(2);
                    logger.debug("Update the training seed to " + trainingSeed.intValue());
                    trainingReader.setAttributeValue(RandomizedXmiCollectionReader.PARAM_SEED, trainingSeed.getValue());
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
                            BeamTypeAnnotator.class, typeSystemDescription,
                            BeamTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                            BeamTypeAnnotator.PARAM_CONFIG, taskConfig.getConfigFile().getPath()
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
                protected void loopActions() {
                    super.loopActions();
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
                                      boolean mergeMention, boolean delayedLaso) throws
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
                    BeamBasedCorefTrainer.PARAM_MERGE_MENTION, mergeMention
            );

            TrainingLooper corefTrainer = new TrainingLooper(config, cvModelDir, trainingReader, corefEngine) {
                @Override
                protected void loopActions() {
                    super.loopActions();
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
     * @param skipTrain      Whether to skip the training if model file exists.
     * @param initialSeed    The initial seed for the reader.
     * @return The trained model directory.
     */
    private String trainLatentTreeCoref(Configuration config, CollectionReaderDescription trainingReader,
                                        String suffix, boolean skipTrain, int initialSeed, String modelDir)
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
                protected void loopActions() {
                    super.loopActions();
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
                                                            boolean mergeMentions, boolean skipCorefTest)
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
                            BeamEventCorefAnnotator.PARAM_MERGE_MENTION, mergeMentions
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

    public String trainJointSpanModel(Configuration config, CollectionReaderDescription trainingReader, String lossType,
                                      boolean useWarmStart, String suffix, boolean skipTrain, int initialSeed,
                                      String realisModelDir, String pretrainedMentionModel, String modelDir)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Start beam based joint training.");
        String cvModelDir = FileUtils.joinPaths(eventModelDir, modelDir, suffix);

        boolean modelExists = new File(cvModelDir).exists();

        if (skipTrain && modelExists) {
            logger.info("Skipping beam joint training, taking existing models.");
        } else {
            logger.info("Saving model directory at : " + cvModelDir);

            AnalysisEngineDescription corefEngine = AnalysisEngineFactory.createEngineDescription(
                    DelayedLaSOJointTrainer.class, typeSystemDescription,
                    DelayedLaSOJointTrainer.PARAM_CONFIG_PATH, config.getConfigFile().getPath(),
                    DelayedLaSOJointTrainer.PARAM_REALIS_MODEL_DIRECTORY, realisModelDir,
                    DelayedLaSOJointTrainer.PARAM_PRETRAINED_MENTION_MODEL_DIRECTORY, pretrainedMentionModel,
                    DelayedLaSOJointTrainer.PARAM_USE_WARM_START, useWarmStart,
                    DelayedLaSOJointTrainer.PARAM_MENTION_LOSS_TYPE, lossType
            );

            TrainingLooper trainer = new TrainingLooper(config, cvModelDir, trainingReader, corefEngine) {
                @Override
                protected void loopActions() {
                    super.loopActions();
                }

                @Override
                protected void finish() throws IOException {
                    DelayedLaSOJointTrainer.finish();
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    DelayedLaSOJointTrainer.saveModels(modelOutputDir);
                }
            };

            trainer.runLoopPipeline();

            logger.info("Coreference training finished ...");
        }
        return cvModelDir;
    }

    private CollectionReaderDescription beamJointSpanCoref(Configuration config, CollectionReaderDescription reader,
                                                           String modelDir, String realisDir, String mainDir,
                                                           String outputBase, boolean skipTest) throws
            UIMAException, SAXException, CpeDescriptorException, IOException {
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
                    AnalysisEngineDescription jointDecoder = AnalysisEngineFactory.createEngineDescription(
                            JointMentionCorefAnnotator.class, typeSystemDescription,
                            JointMentionCorefAnnotator.PARAM_CONFIG_PATH, config.getConfigFile(),
                            JointMentionCorefAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                            JointMentionCorefAnnotator.PARAM_REALIS_MODEL_DIRECTORY, realisDir
                    );

                    List<AnalysisEngineDescription> annotators = new ArrayList<>();
                    addCorefPreprocessors(annotators);
                    annotators.add(jointDecoder);

                    return annotators.toArray(new AnalysisEngineDescription[annotators.size()]);
                }
            }, mainDir, outputBase).runWithOutput();
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, outputBase);
        }
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

        String evalDir = FileUtils.joinPaths(testingWorkingDir, evalBase, "full_run");

        CollectionReaderDescription trainingData = prepareTraining(trainReader,
                trainingWorkingDir, FileUtils.joinPaths(middleResults, fullRunSuffix, "prepared_training"), false,
                seed);

        // Train realis model.
        String realisModelDir = trainRealisTypes(taskConfig, trainingData, fullRunSuffix, skipRealisTrain);

        String vanillaSentCrfModel = trainSentLvType(taskConfig, trainingData, fullRunSuffix, skipTypeTrain, false,
                "hamming", seed);

        // Train coref model.
        String treeCorefModel = trainLatentTreeCoref(taskConfig, trainingData, fullRunSuffix, skipCorefTrain, seed,
                taskConfig.get("edu.cmu.cs.lti.model.event.latent_tree"));

        // Run the vanilla model.
        runPlainModels(taskConfig, testReader, vanillaSentCrfModel, realisModelDir, treeCorefModel, fullRunSuffix,
                "vanillaMention", evalDir, skipTypeTest, skipRealisTest, skipCorefTest);
    }

    public void trainTest(Configuration taskConfig, boolean runAll) throws Exception {
        int seed = taskConfig.getInt("edu.cmu.cs.lti.random.seed", 17);

        CollectionReaderDescription trainingReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, preprocessBase);

        CollectionReaderDescription testDataReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, testingWorkingDir, preprocessBase);

        String evalDir = FileUtils.joinPaths(testingWorkingDir, evalBase, "full_run");

        experiment(taskConfig, fullRunSuffix, trainingReader, testDataReader, evalDir, seed, runAll);
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

            experiment(taskConfig, sliceSuffix, trainingSliceReader, devSliceReader, crossEvalDir, seed, false);
        }
    }

    private void runPlainModels(Configuration taskConfig, CollectionReaderDescription reader,
                                String typeModel, String realisModel, String corefModel,
                                String sliceSuffix, String runName, String evalDir,
                                boolean skipType, boolean skipRealis, boolean skipCoref)
            throws SAXException, UIMAException, CpeDescriptorException, IOException {
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

        writeResults(corefSentMentions, FileUtils.joinPaths(evalDir, runName + "_" + sliceSuffix + ".tbf"), runName);
    }

    private void runBeamMentions(Configuration taskConfig, CollectionReaderDescription reader,
                                 String typeModel, String realisModel, String corefModel,
                                 String sliceSuffix, String runName, String evalDir,
                                 boolean skipType, boolean skipRealis, boolean skipCoref) throws SAXException,
            UIMAException, CpeDescriptorException, IOException {
        logger.info(String.format("Type model is %s, Realis Model is %s, Coref Model is %s.", typeModel, realisModel,
                typeModel));

        String annotatedOutput = FileUtils.joinPaths(middleResults, sliceSuffix, runName);

        CollectionReaderDescription mentionOutput = beamMentionTagging(taskConfig, reader, typeModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "mention"), skipType);

        CollectionReaderDescription realisOutput = realisAnnotation(taskConfig, mentionOutput, realisModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "realis"), skipType && skipRealis);

        CollectionReaderDescription corefSentMentions = corefResolution(taskConfig,
                realisOutput, corefModel, trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "coref"),
                skipType && skipRealis && skipCoref);

        writeResults(corefSentMentions, FileUtils.joinPaths(evalDir, runName + "_" + sliceSuffix + ".tbf"), runName);
    }

    private void runBeamAll(Configuration taskConfig, CollectionReaderDescription reader,
                            String typeModel, String realisModel, String corefModel,
                            String sliceSuffix, String runName, String evalDir,
                            boolean skipType, boolean skipRealis, boolean skipCoref) throws SAXException,
            UIMAException, CpeDescriptorException, IOException {
        logger.info(String.format("Type model is %s, Realis Model is %s, Coref Model is %s.", typeModel, realisModel,
                typeModel));

        String annotatedOutput = FileUtils.joinPaths(middleResults, sliceSuffix, runName);

        CollectionReaderDescription mentionOutput = beamMentionTagging(taskConfig, reader, typeModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "mention"), skipType);

        CollectionReaderDescription realisOutput = realisAnnotation(taskConfig, mentionOutput, realisModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "realis"), skipType && skipRealis);

        CollectionReaderDescription beamCorefDelayedGold = beamCorefResolution(taskConfig, realisOutput,
                corefModel, trainingWorkingDir, FileUtils.joinPaths(middleResults, sliceSuffix,
                        "beam_coref_delayed_gold"), skipType && skipRealis && skipCoref, false);

        writeResults(beamCorefDelayedGold, FileUtils.joinPaths(evalDir, runName + "_" + sliceSuffix + ".tbf"), runName);
    }

    private void experiment(Configuration taskConfig, String sliceSuffix, CollectionReaderDescription trainReader,
                            CollectionReaderDescription testReader, String evalDir, int seed, boolean runAll
    ) throws Exception {
        boolean skipCorefTrain = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptrain", false);
        boolean skipTypeTrain = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);
        boolean skipRealisTrain = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain", false);
        boolean skipBeamTrain = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptrain", false);

        boolean skipTypeTest = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptest", false);
        boolean skipRealisTest = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptest", false);
        boolean skipCorefTest = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptest", false);
        boolean skipBeamTest = runAll || taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptest", false);

//        String[] lossTypes = {"recallHamming", "hamming", "noneHamming"};
        String[] lossTypes = taskConfig.getList("edu.cmu.cs.lti.mention.loss_types");

        CollectionReaderDescription trainingData = prepareTraining(trainReader,
                trainingWorkingDir, FileUtils.joinPaths(middleResults, sliceSuffix, "prepared_training"), false,
                seed);

        // Train realis model.
        String realisModelDir = trainRealisTypes(taskConfig, trainingData, sliceSuffix, skipRealisTrain);

        String[] paSentMentionModels = new String[lossTypes.length];
        String[] paBeamMentionModels = new String[lossTypes.length];
//        String[] delayedPaBeamMentionModels = new String[lossTypes.length];

        for (int i = 0; i < lossTypes.length; i++) {
            String lossType = lossTypes[i];
            paBeamMentionModels[i] = trainBeamTypeModel(taskConfig, trainingData, sliceSuffix + "_" + lossType,
                    skipTypeTrain, true, lossType, false, seed);
            paSentMentionModels[i] = trainSentLvType(taskConfig, trainingData, sliceSuffix + "_" + lossType,
                    skipTypeTrain, true, lossType, seed);
//            delayedPaBeamMentionModels[i] = trainBeamTypeModel(taskConfig, trainingData,
//                    sliceSuffix + "_delayed_" + lossType, skipTypeTrain, true, lossType, true, seed);
        }

        // A beam search model that do not use PA loss.
        String vanillaBeamMentionModel = trainBeamTypeModel(taskConfig, trainingData, sliceSuffix, skipTypeTrain,
                false, "hamming", false, seed);

        // The vanilla crf model.
        String vanillaSentCrfModel = trainSentLvType(taskConfig, trainingData, sliceSuffix, skipTypeTrain, false,
                "hamming", seed);

        // Train coref model.
        String treeCorefModel = trainLatentTreeCoref(taskConfig, trainingData, sliceSuffix, skipCorefTrain, seed,
                taskConfig.get("edu.cmu.cs.lti.model.event.latent_tree"));

        // Train beamed based coref model.
        Map<String, String> beamCorefModels = new HashMap<>();

        String beamDelayedMerged = trainBeamBasedCoref(taskConfig, trainingData, sliceSuffix + "_delayed_merge",
                skipCorefTrain, seed, taskConfig.get("edu.cmu.cs.lti.model.event.latent_tree.beam"), true, true);
        String beamDelayedUnMerge = trainBeamBasedCoref(taskConfig, trainingData, sliceSuffix + "_delayed_unmerge",
                skipCorefTrain, seed, taskConfig.get("edu.cmu.cs.lti.model.event.latent_tree.beam"), false, true);
        String beamEarlyMerged = trainBeamBasedCoref(taskConfig, trainingData, sliceSuffix + "_early_merge",
                skipCorefTrain, seed, taskConfig.get("edu.cmu.cs.lti.model.event.latent_tree.beam"), true, false);
        String beamEarlyUnmerge = trainBeamBasedCoref(taskConfig, trainingData, sliceSuffix + "_early_unmerge",
                skipCorefTrain, seed, taskConfig.get("edu.cmu.cs.lti.model.event.latent_tree.beam"), false, false);

        beamCorefModels.put("delayed_merge", beamDelayedMerged);
        beamCorefModels.put("delayed_unmerge", beamDelayedUnMerge);
        beamCorefModels.put("early_merge", beamEarlyMerged);
        beamCorefModels.put("early_unmerge", beamEarlyUnmerge);

//        // Train Beam search joint model.
//        String regularBeamJointModel = trainJointSpanModel(taskConfig, trainingData, false,
//                sliceSuffix, skipBeamTrain, seed, realisModelDir,
//                vanillaSentCrfModel, taskConfig.get("edu.cmu.cs.lti.model.joint.span.dir"));

        // Run the vanilla model.
        runPlainModels(taskConfig, testReader, vanillaSentCrfModel, realisModelDir, treeCorefModel, sliceSuffix,
                "vanillaMention", evalDir, skipTypeTest, skipRealisTest, skipCorefTest);

        runBeamMentions(taskConfig, testReader, vanillaBeamMentionModel, realisModelDir, treeCorefModel, sliceSuffix,
                "vanillaBeamMention", evalDir, skipTypeTest, skipRealisTest, skipCorefTest);

        // Run the sent models.
        for (int i = 0; i < lossTypes.length; i++) {
            String lossType = lossTypes[i];
            String paModel = paSentMentionModels[i];
            runPlainModels(taskConfig, testReader, paModel, realisModelDir, treeCorefModel, sliceSuffix,
                    "paMention_" + lossType, evalDir, skipTypeTest, skipRealisTest, skipCorefTest);
            String beamModel = paBeamMentionModels[i];
            runBeamMentions(taskConfig, testReader, beamModel, realisModelDir, treeCorefModel, sliceSuffix,
                    "beamMention_" + lossType, evalDir, skipTypeTest, skipRealisTest, skipCorefTest);
//            String delayedBeamModel = delayedPaBeamMentionModels[i];
//            runBeamModels(taskConfig, testReader, delayedBeamModel, realisModelDir, treeCorefModel, sliceSuffix,
//                    "delayedBeamMention_" + lossType, evalDir, skipTypeTest, skipRealisTest, skipCorefTest);

            for (Map.Entry<String, String> beamCorefModelWithName : beamCorefModels.entrySet()) {
                String coreModelName = beamCorefModelWithName.getKey();
                String beamCorefModel = beamCorefModelWithName.getValue();
                runBeamAll(taskConfig, testReader, beamModel, realisModelDir, beamCorefModel, sliceSuffix,
                        "beamMention_" + lossType + "_" + coreModelName, evalDir, skipTypeTest, skipRealisTest,
                        skipCorefTest);
            }
        }

        // Gold mention types.
        CollectionReaderDescription goldMentionTypes = goldMentionAnnotator(testReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "gold_type"), true, false, false, true
                    /* copy type, not realis, not coref, merge types*/);

        // Run realis on gold types.
        CollectionReaderDescription goldTypeSystemRealis = realisAnnotation(taskConfig, goldMentionTypes,
                realisModelDir, trainingWorkingDir, FileUtils.joinPaths(middleResults, sliceSuffix,
                        "gold_system_realis"), skipRealisTest);

        // Run gold mention (type + realis) detection.
        CollectionReaderDescription goldMentionAll = goldMentionAnnotator(testReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "gold_mentions"), true, true, false, true
                    /* copy type, realis, not coref, merge types*/);

        // Beam coreference with gold components.
        for (Map.Entry<String, String> beamCorefModelWithName : beamCorefModels.entrySet()) {
            String coreModelName = beamCorefModelWithName.getKey();
            String beamCorefModel = beamCorefModelWithName.getValue();

            String outputName = "gold_type_" + coreModelName;
            CollectionReaderDescription goldBeamCorefResult = beamCorefResolution(taskConfig, goldMentionAll,
                    beamCorefModel, trainingWorkingDir, FileUtils.joinPaths(middleResults, sliceSuffix,
                            outputName), skipCorefTest, false);
            writeResults(goldBeamCorefResult, FileUtils.joinPaths(evalDir, outputName + ".tbf"), "tree_coref");
        }

        // Coreference with gold components.
        CollectionReaderDescription corefGoldType = corefResolution(taskConfig, goldTypeSystemRealis,
                treeCorefModel, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "coref_gold_type"), skipCorefTest);

        CollectionReaderDescription corefGoldTypeRealis = corefResolution(taskConfig, goldMentionAll,
                treeCorefModel, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "coref_gold_type+realis"), skipCorefTest);

        writeResults(corefGoldType,
                FileUtils.joinPaths(evalDir, "gold_type_coref_" + sliceSuffix + ".tbf"), "gold_types"
        );
        writeResults(
                corefGoldTypeRealis,
                FileUtils.joinPaths(evalDir, "gold_type_realis_coref_" + sliceSuffix + ".tbf"), "tree_coref"
        );

//        writeResults(
//                jointSpanCorefMentions, FileUtils.joinPaths(evalDir, "joint_span_coref_" + sliceSuffix + ".tbf"),
//                "tree_coref"
//        );

        // Produce gold standard tbf for easy evaluation.
        writeGold(testReader, FileUtils.joinPaths(evalDir, "gold_" + sliceSuffix + ".tbf"));
    }
}
