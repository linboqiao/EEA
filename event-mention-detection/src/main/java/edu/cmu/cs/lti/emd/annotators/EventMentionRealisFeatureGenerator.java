package edu.cmu.cs.lti.emd.annotators;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.cmu.cs.lti.collection_reader.EventMentionDetectionDataReader;
import edu.cmu.cs.lti.emd.pipeline.EventMentionTrainer;
import edu.cmu.cs.lti.ling.FrameDataReader;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.resource.ResourceInitializationException;
import org.javatuples.Pair;
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
public class EventMentionRealisFeatureGenerator extends AbstractLoggingAnnotator {

    public static final String PARAM_SEM_LINK_DIR = "semLinkDir";

    public static final String PARAM_IS_TRAINING = "isTraining";

    public static final String PARAM_MODEL_FOLDER = "modelDir";

    public static final String PARAM_MODEL_NAME_FOR_TEST = "pretrainedTestModelName";

    public static final String PARAM_ONLINE_TEST = "isOnlineTest";

    public static final String PARAM_TRAINING_DATASET_PATH = "trainingDataSetPath";

    public static final String PARAM_BROWN_CLUSTERING_PATH = "brownClusteringPath";

    public static BiMap<String, Integer> featureNameMap;

    public static List<Pair<TIntDoubleMap, String>> featuresAndClass;

    public static Set<String> allTypes;

    private double[] emptyVector;

    @ConfigurationParameter(name = PARAM_BROWN_CLUSTERING_PATH)
    String brownClusteringPath;

    @ConfigurationParameter(name = PARAM_SEM_LINK_DIR)
    String semLinkDirPath;

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

    Map<String, String> vn2Fn;

    Map<String, String> pb2Vn;

    TokenAlignmentHelper align = new TokenAlignmentHelper();

    int nextFeatureId = 0;

    Map<Word, Integer> wordIds;
    ArrayList<StanfordCorenlpToken> allWords;
    public static final String OTHER_TYPE = "other_event";

    Map<String, String> brownClusters;

    //in case of test
    private ArrayList<Attribute> featureConfiguration;
    private Classifier pretrainedClassifier;
    private Instances trainingDataSet;
    private List<String> classesToPredict;

    private int numDocuments = 0;

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
        allTypes = new HashSet<>();
        allTypes.add(OTHER_TYPE);
        featuresAndClass = new ArrayList<>();

        if (isTraining) {
            featureNameMap = HashBiMap.create();
        } else {
            if (modelDirPath == null) {
                throw new ResourceInitializationException(new IllegalStateException("Must provide model files if using test mode"));
            } else {
                try {
                    loadModel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void readBrownCluster() throws IOException {
        brownClusters = new HashMap<>();
        for (String line : FileUtils.readLines(new File(brownClusteringPath))) {
            String[] parts = line.split("\t");
            if (parts.length > 2) {
                brownClusters.put(parts[1], parts[0]);
            }
        }
    }

    private void loadModel() throws Exception {
        featureNameMap = (BiMap<String, Integer>) SerializationHelper.read(new File(modelDirPath, EventMentionTrainer.featureNamePath).getCanonicalPath());
        logger.info("Number of features in total: " + featureNameMap.size());

        if (isOnlineTest) {
            featureConfiguration = (ArrayList<Attribute>) SerializationHelper.read(new File(modelDirPath, EventMentionTrainer.featureConfigOutputName).getCanonicalPath());
            pretrainedClassifier = (Classifier) SerializationHelper.read(new File(modelDirPath, modelNameForTest).getCanonicalPath());
            emptyVector = new double[featureConfiguration.size()];
            classesToPredict = (List<String>) SerializationHelper.read(new File(modelDirPath, EventMentionTrainer.predictionLabels).getCanonicalPath());

            BufferedReader reader =
                    new BufferedReader(new FileReader(trainingDataSetPath));
            ArffReader arff = new ArffReader(reader);

            logger.info("Loading data set");
            trainingDataSet = arff.getData();
            int classId = featureConfiguration.get(featureConfiguration.size() - 1).index();
            trainingDataSet.setClass(featureConfiguration.get(featureConfiguration.size() - 1));
            trainingDataSet.classAttribute();
            logger.info("Training class id : " + classId + ". Number of attributes : " + trainingDataSet.numAttributes());
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);

        JCas goldView = UimaConvenience.getView(aJCas, "goldStandard");

        indexWords(aJCas);
        numDocuments++;
        align.loadWord2Stanford(aJCas, EventMentionDetectionDataReader.componentId);
        align.loadFanse2Stanford(aJCas);
        for (CandidateEventMention candidateEventMention : JCasUtil.select(aJCas, CandidateEventMention.class)) {
//            String goldType = candidateEventMention.getGoldStandardMentionType();
            if (candidateEventMention.getGoldStandardMentionType() == null) {
                continue;
            }

            String goldRealis = candidateEventMention.getGoldRealis();

            TIntDoubleMap features = new TIntDoubleHashMap();
            StanfordCorenlpToken candidateHead = candidateEventMention.getHeadWord();

            if (isTraining) {
                addFeature("MentionType_" + candidateEventMention.getGoldStandardMentionType(), 1, features);
            } else {
                addFeature("MentionType_" + candidateEventMention.getPredictedType(), 1, features);
            }

            addHeadWordFeatures(candidateHead, features);
            addSurroundingWordFeatures(candidateHead, 2, features);
            addFrameFeatures(candidateEventMention, features);

            if (isOnlineTest) {
                try {
                    Pair<Double, String> prediction = predict(features);
                    String predictedType = prediction.getValue1();
                    double predictionConfidence = prediction.getValue0();
                    candidateEventMention.setPredictedType(predictedType);
                    candidateEventMention.setTypePredictionConfidence(predictionConfidence);

                    if (!prediction.equals(OTHER_TYPE)) {
                        CandidateEventMention annotateMention =
                                new CandidateEventMention(goldView, candidateEventMention.getBegin(), candidateEventMention.getEnd());
                        annotateMention.setPredictedType(predictedType);
                        annotateMention.setTypePredictionConfidence(predictionConfidence);
                        annotateMention.addToIndexes();
                    }

//                    System.err.println(candidateEventMention.getCoveredText());
//                    System.err.println("Gold type is " + goldType);
//                    if (goldType != null && !prediction.equals(goldType)) {
//                        if (candidateEventMention.getArguments() != null) {
//                            for (CandidateEventMentionArgument argument : FSCollectionFactory.
//                                    create(candidateEventMention.getArguments(), CandidateEventMentionArgument.class)) {
//                                System.err.println("Argument : ");
//                                System.err.println(argument.getCoveredText());
//                                System.err.println("Argument : ");
//                                System.err.println(argument.getCoveredText());
//                            }
//                        }
//                        edu.cmu.cs.lti.utils.Utils.pause();
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                if (goldRealis != null) {
                    featuresAndClass.add(Pair.with(features, goldRealis));
                    if (isTraining) {
                        allTypes.add(goldRealis);
                    }
                }
            }
        }
    }

    private Pair<Double, String> predict(TIntDoubleMap features) throws Exception {
        Instance instance = createInstance(features);

        double[] dist = pretrainedClassifier.distributionForInstance(instance);

        PriorityQueue<Pair<Double, String>> rankList = new PriorityQueue<>(dist.length, Collections.reverseOrder());

        for (int i = 1; i < dist.length; i++) {
            rankList.add(Pair.with(dist[i], classesToPredict.get(i - 1)));
        }

        Pair<Double, String> currentBest = rankList.poll();

        if (currentBest.getValue1().equals(OTHER_TYPE) && currentBest.getValue0() < 0.8) {
            Pair<Double, String> secondBest = rankList.poll();
            Pair<Double, String> thirdBest = rankList.poll();

            if (secondBest.getValue0() > thirdBest.getValue0() * 2) {
                return secondBest;
            } else {
                return currentBest;
            }
        } else {
            return currentBest;
        }


//        for (int i = 0; i < 5; i++) {
//            System.err.println("Best " + (i + 1) + " " + rankList.poll());
//        }
//
//
//        double prediction = pretrainedClassifier.classifyInstance(instance);
//        return classesToPredict.get((int) prediction - 1);
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

    private void addHeadWordFeatures(StanfordCorenlpToken triggerWord, TIntDoubleMap features) {
        String lemma = triggerWord.getLemma().toLowerCase();

        addFeature("TriggerHeadLemma_" + lemma, features);
        addFeature("HeadPOS_" + triggerWord.getPos(), features);

        if (brownClusters.containsKey(lemma)) {
            addFeature("HeadWordLemmaBrownCluster_" + brownClusters.get(lemma), features);
        }

        if (brownClusters.containsKey(triggerWord.getCoveredText())) {
            addFeature("HeadWordSurfaceBrownCluster_" + brownClusters.get(lemma), features);
        }

        if (triggerWord.getNerTag() != null) {
            addFeature("HeadNer_" + triggerWord.getNerTag(), features);
        }

        if (triggerWord.getHeadDependencyRelations() != null) {
            for (Dependency dep : FSCollectionFactory.create(triggerWord.getHeadDependencyRelations(), Dependency.class)) {
//                addFeature("HeadDepType_" + dep.getDependencyType(), features);
//                addFeature("HeadDepLemma_" + dep.getHead().getLemma(), features);
                if (dep.getHead().getNerTag() != null) {
                    addFeature("HeadDep_" + dep.getDependencyType() + "_" + dep.getHead().getLemma(), features);
                }
            }
        }

        if (triggerWord.getChildDependencyRelations() != null) {
            for (Dependency dep : FSCollectionFactory.create(triggerWord.getChildDependencyRelations(), Dependency.class)) {
//                addFeature("ChildDepType_" + dep.getDependencyType(), features);
//                addFeature("ChildDepLemma_" + dep.getChild().getLemma(), features);
                addFeature("ChildDep_" + dep.getDependencyType() + "_" + dep.getChild().getLemma(), features);
                if (dep.getChild().getNerTag() != null) {
                    addFeature("ChildDepNer_" + dep.getDependencyType() + "_" + dep.getChild().getNerTag(), features);
                }
            }
        }
    }

    private void addSurroundingWordFeatures(Word word, int windowSize, TIntDoubleMap features) {
        int centerId = wordIds.get(word);
        int leftLimit = centerId - windowSize > 0 ? centerId - windowSize : 0;
        int rightLimit = centerId + windowSize < allWords.size() - 1 ? centerId + windowSize : allWords.size() - 1;
        for (int i = centerId; i >= leftLimit; i--) {
            addWordFeature(allWords.get(i), features);
        }
        for (int i = centerId; i <= rightLimit; i++) {
            addWordFeature(allWords.get(i), features);
        }
    }


    private void indexWords(JCas aJCas) {
        allWords = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpToken.class));
        wordIds = new HashMap<>();
        int i = 0;
        for (Word word : allWords) {
            wordIds.put(word, i++);
        }

        for (StanfordEntityMention mention : JCasUtil.select(aJCas, StanfordEntityMention.class)) {
            String entityType = mention.getEntityType();
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
                token.setNerTag(entityType);
            }
        }
    }

    private void addWordFeature(StanfordCorenlpToken word, TIntDoubleMap features) {
        addFeature("WindowPOS_" + word.getPos(), features);
        addFeature("WindowLemma_" + word.getLemma(), features);
        if (word.getNerTag() != null) {
            addFeature("WindowNer_" + word.getNerTag(), features);
        }
    }

    private void addFrameFeatures(CandidateEventMention mention, TIntDoubleMap features) {
        StringList potentialFrameFs = mention.getPotentialFrames();

        if (potentialFrameFs != null) {
            for (String potentialFrame : FSCollectionFactory.create(potentialFrameFs)) {
                addFeature("FrameName_" + potentialFrame, 1.0, features);
            }
        }

        FSList argumentFs = mention.getArguments();
        if (argumentFs != null) {
            for (CandidateEventMentionArgument argument : FSCollectionFactory.create(argumentFs, CandidateEventMentionArgument.class)) {
                StanfordCorenlpToken argumentHeadWord = argument.getHeadWord();
                addFeature("FrameArgument_" + argumentHeadWord.getLemma().toLowerCase(), 1.0, features);
                if (brownClusters.containsKey(argumentHeadWord.getLemma())) {
                    addFeature("FrameArgumentBrownCluster_" + brownClusters.get(argumentHeadWord.getLemma()), 1.0, features);
                }
                if (argumentHeadWord.getNerTag() != null) {
                    addFeature("FrameArgumentHeadNer_" + argumentHeadWord.getNerTag(), 1.0, features);
                }

                for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, argument)) {
                    if (token.getNerTag() != null) {
                        addFeature("FrameArgumentSpanNer_" + token.getNerTag(), 1.0, features);
                    }
                }
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