package edu.cmu.cs.lti.uima.io.reader;

import com.google.common.base.Joiner;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstract reader to consume input in a directory whose name is based on the
 * step number for convenience
 *
 * @author Jun Araki
 * @author Zhengzhong Liu
 */
public abstract class AbstractStepBasedDirReader extends JCasCollectionReader_ImplBase {

    public static final String PARAM_PARENT_INPUT_DIR_PATH = "ParentInputDirPath";

    public static final String PARAM_BASE_INPUT_DIR_NAME = "BaseInputDirectoryName";

    public static final String PARAM_INPUT_STEP_NUMBER = "InputStepNumber";

    public static final String PARAM_INPUT_FILE_SUFFIX = "InputFileSuffix";

    public static final String PARAM_FAIL_UNKNOWN = "FailOnUnknownType";

    public static final String PARAM_INPUT_VIEW_NAME = "ViewName";

    @ConfigurationParameter(name = PARAM_PARENT_INPUT_DIR_PATH)
    private String parentInputDirPath;

    @ConfigurationParameter(name = PARAM_BASE_INPUT_DIR_NAME)
    private String baseInputDirName;

    @ConfigurationParameter(name = PARAM_INPUT_STEP_NUMBER, mandatory = false)
    private Integer inputStepNumber;

    @ConfigurationParameter(name = PARAM_INPUT_FILE_SUFFIX, mandatory = false)
    protected String inputFileSuffix;

    @ConfigurationParameter(name = PARAM_INPUT_VIEW_NAME, mandatory = false)
    protected String inputViewName;

    @ConfigurationParameter(name = PARAM_FAIL_UNKNOWN, defaultValue = "false")
    protected Boolean failOnUnknownType;

    protected File inputDir;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        List<String> dirNameSegments = new ArrayList<String>();

        if (inputStepNumber != null) {
            dirNameSegments.add(String.format("%02d", inputStepNumber));
        }
//        if (!StringUtils.isEmpty(inputFileSuffix)) {
//            dirNameSegments.add(inputFileSuffix);
//        }
        dirNameSegments.add(baseInputDirName);

        String dirName = Joiner.on("_").join(dirNameSegments);

        inputDir = new File(parentInputDirPath, dirName);
        if (!inputDir.exists()) {
            throw new IllegalArgumentException(String.format(
                    "Cannot find the directory [%s] specified, please check parameters",
                    inputDir.getAbsolutePath()));
        }

        logger.info(String.format("Reading from [%s]", inputDir.getAbsolutePath()));
    }

}
