package edu.cmu.cs.lti.salience.annotators;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.cmu.cs.lti.salience.utils.LookupTable;
import edu.cmu.cs.lti.salience.utils.SalienceFeatureExtractor;
import edu.cmu.cs.lti.salience.utils.SalienceUtils;
import edu.cmu.cs.lti.script.type.Body;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.GroundedEntity;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.FeatureUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/20/18
 * Time: 6:14 PM
 *
 * @author Zhengzhong Liu
 */
public class SalienceInputCreator extends AbstractLoggingAnnotator {

    public static final String PARAM_JOINT_EMBEDDING = "jointEmbeddingFile";
    @ConfigurationParameter(name = PARAM_JOINT_EMBEDDING)
    private File jointEmbeddingFile;

    public static final String PARAM_TAGME_OUTPUT = "tagmeOutput";
    @ConfigurationParameter(name = PARAM_TAGME_OUTPUT)
    private File tagmeOutputDir;

    public static final String PARAM_OUTPUT_DIR = "outputDir";
    @ConfigurationParameter(name = PARAM_OUTPUT_DIR)
    private File outputDir;

    private LookupTable.SimCalculator simCalculator;

    private BufferedWriter writer;

    private Map<String, File> taggedFiles;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try {
            LookupTable table = SalienceUtils.loadEmbedding(jointEmbeddingFile);
            simCalculator = new LookupTable.SimCalculator(table);
            writer = new BufferedWriter(new FileWriter(new File(outputDir, "data.json")));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        taggedFiles = new HashMap<>();
        for (File file : FileUtils.listFiles(tagmeOutputDir, new String[]{"json"}, false)) {
            taggedFiles.put(file.getName().split("\\.")[0], file);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Body body = JCasUtil.selectSingle(aJCas, Body.class);

        // Dummy empty event and entity salience list.
        List<EventMention> bodyEventMentions = JCasUtil.selectCovered(EventMention.class, body);

        try {
            addTagmeEntities(aJCas);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }

        int[] eventSaliency = new int[bodyEventMentions.size()];
        Set<String> entitySaliency = new HashSet<>();


        List<FeatureUtils.SimpleInstance> entityInstances = SalienceFeatureExtractor.getKbInstances(aJCas,
                entitySaliency, simCalculator);
        List<FeatureUtils.SimpleInstance> eventInstances = SalienceFeatureExtractor.getEventInstances(body,
                entityInstances, eventSaliency, simCalculator);

        try {
            MultiFormatSalienceDataWriter.writeTagged(aJCas, writer, entityInstances, eventInstances, entitySaliency,
                    eventSaliency);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void addTagmeEntities(JCas aJCas) throws IOException {
        String articleBaseName = FilenameUtils.getBaseName(UimaConvenience.getArticleName(aJCas));

        File taggedFile = taggedFiles.get(articleBaseName);
        JsonParser parser = new JsonParser();
        JsonObject taggedData = parser.parse(FileUtils.readFileToString(taggedFile)).getAsJsonObject();

        List<GroundedEntity> entities = new ArrayList<>();

        for (JsonElement jsonElement : taggedData.get("annotations").getAsJsonArray()) {
            JsonObject anno = jsonElement.getAsJsonObject();

            if (!anno.has("mid")) {
                continue;
            }

            String mid = anno.get("mid").getAsString();
            int start = anno.get("start").getAsInt();
            int end = anno.get("end").getAsInt();

            double rho = anno.get("rho").getAsDouble();

            String wikiname = anno.get("title").getAsString().replace(" ", "_");

            GroundedEntity groundedEntity = new GroundedEntity(aJCas, start, end);
            groundedEntity.setKnowledgeBaseId(mid);

            StringArray kbNames = new StringArray(aJCas, 2);
            StringArray kbValues = new StringArray(aJCas, 2);

            kbNames.set(0, "freebase");
            kbValues.set(0, mid);

            kbNames.set(1, "wikipedia");
            kbValues.set(1, wikiname);

            groundedEntity.setKnowledgeBaseNames(kbNames);
            groundedEntity.setKnowledgeBaseValues(kbValues);
            groundedEntity.setConfidence(rho);

            StanfordCorenlpToken headword = UimaNlpUtils.findHeadFromStanfordAnnotation(groundedEntity);
            if (headword == null) {
                // Do not include unmappable entities.
                groundedEntity.removeFromIndexes();
            } else {
                UimaAnnotationUtils.finishAnnotation(groundedEntity, COMPONENT_ID, 0, aJCas);
                entities.add(groundedEntity);
            }
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
}
