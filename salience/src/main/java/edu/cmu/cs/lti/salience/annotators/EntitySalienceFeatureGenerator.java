package edu.cmu.cs.lti.salience.annotators;

import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.salience.utils.FeatureUtils;
import edu.cmu.cs.lti.salience.utils.LookupTable;
import edu.cmu.cs.lti.salience.utils.SalienceUtils;
import edu.cmu.cs.lti.salience.utils.TextUtils;
import edu.cmu.cs.lti.script.type.Headline;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
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
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.util.JCasUtil;
import org.xml.sax.SAXException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/18/17
 * Time: 2:42 PM
 *
 * @author Zhengzhong Liu
 */
public class EntitySalienceFeatureGenerator extends AbstractLoggingAnnotator {
    private BufferedWriter trainInstanceWriter;
    private BufferedWriter testInstanceWriter;

    public static final String PARAM_FEATURE_OUTPUT_DIR = "featureOutput";
    @ConfigurationParameter(name = PARAM_FEATURE_OUTPUT_DIR)
    private File featureOutputDir;

    public static final String PARAM_TEST_SPLIT = "testSplit";
    @ConfigurationParameter(name = PARAM_TEST_SPLIT)
    private File testSplitFile;

    public static final String PARAM_TRAIN_SPLIT = "trainSplit";
    @ConfigurationParameter(name = PARAM_TRAIN_SPLIT)
    private File trainSplitFile;

    public static final String PARAM_ENTITY_EMBEDDING = "entityEmbeddingFile";
    @ConfigurationParameter(name = PARAM_ENTITY_EMBEDDING)
    private File entityEmbeddingFile;

    private Set<String> trainDocs;
    private Set<String> testDocs;

    private LookupTable.SimCalculator simCalculator;

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
            if (!featureOutputDir.exists()) {
                featureOutputDir.mkdirs();
            }

            File trainFile = new File(featureOutputDir, "train.tsv");
            File testFile = new File(featureOutputDir, "test.tsv");
            logger.info("Writing training instances to " + trainFile);
            logger.info("Writing test instances to " + testFile);

            trainInstanceWriter = new BufferedWriter(new FileWriter(trainFile));
            testInstanceWriter = new BufferedWriter(new FileWriter(testFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            LookupTable table = SalienceUtils.loadEmbedding(entityEmbeddingFile);
            simCalculator = new LookupTable.SimCalculator(table);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        StringBuilder sb = new StringBuilder();
        Headline headline = JCasUtil.selectSingle(aJCas, Headline.class);

        String docid = UimaConvenience.getArticleName(aJCas);
        sb.append(docid).append("\t").append(TextUtils.asTokenized(headline)).append("\n");

        List<FeatureUtils.SimpleInstance> instances = FeatureUtils.getKbInstances(aJCas, simCalculator);

        for (FeatureUtils.SimpleInstance instance : instances) {
            sb.append(instance.toString()).append("\t");
        }

        sb.append("\n");

        String articleName = UimaConvenience.getArticleName(aJCas);
        try {
            if (trainDocs.contains(articleName)) {
                trainInstanceWriter.write(sb.toString());
            } else if (testDocs.contains(articleName)) {
                testInstanceWriter.write(sb.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            trainInstanceWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String featureOutput = argv[1];
        String trainingSplitFile = argv[2];
        String testSplitFile = argv[3];
        String entityEmbeddingFile = argv[4];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, "tagged",
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );

        AnalysisEngineDescription generator = AnalysisEngineFactory.createEngineDescription(
                EntitySalienceFeatureGenerator.class, typeSystemDescription,
                EntitySalienceFeatureGenerator.PARAM_FEATURE_OUTPUT_DIR, featureOutput,
                EntitySalienceFeatureGenerator.PARAM_TRAIN_SPLIT, trainingSplitFile,
                EntitySalienceFeatureGenerator.PARAM_TEST_SPLIT, testSplitFile,
                EntitySalienceFeatureGenerator.PARAM_ENTITY_EMBEDDING, entityEmbeddingFile,
                EntitySalienceFeatureGenerator.MULTI_THREAD, true
        );

        new BasicPipeline(reader, true, true, 7, generator).run();
    }
}
