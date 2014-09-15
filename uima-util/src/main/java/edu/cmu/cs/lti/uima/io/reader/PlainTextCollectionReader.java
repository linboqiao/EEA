package edu.cmu.cs.lti.uima.io.reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection reader for text in the IC++ domain.
 * 
 * @author Jun Araki
 */
public class PlainTextCollectionReader extends CollectionReader_ImplBase {

  public static final String PARAM_INPUT_VIEW_NAME = "InputViewName";

  public static final String PARAM_SRC_DOC_INFO_VIEW_NAMES = "SrcDocInfoViewNames";

  public static final String PARAM_INPUTDIR = "InputDirectory";

  public static final String PARAM_ENCODING = "Encoding";

  public static final String PARAM_TEXT_SUFFIX = "TextSuffix";

  private String inputViewName;

  private List<String> srcDocInfoViewNames;

  private ArrayList<File> textFiles;

  private String encoding;

  // private String language;

  private int currentDocIndex;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  public void initialize() throws ResourceInitializationException {
    inputViewName = (String) getConfigParameterValue(PARAM_INPUT_VIEW_NAME);
    srcDocInfoViewNames = Arrays.asList((String[]) getConfigParameterValue(PARAM_SRC_DOC_INFO_VIEW_NAMES));
    File directory = new File((String) getConfigParameterValue(PARAM_INPUTDIR));
    encoding = (String) getConfigParameterValue(PARAM_ENCODING);

    String[] suffix = (String[]) getConfigParameterValue(PARAM_TEXT_SUFFIX);
    currentDocIndex = 0;

    textFiles = new ArrayList<File>();
    File[] files = directory.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (!files[i].isDirectory() && isText(files[i], suffix)) {
        textFiles.add(files[i]);
      }
    }
  }

  private boolean isText(File f, String[] suffix) {
    String filename = f.getPath();
    for (int i = 0; i < suffix.length; i++) {
      if (filename.endsWith(suffix[i])) {
        return true;
      }
    }
    return false;
  }

  public Progress[] getProgress() {
    return new Progress[] { new ProgressImpl(currentDocIndex, textFiles.size(), Progress.ENTITIES) };
  }

  public void getNext(CAS aCAS) throws IOException, CollectionException {
    JCas aJCas = null;
    JCas inputView = null;
    List<JCas> srcDocInfoViews = new ArrayList<JCas>();
    try {
      aJCas = aCAS.getJCas();
      if (!StringUtils.isEmpty(inputViewName)) {
        inputView = ViewCreatorAnnotator.createViewSafely(aJCas, inputViewName);
      }
      if (!CollectionUtils.isEmpty(srcDocInfoViewNames)) {
        for (String srcDocInfoViewName : srcDocInfoViewNames) {
          srcDocInfoViews.add(ViewCreatorAnnotator.createViewSafely(aJCas, srcDocInfoViewName));
        }
        if (!srcDocInfoViewNames.contains(inputViewName)) {
          // The input view should also have source document information.
          srcDocInfoViews.add(inputView);
        }
      }
    } catch (Exception e) {
      throw new CollectionException(e);
    }

    // open input stream to file
    File file = (File) textFiles.get(currentDocIndex++);
    String text = FileUtils.file2String(file, encoding);

    // put document in CAS
    if (inputView != null) {
      // This view is intended to be used in order to put an original document text to a view other
      // than the default view.
      inputView.setDocumentText(text);
    } else {
      aJCas.setDocumentText(text);
    }

    // Also store location of source document in CAS. This information is critical
    // if CAS Consumers will need to know where the original document contents are located.
    // For example, the Semantic Search CAS Indexer writes this information into the
    // search index that it creates, which allows applications that use the search index to
    // locate the documents that satisfy their semantic queries.
    SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(aJCas);
    srcDocInfo.setUri(file.toURI().toURL().toString());
    srcDocInfo.setOffsetInSource(0);
    srcDocInfo.setDocumentSize((int) file.length());
    srcDocInfo.setLastSegment(currentDocIndex == textFiles.size());
    srcDocInfo.addToIndexes();

    for (JCas view : srcDocInfoViews) {
      srcDocInfo = new SourceDocumentInformation(view);
      srcDocInfo.setUri(file.toURI().toURL().toString());
      srcDocInfo.setOffsetInSource(0);
      srcDocInfo.setDocumentSize((int) file.length());
      srcDocInfo.setLastSegment(currentDocIndex == textFiles.size());
      srcDocInfo.addToIndexes();
    }
  }

  public boolean hasNext() {
    return currentDocIndex < textFiles.size();
  }

  public void close() throws IOException {
  }
}
