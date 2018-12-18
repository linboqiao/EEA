package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.emd.annotators.BeamTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.postprocessors.MentionTypeSplitter;
import edu.cmu.cs.lti.emd.annotators.train.BeamBasedMentionTypeTrainer;
import edu.cmu.cs.lti.emd.pipeline.TrainingLooper;
import edu.cmu.cs.lti.learning.utils.ModelUtils;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
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
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/12/16
 * Time: 3:08 PM
 *
 * @author Zhengzhong Liu
 */
public class BeamMentionModelRunner extends AbstractMentionModelRunner {
    public BeamMentionModelRunner(Configuration mainConfig, TypeSystemDescription typeSystemDescription) {
        super(mainConfig, typeSystemDescription);
    }

    public String trainBeamTypeModel(Configuration config, CollectionReaderDescription trainingReader, String suffix,
                                     boolean usePaTraing, String lossType, boolean useLaSO, boolean delayedLaso,
                                     float aggressiveParameter, int initialSeed, CollectionReaderDescription testReader,
                                     File goldStandard, String processOutputDir, String resultDir, boolean skipTrain,
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
                    BeamBasedMentionTypeTrainer.PARAM_USE_PA_UPDATE, usePaTraing,
                    BeamBasedMentionTypeTrainer.PARAM_LOSS_TYPE, lossType,
                    BeamBasedMentionTypeTrainer.PARAM_DELAYED_LASO, delayedLaso,
                    BeamBasedMentionTypeTrainer.PARAM_BEAM_SIZE, beamSize,
                    BeamBasedMentionTypeTrainer.PARAM_USE_LASO, useLaSO,
                    BeamBasedMentionTypeTrainer.PARAM_AGGRESSIVE_PARAMETER, aggressiveParameter,
                    BeamBasedMentionTypeTrainer.PARAM_TYPE_FILE_PATH,
                    FileUtils.joinPaths(mainConfig.get("edu.cmu.cs.lti.training.working.dir"), "mention_types.txt")
            );

            BeamBasedMentionTypeTrainer.setConfig(config);

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
                        testBeamMentionModels(config, testReader, model, suffix, runName, processOutputDir, resultDir,
                                goldStandard, skipTest);
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
            AnalysisEngineDescription sentenceLevelTagger = AnalysisEngineFactory.createEngineDescription(
                    BeamTypeAnnotator.class, typeSystemDescription,
                    BeamTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                    BeamTypeAnnotator.PARAM_BEAM_SIZE, beamSize
            );

            BeamTypeAnnotator.setConfig(taskConfig);

            AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                    MentionTypeSplitter.class, typeSystemDescription
            );

            return new BasicPipeline(reader, mainDir, baseOutput, sentenceLevelTagger, mentionSplitter).run()
                    .getOutput();
        }
    }


    private CollectionReaderDescription testBeamMentionModels(Configuration config, CollectionReaderDescription reader,
                                                              String typeModel, String sliceSuffix, String runName,
                                                              String outputDir, String resultDir, File gold,
                                                              boolean skipTest)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        int beamSize = config.getInt("edu.cmu.cs.lti.mention.beam.size", 5);

        return new ModelTester(mainConfig) {
            @Override
            protected CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription reader,
                                                           String mainDir, String baseDir)
                    throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return beamMentionTagging(taskConfig, reader, typeModel, outputDir, baseDir, beamSize,
                        skipTest);
            }
        }.run(config, reader, typeSystemDescription, sliceSuffix, runName, outputDir, resultDir, gold);
    }
}
