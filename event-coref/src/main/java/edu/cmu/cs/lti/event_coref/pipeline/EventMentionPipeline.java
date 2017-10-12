package edu.cmu.cs.lti.event_coref.pipeline;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.annotators.*;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.emd.annotators.misc.EventMentionTypeClassPrinter;
import edu.cmu.cs.lti.emd.annotators.postprocessors.MentionTypeSplitter;
import edu.cmu.cs.lti.emd.stat.ChineseMentionStats;
import edu.cmu.cs.lti.event_coref.annotators.prepare.ArgumentMerger;
import edu.cmu.cs.lti.exceptions.ConfigurationException;
import edu.cmu.cs.lti.io.EventDataReader;
import edu.cmu.cs.lti.learning.runners.*;
import edu.cmu.cs.lti.learning.utils.ModelUtils;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.ExperimentPaths;
import edu.cmu.cs.lti.utils.FileUtils;
import edu.stanford.nlp.wordseg.ChineseStringUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
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
    final private String devWorkingDir;

    // The directory that stores all the awesome and not-awesome models.
    final private String generalModelDir;
    final private String eventModelDir;
    final private String modelConfigDir;

    private ExperimentPaths paths;

    // When cross validation, we have auto generated suffixes for outputs. Let's make one for the full run too.
    final static String fullRunSuffix = "all";

    final private String language;

    private boolean useCharOffset;

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
     * @param devWorkingDir
     * @param modelConfigDir
     * @param processOutputDir
     */
    private EventMentionPipeline(String typeSystemName, String language, boolean useCharOffset, String modelDir,
                                 String modelOutDir, String trainingWorkingDir, String testingWorkingDir,
                                 String devWorkingDir, String modelConfigDir, String processOutputDir) {
        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);
        this.generalModelDir = modelDir;
        this.eventModelDir = modelOutDir;

        this.modelConfigDir = modelConfigDir;

        this.trainingWorkingDir = trainingWorkingDir;
        this.testingWorkingDir = testingWorkingDir;
        this.devWorkingDir = devWorkingDir;

        if (trainingWorkingDir != null) {
            logger.info(String.format("Training directory will be %s.", trainingWorkingDir));
        }

        if (testingWorkingDir != null && new File(testingWorkingDir).exists()) {
            logger.info(String.format("Testing directory will be %s.", testingWorkingDir));
        }

        if (devWorkingDir != null && new File(devWorkingDir).exists()) {
            logger.info(String.format("Development directory will be %s.", devWorkingDir));
        }

        logger.info(String.format("Models can be found in %s.", modelOutDir));

        this.language = language;
        this.useCharOffset = useCharOffset;

        this.paths = new ExperimentPaths(logger, processOutputDir);

        logger.info("The language is : " + language);
    }

    /**
     * A constructor that only take the training directory.
     *
     * @param typeSystemName The type system to use.
     * @param config         Configuration file.
     */
    public EventMentionPipeline(String typeSystemName, Configuration config) {
        this(typeSystemName, config.getOrElse("edu.cmu.cs.lti.language", "en"),
                config.getBoolean("edu.cmu.cs.lti.output.character.offset", true),
                config.get("edu.cmu.cs.lti.model.dir"),
                config.get("edu.cmu.cs.lti.model.event.dir"),
                config.get("edu.cmu.cs.lti.training.working.dir"),
                config.get("edu.cmu.cs.lti.test.working.dir"),
                config.get("edu.cmu.cs.lti.dev.working.dir"),
                // Experiment data are also put into their own experiemnt folders.
                config.get("edu.cmu.cs.lti.model.config.dir"),
                FileUtils.joinPaths(config.get("edu.cmu.cs.lti.process.base.dir"),
                        config.get("edu.cmu.cs.lti.experiment.name")));

        if (config.getBoolean("edu.cmu.cs.lti.output.character.offset", true)) {
            logger.info("Evaluation mode is character based.");

        } else {
            logger.info("Evaluation mode is token based.");
        }
        this.mainConfig = config;
    }

    private CollectionReaderDescription readDatasets(String datasetConfigPath, String[] datasetNames,
                                                     String parentDir, boolean skipReading)
            throws IOException, UIMAException, SAXException, CpeDescriptorException {
        logger.info(String.format("%d datasets to be read, which are %s",
                datasetNames.length, Joiner.on(",").join(datasetNames)));

        EventDataReader reader = new EventDataReader(parentDir, paths.getRawBase(), skipReading);

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
    public void prepareData(Configuration taskConfig) throws IOException,
            UIMAException, CpeDescriptorException, SAXException {
        boolean skipTrainPreprocess = taskConfig.getBoolean("edu.cmu.cs.lti.train.skip.preprocess", false);
        boolean skipTestPreprocess = taskConfig.getBoolean("edu.cmu.cs.lti.test.skip.preprocess", false);
        boolean skipTrainPrepare = taskConfig.getBoolean("edu.cmu.cs.lti.train.skip.prepare", false);

        String datasetSettingDir = taskConfig.get("edu.cmu.cs.lti.dataset.settings.path");
        String[] trainingDatasets = taskConfig.getList("edu.cmu.cs.lti.training.datasets");
        String[] testDatasets = taskConfig.getList("edu.cmu.cs.lti.testing.datasets");
        String[] devDatasets = taskConfig.getList("edu.cmu.cs.lti.dev.datasets");

        boolean skipRaw = taskConfig.getBoolean("edu.cmu.cs.lti.skip.raw", false);

        int seed = taskConfig.getInt("edu.cmu.cs.lti.random.seed", 17);

        if (skipRaw) {
            logger.info("Existing raw dataset will not be read from corpus again.");
        } else {
            logger.info("Datasets will always be read from corpus.");
        }

        logger.info("Loading training data.");
        CollectionReaderDescription trainingReader = readDatasets(datasetSettingDir, trainingDatasets,
                trainingWorkingDir, skipRaw);
        logger.info("Loading test data.");
        CollectionReaderDescription testReader = readDatasets(datasetSettingDir, testDatasets,
                testingWorkingDir, skipRaw);
        logger.info("Loading dev data.");
        CollectionReaderDescription devReader = readDatasets(datasetSettingDir, devDatasets,
                devWorkingDir, skipRaw);

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
            CollectionReaderDescription preprocssedData = prepareData(taskConfig, trainingWorkingDir,
                    skipTrainPreprocess, trainingReader);
            prepareTraining(preprocssedData, trainingWorkingDir, "prepared_training", skipTrainPrepare, seed);
        } else {
            logger.warn("No training readers specified.");
        }

        if (testDatasets.length != 0) {
            logger.info("Preprocessing the raw testing data.");
            prepareData(taskConfig, testingWorkingDir, skipTestPreprocess, testReader);
        } else {
            logger.warn("No test readers specified.");
        }

        if (devDatasets.length != 0) {
            logger.info("Preprocessing the raw development data.");
            prepareData(taskConfig, devWorkingDir, skipTestPreprocess, devReader);
        } else {
            logger.warn("No dev readers specified.");
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
    public CollectionReaderDescription prepareData(Configuration taskConfig, String workingDirPath, boolean
            skipIfExists, CollectionReaderDescription... inputReaders) throws
            UIMAException, IOException, CpeDescriptorException, SAXException {
        if (workingDirPath == null) {
            logger.info("Working directory not provided, not running");
            return null;
        }

        File workingDir = new File(workingDirPath);
        if (!workingDir.exists()) {
            logger.info("Created directory for preprocessing : " + workingDirPath);
            workingDir.mkdirs();
        }

        File preprocessDir = paths.getPreprocessPath(workingDirPath);

        CollectionReaderDescription output = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
                workingDir.getAbsolutePath(), paths.getPreprocessBase());


        if (skipIfExists && preprocessDir.exists()) {
            logger.info("Preprocessed data exists, not running.");
            return output;
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

        AnalysisEngineDescription[] preprocessors = new AnalysisEngineDescription[preprocessorNames.size()];

        for (int i = 0; i < preprocessorNames.size(); i++) {
            String name = preprocessorNames.get(i);
            AnalysisEngineDescription processor;

            switch (name) {
                case "corenlp":
                    // Normally, the Chinese CoreNLP is not thread-safe, but we can use trick to go over it.
                    boolean multithread = language.equals("en") ? true : false;

                    boolean safer = ChineseStringUtils.validate(logger);

                    if (language.equals("zh")) {
                        if (safer) {
                            logger.info("Find the customized StringUtils, will run Stanford with multi-thread.");
                            multithread = true;
                        } else {
                            logger.info("Cannot find the customized StringUtils, will not run Stanford with " +
                                    "multi-thread.");
                        }
                    }

                    if (multithread) {
                        logger.info("CoreNLP started with multi-thread.");
                    }

                    processor = AnalysisEngineFactory.createEngineDescription(
                            StanfordCoreNlpAnnotator.class, typeSystemDescription,
                            StanfordCoreNlpAnnotator.PARAM_LANGUAGE, language,
                            // We let Stanford to handle the multi thread themselves.
                            // Although the current English pipeline is thread safe, the Chinese one is not.
                            // Any future releases may not be thread safe guaranteed.
                            StanfordCoreNlpAnnotator.PARAM_NUM_THREADS, 10,
                            AbstractAnnotator.MULTI_THREAD, multithread
                    );
                    break;
                case "semafor":
                    processor = AnalysisEngineFactory.createEngineDescription(
                            SemaforAnnotator.class, typeSystemDescription,
                            SemaforAnnotator.SEMAFOR_MODEL_PATH, semaforModelDirectory,
                            SemaforAnnotator.PARAM_JSON_OUTPUT_REDIRECT,
                            FileUtils.joinPaths(workingDirPath, "semafor_json"),
                            AbstractAnnotator.MULTI_THREAD, true
                    );
                    break;
                case "fanse":
                    processor = AnalysisEngineFactory.createEngineDescription(
                            FanseAnnotator.class, typeSystemDescription,
                            FanseAnnotator.PARAM_MODEL_BASE_DIR, fanseModelDirectory,
                            AbstractAnnotator.MULTI_THREAD, true
                    );
                    break;
                case "opennlp":
                    processor = AnalysisEngineFactory.createEngineDescription(
                            OpenNlpChunker.class, typeSystemDescription,
                            OpenNlpChunker.PARAM_MODEL_PATH, opennlpModel);
                    break;
                case "wordnetEntity":
                    processor = AnalysisEngineFactory.createEngineDescription(
                            WordNetBasedEntityAnnotator.class, typeSystemDescription,
                            WordNetBasedEntityAnnotator.PARAM_WN_PATH,
                            FileUtils.joinPaths(taskConfig.get("edu.cmu.cs.lti.resource.dir"),
                                    taskConfig.get("edu.cmu.cs.lti.wndict.path"))
                    );
                    break;
                case "quote":
                    processor = AnalysisEngineFactory.createEngineDescription(
                            QuoteAnnotator.class, typeSystemDescription
                    );
                    break;
                case "ArgumentMerger":
                    processor = AnalysisEngineFactory.createEngineDescription(ArgumentMerger.class,
                            typeSystemDescription);
                    break;
                case "mateChineseSrl":
                    processor = AnalysisEngineFactory.createEngineDescription(
                            MateChineseSrlAnnotator.class, typeSystemDescription,
                            MateChineseSrlAnnotator.PARAM_MODEL_FILE, mateModel,
                            AbstractAnnotator.MULTI_THREAD, true
                    );
                    break;
                case "zpar":
                    processor = AnalysisEngineFactory.createEngineDescription(
                            ZParChineseCharacterConstituentParser.class, typeSystemDescription,
                            ZParChineseCharacterConstituentParser.PARAM_CHINESE_MODEL, zparChineseModel,
                            ZParChineseCharacterConstituentParser.PARAM_ZPAR_BIN_PATH, zparBinPath
                    );
                    break;
                case "ltp":
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
                            new File(generalModelDir, "ltp_models/ltp_data/srl"),
                            AbstractAnnotator.MULTI_THREAD, true
                    );
                    break;
                case "discourse":
                    processor = AnalysisEngineFactory.createEngineDescription(
                            DiscourseParserAnnotator.class, typeSystemDescription);
                    break;
                default:
                    throw new ConfigurationException("Unknown preprocessor specified : " + name);
            }

            logger.info("Adding preprocessor " + name);

            preprocessors[i] = processor;
        }

        for (CollectionReaderDescription reader : inputReaders) {
            boolean robust = taskConfig.getBoolean("edu.cmu.cs.lti.robust", false);
            if (robust) {
                return BasicPipeline.getRobust(reader, workingDirPath, paths.getPreprocessBase(), preprocessors)
                        .run().getOutput();
            } else {
                return new BasicPipeline(reader, workingDirPath, paths.getPreprocessBase(), preprocessors)
                        .run().getOutput();
            }
        }

        return output;
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

    private CollectionReaderDescription removeEventSutff(CollectionReaderDescription reader, String mainDir,
                                                         String baseOutput) throws UIMAException, IOException {
        AnalysisEngineDescription remover = AnalysisEngineFactory.createEngineDescription(EventMentionRemover.class);
        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(mainDir, baseOutput);
        SimplePipeline.runPipeline(reader, remover, writer);
        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, baseOutput);
    }

    private CollectionReaderDescription annotateGoldMentions(CollectionReaderDescription reader, String mainDir,
                                                             String baseOutput, boolean copyType, boolean copyRealis,
                                                             boolean copyCluster, boolean copyRelation, boolean skip)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        if (skip && new File(mainDir, baseOutput).exists()) {
            logger.info("Skipping gold annotator since exists.");
        } else {
            AnalysisEngineDescription annotator = RunnerUtils.getGoldAnnotator(copyType, copyRealis, copyCluster,
                    copyRelation);
            return new BasicPipeline(reader, mainDir, baseOutput, annotator).run().getOutput();
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
        AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                MentionTypeSplitter.class, typeSystemDescription
        );
        return new BasicPipeline(reader, mainDir, baseOutput, mentionSplitter).run().getOutput();
    }

    public void computeStats() throws SAXException, UIMAException, CpeDescriptorException,
            IOException {
        logger.info("Computing statistics for the corpus into " + trainingWorkingDir);
        CollectionReaderDescription trainingReader = paths.getPreprocessReader(typeSystemDescription,
                trainingWorkingDir);

        CollectionReaderDescription trainingData = prepareTraining(trainingReader, trainingWorkingDir,
                paths.getMiddleOutputPath(fullRunSuffix, "prepared_training"), false, 1);

        AnalysisEngineDescription stats = AnalysisEngineFactory.createEngineDescription(
                ChineseMentionStats.class, typeSystemDescription,
                ChineseMentionStats.PARAM_OUTPUT_PATH, new File(trainingWorkingDir, "stats"));

        new BasicPipeline(trainingData, stats).run();
    }

    /**
     * Prepare dataset with gold standard for training. It copies the annotations to the system mentions and annotate
     * arguments.
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
            AnalysisEngineDescription allGoldAnnotator = RunnerUtils.getGoldAnnotator(
                    true, true, true, true);
            List<AnalysisEngineDescription> annotators = new ArrayList<>();
            annotators.add(allGoldAnnotator);
            RunnerUtils.addMentionPostprocessors(annotators, typeSystemDescription, language);
            new BasicPipeline(reader, workingDir, outputBase,
                    annotators.toArray(new AnalysisEngineDescription[annotators.size()])).run();
        }

        return CustomCollectionReaderFactory.createRandomizedXmiReader(typeSystemDescription, workingDir, outputBase,
                seed);
    }

    public void runVanilla(Configuration taskConfig, String workingDir) throws Exception {
        boolean skipType = taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptest", false);
        boolean skipRealis = taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptest", false);
        boolean skipCoref = taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptest", false);

        boolean addSemanticRole = taskConfig.getBoolean("edu.cmu.cs.lti.semantic.role", false);

        Configuration realisConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.realis"));
        Configuration tokenCrfConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.token_crf"));
        Configuration corefConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.coreference"));

        File blackList = taskConfig.getFile("edu.cmu.cs.lti.file.basename.ignores.mention");
        File whiteList = taskConfig.getFile("edu.cmu.cs.lti.file.basename.accept.mention");

        CollectionReaderDescription testReader = paths.getPreprocessReader(typeSystemDescription, workingDir,
                blackList, whiteList);

        String realisModelDir = ModelUtils.getTestModelFile(eventModelDir, realisConfig);
        String sentCrfModel = ModelUtils.getTestModelFile(eventModelDir, tokenCrfConfig);
        String treeCorefModel = ModelUtils.getTestModelFile(eventModelDir, corefConfig);

        String runName = "vanillaMention";

        // Run the vanilla model.
        logger.info(String.format("Type model is %s, Realis Model is %s, Coref Model is %s.", sentCrfModel,
                realisModelDir, treeCorefModel));
        if (addSemanticRole) {
            logger.info("Processor will append semantic role at the end.");
        }

        String annotatedOutput = paths.getMiddleOutputPath(fullRunSuffix, "vanillaMention");

        TokenMentionModelRunner tokenModel = new TokenMentionModelRunner(mainConfig, typeSystemDescription);
        CorefModelRunner corefModelRunner = new CorefModelRunner(mainConfig, typeSystemDescription);
        RealisModelRunner realisModelRunner = new RealisModelRunner(mainConfig, typeSystemDescription);

        CollectionReaderDescription mentionOutput = tokenModel.sentenceLevelMentionTagging(tokenCrfConfig, testReader,
                sentCrfModel, workingDir, FileUtils.joinPaths(annotatedOutput, "mention"), skipType);

        CollectionReaderDescription mentionPost = postProcessMention(mentionOutput, workingDir,
                FileUtils.joinPaths(annotatedOutput, "mention_post"), skipType);

        CollectionReaderDescription realisOutput = realisModelRunner.realisAnnotation(realisConfig, mentionPost,
                realisModelDir, workingDir, FileUtils.joinPaths(annotatedOutput, "realis"),
                skipType && skipRealis);

        CollectionReaderDescription corefSentMentions = corefModelRunner.corefResolution(corefConfig,
                realisOutput, treeCorefModel, workingDir, FileUtils.joinPaths(annotatedOutput, "coref"),
                skipType && skipRealis && skipCoref);

        String resultDir = paths.getResultDir(workingDir, fullRunSuffix);
        String tbfOutput = FileUtils.joinPaths(resultDir, fullRunSuffix, runName + ".tbf");
        RunnerUtils.writeResults(corefSentMentions, typeSystemDescription, tbfOutput, runName, useCharOffset,
                addSemanticRole);
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

        CollectionReaderDescription trainReader = paths.getPreprocessReader(typeSystemDescription, trainingWorkingDir);
        CollectionReaderDescription testReader = paths.getPreprocessReader(typeSystemDescription, testingWorkingDir);

        if (paths.preprocessExists(trainingWorkingDir)) {
            logger.info("Trying on training data.");
            new BasicPipeline(trainReader, trainingWorkingDir, paths.getTrialBase(), ltpAnnotator).run();
        } else {
            logger.info("Training preprocessed data not found, cannot try annotator");
        }

        if (paths.preprocessExists(testingWorkingDir)) {
            logger.info("Trying on test data.");
            new BasicPipeline(trainReader, testingWorkingDir, paths.getTrialBase(), ltpAnnotator).run();
        } else {
            logger.info("Test preprocessed data not found, cannot try annotator");
        }
    }

    public void trainAndDev(Configuration taskConfig, boolean runAll) throws Exception {
        trainEval(taskConfig, runAll, true);
    }

    public void trainAndTest(Configuration taskConfig, boolean runAll) throws Exception {
        trainEval(taskConfig, runAll, false);
    }

    public void trainEval(Configuration taskConfig, boolean runAll, boolean useDev) throws Exception {
        logger.info("Will conduct training testing.");

        int seed = taskConfig.getInt("edu.cmu.cs.lti.random.seed", 17);

        String evalDataWorkingDir = useDev ? devWorkingDir : testingWorkingDir;

        CollectionReaderDescription trainingReader = paths.randomPreparedTraining(typeSystemDescription,
                trainingWorkingDir, seed);

        CollectionReaderDescription evalDataReader = paths.getPreprocessReader(typeSystemDescription,
                evalDataWorkingDir);


        String testGoldStandard = taskConfig.get("edu.cmu.cs.lti.test.gold_standard");
        File testGold;
        if (testGoldStandard == null) {
            testGold = createGoldTBF(evalDataReader, evalDataWorkingDir);
        } else {
            testGold = new File(testGoldStandard);
        }


        if (taskConfig.getBoolean("edu.cmu.cs.lti.individual.models", false)) {
            logger.info("Will run individual model experiments.");
            experiment(taskConfig, fullRunSuffix, trainingReader, evalDataReader, testGold, evalDataWorkingDir, runAll);
            logger.info("Experiment done.");
        }

        if (taskConfig.getBoolean("edu.cmu.cs.lti.joint.models", false)) {
            logger.info("Will run joint model experiments.");
            jointExperiment(taskConfig, fullRunSuffix, trainingReader, evalDataReader, testGold, evalDataWorkingDir,
                    runAll);
            logger.info("Experiment done.");
        }

        if (taskConfig.getBoolean("edu.cmu.lti.after.models", false)) {
            logger.info("Will run after model experiments.");
            afterExperiment(taskConfig, fullRunSuffix, trainingReader, evalDataReader, testGold, evalDataWorkingDir,
                    runAll);
            logger.info("Experiment done.");
        }
    }

    private File createGoldTBF(CollectionReaderDescription evalDataReader, String workingDir) throws UIMAException,
            IOException {
        File outputFile = new File(workingDir, "generated_gold.tbf");

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                TbfStyleEventWriter.class, typeSystemDescription,
                TbfStyleEventWriter.PARAM_OUTPUT_PATH, outputFile,
                TbfStyleEventWriter.PARAM_SYSTEM_ID, "gold",
                TbfStyleEventWriter.PARAM_GOLD_TOKEN_COMPONENT_ID, TbfEventDataReader.COMPONENT_ID,
                TbfStyleEventWriter.PARAM_USE_CHARACTER_OFFSET, true,
                TbfStyleEventWriter.PARAM_USE_GOLD_VIEW, true
        );

        SimplePipeline.runPipeline(evalDataReader, writer);

        return outputFile;
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

        logger.info(String.format("Will conduct %d fold cross validation.", numSplit));
        for (int slice = 0; slice < numSplit; slice++) {
            logger.info("Starting CV split " + slice);

            String sliceSuffix = "split_" + slice;

            Pair<CollectionReaderDescription, CollectionReaderDescription> readers =
                    paths.crossValidationReader(typeSystemDescription, trainingWorkingDir, seed, numSplit, slice);

            CollectionReaderDescription trainingSliceReader = readers.getLeft();
            CollectionReaderDescription devSliceReader = readers.getRight();

            String resultDir = paths.getResultDir(trainingWorkingDir, sliceSuffix);

            logger.info("Writing evaluation output.");
            writeEvaluationOutput(trainingSliceReader, devSliceReader, resultDir, sliceSuffix, true);
            File testGold = new File(getTestGoldPath(resultDir, sliceSuffix));
            logger.info("Done writing evaluation output.");

            if (taskConfig.getBoolean("edu.cmu.cs.lti.individual.models", false)) {
                logger.info("Will run individual model experiments.");
                experiment(taskConfig, sliceSuffix, trainingSliceReader, devSliceReader, testGold,
                        trainingWorkingDir, false);
            }

            if (taskConfig.getBoolean("edu.cmu.cs.lti.joint.models", false)) {
                logger.info("Will run joint model experiments.");
                jointExperiment(taskConfig, sliceSuffix, trainingSliceReader, devSliceReader, testGold,
                        trainingWorkingDir, false);
            }

            if (taskConfig.getBoolean("edu.cmu.lti.after.models", false)) {
                logger.info("Will run after model experiments.");
                afterExperiment(taskConfig, sliceSuffix, trainingSliceReader, devSliceReader, testGold, trainingWorkingDir,
                        false);
            }
        }
    }

    /**
     * Post process the mentions to get important mention component: 1. Mention head word. 2. Mention aguments.
     *
     * @param mentionReader
     * @param workingDir
     * @param outputBase
     * @param skip
     * @return
     * @throws UIMAException
     * @throws IOException
     * @throws CpeDescriptorException
     * @throws SAXException
     */
    private CollectionReaderDescription postProcessMention(CollectionReaderDescription mentionReader,
                                                           String workingDir, String outputBase, boolean skip)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        List<AnalysisEngineDescription> annotators = new ArrayList<>();

        RunnerUtils.addMentionPostprocessors(annotators, typeSystemDescription, language);

        if (skip && new File(workingDir, outputBase).exists()) {
            logger.info("Skipping mention post processing, using existing results.");
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, outputBase);
        } else {
            return new BasicPipeline(mentionReader, workingDir, outputBase,
                    annotators.toArray(new AnalysisEngineDescription[annotators.size()])).run().getOutput();
        }
    }


//    /**
//     * Run two simple downstream tasks after mention detection, to check the performance of mention models.
//     */
//    private void mentionDownstream(Configuration realisConfig, Configuration corefConfig,
//                                   CollectionReaderDescription mentionOutput, String sliceSuffix, String
// processOutDir,
//                                   String subEvalDir, File testGold, boolean skipRealisTest, boolean skipCorefTest)
//            throws InterruptedException, SAXException, UIMAException, CpeDescriptorException, IOException {
//        String realisModel = ModelUtils.getTestModelFile(eventModelDir, realisConfig, sliceSuffix);
//        String treeCorefModel = ModelUtils.getTestModelFile(eventModelDir, corefConfig, sliceSuffix);
//
//        CorefModelRunner corefModelRunner = new CorefModelRunner(mainConfig, typeSystemDescription);
//        RealisModelRunner realisModelRunner = new RealisModelRunner(mainConfig, typeSystemDescription);
//
//        CollectionReaderDescription realisOutput = realisModelRunner.testRealis(realisConfig, mentionOutput,
//                realisModel,
//                sliceSuffix, "realis", processOutDir, subEvalDir, testGold, skipRealisTest);
//
//        corefModelRunner.testCoref(corefConfig, realisOutput, treeCorefModel, sliceSuffix, "treeCoref",
//                processOutDir, subEvalDir, testGold, skipCorefTest);
//    }

    private Configuration getModelConfig(String modelConfigName) throws IOException {
        if (modelConfigName == null || modelConfigName.isEmpty()) {
            return null;
        }
        return new Configuration(new File(modelConfigDir, modelConfigName + ".properties"));
    }

    private String getTestGoldPath(String resultDir, String suffix) {
        return FileUtils.joinPaths(resultDir, "gold_test_" + suffix + ".tbf");
    }

    private String getTrainGoldPath(String resultDir, String suffix) {
        return FileUtils.joinPaths(resultDir, "gold_train_" + suffix + ".tbf");
    }

    private void writeEvaluationOutput(CollectionReaderDescription trainReader,
                                       CollectionReaderDescription testReader,
                                       String resultDir, String sliceSuffix,
                                       boolean hasTestGold)
            throws SAXException, UIMAException, CpeDescriptorException, IOException {
        // Produce gold standard TBF for evaluation.
        if (hasTestGold) {
            logger.info("Writing development gold standard as TBF.");
            RunnerUtils.writeGold(testReader, getTestGoldPath(resultDir, sliceSuffix), useCharOffset);
        }

        logger.info("Writing training data as TBF.");
        RunnerUtils.writeGold(trainReader, getTrainGoldPath(resultDir, sliceSuffix), useCharOffset);
    }

    private void afterExperiment(Configuration taskConfig, String sliceSuffix, CollectionReaderDescription trainingData,
                                 CollectionReaderDescription testReader, File testGold, String evalWorkingDir,
                                 boolean runAll)
            throws IOException, UIMAException, SAXException, CpeDescriptorException, ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException,
            InterruptedException {
        boolean skipAfterTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.after.skiptrain", false);
        boolean skipAfterTest = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.after.skiptest", false);

        Configuration afterConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.after"));

        String resultDir = paths.getResultDir(evalWorkingDir, sliceSuffix);

        logger.info("Producing partial gold standards on test data to tag after links.");
        CollectionReaderDescription goldMentionAll = annotateGoldMentions(testReader, evalWorkingDir,
                paths.getMiddleOutputPath(sliceSuffix, "gold_mentions"),
                true, true, true, false, false);

        CollectionReaderDescription mentionPost = postProcessMention(goldMentionAll, evalWorkingDir,
                paths.getMiddleOutputPath(sliceSuffix, "mention_post"), false);

        logger.info("Done mention post processing.");

        PlainAfterModelRunner runner = new PlainAfterModelRunner(mainConfig, typeSystemDescription);

//        runner.runBaseline(afterConfig, mentionPost, resultDir, sliceSuffix, testGold);

        runner.trainAfterModel(afterConfig, trainingData, mentionPost, resultDir, sliceSuffix, testGold,
                skipAfterTrain, skipAfterTest);
    }

    private void jointExperiment(Configuration taskConfig, String sliceSuffix, CollectionReaderDescription trainingData,
                                 CollectionReaderDescription testReader, File testGold, String evalWorkingDir,
                                 boolean runAll) throws Exception {
        boolean skipRealisTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain", false);
        boolean skipJointTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptrain", false);
        boolean skipJointTest = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptest", false);
        int jointBeamSize = taskConfig.getInt("edu.cmu.cs.lti.joint.beam.size", 5);

        Configuration realisConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.realis"));
        Configuration jointConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.joint"));

        String[] lossTypes = jointConfig.getList("edu.cmu.cs.lti.mention.loss_types");

        String resultDir = paths.getResultDir(evalWorkingDir, sliceSuffix);

        JointSpanCorefModelRunner jointModelRunner = new JointSpanCorefModelRunner(mainConfig, typeSystemDescription);

        RealisModelRunner realisModelRunner = new RealisModelRunner(mainConfig, typeSystemDescription);

        // Train realis model.
        realisModelRunner.trainRealis(realisConfig, trainingData, sliceSuffix, skipRealisTrain);

        // We try experiments with different settings of the following:
        // 1. Loss type
        // 2. Beam size
        // 3. Coreference strategy
        // The model name will be based on these settings.
        for (int i = 0; i < lossTypes.length; i++) {
            String lossType = lossTypes[i];
            for (int strategy = 1; strategy <= 1; strategy++) {
                jointModelRunner.trainJointSpanModel(jointConfig, trainingData, testReader,
                        realisModelRunner.getModelDir(), resultDir, sliceSuffix, testGold, skipJointTrain,
                        skipJointTest, lossType, jointBeamSize, strategy
                );
            }
        }
    }

    private void experiment(Configuration taskConfig, String sliceSuffix, CollectionReaderDescription trainingData,
                            CollectionReaderDescription testReader, File testGold, String evalWorkingDir,
                            boolean runAll)
            throws Exception {
        if (runAll) {
            logger.info("Force to run all experiments, no training will be skipped.");
        }

        String resultDir = paths.getResultDir(evalWorkingDir, sliceSuffix);

        boolean skipCorefTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptrain", false);
        boolean skipTypeTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);
        boolean skipRealisTrain = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain", false);

        boolean skipTypeTest = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptest", false);
        boolean skipRealisTest = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptest", false);
        boolean skipCorefTest = !runAll && taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptest", false);

        logger.info(String.format("Training skips, skip mention : %s, skip realis : %s, skip coref : %s.",
                skipTypeTrain, skipRealisTrain, skipCorefTrain));

        logger.info(String.format("Testing skips, skip mention : %s, skip realis : %s, skip coref : %s.",
                skipTypeTest, skipRealisTest, skipCorefTrain));

        logger.info("Evaluation will be performed against " + testGold);

        Configuration realisConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.realis"));
        Configuration tokenCrfConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.token_crf"));
        Configuration corefConfig = getModelConfig(taskConfig.get("edu.cmu.cs.lti.model.coreference"));

        String subEvalDir = sliceSuffix.equals(fullRunSuffix) ? "final" : "cv";

        // Produce gold mention (type + realis) detection.
        logger.info("Producing partial gold standards with type and realis only.");

        // Make sure no event stuff are annotated on the test data.
        CollectionReaderDescription noEvent = removeEventSutff(testReader, evalWorkingDir,
                paths.getMiddleOutputPath(sliceSuffix, "no_event"));

        CollectionReaderDescription goldMentionAll = annotateGoldMentions(noEvent, evalWorkingDir,
                paths.getMiddleOutputPath(sliceSuffix, "gold_mentions"), true, true, false, false, true);

        // Post process mentions to add headwords and arguments.
        CollectionReaderDescription postMention = postProcessMention(goldMentionAll, evalWorkingDir,
                paths.getMiddleOutputPath(sliceSuffix, "gold_mentions_post"), skipTypeTest);

        // Produce gold mention types, to test performance of realis detection.
        CollectionReaderDescription goldMentionTypes = annotateGoldMentions(testReader, evalWorkingDir,
                paths.getMiddleOutputPath(sliceSuffix, "gold_type"), true, false, false, false, false);

        // Train realis model.
        RealisModelRunner realisModelRunner = new RealisModelRunner(mainConfig, typeSystemDescription);

        if (realisConfig != null) {
            realisModelRunner.trainRealis(realisConfig, trainingData, sliceSuffix, skipRealisTrain);
            realisModelRunner.testRealis(realisConfig, goldMentionTypes, sliceSuffix,
                    "gold_mention_realis", resultDir, testGold, skipRealisTest);
        }

        // Training the vanilla models.
        // The token models.
        TokenMentionModelRunner tokenModel = new TokenMentionModelRunner(mainConfig, typeSystemDescription);
        // The vanilla crf model.
        String vanillaTypeModel = tokenModel.trainSentLvType(tokenCrfConfig, trainingData, noEvent, sliceSuffix,
                false, "hamming", resultDir, testGold, skipTypeTrain, skipTypeTest);

//        tokenMentionErrorAnalysis(tokenCrfConfig, testReader, vanillaTypeModel);

        // The coreference models.
        CorefModelRunner corefModel = new CorefModelRunner(mainConfig, typeSystemDescription);

        // The vanilla coref model.
        String treeCorefModel = corefModel.trainLatentTreeCoref(corefConfig, trainingData, postMention, sliceSuffix,
                resultDir, subEvalDir, testGold, skipCorefTrain, skipTypeTest && skipCorefTest);

        corefModel.testCoref(corefConfig, postMention, treeCorefModel, sliceSuffix, "coref_test",
                resultDir, subEvalDir, testGold, skipTypeTest && skipRealisTest && skipCorefTest);

        // End of the vanilla model training.

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
//                    lossType, evalDir, subEvalDir, testGold, seed, skipTypeTrain, skipTypeTest);
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
//                            sliceSuffix, laso, d, size, evalDir, subEvalDir, testGold, seed, skipCorefTrain,
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
//                        "vanillaBeamMention", evalDir, subEvalDir, testGold, runAll),
//                sliceSuffix, evalDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//        );
//
//        mentionDownstream(realisConfig, corefConfig,
//                testBeamMentionModels(beamTokenCrfConfig, testReader, laSoBeamMentionModel, sliceSuffix,
//                        "noPaLasoBeamMention", evalDir, subEvalDir, testGold, runAll),
//                sliceSuffix, evalDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//        );
//
//        mentionDownstream(realisConfig, corefConfig,
//                testBeamMentionModels(beamTokenCrfConfig, testReader, laSoBeamMentionModel, sliceSuffix,
//                        "noPaLasoBeamMention", evalDir, subEvalDir, testGold, runAll),
//                sliceSuffix, evalDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//        );
//
//        for (int i = 0; i < lossTypes.length; i++) {
//            String lossType = lossTypes[i];
//
//            mentionDownstream(realisConfig, corefConfig,
//                    tokenModel.testPlainMentionModel(tokenCrfConfig, testReader, paMentionModels[i], sliceSuffix,
//                    "paMention_" + lossType, evalDir, subEvalDir, testGold, skipTypeTest),
//                    sliceSuffix, evalDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//            );
//
//            mentionDownstream(realisConfig, corefConfig,
//                    testBeamMentionModels(beamTokenCrfConfig, testReader, paBeamLaSOMentionModels[i], sliceSuffix,
//                            "laSoBeamMention_" + lossType, evalDir, subEvalDir, testGold, runAll),
//                    sliceSuffix, evalDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//            );
//
//
//            mentionDownstream(realisConfig, corefConfig,
//                    testBeamMentionModels(beamTokenCrfConfig, testReader, delayedPaBeamMentionModels[i], sliceSuffix,
//                            "laSoBeamMention_" + lossType, evalDir, subEvalDir, testGold, runAll),
//                    sliceSuffix, evalDir, subEvalDir, testGold, skipRealisTest, skipCorefTest
//            );
//        }
//        /*#################################################
//         * END of the mention experiments.
//         ################################################*/

        CollectionReaderDescription plainMentionOutput = tokenModel.testPlainMentionModel(tokenCrfConfig, noEvent,
                vanillaTypeModel, sliceSuffix, "vanillaMention", resultDir, testGold, skipTypeTest);
        // Post process mentions to add headwords and arguments.
        CollectionReaderDescription mentionWithHeadWord = postProcessMention(plainMentionOutput, trainingWorkingDir,
                paths.getMiddleOutputPath(sliceSuffix, "vanillaMention_post"), skipTypeTest);

        CollectionReaderDescription realisOutput = realisModelRunner.testRealis(realisConfig, mentionWithHeadWord,
                sliceSuffix, "vanillaMentionRealis", resultDir, testGold, skipTypeTest && skipRealisTest);

        corefModel.testCoref(corefConfig, realisOutput, treeCorefModel, sliceSuffix, "vanillaCoref",
                resultDir, subEvalDir, testGold, skipTypeTest && skipRealisTest && skipCorefTest);

//        for (Map.Entry<String, String> beamCorefModelWithName : beamCorefModels.entrySet()) {
//            String corefModelName = beamCorefModelWithName.getKey();
//            String beamCorefModel = beamCorefModelWithName.getValue();
//            testBeamCoref(corefConfig, realisOutput, beamCorefModel, sliceSuffix,
//                    "corefDownStreamTest_" + corefModelName, evalDir, subEvalDir, testGold, skipCorefTest);
//        }
    }
}
