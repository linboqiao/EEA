package edu.cmu.cs.lti.uima.io.reader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
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
 */
public abstract class AbstractDirReader extends CollectionReader_ImplBase {

    public static final String PARAM_INPUT_DIR = "InputDirectory";

    public static final String PARAM_FILE_SUFFIX = "InputFileSuffix";

    public static final String PARAM_FAIL_UNKNOWN = "FailOnUnknownType";

    public static final String PARAM_RECURSIVE = "recursive";

    public static final String PARAM_INPUT_VIEW_NAME = "ViewName";

    protected Boolean failOnUnknownType;

    protected File inputDir;

    protected String inputFileSuffix;

    protected List<File> xmiFiles;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String inputViewName;

    protected int currentDocIndex;

    @Override
    public void initialize() throws ResourceInitializationException {
        super.initialize();

        failOnUnknownType = (Boolean) getConfigParameterValue(PARAM_FAIL_UNKNOWN);
        if (null == failOnUnknownType) {
            failOnUnknownType = true; // default to true if not specified
        }


        Boolean recursive = (Boolean) getConfigParameterValue(PARAM_RECURSIVE);
        if (recursive == null) {
            recursive = false;
        }

        inputFileSuffix = (String) getConfigParameterValue(PARAM_FILE_SUFFIX);
        if (StringUtils.isEmpty(inputFileSuffix)) {
            inputFileSuffix = getDefaultFileSuffix();
        }

        inputDir = new File(((String) getConfigParameterValue(PARAM_INPUT_DIR)).trim());

        // if input directory does not exist or is not a directory, throw exception
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new ResourceInitializationException(ResourceConfigurationException.DIRECTORY_NOT_FOUND,
                    new Object[]{PARAM_INPUT_DIR, this.getMetaData().getName(), inputDir.getPath()});
        }

        String[] exts = new String[1];
        exts[0] = inputFileSuffix;

        logger.info("Looking for files in " + inputDir + " with recursive set to " + recursive);
        xmiFiles = new ArrayList<>(FileUtils.listFiles(inputDir, exts, recursive));

        if (xmiFiles.size() == 0) {
            logger.warn("The directory " + inputDir.getAbsolutePath()
                    + " does not have any files ending with " + inputFileSuffix);
        }

        inputViewName = (String) getConfigParameterValue(PARAM_INPUT_VIEW_NAME);
        currentDocIndex = 0;
    }

    protected abstract String getDefaultFileSuffix();


    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
     */
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(currentDocIndex, xmiFiles.size(), Progress.ENTITIES)};
    }

}
