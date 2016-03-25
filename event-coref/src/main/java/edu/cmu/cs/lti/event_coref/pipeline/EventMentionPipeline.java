package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.annotator.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.annotators.OpenNlpChunker;
import edu.cmu.cs.lti.annotators.QuoteAnnotator;
import edu.cmu.cs.lti.annotators.WordNetBasedEntityAnnotator;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.emd.annotators.classification.RealisTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.crf.CrfMentionTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.crf.MentionSequenceAnnotator;
import edu.cmu.cs.lti.emd.annotators.crf.MentionSequenceCrfTrainer;
import edu.cmu.cs.lti.emd.annotators.crf.MentionTypeCrfTrainer;
import edu.cmu.cs.lti.emd.annotators.postprocessors.MentionTypeSplitter;
import edu.cmu.cs.lti.emd.pipeline.TrainingLooper;
import edu.cmu.cs.lti.event_coref.annotators.*;
import edu.cmu.cs.lti.event_coref.annotators.prepare.ArgumentMerger;
import edu.cmu.cs.lti.event_coref.annotators.prepare.EnglishSrlArgumentExtractor;
import edu.cmu.cs.lti.event_coref.annotators.prepare.EventHeadWordAnnotator;
import edu.cmu.cs.lti.event_coref.train.DelayedLaSOJointTrainer;
import edu.cmu.cs.lti.event_coref.train.PaLatentTreeTrainer;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    final private String outputModelDir;

    // Some conventions of processing data.
    final private String middleResults = "intermediate";
    final private String preprocessBase = "preprocessed";
    final private String evalBase = "eval";

    // When cross validation, we have auto generated suffixes for outputs. Let's make one for the full run too.
    final private String fullRunSuffix = "all";

    private String language;

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
                                String modelOutDir,
                                String trainingWorkingDir, String testingWorkingDir) {
        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);
        this.generalModelDir = modelDir;
        this.outputModelDir = modelOutDir;

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
                config.get("edu.cmu.cs.lti.model.output.dir"), config.get("edu.cmu.cs.lti.training.working.dir"),
                config.get("edu.cmu.cs.lti.test.working.dir"));
    }

    /**
     * Run cross validation on regression.
     *
     * @param config Configuration file.
     * @throws Exception
     */
    public void regression(Configuration config, CollectionReaderDescription... inputReaders) throws Exception {
        String regressionDir = config.get("edu.cmu.cs.lti.regression.dir");

        if (trainingWorkingDir != null) {
            prepare(config, inputReaders, trainingWorkingDir, false);
        }

        if (testingWorkingDir != null) {
            prepare(config, inputReaders, testingWorkingDir, false);
        }

        trainAll(config, false, false, false, false, 17);
        test(config, false, false, false, false, false);
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
                                                       boolean mergetSameSpan)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS,
                new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName},
                GoldStandardEventMentionAnnotator.PARAM_COPY_MENTION_TYPE, copyType,
                GoldStandardEventMentionAnnotator.PARAM_COPY_REALIS, copyRealis,
                GoldStandardEventMentionAnnotator.PARAM_COPY_CLUSTER, copyCluster,
                GoldStandardEventMentionAnnotator.PARAM_MERGE_SAME_SPAN, mergetSameSpan
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
                                  boolean skipTrain, boolean usePaTraing, int initialSeed)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Starting training sentence level mention type model ...");

        String modelPath = joinPaths(outputModelDir, config.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir"), suffix);
        File modelFile = new File(modelPath);

        MutableInt trainingSeed = new MutableInt(initialSeed);

        // Only skip training when model directory exists.
        if (skipTrain && modelFile.exists()) {
            logger.info("Skipping mention type training, taking existing models.");
        } else {
            File cacheDir = new File(joinPaths(trainingWorkingDir, config.get("edu.cmu.cs.lti.mention.cache.base")));

            AnalysisEngineDescription trainingEngine = AnalysisEngineFactory.createEngineDescription(
                    MentionTypeCrfTrainer.class, typeSystemDescription,
                    MentionTypeCrfTrainer.PARAM_GOLD_STANDARD_VIEW_NAME, UimaConst.goldViewName,
                    MentionTypeCrfTrainer.PARAM_CONFIGURATION_FILE, config.getConfigFile(),
                    MentionTypeCrfTrainer.PARAM_CACHE_DIRECTORY, cacheDir,
                    MentionTypeCrfTrainer.PARAM_USE_PA_UPDATE, usePaTraing
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
                    MentionTypeCrfTrainer.loopStopActions();
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    MentionTypeCrfTrainer.saveModels(modelOutputDir, MentionTypeCrfTrainer.MODEL_NAME);

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

    public String trainDocLvType(Configuration config, CollectionReaderDescription trainingReader, String suffix,
                                 boolean skipTrain, int initialSeed)
            throws NoSuchMethodException, UIMAException, InstantiationException, IOException, ClassNotFoundException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Starting training document level mention type model ...");

        String modelPath = joinPaths(outputModelDir, config.get("edu.cmu.cs.lti.model.crf.mention.lv2.dir"), suffix);
        File modelFile = new File(modelPath);

        MutableInt trainingSeed = new MutableInt(initialSeed);

        // Only skip training when model directory exists.
        if (skipTrain && modelFile.exists()) {
            logger.info("Skipping second level mention type training, taking existing models.");
        } else {
            File cacheDir = new File(joinPaths(trainingWorkingDir, config.get("edu.cmu.cs.lti.mention.cache.base")));

            AnalysisEngineDescription trainingEngine = AnalysisEngineFactory.createEngineDescription(
                    MentionSequenceCrfTrainer.class, typeSystemDescription,
                    MentionSequenceCrfTrainer.PARAM_CONFIGURATION_FILE, config.getConfigFile(),
                    MentionSequenceCrfTrainer.PARAM_CACHE_DIRECTORY, cacheDir
            );

            TrainingLooper mentionTypeTrainer = new TrainingLooper(config, modelPath,
                    trainingReader, trainingEngine) {
                @Override
                protected void loopActions() {
                    super.loopActions();
                    trainingSeed.add(2);
                    //TODO the seed update doesn't work for the moment.
                    logger.debug("Update the training seed to " + trainingSeed.intValue());
                    trainingReader.setAttributeValue(RandomizedXmiCollectionReader.PARAM_SEED, trainingSeed.getValue());
                }

                @Override
                protected void finish() throws IOException {
                    MentionSequenceCrfTrainer.loopStopActions();
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    MentionSequenceCrfTrainer.saveModels(modelOutputDir, MentionSequenceCrfTrainer.MODEL_NAME);
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
        String cvModelDir = joinPaths(outputModelDir, modelDir, suffix);

        boolean modelExists = new File(cvModelDir).exists();

        MutableInt trainingSeed = new MutableInt(initialSeed);

        if (skipTrain && modelExists) {
            logger.info("Skipping coreference training, taking existing models.");
        } else {
            logger.info("Saving model directory at : " + cvModelDir);

            String cacheDir = edu.cmu.cs.lti.utils.FileUtils.joinPaths(trainingWorkingDir,
                    config.get("edu.cmu.cs.lti.coref.cache.base"));

            AnalysisEngineDescription corefEngine = AnalysisEngineFactory.createEngineDescription(
                    PaLatentTreeTrainer.class, typeSystemDescription,
                    PaLatentTreeTrainer.PARAM_CONFIG_PATH, config.getConfigFile().getPath(),
                    PaLatentTreeTrainer.PARAM_CACHE_DIRECTORY, cacheDir
            );

            TrainingLooper corefTrainer = new TrainingLooper(config, cvModelDir, trainingReader, corefEngine) {
                @Override
                protected void loopActions() {
                    super.loopActions();
//                    trainingSeed.add(2);
//                    trainingReader.setAttributeValue(RandomizedXmiCollectionReader.PARAM_SEED, trainingSeed
// .getValue());
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

            logger.info("Coreference training finished ...");
        }
        return cvModelDir;
    }

    private CollectionReaderDescription corefResolution(Configuration config, CollectionReaderDescription reader,
                                                        String modelDir, String mainDir, String outputBase,
                                                        File corefRuleFile, boolean useAverage, boolean skipCorefTest)
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
                            EventCorefAnnotator.PARAM_CONFIG_PATH, config.getConfigFile(),
                            EventCorefAnnotator.PARAM_USE_AVERAGE, useAverage
                    );

                    AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                            MentionTypeSplitter.class, typeSystemDescription,
                            MentionTypeSplitter.PARAM_COREF_RULE_FILE, corefRuleFile
                    );

                    List<AnalysisEngineDescription> annotators = new ArrayList<>();
                    addCorefPreprocessors(annotators);
                    annotators.add(corefAnnotator);
                    annotators.add(mentionSplitter);
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
                            MentionTypeSplitter.class, typeSystemDescription,
                            MentionTypeSplitter.PARAM_COREF_RULE_FILE, corefRuleFile
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


//    private String joinTypeTraining(Configuration config, CollectionReaderDescription trainingReader,
//                                    String suffix, File corefRule, boolean skipTrain, int initialSeed) throws
//            UIMAException, NoSuchMethodException, InstantiationException, IOException,
//            IllegalAccessException, InvocationTargetException, ClassNotFoundException {
//        logger.info("Running joint training of mention detection and coreference.");
//        String cvModelDir = joinPaths(outputModelDir, config.get("edu.cmu.cs.lti.model.joint.dir"), suffix);
//
//        boolean modelExists = new File(cvModelDir).exists();
//
//        MutableInt trainingSeed = new MutableInt(initialSeed);
//
//        if (skipTrain && modelExists) {
//            logger.info("Skipping joint training, taking existing models.");
//        } else {
//            logger.info("Saving model directory at : " + cvModelDir);
//
//            AnalysisEngineDescription corefEngine = AnalysisEngineFactory.createEngineDescription(
//                    DDJointTrainer.class, typeSystemDescription,
//                    DDJointTrainer.PARAM_CONFIG_PATH, config.getConfigFile().getPath(),
//                    DDJointTrainer.PARAM_COREF_RULE_FILE, corefRule
//            );
//
//            TrainingLooper corefTrainer = new TrainingLooper(config, cvModelDir, trainingReader, corefEngine) {
//                @Override
//                protected void loopActions() {
//                    super.loopActions();
//                    trainingSeed.add(2);
//                    trainingReader.setAttributeValue(RandomizedXmiCollectionReader.PARAM_SEED, trainingSeed
// .getValue());
//                }
//
//                @Override
//                protected void finish() throws IOException {
//                    DDJointTrainer.finish();
//                }
//
//                @Override
//                protected void saveModel(File modelOutputDir) throws IOException {
//                    DDJointTrainer.saveModels(modelOutputDir);
//                }
//            };
//
//            corefTrainer.runLoopPipeline();
//
//            logger.info("DD Joint Training finished ...");
//        }
//        return cvModelDir;
//    }
//
//    private String jointTypeTrainingNoDD(Configuration config, CollectionReaderDescription trainingReader,
//                                         String suffix, boolean skipTrain, int initialSeed) throws
//            UIMAException, NoSuchMethodException, InstantiationException, IOException,
//            IllegalAccessException, InvocationTargetException, ClassNotFoundException {
//        logger.info("Running joint training (without DD) of mention detection and coreference.");
//        String cvModelDir = joinPaths(outputModelDir, config.get("edu.cmu.cs.lti.model.joint.dir"), suffix);
//
//        boolean modelExists = new File(cvModelDir).exists();
//
//        MutableInt trainingSeed = new MutableInt(initialSeed);
//
//        if (skipTrain && modelExists) {
//            logger.info("Skipping joint training, taking existing models.");
//        } else {
//            logger.info("Saving model directory at : " + cvModelDir);
//
//            AnalysisEngineDescription corefEngine = AnalysisEngineFactory.createEngineDescription(
//                    PlainJointTypeTrainer.class, typeSystemDescription,
//                    PlainJointTypeTrainer.PARAM_CONFIG_PATH, config.getConfigFile().getPath()
//            );
//
//            TrainingLooper corefTrainer = new TrainingLooper(config, cvModelDir, trainingReader, corefEngine) {
//                @Override
//                protected void loopActions() {
//                    super.loopActions();
//                    trainingSeed.add(2);
//                    trainingReader.setAttributeValue(RandomizedXmiCollectionReader.PARAM_SEED, trainingSeed
// .getValue());
//                }
//
//                @Override
//                protected void finish() throws IOException {
//                    PlainJointTypeTrainer.finish();
//                }
//
//                @Override
//                protected void saveModel(File modelOutputDir) throws IOException {
//                    PlainJointTypeTrainer.saveModels(modelOutputDir);
//                }
//            };
//
//            corefTrainer.runLoopPipeline();
//
//            logger.info("Plain Joint Training training finished ...");
//        }
//        return cvModelDir;
//    }

    public String trainJointSpanModel(Configuration config, CollectionReaderDescription trainingReader,
                                      String suffix, boolean skipTrain, int initialSeed, String realisModelDir,
                                      String pretrainedMentionModel, String modelDir)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Start beam based joint training.");
        String cvModelDir = joinPaths(outputModelDir, modelDir, suffix);

        boolean modelExists = new File(cvModelDir).exists();

        if (skipTrain && modelExists) {
            logger.info("Skipping beam joint training, taking existing models.");
        } else {
            logger.info("Saving model directory at : " + cvModelDir);

            AnalysisEngineDescription corefEngine = AnalysisEngineFactory.createEngineDescription(
                    DelayedLaSOJointTrainer.class, typeSystemDescription,
                    DelayedLaSOJointTrainer.PARAM_CONFIG_PATH, config.getConfigFile().getPath(),
                    DelayedLaSOJointTrainer.PARAM_REALIS_MODEL_DIRECTORY, realisModelDir,
                    DelayedLaSOJointTrainer.PARAM_PRETRAINED_MENTION_MODEL_DIRECTORY, pretrainedMentionModel
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
                                                           String modelDir, String mainDir, String outputBase,
                                                           boolean skipTest) throws
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
                            JointMentionCorefAnnotator.PARAM_MODEL_DIRECTORY, modelDir
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

    public void trainAll(Configuration config, boolean skipTypeTrain, boolean skipLv2TypeTrain, boolean skipRealisTrain,
                         boolean skipCorefTrain, int initialSeed)
            throws Exception {
        logger.info("Staring training a full model on all data in " + preprocessBase);
        CollectionReaderDescription trainingReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, preprocessBase);

        CollectionReaderDescription trainingData = prepareTraining(trainingReader, trainingWorkingDir,
                joinPaths(middleResults, fullRunSuffix, "prepared_training"), false, initialSeed
        );

        // Get type rules.
        logger.info("Collecting constraint rules.");

        File corefRuleFile = new File(joinPaths(outputModelDir, "ruleFile"));
        collectCorefConstraint(trainingReader, corefRuleFile);

        logger.info("Done collecting rules.");

//        CollectionReaderDescription trainingDataMergedType = prepareTraining(trainingReader,
//                trainingWorkingDir, joinPaths(middleResults, fullRunSuffix, "prepared_training"), true, initialSeed);
//
//        // Get type rules.
//        File corefRuleFile = new File(joinPaths(trainingWorkingDir, middleResults, fullRunSuffix, "ruleFile"));
//        collectCorefConstraint(trainingDataMergedType, corefRuleFile);

//        // Train lv1 of the mention type model.
//        String sentCrfModelWithMerged = trainSentLvType(config, trainingDataMergedType, fullRunSuffix + "_merged",
//                skipTypeTrain, initialSeed);

        trainSentLvType(config, trainingData, fullRunSuffix, skipCorefTrain, false, initialSeed);

        trainSentLvType(config, trainingData, fullRunSuffix + "_pa", skipCorefTrain, true, initialSeed);

        // Train lv2 of the mention type model.
        trainDocLvType(config, trainingData, fullRunSuffix, skipLv2TypeTrain, initialSeed);

        // Train realis model.
        trainRealisTypes(config, trainingData, fullRunSuffix, skipRealisTrain);

        // Train coref model without merged types.
        trainLatentTreeCoref(config, trainingData, fullRunSuffix, skipCorefTrain, initialSeed,
                config.get("edu.cmu.cs.lti.model.event.latent_tree"));

//        // Train coref model.
//        String treeCorefModelWithMerged = trainLatentTreeCoref(config, trainingDataMergedType, fullRunSuffix +
//                "_merged", skipCorefTrain, initialSeed, config.get("edu.cmu.cs.lti.model.event.latent_tree"));

        logger.info("All training done.");
    }

    /**
     * Run a test, with all the intermediate results retained.
     *
     * @param testConfig The test configuration file.
     * @throws UIMAException
     * @throws IOException
     */
    public void test(Configuration testConfig, boolean skipLv1Test, boolean skipLv2Test, boolean skipRealisTest,
                     boolean skipCorefTest, boolean skipJointTest) throws UIMAException, IOException,
            CpeDescriptorException, SAXException {
        CollectionReaderDescription testDataReader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, testingWorkingDir, preprocessBase);

        File corefRuleFile = new File(joinPaths(outputModelDir, "ruleFile"));

        String sentCrfModel = joinPaths(outputModelDir,
                testConfig.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir"), fullRunSuffix);

        String paSentCrfModel = joinPaths(outputModelDir,
                testConfig.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir"), fullRunSuffix + "_pa");

//        // Run document level crf for mention type refinement.
//        String docCrfModel = joinPaths(outputModelDir,
//                testConfig.get("edu.cmu.cs.lti.model.crf.mention.lv2.dir"), fullRunSuffix);

        // Run realis on Lv1 crf mentions.
        String realisModel = joinPaths(outputModelDir,
                testConfig.get("edu.cmu.cs.lti.model.realis.dir"), fullRunSuffix);

        String lv1OutputBase = joinPaths(middleResults, fullRunSuffix, "mention_lv1");

        // Run mention level crf for mention detection.
        logger.info("Going to run mention type on [" + testingWorkingDir + "/"
                + preprocessBase + "], output will be at " + lv1OutputBase);
        CollectionReaderDescription sentMentions = sentenceLevelMentionTagging(testConfig, testDataReader,
                sentCrfModel, testingWorkingDir, lv1OutputBase, skipLv1Test);

        logger.info("Going to run PA mention type on [" + testingWorkingDir + "/"
                + preprocessBase + "], output will be at " + lv1OutputBase + "_pa");
        CollectionReaderDescription sentMentionsPa = sentenceLevelMentionTagging(testConfig, testDataReader,
                paSentCrfModel, testingWorkingDir, lv1OutputBase + "_pa", skipLv1Test);

        String lv1RealisOutput = joinPaths(middleResults, fullRunSuffix, "lv1_realis");

        logger.info("Going to run realis classifier on " + lv1OutputBase + " output will be at " + lv1RealisOutput);
        CollectionReaderDescription lv1MentionRealisResults = realisAnnotation(testConfig, sentMentions,
                realisModel, testingWorkingDir, lv1RealisOutput, skipRealisTest);

        logger.info("Going to run realis classifier on " + lv1OutputBase + " output will be at " + lv1RealisOutput +
                "_pa");
        CollectionReaderDescription paMentionRealisResults = realisAnnotation(testConfig, sentMentionsPa,
                realisModel, testingWorkingDir, lv1RealisOutput + "_pa", skipRealisTest);

//        // Get span only mention detection
//        CollectionReaderDescription goldSpans = goldMentionAnnotator(testDataReader, testingWorkingDir,
//                joinPaths(middleResults, fullRunSuffix, "gold_span"), false, false, false, true
//                    /*Copy span only, merge types*/);
//
//        // Gold mention types.
//        CollectionReaderDescription goldMentionTypes = goldMentionAnnotator(testDataReader, testingWorkingDir,
//                joinPaths(middleResults, fullRunSuffix, "gold_type"), true, false, false, false
//                    /* copy type, not realis, not coref, not merge types*/);
//
//        // Run realis on gold types.
//        CollectionReaderDescription goldTypeSystemRealis = realisAnnotation(testConfig, goldMentionTypes,
//                realisModel, testingWorkingDir, joinPaths(middleResults, fullRunSuffix, "gold_system_realis"),
//                skipRealisTest);
//
//        // Run realis on gold spans.
//        CollectionReaderDescription goldSpanSystemRealis = realisAnnotation(testConfig, goldSpans,
//                realisModel, testingWorkingDir, joinPaths(middleResults, fullRunSuffix, "gold_span_system_realis"),
//                skipRealisTest);
//
//
//        // Run gold mention detection.
//        CollectionReaderDescription goldMentionAll = goldMentionAnnotator(testDataReader, testingWorkingDir,
//                joinPaths(middleResults, fullRunSuffix, "gold_mentions"), true, true, false, false
//                    /* copy type, realis, not coref, merge types*/);

        // Run coreference on dev data.
        String corefModel = joinPaths(outputModelDir,
                testConfig.get("edu.cmu.cs.lti.model.event.latent_tree"), fullRunSuffix);

        CollectionReaderDescription corefSentMentions = corefResolution(testConfig,
                lv1MentionRealisResults, corefModel, testingWorkingDir,
                joinPaths(middleResults, fullRunSuffix, "coref_lv1_mention"), corefRuleFile, true,
                skipCorefTest);

        CollectionReaderDescription corefPaSentMentions = corefResolution(testConfig,
                paMentionRealisResults, corefModel, testingWorkingDir,
                joinPaths(middleResults, fullRunSuffix, "coref_lv1_mention"), corefRuleFile, true,
                skipCorefTest);

        // Output final result.
        String evalDir = joinPaths(testingWorkingDir, evalBase, "full_run");

        writeResults(corefSentMentions, joinPaths(evalDir, "lv1_coref_" + fullRunSuffix + ".tbf"), "lv1_coref");

        writeResults(corefPaSentMentions, joinPaths(evalDir, "lv1_coref_pa_" + fullRunSuffix + ".tbf"), "lv1_coref_pa");

//        String lv2OutputBase = joinPaths(middleResults, fullRunSuffix, "mention_lv2");
//        logger.info("Going to run document level mention type refinement on [" + testingWorkingDir + "/"
//                + preprocessBase + "], output will be at " + lv2OutputBase);
//        CollectionReaderDescription docMentions = docLevelMentionTagging(testConfig, lv1MentionRealisResults,
//                docCrfModel, testingWorkingDir, lv2OutputBase, skipLv2Test);
//
//        // Run document crf model on gold span, system realis
//        CollectionReaderDescription docTypeGoldSpan = docLevelMentionTagging(testConfig, goldSpanSystemRealis,
//                docCrfModel, testingWorkingDir, joinPaths(middleResults, fullRunSuffix, "gold_span_lv2_type"),
//                skipLv2Test);

        //        String corefModelMerged = joinPaths(outputModelDir,
//                testConfig.get("edu.cmu.cs.lti.model.event.latent_tree"), fullRunSuffix + "_merged");

//        CollectionReaderDescription corefGoldType = corefResolution(testConfig, goldTypeSystemRealis,
//                corefModel, testingWorkingDir,
//                joinPaths(middleResults, fullRunSuffix, "coref_gold_type"), corefRuleFile, true, skipCorefTest);
//        CollectionReaderDescription corefGoldTypeRealis = corefResolution(testConfig, goldMentionAll,
//                corefModel, testingWorkingDir,
//                joinPaths(middleResults, fullRunSuffix, "coref_gold_type+realis"), corefRuleFile, true,
// skipCorefTest);

//        CollectionReaderDescription corefDocMentionsNoMerged = corefResolution(testConfig,
//                docMentions, corefModel, testingWorkingDir,
//                joinPaths(middleResults, fullRunSuffix, "coref_lv2_mention_no_merge"), corefRuleFile, true,
//                skipCorefTest);
//
//        CollectionReaderDescription corefGoldSpanNoMerged = corefResolution(testConfig, docTypeGoldSpan,
//                corefModel, testingWorkingDir,
//                joinPaths(middleResults, fullRunSuffix, "coref_gold_span_no_merge"), corefRuleFile, true,
//                skipCorefTest);

//        writeResults(corefGoldType, joinPaths(evalDir, "gold_type_coref_" + fullRunSuffix + ".tbf"),
//                "gold_type_coref");
//        writeResults(corefGoldTypeRealis, joinPaths(evalDir, "gold_type_realis_coref_" + fullRunSuffix + ".tbf"),
//                "gold_type_realis_coref");

//        writeResults(corefDocMentionsNoMerged, joinPaths(evalDir, "lv2_coref_" + fullRunSuffix + ".tbf"),
//                "lv2_coref");
//        writeResults(corefGoldSpanNoMerged, joinPaths(evalDir, "coref_gold_span_" + fullRunSuffix + ".tbf"),
//                "coref_gold_span");

//        // Run Joint mention + Coref on gold spans, system realis.
//        CollectionReaderDescription jointOnGoldSpan = joinMentionDetectionAndCoreference(testConfig,
//                goldSpanSystemRealis, docCrfModel, corefModel, corefRuleFile, testingWorkingDir,
//                joinPaths(middleResults, fullRunSuffix, "joint_mention_coref_on_gold_span"), skipJointTest);
//        // Run Joint mention + Coref on predicted mentions
//        CollectionReaderDescription joint = joinMentionDetectionAndCoreference(testConfig,
//                lv1MentionRealisResults, docCrfModel, corefModel, corefRuleFile,
//                testingWorkingDir, joinPaths(middleResults, fullRunSuffix, "joint_mention_coref"), skipJointTest);
//
//        writeResults(jointOnGoldSpan, joinPaths(evalDir, "joint_gold_span_" + fullRunSuffix + ".tbf"), "joint");
//        writeResults(joint, joinPaths(evalDir, "joint_" + fullRunSuffix + ".tbf"), "joint");
    }

    private String joinPaths(String... segments) {
        return edu.cmu.cs.lti.utils.FileUtils.joinPaths(segments);
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

    /**
     * Conduct cross validation on various tasks.
     *
     * @param taskConfig
     * @throws Exception
     */
    public void crossValidation(Configuration taskConfig) throws Exception {
        int numSplit = taskConfig.getInt("edu.cmu.cs.lti.cv.split", 5);
        int seed = taskConfig.getInt("edu.cmu.cs.lti.random.seed", 17);

        String corefEval = joinPaths(trainingWorkingDir, evalBase, "tree_coref");
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(corefEval);

        boolean skipCorefTrain = taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptrain", false);
        boolean skipTypeTrain = taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptrain", false);
        boolean skipRealisTrain = taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptrain", false);
        boolean skipBeamTrain = taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptrain", false);

        boolean skipTypeTest = taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptest", false);
        boolean skipRealisTest = taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptest", false);
        boolean skipCorefTest = taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptest", false);
        boolean skipBeamTest = taskConfig.getBoolean("edu.cmu.cs.lti.joint_span.skiptest", false);

/**
 * Deprecated lv2 and DD based joint training.
 boolean skipLv2TypeTrain = taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.lv2.skiptrain", false);
 boolean skipLv2Test = taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.lv2.skiptest", false);
 boolean skipJointTest = taskConfig.getBoolean("edu.cmu.cs.lti.joint.skiptest", false);
 **/

        logger.info(String.format("Will conduct %d fold cross validation.", numSplit));
        for (int slice = 0; slice < numSplit; slice++) {
            logger.info("Starting CV split " + slice);

            String sliceSuffix = "split_" + slice;

            CollectionReaderDescription trainingSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, trainingWorkingDir, preprocessBase, false, seed, numSplit, slice);
            CollectionReaderDescription devSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, trainingWorkingDir, preprocessBase, true, seed, numSplit, slice);

            // Create training data.
//            CollectionReaderDescription trainingDataNotMerged = prepareTraining(trainingSliceReader,
//                    trainingWorkingDir, joinPaths(middleResults, sliceSuffix, "prepared_training"), false, seed
//            );
            CollectionReaderDescription trainingData = prepareTraining(trainingSliceReader,
                    trainingWorkingDir, joinPaths(middleResults, sliceSuffix, "prepared_training"), false,
                    seed);

            // Get type rules.
            logger.info("Collecting constraint rules.");
            File corefRuleFile = new File(joinPaths(trainingWorkingDir, middleResults, sliceSuffix, "ruleFile"));
            collectCorefConstraint(trainingData, corefRuleFile);
            logger.info("Done collecting rules.");

            // Train lv1 of the mention type model.
//            String sentCrfModelNotMerged = trainSentLvType(taskConfig, trainingDataNotMerged, sliceSuffix,
//                    skipTypeTrain, seed);

            String vanillaSentCrfModel = trainSentLvType(taskConfig, trainingData, sliceSuffix, skipTypeTrain, false,
                    seed);

            // Mention types from the sentence crf model.
            CollectionReaderDescription vanillaSentTypeMentions = sentenceLevelMentionTagging(taskConfig,
                    devSliceReader, vanillaSentCrfModel, trainingWorkingDir,
                    joinPaths(middleResults, sliceSuffix, "mentions_lv1"), skipTypeTest);

//            String paSentCrfModel = trainSentLvType(taskConfig, trainingData, sliceSuffix + "_pa",
//                    skipTypeTrain, true, seed);

            // Train realis model.
            String realisModelDir = trainRealisTypes(taskConfig, trainingData, sliceSuffix, skipRealisTrain);

            // Train Beam search joint model.
            String beamJointModel = trainJointSpanModel(taskConfig, trainingData, sliceSuffix,
                    skipBeamTrain, seed, realisModelDir, vanillaSentCrfModel,
                    taskConfig.get("edu.cmu.cs.lti.model.joint.span.dir"));

            // Train coref model.
            String treeCorefModel = trainLatentTreeCoref(taskConfig, trainingData, sliceSuffix, skipCorefTrain, seed,
                    taskConfig.get("edu.cmu.cs.lti.model.event.latent_tree"));

//            // Train coref model without merged types.
//            String treeCorefModelNotMerged = trainLatentTreeCoref(taskConfig, trainingDataNotMerged, sliceSuffix,
//                    skipCorefTrain, seed, taskConfig.get("edu.cmu.cs.lti.model.event.latent_tree"));


//            CollectionReaderDescription paSentTypeMentions = sentenceLevelMentionTagging(taskConfig,
//                    devSliceReader, paSentCrfModel, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "mentions_lv1_merged_pa"), skipTypeTest);


            // Run realis on sentence level crf mentions.
            CollectionReaderDescription vanillaCrfMentionRealis = realisAnnotation(taskConfig,
                    vanillaSentTypeMentions, realisModelDir, trainingWorkingDir,
                    joinPaths(middleResults, sliceSuffix, "lv1_realis_merged"), skipRealisTest);

            // Run realis on pa trained crf mentions.
//            CollectionReaderDescription paCrfMentionRealis = realisAnnotation(taskConfig,
//                    paSentTypeMentions, realisModelDir, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "lv1_realis_merged_pa"), skipRealisTest);

//            // Get span only mention detection
//            CollectionReaderDescription goldSpans = goldMentionAnnotator(devSliceReader, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "gold_span"), false, false, false, true
//                    /*Copy span only, merge types*/);

            // Gold mention types.
            CollectionReaderDescription goldMentionTypes = goldMentionAnnotator(devSliceReader, trainingWorkingDir,
                    joinPaths(middleResults, sliceSuffix, "gold_type"), true, false, false, true
                    /* copy type, not realis, not coref, merge types*/);

            // Run realis on gold types.
            CollectionReaderDescription goldTypeSystemRealis = realisAnnotation(taskConfig, goldMentionTypes,
                    realisModelDir, trainingWorkingDir, joinPaths(middleResults, sliceSuffix, "gold_system_realis"),
                    skipRealisTest);

            // Run gold mention detection.
            CollectionReaderDescription goldMentionAll = goldMentionAnnotator(devSliceReader, trainingWorkingDir,
                    joinPaths(middleResults, sliceSuffix, "gold_mentions"), true, true, false, true
                    /* copy type, realis, not coref, merge types*/);

            // Run coreference on dev data.
            CollectionReaderDescription corefSentMentions = corefResolution(taskConfig,
                    vanillaCrfMentionRealis, treeCorefModel, trainingWorkingDir,
                    joinPaths(middleResults, sliceSuffix, "coref_lv1_mention"), corefRuleFile, true,
                    skipCorefTest);
//            CollectionReaderDescription corefSentMentionsNoMerge = corefResolution(taskConfig,
//                    vanillaCrfMentionRealis, treeCorefModelNotMerged, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "coref_lv1_mention_no_merge"), corefRuleFile, true,
//                    skipCorefTest);
//            CollectionReaderDescription corefPaMentionsNoMerge = corefResolution(taskConfig,
//                    paCrfMentionRealis, treeCorefModelNotMerged, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "coref_lv1_mention_pa_no_merge"), corefRuleFile, true,
//                    skipCorefTest);
            CollectionReaderDescription corefGoldType = corefResolution(taskConfig, goldTypeSystemRealis,
                    treeCorefModel, trainingWorkingDir,
                    joinPaths(middleResults, sliceSuffix, "coref_gold_type"), corefRuleFile, true, skipCorefTest);
            CollectionReaderDescription corefGoldTypeRealis = corefResolution(taskConfig, goldMentionAll,
                    treeCorefModel, trainingWorkingDir,
                    joinPaths(middleResults, sliceSuffix, "coref_gold_type+realis"), corefRuleFile, true,
                    skipCorefTest);

//            // Run joint model.
//            CollectionReaderDescription beamJointResults = beamJointSpanCoref(taskConfig, devSliceReader,
//                    beamJointModel, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "coref_joint_span"), skipBeamTest);

            // Write the predictions.
            writeResults(corefGoldType,
                    joinPaths(corefEval, "gold_type_coref_" + sliceSuffix + ".tbf"), "gold_types"
            );
            writeResults(
                    corefGoldTypeRealis,
                    joinPaths(corefEval, "gold_type_realis_coref_" + sliceSuffix + ".tbf"), "tree_coref"
            );
            writeResults(
                    corefSentMentions,
                    joinPaths(corefEval, "lv1_coref_" + sliceSuffix + ".tbf"), "tree_coref"
            );
//            writeResults(
//                    corefSentMentionsNoMerge, joinPaths(corefEval, "lv1_coref_" + sliceSuffix + ".tbf"), "tree_coref"
//            );
//            writeResults(
//                    corefPaMentionsNoMerge, joinPaths(corefEval, "lv1_pa_coref_" + sliceSuffix + ".tbf"), "tree_coref"
//            );
//            writeResults(
//                    beamJointResults, joinPaths(corefEval, "joint_beam_" + sliceSuffix + ".tbf"), "tree_coref"
//            );

            // Produce gold standard tbf for easy evaluation.
            writeGold(devSliceReader, joinPaths(corefEval, "gold_" + sliceSuffix + ".tbf"));

//            // Train lv2 of the mention type model.
//            String docCrfModel = trainDocLvType(taskConfig, trainingData, sliceSuffix, skipLv2TypeTrain,
//                    seed);
//
//            // Run realis on gold spans.
//            CollectionReaderDescription goldSpanSystemRealis = realisAnnotation(taskConfig, goldSpans,
//                    realisModelDir, trainingWorkingDir, joinPaths(middleResults, sliceSuffix,
//                            "gold_span_system_realis"), skipRealisTest);
//
//            // Run document crf model on gold span, system realis
//            CollectionReaderDescription docTypeGoldSpan = docLevelMentionTagging(taskConfig, goldSpanSystemRealis,
//                    docCrfModel, trainingWorkingDir, joinPaths(middleResults, sliceSuffix, "gold_span_lv2_type"),
//                    skipLv2Test);
//
//            // Mention types from the document crf model.
//            CollectionReaderDescription docTypeMentionsRealisMerged = docLevelMentionTagging(taskConfig,
//                    sentCrfMentionRealisMerged, docCrfModel, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "lv2_realis_merged"), skipLv2Test);
//
//            CollectionReaderDescription docTypeMentionsRealisNoMerge = docLevelMentionTagging(taskConfig,
//                    sentCrfMentionRealisNoMerge, docCrfModel, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "lv2_realis"), skipLv2Test);
//


//            CollectionReaderDescription corefDocMentions = corefResolution(taskConfig, docTypeMentionsRealisMerged,
//                    treeCorefModel, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "coref_lv2_mention_merged"), true, skipCorefTest);

//            CollectionReaderDescription corefDocMentionsNoMerged = corefResolution(taskConfig,
//                    docTypeMentionsRealisNoMerge, treeCorefModelNotMerged, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "coref_lv2_mention_no_merge"), corefRuleFile, true,
//                    skipCorefTest);
////
//            CollectionReaderDescription corefGoldSpanMerged = corefResolution(taskConfig, docTypeGoldSpan,
//                    treeCorefModel, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "coref_gold_span"), corefRuleFile, true, skipCorefTest);
//            CollectionReaderDescription corefGoldSpanNoMerged = corefResolution(taskConfig, docTypeGoldSpan,
//                    treeCorefModelNotMerged, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "coref_gold_span_no_merge"), corefRuleFile, true,
//                    skipCorefTest);

//            writeResults(corefDocMentions, joinPaths(corefEval, "lv2_coref_merged_" + sliceSuffix + ".tbf"),
//                    "tree_coref", false);
//            writeResults(corefDocMentionsNoMerged, joinPaths(corefEval, "lv2_coref_" + sliceSuffix + ".tbf"),
//                    "tree_coref");
//            writeResults(corefGoldSpanMerged, joinPaths(corefEval, "coref_gold_span_merged_" + sliceSuffix + ".tbf"),
//                    "tree_coref", false);
//            writeResults(corefGoldSpanNoMerged, joinPaths(corefEval, "coref_gold_span_" + sliceSuffix + ".tbf"),
//                    "tree_coref");

////            // Run Joint mention + Coref on gold spans, system realis.
//            CollectionReaderDescription jointOnGoldSpan = joinMentionDetectionAndCoreference(taskConfig,
//                    goldSpanSystemRealis, docCrfModel, treeCorefModelNotMerged, corefRuleFile, trainingWorkingDir,
//                    joinPaths(middleResults, sliceSuffix, "joint_mention_coref_on_gold_span"), skipJointTest);

//            // Run Joint mention + Coref on predicted mentions
//            CollectionReaderDescription joint = joinMentionDetectionAndCoreference(taskConfig,
//                    sentCrfMentionRealisNoMerge, docCrfModel, treeCorefModelNotMerged, corefRuleFile,
//                    trainingWorkingDir, joinPaths(middleResults, sliceSuffix, "joint_mention_coref"), skipJointTest);
//
//            writeResults(joint, joinPaths(corefEval, "joint_" + sliceSuffix + ".tbf"), "joint");
//            writeResults(jointOnGoldSpan, joinPaths(corefEval, "joint_gold_span_" + sliceSuffix + ".tbf"), "joint");
        }
    }
}
