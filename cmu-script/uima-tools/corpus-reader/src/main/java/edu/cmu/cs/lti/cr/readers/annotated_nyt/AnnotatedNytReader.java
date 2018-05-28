package edu.cmu.cs.lti.cr.readers.annotated_nyt;

import com.google.common.collect.ArrayListMultimap;
import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;
import edu.cmu.cs.lti.cr.io.IOHelper;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 8/25/14
 * Time: 4:35 PM
 */
public class AnnotatedNytReader {
    private Iterator<File> nytIter;

    NYTCorpusDocumentParser parser = new NYTCorpusDocumentParser();

    boolean doValidate = false;

    File dataRootDirectory;

    int yearIdx = 0;
    int monthIdx = 0;
    int dayIdx = 0;

    List<File> years;

    List<File> months;

    List<File> days;

    File nytFile;

    NYTCorpusDocument nytDoc;

    public AnnotatedNytReader(File dataRootDirectory) {
        if (!dataRootDirectory.isDirectory()) {
            throw new IllegalArgumentException("Please provide a correct directory to start");
        }
        this.dataRootDirectory = dataRootDirectory;

        for (File yearDir : dataRootDirectory.listFiles()) {
            if (yearDir.isDirectory()) {
                years.add(yearDir);
                for (File monthDir : yearDir.listFiles()) {
                    if (monthDir.isDirectory()) {
                        months.add(monthDir);
                        for (File dayDir : monthDir.listFiles()) {
                            if (dayDir.isDirectory()) {
                                days.add(dayDir);
                            }
                        }
                    }
                }
            }
        }
    }

    public void reset(){
        yearIdx = 0;
         monthIdx = 0;
         dayIdx = 0;
    }

    public void resetYear(){
        yearIdx = 0;
    }

    public void resetMonth(){
        monthIdx = 0;
    }

    public void resetDay(){
        dayIdx = 0;
    }

    public void readAllDocuments() {
        scanDocuments(dataRootDirectory);
    }

    public boolean hasNextYear() {
        return yearIdx < years.size() - 1;
    }

    public void readNextYear() {
        scanDocuments(years.get(yearIdx));
        yearIdx ++;
    }

    public boolean hasNextMonth() {
        return monthIdx < months.size() - 1;
    }

    public void readNextMonth() {
        scanDocuments(months.get(monthIdx));
        monthIdx ++;
    }

    public boolean hasNextDay() {
        return dayIdx < days.size() - 1;
    }

    public void readNextDay() {
        scanDocuments(days.get(dayIdx));
        dayIdx ++;
    }

    public void readAll(){
        scanDocuments(dataRootDirectory);
    }

    public void scanDocuments(File dataDirectory) {
        if (!dataDirectory.isDirectory()) {
            throw new IllegalArgumentException("Please provide a correct directory to start");
        }
        Collection<File> nytFiles = FileUtils.listFiles(dataDirectory, new SuffixFileFilter(".xml"), TrueFileFilter.INSTANCE);
        nytIter = nytFiles.iterator();
    }

    public boolean hasNextDocument() {
        return nytIter.hasNext();
    }

    public NYTCorpusDocument getNextDocument() {
        if (hasNextDocument()) {
            nytFile = nytIter.next();
            nytDoc = parser.parseNYTCorpusDocumentFromFile(nytFile, doValidate);
            return nytDoc;
        }else{
            return null;
        }
    }

    public void dumpDocumentByDate(File outDir) throws IOException{
        int month  = nytDoc.getPublicationMonth();
        int year = nytDoc.getPublicationYear();
        int day = nytDoc.getPublicationDayOfMonth();

        File outputFile = new File(String.format("%s/%d/%d/%d/%s",outDir, year, month, day , nytFile.getName()));
        System.out.println("Dumping " + nytFile.getName());
        IOHelper.writeFile(outputFile, FileUtils.readFileToString(nytFile));

    }

    public void dumpDocument(File outDir) throws IOException {
        File outputFile = new File(outDir + "/" + nytFile.getName());
        System.out.println("Dumping " + nytFile.getName());
        IOHelper.writeFile(outputFile, FileUtils.readFileToString(nytFile));
    }

    public void printNytFile(File nytXmlFile) throws IOException {
        NYTCorpusDocument nytDoc = parser.parseNYTCorpusDocumentFromFile(nytXmlFile, doValidate);
        System.out.println("Head line");
        System.out.println(nytDoc.getHeadline());
        System.out.println("Body");
        System.out.println(nytDoc.getBody());
        System.out.println(nytDoc.getPublicationDate());
        System.out.println(nytDoc.getDescriptors());
        System.out.println(nytDoc.getTaxonomicClassifiers());
    }


    public static void main(String[] args) throws IOException, ClassNotFoundException {
        AnnotatedNytReader reader = new AnnotatedNytReader(new File("data"));
        String serializedClassifier = "classifiers/english.muc.7class.distsim.crf.ser.gz";

        AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier(serializedClassifier);

        ArrayListMultimap<Date, List<String>> time2EntityLists = ArrayListMultimap.create();

        while (reader.hasNextDocument()) {
            NYTCorpusDocument doc = reader.getNextDocument();
            List<String> taxo = doc.getTaxonomicClassifiers();
            List<String> descriptors = doc.getTaxonomicClassifiers();
            String body = doc.getBody();

            String entity = "";
            String lastLabel = "";

            List<String> entities = new ArrayList<String>();

            List<List<CoreLabel>> res = classifier.classify(body);
            for (List<CoreLabel> sent : res) {
                for (CoreLabel word : sent) {
                    String thisLabel = word.get(CoreAnnotations.AnswerAnnotation.class);
                    if (!thisLabel.equals("O")) {
                        if (lastLabel.equals(thisLabel)) {
                            entity += " " + word.word();
                        } else {
                            if (lastLabel.equals("PERSON") || lastLabel.equals("ORGANIZATION")) {
                                System.out.println(entity);
                            }
                            entities.add(entity);
                            entity = word.word();

                        }
                    }
                    lastLabel = thisLabel;
                }
            }
        }
    }
}
