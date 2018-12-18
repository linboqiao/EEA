package edu.cmu.cs.lti.salience.annotators;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.salience.utils.SalienceUtils;
import edu.cmu.cs.lti.script.type.GroundedEntity;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import static edu.cmu.cs.lti.collection_reader.AnnotatedNytReader.ABSTRACT_VIEW_NAME;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/28/17
 * Time: 3:49 PM
 *
 * @author Zhengzhong Liu
 */
public class SupportSentenceWriter extends AbstractLoggingAnnotator {

    public static final String PARAM_OUTPUT_FILE = "outputFile";

    @ConfigurationParameter(name = PARAM_OUTPUT_FILE)
    private File outputFile;

    public static final String PARAM_TRAIN_SPLIT = "trainSplit";
    @ConfigurationParameter(name = PARAM_TRAIN_SPLIT)
    private File trainSplitFile;


    private BufferedWriter writer;
    private Set<String> trainDocs;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        try {
            trainDocs = SalienceUtils.readSplit(trainSplitFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            File parent = outputFile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }

            GZIPOutputStream gzipOut = new GZIPOutputStream(new FileOutputStream(outputFile));
            writer = new BufferedWriter(new OutputStreamWriter(gzipOut, "UTF-8"));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            writer.close();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private List<Span> heuristicSentSplit(String documentText) {
        List<Span> spans = new ArrayList<>();
        int begin = 0;
        int end;
        int i = documentText.indexOf(';');
        while (i >= 0) {
            end = i;
            spans.add(Span.of(begin, end + 1));
            begin = i + 1;
            i = documentText.indexOf(';', begin);
        }
        end = documentText.length();
        spans.add(Span.of(begin, end));
        return spans;
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas abstractView = JCasUtil.getView(aJCas, ABSTRACT_VIEW_NAME, false);

        String docid = UimaConvenience.getArticleName(aJCas);

        if (!trainDocs.contains(docid)) {
            return;
        }

        JsonObject doc = new JsonObject();
        doc.addProperty("docid", docid);

        SetMultimap<String, String> supportSentences = HashMultimap.create();

        String text = abstractView.getDocumentText();
        for (Span span : heuristicSentSplit(text)) {
            String sentText = text.substring(span.getBegin(), span.getEnd()).replaceAll("\n", " ");
            for (GroundedEntity groundedEntity : JCasUtil.selectCovered(abstractView,
                    GroundedEntity.class, span.getBegin(), span.getEnd())) {
                supportSentences.put(groundedEntity.getKnowledgeBaseId(), sentText);
            }
        }

        JsonArray allSupports = new JsonArray();

        for (Map.Entry<String, Collection<String>> entitySupports : supportSentences.asMap().entrySet()) {
            JsonObject support = new JsonObject();

            support.addProperty("id", entitySupports.getKey());

            JsonArray supportSents = new JsonArray();
            for (String sent : entitySupports.getValue()) {
                supportSents.add(sent);
            }
            support.add("sentences", supportSents);
            allSupports.add(support);
        }

        doc.add("supports", allSupports);
        String jsonStr = doc.toString() + "\n";

        try {
            writer.write(jsonStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String trainingSplitFile = argv[1];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, "tagged",
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );

        AnalysisEngineDescription generator = AnalysisEngineFactory.createEngineDescription(
                SupportSentenceWriter.class, typeSystemDescription,
                SupportSentenceWriter.PARAM_OUTPUT_FILE,
                FileUtils.joinPaths(workingDir, "supports", "abstract_support.json.gz"),
                SupportSentenceWriter.PARAM_TRAIN_SPLIT, trainingSplitFile
        );

        new BasicPipeline(reader, true, true, 7, generator).run();
    }
}
