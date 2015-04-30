/**
 *
 */
package edu.cmu.cs.lti.script.runners.writers;

import edu.cmu.cs.lti.script.annotators.writers.EntityFeatureExtractor;
import edu.cmu.cs.lti.script.annotators.writers.EventEntityLinkProducer;
import edu.cmu.cs.lti.script.annotators.writers.EventFeatureExtractor;
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
public class EntityFeatureGenerationPipeline {
    private static String className = EntityFeatureGenerationPipeline.class.getSimpleName();

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
        // String paramBaseOutputDirName = "entity_features_csv";
        String paramOutputFileSuffix = "csv";
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


        AnalysisEngineDescription entityFeatureWriter = AnalysisEngineFactory.createEngineDescription(
                EntityFeatureExtractor.class, typeSystemDescription,
                EntityFeatureExtractor.PARAM_BASE_OUTPUT_DIR_NAME, "entity_features",
                EntityFeatureExtractor.PARAM_OUTPUT_FILE_SUFFIX, paramOutputFileSuffix,
                EntityFeatureExtractor.PARAM_PARENT_OUTPUT_DIR, paramParentOutputDir,
                EntityFeatureExtractor.PARAM_STEP_NUMBER, stepNum);

        AnalysisEngineDescription eventFeatureWriter = AnalysisEngineFactory.createEngineDescription(
                EventFeatureExtractor.class, typeSystemDescription,
                EventFeatureExtractor.PARAM_BASE_OUTPUT_DIR_NAME, "event_features",
                EventFeatureExtractor.PARAM_OUTPUT_FILE_SUFFIX, paramOutputFileSuffix,
                EventFeatureExtractor.PARAM_PARENT_OUTPUT_DIR, paramParentOutputDir,
                EventFeatureExtractor.PARAM_STEP_NUMBER, stepNum);

        AnalysisEngineDescription entityEventLinkWriter = AnalysisEngineFactory.createEngineDescription(
                EventEntityLinkProducer.class, typeSystemDescription,
                EventEntityLinkProducer.PARAM_BASE_OUTPUT_DIR_NAME, "entity_event_links",
                EventEntityLinkProducer.PARAM_OUTPUT_FILE_SUFFIX, paramOutputFileSuffix,
                EventEntityLinkProducer.PARAM_PARENT_OUTPUT_DIR, paramParentOutputDir,
                EventEntityLinkProducer.PARAM_STEP_NUMBER, stepNum);

        SimplePipeline.runPipeline(reader, entityFeatureWriter, eventFeatureWriter,
                entityEventLinkWriter);

        System.out.println(className + " completed.");
    }

}
