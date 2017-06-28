package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.emd.pipeline.TrainingLooper;
import edu.cmu.cs.lti.event_coref.annotators.JointMentionCorefAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.misc.GoldRemover;
import edu.cmu.cs.lti.event_coref.annotators.train.BeamJointTrainer;
import edu.cmu.cs.lti.learning.utils.ModelUtils;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
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
 * Time: 2:37 PM
 *
 * @author Zhengzhong Liu
 */
public class JointSpanCorefModelRunner extends AbstractMentionModelRunner {
    public JointSpanCorefModelRunner(Configuration mainConfig, TypeSystemDescription typeSystemDescription) {
        super(mainConfig, typeSystemDescription);
    }


    public String trainJointSpanModel(Configuration config, CollectionReaderDescription trainReader,
                                      CollectionReaderDescription testReader, String realisModelDir,
                                      String processOutputDir, String sliceSuffix,
                                      File testGold, boolean skipTrain, boolean skipTest,
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
                        String runName = "joint_heldout_iter" + numIteration;
                        String modelPath = cvModelDir + "_iter" + numIteration;
                        test(modelPath, runName);
                    }
                    return modelSaved;
                }

                @Override
                protected void finish() throws IOException {
                    BeamJointTrainer.finish();
                    // Test using the final model.
                    String runName = "joint_heldout_final";
                    String modelPath = cvModelDir + "_iter" + numIteration;
                    test(modelPath, runName);
                }

                private void test(String model, String runName) {
                    if (testReader != null) {
                        try {
                            testJoint(config, testReader, model, runName, beamSize, realisModelDir, skipTest,
                                    processOutputDir, subEvalDir, testGold, sliceSuffix);
                        } catch (SAXException | InterruptedException | IOException | CpeDescriptorException |
                                UIMAException e) {
                            e.printStackTrace();
                        }
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


    private void testJoint(Configuration taskConfig, CollectionReaderDescription devReader, String modelDir,
                           String runName, int beamSize, String realisModelDir, boolean skipTest,
                           String processOutputDir, String subEvalDir, File goldStandard, String sliceSuffix)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        new ModelTester(mainConfig) {
            @Override
            protected CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription
                    reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return beamJointSpanCoref(taskConfig, devReader, modelDir, realisModelDir, trainingWorkingDir,
                        processOutputDir, beamSize, true, skipTest);
            }
        }.run(taskConfig, devReader, typeSystemDescription, sliceSuffix, runName, processOutputDir, goldStandard);

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

            return new BasicPipeline(reader, mainDir, outputBase, goldRemover, jointDecoder).run().getOutput();
        }
    }

}
