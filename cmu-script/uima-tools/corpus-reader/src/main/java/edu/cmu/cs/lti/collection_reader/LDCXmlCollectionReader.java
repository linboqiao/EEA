package edu.cmu.cs.lti.collection_reader;

import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.NoiseTextFormatter;
import edu.cmu.cs.lti.utils.XMLUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Read the LDC XML format files. Each file is simply taken as it is, retaining all original XML offsets.
 *
 * @author Zhengzhong Liu
 */
public class LDCXmlCollectionReader extends AbstractCollectionReader {
    public static final String PARAM_DATA_PATH = "dataPath";

    public static final String PARAM_INPUT_VIEW_NAME = "inputViewName";

    @ConfigurationParameter(name = PARAM_DATA_PATH)
    private String dataPath;

    public static final String COMPONENT_ID = LDCXmlCollectionReader.class.getSimpleName();

    private List<File> files;
    private int fileIndex;


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        logger.info("Reading from : " + dataPath);
        files = new ArrayList<>(FileUtils.listFiles(new File(dataPath), new String[]{"xml"}, true));
        fileIndex = 0;
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        try {
            // Next file to be processed.
            File f = files.get(fileIndex);

            String rawText = FileUtils.readFileToString(f);

            // Set the input view.
            JCas inputView = ViewCreatorAnnotator.createViewSafely(jCas, inputViewName);
            inputView.setDocumentText(rawText);

            // Create a view to store golden standard information
            JCas goldStandardView = jCas.createView(goldStandardViewName);

            // Add document text to both view
            String documentText = getDocumentText(rawText);
            goldStandardView.setDocumentText(documentText);
            jCas.setDocumentText(documentText);

            // source document information are useful to reach the golden standard file while annotating
            UimaAnnotationUtils.setSourceDocumentInformation(jCas, f.toURI().toString(), (int) f.length(), 0, true);

            Article article = new Article(jCas);
            UimaAnnotationUtils.finishAnnotation(article, 0, documentText.length(), COMPONENT_ID, 0, jCas);
            article.setArticleName(StringUtils.removeEnd(f.getName(), ".xml"));
            article.setLanguage("en");
        } catch (CASException | AnalysisEngineProcessException | XMLStreamException e) {
            e.printStackTrace();
        }
        fileIndex++;
    }

    private String getDocumentText(String xmlText) throws XMLStreamException {
        return new NoiseTextFormatter(XMLUtils.parseXMLTextWithOffsets(xmlText, true)).multiNewLineBreaker().getText();
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return fileIndex < files.size();
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(fileIndex, files.size(), Progress.ENTITIES)};
    }

    public static void main(String[] args) throws UIMAException {
        System.out.println("Testing the LDC XML reader ...");

        String inputPath =
                "data/event_mention_detection/input" +
                        "/LDC2015E77_TAC_KBP_2015_English_Cold_Start_Evaluation_Source_Corpus_V2.0/data/";

        // Parameters for the writer
        String paramParentOutputDir = "data/event_mention_detection/output/LDC2015E77";
        String paramBaseOutputDirName = "plain";
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                LDCXmlCollectionReader.class, typeSystemDescription,
                LDCXmlCollectionReader.PARAM_DATA_PATH, inputPath
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, 0);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, writer);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
