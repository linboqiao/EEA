/**
 *
 */
package edu.cmu.cs.lti.uima.io.writer;

import au.com.bytecode.opencsv.CSVWriter;
import edu.cmu.cs.lti.uima.util.CasSerialization;
import edu.cmu.cs.lti.uima.util.CsvFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author Zhengzhong Liu, Hector
 */
public abstract class AbstractCsvWriterAnalysisEngine extends AbstractStepBasedDirWriter {
    private File outputDir;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static String DEFAULT_FILE_SUFFIX = ".csv";

    private char separator = ',';

    private int docCounter;


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        try {
            super.initialize(context);
        } catch (ResourceInitializationException e) {
            throw new ResourceInitializationException(e);
        }

        docCounter = 0;
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas srcDocInfoView = JCasUtil.getView(aJCas, srcDocInfoViewName, aJCas);
        prepare(aJCas);

        // Retrieve the filename of the input file from the CAS.
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

        CSVWriter writer = CsvFactory.getCSVWriter(outputFile, separator);
        String[] header = getHeader();
        if (header != null) {
            writer.writeNext(header);
        }

        while (hasNextRow()) {
            writer.writeNext(getNextCsvRow());
        }
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract String[] getHeader();

    protected abstract void prepare(JCas aJCas);

    protected abstract boolean hasNextRow();

    protected abstract String[] getNextCsvRow();

    protected void setSeparator(char sep) {
        separator = sep;
    }
}
