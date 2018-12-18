package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.annotators.*;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.CrfMentionTypeAnnotator;
import edu.cmu.cs.lti.script.annotators.writers.TbfStyleEventWriter;
import edu.cmu.cs.lti.emd.annotators.classification.RealisTypeAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.EventCorefAnnotator;
import edu.cmu.cs.lti.script.annotators.ArgumentMerger;
import edu.cmu.cs.lti.script.annotators.EventHeadWordAnnotator;
import edu.cmu.cs.lti.exceptions.ConfigurationException;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static edu.cmu.cs.lti.event_coref.pipeline.EventMentionPipeline.fullRunSuffix;

/**
 * A processor that starts with some plain text, process it, and then produce event mention
 *
 * @author Zhengzhong Liu
 */
public class PlainTextProcessor {
    private final TypeSystemDescription typeSystemDescription;

    // The directory that stores all the awesome and not-awesome models.
    private final String generalModelDir;

    private final String language;

    private final String mentionDetectionModelDir;

    private final String realisModelDir;

    private final String corefModelDir;

    private final boolean useCharOffset;

    private final String middleResults = "intermediate";
    private final String preprocessBase = "preprocessed";
    private final String evalBase = "eval";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * A constructor that only take the training directory.
     *
     * @param typeSystemName The type system to use.
     * @param config         Configuration file.
     */
    public PlainTextProcessor(String typeSystemName, Configuration config) {
        this(config,
                config.get("edu.cmu.cs.lti.model.dir"),
                config.get("edu.cmu.cs.lti.model.event.dir"),
                config.getOrElse("edu.cmu.cs.lti.language", "en"),
                typeSystemName,
                true
        );
    }

    private PlainTextProcessor(Configuration config, String generalModelDir, String eventModelDir, String language,
                               String typeSystemName, boolean useCharOffset) {
        this.useCharOffset = useCharOffset;
        this.generalModelDir = generalModelDir;
        this.language = language;

//        this.mainWorkingDir = mainWorkingDir;

        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);

        mentionDetectionModelDir = FileUtils.joinPaths(eventModelDir,
                config.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir"), fullRunSuffix);

        realisModelDir = FileUtils.joinPaths(eventModelDir,
                config.get("edu.cmu.cs.lti.model.realis.dir"), fullRunSuffix);

        corefModelDir = FileUtils.joinPaths(eventModelDir,
                config.get("edu.cmu.cs.lti.model.event.latent_tree"), fullRunSuffix);
    }

    /**
     * Run a test, with all the intermediate results retained.
     *
     * @param taskConfig The test configuration file.
     * @throws UIMAException
     * @throws IOException
     */
    public void run(Configuration taskConfig, String outputDir, String... inputDir) throws UIMAException, IOException,
            CpeDescriptorException, SAXException {

        prepare(taskConfig, true, outputDir, inputDir);

        boolean skipTypeTest = taskConfig.getBoolean("edu.cmu.cs.lti.mention_type.skiptest", false);
        boolean skipRealisTest = taskConfig.getBoolean("edu.cmu.cs.lti.mention_realis.skiptest", false);
        boolean skipCorefTest = taskConfig.getBoolean("edu.cmu.cs.lti.coref.skiptest", false);

        CollectionReaderDescription preprocessedData = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, outputDir, preprocessBase);

        String lv1OutputBase = FileUtils.joinPaths(middleResults, "mention_lv1");

        // Run mention level crf for mention detection.
        CollectionReaderDescription sentMentions = sentenceLevelMentionTagging(taskConfig, preprocessedData,
                mentionDetectionModelDir, outputDir, lv1OutputBase, skipTypeTest);

        String lv1RealisOutput = FileUtils.joinPaths(middleResults, "lv1_realis");

        logger.info("Going to run realis classifier on " + lv1OutputBase + " output will be at " + lv1RealisOutput);
        CollectionReaderDescription lv1MentionRealisResults = realisAnnotation(taskConfig, sentMentions,
                realisModelDir, outputDir, lv1RealisOutput, skipRealisTest);

        // Run coreference.
        CollectionReaderDescription corefSentMentions = corefResolution(taskConfig,
                lv1MentionRealisResults, corefModelDir, outputDir,
                FileUtils.joinPaths(middleResults, "coref_lv1_mention"), skipCorefTest);

        // Output final result.
        String evalDir = FileUtils.joinPaths(outputDir, evalBase, "full_run");

        writeResults(corefSentMentions, FileUtils.joinPaths(evalDir, "lv1_coref" + ".tbf"), "lv1_coref");
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
            AnalysisEngineDescription sentenceLevelTagger = AnalysisEngineFactory.createEngineDescription(
                    CrfMentionTypeAnnotator.class, typeSystemDescription,
                    CrfMentionTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir
            );

            CrfMentionTypeAnnotator.setConfig(taskConfig);

            return new BasicPipeline(reader, mainDir, baseOutput, sentenceLevelTagger).run().getOutput();
        }
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
            AnalysisEngineDescription corefAnnotator = AnalysisEngineFactory.createEngineDescription(
                    EventCorefAnnotator.class, typeSystemDescription,
                    EventCorefAnnotator.PARAM_MODEL_DIRECTORY, modelDir
            );

            EventCorefAnnotator.setConfig(config);

            AnalysisEngineDescription headWordExtractor = AnalysisEngineFactory.createEngineDescription(
                    EventHeadWordAnnotator.class, typeSystemDescription
            );
            return new BasicPipeline(reader, mainDir, outputBase, headWordExtractor, corefAnnotator).run().getOutput();
        }
    }

    public CollectionReaderDescription realisAnnotation(Configuration realisConfig, CollectionReaderDescription reader,
                                                        String modelDir, String mainDir, String realisOutputBase,
                                                        boolean skipTest)
            throws IOException, UIMAException, CpeDescriptorException, SAXException {

        if (skipTest && new File(mainDir, realisOutputBase).exists()) {
            logger.info("Skipping realis detection because output exists.");
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, realisOutputBase);
        } else {
            AnalysisEngineDescription realisAnnotator = AnalysisEngineFactory.createEngineDescription(
                    RealisTypeAnnotator.class, typeSystemDescription,
                    RealisTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir,
                    RealisTypeAnnotator.PARAM_CONFIG, realisConfig
            );
            return new BasicPipeline(reader, mainDir, realisOutputBase, realisAnnotator).run().getOutput();
        }
    }


    public void writeResults(CollectionReaderDescription processedResultReader, String tbfOutput, String systemId)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        logger.info("Writing results to " + tbfOutput);

        AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                TbfStyleEventWriter.class, typeSystemDescription,
                TbfStyleEventWriter.PARAM_OUTPUT_PATH, tbfOutput,
                TbfStyleEventWriter.PARAM_SYSTEM_ID, systemId,
                TbfStyleEventWriter.PARAM_GOLD_TOKEN_COMPONENT_ID, TbfEventDataReader.COMPONENT_ID,
                TbfStyleEventWriter.PARAM_USE_CHARACTER_OFFSET, useCharOffset
        );
        new BasicPipeline(processedResultReader, resultWriter).run();
    }

    /**
     * Run major preprocessing steps for all the downstream tasks.
     *
     * @param taskConfig   The main configuration file.
     * @param skipIfExists Skip preprocessing if data exists.
     * @throws UIMAException
     * @throws IOException
     */
    public void prepare(Configuration taskConfig, boolean skipIfExists, String outputDir, String... inputDirs) throws
            UIMAException, IOException, CpeDescriptorException, SAXException {
        if (outputDir == null) {
            logger.info("Output directory not provided, not running");
            return;
        }

        CollectionReaderDescription[] inputReaders;
        inputReaders = new CollectionReaderDescription[inputDirs.length];
        for (int i = 0; i < inputDirs.length; i++) {
            CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                    PlainTextCollectionReader.class, typeSystemDescription,
                    PlainTextCollectionReader.PARAM_INPUTDIR, inputDirs[i],
                    PlainTextCollectionReader.PARAM_LANGUAGE, language,
                    PlainTextCollectionReader.PARAM_DO_NOISE_FILTER, true
            );
            inputReaders[i] = reader;
        }

        File workingDir = new File(outputDir);
        if (!workingDir.exists()) {
            logger.info("Created directory for preprocessing : " + outputDir);
            workingDir.mkdirs();
        }

        File preprocessDir = new File(outputDir, preprocessBase);

        if (skipIfExists && preprocessDir.exists()) {
            logger.info("Preprocessed data exists, not running.");
            return;
        } else {
            logger.info(String.format("Starting pre-processing at %s.", outputDir));
        }

        List<String> preprocessorNames = validatePreprocessors(taskConfig.getList("edu.cmu.cs.lti.preprocessors"));

        final String semaforModelDirectory = generalModelDir + "/semafor_malt_model_20121129";
        final String fanseModelDirectory = generalModelDir + "/fanse_models";
        final String opennlpDirectory = generalModelDir + "/opennlp/en-chunker.bin";

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

        for (CollectionReaderDescription reader : inputReaders) {
            new BasicPipeline(reader, outputDir, preprocessBase, preprocessors).run();
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


    public static void main(String[] argv) throws IOException, UIMAException, SAXException, CpeDescriptorException {
        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        Configuration taskConfig = new Configuration(argv[0]);

        String inputDir = argv[1];
        String outputDir = argv[2];

        PlainTextProcessor processor = new PlainTextProcessor(typeSystemName, taskConfig);
        processor.run(taskConfig, outputDir, inputDir);
    }
}
