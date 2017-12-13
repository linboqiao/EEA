package edu.cmu.cs.lti.annotators;

import com.google.common.collect.Lists;
import com.google.gson.*;
import edu.cmu.cs.lti.script.type.GroundedEntity;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/15/17
 * Time: 4:56 PM
 *
 * @author Zhengzhong Liu
 */
public class TagmeEntityLinkerResultAnnotator extends AbstractLoggingAnnotator {
    public static final String PARAM_ENTITY_RESULT_FOLDER = "entityResultFolder";
    @ConfigurationParameter(name = PARAM_ENTITY_RESULT_FOLDER)
    private File linkerResultFolder;

    public static final String PARAM_USE_TOKEN = "useToken";
    @ConfigurationParameter(name = PARAM_USE_TOKEN)
    private boolean useToken;

    public static final String PARAM_ADDITIONAL_VIEW = "additionalViews";
    @ConfigurationParameter(name = PARAM_ADDITIONAL_VIEW)
    private List<String> additionalViews;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    class EntityAnnotation {
        String entityId;
        String wikiname;
        int begin;
        int end;
        int offset;
        double score;

        public EntityAnnotation(String entityId, String wikiname, int begin, int end, int offset, double score) {
            this.entityId = entityId;
            this.begin = begin;
            this.end = end;
            this.offset = offset;
            this.wikiname = wikiname;
            this.score = score;
        }

        private int getBegin() {
            return begin + offset;
        }

        private int getEnd() {
            return end + offset;
        }
    }

    private JsonObject parseResultFile(File linkerResultFile) throws IOException {
        String jsonStr = FileUtils.readFileToString(linkerResultFile);
        Gson gson = new Gson();
        gson.toJson(jsonStr);
        JsonParser parser = new JsonParser();
        JsonObject document = parser.parse(jsonStr).getAsJsonObject();
        return document;
    }

    private int countTokensInJson(JsonObject document,
                                  List<String> fields) {
        int count = 0;
        for (String field : fields) {
            String text = document.get(field).getAsString();
            if (text.equals("N/A")) {
                return 0;
            }
            count += text.split(" ").length;
        }
        return count;
    }

    private int[] loadTokenMapping(int spaceTokenSize, List<StanfordCorenlpToken> tokens) {
        int[] tokenIndexMap = new int[spaceTokenSize];

        int tokenId = 0;
        int spaceTokenId = 0;
        for (StanfordCorenlpToken token : tokens) {
            int size = token.getCoveredText().split(" ").length;
            for (int i = 0; i < size; i++) {
                tokenIndexMap[spaceTokenId] = tokenId;
                spaceTokenId++;
            }
            tokenId++;
        }

        for (int i = spaceTokenId; i < tokenIndexMap.length; i++) {
            tokenIndexMap[i] = -1;
        }
        return tokenIndexMap;
    }

    private List<EntityAnnotation> loadResults(JsonObject document,
                                               List<String> fields,
                                               boolean useToken) throws IOException {
        List<EntityAnnotation> annotations = new ArrayList<>();
        JsonObject allSpots = document.get("spot").getAsJsonObject();

        int offset = 0;
        for (String field : fields) {
            JsonArray spots = allSpots.get(field).getAsJsonArray();
            for (JsonElement spot : spots) {
                annotations.add(addSpot(spot, offset));
            }
            String text = document.get(field).getAsString();
            offset += useToken ? text.split(" ").length : text.length() + 1;
        }

        return annotations;
    }

    private EntityAnnotation addSpot(JsonElement spot, int offset) {
        JsonObject spotObj = spot.getAsJsonObject();
        JsonArray locs = spotObj.get("loc").getAsJsonArray();
        int begin = locs.get(0).getAsInt();
        int end = locs.get(1).getAsInt();
        String wikiname = spotObj.get("wiki_name").getAsString();
        JsonObject linkedResult = spotObj.get("entities").getAsJsonArray().get(0).getAsJsonObject();
        String entityId = linkedResult.get("id").getAsString();
        double score = linkedResult.get("score").getAsDouble();
        return new EntityAnnotation(entityId, wikiname, begin, end, offset, score);
    }

    private int setAnnotationsFromToken(JCas aJCas, List<EntityAnnotation> annotations,
                                        List<StanfordCorenlpToken> tokens, int[] tokenMapping) {
        int numAdded = 0;
        for (EntityAnnotation entityAnnotation : annotations) {
            int uimaTokenBegin = tokenMapping[entityAnnotation.getBegin()];
            int uimaTokenEnd = tokenMapping[entityAnnotation.getEnd() - 1];

            if (uimaTokenBegin >= 0 && uimaTokenEnd >= 0) {
                int begin = tokens.get(uimaTokenBegin).getBegin();
                int end = tokens.get(uimaTokenEnd).getEnd();
                setAnnotations(aJCas, entityAnnotation, begin, end);
                numAdded++;
            } else {
                logger.info(String.format("Missing annotation %s at token span [%d:%d], calculated span is [%d:%d].",
                        entityAnnotation.entityId, entityAnnotation.getBegin(), entityAnnotation.getEnd(),
                        uimaTokenBegin, uimaTokenEnd));
            }
        }
        return numAdded;
    }

    private void setAnnotationsFromCharacters(JCas aJCas, List<EntityAnnotation> annotations) {
        for (EntityAnnotation entityAnnotation : annotations) {
            if (entityAnnotation.getEnd() < aJCas.getDocumentText().length()) {
                int begin = entityAnnotation.getBegin();
                int end = entityAnnotation.getEnd();
                setAnnotations(aJCas, entityAnnotation, begin, end);
            }
        }
    }

    private void setAnnotations(JCas aJCas, EntityAnnotation entityAnnotation, int begin, int end) {
        GroundedEntity groundedEntity = new GroundedEntity(aJCas, begin, end);
        groundedEntity.setKnowledgeBaseId(entityAnnotation.entityId);

        StringArray kbNames = new StringArray(aJCas, 2);
        StringArray kbValues = new StringArray(aJCas, 2);

        kbNames.set(0, "freebase");
        kbValues.set(0, entityAnnotation.entityId);

        kbNames.set(1, "wikipedia");
        kbValues.set(1, entityAnnotation.wikiname);

        groundedEntity.setKnowledgeBaseNames(kbNames);
        groundedEntity.setKnowledgeBaseValues(kbValues);

        groundedEntity.setConfidence(entityAnnotation.score);

        UimaAnnotationUtils.finishAnnotation(groundedEntity, COMPONENT_ID, 0, aJCas);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String baseName = UimaConvenience.getArticleName(aJCas);
        File tagmeResultFile = new File(linkerResultFolder, baseName);


        List<String> defaultFields = new ArrayList<>();
        defaultFields.add("title");
        defaultFields.add("bodyText");

        try {
            if (tagmeResultFile.exists()) {
                JsonObject jsonDoc = parseResultFile(tagmeResultFile);

                List<EntityAnnotation> annotations = loadResults(jsonDoc, defaultFields, useToken);

                if (useToken) {
                    ArrayList<StanfordCorenlpToken> defaultTokens = new ArrayList<>(
                            JCasUtil.select(aJCas, StanfordCorenlpToken.class));
                    int defaultJsonTokenCount = countTokensInJson(jsonDoc, defaultFields);

                    int[] tokenMapping = loadTokenMapping(defaultJsonTokenCount, defaultTokens);

                    int numberAdded = setAnnotationsFromToken(aJCas, annotations, defaultTokens, tokenMapping);
                    if (numberAdded != annotations.size()) {
                        logger.warn(String.format("Number annotations added %d is not the same as annotations " +
                                "read %d in default view from doc %s.", numberAdded, annotations.size(), baseName));
                    }
                } else {
                    setAnnotationsFromCharacters(aJCas, annotations);
                }

                for (String additionalViewName : additionalViews) {
                    List<String> additionalFields = Lists.newArrayList(additionalViewName);
                    List<EntityAnnotation> viewAnnotations = loadResults(jsonDoc, additionalFields, useToken);
                    JCas view = JCasUtil.getView(aJCas, additionalViewName, false);

                    List<StanfordCorenlpToken> viewTokens = new ArrayList<>(
                            JCasUtil.select(view, StanfordCorenlpToken.class));

                    if (useToken) {
                        int viewJsonTokenCount = countTokensInJson(jsonDoc, additionalFields);

                        int[] tokenMapping = loadTokenMapping(viewJsonTokenCount, viewTokens);

                        int numberAdded = setAnnotationsFromToken(view, viewAnnotations, viewTokens, tokenMapping);
                        if (numberAdded != viewAnnotations.size()) {
                            logger.warn(String.format("Number annotations added %d is not the same as annotations " +
                                            "read %d in view %s from doc %s.",
                                    numberAdded, viewAnnotations.size(), additionalViewName, baseName));
                        }

                    } else {
                        setAnnotationsFromCharacters(view, viewAnnotations);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
