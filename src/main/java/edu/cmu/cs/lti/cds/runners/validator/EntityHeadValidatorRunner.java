package edu.cmu.cs.lti.cds.runners.validator;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.examples.xmi.XmiCollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import edu.cmu.cs.lti.cds.annotators.validators.EntityHeadValidator;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;

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
    String paramInputDir = "data/01_event_tuples";

    // ////////////////////////////////////////////////////////////////

    String paramTypeSystemDescriptor = "TypeSystem";

    // Instantiate the analysis engine.
    TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
            .createTypeSystemDescription(paramTypeSystemDescriptor);

    // Instantiate a collection reader to get XMI as input.
    // Note that you should change the following parameters for your setting.
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
            XmiCollectionReader.class, typeSystemDescription, XmiCollectionReader.PARAM_INPUTDIR,
            paramInputDir);

    AnalysisEngineDescription validator = CustomAnalysisEngineFactory.createAnalysisEngine(
            EntityHeadValidator.class, typeSystemDescription);

    SimplePipeline.runPipeline(reader, validator);

    System.out.println(className + " completed.");
  }
}
