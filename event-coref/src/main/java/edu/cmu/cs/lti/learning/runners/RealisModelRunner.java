package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.emd.annotators.classification.AllActualRealisAnnotator;
import edu.cmu.cs.lti.emd.annotators.classification.RealisTypeAnnotator;
import edu.cmu.cs.lti.learning.train.RealisClassifierTrainer;
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

/**
 * Created with IntelliJ IDEA.
 * Date: 12/12/16
 * Time: 3:11 PM
 *
 * @author Zhengzhong Liu
 */
public class RealisModelRunner extends AbstractMentionModelRunner {
    public RealisModelRunner(Configuration mainConfig, TypeSystemDescription typeSystemDescription) {
        super(mainConfig, typeSystemDescription);
    }

    public String trainRealis(Configuration config, CollectionReaderDescription trainingReader, String suffix,
                              boolean skipTrain) throws Exception {
        String realisCvModelDir = ModelUtils.getTrainModelPath(eventModelDir, config, suffix);

        if (skipTrain && new File(realisCvModelDir).exists()) {
            logger.info("Skipping realis training, taking existing models: " + realisCvModelDir);
        } else {
            RealisClassifierTrainer trainer = new RealisClassifierTrainer(typeSystemDescription, trainingReader,
                    config);
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
                    AnalysisEngineDescription realisAnnotator;
                    if (modelDir == null) {
                        realisAnnotator = AnalysisEngineFactory.createEngineDescription(
                                AllActualRealisAnnotator.class, typeSystemDescription
                        );
                    } else {
                        realisAnnotator = AnalysisEngineFactory.createEngineDescription(
                                RealisTypeAnnotator.class, typeSystemDescription,
                                RealisTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                                RealisTypeAnnotator.PARAM_CONFIG_PATH, taskConfig.getConfigFile()
                        );
                    }
                    return new AnalysisEngineDescription[]{realisAnnotator};
                }
            }, mainDir, realisOutputBase).runWithOutput();
        }
    }

    public CollectionReaderDescription testRealis(Configuration taskConfig, CollectionReaderDescription reader,
                                                  String realisModel, String sliceSuffix, String runName,
                                                  String outputDir, String subEval, File gold, boolean skipTest)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {

        return new ModelTester(mainConfig) {
            @Override
            protected CollectionReaderDescription runModel(Configuration taskConfig, CollectionReaderDescription
                    reader, String
                    mainDir, String baseDir) throws SAXException, UIMAException, CpeDescriptorException, IOException {
                return realisAnnotation(taskConfig, reader, realisModel, trainingWorkingDir, baseDir, skipTest);
            }
        }.run(taskConfig, reader, typeSystemDescription, sliceSuffix, runName, outputDir, gold);
    }
}
