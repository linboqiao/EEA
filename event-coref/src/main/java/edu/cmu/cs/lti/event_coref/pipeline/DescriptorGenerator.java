package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.annotators.*;
import edu.cmu.cs.lti.emd.annotators.CrfMentionTypeAnnotator;
import edu.cmu.cs.lti.emd.annotators.classification.RealisTypeAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.EventCorefAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.prepare.ArgumentMerger;
import edu.cmu.cs.lti.event_coref.annotators.prepare.EventHeadWordAnnotator;
import edu.cmu.cs.lti.exceptions.ConfigurationException;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static edu.cmu.cs.lti.event_coref.pipeline.EventMentionPipeline.fullRunSuffix;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/22/17
 * Time: 12:06 PM
 *
 * @author Zhengzhong Liu
 */
public class DescriptorGenerator {

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

    private final String despcriptorOutputDir;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * A constructor that only take the training directory.
     *
     * @param typeSystemName The type system to use.
     * @param config         Configuration file.
     */
    public DescriptorGenerator(String typeSystemName, Configuration config, String descriptorDir) {
        this(config, descriptorDir,
                config.get("edu.cmu.cs.lti.model.dir"),
                config.get("edu.cmu.cs.lti.model.event.dir"),
                config.getOrElse("edu.cmu.cs.lti.language", "en"),
                typeSystemName,
                true
        );
    }

    private DescriptorGenerator(Configuration config, String descriptorDir, String generalModelDir,
                                String eventModelDir, String language, String typeSystemName, boolean useCharOffset) {
        this.useCharOffset = useCharOffset;
        this.generalModelDir = generalModelDir;
        this.language = language;

        this.despcriptorOutputDir = descriptorDir;

        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);

        mentionDetectionModelDir = FileUtils.joinPaths(eventModelDir,
                config.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir"), fullRunSuffix);

        realisModelDir = FileUtils.joinPaths(eventModelDir,
                config.get("edu.cmu.cs.lti.model.realis.dir"), fullRunSuffix);

        corefModelDir = FileUtils.joinPaths(eventModelDir,
                config.get("edu.cmu.cs.lti.model.event.latent_tree"), fullRunSuffix);
    }

    private void generateReaderDescriptors() throws ResourceInitializationException, IOException, SAXException {
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class, typeSystemDescription
        );

        String desciptorname = "InputDescriptor" + ".xml";

        File outputFile = FileUtils.joinPathsAsFile(despcriptorOutputDir, "io", desciptorname);
        FileUtils.ensureDirectory(outputFile.getParent());

        reader.toXML(new BufferedOutputStream(new FileOutputStream(outputFile)));
    }

    private void generateWriterDescriptors() throws ResourceInitializationException,
            IOException, SAXException {
        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter("", "");

        String desciptorname = "WriterDescriptor" + ".xml";

        File outputFile = FileUtils.joinPathsAsFile(despcriptorOutputDir, "io", desciptorname);
        FileUtils.ensureDirectory(outputFile.getParent());

        writer.toXML(new BufferedOutputStream(new FileOutputStream(outputFile)));
    }

    private void generateProcessorDesciptors() throws IOException, ResourceInitializationException, SAXException {
        String outputDir = FileUtils.joinPaths(despcriptorOutputDir, "processor");
        FileUtils.ensureDirectory(outputDir);


        AnalysisEngineDescription typeEngine = AnalysisEngineFactory.createEngineDescription(
                CrfMentionTypeAnnotator.class, typeSystemDescription
        );
        writeDescriptor(typeEngine, outputDir, "type");

        AnalysisEngineDescription headwordEngine = AnalysisEngineFactory.createEngineDescription(
                EventHeadWordAnnotator.class, typeSystemDescription
        );

        writeDescriptor(headwordEngine, outputDir, "headword");

        AnalysisEngineDescription realisEngine = AnalysisEngineFactory.createEngineDescription(
                RealisTypeAnnotator.class, typeSystemDescription
        );
        writeDescriptor(realisEngine, outputDir, "realis");

        AnalysisEngineDescription corefEngine = AnalysisEngineFactory.createEngineDescription(
                EventCorefAnnotator.class, typeSystemDescription
        );
        writeDescriptor(corefEngine, outputDir, "coref");

        //        List<AnalysisEngineDescription> engines = new ArrayList<>();
//        List<String> names = new ArrayList<>();
        //
//        engines.add(typeEngine);
//        engines.add(headwordEngine);
//        engines.add(realisEngine);
//        engines.add(corefEngine);
//
//        names.add("type");
//        names.add("headword");
//        names.add("realis");
//        names.add("coref");
//
//        AnalysisEngineFactory.createEngineDescription(engines, names);

    }

    private void writeDescriptor(AnalysisEngineDescription descriptor, String outputDir, String name) throws
            IOException, SAXException {
        descriptor.toXML(new BufferedOutputStream(new FileOutputStream(new File(outputDir, name + "Descriptor.xml"))));
    }

    private void generatePreprocessDescriptors(Configuration taskConfig) throws ResourceInitializationException,
            IOException, SAXException {
        String[] preprocessorNames = taskConfig.getList("edu.cmu.cs.lti.preprocessors");
        final String semaforModelDirectory = generalModelDir + "/semafor_malt_model_20121129";
        final String fanseModelDirectory = generalModelDir + "/fanse_models";
        final String opennlpDirectory = generalModelDir + "/opennlp/en-chunker.bin";

        AnalysisEngineDescription[] preprocessors = new AnalysisEngineDescription[preprocessorNames.length];

        for (int i = 0; i < preprocessorNames.length; i++) {
            String name = preprocessorNames[i];
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

        for (int i = 0; i < preprocessors.length; i++) {
            String desciptorname = preprocessorNames[i] + "EngineDescriptor" + ".xml";

            File outputFile = FileUtils.joinPathsAsFile(despcriptorOutputDir, "preprocess", desciptorname);
            FileUtils.ensureDirectory(outputFile.getParent());

            preprocessors[i].toXML(new BufferedOutputStream(new FileOutputStream(outputFile)));
        }

    }

    public static void main(String[] argv) throws IOException, SAXException, ResourceInitializationException {
        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        Configuration taskConfig = new Configuration(argv[0]);

        String outputDir = argv[1];

        DescriptorGenerator generator = new DescriptorGenerator(typeSystemName, taskConfig,
                outputDir);
        generator.generateReaderDescriptors();
        generator.generatePreprocessDescriptors(taskConfig);
        generator.generateWriterDescriptors();
        generator.generateProcessorDesciptors();
    }
}
