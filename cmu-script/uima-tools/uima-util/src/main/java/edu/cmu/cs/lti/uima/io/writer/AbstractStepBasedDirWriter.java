package edu.cmu.cs.lti.uima.io.writer;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstract writer to generate output in a directory whose name is based on the current date and
 * specified step number for convenience
 *
 * @author Zhengzhong Liu, Hector
 * @author Jun Araki
 */
public abstract class AbstractStepBasedDirWriter extends AbstractLoggingAnnotator {

    public static final String PARAM_PARENT_OUTPUT_DIR_PATH = "ParentOutputDirPath";

    public static final String PARAM_BASE_OUTPUT_DIR_NAME = "BaseOutputDirectoryName";

    public static final String PARAM_OUTPUT_STEP_NUMBER = "OutputStepNumber";

    public static final String PARAM_OUTPUT_FILE_SUFFIX = "OutputFileSuffix";

    public static final String PARAM_SRC_DOC_INFO_VIEW_NAME = "SourceDocumentInfoViewName";

    @ConfigurationParameter(name = PARAM_PARENT_OUTPUT_DIR_PATH, mandatory = true)
    private String parentOutputDirPath;

    @ConfigurationParameter(name = PARAM_BASE_OUTPUT_DIR_NAME, mandatory = true)
    private String baseOutputDirName;

    @ConfigurationParameter(name = PARAM_OUTPUT_STEP_NUMBER, mandatory = false)
    private Integer outputStepNumber;

    @ConfigurationParameter(name = PARAM_OUTPUT_FILE_SUFFIX, mandatory = false)
    protected String outputFileSuffix;

    @ConfigurationParameter(name = PARAM_SRC_DOC_INFO_VIEW_NAME, mandatory = false)
    /** The view where you extract source document information */
    protected String srcDocInfoViewName;

    protected File outputDir;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        List<String> dirNameSegments = new ArrayList<String>();
        if (outputStepNumber != null) {
            dirNameSegments.add(String.format("%02d", outputStepNumber));
        }
        dirNameSegments.add(baseOutputDirName);

        String dirName = Joiner.on("_").join(dirNameSegments);
        outputDir = new File(parentOutputDirPath, dirName);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try {
            logger.info("Writing documents to " + outputDir.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
