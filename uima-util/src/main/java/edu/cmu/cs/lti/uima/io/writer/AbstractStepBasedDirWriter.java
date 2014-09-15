package edu.cmu.cs.lti.uima.io.writer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import com.google.common.base.Joiner;

import edu.cmu.cs.lti.uima.util.TimeUtils;

/**
 * An abstract writer to generate output in a directory whose name is based on the current date and
 * specified step number for convenience
 * 
 * @author Zhengzhong Liu, Hector
 * @author Jun Araki
 */
public abstract class AbstractStepBasedDirWriter extends JCasAnnotator_ImplBase {

  public static final String PARAM_PARENT_OUTPUT_DIR_PATH = "ParentOutputDirPath";

  public static final String PARAM_BASE_OUTPUT_DIR_NAME = "BaseOutputDirectoryName";

  public static final String PARAM_OUTPUT_STEP_NUMBER = "OutputStepNumber";

  public static final String PARAM_OUTPUT_FILE_SUFFIX = "OutputFileSuffix";

  @ConfigurationParameter(name = PARAM_PARENT_OUTPUT_DIR_PATH, mandatory = true)
  private String parentOutputDirPath;

  @ConfigurationParameter(name = PARAM_BASE_OUTPUT_DIR_NAME, mandatory = true)
  private String baseOutputDirName;

  @ConfigurationParameter(name = PARAM_OUTPUT_STEP_NUMBER, mandatory = true)
  private Integer outputStepNumber;

  @ConfigurationParameter(name = PARAM_OUTPUT_FILE_SUFFIX, mandatory = false)
  protected String outputFileSuffix;

  protected File outputDir;

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);

    List<String> dirNameSegments = new ArrayList<String>();
    dirNameSegments.add(String.format("%02d", outputStepNumber));
    dirNameSegments.add(baseOutputDirName);

    String dirName = Joiner.on("_").join(dirNameSegments);
    outputDir = new File(parentOutputDirPath, dirName);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    subInitialize(aContext);
  }

  /**
   * A sub-class can do its own initialization in this method.
   * 
   * @param aContext
   */
  public abstract void subInitialize(UimaContext aContext);

}
