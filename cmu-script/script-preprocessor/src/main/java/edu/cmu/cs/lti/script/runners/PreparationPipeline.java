package edu.cmu.cs.lti.script.runners;

import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.collection_reader.AgigaCollectionReader;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * @author zhengzhongliu
 */
public class PreparationPipeline {

    private static String className = PreparationPipeline.class.getSimpleName();

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.resource.ResourceInitializationException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws IOException, UIMAException, SAXException, CpeDescriptorException {
        System.out.println(className + " started...");

        String inputDir = args[0];// "/Users/zhengzhongliu/Downloads/agiga_sample"
        String modelBaseDir = args[1];// "../models"
        String parentOutputDir = args[2];
        String ignores = args[3];

        // Parameters for the writer
        String baseOutputDirName = "fanse_parsed";

        String paramTypeSystemDescriptor = "TypeSystem";

        System.out.println("Reading from " + inputDir + " , writing to base dir " + baseOutputDirName);

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                AgigaCollectionReader.class, typeSystemDescription,
                AgigaCollectionReader.PARAM_DATA_PATH, inputDir,
                AgigaCollectionReader.PARAM_EXTENSION, ".xml.gz",
                AgigaCollectionReader.PARAM_BASE_NAME_IGNORES, ignores
        );

        AnalysisEngineDescription fanseParser = AnalysisEngineFactory.createEngineDescription(
                FanseAnnotator.class, typeSystemDescription, FanseAnnotator.PARAM_MODEL_BASE_DIR,
                modelBaseDir);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzippedXmiWriter(
                parentOutputDir, baseOutputDirName, 0, null);

        // Run the pipeline.
        new BasicPipeline(reader, 5, fanseParser, writer).run();

        System.out.println(className + " successfully completed.");
    }
}