package edu.cmu.cs.lti.salience.annotators;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import edu.cmu.cs.lti.collection_reader.AnnotatedNytReader;
import edu.cmu.cs.lti.model.Span;
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
import org.uimafit.pipeline.SimplePipeline;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/4/17
 * Time: 1:40 PM
 *
 * @author Zhengzhong Liu
 */
public class GoogleStyleSalienceGoldStandardWriter extends AbstractLoggingAnnotator {
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

    private Set<String> trainDocs;
    private Set<String> testDocs;

    private BufferedWriter trainGoldWriter;
    private BufferedWriter testGoldWriter;

    private BufferedWriter trainGoldCharWriter;
    private BufferedWriter testGoldCharWriter;

    private BufferedWriter trainTagsWriter;
    private BufferedWriter testTagsWriter;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            trainDocs = readList(trainSplitFile);
            testDocs = readList(testSplitFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            trainGoldWriter = getWriter(outputDir, "salience", "token", outputPrefix + "_train");
            testGoldWriter = getWriter(outputDir, "salience", "token", outputPrefix + "_test");

            trainGoldCharWriter = getWriter(outputDir, "salience", "char", outputPrefix + "_train");
            testGoldCharWriter = getWriter(outputDir, "salience", "char", outputPrefix + "_test");

            trainTagsWriter = getWriter(outputDir, "docs", outputPrefix + "_tagged_train.json");
            testTagsWriter = getWriter(outputDir, "docs", outputPrefix + "_tagged_test.json");
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
        for (String s : FileUtils.readLines(trainSplitFile)) {
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
        String wiki_name;
        String surface;
        List<Link> entities;
    }

    class Link {
        public Link(double score, String id) {
            this.score = score;
            this.id = id;
        }

        double score;
        String id;
    }

    private Span getSpaceTokenOffset(ArticleComponent articleComponent, ComponentAnnotation annotation) {
        int tokensBefore = 0;
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, articleComponent)) {
            if (token.getEnd() - 1 < annotation.getBegin()) {
                tokensBefore += asTokenized(token).split(" ").length;
            }
        }

        int begin = tokensBefore;

        int annoLength = 0;
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, annotation)) {
            annoLength += asTokenized(token).split(" ").length;
        }

        int end = begin + annoLength;

        return Span.of(begin, end);
    }

    private List<Spot> getSpots(ArticleComponent articleComponent) {
        List<Spot> spots = new ArrayList<>();

        for (GroundedEntity groundedEntity : JCasUtil.selectCovered(GroundedEntity.class, articleComponent)) {
            Span tokenOffset = getSpaceTokenOffset(articleComponent, groundedEntity);
            Spot spot = new Spot();
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

    private String asTokenized(ComponentAnnotation component) {
        List<String> words = new ArrayList<>();
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, component)) {
            words.add(token.getCoveredText());
        }
        return Joiner.on(" ").join(words);
    }

    private void writeGold(JCas mainView, Writer output, boolean useToken) throws IOException {
        JCas abstractView = JCasUtil.getView(mainView, AnnotatedNytReader.ABSTRACT_VIEW_NAME, false);

        String docno = UimaConvenience.getArticleName(mainView);
        String title = asTokenized(JCasUtil.selectSingle(mainView, Headline.class));

        output.write(docno + " " + title + "\n");

        Set<String> abstractEntities = new HashSet<>();
        for (GroundedEntity groundedEntity : JCasUtil.select(abstractView, GroundedEntity.class)) {
            abstractEntities.add(groundedEntity.getKnowledgeBaseId());
        }

        int index = 0;

        TObjectIntMap<String> entityCounts = new TObjectIntHashMap<>();
        for (GroundedEntity groundedEntity : JCasUtil.select(mainView, GroundedEntity.class)) {
            entityCounts.adjustOrPutValue(groundedEntity.getKnowledgeBaseId(), 1, 1);
        }

        Body body = JCasUtil.selectSingle(mainView, Body.class);

        for (GroundedEntity groundedEntity : JCasUtil.selectCovered(GroundedEntity.class, body)) {
            String id = groundedEntity.getKnowledgeBaseId();
            int saliency = abstractEntities.contains(id) ? 1 : 0;

            StringBuilder sb = new StringBuilder();
            sb.append(index);
            sb.append("\t");
            sb.append(saliency);
            sb.append("\t");
            sb.append(entityCounts.get(id));
            sb.append("\t");
            sb.append(groundedEntity.getCoveredText());

            int begin;
            int end;
            if (useToken) {
                Span tokenSpan = getSpaceTokenOffset(body, groundedEntity);
                begin = tokenSpan.getBegin();
                end = tokenSpan.getEnd();
            } else {
                begin = groundedEntity.getBegin();
                end = groundedEntity.getEnd();
            }

            sb.append("\t").append(begin).append("\t").append(end).append("\t").append(id).append("\n");

            index++;
            output.write(sb.toString());

        }
        output.write("\n");
    }

    private void writeTagged(JCas aJCas, Writer output) throws IOException {
        Gson gson = new Gson();

        Headline title = JCasUtil.selectSingle(aJCas, Headline.class);
        Body body = JCasUtil.selectSingle(aJCas, Body.class);

        List<Spot> titleSpots = getSpots(title);
        List<Spot> bodySpots = getSpots(body);

        JCas abstractView = JCasUtil.getView(aJCas, AnnotatedNytReader.ABSTRACT_VIEW_NAME, false);
        Article abstractArticle = JCasUtil.selectSingle(abstractView, Article.class);

        List<Spot> abstractSpots = getSpots(abstractArticle);

        DocStructure doc = new DocStructure();
        Spots allSpots = new Spots();
        allSpots.bodyText = bodySpots;
        allSpots.abstractSpots = abstractSpots;
        allSpots.title = titleSpots;

        doc.bodyText = asTokenized(body);
        doc.docno = UimaConvenience.getArticleName(aJCas);
        doc.spot = allSpots;
        doc.title = asTokenized(title);
        doc.abstractText = asTokenized(abstractArticle);

        String jsonStr = gson.toJson(doc);
        output.write(jsonStr + "\n");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String articleName = UimaConvenience.getArticleName(aJCas);
        if (trainDocs.contains(articleName)) {
            try {
                writeGold(aJCas, trainGoldWriter, true);
                writeGold(aJCas, trainGoldCharWriter, false);
                writeTagged(aJCas, trainTagsWriter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (testDocs.contains(articleName)) {
            try {
                writeGold(aJCas, testGoldWriter, true);
                writeGold(aJCas, testGoldCharWriter, false);
                writeTagged(aJCas, testTagsWriter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String splitName = argv[1];
        String trainingSplitFile = argv[2];
        String testSplitFile = argv[3];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, "tagged_test",
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                GoogleStyleSalienceGoldStandardWriter.class, typeSystemDescription,
                GoogleStyleSalienceGoldStandardWriter.PARAM_OUTPUT_DIR, new File(workingDir, splitName),
                GoogleStyleSalienceGoldStandardWriter.PARAM_TRAIN_SPLIT, trainingSplitFile,
                GoogleStyleSalienceGoldStandardWriter.PARAM_TEST_SPLIT, testSplitFile,
                GoogleStyleSalienceGoldStandardWriter.PARAM_OUTPUT_PREFIX, "nyt_salience"
        );

//        new BasicPipeline(reader, true, true, 1, writer).run();
        SimplePipeline.runPipeline(reader, writer);
    }
}
