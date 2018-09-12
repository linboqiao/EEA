/**
 *
 */
package edu.cmu.cs.lti.script.runners.writers;

import edu.cmu.cs.lti.script.annotators.writers.DocumentLevelEventWriter;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.IOException;

/**
 * @author zhengzhongliu
 */
public class EventSurfacePrinter {
    private static String className = EventSurfacePrinter.class.getSimpleName();

    /**
     * @param args
     * @throws IOException
     * @throws UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        System.out.println(className + " started...");

        // ///////////////////////// Parameter Setting ////////////////////////////
        // Note that you should change the parameters below for your configuration.
        // //////////////////////////////////////////////////////////////////////////
        // Parameters for the reader
        String parentInput = "data";
        String baseInput = "01_event_tuples";

        // Parameters for the writer
        String paramParentOutputDir = "data";
        String paramBaseOutputDirName = "event_surfaces";
        String paramOutputFileSuffix = "tsv";
        int stepNum = 2;
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, parentInput, baseInput);


        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                DocumentLevelEventWriter.class, typeSystemDescription,
                DocumentLevelEventWriter.PARAM_BASE_OUTPUT_DIR_NAME, paramBaseOutputDirName,
                DocumentLevelEventWriter.PARAM_OUTPUT_FILE_SUFFIX, paramOutputFileSuffix,
                DocumentLevelEventWriter.PARAM_PARENT_OUTPUT_DIR_PATH, paramParentOutputDir,
                DocumentLevelEventWriter.PARAM_OUTPUT_STEP_NUMBER, stepNum);

        SimplePipeline.runPipeline(reader, writer);

        System.out.println(className + " completed.");
    }
}
