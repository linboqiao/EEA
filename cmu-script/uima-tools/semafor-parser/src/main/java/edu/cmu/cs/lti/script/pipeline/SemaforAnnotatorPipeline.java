package edu.cmu.cs.lti.script.pipeline;

import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.pipeline.SimplePipeline;

/**
 * This pipeline runs FanseAnnotator.
 */
public class SemaforAnnotatorPipeline {

    private static String className = SemaforAnnotatorPipeline.class.getSimpleName();

    public static void main(String[] args) throws UIMAException {
        System.out.println(className + " started...");

        // ///////////////////////// Parameter Setting ////////////////////////////
        // Note that you should change the parameters below for your configuration.
        // //////////////////////////////////////////////////////////////////////////
        // Parameters for the reader
        String paramParentInputDir = "../edu.cmu.lti.event_coref.system_jun/data/02_event_coreference_resolution/IC_domain_65_articles_v1";
        String paramBaseInputDirName = "xmi_after_annotating_stanford_corenlp";
        int paramInputStepNumber = 3;
        boolean paramFailUnknown = false;

        // Parameters for the analyzer
        String paramModelBaseDirectory = "src/main/resources/";
        String paramTypeSystemDescriptor = "EventCoreferenceAllTypeSystems";

        // Parameters for the writer
        String paramParentOutputDir = paramParentInputDir;
        String paramBaseOutputDirName = "xmi_after_annotating_fanse";
        String paramOutputFileSuffix = null;
        int paramOutputStepNumber = paramInputStepNumber + 1;
        // ////////////////////////////////////////////////////////////////

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(
                paramParentInputDir, paramBaseInputDirName, paramInputStepNumber,
                paramFailUnknown);

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);
        AnalysisEngineDescription analyzer = AnalysisEngineFactory.createEngineDescription(
                SemaforAnnotator.class, typeSystemDescription,
                SemaforAnnotator.SEMAFOR_MODEL_PATH, paramModelBaseDirectory);

        // Instantiate a XMI writer to put XMI as output.
        // Note that you should change the following parameters for your setting.
        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, paramOutputStepNumber,
                paramOutputFileSuffix);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, analyzer, writer);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println(className + " successfully completed.");
    }

}
