package edu.cmu.cs.lti.salience.annotators;

import com.google.gson.Gson;
import edu.cmu.cs.lti.collection_reader.AnnotatedNytReader;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.salience.model.SalienceJSONClasses.*;
import edu.cmu.cs.lti.salience.utils.LookupTable;
import edu.cmu.cs.lti.salience.utils.SalienceFeatureExtractor;
import edu.cmu.cs.lti.salience.utils.SalienceUtils;
import edu.cmu.cs.lti.salience.utils.TextUtils;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.FeatureUtils;
import edu.cmu.cs.lti.utils.FileUtils;
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
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/4/17
 * Time: 1:40 PM
 *
 * @author Zhengzhong Liu
 */
public class MultiFormatSalienceDataWriter extends AbstractLoggingAnnotator {
    public static final String PARAM_PROCESS_MODE = "processMode";
    @ConfigurationParameter(name = PARAM_PROCESS_MODE)
    private boolean processMode;

    public static final String PARAM_TEST_SPLIT = "testSplit";
    @ConfigurationParameter(name = PARAM_TEST_SPLIT)
    private File testSplitFile;

    public static final String PARAM_TRAIN_SPLIT = "trainSplit";
    @ConfigurationParameter(name = PARAM_TRAIN_SPLIT)
    private File trainSplitFile;

    public static final String PARAM_DEV_SPLIT = "devSplit";
    @ConfigurationParameter(name = PARAM_DEV_SPLIT, mandatory = false)
    private File devSplitFile;

    public static final String PARAM_OUTPUT_DIR = "outputDir";
    @ConfigurationParameter(name = PARAM_OUTPUT_DIR)
    private String outputDir;

    public static final String PARAM_OUTPUT_PREFIX = "outputPrefix";
    @ConfigurationParameter(name = PARAM_OUTPUT_PREFIX)
    private String outputPrefix;

    public static final String PARAM_JOINT_EMBEDDING = "jointEmbeddingFile";
    @ConfigurationParameter(name = PARAM_JOINT_EMBEDDING)
    private File jointEmbeddingFile;

    public static final String PARAM_WRITE_ENTITY = "writeEntity";
    @ConfigurationParameter(name = PARAM_WRITE_ENTITY, defaultValue = "false")
    private boolean writeEntity;

    public static final String PARAM_WRITE_EVENT = "writeEvent";
    @ConfigurationParameter(name = PARAM_WRITE_EVENT, defaultValue = "false")
    private boolean writeEvent;

    private Set<String> trainDocs;
    private Set<String> testDocs;
    private Set<String> devDocs;

    // Calculate embedding similarity
    private LookupTable.SimCalculator simCalculator;

    private Map<String, BufferedWriter> goldTokenEntityWriters;
    private Map<String, BufferedWriter> goldTokenEventWriters;
    private Map<String, BufferedWriter> goldCharEntityWriters;
    private Map<String, BufferedWriter> goldCharEventWriters;
    private Map<String, BufferedWriter> tagWriters;

    private AtomicInteger documentCount;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        if (!processMode) {
            try {
                trainDocs = SalienceUtils.readSplit(trainSplitFile);
                logger.info("Number of docs in training: " + trainDocs.size());

                testDocs = SalienceUtils.readSplit(testSplitFile);
                logger.info("Number of docs in testing: " + testDocs.size());

                if (devSplitFile != null) {
                    devDocs = SalienceUtils.readSplit(devSplitFile);
                    logger.info("Number of docs in dev: " + devDocs.size());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            LookupTable table = SalienceUtils.loadEmbedding(jointEmbeddingFile);
            simCalculator = new LookupTable.SimCalculator(table);
        } catch (IOException e) {
            e.printStackTrace();
        }

        documentCount = new AtomicInteger();

        try {
            goldTokenEntityWriters = getWriters(outputDir, "entity_gold", "token");
            goldTokenEventWriters = getWriters(outputDir, "event_gold", "token");

            goldCharEntityWriters = getWriters(outputDir, "entity_gold", "char");
            goldCharEventWriters = getWriters(outputDir, "event_gold", "char");

            tagWriters = getWriters(outputDir, "docs");
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

    private Map<String, BufferedWriter> getWriters(String... segments) throws IOException {
        Map<String, BufferedWriter> writers = new HashMap<>();

        File outputParent = FileUtils.joinPathsAsFile(segments);
        File fullTrainOutput = new File(outputParent, "train.gz");
        File fullTestOutput = new File(outputParent, "test.gz");

        if (!outputParent.exists()) {
            outputParent.mkdirs();
        }

        writers.put("train", new BufferedWriter(
                new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fullTrainOutput)))
        ));
        writers.put("test", new BufferedWriter(
                new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fullTestOutput)))
        ));


        if (devSplitFile != null) {
            File fullDevOutput = new File(outputParent, "dev.gz");
            writers.put("dev", new BufferedWriter(
                    new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fullDevOutput)))
            ));
        }
        return writers;
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            close(goldTokenEntityWriters);
            close(goldTokenEventWriters);
            close(goldCharEntityWriters);
            close(goldCharEventWriters);
            close(tagWriters);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close(Map<String, BufferedWriter> writers) throws IOException {
        for (BufferedWriter writer : writers.values()) {
            writer.close();
        }
    }

    private static List<Spot> getEventSpots(ArticleComponent articleComponent, Map<StanfordCorenlpToken, String>
            entityIds) {
        List<Spot> spots = new ArrayList<>();

        int index = 0;

        for (EventMention eventMention : JCasUtil.selectCovered(EventMention.class, articleComponent)) {
            EventSpot spot = new EventSpot();
            spot.frame_name = eventMention.getFrameName();
            Span tokenOffset = TextUtils.getSpaceTokenOffset(articleComponent, eventMention);
            spot.loc = Arrays.asList(tokenOffset.getBegin(), tokenOffset.getEnd());
            spot.surface = TextUtils.asTokenized(eventMention);
            spot.id = Integer.toString(index);

            for (EventMentionArgumentLink argumentLink :
                    FSCollectionFactory.create(eventMention.getArguments(), EventMentionArgumentLink.class)) {
                EntityMention argumentMention = argumentLink.getArgument();
                Argument argument = new Argument();
                argument.surface = TextUtils.asTokenized(argumentMention);
                StanfordCorenlpToken argumentHead = UimaNlpUtils.findHeadFromStanfordAnnotation(argumentMention);
                argument.headEntityId = entityIds.getOrDefault(argumentHead, "-");
                argument.type = argumentLink.getArgumentRole();
            }
            spots.add(spot);
            index++;
        }
        return spots;
    }


    private static List<Spot> getEntitySpots(ArticleComponent articleComponent, Map<StanfordCorenlpToken, String>
            entityIds) {
        List<Spot> spots = new ArrayList<>();

        int index = 0;


        for (GroundedEntity groundedEntity : JCasUtil.selectCovered(GroundedEntity.class, articleComponent)) {
            Span tokenOffset = TextUtils.getSpaceTokenOffset(articleComponent, groundedEntity);
            EntitySpot spot = new EntitySpot();
            spot.loc = Arrays.asList(tokenOffset.getBegin(), tokenOffset.getEnd());

            spot.surface = groundedEntity.getCoveredText();
            spot.id = groundedEntity.getKnowledgeBaseId();
            spot.score = groundedEntity.getConfidence();

            StringArray kbNames = groundedEntity.getKnowledgeBaseNames();
            StringArray kbValues = groundedEntity.getKnowledgeBaseValues();

            for (int i = 0; i < kbNames.size(); i++) {
                String name = kbNames.get(i);
                String value = kbValues.get(i);
                if (name.equals("wikipedia")) {
                    spot.wiki_name = value;
                }
            }

            StanfordCorenlpToken entityHead = UimaNlpUtils.findHeadFromStanfordAnnotation(groundedEntity);
            entityIds.put(entityHead, Integer.toString(index));

            spots.add(spot);
            index++;
        }

        return spots;
    }

    private void writeEventGold(JCas mainView, int[] saliency, Writer output, boolean useToken) throws IOException {
        String docno = UimaConvenience.getArticleName(mainView);
        StringBuilder sb = new StringBuilder();
        sb.append(docno).append(" ").append(getHeadline(mainView)).append("\n");

        Body body = JCasUtil.selectSingle(mainView, Body.class);

        int index = 0;
        for (EventMention eventMention : JCasUtil.selectCovered(EventMention.class, body)) {
            StanfordCorenlpToken targetHead = UimaNlpUtils.findHeadFromStanfordAnnotation(eventMention);
            if (targetHead != null) {
                int salience = saliency[index];
                sb.append(index).append("\t").append(salience).append("\t").append("-").append("\t");
                addSpan(sb, useToken, eventMention, body);
                sb.append("\t").append(eventMention.getFrameName()).append("\n");
                index++;
            }
        }

        sb.append("\n");
        output.write(sb.toString());
    }

    private int[] getEventSaliency(JCas mainView, List<EventMention> eventMentions) {
        JCas abstractView = JCasUtil.getView(mainView, AnnotatedNytReader.ABSTRACT_VIEW_NAME, false);
        Set<String> abstractLemmas = new HashSet<>();

        for (StanfordCorenlpToken token : JCasUtil.select(abstractView, StanfordCorenlpToken.class)) {
            abstractLemmas.add(token.getLemma().toLowerCase());
        }

        int[] saliency = new int[eventMentions.size()];
        int index = 0;

        for (EventMention eventMention : eventMentions) {
            StanfordCorenlpToken targetHead = UimaNlpUtils.findHeadFromStanfordAnnotation(eventMention);
            int salience = 0;
            if (targetHead != null) {
                String targetLemma = targetHead.getLemma().toLowerCase();
                salience = abstractLemmas.contains(targetLemma) ? 1 : 0;
            }
            saliency[index] = salience;
            index++;
        }
        return saliency;
    }

    private void writeEntityGold(JCas mainView, Set<String> salientEntities, Writer output, boolean useToken) throws
            IOException {
        String docno = UimaConvenience.getArticleName(mainView);

        StringBuilder sb = new StringBuilder();

        sb.append(docno).append(" ").append(getHeadline(mainView)).append("\n");

        int index = 0;

        TObjectIntMap<String> entityCounts = new TObjectIntHashMap<>();
        for (GroundedEntity groundedEntity : JCasUtil.select(mainView, GroundedEntity.class)) {
            entityCounts.adjustOrPutValue(groundedEntity.getKnowledgeBaseId(), 1, 1);
        }

        Body body = JCasUtil.selectSingle(mainView, Body.class);

        for (GroundedEntity groundedEntity : JCasUtil.selectCovered(GroundedEntity.class, body)) {
            String id = groundedEntity.getKnowledgeBaseId();
            int salience = salientEntities.contains(id) ? 1 : 0;
            sb.append(index).append("\t").append(salience).append("\t").append(entityCounts.get(id)).append("\t");
            addSpan(sb, useToken, groundedEntity, body);
            sb.append("\t").append(id).append("\n");
            index++;
        }

        sb.append("\n");
        output.write(sb.toString());
    }

    private String getHeadline(JCas view) {
        if (JCasUtil.select(view, Headline.class).size() == 1) {
            return TextUtils.asTokenized(JCasUtil.selectSingle(view, Headline.class));
        } else {
            return "";
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

    public static void writeTagged(JCas aJCas, Writer output,
                                   List<FeatureUtils.SimpleInstance> entityFeatures,
                                   List<FeatureUtils.SimpleInstance> eventFeatures,
                                   Set<String> entitySaliency,
                                   int[] eventSaliency
    ) throws IOException {
        Gson gson = new Gson();

        Body body = JCasUtil.selectSingle(aJCas, Body.class);

        String docid = UimaConvenience.getArticleName(aJCas);

        Map<StanfordCorenlpToken, String> bodyEntityIds = new HashMap<>();
        List<Spot> bodyEntities = getEntitySpots(body, bodyEntityIds);
        List<Spot> bodyEvents = getEventSpots(body, bodyEntityIds);

        addFeatureToSpots(bodyEntities, entityFeatures);
        addFeatureToSpots(bodyEvents, eventFeatures);

        addEventSalienceToSpots(bodyEvents, eventSaliency);
        addEntitySalienceToSpots(bodyEntities, entitySaliency);

        DocStructure doc = new DocStructure();

        Spots entitySpots = new Spots();
        entitySpots.bodyText = bodyEntities;

        Spots eventSpots = new Spots();
        eventSpots.bodyText = bodyEvents;

        doc.bodyText = TextUtils.asTokenized(body);
        doc.docno = docid;
        doc.spot = entitySpots;
        doc.event = eventSpots;

        JCas abstractView = JCasUtil.getView(aJCas, AnnotatedNytReader.ABSTRACT_VIEW_NAME, aJCas);
        if (abstractView != aJCas) {
            // This means there is not abstract view and we fall back to the original view.
            Article abstractArticle = JCasUtil.selectSingle(abstractView, Article.class);
            Map<StanfordCorenlpToken, String> abstractEntityIds = new HashMap<>();
            List<Spot> abstractEntities = getEntitySpots(abstractArticle, abstractEntityIds);
            List<Spot> abstractEvents = getEventSpots(abstractArticle, abstractEntityIds);

            entitySpots.abstractSpots = abstractEntities;
            eventSpots.abstractSpots = abstractEvents;
            doc.abstractText = TextUtils.asTokenized(abstractArticle);
        }
        String jsonStr = gson.toJson(doc);

        output.write(jsonStr + "\n");
    }

    private static void addFeatureToSpots(List<Spot> bodySpots, List<FeatureUtils.SimpleInstance> instances) {
        Map<String, FeatureUtils.SimpleInstance> featureLookup = new HashMap<>();
        for (FeatureUtils.SimpleInstance instance : instances) {
            featureLookup.put(instance.getInstanceName(), instance);
        }

        for (Spot bodySpot : bodySpots) {
            FeatureUtils.SimpleInstance instance = featureLookup.get(bodySpot.id);
            bodySpot.feature = new Feature(instance);
        }
    }

    private static void addEntitySalienceToSpots(List<Spot> spots, Set<String> salientEids) {
        for (Spot spot : spots) {
            spot.salience = salientEids.contains(spot.id) ? 1 : 0;
        }
    }

    private static void addEventSalienceToSpots(List<Spot> bodySpots, int[] eventSaliency) {
        for (int i = 0; i < bodySpots.size(); i++) {
            bodySpots.get(i).salience = eventSaliency[i];
        }
    }

    private void setAbstractArticle(JCas abstractView, Article mainArticle) {
        if (JCasUtil.select(abstractView, Article.class).isEmpty()) {
            // Article in abstract view.
            Article abstractArticle = new Article(abstractView);
            UimaAnnotationUtils.finishAnnotation(abstractArticle, 0, abstractView.getDocumentText().length(),
                    COMPONENT_ID, 0, abstractView);
            abstractArticle.setArticleName(mainArticle.getArticleName());
            abstractArticle.setLanguage(mainArticle.getLanguage());
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Article mainArticle = JCasUtil.selectSingle(aJCas, Article.class);
        JCas abstractView = JCasUtil.getView(aJCas, AnnotatedNytReader.ABSTRACT_VIEW_NAME, false);
        String articleName = mainArticle.getArticleName();

        setAbstractArticle(abstractView, mainArticle);

        Body body = JCasUtil.selectSingle(aJCas, Body.class);

        List<EventMention> bodyEventMentions = JCasUtil.selectCovered(EventMention.class, body);

        int[] eventSaliency = getEventSaliency(aJCas, bodyEventMentions);
        Set<String> entitySaliency = SalienceUtils.getAbstractEntities(aJCas);

        List<FeatureUtils.SimpleInstance> entityInstances = SalienceFeatureExtractor.getKbInstances(aJCas,
                entitySaliency, simCalculator);
        List<FeatureUtils.SimpleInstance> eventInstances = SalienceFeatureExtractor.getEventInstances(body,
                entityInstances, eventSaliency, simCalculator);

        try {
            if (trainDocs.contains(articleName)) {
                writeEntityGold(aJCas, entitySaliency, goldTokenEntityWriters.get("train"), true);
                writeEntityGold(aJCas, entitySaliency, goldCharEntityWriters.get("train"), false);

                writeEventGold(aJCas, eventSaliency, goldTokenEventWriters.get("train"), true);
                writeEventGold(aJCas, eventSaliency, goldCharEventWriters.get("train"), false);

                writeTagged(aJCas, tagWriters.get("train"), entityInstances, eventInstances, entitySaliency,
                        eventSaliency);
            } else if (testDocs.contains(articleName)) {
                writeEntityGold(aJCas, entitySaliency, goldTokenEntityWriters.get("test"), true);
                writeEntityGold(aJCas, entitySaliency, goldCharEntityWriters.get("test"), false);

                writeEventGold(aJCas, eventSaliency, goldTokenEventWriters.get("test"), true);
                writeEventGold(aJCas, eventSaliency, goldCharEventWriters.get("test"), false);

                writeTagged(aJCas, tagWriters.get("test"), entityInstances, eventInstances, entitySaliency,
                        eventSaliency);
            } else if (devDocs.contains(articleName)) {
                writeEntityGold(aJCas, entitySaliency, goldTokenEntityWriters.get("dev"), true);
                writeEntityGold(aJCas, entitySaliency, goldCharEntityWriters.get("dev"), false);

                writeEventGold(aJCas, eventSaliency, goldTokenEventWriters.get("dev"), true);
                writeEventGold(aJCas, eventSaliency, goldCharEventWriters.get("dev"), false);

                writeTagged(aJCas, tagWriters.get("dev"), entityInstances, eventInstances, entitySaliency,
                        eventSaliency);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int numDocs = documentCount.incrementAndGet();
    }

    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String inputDir = argv[1];
        String outputDir = argv[2];
        String trainingSplitFile = argv[3];
        String testSplitFile = argv[4];
        String embeddingPath = argv[5];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, inputDir,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz",
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true
        );

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                MultiFormatSalienceDataWriter.class, typeSystemDescription,
                MultiFormatSalienceDataWriter.PARAM_OUTPUT_DIR, new File(workingDir, outputDir),
                MultiFormatSalienceDataWriter.PARAM_TRAIN_SPLIT, trainingSplitFile,
                MultiFormatSalienceDataWriter.PARAM_TEST_SPLIT, testSplitFile,
                MultiFormatSalienceDataWriter.PARAM_OUTPUT_PREFIX, "nyt_salience",
                MultiFormatSalienceDataWriter.MULTI_THREAD, true,
                MultiFormatSalienceDataWriter.PARAM_JOINT_EMBEDDING, embeddingPath,
                MultiFormatSalienceDataWriter.PARAM_WRITE_EVENT, true
        );

        new BasicPipeline(reader, true, true, 7, writer).run();
    }
}
