/**
 *
 */
package edu.cmu.cs.lti.collection_reader;

import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code refactored from ClearTk for reading Ace2005 corpus documents
 * The engine may have several assumption over the folder hierarchy. The current version
 * assumes input from ACE2005-TrainingData-V6.0 (LDC2005E18).
 * <p/>
 * NOTE: please put apf.v5.1.1.dtd file at the same folder level of apf.xml files
 *
 * @author Zhengzhong Liu, Hector
 */
public class AceDataCollectionReader extends AbstractCollectionReader {

    public final static String PARAM_ACE_ENGLISH_DATA_PATH = "ace_english_data_path";

    public final static String PARAM_ACE_TYPES = "ace_types_to_read";

    public final static String PARAM_GOLD_STANDARD_VIEW_NAME = "goldStandard";

    @ConfigurationParameter(mandatory = true, description = "The path of the directory that contains the ACE data with various formats, typically it would be .../ACE2005/ACE2005-TrainingData-V6.0/English ", name = PARAM_ACE_ENGLISH_DATA_PATH)
    private String aceEnglishDatPath;

    @ConfigurationParameter(mandatory = false, description = "By default we read in all the types, it can be 'nw', 'bn', 'bc','wl','un',cts'", name = PARAM_ACE_TYPES)
    private Set<String> aceTypesToRead;

    private String[] allAceTypes = {"nw", "bn", "bc", "wl", "un", "cts"};

    // this is assumed value for the ACE2005 training data
    public final String inputAceDataStatus = "timex2norm";

    // suffix of the file containing the plain text
    public final String textFileSuffix = ".sgm";

    private Map<String, File[]> aceFilesByType;

    private List<File> aceFilesToRead;

    private int aceFileIndex = 0;

    private File currentSGMFile;

    public static final String TAG_REGEX = "<.*?>";

    Pattern tagPattern;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        File englishDirectory = new File(aceEnglishDatPath);
        try {
            aceFilesByType = getContentDirectoryByType(englishDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (aceTypesToRead == null) {
            aceTypesToRead = new HashSet<String>(Arrays.asList(allAceTypes));
        }

        aceFilesToRead = new ArrayList<File>();
        for (Entry<String, File[]> typeAndFiles : aceFilesByType.entrySet()) {
            aceFilesToRead.addAll(Arrays.asList(typeAndFiles.getValue()));
        }

        System.out.println(aceFilesToRead.size() + " files to read in total");

        tagPattern = Pattern.compile(TAG_REGEX, Pattern.MULTILINE | Pattern.DOTALL);

    }

    /**
     * Get the file paths but organized them by the text type, we assume that all sub-directories
     * under this directory are all corpus documents, and each directory store one type, this is the
     * same with the ACE2005 LDC distribution
     * <p/>
     * In ACE2005 English documents, the types are as followed, see data README for details
     * <p/>
     * - Newswire (NW)
     * - Broadcast News (BN)
     * - Broadcast Conversation (BC)
     * - Weblog (WL)
     * - Usenet Newsgroups/Discussion Forum (UN)
     * - Conversational Telephone Speech (CTS)
     *
     * @param directoryOfLanguage The input directory, at Language level
     */
    private Map<String, File[]> getContentDirectoryByType(File directoryOfLanguage) throws IOException {
        if (!directoryExist(directoryOfLanguage))
            throw new IllegalArgumentException(
                    "Input not a directory (or not exist), cannot get file list from each source : " + directoryOfLanguage.getCanonicalPath());

        Map<String, File[]> contentDirectories = new HashMap<String, File[]>();

        for (File typeDir : directoryOfLanguage.listFiles()) {
            if (typeDir.isDirectory()) {
                String type = typeDir.getName();
                String contentDirPath = typeDir.getAbsolutePath() + "/" + inputAceDataStatus;
                File contentDir = new File(contentDirPath);
                if (directoryExist(contentDir)) {
                    contentDirectories.put(type, contentDir.listFiles());
                }
            }
        }

        return contentDirectories;
    }

    private boolean directoryExist(File dir) {
        return dir.exists() && dir.isDirectory();
    }

    private File getNextSGMFile() {
        if (currentSGMFile != null)
            return currentSGMFile;
        while (aceFileIndex < aceFilesToRead.size()) {
            File sgmFile = aceFilesToRead.get(aceFileIndex++);
            if (sgmFile.getName().endsWith(textFileSuffix)) {
                currentSGMFile = sgmFile;
                return sgmFile;
            }
        }
        return null;
    }

    private File getAPFFile(File sgmFile) {
        String apfFileName = sgmFile.getPath();
        apfFileName = sgmFile.getPath().substring(0, apfFileName.length() - 3) + "apf.xml";
        if (new File(apfFileName).exists())
            return new File(apfFileName);

        apfFileName = sgmFile.getPath();
        apfFileName = sgmFile.getPath().substring(0, apfFileName.length() - 3) + "entities.apf.xml";
        if (new File(apfFileName).exists())
            return new File(apfFileName);

        apfFileName = sgmFile.getPath();
        apfFileName = sgmFile.getPath().substring(0, apfFileName.length() - 3) + "mentions.apf.xml";
        if (new File(apfFileName).exists())
            return new File(apfFileName);

        return null;
    }

    /**
     * It seems that ACE gold standard annotation are corresponding to the raw text without tags
     *
     * @param sgmText
     * @return
     */
    private String getDocumentText(String sgmText) {
        StringBuffer rawDocumentText = new StringBuffer(sgmText);
        Matcher tagMatcher = tagPattern.matcher(rawDocumentText);
        String documentText = tagMatcher.replaceAll("");
        return documentText;
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException {
        try {
            // we need the next sgm file which will typically be 'currentSGMFile' - but we
            // will call getNextSGMFile() to be safe
            File sgmFile = getNextSGMFile();
            // setting currentSGMFile to null tells getNextSGMFile to get the next sgm file
            // rather than simply returning the current value.
            currentSGMFile = null;

            String sgmText = FileUtils.readFileToString(sgmFile);

            // create a view to store golden standard information
            JCas goldStandardView = aJCas.createView(goldStandardViewName);
            String documentText = getDocumentText(sgmText);

            // add document text to both view
            goldStandardView.setDocumentText(documentText);
            aJCas.setDocumentText(documentText);

            SAXBuilder builder = new SAXBuilder();
            builder.setDTDHandler(null);

            // source document information are useful to reach the golden standard file while annotating
            UimaAnnotationUtils.setSourceDocumentInformation(aJCas, sgmFile.toURI().toString(), (int) sgmFile.length(),
                    0, !hasNext());
        } catch (CASException ce) {
            throw new CollectionException(ce);
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(aceFileIndex, aceFilesToRead.size(), Progress.ENTITIES)};
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#hasNext()
     */
    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return getNextSGMFile() != null;
    }

}
