package edu.cmu.cs.lti.salience.stats;

import edu.cmu.cs.lti.collection_reader.AnnotatedNytReader;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.salience.utils.SalienceUtils;
import edu.cmu.cs.lti.script.type.GroundedEntity;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
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
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/7/17
 * Time: 11:26 AM
 *
 * @author Zhengzhong Liu
 */
public class NytEntityCounter extends AbstractLoggingAnnotator {
    public static final String PARAM_TEST_SPLIT = "testSplit";
    @ConfigurationParameter(name = PARAM_TEST_SPLIT)
    private File testSplitFile;

    public static final String PARAM_TRAIN_SPLIT = "trainSplit";
    @ConfigurationParameter(name = PARAM_TRAIN_SPLIT)
    private File trainSplitFile;

    public static final String PARAM_RESULT_FILE = "result";
    @ConfigurationParameter(name = PARAM_RESULT_FILE)
    private File resultFile;

    private Set<String> trainDocs;
    private Set<String> testDocs;

    private TObjectIntMap<String> trainIdCounts;
    private TObjectIntMap<String> trainSalienceCounts;

    private TObjectIntMap<String> testIdCounts;
    private TObjectIntMap<String> testSalienceCounts;


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            trainDocs = SalienceUtils.readSplit(trainSplitFile);
            testDocs = SalienceUtils.readSplit(testSplitFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        trainIdCounts = new TObjectIntHashMap<>();
        trainSalienceCounts = new TObjectIntHashMap<>();
        testIdCounts = new TObjectIntHashMap<>();
        testSalienceCounts = new TObjectIntHashMap<>();

        File parent = resultFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas abstractView = JCasUtil.getView(aJCas, AnnotatedNytReader.ABSTRACT_VIEW_NAME, false);
        Set<String> mainIds = getKnowledgeIds(aJCas);
        Set<String> abstractIds = getKnowledgeIds(abstractView);

        String articleName = UimaConvenience.getArticleName(aJCas);

        for (String id : mainIds) {
            if (trainDocs.contains(articleName)) {
                incrementMap(trainIdCounts, id);
                if (abstractIds.contains(id)) {
                    incrementMap(trainSalienceCounts, id);
                }
            } else if (testDocs.contains(articleName)) {
                incrementMap(testIdCounts, id);
                if (abstractIds.contains(id)) {
                    incrementMap(testSalienceCounts, id);
                }
            }
        }
    }

    private synchronized void incrementMap(TObjectIntMap<String> counts, String key) {
        counts.adjustOrPutValue(key, 1, 1);
    }


    private Set<String> getKnowledgeIds(JCas aJCas) {
        Set<String> ids = new HashSet<>();
        for (GroundedEntity groundedEntity : JCasUtil.select(aJCas, GroundedEntity.class)) {
            ids.add(groundedEntity.getKnowledgeBaseId());
        }
        return ids;
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();

        try {
            Writer writer = new BufferedWriter(new FileWriter(resultFile));
            Set<String> trainIds = new HashSet<>(trainIdCounts.keySet());
            Set<String> testIds = testIdCounts.keySet();

            trainIds.addAll(testIds);

            writer.write(String.format("%s\t%s\t%s\t%s\t%s\n", "id", "train_main", "train_salience", "test_main",
                    "test_salience"));
            for (String trainId : trainIds) {
                writer.write(String.format("%s\t%d\t%d\t%d\t%d\n", trainId, trainIdCounts.get(trainId),
                        trainSalienceCounts.get(trainId), testIdCounts.get(trainId), testSalienceCounts.get(trainId)));
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String input = argv[1];
        String result = argv[2];
        String trainingSplitFile = argv[3];
        String testSplitFile = argv[4];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, input,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz",
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true
        );

        AnalysisEngineDescription counter = AnalysisEngineFactory.createEngineDescription(
                NytEntityCounter.class, typeSystemDescription,
                NytEntityCounter.PARAM_RESULT_FILE, result,
                NytEntityCounter.PARAM_TRAIN_SPLIT, trainingSplitFile,
                NytEntityCounter.PARAM_TEST_SPLIT, testSplitFile,
                NytEntityCounter.MULTI_THREAD, true
        );

        new BasicPipeline(reader, true, true, 7, counter).run();

    }
}
