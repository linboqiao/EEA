package edu.cmu.cs.lti.salience.annotators;

import com.google.common.base.Joiner;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.Body;
import edu.cmu.cs.lti.script.type.GroundedEntity;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.DebugUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static edu.cmu.cs.lti.uima.util.CasSerialization.cleanText;

public class TagmeStyleJSONReader extends JCasCollectionReader_ImplBase {
    public static final String SALIENCE_VIEW_NAME = "abstract";

    public static final String PARAM_INPUT_JSON = "inputJson";
    @ConfigurationParameter(name = PARAM_INPUT_JSON)
    private File inputJSon;

    public static final String PARAM_HEADER_FIELDS = "headerFields";
    @ConfigurationParameter(name = PARAM_HEADER_FIELDS, mandatory = false)
    private List<String> headerFields;

    public static final String PARAM_BODY_FIELDS = "textFields";
    @ConfigurationParameter(name = PARAM_BODY_FIELDS)
    private List<String> bodyFields;

    public static final String PARAM_ABSTRACT_FIELD = "abstractFields";
    @ConfigurationParameter(name = PARAM_ABSTRACT_FIELD)
    private String abstractField;

    public static final String PARAM_LANGUAGE = "language";
    @ConfigurationParameter(name = PARAM_LANGUAGE, defaultValue = "en")
    protected String language;

    private BufferedReader reader;

    private String nextLine;

    private int lineNumber;

    public final String COMPONENT_ID = this.getClass().getSimpleName();

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputJSon)));
        } catch (FileNotFoundException e) {
            throw new ResourceInitializationException(e);
        }

        if (headerFields == null) {
            headerFields = new ArrayList<>();
        }
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        JsonObject docInfo = new JsonParser().parse(nextLine).getAsJsonObject();
        String docid = docInfo.get("docno").getAsString();

        UimaConvenience.printProcessLog(jCas);

        List<String> allText = new ArrayList<>();
        JsonObject allSpots = docInfo.get("spot").getAsJsonObject();

        int offset = 0;
        for (String headerField : headerFields) {
            String text = addTokenFormatFields(jCas, docInfo, allSpots, headerField, offset);
            allText.add(text);
            offset += text.split(" ").length + 1;
        }

        int bodyStart = offset;
        for (String textField : bodyFields) {
            String text = addTokenFormatFields(jCas, docInfo, allSpots, textField, offset);
            allText.add(text);
            offset += text.split(" ").length + 1;
        }
        int bodyEnd = offset;

        Body body = new Body(jCas, bodyStart, bodyEnd);
        UimaAnnotationUtils.finishAnnotation(body, COMPONENT_ID, 0, jCas);

        String documentText = Joiner.on("\n").join(allText);
        jCas.setDocumentText(documentText);

        JCas salienceView = JCasUtil.getView(jCas, SALIENCE_VIEW_NAME, true);

        String salienceText = addTokenFormatFields(salienceView, docInfo, allSpots, abstractField, 0);
        salienceView.setDocumentText(salienceText);

//        for (GroundedEntity entity : JCasUtil.select(jCas, GroundedEntity.class)) {
//            System.out.println(String.format("Entity %s [%d:%d], %s", entity.getCoveredText(), entity.getBegin(),
//                    entity.getEnd(), entity.getKnowledgeBaseId()));
//        }
//
//        for (GroundedEntity entity : JCasUtil.select(salienceView, GroundedEntity.class)) {
//            System.out.println(String.format("Entity %s [%d:%d], %s", entity.getCoveredText(), entity.getBegin(),
//                    entity.getEnd(), entity.getKnowledgeBaseId()));
//        }
//
//        DebugUtils.pause();

        Article article = new Article(jCas);
        UimaAnnotationUtils.finishAnnotation(article, 0, documentText.length(), COMPONENT_ID, 0, jCas);
        article.setArticleName(FilenameUtils.getBaseName(docid));
        article.setLanguage(language);

        // Also store location of source document in CAS. This information is critical
        // if CAS Consumers will need to know where the original document contents are located.
        // For example, the Semantic Search CAS Indexer writes this information into the
        // search index that it creates, which allows applications that use the search index to
        // locate the documents that satisfy their semantic queries.
        SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(jCas);
        srcDocInfo.setUri(inputJSon.toURI().toURL().toString());
        srcDocInfo.setOffsetInSource(lineNumber);
        srcDocInfo.setDocumentSize((int) inputJSon.length());
        srcDocInfo.setLastSegment(false);
        srcDocInfo.addToIndexes();

        lineNumber++;

        DebugUtils.pause();
    }

    private String addCharFormatFields(JCas jCas, JsonObject docInfo, JsonObject allSpots, String field, int offset) {
        String text = Joiner.on(" ").join(cleanText(docInfo.get(field).getAsString()).split("\\s+"));
        JsonArray spots = allSpots.get(field).getAsJsonArray();
        addSpots(jCas, text, spots, false, offset);
        return text;
    }

    private String addTokenFormatFields(JCas jCas, JsonObject docInfo, JsonObject allSpots, String field, int offset) {
        String text = Joiner.on(" ").join(cleanText(docInfo.get(field).getAsString()).split("\\s+"));
        JsonArray spots = allSpots.get(field).getAsJsonArray();
        addSpots(jCas, text, spots, true, offset);
        return text;
    }

    private void addSpots(JCas aJCas, String text, JsonArray spots, boolean tokenFormat, int offset) {
        for (int i = 0; i < spots.size(); i++) {
            JsonElement spot = spots.get(i);
            JsonObject spotObj = spot.getAsJsonObject();

            Span entitySpan;

            if (tokenFormat) {
                entitySpan = fromWordSpan(text, spotObj, offset);
            } else {
                entitySpan = fromCharacterSpan(spotObj, offset);
            }

            JsonObject spotEntity = spotObj.get("entities").getAsJsonArray().getAsJsonArray().get(0).getAsJsonObject();
            String eid = spotEntity.get("id").getAsString();
            double score = spotEntity.get("score").getAsDouble();

            String wikiname = spotObj.get("wiki_name").getAsString();

            StringArray kbNames = new StringArray(aJCas, 2);
            StringArray kbValues = new StringArray(aJCas, 2);

            kbNames.set(0, "freebase");
            kbValues.set(0, eid);

            kbNames.set(1, "wikipedia");
            kbValues.set(1, wikiname);

            GroundedEntity entity = new GroundedEntity(aJCas, entitySpan.getBegin(), entitySpan.getEnd());
            entity.setKnowledgeBaseNames(kbNames);
            entity.setKnowledgeBaseValues(kbValues);
            entity.setConfidence(score);
            entity.setKnowledgeBaseId(eid);

            UimaAnnotationUtils.finishAnnotation(entity, COMPONENT_ID, i, aJCas);
        }
    }

    private Span fromCharacterSpan(JsonObject spotObj, int offset) {
        JsonArray locArray = spotObj.get("loc").getAsJsonArray();
        int charBegin = locArray.get(0).getAsInt() + offset;
        int charEnd = locArray.get(1).getAsInt() + offset;
        return Span.of(charBegin, charEnd);
    }

    private Span fromWordSpan(String text, JsonObject spotObj, int tokenOffset) {
        String[] tokens = text.split(" ");
        int[][] spans = new int[tokens.length][2];

        int begin = 0;
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            spans[i][0] = begin;
            spans[i][1] = begin + token.length();
            begin += token.length() + 1;
        }

        JsonArray locArray = spotObj.get("loc").getAsJsonArray();
        int tokenBegin = locArray.get(0).getAsInt() + tokenOffset;
        int tokenEnd = locArray.get(1).getAsInt() - 1 + tokenOffset;

        return Span.of(spans[tokenBegin][0], spans[tokenEnd][1]);
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return (nextLine = reader.readLine()) != null;
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[0];
    }

    @Override
    public void close() throws IOException {
        super.close();
        reader.close();
    }

    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String inputJson = argv[1];
        String xmiOutput = argv[2];

        // This reader can read Semantic scholar data.
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TagmeStyleJSONReader.class, typeSystemDescription,
                TagmeStyleJSONReader.PARAM_INPUT_JSON, inputJson,
                TagmeStyleJSONReader.PARAM_ABSTRACT_FIELD, "title",
                TagmeStyleJSONReader.PARAM_BODY_FIELDS, new String[]{"paperAbstract"}
        );

        new BasicPipeline(reader, true, true, 7, workingDir, xmiOutput, true).run();
    }
}