package edu.cmu.cs.lti.uima.io.writer;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * Provide an funciton to implemented, which allow customizing what text to be printed out
 *
 * @author Zhengzhong Liu, Hector
 */
public abstract class AbstractOnCompletionTextWriterAnalysisEngine extends AbstractLoggingAnnotator {

    public static final String PARAM_OUTPUT_PATH = "outputPath";

    @ConfigurationParameter(name = PARAM_OUTPUT_PATH, mandatory = true)
    private String outputPath;

    private File outputFile;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        outputFile = new File(outputPath);

        File parentDir = outputFile.getParentFile();

        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    public abstract String getTextToPrint();

    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        String text = getTextToPrint();

        try {
            FileUtils.write(outputFile, text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}