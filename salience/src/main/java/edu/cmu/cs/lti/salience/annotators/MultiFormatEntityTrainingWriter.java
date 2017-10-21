package edu.cmu.cs.lti.salience.annotators;

import com.google.gson.Gson;
import edu.cmu.cs.lti.collection_reader.AnnotatedNytReader;
import edu.cmu.cs.lti.frame.FrameExtractor;
import edu.cmu.cs.lti.frame.FrameStructure;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.salience.model.SalienceJSONClasses.*;
import edu.cmu.cs.lti.salience.utils.FeatureUtils;
import edu.cmu.cs.lti.salience.utils.LookupTable;
import edu.cmu.cs.lti.salience.utils.SalienceUtils;
import edu.cmu.cs.lti.salience.utils.TextUtils;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
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
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.*;

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

    public static final String PARAM_JOINT_EMBEDDING = "jointEmbeddingFile";
    @ConfigurationParameter(name = PARAM_JOINT_EMBEDDING)
    private File jointEmbeddingFile;

    public static final String PARAM_FRAME_RELATION = "frameRelationFile";
    @ConfigurationParameter(name = PARAM_FRAME_RELATION, mandatory = false)
    private File frameRelationFile;

    public static final String PARAM_FEATURE_OUTPUT_DIR = "featureOutput";
    @ConfigurationParameter(name = PARAM_FEATURE_OUTPUT_DIR)
    private String featureOutputDir;

    public static final String PARAM_WRITE_ENTITY = "writeEntity";
    @ConfigurationParameter(name = PARAM_WRITE_ENTITY, defaultValue = "false")
    private boolean writeEntity;

    public static final String PARAM_WRITE_EVENT = "writeEvent";
    @ConfigurationParameter(name = PARAM_WRITE_EVENT, defaultValue = "false")
    private boolean writeEvent;

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

    // Calculate embedding similarity
    private LookupTable.SimCalculator simCalculator;
    private LookupTable table;

    //
    private FrameExtractor frameExtractor;
    private Set<String> eventFrameTypes;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            trainDocs = SalienceUtils.readSplit(trainSplitFile);
            testDocs = SalienceUtils.readSplit(testSplitFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Number of docs in training: " + trainDocs.size());
        logger.info("Number of docs in testing: " + testDocs.size());

        try {
            table = SalienceUtils.loadEmbedding(jointEmbeddingFile);
            simCalculator = new LookupTable.SimCalculator(table);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (frameRelationFile != null) {
            try {
                frameExtractor = new FrameExtractor(frameRelationFile.getPath());
                eventFrameTypes = frameExtractor.getAllInHeritedFrameNames("Event");
            } catch (JDOMException | IOException e) {
                e.printStackTrace();
            }
        }

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


        if (!(writeEntity || writeEvent)) {
            throw new ResourceInitializationException(new IllegalArgumentException("Not writing entity or events!"));
        }

        if (writeEvent && writeEntity) {
            throw new ResourceInitializationException(new IllegalArgumentException("Writing both entity and events!"));
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

    private void writeEventGold(JCas mainView, Writer output, boolean useToken) throws IOException {
        String docno = UimaConvenience.getArticleName(mainView);
        String title = TextUtils.asTokenized(JCasUtil.selectSingle(mainView, Headline.class));

        StringBuilder sb = new StringBuilder();
        sb.append(docno).append(" ").append(title).append("\n");

        JCas abstractView = JCasUtil.getView(mainView, AnnotatedNytReader.ABSTRACT_VIEW_NAME, false);
        Set<String> abstractLemmas = new HashSet<>();

        for (StanfordCorenlpToken token : JCasUtil.select(abstractView, StanfordCorenlpToken.class)) {
            abstractLemmas.add(token.getLemma().toLowerCase());
        }

        Body body = JCasUtil.selectSingle(mainView, Body.class);

        int index = 0;
        for (FrameStructure fs : frameExtractor.getFramesOfType(body, eventFrameTypes)) {
            SemaforLabel target = fs.getTarget();
            StanfordCorenlpToken targetHead = UimaNlpUtils.findHeadFromStanfordAnnotation(fs.getTarget());
            if (targetHead != null) {
                String targetLemma = targetHead.getLemma().toLowerCase();
                if (abstractLemmas.contains(targetLemma)) {
                    int salience = abstractLemmas.contains(targetLemma) ? 1 : 0;
                    sb.append(index).append("\t").append(salience).append("\t").append("-").append("\t");
                    addSpan(sb, useToken, target, body);
                    sb.append("\t").append(fs.getFrameName()).append("\n");
                }
                index++;
            }
        }

        sb.append("\n");
        output.write(sb.toString());
    }

    private void writeEntityGold(JCas mainView, Writer output, boolean useToken) throws IOException {
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

            sb.append(index).append("\t").append(salience).append("\t").append(entityCounts.get(id)).append("\t");
            addSpan(sb, useToken, groundedEntity, body);
            sb.append("\t").append(id).append("\n");
            index++;
        }

        sb.append("\n");
        output.write(sb.toString());
    }

    private void writeGold(JCas mainView, Writer output, boolean useToken) throws IOException {
        if (writeEntity) {
            writeEntityGold(mainView, output, useToken);
        } else if (writeEvent) {
            writeEventGold(mainView, output, useToken);
        }
    }

    private void addSpan(StringBuilder sb, boolean useToken, ComponentAnnotation anno, ArticleComponent article) {
        String text = anno.getCoveredText().replaceAll("\t", " ").replaceAll("\n", " ");
        int begin;
        int end;
        if (useToken) {
            Span tokenSpan = TextUtils.getSpaceTokenOffset(article, anno);
            begin = tokenSpan.getBegin();
            end = tokenSpan.getEnd();
        } else {
            begin = anno.getBegin();
            end = anno.getEnd();
        }
        sb.append(text).append("\t").append(begin).append("\t").append(end);
    }

    private void writeTagged(JCas aJCas, Writer output, Writer featureWriter) throws IOException {
        if (writeEntity) {
            writeEntityTagged(aJCas, output, featureWriter);
        } else {

        }
    }

    private void writeEntityTagged(JCas aJCas, Writer output, Writer featureWriter) throws IOException {
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
                    entity.feature = new Feature(table, instanceLookup.get(entity.id));
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
        String inputDir = argv[1];
        String outputDir = argv[2];
        String trainingSplitFile = argv[3];
        String testSplitFile = argv[4];
        String featureOutput = argv[5];
        String embeddingPath = argv[6];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, inputDir,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz",
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true
        );

//        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
//                MultiFormatEntityTrainingWriter.class, typeSystemDescription,
//                MultiFormatEntityTrainingWriter.PARAM_OUTPUT_DIR, new File(workingDir, outputDir),
//                MultiFormatEntityTrainingWriter.PARAM_TRAIN_SPLIT, trainingSplitFile,
//                MultiFormatEntityTrainingWriter.PARAM_TEST_SPLIT, testSplitFile,
//                MultiFormatEntityTrainingWriter.PARAM_OUTPUT_PREFIX, "nyt_salience",
//                MultiFormatEntityTrainingWriter.MULTI_THREAD, true,
//                MultiFormatEntityTrainingWriter.PARAM_JOINT_EMBEDDING, embeddingPath,
//                MultiFormatEntityTrainingWriter.PARAM_FEATURE_OUTPUT_DIR, featureOutput
//        );

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                MultiFormatEntityTrainingWriter.class, typeSystemDescription,
                MultiFormatEntityTrainingWriter.PARAM_OUTPUT_DIR, new File(workingDir, outputDir),
                MultiFormatEntityTrainingWriter.PARAM_TRAIN_SPLIT, trainingSplitFile,
                MultiFormatEntityTrainingWriter.PARAM_TEST_SPLIT, testSplitFile,
                MultiFormatEntityTrainingWriter.PARAM_OUTPUT_PREFIX, "nyt_salience",
                MultiFormatEntityTrainingWriter.MULTI_THREAD, true,
                MultiFormatEntityTrainingWriter.PARAM_JOINT_EMBEDDING, embeddingPath,
                MultiFormatEntityTrainingWriter.PARAM_FEATURE_OUTPUT_DIR, featureOutput,
                MultiFormatEntityTrainingWriter.PARAM_WRITE_EVENT, true,
                MultiFormatEntityTrainingWriter.PARAM_FRAME_RELATION, "../data/resources/fndata-1.7/frRelation.xml"
        );


        new BasicPipeline(reader, true, true, 7, writer).run();
    }
}
