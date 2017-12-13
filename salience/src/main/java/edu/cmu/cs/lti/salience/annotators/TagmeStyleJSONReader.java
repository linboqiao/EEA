package edu.cmu.cs.lti.annotators;

import com.google.common.base.Joiner;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.GroundedEntity;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static edu.cmu.cs.lti.uima.util.CasSerialization.cleanText;

public class TagmeStyleJSONReader extends JCasCollectionReader_ImplBase {
    public static final String SALIENCE_VIEW_NAME = "abstract";

    public static final String PARAM_INPUT_JSON = "inputJson";
    @ConfigurationParameter(name = PARAM_INPUT_JSON)
    private File inputJSon;

    public static final String PARAM_TEXT_FIELDS = "textFields";
    @ConfigurationParameter(name = PARAM_TEXT_FIELDS)
    private List<String> textFields;

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
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        JsonObject jsonObj = new JsonParser().parse(nextLine).getAsJsonObject();
        String docid = jsonObj.get("docno").getAsString();

        List<String> allText = new ArrayList<>();
        JsonObject allSpots = jsonObj.get("spot").getAsJsonObject();

        int offset = 0;
        for (String textField : textFields) {
            String text = cleanText(jsonObj.get(textField).getAsString());
            JsonArray spots = allSpots.get(textField).getAsJsonArray();
            allText.add(text);
            offset += text.length() + 1;
            addSpots(jCas, spots, offset);
        }

        String documentText = cleanText(Joiner.on("\n").join(allText));
        jCas.setDocumentText(documentText);

        JCas salienceView = JCasUtil.getView(jCas, SALIENCE_VIEW_NAME, true);

        String salienceText = cleanText(jsonObj.get(abstractField).getAsString());
        salienceView.setDocumentText(salienceText);

        JsonArray salienceSpots = allSpots.get(abstractField).getAsJsonArray();
        addSpots(salienceView, salienceSpots, 0);

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
    }

    private void addSpots(JCas aJCas, JsonArray spots, int offset) {
        for (int i = 0; i < spots.size(); i++) {
            JsonElement spot = spots.get(i);
            JsonObject spotObj = spot.getAsJsonObject();
            JsonArray locArray = spotObj.get("loc").getAsJsonArray();
            int charBegin = locArray.get(0).getAsInt() + offset;
            int charEnd = locArray.get(1).getAsInt() + offset;
            GroundedEntity entity = new GroundedEntity(aJCas, charBegin, charEnd);
            UimaAnnotationUtils.finishAnnotation(entity, COMPONENT_ID, i, aJCas);
        }
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
}