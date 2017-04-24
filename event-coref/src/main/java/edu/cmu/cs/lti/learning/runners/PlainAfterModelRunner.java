package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.after.annotators.GoldTemporalBaseline;
import edu.cmu.cs.lti.after.annotators.LatentTreeAfterAnnotator;
import edu.cmu.cs.lti.after.annotators.SelectedTemporalBaseline;
import edu.cmu.cs.lti.after.annotators.TemporalBaseline;
import edu.cmu.cs.lti.after.train.LatentTreeAfterTrainer;
import edu.cmu.cs.lti.emd.pipeline.TrainingLooper;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/12/16
 * Time: 3:26 PM
 *
 * @author Zhengzhong Liu
 */
public class PlainAfterModelRunner extends AbstractMentionModelRunner {

    public PlainAfterModelRunner(Configuration mainConfig, TypeSystemDescription typeSystemDescription) {
        super(mainConfig, typeSystemDescription);
    }

    public void runBaseline(Configuration config, CollectionReaderDescription testReader, String resultDir,
                            String suffix, File testGold)
            throws InterruptedException, SAXException, UIMAException, CpeDescriptorException, IOException {
        goldTemporalBaseline(config, testReader, suffix, resultDir, testGold);
    }

    public String trainAfterModel(Configuration config, CollectionReaderDescription trainReader,
                                  CollectionReaderDescription testReader, String processOutputDir, String suffix,
                                  File testGold, boolean skipTrain, boolean skipTest)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, CpeDescriptorException, InterruptedException,
            SAXException {
        logger.info("Start after model training.");
        String cvModelDir = ModelUtils.getTrainModelPath(eventModelDir, config, suffix);

        int maxIter = config.getInt("edu.cmu.cs.lti.perceptron.maxiter", 20);
        int modelOutputFreq = config.getInt("edu.cmu.cs.lti.perceptron.model.save.frequency", 3);

        boolean modelExists = new File(cvModelDir).exists();

        if (skipTrain && modelExists) {
            logger.info("Skipping after training, taking existing models.");
            logger.info("Directly run the test and evaluate the performance.");
            testAfter(config, testReader, cvModelDir, suffix, "test_only", processOutputDir,
                    testGold, skipTest);
        } else {
            logger.info("Saving model directory at : " + cvModelDir);
            AnalysisEngineDescription trainEngine = AnalysisEngineFactory.createEngineDescription(
                    LatentTreeAfterTrainer.class, typeSystemDescription,
                    LatentTreeAfterTrainer.PARAM_CONFIG_PATH, config.getConfigFile()
            );

            TrainingLooper trainer = new TrainingLooper(cvModelDir, trainReader, trainEngine, 2,
                    modelOutputFreq) {
                @Override
                protected boolean loopActions() {
                    boolean modelSaved = super.loopActions();

                    if (modelSaved) {
                        String modelPath = cvModelDir + "_iter" + numIteration;
                        test(modelPath, "after_heldout_iter" + numIteration);
                    }
                    return modelSaved;
                }

                @Override
                protected void finish() throws IOException {
                    // Test using the final model.
                    String runName = "after_heldout_final";
                    CollectionReaderDescription testOutput = test(cvModelDir, runName);
                    // Run the baselines.
                    baseline(testOutput);
                }

                private CollectionReaderDescription test(String model, String runName) {
                    if (testReader != null) {
                        try {
                            return testAfter(config, testReader, model, suffix, runName, processOutputDir,
                                    testGold, skipTest);
                        } catch (SAXException | InterruptedException | IOException | CpeDescriptorException |
                                UIMAException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }

                private void baseline(CollectionReaderDescription reader) {
                    if (testReader != null) {
                        try {
                            selectedTemporalBaseline(config, reader, suffix, processOutputDir, testGold);
                            temporalBaseline(config, reader, suffix, processOutputDir, testGold);
                        } catch (InterruptedException | SAXException | UIMAException | IOException |
                                CpeDescriptorException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                protected void saveModel(File modelOutputDir) throws IOException {
                    File modelOut = LatentTreeAfterTrainer.saveModels(modelOutputDir);
                    logger.info(String.format("Model is saved at : %s", modelOut));
                }
            };

            trainer.runLoopPipeline();

            logger.info("Tree Based After training finished ...");
        }
        return cvModelDir;
    }

    /**
     * Test the token based mention model and return the result directory as a reader
     */
    public CollectionReaderDescription testAfter(Configuration taskConfig,
                                                 CollectionReaderDescription reader, String afterModel,
                                                 String sliceSuffix, String runName, String outputDir,
                                                 File gold, boolean skipTest)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        return new ModelTester(mainConfig, "plain_after_model") {
            @Override
            protected CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription
                    reader, String mainDir, String baseDir)
                    throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return afterLinking(taskConfig, reader, afterModel, trainingWorkingDir, baseDir, skipTest);
            }
        }.run(taskConfig, reader, typeSystemDescription, sliceSuffix, runName, outputDir, gold);
    }

    private CollectionReaderDescription goldTemporalBaseline(Configuration taskConfig,
                                                             CollectionReaderDescription reader,
                                                             String sliceSuffix,
                                                             String outputDir,
                                                             File gold)
            throws InterruptedException, SAXException, UIMAException, CpeDescriptorException, IOException {
        logger.info("Running gold based temporal baseline.");
        String goldBaselineRun = "gold_temporal_baseline";
        return new ModelTester(mainConfig, "goldTemporal") {
            @Override
            protected CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription
                    reader, String mainDir, String baseDir) throws SAXException,
                    UIMAException, CpeDescriptorException, IOException {
                return GoldTemporalBaseline.run(reader, typeSystemDescription, trainingWorkingDir, baseDir);
            }
        }.run(taskConfig, reader, typeSystemDescription, sliceSuffix, goldBaselineRun, outputDir, gold);
    }

    private CollectionReaderDescription temporalBaseline(Configuration taskConfig,
                                                         CollectionReaderDescription reader,
                                                         String sliceSuffix, String outputDir,
                                                         File gold) throws InterruptedException, SAXException,
            UIMAException, CpeDescriptorException, IOException {
        logger.info("Running plain temporal baselin.");
        String plainBaselineRun = "plain_temporal_baseline";
        return new ModelTester(mainConfig, "plainTemporal") {
            @Override
            protected CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription
                    reader, String mainDir, String baseDir)
                    throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return TemporalBaseline.run(reader, typeSystemDescription, trainingWorkingDir, baseDir);
            }
        }.run(taskConfig, reader, typeSystemDescription, sliceSuffix, plainBaselineRun, outputDir, gold);
    }

    private CollectionReaderDescription selectedTemporalBaseline(Configuration taskConfig,
                                                                 CollectionReaderDescription reader,
                                                                 String sliceSuffix, String outputDir,
                                                                 File gold) throws InterruptedException, SAXException,
            UIMAException, CpeDescriptorException, IOException {
        logger.info("Running plain temporal baselin.");
        String selectedBaselineRun = "selected_temporal_baseline";
        return new ModelTester(mainConfig, "plainTemporal") {
            @Override
            protected CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription
                    reader, String mainDir, String baseDir)
                    throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return SelectedTemporalBaseline.run(reader, typeSystemDescription, trainingWorkingDir, baseDir);
            }
        }.run(taskConfig, reader, typeSystemDescription, sliceSuffix, selectedBaselineRun, outputDir, gold);
    }

    public CollectionReaderDescription afterLinking(Configuration taskConfig, CollectionReaderDescription reader,
                                                    String model, String mainDir, String baseOutput, boolean skipTest)
            throws UIMAException, SAXException, CpeDescriptorException, IOException {
        File outputFile = new File(mainDir, baseOutput);

        if (skipTest && outputFile.exists()) {
            logger.info("Skipping sent level tagging because output exists.");
            return CustomCollectionReaderFactory.createXmiReader(mainDir, baseOutput);
        } else {
            logger.info("Running after link.");
            return new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return reader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    AnalysisEngineDescription afterLinker = AnalysisEngineFactory.createEngineDescription(
                            LatentTreeAfterAnnotator.class, typeSystemDescription,
                            LatentTreeAfterAnnotator.PARAM_MODEL_DIRECTORY, model,
                            LatentTreeAfterAnnotator.PARAM_CONFIG, taskConfig.getConfigFile().getPath()
                    );

                    List<AnalysisEngineDescription> annotators = new ArrayList<>();
//                    RunnerUtils.addMentionPostprocessors(annotators, language);
                    annotators.add(afterLinker);
                    return annotators.toArray(new AnalysisEngineDescription[annotators.size()]);
                }
            }, mainDir, baseOutput).runWithOutput();
        }
    }

}
