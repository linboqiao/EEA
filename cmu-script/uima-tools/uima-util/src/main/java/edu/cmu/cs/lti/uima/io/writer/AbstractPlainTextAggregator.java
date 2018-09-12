package edu.cmu.cs.lti.uima.io.writer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * This plain text writer generates some text-based output at the end of its process (i.e., via the
 * collectionProcessComplete() method). You can use this writer to output a certain kind of summary
 * or statistics over all input.
 * 
 * @author Jun Araki
 */
public abstract class AbstractPlainTextAggregator extends JCasAnnotator_ImplBase {

  public static final String PARAM_OUTPUT_FILE_PATH = "OutputFilePath";

  @ConfigurationParameter(name = PARAM_OUTPUT_FILE_PATH, mandatory = true)
  private String outputFilePath;

  private File outputFile;

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);

    outputFile = new File(outputFilePath);

    subInitialize(context);
  }

  @Override
  public void collectionProcessComplete() {
    String text = getAggregatedTextToPrint();
    try {
      FileUtils.write(outputFile, text);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public abstract String getAggregatedTextToPrint();

  /**
   * Subclass can implement this to get more things to done
   * 
   * @param context
   */
  public abstract void subInitialize(UimaContext context);

}