package edu.cmu.cs.lti.script.runners; /**
 *
 */

import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.collection_reader.AgigaCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

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
    public static void main(String[] args) throws IOException, ResourceInitializationException {
        System.out.println(className + " started...");

        String paramInputDir = args[0];// "/Users/zhengzhongliu/Downloads/agiga_sample"
        String paramModelBaseDirectory = args[1];// "../models"

        // Parameters for the writer
        String paramParentOutputDir = "data";
        String paramBaseOutputDirName = "fanse_parsed";
        String paramOutputFileSuffix = null;

        String paramTypeSystemDescriptor = "TypeSystem";

        System.out.println("Reading from " + paramInputDir + " , writing to base dir " + paramBaseOutputDirName);

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                AgigaCollectionReader.class, typeSystemDescription,
                AgigaCollectionReader.PARAM_INPUTDIR, paramInputDir);

        AnalysisEngineDescription fanseParser = AnalysisEngineFactory.createEngineDescription(
                FanseAnnotator.class, typeSystemDescription, FanseAnnotator.PARAM_MODEL_BASE_DIR,
                paramModelBaseDirectory);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzippedXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, 0, paramOutputFileSuffix);

        // Run the pipeline.

        try {
            SimplePipeline.runPipeline(reader, fanseParser, writer);
        } catch (UIMAException e) {
            e.printStackTrace();
        }

        System.out.println(className + " successfully completed.");
    }
}