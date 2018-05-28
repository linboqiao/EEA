package edu.cmu.cs.lti.uima.io.writer;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * Provide an function to implemented, which allow customizing what text to be printed out
 *
 * @author Zhengzhong Liu, Hector
 */
public abstract class AbstractSimpleTextWriterAnalysisEngine extends AbstractLoggingAnnotator {

    public static final String PARAM_OUTPUT_PATH = "outputPath";

    @ConfigurationParameter(name = PARAM_OUTPUT_PATH, mandatory = true)
    private String outputPath;

    private File outputFile;


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        outputFile = new File(outputPath);

        File parentDir = outputFile.getAbsoluteFile().getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        try {
            FileUtils.write(outputFile, "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String text = getTextToPrint(aJCas);
        if (text != null) {
            try {
                FileUtils.write(outputFile, text, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public abstract String getTextToPrint(JCas aJCas);


}