package edu.cmu.cs.lti.emd.annotators.twostep;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.pipeline.twostep.EventMentionTrainer;
import edu.cmu.cs.lti.emd.utils.WordNetSenseIdentifier;
import edu.cmu.cs.lti.learning.feature.sentence.generator.EventMentionFeatureGenerator;
import edu.cmu.cs.lti.learning.feature.sentence.generator.impl.FrameArgumentLemmaFeatureGenerator;
import edu.cmu.cs.lti.learning.feature.sentence.generator.impl.FrameNameFeatureGenerator;
import edu.cmu.cs.lti.learning.feature.sentence.generator.impl.HeadWordFeatureGenerator;
import edu.cmu.cs.lti.learning.feature.sentence.generator.impl.WindowWordFeatureGenerator;
import edu.cmu.cs.lti.ling.FrameDataReader;
import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import weka.classifiers.Classifier;
import weka.core.*;
import weka.core.converters.ArffLoader.ArffReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/27/15
 * Time: 12:03 PM
 */
public class EventMentionTypeLearner extends AbstractLoggingAnnotator {


    public static final String PARAM_SEM_LINK_DIR = "semLinkDir";

    public static final String PARAM_IS_TRAINING = "isTraining";

    public static final String PARAM_MODEL_FOLDER = "modelDir";

    public static final String PARAM_MODEL_NAME_FOR_TEST = "pretrainedTestModelName";

    public static final String PARAM_ONLINE_TEST = "isOnlineTest";

    public static final String PARAM_TRAINING_DATASET_PATH = "trainingDataSetPath";

    public static final String PARAM_BROWN_CLUSTERING_PATH = "brownClusteringPath";

    public static final String PARAM_WORDNET_PATH = "wordNetPath";

    public static final String PARAM_FEAUTRE_SUBSET = "featureSubset";

    @ConfigurationParameter(name = PARAM_BROWN_CLUSTERING_PATH)
    String brownClusteringPath;

    @ConfigurationParameter(name = PARAM_SEM_LINK_DIR)
    String semLinkDirPath;

    @ConfigurationParameter(name = PARAM_WORDNET_PATH)
    private String wnDictPath;

    @ConfigurationParameter(name = PARAM_ONLINE_TEST)
    boolean isOnlineTest;

    @ConfigurationParameter(name = PARAM_IS_TRAINING)
    boolean isTraining;

    @ConfigurationParameter(name = PARAM_MODEL_FOLDER, mandatory = false)
    String modelDirPath;

    @ConfigurationParameter(name = PARAM_MODEL_NAME_FOR_TEST, mandatory = false)
    String modelNameForTest;

    @ConfigurationParameter(name = PARAM_TRAINING_DATASET_PATH, mandatory = false)
    String trainingDataSetPath;

    @ConfigurationParameter(name = PARAM_FEAUTRE_SUBSET, mandatory = false)
    Set<String> featureSubset;


    //TODO not used
    Map<String, String> vn2Fn;
    Map<String, String> pb2Vn;

    public static BiMap<String, Integer> featureNameMap;

    public static List<Pair<TIntDoubleMap, String>> featuresAndClass;

    public static Set<String> allTypes;

    private double[] emptyVector;

    private WordNetSenseIdentifier wnsi;

    private List<EventMentionFeatureGenerator> featureGenerators;


    TokenAlignmentHelper align = new TokenAlignmentHelper();

    int nextFeatureId = 0;

    ArrayList<StanfordCorenlpToken> allWords;
    public static final String OTHER_TYPE = "other_event";

    ArrayListMultimap<String, String> brownClusters;

    //in case of test
    private ArrayList<Attribute> featureConfiguration;
    private Classifier pretrainedClassifier;
    private Instances trainingDataSet;
    private List<String> classesToPredict;

    private int numDocuments = 0;
    private int[] brownClusterPrefix = {13, 16, 20};

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        vn2Fn = FrameDataReader.getFN2VNFrameMap(semLinkDirPath + "/vn-fn/VN-FNRoleMapping.txt", true);
        pb2Vn = FrameDataReader.getFN2VNFrameMap(semLinkDirPath + "/vn-pb/vnpbMappings", true);
        try {
            readBrownCluster();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            logger.info("Loading wordnet dictionary");
            WordNetSearcher wns = new WordNetSearcher(wnDictPath);
            wnsi = new WordNetSenseIdentifier(wns);
        } catch (IOException e) {
            e.printStackTrace();
        }


        allTypes = new HashSet<>();
        allTypes.add(OTHER_TYPE);
        featuresAndClass = new ArrayList<>();

        if (isTraining) {
            featureNameMap = HashBiMap.create();
        } else {
            if (modelDirPath == null) {
                throw new ResourceInitializationException(new IllegalStateException("Must provide model files if " +
                        "using test mode"));
            } else {
                try {
                    loadModel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (featureSubset == null) {
            featureSubset = new HashSet<>();
        }


    }

    private void readBrownCluster() throws IOException {
        logger.info("Loading Brown clusters");
        brownClusters = ArrayListMultimap.create();
        for (String line : FileUtils.readLines(new File(brownClusteringPath))) {
            String[] parts = line.split("\t");
            if (parts.length > 2) {
                for (int prefixLength : brownClusterPrefix) {
                    if (prefixLength <= parts[0].length()) {
                        String clusterId = parts[0].substring(0, prefixLength);
                        brownClusters.put(parts[1], clusterId);
                    }
                }
            }
        }
    }

    private void registerFeatures() {
        featureGenerators = new ArrayList<>();
        featureGenerators.add(new HeadWordFeatureGenerator(brownClusters, wnsi, featureSubset));
        featureGenerators.add(new FrameNameFeatureGenerator(featureSubset));
        featureGenerators.add(new FrameArgumentLemmaFeatureGenerator(brownClusters, wnsi, featureSubset));
        featureGenerators.add(new WindowWordFeatureGenerator(2, wnsi, allWords, featureSubset));
    }

    private void loadModel() throws Exception {
        featureNameMap = (BiMap<String, Integer>) SerializationHelper.read(new File(modelDirPath, EventMentionTrainer
                .featureNamePath).getCanonicalPath());
        logger.info("Number of features in total: " + featureNameMap.size());

        if (isOnlineTest) {
            featureConfiguration = (ArrayList<Attribute>) SerializationHelper.read(new File(modelDirPath,
                    EventMentionTrainer.featureConfigOutputName).getCanonicalPath());
            pretrainedClassifier = (Classifier) SerializationHelper.read(new File(modelDirPath, modelNameForTest)
                    .getCanonicalPath());
            emptyVector = new double[featureConfiguration.size()];
            classesToPredict = (List<String>) SerializationHelper.read(new File(modelDirPath, EventMentionTrainer
                    .predictionLabels).getCanonicalPath());

            BufferedReader reader =
                    new BufferedReader(new FileReader(trainingDataSetPath));
            ArffReader arff = new ArffReader(reader);

            logger.info("Loading data set");
            trainingDataSet = arff.getData();
            int classId = featureConfiguration.get(featureConfiguration.size() - 1).index();
            trainingDataSet.setClass(featureConfiguration.get(featureConfiguration.size() - 1));
            trainingDataSet.classAttribute();
            logger.info("Training class id : " + classId + ". Number of attributes : " + trainingDataSet
                    .numAttributes());
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);
        JCas goldView = UimaConvenience.getView(aJCas, "goldStandard");
        indexWords(aJCas);
        registerFeatures();

        numDocuments++;
        align.loadWord2Stanford(aJCas, TbfEventDataReader.COMPONENT_ID);
        align.loadFanse2Stanford(aJCas);
        for (CandidateEventMention candidateEventMention : JCasUtil.select(aJCas, CandidateEventMention.class)) {
            String goldType = candidateEventMention.getGoldStandardMentionType();
            TIntDoubleMap features = new TIntDoubleHashMap();

            for (EventMentionFeatureGenerator featureGenerator : featureGenerators) {
                Map<String, Double> generatedFeatures = featureGenerator.genFeatures(candidateEventMention);
                for (Map.Entry<String, Double> generatedFeature : generatedFeatures.entrySet()) {
                    addFeature(generatedFeature.getKey(), generatedFeature.getValue(), features);
                }
            }

            if (isOnlineTest) {
                try {
                    Pair<Double, String> prediction = predict(features);
                    String predictedType = prediction.getValue();
                    double predictionConfidence = prediction.getKey();
                    candidateEventMention.setPredictedType(predictedType);
                    candidateEventMention.setTypePredictionConfidence(predictionConfidence);

                    if (!predictedType.equals(OTHER_TYPE)) {
                        CandidateEventMention annotateMention = new CandidateEventMention(goldView,
                                candidateEventMention.getBegin(), candidateEventMention.getEnd());
                        annotateMention.setPredictedType(predictedType);
                        annotateMention.setTypePredictionConfidence(predictionConfidence);
                        annotateMention.addToIndexes();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                if (goldType != null) {
                    featuresAndClass.add(Pair.of(features, goldType));
                    if (isTraining) {
                        allTypes.add(goldType);
                    }
                } else {
                    featuresAndClass.add(Pair.of(features, OTHER_TYPE));
                }
            }
        }
    }

    private void dumpFeature(TIntDoubleMap features) {
        for (TIntDoubleIterator iter = features.iterator(); iter.hasNext(); ) {
            iter.advance();
            System.err.println(String.format("%s", featureNameMap.inverse().get(iter.key())));
        }
    }

    private Pair<Double, String> predict(TIntDoubleMap features) throws Exception {
        Instance instance = createInstance(features);
        double[] dist = pretrainedClassifier.distributionForInstance(instance);
        PriorityQueue<Pair<Double, String>> rankList = new PriorityQueue<>(dist.length, Collections.reverseOrder());

        for (int i = 1; i < dist.length; i++) {
            rankList.add(Pair.of(dist[i], classesToPredict.get(i - 1)));
        }

        Pair<Double, String> currentBest = rankList.poll();
        Pair<Double, String> secondBest = rankList.poll();
        Pair<Double, String> thirdBest = rankList.poll();

//        if (currentBest.getRight().equals(OTHER_TYPE)){
////            System.out.println("Predicate as " + "[Other]");
//            return secondBest;
//        }

        return currentBest;
    }

    private Instance createInstance(TIntDoubleMap features) {
        Instance instance = new SparseInstance(1, emptyVector);
        instance.setDataset(trainingDataSet);
        instance.setClassMissing();

        for (TIntDoubleIterator fIter = features.iterator(); fIter.hasNext(); ) {
            fIter.advance();
            int featureId = fIter.key();
            double featureVal = fIter.value();
            instance.setValue(featureConfiguration.get(featureId), featureVal);
        }

        return instance;
    }


    private void indexWords(JCas aJCas) {
        allWords = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpToken.class));
        for (StanfordEntityMention mention : JCasUtil.select(aJCas, StanfordEntityMention.class)) {
            String entityType = mention.getEntityType();
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
                token.setNerTag(entityType);
            }
        }
    }


    private int getFeatureId(String featureName) {
        int featureId;

        if (isTraining) {
            if (featureNameMap.containsKey(featureName)) {
                featureId = featureNameMap.get(featureName);
            } else {
                featureId = nextFeatureId;
                nextFeatureId++;
                featureNameMap.put(featureName, featureId);
            }
        } else {
            if (featureNameMap.containsKey(featureName)) {
                featureId = featureNameMap.get(featureName);
            } else {
                return -1;//no such feature
            }
        }
        return featureId;
    }

    private void addFeature(String featureName, double featureVal, TIntDoubleMap features) {
        int featureId = getFeatureId(featureName);
        if (featureId >= 0) {
            features.adjustOrPutValue(featureId, featureVal, featureVal);
        }
    }

    private void addFeature(String featureName, TIntDoubleMap features) {
        int featureId = getFeatureId(featureName);
        if (featureId >= 0) {
            features.adjustOrPutValue(featureId, 1.0, 1.0);
        }
    }


    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Processed " + numDocuments + " documents for " + (isTraining ? "training" : "testing"));
    }
}