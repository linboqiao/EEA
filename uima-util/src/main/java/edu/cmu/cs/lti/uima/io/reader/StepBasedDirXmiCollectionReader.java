package edu.cmu.cs.lti.uima.io.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.xml.sax.SAXException;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
public class StepBasedDirXmiCollectionReader extends AbstractStepBasedDirReader {

  public static final String PARAM_INPUT_VIEW_NAME = "ViewName";

  private String inputViewName;

  private List<File> xmiFiles;

  private int currentDocIndex;

  /**
   * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
   */
  public void initialize() throws ResourceInitializationException {
    super.initialize();

    inputViewName = (String) getConfigParameterValue(PARAM_INPUT_VIEW_NAME);
    if (StringUtils.isEmpty(inputFileSuffix)) {
      inputFileSuffix = ".xmi";
    }

    // Get a list of XMI files in the specified directory
    xmiFiles = new ArrayList<File>();
    File[] files = inputDir.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (!files[i].isDirectory() && files[i].getName().endsWith(inputFileSuffix)) {
        xmiFiles.add(files[i]);
      }
    }

    currentDocIndex = 0;
  }

  @Override
  public void subInitialize() {
  }

  /**
   * @see org.apache.uima.collection.CollectionReader#hasNext()
   */
  public boolean hasNext() {
    return currentDocIndex < xmiFiles.size();
  }

  /**
   * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
   */
  public void getNext(CAS aCAS) throws IOException, CollectionException {
    try {
      if (!StringUtils.isEmpty(inputViewName)) {
        aCAS = aCAS.getView(inputViewName);
      }
    } catch (Exception e) {
      throw new CollectionException(e);
    }

    File currentFile = xmiFiles.get(currentDocIndex);
    currentDocIndex++;

    FileInputStream inputStream = new FileInputStream(currentFile);
    try {
      XmiCasDeserializer.deserialize(inputStream, aCAS, !failOnUnknownType);
    } catch (SAXException e) {
      throw new CollectionException(e);
    } finally {
      inputStream.close();
    }
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
   */
  public void close() throws IOException {
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
   */
  public Progress[] getProgress() {
    return new Progress[] { new ProgressImpl(currentDocIndex, xmiFiles.size(), Progress.ENTITIES) };
  }

}