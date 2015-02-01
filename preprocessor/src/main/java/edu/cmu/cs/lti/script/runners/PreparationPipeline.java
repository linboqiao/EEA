package edu.cmu.cs.lti.script.runners; /**
 * 
 */

import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.collection_reader.AgigaCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;

/**
 * @author zhengzhongliu
 * 
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

    // ///////////////////////// Parameter Setting ////////////////////////////
    // Note that you should change the parameters below for your configuration.
    // //////////////////////////////////////////////////////////////////////////
    // Parameters for the reader
    String paramInputDir = args[0];// "/Users/zhengzhongliu/Downloads/agiga_sample"

    // Parameters for the writer
    String paramParentOutputDir = "data";
    String paramBaseOutputDirName = args[1];
    String paramOutputFileSuffix = null;

    String paramModelBaseDirectory = args[2];// "/Users/zhengzhongliu/Documents/projects/uimafied-tools/fanse-parser/src/main/resources/"
    // ////////////////////////////////////////////////////////////////

    String paramTypeSystemDescriptor = "TypeSystem";

    System.out.println("Reading from " + paramInputDir + " , writing to base dir " + args[1]);

    // Instantiate the analysis engine.
    TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
            .createTypeSystemDescription(paramTypeSystemDescriptor);

    // Instantiate a collection reader to get XMI as input.
    // Note that you should change the following parameters for your setting.
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
            AgigaCollectionReader.class, typeSystemDescription,
            AgigaCollectionReader.PARAM_INPUTDIR, paramInputDir);

    AnalysisEngineDescription fanseParser = CustomAnalysisEngineFactory.createAnalysisEngine(
            FanseAnnotator.class, typeSystemDescription, FanseAnnotator.PARAM_MODEL_BASE_DIR,
            paramModelBaseDirectory);

    // Instantiate a XMI writer to put XMI as output.
    // Note that you should change the following parameters for your setting.
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