/**
 *
 */
package edu.cmu.cs.lti.cds.runners;

import edu.cmu.cs.lti.cds.annotators.clustering.StreamingEntityCluster;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;
import java.util.Date;

/**
 * @author zhengzhongliu
 */
public class StreamingEntityClusteringRunner {
    private static String className = StreamingEntityClusteringRunner.class.getSimpleName();

    public static Date date = null;

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
        String paramInputDir = "data/02_event_tuples";

        // Parameters for the writer
        int stepNum = 2;
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);


        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, paramInputDir, false);
        AnalysisEngineDescription coreferenceProcessor = CustomAnalysisEngineFactory.createAnalysisEngine(StreamingEntityCluster.class, typeSystemDescription);

        SimplePipeline.runPipeline(reader, coreferenceProcessor);

        System.out.println(className + " completed.");
    }
}
