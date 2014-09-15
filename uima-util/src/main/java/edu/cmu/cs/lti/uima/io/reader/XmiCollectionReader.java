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
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import edu.cmu.cs.lti.uima.util.StringConstants.BasicStringConstant;
import edu.cmu.cs.lti.uima.util.TimeUtils;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
public class XmiCollectionReader extends CollectionReader_ImplBase {
  /**
   * Name of configuration parameter that must be set to the path of a directory containing the XMI
   * files.
   */
  public static final String PARAM_VIEW_NAME = "ViewName";

  public static final String PARAM_PARENT_INPUT_DIR = "ParentInputDirectory";

  public static final String PARAM_BASE_INPUT_DIR_NAME = "BaseInputDirectoryName";

  public static final String PARAM_INPUT_DIR_DATE = "InputDirectoryDate";

  public static final String PARAM_STEP_NUMBER = "StepNumber";

  /**
   * Name of the configuration parameter that must be set to indicate if the execution fails if an
   * encountered type is unknown
   */
  public static final String PARAM_FAILUNKNOWN = "FailOnUnknownType";

  private String mViewName;

  private String mParentInputDir;

  private String mBaseDirectoryName;

  private String mInputDirectoryDate;

  private Integer mStepNumber;

  private Boolean mFailOnUnknownType;

  private ArrayList<File> mFiles;

  private int mCurrentIndex;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
   */
  public void initialize() throws ResourceInitializationException {
    mViewName = (String) getConfigParameterValue(PARAM_VIEW_NAME);
    mParentInputDir = (String) getConfigParameterValue(PARAM_PARENT_INPUT_DIR);
    mBaseDirectoryName = (String) getConfigParameterValue(PARAM_BASE_INPUT_DIR_NAME);
    mInputDirectoryDate = (String) getConfigParameterValue(PARAM_INPUT_DIR_DATE);
    if (StringUtils.isEmpty(mInputDirectoryDate)) {
      // Default date unless otherwise specified.
      mInputDirectoryDate = TimeUtils.getCurrentYYYYMMDD();
    }

    mStepNumber = (Integer) getConfigParameterValue(PARAM_STEP_NUMBER);

    mFailOnUnknownType = (Boolean) getConfigParameterValue(PARAM_FAILUNKNOWN);
    if (null == mFailOnUnknownType) {
      mFailOnUnknownType = true; // default to true if not specified
    }

    List<Object> partOfDirNames = new ArrayList<Object>();

    partOfDirNames.add(mInputDirectoryDate);
    if (mStepNumber != null) {
      String stepNumberStr = Integer.toString(mStepNumber);
      partOfDirNames.add(StringUtils.leftPad(stepNumberStr, 2, '0'));
    }
    partOfDirNames.add(mBaseDirectoryName);

    String inputDirectory = mParentInputDir + "/"
            + StringUtils.join(partOfDirNames, BasicStringConstant.UNDERSCORE.toString());
    File directory = new File(inputDirectory);
    mCurrentIndex = 0;

    // if input directory does not exist or is not a directory, throw exception
    if (!directory.exists() || !directory.isDirectory()) {
      throw new ResourceInitializationException(ResourceConfigurationException.DIRECTORY_NOT_FOUND,
              new Object[] { PARAM_PARENT_INPUT_DIR, this.getMetaData().getName(),
                  directory.getPath() });
    }

    // get list of .xmi files in the specified directory
    mFiles = new ArrayList<File>();
    File[] files = directory.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (!files[i].isDirectory() && files[i].getName().endsWith(".xmi")) {
        mFiles.add(files[i]);
      }
    }
  }

  /**
   * @see org.apache.uima.collection.CollectionReader#hasNext()
   */
  public boolean hasNext() {
    return mCurrentIndex < mFiles.size();
  }

  /**
   * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
   */
  public void getNext(CAS aCAS) throws IOException, CollectionException {
    try {
      if (!StringUtils.isEmpty(mViewName)) {
        aCAS.createView(mViewName);
        aCAS = aCAS.getView(mViewName);
      }
    } catch (Exception e) {
      throw new CollectionException(e);
    }

    File currentFile = (File) mFiles.get(mCurrentIndex++);
    FileInputStream inputStream = new FileInputStream(currentFile);
    try {
      XmiCasDeserializer.deserialize(inputStream, aCAS, !mFailOnUnknownType);
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
    return new Progress[] { new ProgressImpl(mCurrentIndex, mFiles.size(), Progress.ENTITIES) };
  }

}