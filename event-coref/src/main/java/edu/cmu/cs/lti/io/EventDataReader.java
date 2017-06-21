package edu.cmu.cs.lti.io;

import edu.cmu.cs.lti.collection_reader.*;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/3/16
 * Time: 9:18 AM
 *
 * @author Zhengzhong Liu
 */
public class EventDataReader {
    private String rawBase = "raw";

    private boolean skipRaw = false;

    private String workingDir;

    public EventDataReader(String workingDir, String baseDir, boolean skipReading) {
        this.workingDir = workingDir;
        this.skipRaw = skipReading;
        this.rawBase = baseDir;
    }

    public CollectionReaderDescription getReader() throws ResourceInitializationException {
        return CustomCollectionReaderFactory.createXmiReader(workingDir, rawBase);
    }

    public void readData(Configuration datasetConfig, TypeSystemDescription typeSystemDescription)
            throws UIMAException, SAXException, CpeDescriptorException, IOException {
        readData(datasetConfig, typeSystemDescription, rawBase);
    }

    private void readData(Configuration datasetConfig, TypeSystemDescription typeSystemDescription, String rawBase)
            throws UIMAException, SAXException, CpeDescriptorException, IOException {
        String datasetFormat = datasetConfig.get("edu.cmu.cs.lti.data.format").toLowerCase();
        switch (datasetFormat) {
            case "ere":
                readEre(datasetConfig, typeSystemDescription, rawBase);
                break;
            case "tbf":
                readTbf(datasetConfig, typeSystemDescription, rawBase);
                break;
            case "ace":
                readAce(datasetConfig, typeSystemDescription, rawBase);
                break;
            case "bratafter":
                readBratAfter(datasetConfig, typeSystemDescription, rawBase);
                break;
            case "brat":
                readBrat(datasetConfig, typeSystemDescription, rawBase);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown dataset format %s.", datasetFormat));
        }
    }

    private void readAce(Configuration datasetConfig, TypeSystemDescription typeSystemDescription, String rawBase)
            throws UIMAException, SAXException, CpeDescriptorException, IOException {

        if (!skipRaw || !new File(workingDir, rawBase).exists()) {
            CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                    AceDataCollectionReader.class, typeSystemDescription,
                    AceDataCollectionReader.PARAM_ACE_DATA_PATH, datasetConfig.get("edu.cmu.cs.lti.data.path"),
                    AceDataCollectionReader.PARAM_ACE_DATA_STATUS, datasetConfig.get("edu.cmu.cs.lti.data.ace.status"),
                    AceDataCollectionReader.PARAM_LANGUAGE, datasetConfig.get("edu.cmu.cs.lti.data.language")
            );
            writeAsXmi(reader, workingDir, rawBase, skipRaw);
        }
    }

    private void readEre(Configuration datasetConfig, TypeSystemDescription typeSystemDescription, String base)
            throws UIMAException, SAXException, CpeDescriptorException, IOException {
        if (!skipRaw || !new File(workingDir, rawBase).exists()) {
            CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(EreCorpusReader.class,
                    typeSystemDescription,
                    EreCorpusReader.PARAM_ERE_ANNOTATION_DIR, datasetConfig.get("edu.cmu.cs.lti.data.annotation.path"),
                    EreCorpusReader.PARAM_SOURCE_TEXT_DIR, datasetConfig.get("edu.cmu.cs.lti.data.source.path"),
                    EreCorpusReader.PARAM_ERE_ANNOTATION_EXT, datasetConfig.get("edu.cmu.cs.lti.data.annotation" +
                            ".extension"),
                    EreCorpusReader.PARAM_SOURCE_EXT, datasetConfig.get("edu.cmu.cs.lti.data.source.extension"),
                    EreCorpusReader.PARAM_ERE_EVENT_SPLIT_DOC,
                    datasetConfig.getBoolean("edu.cmu.cs.lti.data.event.split_doc", false),
                    EreCorpusReader.PARAM_LANGUAGE, datasetConfig.get("edu.cmu.cs.lti.data.language"),
                    EreCorpusReader.PARAM_REMOVE_QUOTES,
                    datasetConfig.getBoolean("edu.cmu.cs.lti.data.quotes.remove", false),
                    EreCorpusReader.PARAM_QUOTED_AREA_FILE,
                    datasetConfig.get("edu.cmu.cs.lti.data.quotes.file")
            );
            writeAsXmi(reader, workingDir, base, skipRaw);
        }
    }


    private void readTbf(Configuration datasetConfig, TypeSystemDescription typeSystemDescription, String rawBase)
            throws UIMAException, SAXException, CpeDescriptorException, IOException {
        if (!skipRaw || !new File(workingDir, rawBase).exists()) {
            CollectionReaderDescription reader;
            if (datasetConfig.get("edu.cmu.cs.lti.data.span").equals("token")) {
                reader = CollectionReaderFactory.createReaderDescription(
                        TbfEventDataReader.class, typeSystemDescription,
                        TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, datasetConfig.get("edu.cmu.cs.lti.data.gold.tbf"),
                        TbfEventDataReader.PARAM_SOURCE_EXT, datasetConfig.get("edu.cmu.cs.lti.data.source.extension"),
                        TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY,
                        datasetConfig.get("edu.cmu.cs.lti.data.source.path"),
                        TbfEventDataReader.PARAM_TOKEN_DIRECTORY, datasetConfig.get("edu.cmu.cs.lti.token_map.dir"),
                        TbfEventDataReader.PARAM_TOKEN_EXT, datasetConfig.get("edu.cmu.cs.lti.data.token.extension"),
                        TbfEventDataReader.PARAM_LANGUAGE, datasetConfig.get("edu.cmu.cs.lti.data.language"),
                        TbfEventDataReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
                );
            } else {
                reader = CollectionReaderFactory.createReaderDescription(
                        TbfEventDataReader.class, typeSystemDescription,
                        TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, datasetConfig.get("edu.cmu.cs.lti.data.gold.tbf"),
                        TbfEventDataReader.PARAM_SOURCE_EXT, datasetConfig.get("edu.cmu.cs.lti.data.source.extension"),
                        TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY,
                        datasetConfig.get("edu.cmu.cs.lti.data.source.path"),
                        TbfEventDataReader.PARAM_LANGUAGE, datasetConfig.get("edu.cmu.cs.lti.data.language"),
                        TbfEventDataReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
                );
            }
            writeAsXmi(reader, workingDir, rawBase, skipRaw);
        }
    }

    private static void writeAsXmi(CollectionReaderDescription reader, String parentDir,
                                   String rawBase, boolean skipReading)
            throws SAXException, UIMAException, CpeDescriptorException, IOException {
        if (!skipReading || !new File(parentDir, rawBase).exists()) {
            // We insert EventMentionSpan annotation here.
            AnalysisEngineDescription emsAnnotator = AnalysisEngineFactory.createEngineDescription(
                    EventSpanProcessor.class
            );
            AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(parentDir, rawBase);
            SimplePipeline.runPipeline(reader, emsAnnotator, writer);
        }
    }

    private void readBrat(Configuration datasetConfig, TypeSystemDescription typeSystemDescription, String rawBase)
            throws UIMAException, IOException {
        if (!skipRaw || !new File(workingDir, rawBase).exists()) {
            CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                    PlainTextCollectionReader.class, typeSystemDescription,
                    PlainTextCollectionReader.PARAM_INPUTDIR, datasetConfig.get("edu.cmu.cs.lti.data.source.path"),
                    PlainTextCollectionReader.PARAM_TEXT_SUFFIX, datasetConfig.get("edu.cmu.cs.lti.data.source" +
                            ".extension"),
                    PlainTextCollectionReader.PARAM_REMOVE_QUOTES,
                    datasetConfig.getBoolean("edu.cmu.cs.lti.data.quotes.remove", true)
            );


            AnalysisEngineDescription goldAnnotator = AnalysisEngineFactory.createEngineDescription(
                    BratEventGoldStandardAnnotator.class, typeSystemDescription,
                    BratEventGoldStandardAnnotator.PARAM_ANNOTATION_DIR,
                    datasetConfig.get("edu.cmu.cs.lti.data.annotation.path"),
                    BratEventGoldStandardAnnotator.PARAM_TEXT_FILE_SUFFIX, ".txt",
                    BratEventGoldStandardAnnotator.PARAM_ANNOTATION_FILE_NAME_SUFFIX, ".ann",
                    BratEventGoldStandardAnnotator.PARAM_PREFER_COREF_LINK, true
            );

            AnalysisEngineDescription emsAnnotator = AnalysisEngineFactory.createEngineDescription(
                    EventSpanProcessor.class, typeSystemDescription
            );

//            CustomAnalysisEngineFactory.setTypeSystem(typeSystemDescription);

            AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(workingDir, rawBase);

            SimplePipeline.runPipeline(reader, goldAnnotator, emsAnnotator, writer);
        }
    }

    private void readBratAfter(Configuration datasetConfig, TypeSystemDescription typeSystemDescription, String rawBase)
            throws SAXException, UIMAException, CpeDescriptorException, IOException {
        if (!skipRaw || !new File(workingDir, rawBase).exists()) {
            CollectionReaderDescription baseReader = getBaseReader(datasetConfig, typeSystemDescription,
                    "before_after");
            AnalysisEngineDescription goldAfterLinker = AnalysisEngineFactory.createEngineDescription(
                    AfterLinkGoldStandardAnnotator.class, typeSystemDescription,
                    AfterLinkGoldStandardAnnotator.PARAM_ANNOTATION_DIR,
                    datasetConfig.get("edu.cmu.cs.lti.data.annotation.path")
            );
            AnalysisEngineDescription emsAnnotator = AnalysisEngineFactory.createEngineDescription(
                    EventSpanProcessor.class, typeSystemDescription
            );
            AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(workingDir, rawBase);
            SimplePipeline.runPipeline(baseReader, goldAfterLinker, emsAnnotator, writer);
        }
    }

    private CollectionReaderDescription getBaseReader(Configuration datasetConfig,
                                                      TypeSystemDescription typeSystemDescription, String base)
            throws IOException, UIMAException, SAXException, CpeDescriptorException {
        File configDir = datasetConfig.getConfigFile().getParentFile();

        Configuration baseDatasetConfig = new Configuration(new File(configDir,
                datasetConfig.get("edu.cmu.cs.lti.data.base.corpus") + ".properties"));
        readData(baseDatasetConfig, typeSystemDescription, base);

        return CustomCollectionReaderFactory.createXmiReader(workingDir, base);
    }
}
