/**
 *
 */
package edu.cmu.cs.lti.script.annotators.writers;

import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalysisEngine;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.IOException;

/**
 * @author zhengzhongliu
 */
public class DocumentTextPrinter extends AbstractCustomizedTextWriterAnalysisEngine {

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalsysisEngine#getTextToPrint(org
     * .apache.uima.jcas.JCas)
     */
    @Override
    public String getTextToPrint(JCas aJCas) {
        return aJCas.getDocumentText();
    }


    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        String className = DocumentTextPrinter.class.getSimpleName();

        System.out.println(className + " started...");


        // Parameters for the reader
        String parentInput = "data";
        String baseInput = "01_event_tuples_sample";

        // Parameters for the writer
        String paramParentOutputDir = "data";
        String paramBaseOutputDirName = "plain_text";
        int stepNum = 2;

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, parentInput, baseInput);

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                DocumentTextPrinter.class, typeSystemDescription,
                AbstractCustomizedTextWriterAnalysisEngine.PARAM_BASE_OUTPUT_DIR_NAME,
                paramBaseOutputDirName,
                AbstractCustomizedTextWriterAnalysisEngine.PARAM_PARENT_OUTPUT_DIR_PATH,
                paramParentOutputDir,
                // AbstractCustomizedTextWriterAnalsysisEngine.PARAM_OUTPUT_FILE_SUFFIX, null,
                AbstractCustomizedTextWriterAnalysisEngine.PARAM_OUTPUT_STEP_NUMBER, stepNum);

        SimplePipeline.runPipeline(reader, writer);

        System.out.println(className + " completed.");
    }

}
