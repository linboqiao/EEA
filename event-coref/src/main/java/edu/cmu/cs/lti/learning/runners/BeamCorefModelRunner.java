package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.emd.annotators.postprocessors.MentionTypeSplitter;
import edu.cmu.cs.lti.emd.pipeline.TrainingLooper;
import edu.cmu.cs.lti.event_coref.annotators.BeamEventCorefAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.train.BeamBasedCorefTrainer;
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
 * Time: 2:51 PM
 *
 * @author Zhengzhong Liu
 */
public class BeamCorefModelRunner extends AbstractMentionModelRunner {

    public BeamCorefModelRunner(Configuration mainConfig, TypeSystemDescription typeSystemDescription) {
        super(mainConfig, typeSystemDescription);
    }

    public String trainBeamBasedCoref(Configuration config, CollectionReaderDescription trainingReader,
                                      CollectionReaderDescription testReader, String suffix, boolean useLaSO,
                                      boolean delayedLaso, int beamSize, String outputDir, String subEvalDir,
                                      File gold, int initialSeed, boolean skipTrain, boolean skipTest) throws
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
                    RunnerUtils.addCorefPreprocessors(annotators, language);
//                    annotators.add(mentionSplitter);
                    annotators.add(corefAnnotator);
                    return annotators.toArray(new AnalysisEngineDescription[annotators.size()]);
                }
            }, mainDir, outputBase).runWithOutput();
        }
    }


    private CollectionReaderDescription testBeamCoref(Configuration config, CollectionReaderDescription reader,
                                                      String corefModel, String sliceSuffix, String runName,
                                                      String outputDir, String subEval, File gold, boolean skipTest)
            throws InterruptedException, SAXException, UIMAException, CpeDescriptorException, IOException {
        return new ModelTester(mainConfig, "beamCoref") {
            @Override
            CollectionReaderDescription runModel(Configuration config, CollectionReaderDescription reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return beamCorefResolution(config, reader, corefModel, trainingWorkingDir, baseDir, skipTest,
                        config.getInt("edu.cmu.cs.lti.coref.beam.size", 5)
                );
            }
        }.run(config, reader, sliceSuffix, runName, outputDir, subEval, gold);
    }
}
