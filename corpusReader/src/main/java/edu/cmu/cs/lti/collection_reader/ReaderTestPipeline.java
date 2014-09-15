package edu.cmu.cs.lti.collection_reader;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;

/**
 * This pipeline runs FanseAnnotator.
 * 
 */
public class ReaderTestPipeline {

  private static String className = ReaderTestPipeline.class.getSimpleName();

  public static void main(String[] args) throws UIMAException {
    System.out.println(className + " started...");

    // ///////////////////////// Parameter Setting ////////////////////////////
    // Note that you should change the parameters below for your configuration.
    // //////////////////////////////////////////////////////////////////////////
    // Parameters for the reader
    String paramInputDir = "/Users/zhengzhongliu/Downloads/agiga_sample";
 
    // Parameters for the writer
    String paramParentOutputDir = "data";
    String paramBaseOutputDirName = "xmi";
    String paramOutputFileSuffix = null;
    // ////////////////////////////////////////////////////////////////

    String paramTypeSystemDescriptor = "TypeSystem";
    
    // Instantiate the analysis engine.
    TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
            .createTypeSystemDescription(paramTypeSystemDescriptor);
    
    // Instantiate a collection reader to get XMI as input.
    // Note that you should change the following parameters for your setting.
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
            AgigaCollectionReader.class,typeSystemDescription,
            AgigaCollectionReader.PARAM_INPUTDIR, paramInputDir);
    
    // Instantiate a XMI writer to put XMI as output.
    // Note that you should change the following parameters for your setting.
    AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
            paramParentOutputDir, paramBaseOutputDirName, 0,
            paramOutputFileSuffix);

    // Run the pipeline.
    try {
      SimplePipeline.runPipeline(reader, writer);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

    System.out.println(className + " successfully completed.");
  }

}
