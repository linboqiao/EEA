package edu.cmu.cs.lti.cds.runners.validator;

import edu.cmu.cs.lti.cds.annotators.validators.EntityHeadValidator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;

public class EntityHeadValidatorRunner {
    private static String className = EntityHeadValidatorRunner.class.getSimpleName();

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
        String parentInputDirPath = "data";
        String baseInputDirName = "event_tuples";
        String inputFileSuffix = null;
        int inputStepNumber = 2;

        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createStepBasedGzippedXmiReader(parentInputDirPath, baseInputDirName, inputStepNumber, false);

        AnalysisEngineDescription validator = CustomAnalysisEngineFactory.createAnalysisEngine(
                EntityHeadValidator.class, typeSystemDescription);

        SimplePipeline.runPipeline(reader, validator);

        System.out.println(className + " completed.");
    }
}
