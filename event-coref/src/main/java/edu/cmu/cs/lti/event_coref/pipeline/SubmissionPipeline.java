package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.MultiModelMentionTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.emd.annotators.classification.AllActualRealisAnnotator;
import edu.cmu.cs.lti.emd.annotators.classification.RealisTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.misc.TypeBasedMentionSelector;
import edu.cmu.cs.lti.emd.annotators.postprocessors.MentionTypeSplitter;
import edu.cmu.cs.lti.event_coref.annotators.EventCorefAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.prepare.EnglishSrlArgumentExtractor;
import edu.cmu.cs.lti.event_coref.annotators.prepare.EventHeadWordAnnotator;
import edu.cmu.cs.lti.learning.utils.ModelUtils;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.utils.Configuration;
import edu.stanford.nlp.util.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/3/16
 * Time: 5:56 PM
 *
 * @author Zhengzhong Liu
 */
public class SubmissionPipeline {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String language;
    private String mainModelDir;
    private TypeSystemDescription typeSystemDescription;


    public SubmissionPipeline(String language, String mainModelDir, String typeSystemName) {
        this.language = language;
        this.mainModelDir = mainModelDir;
        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);
    }

    private CollectionReaderDescription runTypeDetectors(String mainOutputDir,
                                                         String[] configurations, String strategy,
                                                         CollectionReaderDescription... readers) throws
            UIMAException, SAXException, CpeDescriptorException, IOException {
        if (new File(mainOutputDir, "type").exists()) {
            logger.info("Using existing type output.");
            return CustomCollectionReaderFactory.createXmiReader(mainOutputDir, "type");
        } else {
            AnalysisEngineDescription engine = AnalysisEngineFactory.createEngineDescription(
                    MultiModelMentionTypeAnnotator.class, typeSystemDescription,
                    MultiModelMentionTypeAnnotator.PARAM_MAIN_MODEL_DIRECTORY, mainModelDir,
                    MultiModelMentionTypeAnnotator.PARAM_CONFIGS, configurations,
                    MultiModelMentionTypeAnnotator.PARAM_STRATEGY, strategy
            );

            AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                    MentionTypeSplitter.class, typeSystemDescription
            );

            CollectionReaderDescription output = null;
            for (CollectionReaderDescription reader : readers) {
                output = new BasicPipeline(reader, mainOutputDir, "type", engine, mentionSplitter).run().getOutput();
            }

            if (readers.length == 0) {
                logger.warn("No readers specified.");
            }
            return output;
        }
    }

    private CollectionReaderDescription filterEventTypes(CollectionReaderDescription reader, String mainOutputDir,
                                                         String selectedTypeFile)
            throws UIMAException, SAXException, CpeDescriptorException, IOException {
        AnalysisEngineDescription mentionFilter = AnalysisEngineFactory.createEngineDescription(
                TypeBasedMentionSelector.class, typeSystemDescription,
                TypeBasedMentionSelector.PARAM_SELECTED_MENTION_TYPE_FILE, selectedTypeFile
        );

        return new BasicPipeline(reader, mainOutputDir, "filtered", mentionFilter).run().getOutput();
    }

    // Run realis.
    private CollectionReaderDescription realisAnnotation(Configuration config, CollectionReaderDescription reader,
                                                         String mainOutputDir)
            throws IOException, UIMAException, CpeDescriptorException, SAXException {
        AnalysisEngineDescription realisAnnotator;
        if (config == null) {
            realisAnnotator = AnalysisEngineFactory.createEngineDescription(
                    AllActualRealisAnnotator.class
            );
        } else {
            String modelDir = ModelUtils.getTestModelFile(mainModelDir, config);

            realisAnnotator = AnalysisEngineFactory.createEngineDescription(
                    RealisTypeAnnotator.class, typeSystemDescription,
                    RealisTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                    RealisTypeAnnotator.PARAM_CONFIG_PATH, config.getConfigFile()
            );
        }
        return new BasicPipeline(reader, mainOutputDir, "realis", realisAnnotator).run().getOutput();
    }


    private CollectionReaderDescription corefResolution(Configuration modelConfig, CollectionReaderDescription reader,
                                                        String mainOutputDir)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        String modelDir = ModelUtils.getTestModelFile(mainModelDir, modelConfig);

        AnalysisEngineDescription corefAnnotator = AnalysisEngineFactory.createEngineDescription(
                EventCorefAnnotator.class, typeSystemDescription,
                EventCorefAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                EventCorefAnnotator.PARAM_CONFIG_PATH, modelConfig.getConfigFile()
        );

        List<AnalysisEngineDescription> annotators = new ArrayList<>();
        addCorefPreprocessors(annotators);
        annotators.add(corefAnnotator);

        return new BasicPipeline(reader, mainOutputDir, "coref", annotators.toArray(new
                AnalysisEngineDescription[annotators.size()])).getOutput();
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
                    EnglishSrlArgumentExtractor.class
            );
            preAnnotators.add(headWordExtractor);
            preAnnotators.add(argumentExtractor);
        }
    }

    private void writeResults(CollectionReaderDescription processedResultReader, String mainOutputDir, String systemId)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        File tbfOutput = new File(mainOutputDir, systemId);
        logger.info("Writing results to " + tbfOutput);

        AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                TbfStyleEventWriter.class, typeSystemDescription,
                TbfStyleEventWriter.PARAM_OUTPUT_PATH, tbfOutput,
                TbfStyleEventWriter.PARAM_SYSTEM_ID, systemId,
                TbfStyleEventWriter.PARAM_GOLD_TOKEN_COMPONENT_ID, TbfEventDataReader.COMPONENT_ID,
                TbfStyleEventWriter.PARAM_USE_CHARACTER_OFFSET, true
        );

        new BasicPipeline(processedResultReader, resultWriter).run();
    }

    private void runAll(String mainOutputDir, List<String> inputDirs, String[] typeConfigs, String selectedTypeFile,
                        Configuration realisModelConfig, Configuration corefModelConfig, String typeMergeStrategy,
                        String systemName)
            throws UIMAException, SAXException, CpeDescriptorException, IOException {
        CollectionReaderDescription[] inputReaders = new CollectionReaderDescription[inputDirs.size()];
        for (int i = 0; i < inputDirs.size(); i++) {
            String inputDir = inputDirs.get(i);
            inputReaders[i] = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, inputDir);
        }
        CollectionReaderDescription types = runTypeDetectors(mainOutputDir, typeConfigs,
                typeMergeStrategy, inputReaders);
        CollectionReaderDescription filtered = filterEventTypes(types, mainOutputDir, selectedTypeFile);
        CollectionReaderDescription realis = realisAnnotation(realisModelConfig, filtered, mainOutputDir);

        writeResults(realis, mainOutputDir, systemName + "_mention_only" + ".tbf");

        CollectionReaderDescription coref = corefResolution(corefModelConfig, realis, mainOutputDir);

        writeResults(coref, mainOutputDir, systemName + "_with_coref" + ".tbf");
    }

    private static Configuration loadModelConfig(String configDir, String configName) {
        File configFile = new File(configDir, configName + ".properties");
        try {
            return new Configuration(configFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Model configuration node found: " + configFile);
        }
    }

    public static void main(String[] argv) throws SAXException, UIMAException, CpeDescriptorException, IOException {
        String mainOutput = argv[0];
        String eventModelDir = argv[1];
        String name = argv[2];

        SubmissionPipeline pipeline = new SubmissionPipeline("zh", eventModelDir,
                "TaskEventMentionDetectionTypeSystem");

        List<String> inputs = StringUtils.split(argv[3], ",");

        String configDir = "settings/nugget/models";

//        List<String> typeConfigNames = StringUtils.split(argv[4], ",");
//        String[] typeConfigs = new String[typeConfigNames.size()];
//        for (String configName : typeConfigNames) {
//            typeCon
//        }

        String[] typeConfigs = StringUtils.split(argv[4], ",").stream()
                .map(p -> new File(configDir, p + ".properties").getAbsolutePath()).toArray(String[]::new);

        Configuration corefConfig = loadModelConfig(configDir, argv[5]);

        Configuration realisConfig = loadModelConfig(configDir, argv[6]);

        pipeline.runAll(mainOutput, inputs, typeConfigs,
                "../EvmEval/TAC_KBP_eval_type_2016.txt", realisConfig, corefConfig, "union", name);
    }
}
