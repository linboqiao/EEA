package edu.cmu.cs.lti.uima.io.writer;

import edu.cmu.cs.lti.uima.util.CasSerialization;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * Provide an function to implemented, which allow customizing what text to be printed out
 *
 * @author Zhengzhong Liu, Hector
 */
public abstract class AbstractCustomizedTextWriterAnalysisEngine extends AbstractStepBasedDirWriter {
    public static final String DEFAULT_FILE_SUFFIX = ".txt";

    private int docCounter;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        docCounter = 0;
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas srcDocInfoView = JCasUtil.getView(aJCas, srcDocInfoViewName, aJCas);

        if (StringUtils.isEmpty(outputFileSuffix)) {
            outputFileSuffix = DEFAULT_FILE_SUFFIX;
        }
        String outputFileName = CasSerialization.getOutputFileName(srcDocInfoView, outputFileSuffix);


        File outputFile;
        if (outputFileName == null) {
            outputFile = new File(outputDir, "doc" + (docCounter++) + DEFAULT_FILE_SUFFIX);
        } else {
            outputFile = new File(outputDir, outputFileName);
        }

        String text = getTextToPrint(aJCas);

        try {
            FileUtils.writeStringToFile(outputFile, text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public abstract String getTextToPrint(JCas aJCas);
}