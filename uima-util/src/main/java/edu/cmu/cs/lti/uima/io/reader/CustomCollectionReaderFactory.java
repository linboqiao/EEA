package edu.cmu.cs.lti.uima.io.reader;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

public class CustomCollectionReaderFactory {

  /**
   * Creates a simple XMI reader assuming the directory naming convention
   * 
   * @param parentInputDirName
   * @param baseInputDirName
   * @param stepNumber
   * @param failOnUnkown
   * @return
   * @throws ResourceInitializationException
   */
  public static CollectionReaderDescription createXmiReader(String parentInputDirName,
          String baseInputDirName, Integer stepNumber, Boolean failOnUnkown)
          throws ResourceInitializationException {
    // Instantiate a collection reader to get XMI as input.
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
            StepBasedDirXmiCollectionReader.class,
            StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
            StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
            StepBasedDirXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber,
            StepBasedDirXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);

    return reader;
  }

  /**
   * Creates a simple XMI reader with the specified type system, assuming the directory naming
   * convention.
   * 
   * @param typeSystemDescription
   * @param parentInputDirName
   * @param baseInputDirName
   * @param stepNumber
   * @param failOnUnkown
   * @return
   * @throws ResourceInitializationException
   */
  public static CollectionReaderDescription createXmiReader(
          TypeSystemDescription typeSystemDescription, String parentInputDirName,
          String baseInputDirName, Integer stepNumber, Boolean failOnUnkown)
          throws ResourceInitializationException {
    // Instantiate a collection reader to get XMI as input.
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
            XmiCollectionReader.class, typeSystemDescription,
            XmiCollectionReader.PARAM_PARENT_INPUT_DIR, parentInputDirName,
            XmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
            XmiCollectionReader.PARAM_STEP_NUMBER, stepNumber,
            XmiCollectionReader.PARAM_FAILUNKNOWN, failOnUnkown);
    return reader;
  }

  /**
   * Create a gzipped XMI reader assuming the directory naming convention.
   * 
   * @param parentInputDirName
   * @param baseInputDirName
   * @param stepNumber
   * @param failOnUnkown
   * @return
   * @throws ResourceInitializationException
   */
  public static CollectionReaderDescription createGzippedXmiReader(String parentInputDirName,
          String baseInputDirName, Integer stepNumber, Boolean failOnUnkown)
          throws ResourceInitializationException {
    // Instantiate a collection reader to get XMI as input.
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
            StepBasedDirGzippedXmiCollectionReader.class,
            StepBasedDirGzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
            StepBasedDirGzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
            StepBasedDirGzippedXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber,
            StepBasedDirGzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);

    return reader;
  }

  /**
   * Create a gzipped XMI reader, with specified type system. Assuming the directory naming
   * convention.
   * 
   * @param typeSystemDescription
   * @param parentInputDirName
   * @param baseInputDirName
   * @param stepNumber
   * @param failOnUnkown
   * @return
   * @throws ResourceInitializationException
   */
  public static CollectionReaderDescription createGzippedXmiReader(
          TypeSystemDescription typeSystemDescription, String parentInputDirName,
          String baseInputDirName, Integer stepNumber, Boolean failOnUnkown)
          throws ResourceInitializationException {
    // Instantiate a collection reader to get XMI as input.
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
            StepBasedDirGzippedXmiCollectionReader.class, typeSystemDescription,
            StepBasedDirGzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
            StepBasedDirGzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
            StepBasedDirGzippedXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber,
            StepBasedDirGzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);

    return reader;
  }

  /**
   * Creates a Gzipped XMI reader assuming the directory naming convention
   * 
   * @param parentInputDirName
   * @param baseInputDirName
   * @param stepNumber
   * @param failOnUnkown
   * @return
   * @throws ResourceInitializationException
   */
  public static CollectionReaderDescription createGzipXmiReader(String parentInputDirName,
          String baseInputDirName, Integer stepNumber, Boolean failOnUnkown)
          throws ResourceInitializationException {
    // Instantiate a collection reader to get XMI as input.
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
            StepBasedDirGzippedXmiCollectionReader.class,
            StepBasedDirGzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
            StepBasedDirGzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
            StepBasedDirGzippedXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber,
            StepBasedDirGzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);

    return reader;
  }

  /**
   * Creates a Gzipped XMI reader assuming the directory naming convention
   * 
   * @param parentInputDirName
   * @param baseInputDirName
   * @param dirDate
   * @param stepNumber
   * @param failOnUnkown
   * @return
   * @throws ResourceInitializationException
   */
  public static CollectionReaderDescription createGzipXmiReader(
          TypeSystemDescription typeSystemDescription, String parentInputDirName,
          String baseInputDirName, Integer stepNumber, Boolean failOnUnkown)
          throws ResourceInitializationException {
    // Instantiate a collection reader to get XMI as input.
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
            StepBasedDirGzippedXmiCollectionReader.class, typeSystemDescription,
            StepBasedDirGzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
            StepBasedDirGzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
            StepBasedDirGzippedXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber,
            StepBasedDirGzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);

    return reader;
  }

  /**
   * Creates a simple plain text reader for the text under the specified directory.
   * 
   * @param viewName
   * @param parentInputDirName
   * @param encoding
   * @param textSuffix
   * @return
   * @throws ResourceInitializationException
   */
  public static CollectionReaderDescription createPlainTextReader(String inputViewName,
          String parentInputDirName, String encoding, String[] textSuffix)
          throws ResourceInitializationException {
    // Instantiate a collection reader to get plain text as input.
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
            PlainTextCollectionReader.class, PlainTextCollectionReader.PARAM_INPUT_VIEW_NAME,
            inputViewName, PlainTextCollectionReader.PARAM_INPUTDIR, parentInputDirName,
            PlainTextCollectionReader.PARAM_ENCODING, encoding,
            PlainTextCollectionReader.PARAM_TEXT_SUFFIX, textSuffix);
    return reader;
  }

  public static CollectionReaderDescription createPlainTextReader(String inputViewName,
          String[] srcDocInfoViewNames, String parentInputDirName, String encoding,
          String[] textSuffix) throws ResourceInitializationException {
    // Instantiate a collection reader to get plain text as input.
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
            PlainTextCollectionReader.class, PlainTextCollectionReader.PARAM_INPUT_VIEW_NAME,
            inputViewName, PlainTextCollectionReader.PARAM_SRC_DOC_INFO_VIEW_NAMES,
            srcDocInfoViewNames, PlainTextCollectionReader.PARAM_INPUTDIR, parentInputDirName,
            PlainTextCollectionReader.PARAM_ENCODING, encoding,
            PlainTextCollectionReader.PARAM_TEXT_SUFFIX, textSuffix);
    return reader;
  }

}
