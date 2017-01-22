package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.emd.annotators.CrfMentionTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.TokenBasedMentionErrorAnalyzer;
import edu.cmu.cs.lti.emd.annotators.postprocessors.MentionTypeSplitter;
import edu.cmu.cs.lti.emd.annotators.train.TokenLevelEventMentionCrfTrainer;
import edu.cmu.cs.lti.emd.pipeline.TrainingLooper;
import edu.cmu.cs.lti.learning.utils.ModelUtils;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.pipeline.ProcessorWrapper;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.reader.RandomizedXmiCollectionReader;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/11/16
 * Time: 9:52 PM
 *
 * @author Zhengzhong Liu
 */
public class TokenMentionModelRunner extends AbstractMentionModelRunner {
    public TokenMentionModelRunner(Configuration config, TypeSystemDescription typeSystemDescription) {
        super(config, typeSystemDescription);
    }

    public String getModelPath(Configuration config, String suffix, String lossType) {
        return ModelUtils.getTrainModelPath(eventModelDir, config, suffix, "loss=" + lossType);
    }

    public String trainSentLvType(Configuration config, CollectionReaderDescription trainingReader,
                                  CollectionReaderDescription testReader, String suffix, boolean usePaTraing,
                                  String lossType, String processOutputDir, File testGold,
                                  boolean skipTrain, boolean skipTest)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Starting training sentence level mention type model ...");

        String subEvalDir = suffix.equals(fullRunSuffix) ? "final" : "cv";

        String modelPath = getModelPath(config, suffix, "loss=" + lossType);
        File modelFile = new File(modelPath);


        MutableInt trainingSeed = new MutableInt(config.getInt("edu.cmu.cs.lti.random.seed", 17));
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
                                    subEvalDir, testGold, skipTest);
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

    /**
     * Test the token based mention model and return the result directory as a reader
     */
    public CollectionReaderDescription testPlainMentionModel(Configuration taskConfig,
                                                             CollectionReaderDescription reader, String typeModel,
                                                             String sliceSuffix, String runName, String outputDir,
                                                             String subEval, File gold, boolean skipTest)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        return new ModelTester(mainConfig, "token_based_mention") {
            @Override
            CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException,
                    CpeDescriptorException, IOException {
                return sentenceLevelMentionTagging(taskConfig, reader, typeModel,
                        trainingWorkingDir, baseDir, skipTest);
            }
        }.run(taskConfig, reader, sliceSuffix, runName, outputDir, subEval, gold);
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

    public void tokenMentionErrorAnalysis(Configuration taskConfig,
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
}
