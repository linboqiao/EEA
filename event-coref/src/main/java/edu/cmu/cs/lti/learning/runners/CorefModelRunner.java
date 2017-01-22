package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.emd.annotators.postprocessors.MentionTypeSplitter;
import edu.cmu.cs.lti.emd.pipeline.TrainingLooper;
import edu.cmu.cs.lti.event_coref.annotators.EventCorefAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.train.PaLatentTreeTrainer;
import edu.cmu.cs.lti.learning.utils.ModelUtils;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.pipeline.ProcessorWrapper;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
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
 * Time: 3:02 PM
 *
 * @author Zhengzhong Liu
 */
public class CorefModelRunner extends AbstractMentionModelRunner {

    public CorefModelRunner(Configuration mainConfig, TypeSystemDescription typeSystemDescription) {
        super(mainConfig, typeSystemDescription);
    }

    public String getModelPath(Configuration config, String suffix) {
        return ModelUtils.getTrainModelPath(eventModelDir, config, suffix);
    }

    /**
     * Train the latent tree model coreference resolver.
     *
     * @param config         The configuration file.
     * @param trainingReader Reader for the training data.
     * @param testReader     Reader for the test data.
     * @param suffix         The suffix for the model.
     * @param skipTrain      Whether to skip the training if model file exists.
     * @return The trained model directory.
     */
    public String trainLatentTreeCoref(Configuration config, CollectionReaderDescription trainingReader,
                                       CollectionReaderDescription testReader, String suffix, String outputDir,
                                       String subEvalDir, File gold, boolean skipTrain,
                                       boolean skipTest)
            throws UIMAException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        logger.info("Start coreference training.");

        String modelPath = getModelPath(config, suffix);

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

    public CollectionReaderDescription corefResolution(Configuration config, CollectionReaderDescription reader,
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
                    RunnerUtils.addCorefPreprocessors(annotators, language);

                    annotators.add(mentionSplitter);
                    annotators.add(corefAnnotator);
                    return annotators.toArray(new AnalysisEngineDescription[annotators.size()]);
                }
            }, mainDir, outputBase).runWithOutput();
        }
    }


    public CollectionReaderDescription testCoref(Configuration taskConfig,
                                                 CollectionReaderDescription reader, String corefModel,
                                                 String sliceSuffix, String runName, String outputDir,
                                                 String subEval, File gold, boolean skipTest)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        return new ModelTester(mainConfig, "treeCoref") {
            @Override
            CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return corefResolution(taskConfig, reader, corefModel, trainingWorkingDir, baseDir, skipTest);
            }
        }.run(taskConfig, reader, sliceSuffix, runName, outputDir, subEval, gold);
    }


}
