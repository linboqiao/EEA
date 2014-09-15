package edu.cmu.cs.lti.uima.io.writer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;

public abstract class AbstractStepBasedDirPlainTextWriter extends AbstractStepBasedDirWriter {

  public static final String PARAM_SRC_DOC_INFO_VIEW_NAME = "SourceDocumentInfoViewName";

  @ConfigurationParameter(name = PARAM_SRC_DOC_INFO_VIEW_NAME, mandatory = false)
  /** The view where you extract source document information */
  private String srcDocInfoViewName;

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
  }

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {

    JCas srcDocInfoView = null;
    try {
      if (!StringUtils.isEmpty(srcDocInfoViewName)) {
        srcDocInfoView = aJCas.getView(srcDocInfoViewName);
      } else {
        srcDocInfoView = aJCas;
      }
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    }

    // Retrieve the filename of the input file from the CAS.
    File outputFile = null;

    SourceDocumentInformation fileLoc = JCasUtil.selectSingle(srcDocInfoView,
            SourceDocumentInformation.class);

    File inFile;
    try {
      inFile = new File(new URL(fileLoc.getUri()).getPath());
      StringBuilder buf = new StringBuilder();
      buf.append(inFile.getName());
      if (fileLoc.getOffsetInSource() > 0) {
        buf.append("_" + fileLoc.getOffsetInSource());
      }
      if (StringUtils.isEmpty(outputFileSuffix)) {
        buf.append(".txt");
      } else {
        buf.append(outputFileSuffix);
      }

      String outputFileName = buf.toString();
      outputFile = new File(outputDir, outputFileName);

      String text = getTextToPrint(aJCas);

      FileUtils.writeStringToFile(outputFile, text);
    } catch (MalformedURLException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /**
   * This method is expected to create text to print out against each CAS in a subclass.
   * 
   * @param aJCas
   * @return
   */
  public abstract String getTextToPrint(JCas aJCas);

}