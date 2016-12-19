package edu.cmu.cs.lti.event_coref.pipeline;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.annotators.*;
import edu.cmu.cs.lti.emd.annotators.misc.EventMentionTypeClassPrinter;
import edu.cmu.cs.lti.emd.annotators.postprocessors.MentionTypeSplitter;
import edu.cmu.cs.lti.emd.stat.ChineseMentionStats;
import edu.cmu.cs.lti.event_coref.annotators.prepare.ArgumentMerger;
import edu.cmu.cs.lti.exceptions.ConfigurationException;
import edu.cmu.cs.lti.io.EventDataReader;
import edu.cmu.cs.lti.learning.runners.*;
import edu.cmu.cs.lti.learning.utils.ModelUtils;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.pipeline.ProcessorWrapper;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    private static final Logger logger = LoggerFactory.getLogger(EventMentionPipeline.class);

    private Configuration mainConfig;

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
            logger.info("Evaluation mode is token based.");
            this.tokenDir = config.get("edu.cmu.cs.lti.training.token_map.dir");
        } else if (evalMode.equals("char")) {
            logger.info("Evaluaton mode is character based.");
        } else {
            throw new IllegalArgumentException(String.format("Unknown evaluation mode: %s.", evalMode));
        }

        this.evalLogOutputDir = FileUtils.joinPaths(config.get("edu.cmu.cs.lti.eval.log_dir"),
                config.get("edu.cmu.cs.lti.experiment.name"));
        this.evalScript = config.get("edu.cmu.cs.lti.eval.script");

        this.mainConfig = config;
    }

    private CollectionReaderDescription readDatasets(String datasetConfigPath, String[] datasetNames,
                                                     String parentDir, boolean skipReading)
            throws IOException, UIMAException, SAXException, CpeDescriptorException {
        logger.info(String.format("%d datasets to be read, which are %s",
                datasetNames.length, Joiner.on(",").join(datasetNames)));

        EventDataReader reader = new EventDataReader(parentDir, rawBase, skipReading);

        for (String datasetName : datasetNames) {
            logger.info("Reading dataset : " + datasetName);
            Configuration datasetConfig = new Configuration(new File(datasetConfigPath, datasetName + ".properties"));
            reader.readData(datasetConfig, typeSystemDescription);
        }

        return reader.getReader();
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

        boolean skipRaw = taskConfig.getBoolean("edu.cmu.cs.lti.skip.raw", false);

        if (skipRaw) {
            logger.info("Will not read raw files from corpus again.");
        } else {
            logger.info("Will read raw files from corpus.");
        }

        CollectionReaderDescription trainingReader = readDatasets(datasetSettingDir, trainingDatasets,
                trainingWorkingDir, skipRaw);
        CollectionReaderDescription testReader = readDatasets(datasetSettingDir, testDatasets,
                testingWorkingDir, skipRaw);

        File classFile = new File(FileUtils.joinPaths(trainingWorkingDir, "mention_types.txt"));
        if (classFile.exists()) {
            try (FileWriter writer = new FileWriter(classFile)) {
                writer.write("");
            }
        }


        if (trainingDatasets.length != 0) {
            logger.info("Writing possible classes from training data.");
            AnalysisEngineDescription classPrinter = AnalysisEngineFactory.createEngineDescription(
                    EventMentionTypeClassPrinter.class, typeSystemDescription,
                    EventMentionTypeClassPrinter.CLASS_OUTPUT_FILE, classFile
            );
            SimplePipeline.runPipeline(trainingReader, classPrinter);
            prepare(taskConfig, trainingWorkingDir, skipTrainPrepare, trainingReader);
        } else {
            logger.warn("No training readers specified.");
        }

        if (testDatasets.length != 0) {
            logger.info("Preprocessing the raw testing data.");
            prepare(taskConfig, testingWorkingDir, skipTestPrepare, testReader);
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
                            RunnerUtils.getGoldAnnotator(copyType, copyRealis, copyCluster, mergeSameSpan)
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

    public void computeStats() throws SAXException, UIMAException, CpeDescriptorException,
            IOException {
        logger.info("Computing statistics for the corpus into " + trainingWorkingDir);
        CollectionReaderDescription trainingReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, preprocessBase);
        CollectionReaderDescription trainingData = prepareTraining(trainingReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, fullRunSuffix, "prepared_training"), false, 1);

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
     * Prepare dataset for training. It copies the annotations to the system mentions and annotate arguments.
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
                                                        String outputBase, boolean skipTrainPrepare, int seed)
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
                    AnalysisEngineDescription mentionAndCorefGoldAnnotator = RunnerUtils.getGoldAnnotator(true, true,
                            true, false);
                    List<AnalysisEngineDescription> annotators = new ArrayList<>();
                    annotators.add(mentionAndCorefGoldAnnotator);
                    RunnerUtils.addCorefPreprocessors(annotators, language);
                    return annotators.toArray(new AnalysisEngineDescription[annotators.size()]);
                }
            }, workingDir, outputBase).runWithOutput();
        }

        return CustomCollectionReaderFactory.createRandomizedXmiReader(typeSystemDescription, workingDir, outputBase,
                seed);
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
                FileUtils.joinPaths(middleResults, fullRunSuffix, "prepared_training"), skipTrainPrepare, seed);

        TokenMentionModelRunner tokenModel = new TokenMentionModelRunner(mainConfig,
                typeSystemDescription);

        CorefModelRunner corefModel = new CorefModelRunner(mainConfig, typeSystemDescription);

        RealisModelRunner realisModelRunner = new RealisModelRunner(mainConfig, typeSystemDescription);

        // Train realis model.
        String realisModelDir = realisModelRunner.trainRealis(realisConfig, trainingData, fullRunSuffix,
                skipRealisTrain);

        String vanillaSentCrfModel = tokenModel.trainSentLvType(tokenCrfConfig, trainingData, testReader, fullRunSuffix,
                false, "hamming", processDir, null, skipTypeTrain, skipTypeTest);

        // Train coref model.
        String treeCorefModel = corefModel.trainLatentTreeCoref(corefConfig, trainingData, testReader, fullRunSuffix,
                processDir, null, null, skipCorefTrain, skipCorefTest);

        // Run the vanilla model.
        runOnly(tokenCrfConfig, realisConfig, corefConfig, testReader, vanillaSentCrfModel, realisModelDir,
                treeCorefModel, fullRunSuffix, "vanillaMention", processDir, skipTypeTest, skipRealisTest,
                skipCorefTest);
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

    public void trainTest(Configuration taskConfig, boolean runAll) throws Exception {
        int seed = taskConfig.getInt("edu.cmu.cs.lti.random.seed", 17);

        CollectionReaderDescription trainingReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, preprocessBase);

        CollectionReaderDescription testDataReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, testingWorkingDir, preprocessBase);

        String evalDir = FileUtils.joinPaths(testingWorkingDir, evalBase, "full_run");

        if (taskConfig.getBoolean("edu.cmu.cs.lti.individual.models", true)) {
            logger.info("Will run individual model experiments.");
            experiment(taskConfig, fullRunSuffix, trainingReader, testDataReader, evalDir, false, runAll);
        }

        if (taskConfig.getBoolean("edu.cmu.cs.lti.joint.models", true)) {
            logger.info("Will run joint model experiments.");
            jointExperiment(taskConfig, fullRunSuffix, trainingReader, testDataReader, evalDir, runAll);
        }

        if (taskConfig.getBoolean("edu.cmu.lti.after.models", true)) {
            logger.info("Will run after model experiments.");

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

            CollectionReaderDescription trainingData = prepareExperimentData(taskConfig, trainingSliceReader,
                    devSliceReader, processOut, sliceSuffix, true, seed);

            if (taskConfig.getBoolean("edu.cmu.cs.lti.individual.models", true)) {
                logger.info("Will run individual model experiments.");
                experiment(taskConfig, sliceSuffix, trainingData, devSliceReader, crossEvalDir, true, false);
            }

            if (taskConfig.getBoolean("edu.cmu.cs.lti.joint.models", true)) {
                logger.info("Will run joint model experiments.");
                jointExperiment(taskConfig, sliceSuffix, trainingData, devSliceReader, crossEvalDir, false);
            }

            if (taskConfig.getBoolean("edu.cmu.lti.after.models", true)) {
                logger.info("Will run after model experiments.");
                afterExperiment(taskConfig, sliceSuffix, trainingData, devSliceReader, crossEvalDir, false);

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
        TokenMentionModelRunner tokenModel = new TokenMentionModelRunner(mainConfig,
                typeSystemDescription);

        CorefModelRunner corefModelRunner = new CorefModelRunner(mainConfig, typeSystemDescription);
        RealisModelRunner realisModelRunner = new RealisModelRunner(mainConfig, typeSystemDescription);

        CollectionReaderDescription mentionOutput = tokenModel.sentenceLevelMentionTagging(tokenCrfConfig, reader,
                typeModel, trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "mention"), skipType);

        CollectionReaderDescription realisOutput = realisModelRunner.realisAnnotation(realisConfig, mentionOutput,
                realisModel, trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "realis"),
                skipType && skipRealis);

        CollectionReaderDescription corefSentMentions = corefModelRunner.corefResolution(corefConfig,
                realisOutput, corefModel, trainingWorkingDir, FileUtils.joinPaths(annotatedOutput, "coref"),
                skipType && skipRealis && skipCoref);

        String tbfOutput = FileUtils.joinPaths(outputDir, sliceSuffix, runName + ".tbf");
        RunnerUtils.writeResults(corefSentMentions, tbfOutput, runName, useCharOffset);

        return tbfOutput;
    }

    /**
     * Run two simple downstream tasks after mention detection, to check the performance of mention models.
     */
    private void mentionDownstream(Configuration realisConfig, Configuration corefConfig,
                                   CollectionReaderDescription mentionOutput, String sliceSuffix, String processOutDir,
                                   String subEvalDir, File testGold, boolean skipRealisTest, boolean skipCorefTest)
            throws InterruptedException, SAXException, UIMAException, CpeDescriptorException, IOException {
        String realisModel = ModelUtils.getTestModelFile(eventModelDir, realisConfig, sliceSuffix);
        String treeCorefModel = ModelUtils.getTestModelFile(eventModelDir, corefConfig, sliceSuffix);

        CorefModelRunner corefModelRunner = new CorefModelRunner(mainConfig, typeSystemDescription);
        RealisModelRunner realisModelRunner = new RealisModelRunner(mainConfig, typeSystemDescription);

        CollectionReaderDescription realisOutput = realisModelRunner.testRealis(realisConfig, mentionOutput,
                realisModel,
                sliceSuffix, "realis", processOutDir, subEvalDir, testGold, skipRealisTest);

        corefModelRunner.testCoref(corefConfig, realisOutput, treeCorefModel, sliceSuffix, "treeCoref",
                processOutDir, subEvalDir, testGold, skipCorefTest);
    }

    private Configuration getModelConfig(String modelConfigName) throws IOException {
        return new Configuration(new File(modelConfigDir, modelConfigName + ".properties"));
    }

    private CollectionReaderDescription prepareExperimentData(Configuration taskConfig,
                                                              CollectionReaderDescription trainReader,
                                                              CollectionReaderDescription testReader,
                                                              String processOutDir, String sliceSuffix,
                                                              boolean hasTestGold, int seed)
            throws SAXException, UIMAException, CpeDescriptorException, IOException {
        boolean skipTrainPrepare = taskConfig.getBoolean("edu.cmu.cs.lti.train.skip.prepare", false);
        CollectionReaderDescription trainingData = prepareTraining(trainReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "prepared_training"), skipTrainPrepare, seed);

        // Produce gold standard tbf for evaluation.
        String testGold = null;
        if (hasTestGold) {
            testGold = FileUtils.joinPaths(processOutDir, "gold_test_" + sliceSuffix + ".tbf");
            RunnerUtils.writeGold(testReader, testGold, useCharOffset);
        }

        String trainGold = FileUtils.joinPaths(processOutDir, "gold_train_" + sliceSuffix + ".tbf");
        RunnerUtils.writeGold(trainingData, trainGold, useCharOffset);

        return trainingData;
    }

    private void afterExperiment(Configuration taskConfig, String sliceSuffix, CollectionReaderDescription trainingData,
                                 CollectionReaderDescription testReader, String processOutDir, boolean runAll)
            throws IOException, UIMAException, SAXException, CpeDescriptorException, ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        boolean skipAfterTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.after.skiptrain", false);
        boolean skipAfterTest = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.after.skiptest", false);
        Configuration afterConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.after"));

        File testGold = FileUtils.joinPathsAsFile(processOutDir, "gold_test_" + sliceSuffix + ".tbf");

        logger.info("Producing partial gold standards to tag after links.");
        CollectionReaderDescription goldMentionAll = annotateGoldMentions(testReader, trainingWorkingDir,
                FileUtils.joinPaths(middleResults, sliceSuffix, "gold_mentions"), true, true, false, true, false
                    /* copy type, realis, not coref, merge types, skip*/);

        AfterModelRunner runner = new AfterModelRunner(mainConfig, typeSystemDescription);

        runner.trainAfterModel(afterConfig, trainingData, testReader, processOutDir, sliceSuffix, testGold,
                skipAfterTrain, skipAfterTest);
    }

    private void jointExperiment(Configuration taskConfig, String sliceSuffix, CollectionReaderDescription trainingData,
                                 CollectionReaderDescription testReader, String processOutDir, boolean runAll
    ) throws Exception {
        boolean skipRealisTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain", false);
        boolean skipJointTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptrain", false);
        boolean skipJointTest = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptest", false);
        int jointBeamSize = taskConfig.getInt("edu.cmu.cs.lti.joint.beam.size", 5);

        Configuration realisConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.realis"));
        Configuration jointConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.joint"));

        String[] lossTypes = jointConfig.getList("edu.cmu.cs.lti.mention.loss_types");

        // Produce gold standard tbf for evaluation.
        File testGold = FileUtils.joinPathsAsFile(processOutDir, "gold_test_" + sliceSuffix + ".tbf");

        JointSpanCorefModelRunner jointModelRunner = new JointSpanCorefModelRunner(mainConfig, typeSystemDescription);

        RealisModelRunner realisModelRunner = new RealisModelRunner(mainConfig, typeSystemDescription);

        // Train realis model.
        String realisModelDir = realisModelRunner.trainRealis(realisConfig, trainingData, sliceSuffix, skipRealisTrain);


        // We try experiments with different settings of the following:
        // 1. Loss type
        // 2. Beam size
        // 3. Coreference strategy
        // The model name will be based on these settings.
        for (int i = 0; i < lossTypes.length; i++) {
            String lossType = lossTypes[i];
            for (int strategy = 1; strategy <= 1; strategy++) {
                jointModelRunner.trainJointSpanModel(jointConfig, trainingData, testReader, realisModelDir,
                        processOutDir,
                        sliceSuffix, testGold, skipJointTrain, skipJointTest, lossType, jointBeamSize, strategy
                );
            }
        }
    }

    private void experiment(Configuration taskConfig, String sliceSuffix, CollectionReaderDescription trainingData,
                            CollectionReaderDescription testReader, String processOutDir, boolean hasTestGold,
                            boolean runAll) throws Exception {
        if (runAll) {
            logger.info("Force to run all experiments, no training will be skipped.");
        }

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
        Configuration corefConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.coreference"));

        String subEvalDir = sliceSuffix.equals(fullRunSuffix) ? "final" : "cv";

        // Produce gold standard tbf for evaluation.
        File testGold = FileUtils.joinPathsAsFile(processOutDir, "gold_test_" + sliceSuffix + ".tbf");

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
        RealisModelRunner realisModelRunner = new RealisModelRunner(mainConfig, typeSystemDescription);

        if (hasRealis) {
            // Train realis model.
            realisModelDir = realisModelRunner.trainRealis(realisConfig, trainingData, sliceSuffix, skipRealisTrain);
            realisModelRunner.testRealis(realisConfig, goldMentionTypes, realisModelDir,
                    sliceSuffix, "gold_mention_realis", processOutDir, subEvalDir, testGold, skipRealisTest);
        }

        /*####################################################
         * End of gold standard benchmarks.
         ###################################################*/

        /*#################################################
         * BEGIN of the Vanilla perceptron models training:
         ################################################*/

        TokenMentionModelRunner tokenModel = new TokenMentionModelRunner(mainConfig,
                typeSystemDescription);
        CorefModelRunner corefModel = new CorefModelRunner(mainConfig, typeSystemDescription);

        // The vanilla crf model.
        String vanillaTypeModel = tokenModel.trainSentLvType(tokenCrfConfig, trainingData, testReader, sliceSuffix,
                false,
                "hamming", processOutDir, testGold, skipTypeTrain, skipTypeTest);

//        tokenMentionErrorAnalysis(tokenCrfConfig, testReader, vanillaTypeModel);

        // The vanilla coref model.
        String treeCorefModel = corefModel.trainLatentTreeCoref(corefConfig, trainingData, goldMentionAll, sliceSuffix,
                processOutDir, subEvalDir, testGold, skipCorefTrain, skipTypeTest && skipCorefTest);

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
//                    tokenModel.testPlainMentionModel(tokenCrfConfig, testReader, paMentionModels[i], sliceSuffix,
//                    "paMention_" + lossType, processOutDir, subEvalDir, testGold, skipTypeTest),
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
        CollectionReaderDescription plainMentionOutput = tokenModel.testPlainMentionModel(tokenCrfConfig, testReader,
                vanillaTypeModel, sliceSuffix, "vanillaMention", processOutDir, subEvalDir, testGold, skipTypeTest);
        CollectionReaderDescription realisOutput = realisModelRunner.testRealis(realisConfig, plainMentionOutput,
                realisModelDir, sliceSuffix, "vanillaMentionRealis", processOutDir, subEvalDir, testGold,
                skipTypeTest && skipRealisTest);
        corefModel.testCoref(corefConfig, realisOutput, treeCorefModel, sliceSuffix, "corefDownStreamTest",
                processOutDir, subEvalDir, testGold, skipTypeTest && skipRealisTest && skipCorefTest);

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
