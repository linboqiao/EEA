package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.after.annotators.PairwiseAfterAnnotator;
import edu.cmu.cs.lti.after.train.PairwiseAfterTrainer;
import edu.cmu.cs.lti.emd.pipeline.TrainingLooper;
import edu.cmu.cs.lti.event_coref.annotators.train.BeamJointTrainer;
import edu.cmu.cs.lti.learning.utils.ModelUtils;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.pipeline.ProcessorWrapper;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.utils.Configuration;
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
 * Date: 12/12/16
 * Time: 3:26 PM
 *
 * @author Zhengzhong Liu
 */
public class AfterModelRunner extends AbstractMentionModelRunner {
    public AfterModelRunner(Configuration mainConfig, TypeSystemDescription typeSystemDescription) {
        super(mainConfig, typeSystemDescription);
    }

    public String trainAfterModel(Configuration config, CollectionReaderDescription trainReader,
                                  CollectionReaderDescription testReader, String processOutputDir, String suffix,
                                  File testGold, boolean skipTrain, boolean skipTest)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Start after model training.");
        String cvModelDir = ModelUtils.getTrainModelPath(eventModelDir, config, suffix);

        String subEvalDir = suffix.equals(fullRunSuffix) ? "final" : "cv";

        int jointMaxIter = config.getInt("edu.cmu.cs.lti.perceptron.joint.maxiter", 30);
        int modelOutputFreq = config.getInt("edu.cmu.cs.lti.perceptron.model.save.frequency", 1);

        boolean modelExists = new File(cvModelDir).exists();

        if (skipTrain && modelExists) {
            logger.info("Skipping beam joint training, taking existing models.");
        } else {
            logger.info("Saving model directory at : " + cvModelDir);
            AnalysisEngineDescription trainEngine = AnalysisEngineFactory.createEngineDescription(
                    PairwiseAfterTrainer.class, typeSystemDescription
            );

            TrainingLooper trainer = new TrainingLooper(cvModelDir, trainReader, trainEngine, jointMaxIter,
                    modelOutputFreq) {
                @Override
                protected boolean loopActions() {
                    boolean modelSaved = super.loopActions();
                    BeamJointTrainer.loopAction();

                    if (modelSaved) {
                        String modelPath = cvModelDir + "_iter" + numIteration;
                        test(modelPath, "after_heldout_iter" + numIteration);
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
                            testAfter(config, testReader, model, suffix, runName, processOutputDir,
                                    subEvalDir, testGold, skipTest);
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

    /**
     * Test the token based mention model and return the result directory as a reader
     */
    public CollectionReaderDescription testAfter(Configuration taskConfig,
                                                 CollectionReaderDescription reader, String afterModel,
                                                 String sliceSuffix, String runName, String outputDir,
                                                 String subEval, File gold, boolean skipTest)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        return new ModelTester(mainConfig, "token_based_mention") {
            @Override
            CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException,
                    CpeDescriptorException, IOException {
                return afterLinking(taskConfig, reader, afterModel, trainingWorkingDir, baseDir, skipTest);
            }
        }.run(taskConfig, reader, sliceSuffix, runName, outputDir, subEval, gold);
    }

    public CollectionReaderDescription afterLinking(Configuration taskConfig, CollectionReaderDescription reader,
                                                    String model, String mainDir, String baseOutput, boolean skipTest)
            throws UIMAException, SAXException, CpeDescriptorException, IOException {
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
                    AnalysisEngineDescription afterLinker = AnalysisEngineFactory.createEngineDescription(
                            PairwiseAfterAnnotator.class, typeSystemDescription,
                            PairwiseAfterAnnotator.PARAM_MODEL_DIRECTORY, model,
                            PairwiseAfterAnnotator.PARAM_CONFIG, taskConfig.getConfigFile().getPath()
                    );

                    return new AnalysisEngineDescription[]{afterLinker};
                }
            }, mainDir, baseOutput).runWithOutput();
        }
    }

}
