package edu.cmu.cs.lti.uima.io.reader;

import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.util.NoiseTextFormatter;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A collection reader for plain text documents.
 *
 * @author Jun Araki
 * @author zhengzhongliu
 */
public class PlainTextCollectionReader extends AbstractCollectionReader {

    public static final String PARAM_INPUT_VIEW_NAME = "InputViewName";

    public static final String PARAM_INPUTDIR = "InputDirectory";

    public static final String PARAM_TEXT_SUFFIX = "TextSuffix";

    public static final String PARAM_DO_NOISE_FILTER = "NoiseFilter";

    @ConfigurationParameter(name = PARAM_INPUTDIR)
    private String inputDirPath;

    @ConfigurationParameter(name = PARAM_TEXT_SUFFIX, mandatory = false)
    String[] suffix;

    @ConfigurationParameter(name = PARAM_DO_NOISE_FILTER, defaultValue = "false")
    boolean doNoiseFilter;

    private ArrayList<File> textFiles;

    private int currentDocIndex;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public void initialize(UimaContext context) throws ResourceInitializationException {
        File directory = new File(inputDirPath);
        if (!directory.exists() || directory.isFile()) {
            logger.error("Cannot find directory at : " + inputDirPath);
        }

        if (suffix == null) {
            suffix = new String[]{""};
        }

        currentDocIndex = 0;
        textFiles = new ArrayList<>();
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].isDirectory() && isText(files[i], suffix)) {
                textFiles.add(files[i]);
            }
        }
    }

    private boolean isText(File f, String[] suffix) {
        String filename = f.getPath();
        for (String aSuffix : suffix) {
            if (filename.endsWith(aSuffix)) {
                return true;
            }
        }
        return false;
    }

    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(currentDocIndex, textFiles.size(), Progress.ENTITIES)};
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException {
        JCas inputView = null;
        try {
            if (!StringUtils.isEmpty(inputViewName)) {
                inputView = ViewCreatorAnnotator.createViewSafely(aJCas, inputViewName);
            }
        } catch (Exception e) {
            throw new CollectionException(e);
        }

        // open input stream to file
        File file = (File) textFiles.get(currentDocIndex++);
        String text = FileUtils.file2String(file, encoding);

        if (doNoiseFilter) {
            text = new NoiseTextFormatter(text).cleanForum().cleanNews().multiNewLineBreaker().getText();
        }

        // put document in CAS
        if (inputView != null) {
            // This view is intended to be used in order to put an original document text to a view other
            // than the default view.
            inputView.setDocumentText(text);
        }

        aJCas.setDocumentText(text);

        Article article = new Article(aJCas);
        UimaAnnotationUtils.finishAnnotation(article, 0, text.length(), COMPONENT_ID, 0, aJCas);
        article.setArticleName(FilenameUtils.getBaseName(file.getName()));
        article.setLanguage("en");

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
    }


    public boolean hasNext() {
        return currentDocIndex < textFiles.size();
    }

    public void close() throws IOException {
    }
}
