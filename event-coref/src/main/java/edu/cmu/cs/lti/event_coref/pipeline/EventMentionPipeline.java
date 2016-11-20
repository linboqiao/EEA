package edu.cmu.cs.lti.event_coref.pipeline;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.annotators.*;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.BeamTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.CrfMentionTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.emd.annotators.TokenBasedMentionErrorAnalyzer;
import edu.cmu.cs.lti.emd.annotators.classification.AllActualRealisAnnotator;
import edu.cmu.cs.lti.emd.annotators.classification.RealisTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.misc.EventMentionTypeClassPrinter;
import edu.cmu.cs.lti.emd.annotators.postprocessors.MentionTypeSplitter;
import edu.cmu.cs.lti.emd.annotators.train.BeamBasedMentionTypeTrainer;
import edu.cmu.cs.lti.emd.annotators.train.TokenLevelEventMentionCrfTrainer;
import edu.cmu.cs.lti.emd.pipeline.TrainingLooper;
import edu.cmu.cs.lti.emd.stat.ChineseMentionStats;
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
import edu.cmu.cs.lti.io.EventDataReader;
import edu.cmu.cs.lti.learning.train.RealisClassifierTrainer;
import edu.cmu.cs.lti.learning.utils.ModelUtils;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.pipeline.ProcessorWrapper;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.reader.RandomizedXmiCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.pipeline.MorePipeline;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.NotImplementedException;
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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

//import edu.cmu.cs.lti.annotators.*;


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
    final private String modelConfigDir;

    // Some conventions of processing data.
    final private String preprocessBase = "preprocessed";
    final private String rawBase = "raw";
    final private String trialBase = "trial";
    final private String processOut;
    final private String evalBase;
    final private String middleResults;

    // When cross validation, we have auto generated suffixes for outputs. Let's make one for the full run too.
    final static String fullRunSuffix = "all";

    final private String language;

    private boolean useCharOffset;

    private String tokenDir;

    private String evalMode;

    private String evalLogOutputDir;

    private String evalScript;


    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * A constructor that take both the training and testing directory.
     *
     * @param typeSystemName     The type system to use.
     * @param modelDir           The models directory saving other NLP tools model.
     * @param modelOutDir        The models directory saving the output models.
     * @param trainingWorkingDir The main working directory of the training data.
     * @param testingWorkingDir  The main working directory of the testing data.
     * @param modelConfigDir
     * @param processOutputDir
     */
    private EventMentionPipeline(String typeSystemName, String language, boolean useCharOffset, String modelDir,
                                 String modelOutDir, String trainingWorkingDir, String testingWorkingDir,
                                 String modelConfigDir, String processOutputDir) {
        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);
        this.generalModelDir = modelDir;
        this.eventModelDir = modelOutDir;

        this.trainingWorkingDir = trainingWorkingDir;
        this.testingWorkingDir = testingWorkingDir;
        this.modelConfigDir = modelConfigDir;

        if (trainingWorkingDir != null) {
            logger.info(String.format("Training directory will be %s.", trainingWorkingDir));
        }

        if (testingWorkingDir != null && new File(testingWorkingDir).exists()) {
            logger.info(String.format("Testing directory will be %s.", testingWorkingDir));
        }

        logger.info(String.format("Models can be found in %s.", modelOutDir));

        this.language = language;
        this.useCharOffset = useCharOffset;

        this.processOut = processOutputDir;
        this.evalBase = processOutputDir + "/eval";
        this.middleResults = processOutputDir + "/intermediate";
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
                config.get("edu.cmu.cs.lti.model.event.dir"),
                config.get("edu.cmu.cs.lti.training.working.dir"),
                config.get("edu.cmu.cs.lti.test.working.dir"),
                config.get("edu.cmu.cs.lti.model.config.dir"),
                config.get("edu.cmu.cs.lti.process.base.dir") + "_" + config.get("edu.cmu.cs.lti.experiment.name")
        );

        this.evalMode = config.get("edu.cmu.cs.lti.eval.mode");

        if (evalMode.equals("token")) {
            this.tokenDir = config.get("edu.cmu.cs.lti.training.token_map.dir");
        } else if (evalMode.equals("char")) {

        } else {
            throw new IllegalArgumentException(String.format("Unknown evaluation mode: %s.", evalMode));
        }

        this.evalLogOutputDir = FileUtils.joinPaths(config.get("edu.cmu.cs.lti.eval.log_dir"),
                config.get("edu.cmu.cs.lti.experiment.name"));
        this.evalScript = config.get("edu.cmu.cs.lti.eval.script");
    }

    private CollectionReaderDescription[] getDatasetReaders(String datasetConfigPath, String[] datasetNames)
            throws IOException, ResourceInitializationException {
        logger.info(String.format("%d datasets to be read, which are %s",
                datasetNames.length, Joiner.on(",").join(datasetNames)));
        CollectionReaderDescription[] readers = new CollectionReaderDescription[datasetNames.length];

        for (int i = 0; i < datasetNames.length; i++) {
            String datasetName = datasetNames[i];
            Configuration datasetConfig = new Configuration(new File(datasetConfigPath, datasetName + ".properties"));
            readers[i] = EventDataReader.getReader(datasetConfig, typeSystemDescription);
        }

        return readers;
    }

    /**
     * Run preprocessing for training and testing directory.
     *
     * @param taskConfig The configuration file.
     * @throws IOException
     * @throws UIMAException
     */
    public void prepare(Configuration taskConfig) throws IOException,
            UIMAException, CpeDescriptorException, SAXException {
        boolean skipTrainPrepare = taskConfig.getBoolean("edu.cmu.cs.lti.train.skip.preprocess", false);
        boolean skipTestPrepare = taskConfig.getBoolean("edu.cmu.cs.lti.test.skip.preprocess", false);

        String datasetSettingDir = taskConfig.get("edu.cmu.cs.lti.dataset.settings.path");
        String[] trainingDatasets = taskConfig.getList("edu.cmu.cs.lti.training.datasets");
        String[] testDatasets = taskConfig.getList("edu.cmu.cs.lti.testing.datasets");

        CollectionReaderDescription[] trainingReaders = getDatasetReaders(datasetSettingDir, trainingDatasets);
        CollectionReaderDescription[] testReaders = getDatasetReaders(datasetSettingDir, testDatasets);

        File classFile = new File(FileUtils.joinPaths(trainingWorkingDir, "mention_types.txt"));
        if (classFile.exists()) {
            try (FileWriter writer = new FileWriter(classFile)) {
                writer.write("");
            }
        }

        boolean skipRaw = taskConfig.getBoolean("edu.cmu.cs.lti.skip.raw", false);

        if (trainingReaders.length != 0) {
            logger.info("Writing possible classes from training data.");
            AnalysisEngineDescription classPrinter = AnalysisEngineFactory.createEngineDescription(
                    EventMentionTypeClassPrinter.class, typeSystemDescription,
                    EventMentionTypeClassPrinter.CLASS_OUTPUT_FILE, classFile
            );
            MorePipeline.runPipelineWithMultiReaderDesc(Arrays.asList(trainingReaders), classPrinter);

            if (skipRaw && new File(trainingWorkingDir, rawBase).exists()) {
                logger.info("Skip training raw writing.");
            } else {
                logger.info("Writing raw training data to disk, not skipping");
                MorePipeline.runPipelineWithMultiReaderDesc(Arrays.asList(trainingReaders),
                        CustomAnalysisEngineFactory.createXmiWriter(trainingWorkingDir, rawBase));
            }

            logger.info("Preprocessing the raw training data.");
            CollectionReaderDescription rawReader = CustomCollectionReaderFactory.createXmiReader(trainingWorkingDir,
                    rawBase);
            prepare(taskConfig, trainingWorkingDir, skipTrainPrepare, rawReader);
        } else {
            logger.warn("No training readers specified.");
        }

        if (testReaders.length != 0) {
            if (skipRaw && new File(testingWorkingDir, rawBase).exists()) {
                logger.info("Skip test raw writing.");
            } else {
                logger.info("Writing raw test data to disk, not skipping");
                MorePipeline.runPipelineWithMultiReaderDesc(Arrays.asList(testReaders),
                        CustomAnalysisEngineFactory.createXmiWriter(testingWorkingDir, rawBase));
            }

            CollectionReaderDescription rawReader = CustomCollectionReaderFactory.createXmiReader(testingWorkingDir,
                    rawBase);

            logger.info("Preprocesisng the raw testing data.");
            prepare(taskConfig, testingWorkingDir, skipTestPrepare, rawReader);
        } else {
            logger.warn("No test readers specified.");
        }
    }

    public void prepareToProcess(Configuration taskConfig, CollectionReaderDescription reader)
            throws SAXException, UIMAException, CpeDescriptorException, IOException {
        boolean skipTestPrepare = taskConfig.getBoolean("edu.cmu.cs.lti.test.skip.preprocess", false);
        prepare(taskConfig, testingWorkingDir, skipTestPrepare, reader);
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
    private void prepare(Configuration taskConfig, String workingDirPath, boolean skipIfExists,
                         CollectionReaderDescription... inputReaders) throws
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
        final String opennlpModel = generalModelDir + "/opennlp/en-chunker.bin";
        final String mateModel = generalModelDir + "/mate/CoNLL2009-ST-Chinese-ALL.anna-3.3.srl-4.1.srl.model";
        final String zparChineseModel = generalModelDir + "/zpar/chinese";

        String os = System.getProperty("os.name").toLowerCase();
        final String zparBinPath;
        if (os.contains("mac")) {
            zparBinPath = generalModelDir + "/zpar/zpar-07-mac/dist/zpar.zh";
        } else if (os.contains("nix") || os.contains("nux")) {
            zparBinPath = generalModelDir + "/zpar/zpar-linux/dist/zpar.zh";
        } else {
            throw new NotImplementedException(String.format("Not implemented for OS %s.", os));
        }

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
                                    OpenNlpChunker.PARAM_MODEL_PATH, opennlpModel);
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
                        } else if (name.equals("mateChineseSrl")) {
                            processor = AnalysisEngineFactory.createEngineDescription(
                                    MateChineseSrlAnnotator.class, typeSystemDescription,
                                    MateChineseSrlAnnotator.PARAM_MODEL_FILE, mateModel
                            );
                        } else if (name.equals("zpar")) {
                            processor = AnalysisEngineFactory.createEngineDescription(
                                    ZParChineseCharacterConstituentParser.class, typeSystemDescription,
                                    ZParChineseCharacterConstituentParser.PARAM_CHINESE_MODEL, zparChineseModel,
                                    ZParChineseCharacterConstituentParser.PARAM_ZPAR_BIN_PATH, zparBinPath
                            );
                        } else if (name.equals("ltp")) {
                            processor = AnalysisEngineFactory.createEngineDescription(
                                    LtpAnnotator.class, typeSystemDescription,
                                    LtpAnnotator.PARAM_CWS_MODEL,
                                    new File(generalModelDir, "ltp_models/ltp_data/cws.model"),
                                    LtpAnnotator.PARAM_POS_MODEL,
                                    new File(generalModelDir, "ltp_models/ltp_data/pos.model"),
                                    LtpAnnotator.PARAM_NER_MODEL,
                                    new File(generalModelDir, "ltp_models/ltp_data/ner.model"),
                                    LtpAnnotator.PARAM_DEPENDENCY_MODEL,
                                    new File(generalModelDir, "ltp_models/ltp_data/parser.model"),
                                    LtpAnnotator.PARAM_SRL_MODEL,
                                    new File(generalModelDir, "ltp_models/ltp_data/srl")
                            );
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

        List<String> processors = new ArrayList<>();

        if (uniqueNames.size() > 0) {
            if (!uniqueNames.contains("corenlp")) {
                throw new ConfigurationException("Preprocessor [corenlp] is mandatory.");
            } else {
                uniqueNames.remove("corenlp");
            }
            processors.add("corenlp");
        }

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

    private CollectionReaderDescription annotateGoldMentions(CollectionReaderDescription reader, String mainDir,
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
                    return new AnalysisEngineDescription[]{
                            getGoldAnnotator(copyType, copyRealis, copyCluster, mergeSameSpan)
//                            getGoldAnnotator(true, true, true, false)
                    };
                }
            }, mainDir, baseOutput).runWithOutput();
        }
        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, baseOutput);
    }

    /**
     * Mention types are normally predicted as merged string. We need to do the splitting before evaluation.
     *
     * @param reader Annotation output.
     * @return
     */
    private CollectionReaderDescription splitMentions(CollectionReaderDescription reader, String mainDir,
                                                      String baseOutput) throws SAXException,
            UIMAException, CpeDescriptorException, IOException {
        return new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                        MentionTypeSplitter.class, typeSystemDescription
                );
                return new AnalysisEngineDescription[]{mentionSplitter};
            }
        }, mainDir, baseOutput).runWithOutput();
    }

    public String trainSentLvType(Configuration config, CollectionReaderDescription trainingReader,
                                  CollectionReaderDescription testReader, String suffix, boolean usePaTraing,
                                  String lossType, String processOutputDir, String subEvalDir, String goldStandard,
                                  int initialSeed, boolean skipTrain, boolean skipTest)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Starting training sentence level mention type model ...");

        String modelPath = ModelUtils.getTrainModelPath(eventModelDir, config, suffix, "loss=" + lossType);
        File modelFile = new File(modelPath);

        MutableInt trainingSeed = new MutableInt(initialSeed);
        int maxIter = config.getInt("edu.cmu.cs.lti.perceptron.maxiter", 20);
        int modelOutputFreq = config.getInt("edu.cmu.cs.lti.perceptron.model.save.frequency", 1);
        boolean ignoreUnannotated = config.getBoolean("edu.cmu.cs.lti.mention.ignore.empty.sentence", false);

        String classFile = FileUtils.joinPaths(trainingWorkingDir, "mention_types.txt");

        if (usePaTraing) {
            logger.info("Use PA with loss : " + lossType);
        }

        // Only skip training when model directory exists.
        if (skipTrain && modelFile.exists()) {
            logger.info("Skipping mention type training, taking existing models.");
        } else {
            logger.info("Model file " + modelFile + " not exists or no skipping, start training.");
            File cacheDir = new File(FileUtils.joinPaths(trainingWorkingDir, processOut,
                    config.get("edu.cmu.cs.lti.mention.cache.base")));

            AnalysisEngineDescription trainingEngine = AnalysisEngineFactory.createEngineDescription(
                    TokenLevelEventMentionCrfTrainer.class, typeSystemDescription,
                    TokenLevelEventMentionCrfTrainer.PARAM_GOLD_STANDARD_VIEW_NAME, UimaConst.goldViewName,
                    TokenLevelEventMentionCrfTrainer.PARAM_CONFIGURATION_FILE, config.getConfigFile(),
                    TokenLevelEventMentionCrfTrainer.PARAM_CLASS_FILE, classFile,
                    TokenLevelEventMentionCrfTrainer.PARAM_CACHE_DIRECTORY, cacheDir,
                    TokenLevelEventMentionCrfTrainer.PARAM_USE_PA_UPDATE, usePaTraing,
                    TokenLevelEventMentionCrfTrainer.PARAM_LOSS_TYPE, lossType,
                    TokenLevelEventMentionCrfTrainer.PARAM_IGNORE_UNANNOTATED_SENTENCE, ignoreUnannotated
            );

            TrainingLooper mentionTypeTrainer = new TrainingLooper(modelPath, trainingReader, trainingEngine,
                    maxIter, modelOutputFreq) {
                @Override
                protected boolean loopActions() {
                    boolean modelSaved = super.loopActions();

                    if (modelSaved && testReader != null) {
                        test(modelPath + "_iter" + numIteration, "token_mention_heldout_iter" + numIteration);
                    }

//                    if (numIteration > 3) {
//                        TokenLevelEventMentionCrfTrainer.TOGGLE_CHECK_UPDATE = true;
//                    }

                    trainingSeed.add(2);
                    logger.debug("Update the training seed to " + trainingSeed.intValue());
                    trainingReader.setAttributeValue(RandomizedXmiCollectionReader.PARAM_SEED, trainingSeed.getValue());

                    return modelSaved;
                }

                @Override
                protected void finish() throws IOException {
                    test(modelPath + "_iter" + numIteration, "token_mention_heldout_final");
                    TokenLevelEventMentionCrfTrainer.loopStopActions();
                }

                private void test(String model, String runName) {
                    if (testReader != null) {
                        try {
                            testPlainMentionModel(config, testReader, model, suffix, runName, processOutputDir,
                                    subEvalDir, goldStandard, skipTest);
                        } catch (SAXException | InterruptedException | IOException | CpeDescriptorException |
                                UIMAException e) {
                            e.printStackTrace();
                        }
                    }
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

    public CollectionReaderDescription sentenceLevelMentionTagging(Configuration crfConfig,
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
                            CrfMentionTypeAnnotator.PARAM_CONFIG, crfConfig.getConfigFile().getPath()
                    );

                    AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                            MentionTypeSplitter.class, typeSystemDescription
                    );

                    return new AnalysisEngineDescription[]{sentenceLevelTagger, mentionSplitter};
                }
            }, mainDir, baseOutput).runWithOutput();
        }
    }

    public String trainBeamTypeModel(Configuration config, CollectionReaderDescription trainingReader, String suffix,
                                     boolean usePaTraing, String lossType, boolean useLaSO, boolean delayedLaso,
                                     float aggressiveParameter, int initialSeed, CollectionReaderDescription testReader,
                                     String subEvalDir, String goldStandard, String processOutputDir, boolean skipTrain,
                                     boolean skipTest)
            throws UIMAException, NoSuchMethodException, IOException, InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {
        logger.info("Start training beam based type model.");

        String trainingStratey = "vanilla";

        if (useLaSO) {
            trainingStratey = "laso";
            if (delayedLaso) {
                trainingStratey = "delayedLaso";
            }
        } else {
            if (delayedLaso) {
                throw new IllegalArgumentException("Cannot do delayed LaSO without doing LaSO");
            }
        }

        String modelPath = ModelUtils.getTrainModelPath(eventModelDir, config, suffix,
                trainingStratey + "_loss=" + lossType);

        File modelFile = new File(modelPath);

        int maxIter = config.getInt("edu.cmu.cs.lti.perceptron.maxiter", 20);
        int modelOutputFreq = config.getInt("edu.cmu.cs.lti.perceptron.model.save.frequency", 1);
        int beamSize = config.getInt("edu.cmu.cs.lti.mention.beam.size", 5);

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
                    BeamBasedMentionTypeTrainer.PARAM_AGGRESSIVE_PARAMETER, aggressiveParameter,
                    BeamBasedMentionTypeTrainer.PARAM_TYPE_FILE_PATH,
                    FileUtils.joinPaths(trainingWorkingDir, "mention_types.txt")
            );

            MutableInt trainingSeed = new MutableInt(initialSeed);

            new TrainingLooper(modelPath, trainingReader, trainer, maxIter, modelOutputFreq) {
                @Override
                protected boolean loopActions() {
                    boolean modelSaved = super.loopActions();

                    if (modelSaved) {
                        test(modelPath + "_iter" + numIteration, "beam_mention_heldout_iter" + numIteration);
                    }

                    trainingSeed.add(2);
                    logger.debug("Update the training seed to " + trainingSeed.intValue());
                    trainingReader.setAttributeValue(RandomizedXmiCollectionReader.PARAM_SEED, trainingSeed.getValue());

                    return modelSaved;
                }

                @Override
                protected void finish() throws IOException {
                    test(modelPath, "beam_mention_heldout_final");
                }

                private void test(String model, String runName) {
                    try {
                        testPlainMentionModel(config, testReader, model, suffix, runName, processOutputDir,
                                subEvalDir, goldStandard, skipTest);
                    } catch (SAXException | InterruptedException | IOException | CpeDescriptorException |
                            UIMAException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    BeamBasedMentionTypeTrainer.saveModels(modelOutputDir);
                }
            }.runLoopPipeline();
        }
        return modelPath;
    }


    private CollectionReaderDescription beamMentionTagging(Configuration taskConfig, CollectionReaderDescription reader,
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

                    AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                            MentionTypeSplitter.class, typeSystemDescription
                    );

                    return new AnalysisEngineDescription[]{sentenceLevelTagger, mentionSplitter};
                }
            }, mainDir, baseOutput).runWithOutput();
        }
    }

    private String trainRealis(Configuration config, CollectionReaderDescription trainingReader, String suffix,
                               boolean skipTrain) throws Exception {
        String realisCvModelDir = ModelUtils.getTrainModelPath(eventModelDir, config, suffix);

        if (skipTrain && new File(realisCvModelDir).exists()) {
            logger.info("Skipping realis training, taking existing models: " + realisCvModelDir);
        } else {
            RealisClassifierTrainer trainer = new RealisClassifierTrainer(typeSystemDescription, trainingReader,
                    config);
            trainer.buildModels(realisCvModelDir);
        }

        return realisCvModelDir;
    }

    private CollectionReaderDescription realisAnnotation(Configuration taskConfig, CollectionReaderDescription reader,
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
                    AnalysisEngineDescription realisAnnotator;
                    if (modelDir == null) {
                        realisAnnotator = AnalysisEngineFactory.createEngineDescription(
                                AllActualRealisAnnotator.class, typeSystemDescription
                        );
                    } else {
                        realisAnnotator = AnalysisEngineFactory.createEngineDescription(
                                RealisTypeAnnotator.class, typeSystemDescription,
                                RealisTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                                RealisTypeAnnotator.PARAM_CONFIG_PATH, taskConfig.getConfigFile()
                        );
                    }
                    return new AnalysisEngineDescription[]{realisAnnotator};
                }
            }, mainDir, realisOutputBase).runWithOutput();
        }
    }


    public void computeStats() throws SAXException, UIMAException, CpeDescriptorException,
            IOException {
        logger.info("Computing statistics for the corpus into " + trainingWorkingDir);
        CollectionReaderDescription trainingReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, preprocessBase);
        CollectionReaderDescription trainingData = prepareTraining(trainingReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, fullRunSuffix, "prepared_training"), false, false, 1);

        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return trainingData;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription stats = AnalysisEngineFactory.createEngineDescription(
                        ChineseMentionStats.class, typeSystemDescription,
                        ChineseMentionStats.PARAM_OUTPUT_PATH, new File(trainingWorkingDir, "stats"));
                return new AnalysisEngineDescription[]{stats};
            }
        }).run();
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
                                                        String outputBase, boolean skipTrainPrepare, boolean mergeTypes,
                                                        int seed)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {

        File preparedDir = new File(workingDir, outputBase);
        if (preparedDir.exists() && skipTrainPrepare) {
            logger.info("Prepared training data exists at : " + preparedDir);
        } else {
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
                                      CollectionReaderDescription testReader, String suffix, boolean useLaSO,
                                      boolean delayedLaso, int beamSize, String outputDir, String subEvalDir,
                                      String gold, int initialSeed, boolean skipTrain, boolean skipTest) throws
            UIMAException, NoSuchMethodException, InstantiationException, IOException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        logger.info("Start beam based coreference training.");

        String name = String.format("coref_laso=%s_delayed=%s_beamSize=%d", useLaSO, delayedLaso, beamSize);

        String modelPath = ModelUtils.getTrainModelPath(eventModelDir, config, suffix, name);

        int maxIter = config.getInt("edu.cmu.cs.lti.perceptron.maxiter", 15);
        int modelOutputFreq = config.getInt("edu.cmu.cs.lti.perceptron.model.save.frequency", 1);

        boolean modelExists = new File(modelPath).exists();
        if (skipTrain && modelExists) {
            logger.info("Skipping training, taking existing models.");
        } else {
            logger.info("Saving model directory at : " + modelPath);
            AnalysisEngineDescription corefEngine = AnalysisEngineFactory.createEngineDescription(
                    BeamBasedCorefTrainer.class, typeSystemDescription,
                    BeamBasedCorefTrainer.PARAM_CONFIGURATION_FILE, config.getConfigFile().getPath(),
                    BeamBasedCorefTrainer.PARAM_DELAYED_LASO, delayedLaso,
                    BeamBasedCorefTrainer.PARAM_BEAM_SIZE, beamSize,
                    BeamBasedCorefTrainer.PARAM_USE_LASO, useLaSO
            );

            TrainingLooper corefTrainer = new TrainingLooper(modelPath, trainingReader, corefEngine, maxIter,
                    modelOutputFreq) {
                @Override
                protected boolean loopActions() {
                    boolean modelSaved = super.loopActions();
                    if (modelSaved && testReader != null) {
                        test(modelPath + "_iter" + numIteration, "beam_coref_heldout_iter" + numIteration);
                    }
                    return modelSaved;
                }

                @Override
                protected void finish() throws IOException {
                    if (testReader != null) {
                        test(modelPath, "beam_coref_heldout_final");
                    }
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    BeamBasedCorefTrainer.saveModels(modelOutputDir);
                }

                private void test(String model, String runName) {
                    try {
                        testBeamCoref(config, testReader, model, suffix, runName,
                                outputDir, subEvalDir, gold, skipTest);
                    } catch (SAXException | UIMAException | IOException | InterruptedException |
                            CpeDescriptorException e) {
                        e.printStackTrace();
                    }
                }
            };

            corefTrainer.runLoopPipeline();
        }
        return modelPath;
    }

    /**
     * Train the latent tree model coreference resolver.
     *
     * @param config         The configuration file.
     * @param trainingReader Reader for the training data.
     * @param testReader     Reader for the test data.
     * @param suffix         The suffix for the model.
     * @param initialSeed    The initial seed for the reader.
     * @param skipTrain      Whether to skip the training if model file exists.
     * @return The trained model directory.
     */
    private String trainLatentTreeCoref(Configuration config, CollectionReaderDescription trainingReader,
                                        CollectionReaderDescription testReader, String suffix, String outputDir,
                                        String subEvalDir, String gold, int initialSeed, boolean skipTrain,
                                        boolean skipTest)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Start coreference training.");

        String modelPath = ModelUtils.getTrainModelPath(eventModelDir, config, suffix);

        int maxIter = config.getInt("edu.cmu.cs.lti.perceptron.maxiter", 15);
        int modelOutputFreq = config.getInt("edu.cmu.cs.lti.perceptron.model.save.frequency", 1);

        boolean modelExists = new File(modelPath).exists();
        if (skipTrain && modelExists) {
            logger.info("Skipping training, taking existing models.");
        } else {
            logger.info("Saving model directory at : " + modelPath);
            String cacheDir = FileUtils.joinPaths(trainingWorkingDir, processOut,
                    config.get("edu.cmu.cs.lti.coref.cache.base"));
            AnalysisEngineDescription corefEngine = AnalysisEngineFactory.createEngineDescription(
                    PaLatentTreeTrainer.class, typeSystemDescription,
                    PaLatentTreeTrainer.PARAM_CONFIG_PATH, config.getConfigFile().getPath(),
                    PaLatentTreeTrainer.PARAM_CACHE_DIRECTORY, cacheDir
            );

            TrainingLooper corefTrainer = new TrainingLooper(modelPath, trainingReader, corefEngine, maxIter,
                    modelOutputFreq) {
                @Override
                protected boolean loopActions() {
                    boolean modelSaved = super.loopActions();
                    if (modelSaved && testReader != null) {
                        test(modelPath + "_iter" + numIteration, "coref_heldout_iter" + numIteration);
                    }
                    return modelSaved;
                }

                @Override
                protected void finish() throws IOException {
                    PaLatentTreeTrainer.finish();
                    test(modelPath, "coref_heldout_final");
                }

                private void test(String model, String runName) {
                    if (testReader != null) {
                        try {
                            testCoref(config, testReader, model, suffix, runName, outputDir, subEvalDir, gold,
                                    skipTest);
                        } catch (SAXException | UIMAException | IOException | InterruptedException |
                                CpeDescriptorException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    PaLatentTreeTrainer.saveModels(modelOutputDir);
                }
            };

            corefTrainer.runLoopPipeline();
        }
        return modelPath;
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
                                                            boolean skipCorefTest, int beamSize)
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
                            BeamEventCorefAnnotator.PARAM_BEAM_SIZE, beamSize
                    );

                    AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                            MentionTypeSplitter.class, typeSystemDescription
                    );

                    List<AnalysisEngineDescription> annotators = new ArrayList<>();
                    addCorefPreprocessors(annotators);
//                    annotators.add(mentionSplitter);
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

    private void testJoint(Configuration config, CollectionReaderDescription devReader, String modelDir,
                           String runName, int beamSize, String realisModelDir, boolean skipTest, String workingDir,
                           String processOutputDir, String subEvalDir, String goldStandard, String sliceSuffix)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        new ModelTester("joint") {
            @Override
            CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return beamJointSpanCoref(config, devReader, modelDir, realisModelDir, trainingWorkingDir,
                        processOutputDir, beamSize, true, skipTest);
            }
        }.run(config, devReader, sliceSuffix, runName, processOutputDir, subEvalDir, goldStandard);

    }

    public String trainJointSpanModel(Configuration config, CollectionReaderDescription trainReader,
                                      CollectionReaderDescription devReader, String realisModelDir,
                                      String processOutputDir, String sliceSuffix,
                                      String devGold, boolean skipTrain, boolean skipTest,
                                      String mentionLossType, int beamSize, int constraintType)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Start beam based joint training.");
        String cvModelDir = ModelUtils.getTrainModelPath(eventModelDir, config, sliceSuffix,
                String.format("loss=%s_beamSize=%d_con=%d", mentionLossType, beamSize, constraintType));

        String subEvalDir = sliceSuffix.equals(fullRunSuffix) ? "final" : "cv";

        File cacheDir = new File(FileUtils.joinPaths(trainingWorkingDir, processOut,
                config.get("edu.cmu.cs.lti.joint.cache.base")));

        int jointMaxIter = config.getInt("edu.cmu.cs.lti.perceptron.joint.maxiter", 30);
        int modelOutputFreq = config.getInt("edu.cmu.cs.lti.perceptron.model.save.frequency", 1);

        boolean useTwoLayer = config.getBoolean("edu.cmu.cs.lti.joint.two.layer", false);

        boolean modelExists = new File(cvModelDir).exists();

        int warmStartIter = config.getInt("edu.cmu.cs.lti.joint.warm.iter", 5);
        String warmStartModelDir = cvModelDir + "_iter" + warmStartIter;

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
                    BeamJointTrainer.PARAM_WARM_START_MENTION_MODEL, warmStartModelDir,
                    BeamJointTrainer.PARAM_STRATEGY_TYPE, constraintType,
                    BeamJointTrainer.PARAM_CACHE_DIR, cacheDir,
                    BeamJointTrainer.PARAM_TWO_LAYER, useTwoLayer,
                    BeamJointTrainer.PARAM_WARM_START_ITER, warmStartIter
            );

            TrainingLooper trainer = new TrainingLooper(cvModelDir, trainReader, trainEngine, jointMaxIter,
                    modelOutputFreq) {
                @Override
                protected boolean loopActions() {
                    boolean modelSaved = super.loopActions();
                    BeamJointTrainer.loopAction();

                    if (modelSaved) {
                        try {
                            String runName = "joint_heldout_iter" + numIteration;
                            String modelPath = cvModelDir + "_iter" + numIteration;
                            testJoint(config, devReader, modelPath, runName, beamSize, realisModelDir, skipTest,
                                    trainingWorkingDir, processOutputDir, subEvalDir, devGold, sliceSuffix);
                        } catch (SAXException | InterruptedException | IOException | CpeDescriptorException |
                                UIMAException e) {
                            e.printStackTrace();
                        }
                    }
                    return modelSaved;
                }

                @Override
                protected void finish() throws IOException {
                    BeamJointTrainer.finish();
                    try {
                        // Test using the final model.
                        String runName = "joint_heldout_final";
                        String modelPath = cvModelDir + "_iter" + numIteration;

                        testJoint(config, devReader, modelPath, runName, beamSize, realisModelDir, skipTest,
                                trainingWorkingDir, processOutputDir, subEvalDir, devGold, sliceSuffix);
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

        boolean useTwoLayer = config.getBoolean("edu.cmu.cs.lti.joint.two.layer", false);

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
                            JointMentionCorefAnnotator.PARAM_USE_LASO, useLaso,
                            JointMentionCorefAnnotator.PARAM_TWO_LAYER, useTwoLayer
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

    private void eval(String gold, String system, String subDir, String runName, String suffix, String typesPath)
            throws IOException, InterruptedException {
        boolean useSelectedType = typesPath != null;

        String evalDir;

        if (useSelectedType) {
            String typeName = FilenameUtils.removeExtension(FilenameUtils.getBaseName(typesPath));
            evalDir = FileUtils.joinPaths(evalLogOutputDir, subDir, suffix, typeName, runName);
        } else {
            evalDir = FileUtils.joinPaths(evalLogOutputDir, subDir, suffix, "main", runName);
        }

        String evalLog = FileUtils.joinPaths(evalDir, "scoring_log.txt");
        File file = new File(evalLog);
        file.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(file);

        logger.info("Evaluating with " + evalScript + ", saving results to " + evalDir);
        logger.info("Gold file is " + gold);
        logger.info("System file is " + system);

        List<String> commands = new ArrayList<>(Arrays.asList(
                "python", evalScript, "-g", gold, "-s", system,
                "-d", FileUtils.joinPaths(evalDir, suffix + ".cmp"),
                "-o", FileUtils.joinPaths(evalDir, suffix + ".scores"),
                "-c", FileUtils.joinPaths(evalDir, suffix + ".coref_out"),
                "--eval_mode", evalMode
        ));

        if (evalMode.equals("token")) {
            commands.add("-t");
            commands.add(tokenDir);
        }

        if (useSelectedType) {
            commands.add("-wl");
            commands.add(typesPath);
        }

        ProcessBuilder pb = new ProcessBuilder(commands.toArray(new String[commands.size()]));

//        for (String s : pb.command()) {
//            System.out.println(s);
//        }

        Process p = pb.start();

        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            writer.write(line);
            writer.write("\n");
        }

        Thread thread = new Thread(() -> {
            try {
                p.waitFor();
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.start();
//        thread.join(300000); // 300 seconds
    }

    private void writeResults(CollectionReaderDescription processedResultReader, String tbfOutput, String systemId)
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

        // When training is not skipped, testing must be performed.
        boolean skipTypeTest = taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptest", false) && skipTypeTrain;
        boolean skipRealisTest = taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptest", false) &&
                skipRealisTrain;
        boolean skipCorefTest = taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptest", false) && skipCorefTrain;

        boolean skipTrainPrepare = taskConfig.getBoolean("edu.cmu.cs.lti.train.skip.prepare", false);

        Configuration realisConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.realis"));
        Configuration tokenCrfConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.token_crf"));
        Configuration corefConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.coreference"));

        int seed = taskConfig.getInt("edu.cmu.cs.lti.random.seed", 17);

        CollectionReaderDescription trainReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, preprocessBase);

        CollectionReaderDescription testReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, testingWorkingDir, preprocessBase);

        String processDir = FileUtils.joinPaths(testingWorkingDir, evalBase, "full_run");

        CollectionReaderDescription trainingData = prepareTraining(trainReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, fullRunSuffix, "prepared_training"), skipTrainPrepare, false, seed);

        // Train realis model.
        String realisModelDir = trainRealis(realisConfig, trainingData, fullRunSuffix, skipRealisTrain);

        String vanillaSentCrfModel = trainSentLvType(tokenCrfConfig, trainingData, testReader, fullRunSuffix, false,
                "hamming", processDir, null, null, seed, skipTypeTrain, skipTypeTest);

        // Train coref model.
        String treeCorefModel = trainLatentTreeCoref(corefConfig, trainingData, testReader, fullRunSuffix,
                processDir, null, null, seed, skipCorefTrain, skipCorefTest);

        // Run the vanilla model.
        runOnly(tokenCrfConfig, realisConfig, corefConfig, testReader, vanillaSentCrfModel, realisModelDir,
                treeCorefModel, fullRunSuffix, "vanillaMention", processDir, skipTypeTest, skipRealisTest,
                skipCorefTest);
    }

    public void trainTest(Configuration taskConfig, boolean runAll) throws Exception {
        int seed = taskConfig.getInt("edu.cmu.cs.lti.random.seed", 17);

        CollectionReaderDescription trainingReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, preprocessBase);

        CollectionReaderDescription testDataReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, testingWorkingDir, preprocessBase);

        String evalDir = FileUtils.joinPaths(testingWorkingDir, evalBase, "full_run");

        if (taskConfig.getBoolean("edu.cmu.cs.lti.individual.models", true)) {
            logger.info("Will run individual model experiments.");
            experiment(taskConfig, fullRunSuffix, trainingReader, testDataReader, evalDir, false, seed, runAll);
        }

        if (taskConfig.getBoolean("edu.cmu.cs.lti.joint.models", true)) {
            logger.info("Will run joint model experiments.");
            jointExperiment(taskConfig, fullRunSuffix, trainingReader, testDataReader, evalDir, seed, runAll);
        }
    }

    public void tryAnnotator(Configuration taskConfig) throws SAXException, UIMAException, CpeDescriptorException,
            IOException {

        String baseResourcePath = taskConfig.get("edu.cmu.cs.lti.model.dir");

        AnalysisEngineDescription ltpAnnotator = AnalysisEngineFactory.createEngineDescription(
                LtpAnnotator.class, typeSystemDescription,
                LtpAnnotator.PARAM_CWS_MODEL, new File(baseResourcePath, "ltp_models/ltp_data/cws.model"),
                LtpAnnotator.PARAM_POS_MODEL, new File(baseResourcePath, "ltp_models/ltp_data/pos.model"),
                LtpAnnotator.PARAM_NER_MODEL, new File(baseResourcePath, "ltp_models/ltp_data/ner.model"),
                LtpAnnotator.PARAM_DEPENDENCY_MODEL, new File(baseResourcePath, "ltp_models/ltp_data/parser.model"),
                LtpAnnotator.PARAM_SRL_MODEL, new File(baseResourcePath, "ltp_models/ltp_data/srl")
        );

        CollectionReaderDescription trainReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, preprocessBase);
        CollectionReaderDescription testReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, testingWorkingDir, preprocessBase);

        AnalysisEngineDescription[] processors = new AnalysisEngineDescription[]{ltpAnnotator};

        File trainingPreprocessed = new File(trainingWorkingDir, preprocessBase);
        if (trainingPreprocessed.exists()) {
            logger.info("Trying on training data.");
            new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return trainReader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    return processors;
                }
            }, trainingWorkingDir, trialBase).runWithOutput();
        } else {
            logger.info(String.format("Training preprocessed data [%s] not found, cannot try annotator",
                    trainingPreprocessed));
        }


        File testPreprocessed = new File(testingWorkingDir, preprocessBase);
        if (testPreprocessed.exists()) {
            logger.info("Trying on test data.");
            new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return testReader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    return processors;
                }
            }, testingWorkingDir, trialBase).runWithOutput();
        } else {
            logger.info(String.format("Test preprocessed data [%s] not found, cannot try annotator",
                    testPreprocessed));
        }
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

            if (taskConfig.getBoolean("edu.cmu.cs.lti.individual.models", true)) {
                logger.info("Will run individual model experiments.");
                experiment(taskConfig, sliceSuffix, trainingSliceReader, devSliceReader, crossEvalDir, true, seed,
                        false);
            }
            if (taskConfig.getBoolean("edu.cmu.cs.lti.joint.models", true)) {
                logger.info("Will run joint model experiments.");
                jointExperiment(taskConfig, sliceSuffix, trainingSliceReader, devSliceReader, crossEvalDir, seed,
                        false);
            }
        }
    }

    private String runOnly(Configuration tokenCrfConfig, Configuration realisConfig, Configuration corefConfig,
                           CollectionReaderDescription reader, String typeModel, String realisModel, String corefModel,
                           String sliceSuffix, String runName, String outputDir, boolean skipType, boolean skipRealis,
                           boolean skipCoref)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        logger.info(String.format("Type model is %s, Realis Model is %s, Coref Model is %s.", typeModel, realisModel,
                typeModel));

        String annotatedOutput = FileUtils.joinPaths(middleResults, sliceSuffix, runName);

        CollectionReaderDescription mentionOutput = sentenceLevelMentionTagging(tokenCrfConfig, reader, typeModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "mention"), skipType);

        CollectionReaderDescription realisOutput = realisAnnotation(realisConfig, mentionOutput, realisModel,
                trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "realis"), skipType && skipRealis);

        CollectionReaderDescription corefSentMentions = corefResolution(corefConfig,
                realisOutput, corefModel, trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "coref"),
                skipType && skipRealis && skipCoref);

        String tbfOutput = FileUtils.joinPaths(outputDir, sliceSuffix, runName + ".tbf");
        writeResults(corefSentMentions, tbfOutput, runName);

        return tbfOutput;
    }

    private abstract class ModelTester {
        private String modelName;

        ModelTester(String modelName) {
            this.modelName = modelName;
        }

        /**
         * @param taskConfig
         * @param reader
         * @param sliceSuffix
         * @param runName
         * @param outputDir
         * @param subEval
         * @param gold
         * @return
         * @throws SAXException
         * @throws UIMAException
         * @throws CpeDescriptorException
         * @throws IOException
         * @throws InterruptedException
         */
        CollectionReaderDescription run(Configuration taskConfig, CollectionReaderDescription reader,
                                        String sliceSuffix, String runName, String outputDir, String subEval,
                                        String gold)
                throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
            logger.info(String.format("Running model %s", modelName));

            String annotatedOutput = FileUtils.joinPaths(middleResults, sliceSuffix, runName, modelName);

            CollectionReaderDescription output = runModel(taskConfig, reader, trainingWorkingDir, annotatedOutput);

            String tbfOutput = FileUtils.joinPaths(outputDir, sliceSuffix, modelName, runName + ".tbf");
            writeResults(output, tbfOutput, runName);

            if (gold != null) {
                logger.info("Evaluating over all event mentions.");
                eval(gold, tbfOutput, subEval, runName, sliceSuffix, null);
                String selectedTypePath = taskConfig.get("edu.cmu.cs.lti.eval.selected_type.file");
                if (selectedTypePath != null) {
                    logger.info("Evaluating on selected event types.");
                    eval(gold, tbfOutput, subEval, runName, sliceSuffix, selectedTypePath);
                }
            }
            return output;
        }

        abstract CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription reader,
                                                      String mainDir, String baseDir)
                throws SAXException, UIMAException, CpeDescriptorException, IOException;
    }

    /**
     * Test the token based mention model and return the result directory as a reader
     */
    private CollectionReaderDescription testPlainMentionModel(Configuration taskConfig,
                                                              CollectionReaderDescription reader, String typeModel,
                                                              String sliceSuffix, String runName, String outputDir,
                                                              String subEval, String gold, boolean skipTest)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        return new ModelTester("token_based_mention") {
            @Override
            CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException,
                    CpeDescriptorException, IOException {
                return sentenceLevelMentionTagging(taskConfig, reader, typeModel,
                        trainingWorkingDir, baseDir, skipTest);
            }
        }.run(taskConfig, reader, sliceSuffix, runName, outputDir, subEval, gold);
    }

    private CollectionReaderDescription testRealis(Configuration taskConfig, CollectionReaderDescription reader,
                                                   String realisModel, String sliceSuffix, String runName,
                                                   String outputDir, String subEval, String gold, boolean skipTest)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {

        return new ModelTester("realis_model") {
            @Override
            CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return realisAnnotation(taskConfig, reader, realisModel, trainingWorkingDir, baseDir, skipTest);
            }
        }.run(taskConfig, reader, sliceSuffix, runName, outputDir, subEval, gold);
    }

    private CollectionReaderDescription testBeamMentionModels(Configuration config, CollectionReaderDescription reader,
                                                              String typeModel, String sliceSuffix, String runName,
                                                              String outputDir, String subEval, String gold,
                                                              boolean skipTest)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        int beamSize = config.getInt("edu.cmu.cs.lti.mention.beam.size", 5);

        return new ModelTester("beam_model") {
            @Override
            CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException,
                    CpeDescriptorException, IOException {
                return beamMentionTagging(taskConfig, reader, typeModel, trainingWorkingDir, baseDir, beamSize,
                        skipTest);
            }
        }.run(config, reader, sliceSuffix, runName, outputDir, subEval, gold);
    }

    private CollectionReaderDescription testCoref(Configuration taskConfig,
                                                  CollectionReaderDescription reader, String corefModel,
                                                  String sliceSuffix, String runName, String outputDir,
                                                  String subEval, String gold, boolean skipTest)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        return new ModelTester("treeCoref") {
            @Override
            CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return corefResolution(taskConfig, reader, corefModel, trainingWorkingDir, baseDir, skipTest);
            }
        }.run(taskConfig, reader, sliceSuffix, runName, outputDir, subEval, gold);
    }

    private CollectionReaderDescription testBeamCoref(Configuration config, CollectionReaderDescription reader,
                                                      String corefModel, String sliceSuffix, String runName,
                                                      String outputDir, String subEval, String gold, boolean skipTest)
            throws InterruptedException, SAXException, UIMAException, CpeDescriptorException, IOException {
        return new ModelTester("beamCoref") {
            @Override
            CollectionReaderDescription runModel(Configuration config, CollectionReaderDescription reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return beamCorefResolution(config, reader, corefModel, trainingWorkingDir, baseDir, skipTest,
                        config.getInt("edu.cmu.cs.lti.coref.beam.size", 5)
                );
            }
        }.run(config, reader, sliceSuffix, runName, outputDir, subEval, gold);
    }

    private void tokenMentionErrorAnalysis(Configuration taskConfig,
                                           CollectionReaderDescription reader, String tokenModel) throws
            SAXException, UIMAException, CpeDescriptorException, IOException {
        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription analyzer = AnalysisEngineFactory.createEngineDescription(
                        TokenBasedMentionErrorAnalyzer.class, typeSystemDescription,
                        TokenBasedMentionErrorAnalyzer.PARAM_MODEL_DIRECTORY, tokenModel,
                        TokenBasedMentionErrorAnalyzer.PARAM_CONFIG, taskConfig.getConfigFile().getPath()
                );

                return new AnalysisEngineDescription[]{
                        analyzer
                };
            }
        }).run();
    }


    private void jointExperiment(Configuration taskConfig, String sliceSuffix, CollectionReaderDescription trainReader,
                                 CollectionReaderDescription devReader, String processOutDir, int seed, boolean runAll
    ) throws Exception {
        boolean skipRealisTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain", false);
        boolean skipJointTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptrain", false);
        boolean skipJointTest = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptest", false);
        int jointBeamSize = taskConfig.getInt("edu.cmu.cs.lti.joint.beam.size", 5);

        Configuration realisConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.realis"));
        Configuration jointConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.joint"));

        String[] lossTypes = jointConfig.getList("edu.cmu.cs.lti.mention.loss_types");

        boolean skipTrainPrepare = taskConfig.getBoolean("edu.cmu.cs.lti.train.skip.prepare", false);

        logger.info("Ready to prepare training data.");
        CollectionReaderDescription trainingData = prepareTraining(trainReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "prepared_training"), skipTrainPrepare, false, seed);
        logger.info("Done prepare training data.");

        // Produce gold standard tbf for evaluation.
        String devGold = FileUtils.joinPaths(processOutDir, "gold_test_" + sliceSuffix + ".tbf");
        writeGold(devReader, devGold);

//        String trainGold = FileUtils.joinPaths(processOutDir, "gold_train_" + sliceSuffix + ".tbf");
//        writeGold(trainingData, trainGold);

        // Train realis model.
        String realisModelDir = trainRealis(realisConfig, trainingData, sliceSuffix, skipRealisTrain);

        // We try experiments with different settings of the following:
        // 1. Loss type
        // 2. Beam size
        // 3. Coreference strategy
        // The model name will be based on these settings.
        for (int i = 0; i < lossTypes.length; i++) {
            String lossType = lossTypes[i];

            for (int strategy = 1; strategy <= 1; strategy++) {
                trainJointSpanModel(jointConfig, trainingData, devReader, realisModelDir, processOutDir,
                        sliceSuffix, devGold, skipJointTrain, skipJointTest, lossType, jointBeamSize, strategy
                );
            }
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

    /**
     * Run two simple downstream tasks after mention detection, to check the performance of mention models.
     */
    private void mentionDownstream(Configuration realisConfig, Configuration corefConfig,
                                   CollectionReaderDescription mentionOutput, String sliceSuffix, String processOutDir,
                                   String subEvalDir, String testGold, boolean skipRealisTest, boolean skipCorefTest)
            throws InterruptedException, SAXException, UIMAException, CpeDescriptorException, IOException {
        String realisModel = ModelUtils.getTestModelFile(eventModelDir, realisConfig, sliceSuffix);
        String treeCorefModel = ModelUtils.getTestModelFile(eventModelDir, corefConfig, sliceSuffix);

        CollectionReaderDescription realisOutput = testRealis(realisConfig, mentionOutput, realisModel,
                sliceSuffix, "realis", processOutDir, subEvalDir, testGold, skipRealisTest);

        testCoref(corefConfig, realisOutput, treeCorefModel, sliceSuffix, "treeCoref", processOutDir, subEvalDir,
                testGold, skipCorefTest);
    }

    private Configuration getModelConfig(String modelConfigName) throws IOException {
        return new Configuration(new File(modelConfigDir, modelConfigName + ".properties"));
    }

    private void experiment(Configuration taskConfig, String sliceSuffix, CollectionReaderDescription trainReader,
                            CollectionReaderDescription testReader, String processOutDir, boolean hasTestGold,
                            int seed, boolean runAll) throws Exception {
        if (runAll) {
            logger.info("Force to run all experiments, no training will be skipped.");
        }

//        logger.info("Testing write gold");
//        writeGold(testReader, "temp");
//        logger.info("Test copy");
//        annotateGoldMentions(testReader, trainingWorkingDir,
//                FileUtils.joinPaths(middleResults, sliceSuffix, "gold_mentions"), true, true, false, true, false
//                    /* copy type, realis, not coref, merge types, skip*/);
//        DebugUtils.pause();


        boolean skipCorefTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptrain", false);
        boolean skipTypeTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);
        boolean skipRealisTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain", false);

        boolean skipTypeTest = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptest", false);
        boolean skipRealisTest = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptest", false);
        boolean skipCorefTest = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptest", false);

        boolean hasRealis = taskConfig.getBoolean("edu.cmu.cs.lti.mention.has_realis", true);

        logger.info(String.format("Training skips, skip mention : %s, skip realis : %s, skip coref : %s.",
                skipTypeTrain, skipRealisTrain, skipCorefTrain));

        logger.info(String.format("Testing skips, skip mention : %s, skip realis : %s, skip coref : %s.",
                skipTypeTest, skipRealisTest, skipCorefTrain));

        Configuration realisConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.realis"));
        Configuration tokenCrfConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.token_crf"));
//        Configuration beamTokenCrfConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.beam.crf"));
        Configuration corefConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.coreference"));

        String subEvalDir = sliceSuffix.equals(fullRunSuffix) ? "final" : "cv";

        boolean skipTrainPrepare = taskConfig.getBoolean("edu.cmu.cs.lti.train.skip.prepare", false);

        CollectionReaderDescription trainingData = prepareTraining(trainReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "prepared_training"), skipTrainPrepare,
                false /*Not merging*/, seed);

        // Produce gold standard tbf for evaluation.
        String testGold = null;
        if (hasTestGold) {
            testGold = FileUtils.joinPaths(processOutDir, "gold_test_" + sliceSuffix + ".tbf");
            writeGold(testReader, testGold);
        }

        String trainGold = FileUtils.joinPaths(processOutDir, "gold_train_" + sliceSuffix + ".tbf");
        writeGold(trainingData, trainGold);

        /*####################################################
         * Begin of gold standard benchmarks.
         ###################################################*/
        // Produce gold mention (type + realis) detection.
        logger.info("Producing partial gold standards.");
        CollectionReaderDescription goldMentionAll = annotateGoldMentions(testReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "gold_mentions"), true, true, false, true, false
                    /* copy type, realis, not coref, merge types, skip*/);

        // Gold mention types.
        CollectionReaderDescription goldMentionTypes = annotateGoldMentions(testReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "gold_type"), true, false, false, false, false
                    /* copy type, not realis, not coref, not merging types, skip*/);

        String realisModelDir = null;
        if (hasRealis) {
            // Train realis model.
            realisModelDir = trainRealis(realisConfig, trainingData, sliceSuffix, skipRealisTrain);
            testRealis(realisConfig, goldMentionTypes, realisModelDir,
                    sliceSuffix, "gold_mention_realis", processOutDir, subEvalDir, testGold, skipRealisTest);
        }

        /*####################################################
         * End of gold standard benchmarks.
         ###################################################*/

        /*#################################################
         * BEGIN of the Vanilla perceptron models training:
         ################################################*/
        // The vanilla crf model.
        String vanillaTypeModel = trainSentLvType(tokenCrfConfig, trainingData, testReader, sliceSuffix, false,
                "hamming", processOutDir, subEvalDir, testGold, seed, skipTypeTrain, skipTypeTest);

//        tokenMentionErrorAnalysis(tokenCrfConfig, testReader, vanillaTypeModel);

        // The vanilla coref model.
        String treeCorefModel = trainLatentTreeCoref(corefConfig, trainingData, goldMentionAll, sliceSuffix,
                processOutDir, subEvalDir, testGold, seed, skipCorefTrain, skipTypeTest && skipCorefTest);

        /*#################################################
         * END of the Vanilla perceptron models training:
         ################################################*/


//        /*#################################################
//         * Begin of training of crf mention models.
//         ################################################*/
//        String[] lossTypes = tokenCrfConfig.getList("edu.cmu.cs.lti.mention.loss_types");
//        String[] paMentionModels = new String[lossTypes.length];
//        String[] paBeamLaSOMentionModels = new String[lossTypes.length];
//        String[] delayedPaBeamMentionModels = new String[lossTypes.length];
//
//        // Train beam based crf models.
//        for (int i = 0; i < lossTypes.length; i++) {
//            String lossType = lossTypes[i];
//
//            // PA, LaSO, delayed.
//            delayedPaBeamMentionModels[i] = trainBeamTypeModel(beamTokenCrfConfig, trainingData, sliceSuffix, true,
//                    lossType, true, true, 0, seed, testReader, subEvalDir, testGold, processOut, skipTypeTrain,
//                    skipTypeTest);
//
//            // PA, LaSO, but not delayed.
//            paBeamLaSOMentionModels[i] = trainBeamTypeModel(beamTokenCrfConfig, trainingData, sliceSuffix, true,
//                    lossType, true, false, 0, seed, testReader, subEvalDir, testGold, processOut, skipTypeTrain,
//                    skipTypeTest);
//
//            // PA, no beam search.
//            paMentionModels[i] = trainSentLvType(tokenCrfConfig, trainingData, testReader, sliceSuffix, true,
//                    lossType, processOutDir, subEvalDir, testGold, seed, skipTypeTrain, skipTypeTest);
//        }
//
//
//        // A beam search model that do not use PA loss.
//        String vanillaBeamMentionModel = trainBeamTypeModel(beamTokenCrfConfig, trainingData, sliceSuffix, false,
//                "hamming", false, false, 0, seed, testReader, subEvalDir, testGold, processOut, skipTypeTrain,
//                skipTypeTest);
//        String laSoBeamMentionModel = trainBeamTypeModel(beamTokenCrfConfig, trainingData, sliceSuffix, false,
//                "hamming", true, false, 0, seed, testReader, subEvalDir, testGold, processOut, skipTypeTrain,
//                skipTypeTest);
//
//        /*#################################################
//         * End of training of beam crf mention models.
//         ################################################*/
//
//
//        /*#################################################
//         * BEGIN OF the Beam coreference models training:
//         ################################################*/
//        // Train beamed based coref model.
//        Map<String, String> beamCorefModels = new HashMap<>();
//
//        int[] sizes = {5};
//        boolean[] useLaSO = {true}; // When using local features, whether to use LaSO is equivalent.
//        boolean[] delayed = {true};
//
//        for (int size : sizes) {
//            for (boolean laso : useLaSO) {
//                for (boolean d : delayed) {
//                    if (!laso && !d) {
//                        // When not using LaSO, delayed doesn't matter.
//                        continue;
//                    }
//                    String name = String.format("coref_laso=%s_delayed=%s_beamSize=%d", laso, d, size);
//
//                    String beamCoref = trainBeamBasedCoref(corefConfig, trainingData, goldMentionAll,
//                            sliceSuffix, laso, d, size, processOutDir, subEvalDir, testGold, seed, skipCorefTrain,
//                            skipCorefTest);
//                    beamCorefModels.put(name, beamCoref);
//                }
//            }
//        }
//        /*#################################################
//         * End of the Beam coreference models training
//         ################################################*/
//
//         /*#################################################
//         * BEGIN OF the mention experiments.
//         ################################################*/
//
//        //  Run the mention models, followed by the other trained models, to see the effect of mention models to the
//        // final goal.
//
//        mentionDownstream(realisConfig, corefConfig,
//                testBeamMentionModels(beamTokenCrfConfig, testReader, vanillaBeamMentionModel, sliceSuffix,
//                        "vanillaBeamMention", processOutDir, subEvalDir, testGold, runAll),
//                sliceSuffix, processOutDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//        );
//
//        mentionDownstream(realisConfig, corefConfig,
//                testBeamMentionModels(beamTokenCrfConfig, testReader, laSoBeamMentionModel, sliceSuffix,
//                        "noPaLasoBeamMention", processOutDir, subEvalDir, testGold, runAll),
//                sliceSuffix, processOutDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//        );
//
//        mentionDownstream(realisConfig, corefConfig,
//                testBeamMentionModels(beamTokenCrfConfig, testReader, laSoBeamMentionModel, sliceSuffix,
//                        "noPaLasoBeamMention", processOutDir, subEvalDir, testGold, runAll),
//                sliceSuffix, processOutDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//        );
//
//        for (int i = 0; i < lossTypes.length; i++) {
//            String lossType = lossTypes[i];
//
//            mentionDownstream(realisConfig, corefConfig,
//                    testPlainMentionModel(tokenCrfConfig, testReader, paMentionModels[i], sliceSuffix, "paMention_" +
//                            lossType, processOutDir, subEvalDir, testGold, skipTypeTest),
//                    sliceSuffix, processOutDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//            );
//
//            mentionDownstream(realisConfig, corefConfig,
//                    testBeamMentionModels(beamTokenCrfConfig, testReader, paBeamLaSOMentionModels[i], sliceSuffix,
//                            "laSoBeamMention_" + lossType, processOutDir, subEvalDir, testGold, runAll),
//                    sliceSuffix, processOutDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//            );
//
//
//            mentionDownstream(realisConfig, corefConfig,
//                    testBeamMentionModels(beamTokenCrfConfig, testReader, delayedPaBeamMentionModels[i], sliceSuffix,
//                            "laSoBeamMention_" + lossType, processOutDir, subEvalDir, testGold, runAll),
//                    sliceSuffix, processOutDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//            );
//        }
//        /*#################################################
//         * END of the mention experiments.
//         ################################################*/

         /*#################################################
         * BEGIN to use test coreference after simple type detection and realis
         ################################################*/
        CollectionReaderDescription plainMentionOutput = testPlainMentionModel(tokenCrfConfig, testReader,
                vanillaTypeModel, sliceSuffix, "vanillaMention", processOutDir, subEvalDir, testGold, skipTypeTest);
        CollectionReaderDescription realisOutput = testRealis(realisConfig, plainMentionOutput, realisModelDir,
                sliceSuffix, "vanillaMentionRealis", processOutDir, subEvalDir, testGold,
                skipTypeTest && skipRealisTest);
        testCoref(corefConfig, realisOutput, treeCorefModel, sliceSuffix, "corefDownStreamTest", processOutDir,
                subEvalDir, testGold, skipTypeTest && skipRealisTest && skipCorefTest);

//        for (Map.Entry<String, String> beamCorefModelWithName : beamCorefModels.entrySet()) {
//            String corefModelName = beamCorefModelWithName.getKey();
//            String beamCorefModel = beamCorefModelWithName.getValue();
//            testBeamCoref(corefConfig, realisOutput, beamCorefModel, sliceSuffix,
//                    "corefDownStreamTest_" + corefModelName, processOutDir, subEvalDir, testGold, skipCorefTest);
//        }
        /*#################################################
         * END of the Beam coreference models testing
        ################################################*/
    }
}
