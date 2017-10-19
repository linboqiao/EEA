package edu.cmu.cs.lti.salience.annotators;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import edu.cmu.cs.lti.collection_reader.AnnotatedNytReader;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.salience.utils.FeatureUtils;
import edu.cmu.cs.lti.salience.utils.LookupTable;
import edu.cmu.cs.lti.salience.utils.SalienceUtils;
import edu.cmu.cs.lti.salience.utils.TextUtils;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.io.FileUtils;
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
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.*;

import static edu.cmu.cs.lti.salience.utils.FeatureUtils.lexicalPrefix;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/4/17
 * Time: 1:40 PM
 *
 * @author Zhengzhong Liu
 */
public class MultiFormatEntityTrainingWriter extends AbstractLoggingAnnotator {
    public static final String PARAM_TEST_SPLIT = "testSplit";
    @ConfigurationParameter(name = PARAM_TEST_SPLIT)
    private File testSplitFile;

    public static final String PARAM_TRAIN_SPLIT = "trainSplit";
    @ConfigurationParameter(name = PARAM_TRAIN_SPLIT)
    private File trainSplitFile;

    public static final String PARAM_OUTPUT_DIR = "outputDir";
    @ConfigurationParameter(name = PARAM_OUTPUT_DIR)
    private String outputDir;

    public static final String PARAM_OUTPUT_PREFIX = "outputPrefix";
    @ConfigurationParameter(name = PARAM_OUTPUT_PREFIX)
    private String outputPrefix;

    public static final String PARAM_ENTITY_EMBEDDING = "entityEmbeddingFile";
    @ConfigurationParameter(name = PARAM_ENTITY_EMBEDDING)
    private File entityEmbeddingFile;

    public static final String PARAM_FEATURE_OUTPUT_DIR = "featureOutput";
    @ConfigurationParameter(name = PARAM_FEATURE_OUTPUT_DIR)
    private String featureOutputDir;

    private Set<String> trainDocs;
    private Set<String> testDocs;

    private BufferedWriter trainGoldWriter;
    private BufferedWriter testGoldWriter;

    private BufferedWriter trainGoldCharWriter;
    private BufferedWriter testGoldCharWriter;

    private BufferedWriter trainTagsWriter;
    private BufferedWriter testTagsWriter;

    private BufferedWriter trainFeatures;
    private BufferedWriter testFeatures;

    private LookupTable.SimCalculator simCalculator;
    private LookupTable table;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            trainDocs = SalienceUtils.readSplit(trainSplitFile);
            testDocs = SalienceUtils.readSplit(testSplitFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            table = SalienceUtils.loadEmbedding(entityEmbeddingFile);
            simCalculator = new LookupTable.SimCalculator(table);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Number of docs in training: " + trainDocs.size());
        logger.info("Number of docs in testing: " + testDocs.size());

        try {
            trainGoldWriter = getWriter(outputDir, "salience", "token", outputPrefix + "_train");
            testGoldWriter = getWriter(outputDir, "salience", "token", outputPrefix + "_test");

            trainGoldCharWriter = getWriter(outputDir, "salience", "char", outputPrefix + "_train");
            testGoldCharWriter = getWriter(outputDir, "salience", "char", outputPrefix + "_test");

            trainTagsWriter = getWriter(outputDir, "docs", outputPrefix + "_tagged_train.json");
            testTagsWriter = getWriter(outputDir, "docs", outputPrefix + "_tagged_test.json");

            trainFeatures = getWriter(featureOutputDir, "train.tsv");
            testFeatures = getWriter(featureOutputDir, "test.tsv");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedWriter getWriter(String... segments) throws IOException {
        File outputPath = edu.cmu.cs.lti.utils.FileUtils.joinPathsAsFile(segments);
        File parent = outputPath.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        logger.info("Writing to : " + outputPath);
        return new BufferedWriter(new FileWriter(outputPath));
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            trainGoldWriter.close();
            testGoldWriter.close();
            trainTagsWriter.close();
            testTagsWriter.close();
            trainTagsWriter.close();
            testTagsWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<String> readList(File splitFile) throws IOException {
        Set<String> docs = new HashSet<>();
        for (String s : FileUtils.readLines(splitFile)) {
            docs.add(s.trim());
        }
        return docs;
    }

    class DocStructure {
        private String bodyText;
        private String docno;
        private Spots spot;
        private String title;
        @SerializedName("abstract")
        private String abstractText;
    }

    class Spots {
        private List<Spot> bodyText;
        @SerializedName("abstract")
        private List<Spot> abstractSpots;
        private List<Spot> title;
    }

    class Spot {
        List<Integer> loc;
        String surface;
    }

    class EventSpot extends Spot {
    }

    class EntitySpot extends Spot {
        String wiki_name;
        List<Link> entities;
    }

    class Link {
        public Link(double score, String id) {
            this.score = score;
            this.id = id;
        }

        double score;
        String id;
        Feature feature;
    }

    class Feature {
        private Feature(FeatureUtils.SimpleInstance instance) {
            featureArray = new ArrayList<>();
            List<Double> lexicalFeatures = new ArrayList<>();
            instance.getFeatureMap().keySet().stream().sorted().forEach(f -> {
                if (!f.startsWith(lexicalPrefix)) {
                    featureArray.add(instance.getFeatureMap().get(f));
                } else {
                    String word = f.split("_")[1];
                    for (double v : table.getEmbedding(word)) {
                        lexicalFeatures.add(v);
                    }
                }
            });
            featureArray.addAll(lexicalFeatures);
            featureNames = instance.getFeatureNames();
        }

        private Feature() {
            featureArray = new ArrayList<>();
        }

        private List<Double> featureArray;
        private List<String> featureNames;
    }

    private List<Spot> getEntitySpots(ArticleComponent articleComponent) {
        List<Spot> spots = new ArrayList<>();

        for (GroundedEntity groundedEntity : JCasUtil.selectCovered(GroundedEntity.class, articleComponent)) {
            Span tokenOffset = TextUtils.getSpaceTokenOffset(articleComponent, groundedEntity);
            EntitySpot spot = new EntitySpot();
            spot.loc = new ArrayList<>();
            spot.loc.add(tokenOffset.getBegin());
            spot.loc.add(tokenOffset.getEnd());
            spot.wiki_name = groundedEntity.getKnowledgeBaseId();
            spot.surface = groundedEntity.getCoveredText();

            StringArray kbNames = groundedEntity.getKnowledgeBaseNames();
            StringArray kbValues = groundedEntity.getKnowledgeBaseValues();

            for (int i = 0; i < kbNames.size(); i++) {
                String name = kbNames.get(i);
                String value = kbValues.get(i);
                if (name.equals("wikipedia")) {
                    spot.wiki_name = value;
                }
            }

            Link link = new Link(groundedEntity.getConfidence(), groundedEntity.getKnowledgeBaseId());

            spot.entities = new ArrayList<>();

            spot.entities.add(link);

            spots.add(spot);
        }
        return spots;
    }

    private void getEventiveFrames(ArticleComponent component) {
    }

    private void writeGold(JCas mainView, Writer output, boolean useToken) throws IOException {
        String docno = UimaConvenience.getArticleName(mainView);
        String title = TextUtils.asTokenized(JCasUtil.selectSingle(mainView, Headline.class));

        StringBuilder sb = new StringBuilder();
        sb.append(docno).append(" ").append(title).append("\n");

        Set<String> abstractEntities = SalienceUtils.getAbstractEntities(mainView);

        int index = 0;

        TObjectIntMap<String> entityCounts = new TObjectIntHashMap<>();
        for (GroundedEntity groundedEntity : JCasUtil.select(mainView, GroundedEntity.class)) {
            entityCounts.adjustOrPutValue(groundedEntity.getKnowledgeBaseId(), 1, 1);
        }

        Body body = JCasUtil.selectSingle(mainView, Body.class);

        for (GroundedEntity groundedEntity : JCasUtil.selectCovered(GroundedEntity.class, body)) {
            String id = groundedEntity.getKnowledgeBaseId();
            int salience = abstractEntities.contains(id) ? 1 : 0;

            sb.append(index);
            sb.append("\t");
            sb.append(salience);
            sb.append("\t");
            sb.append(entityCounts.get(id));
            sb.append("\t");
            sb.append(groundedEntity.getCoveredText().replaceAll("\t", " ")
                    .replaceAll("\n", " "));

            int begin;
            int end;
            if (useToken) {
                Span tokenSpan = TextUtils.getSpaceTokenOffset(body, groundedEntity);
                begin = tokenSpan.getBegin();
                end = tokenSpan.getEnd();
            } else {
                begin = groundedEntity.getBegin();
                end = groundedEntity.getEnd();
            }

            sb.append("\t").append(begin).append("\t").append(end).append("\t").append(id).append("\n");

            index++;
        }

        sb.append("\n");
        output.write(sb.toString());
    }

    private void writeTagged(JCas aJCas, Writer output, Writer featureWriter) throws IOException {
        Gson gson = new Gson();

        Headline title = JCasUtil.selectSingle(aJCas, Headline.class);
        String titleStr = TextUtils.asTokenized(title);
        String docid = UimaConvenience.getArticleName(aJCas);

        Body body = JCasUtil.selectSingle(aJCas, Body.class);

        List<Spot> titleSpots = getEntitySpots(title);
        List<Spot> bodySpots = getEntitySpots(body);

        // Handle features.
        featureWriter.write(docid + " " + titleStr + "\n");
        List<FeatureUtils.SimpleInstance> instances = FeatureUtils.getKbInstances(aJCas, simCalculator);
        writeFeatures(instances, featureWriter);
        featureWriter.write("\n");
        addEntityFeatureToSpots(bodySpots, instances);


        JCas abstractView = JCasUtil.getView(aJCas, AnnotatedNytReader.ABSTRACT_VIEW_NAME, false);
        Article abstractArticle = JCasUtil.selectSingle(abstractView, Article.class);

        List<Spot> abstractSpots = getEntitySpots(abstractArticle);

        DocStructure doc = new DocStructure();
        Spots allSpots = new Spots();
        allSpots.bodyText = bodySpots;
        allSpots.abstractSpots = abstractSpots;
        allSpots.title = titleSpots;

        doc.bodyText = TextUtils.asTokenized(body);
        doc.docno = docid;
        doc.spot = allSpots;
        doc.title = titleStr;
        doc.abstractText = TextUtils.asTokenized(abstractArticle);

        String jsonStr = gson.toJson(doc);
        output.write(jsonStr + "\n");

    }

    private void addEntityFeatureToSpots(List<Spot> bodySpots, List<FeatureUtils.SimpleInstance> instances) {
        Map<String, FeatureUtils.SimpleInstance> instanceLookup = new HashMap<>();
        for (FeatureUtils.SimpleInstance instance : instances) {
            instanceLookup.put(instance.getInstanceName(), instance);
        }

        for (Spot bodySpot : bodySpots) {
            for (Link entity : ((EntitySpot) bodySpot).entities) {
                FeatureUtils.SimpleInstance instance = instanceLookup.get(entity.id);
                if (instance != null) {
                    entity.feature = new Feature(instanceLookup.get(entity.id));
                } else {
                    // This should not happen because both spots are looking for entities in the body.
                    entity.feature = new Feature();
                }
            }
        }
    }

    private void writeFeatures(List<FeatureUtils.SimpleInstance> instances, Writer featureWriter) throws IOException {
        for (FeatureUtils.SimpleInstance instance : instances) {
            featureWriter.write(instance.toString() + "\n");
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String articleName = UimaConvenience.getArticleName(aJCas);
        try {
            if (trainDocs.contains(articleName)) {
                writeGold(aJCas, trainGoldWriter, true);
                writeGold(aJCas, trainGoldCharWriter, false);
                writeTagged(aJCas, trainTagsWriter, trainFeatures);
            } else if (testDocs.contains(articleName)) {
                writeGold(aJCas, testGoldWriter, true);
                writeGold(aJCas, testGoldCharWriter, false);
                writeTagged(aJCas, testTagsWriter, testFeatures);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String outputDir = argv[1];
        String trainingSplitFile = argv[2];
        String testSplitFile = argv[3];
        String featureOutput = argv[4];
        String embeddingPath = argv[5];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, "tagged",
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                MultiFormatEntityTrainingWriter.class, typeSystemDescription,
                MultiFormatEntityTrainingWriter.PARAM_OUTPUT_DIR, new File(workingDir, outputDir),
                MultiFormatEntityTrainingWriter.PARAM_TRAIN_SPLIT, trainingSplitFile,
                MultiFormatEntityTrainingWriter.PARAM_TEST_SPLIT, testSplitFile,
                MultiFormatEntityTrainingWriter.PARAM_OUTPUT_PREFIX, "nyt_salience",
                MultiFormatEntityTrainingWriter.MULTI_THREAD, true,
                MultiFormatEntityTrainingWriter.PARAM_ENTITY_EMBEDDING, embeddingPath,
                MultiFormatEntityTrainingWriter.PARAM_FEATURE_OUTPUT_DIR, featureOutput
        );

        new BasicPipeline(reader, true, true, 7, writer).run();
    }
}
