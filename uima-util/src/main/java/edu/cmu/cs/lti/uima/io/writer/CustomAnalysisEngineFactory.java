package edu.cmu.cs.lti.uima.io.writer;

import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;

public class CustomAnalysisEngineFactory {

  /**
   * Creates a simple analysis engine with a specified type system Current it cannot provide wrapper
   * to parameters
   * 
   * @param engineClass
   * @param typeSystemDescription
   * @return
   * @throws ResourceInitializationException
   */
  public static <T extends JCasAnnotator_ImplBase> AnalysisEngineDescription createAnalysisEngine(
          Class<T> engineClass, TypeSystemDescription typeSystemDescription)
          throws ResourceInitializationException {
    // Instantiate the analysis engine.
    AnalysisEngineDescription engine = AnalysisEngineFactory.createEngineDescription(
            engineClass, typeSystemDescription);
    return engine;
  }

  public static <T extends JCasAnnotator_ImplBase> AnalysisEngineDescription createAnalysisEngine(
          Class<T> engineClass, TypeSystemDescription typeSystemDescription,
          Object... configurationData) throws ResourceInitializationException {
    // Instantiate the analysis engine.
    AnalysisEngineDescription engine = AnalysisEngineFactory.createEngineDescription(
            engineClass, typeSystemDescription, configurationData);
    return engine;
  }

  public static void setTypeSystem(AnalysisEngineDescription coreferenceEngine,
          TypeSystemDescription typeSystem) {
    AnalysisEngineMetaData metatData = coreferenceEngine.getAnalysisEngineMetaData();
    metatData.setTypeSystem(typeSystem);
    coreferenceEngine.setMetaData(metatData);
  }

  /**
   * Creates an XMI writer assuming the directory naming convention
   * 
   * @param parentOutputDirPath
   * @param baseOutputDirName
   * @param stepNumber
   * @param outputFileSuffix
   * @return
   * @throws ResourceInitializationException
   */
  public static AnalysisEngineDescription createXmiWriter(String parentOutputDirPath,
          String baseOutputDirName, Integer stepNumber, String outputFileSuffix)
          throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            StepBasedDirXmiWriter.class, StepBasedDirXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH,
            parentOutputDirPath, StepBasedDirXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME,
            baseOutputDirName, StepBasedDirXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            StepBasedDirXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber);
    return writer;
  }

  /**
   * Creates an XMI writer assuming the directory naming convention
   * 
   * @param parentOutputDirPath
   * @param baseOutputDirName
   * @param stepNumber
   * @param outputFileSuffix
   * @param srcDocInfoViewName
   *          the view that contains the source document info
   * @return
   * @throws ResourceInitializationException
   */
  public static AnalysisEngineDescription createXmiWriter(String parentOutputDirPath,
          String baseOutputDirName, Integer stepNumber, String outputFileSuffix,
          String srcDocInfoViewName) throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            StepBasedDirXmiWriter.class, StepBasedDirXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH,
            parentOutputDirPath, StepBasedDirXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME,
            baseOutputDirName, StepBasedDirXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            StepBasedDirXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
            StepBasedDirXmiWriter.PARAM_SRC_DOC_INFO_VIEW_NAME, srcDocInfoViewName);
    return writer;
  }

  /**
   * Creates an XMI writer assuming the directory naming convention but compress into gzip
   * 
   * @param parentOutputDirPath
   * @param baseOutputDirName
   * @param stepNumber
   * @param outputFileSuffix
   * @param srcDocInfoViewName
   * @return
   * @throws ResourceInitializationException
   */
  public static AnalysisEngineDescription createGzipWriter(String parentOutputDirPath,
          String baseOutputDirName, Integer stepNumber, String outputFileSuffix,
          String srcDocInfoViewName) throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            StepBasedDirGzippedXmiWriter.class,
            StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
            StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
            StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
            StepBasedDirGzippedXmiWriter.PARAM_SRC_DOC_INFO_VIEW_NAME, srcDocInfoViewName);
    return writer;
  }

  /**
   * Creates an XMI writer assuming the directory naming convention but compress into gzip
   * 
   * @param parentOutputDirPath
   * @param baseOutputDirName
   * @param stepNumber
   * @param outputFileSuffix
   * @param srcDocInfoViewName
   * @return
   * @throws ResourceInitializationException
   */
  public static AnalysisEngineDescription createSelectiveGzipWriter(String parentOutputDirPath,
          String baseOutputDirName, Integer stepNumber, String outputFileSuffix,
          String srcDocInfoViewName, Set<Integer> outputDocumentNumbers)
          throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            StepBasedDirGzippedXmiWriter.class,
            StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
            StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
            StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
            StepBasedDirGzippedXmiWriter.PARAM_SRC_DOC_INFO_VIEW_NAME, srcDocInfoViewName,
            StepBasedDirXmiWriter.PARAM_OUTPUT_FILE_NUMBERS, outputDocumentNumbers);
    return writer;
  }

  /**
   * Creates an XMI writer assuming the directory naming convention and provides an array of indices
   * to select documents to output
   * 
   * @param parentOutputDirPath
   * @param baseOutputDirName
   * @param stepNumber
   * @param outputFileSuffix
   * @param srcDocInfoViewName
   *          the view that contains the source document info
   * @return
   * @throws ResourceInitializationException
   */
  public static AnalysisEngineDescription createSelectiveXmiWriter(String parentOutputDirPath,
          String baseOutputDirName, Integer stepNumber, String outputFileSuffix,
          Set<Integer> outputDocumentNumbers) throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            StepBasedDirXmiWriter.class, StepBasedDirXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH,
            parentOutputDirPath, StepBasedDirXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME,
            baseOutputDirName, StepBasedDirXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            StepBasedDirXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
            StepBasedDirXmiWriter.PARAM_OUTPUT_FILE_NUMBERS, outputDocumentNumbers);
    return writer;
  }

  /**
   * Creates a gzipped XMI writer without specifying a particular output view.
   * 
   * @param parentOutputDirPath
   * @param baseOutputDirName
   * @param stepNumber
   * @param outputFileSuffix
   * @param viewName
   * @return
   * @throws ResourceInitializationException
   */
  public static AnalysisEngineDescription createGzippedXmiWriter(String parentOutputDirPath,
          String baseOutputDirName, Integer stepNumber, String outputFileSuffix)
          throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            StepBasedDirGzippedXmiWriter.class,
            StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
            StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
            StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber);
    return writer;
  }

  /**
   * Creates a gzipped XMI writer while specifying an output view.
   * 
   * @param parentOutputDirPath
   * @param baseOutputDirName
   * @param stepNumber
   * @param outputFileSuffix
   * @param srcDocInfoViewName
   * @return
   * @throws ResourceInitializationException
   */
  public static AnalysisEngineDescription createGzippedXmiWriter(String parentOutputDirPath,
          String baseOutputDirName, Integer stepNumber, String outputFileSuffix,
          String srcDocInfoViewName) throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            StepBasedDirGzippedXmiWriter.class,
            StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
            StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
            StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
            StepBasedDirGzippedXmiWriter.PARAM_SRC_DOC_INFO_VIEW_NAME, srcDocInfoViewName);
    return writer;
  }

  public static AnalysisEngineDescription createSelectiveGzippedXmiWriter(
          String parentOutputDirPath, String baseOutputDirName, Integer stepNumber,
          String outputFileSuffix, Set<Integer> outputDocumentNumbers)
          throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            StepBasedDirGzippedXmiWriter.class,
            StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
            StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
            StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
            StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_NUMBERS, outputDocumentNumbers);
    return writer;
  }

  /**
   * Creates a plain text writer.
   * 
   * @param parentOutputDirPath
   * @param baseOutputDirName
   * @param stepNumber
   * @param outputFileSuffix
   * @param viewName
   * @return
   * @throws ResourceInitializationException
   */
  public static AnalysisEngineDescription createDocumentTextWriter(String parentOutputDirPath,
          String baseOutputDirName, Integer stepNumber, String outputFileSuffix, String viewName)
          throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            DocumentTextWriter.class, DocumentTextWriter.PARAM_PARENT_OUTPUT_DIR,
            parentOutputDirPath, DocumentTextWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
            DocumentTextWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            DocumentTextWriter.PARAM_STEP_NUMBER, stepNumber,
            DocumentTextWriter.PARAM_SOURCE_DOCUMENT_INFO_VIEW_NAME, viewName);
    return writer;
  }

  /**
   * 
   * Create a description for subclasses of AbstractCustomizedTextWriterAnalsysisEngine. However, if
   * you need to use additional parameters, this method cannot help you.
   * 
   * @param writerClass
   *          The writer engine class, which need to extend
   *          {@link AbstractCustomizedTextWriterAnalsysisEngine}
   * @param parentOutputDirPath
   * @param baseOutputDirName
   * @param stepNumber
   * @param outputFileSuffix
   * @param configurationData
   * @return
   * @throws ResourceInitializationException
   */
  public static <T extends AbstractCustomizedTextWriterAnalsysisEngine> AnalysisEngineDescription createCustomizedTextWriter(
          Class<T> writerClass, TypeSystemDescription typeSystemDescription,
          String parentOutputDirPath, String baseOutputDirName, Integer stepNumber,
          String outputFileSuffix, String sourceDocumentViewName)
          throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            writerClass, typeSystemDescription,
            AbstractCustomizedTextWriterAnalsysisEngine.PARAM_PARENT_OUTPUT_DIR,
            parentOutputDirPath,
            AbstractCustomizedTextWriterAnalsysisEngine.PARAM_BASE_OUTPUT_DIR_NAME,
            baseOutputDirName,
            AbstractCustomizedTextWriterAnalsysisEngine.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            AbstractCustomizedTextWriterAnalsysisEngine.PARAM_STEP_NUMBER, stepNumber,
            AbstractCustomizedTextWriterAnalsysisEngine.PARAM_SOURCE_DOCUMENT_INFO_VIEW_NAME,
            sourceDocumentViewName);
    return writer;
  }

  /**
   * Create a description for subclasses of AbstractCsvWriterAnalysisEngine. However, if you need to
   * use additional parameters, this method cannot help you.
   * 
   * @param writerClass
   * @param typeSystemDescription
   * @param parentOutputDirPath
   * @param baseOutputDirName
   * @param stepNumber
   * @param outputFileSuffix
   * @param sourceDocumentInfoViewName
   * @param configurationData
   * @return
   * @throws ResourceInitializationException
   */
  public static <T extends AbstractCsvWriterAnalysisEngine> AnalysisEngineDescription createCustomizedCsvWriter(
          Class<T> writerClass, TypeSystemDescription typeSystemDescription,
          String parentOutputDirPath, String baseOutputDirName, Integer stepNumber,
          String outputFileSuffix, String sourceDocumentInfoViewName)
          throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            writerClass, typeSystemDescription,
            AbstractCsvWriterAnalysisEngine.PARAM_PARENT_OUTPUT_DIR, parentOutputDirPath,
            AbstractCsvWriterAnalysisEngine.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
            AbstractCsvWriterAnalysisEngine.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            AbstractCsvWriterAnalysisEngine.PARAM_STEP_NUMBER, stepNumber,
            AbstractCsvWriterAnalysisEngine.PARAM_SOURCE_DOCUMENT_INFO_VIEW_NAME,
            sourceDocumentInfoViewName);

    return writer;
  }

  /**
   * Creates a customized plain text aggregator.
   * 
   * @param parentOutputDirPath
   * @return
   * @throws ResourceInitializationException
   */
  public static <T extends AbstractPlainTextAggregator> AnalysisEngineDescription createCustomPlainTextAggregator(
          Class<T> writerClass, TypeSystemDescription typeSystemDescription, String outputFilePath)
          throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            writerClass, typeSystemDescription, AbstractPlainTextAggregator.PARAM_OUTPUT_FILE_PATH,
            outputFilePath);

    return writer;
  }

  /**
   * Creates a plain text aggregator.
   * 
   * @param parentOutputDirPath
   * @param baseOutputDirName
   * @param stepNumber
   * @param outputFileSuffix
   * @param outputFileName
   * @return
   * @throws ResourceInitializationException
   */
  public static <T extends AbstractStepBasedDirPlainTextAggregator> AnalysisEngineDescription createCustomPlainTextAggregator(Class<T> writerClass,
          TypeSystemDescription typeSystemDescription, String parentOutputDirPath,
          String baseOutputDirName, Integer stepNumber, String outputFileSuffix,
          String outputFileName) throws ResourceInitializationException {
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
            writerClass, typeSystemDescription,
            AbstractStepBasedDirPlainTextAggregator.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
            AbstractStepBasedDirPlainTextAggregator.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
            AbstractStepBasedDirPlainTextAggregator.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
            AbstractStepBasedDirPlainTextAggregator.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
            AbstractStepBasedDirPlainTextAggregator.PARAM_OUTPUT_FILE_NAME, outputFileName);
    return writer;
  }

}
