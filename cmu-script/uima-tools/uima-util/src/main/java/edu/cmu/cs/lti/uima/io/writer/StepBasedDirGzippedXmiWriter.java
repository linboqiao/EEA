package edu.cmu.cs.lti.uima.io.writer;

import edu.cmu.cs.lti.uima.util.CasSerialization;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This analysis engine outputs gzipped CAS in the XMI format.
 *
 * @author Jun Araki
 */
public class StepBasedDirGzippedXmiWriter extends AbstractStepBasedDirWriter {
    public static final String PARAM_OUTPUT_FILE_NUMBERS = "OutputFileNumbers";

    private static final String DEFAULT_FILE_SUFFIX = ".xmi.gz";

    @ConfigurationParameter(name = PARAM_OUTPUT_FILE_NUMBERS, mandatory = false)
    /**
     * This is a list of documents that you want to generate XMI output. If it is
     * null or empty, the writer works against all input.
     */
    private List<String> outputDocumentNumberList;

    private int docCounter;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        docCounter = 0;
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        if (!CollectionUtils.isEmpty(outputDocumentNumberList)) {
            if (!outputDocumentNumberList.contains(Integer.toString(docCounter))) {
                return;
            }
        }

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

        // serialize XCAS and write to output file
        try {
            CasSerialization.writeAsGzip(aJCas.getCas(), outputFile);
        } catch (IOException | SAXException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }
}
