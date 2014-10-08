/**
 * 
 */
package edu.cmu.cs.lti.cds.runners.writers;

import edu.cmu.cs.lti.cds.annotators.writers.DocumentTextPrinter;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalsysisEngine;
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
 * 
 */
public class DocumentTextPrinterRunner {

  private static String className = DocumentTextPrinterRunner.class.getSimpleName();

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
    String paramInputDir = "data/01_event_tuples_sample";

    // Parameters for the writer
    String paramParentOutputDir = "data";
    String paramBaseOutputDirName = "plain_text";
    int stepNum = 2;
    // ////////////////////////////////////////////////////////////////

    String paramTypeSystemDescriptor = "TypeSystem";

    // Instantiate the analysis engine.
    TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
            .createTypeSystemDescription(paramTypeSystemDescriptor);

    // Instantiate a collection reader to get XMI as input.
    // Note that you should change the following parameters for your setting.
      CollectionReaderDescription reader =
              CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, paramInputDir, false);

    AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createAnalysisEngine(
            DocumentTextPrinter.class, typeSystemDescription,
            AbstractCustomizedTextWriterAnalsysisEngine.PARAM_BASE_OUTPUT_DIR_NAME,
            paramBaseOutputDirName,
            AbstractCustomizedTextWriterAnalsysisEngine.PARAM_PARENT_OUTPUT_DIR,
            paramParentOutputDir,
            // AbstractCustomizedTextWriterAnalsysisEngine.PARAM_OUTPUT_FILE_SUFFIX, null,
            AbstractCustomizedTextWriterAnalsysisEngine.PARAM_STEP_NUMBER, stepNum);

    SimplePipeline.runPipeline(reader, writer);

    System.out.println(className + " completed.");
  }
}
