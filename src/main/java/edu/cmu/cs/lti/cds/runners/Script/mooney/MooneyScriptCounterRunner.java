/**
 *
 */
package edu.cmu.cs.lti.cds.runners.script.mooney;

import edu.cmu.cs.lti.cds.annotators.script.karlmooney.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;

/**
 * @author zhengzhongliu
 */
public class MooneyScriptCounterRunner {
    private static String className = MooneyScriptCounterRunner.class.getSimpleName();

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        System.out.println(className + " started...");

        // ///////////////////////// Parameter Setting ////////////////////////////
        // Note that you should change the parameters below for your configuration.
        // //////////////////////////////////////////////////////////////////////////
        // Parameters for the reader
        String inputDir = "data/02_event_tuples";

        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription kmScriptCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                KarlMooneyScriptCounter.class, typeSystemDescription,
                KarlMooneyScriptCounter.PARAM_DB_DIR_PATH, "data/_db/",
                KarlMooneyScriptCounter.PARAM_SKIP_BIGRAM_N, 2,
                AbstractLoggingAnnotator.PARAM_KEEP_QUIET, false);

        SimplePipeline.runPipeline(reader, kmScriptCounter);

        System.out.println(className + " completed.");
    }
}
