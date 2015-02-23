package edu.cmu.cs.lti.emd.annotators;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.cmu.cs.lti.collection_reader.EventMentionDetectionDataReader;
import edu.cmu.cs.lti.emd.pipeline.EventMentionTrainer;
import edu.cmu.cs.lti.ling.FrameDataReader;
import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
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
public class EventMentionCandidateFeatureGenerator extends AbstractLoggingAnnotator {

    public static final String PARAM_SEM_LINK_DIR = "semLinkDir";

    public static final String PARAM_IS_TRAINING = "isTraining";

    public static final String PARAM_MODEL_FOLDER = "modelDir";

    public static final String PARAM_MODEL_NAME_FOR_TEST = "pretrainedTestModelName";

    public static final String PARAM_ONLINE_TEST = "isOnlineTest";

    public static final String PARAM_TRAINING_DATASET_PATH = "trainingDataSetPath";

    public static final String PARAM_BROWN_CLUSTERING_PATH = "brownClusteringPath";

    public static final String PARAM_WORDNET_PATH = "wordNetPath";

    public static BiMap<String, Integer> featureNameMap;

    public static List<Pair<TIntDoubleMap, String>> featuresAndClass;

    public static Set<String> allTypes;

    private double[] emptyVector;

    private WordNetSearcher wns;

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

    //TODO not used
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

    private String[] injuryRelatedSenses = {"body_part"};
    private String[] physicalSense = {"artifact", "whole", "component"};
    private String[] intangibleAssets = {"possession", "transferred_property", "liabilities", "assets"};

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
            wns = new WordNetSearcher(wnDictPath);
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
        logger.info("Loading Brown clusters");
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
            String goldType = candidateEventMention.getGoldStandardMentionType();
            TIntDoubleMap features = new TIntDoubleHashMap();
            StanfordCorenlpToken candidateHead = candidateEventMention.getHeadWord();

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

                    if (!predictedType.equals(OTHER_TYPE)) {
                        CandidateEventMention annotateMention =
                                new CandidateEventMention(goldView, candidateEventMention.getBegin(), candidateEventMention.getEnd());
                        annotateMention.setPredictedType(predictedType);
                        annotateMention.setTypePredictionConfidence(predictionConfidence);
                        annotateMention.addToIndexes();
                    }

//                    if (goldType != null && goldType.equals("Transaction_Transfer-Ownership")) {
//                        System.err.println(goldType + " " + predictedType);
//                        System.err.println(candidateEventMention.getCoveredText());
//                        dumpFeature(features);
//                        edu.cmu.cs.lti.utils.Utils.pause();
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                if (goldType != null) {
                    featuresAndClass.add(Pair.with(features, goldType));
                    if (isTraining) {
                        allTypes.add(goldType);
//                        if (goldType.equals("Transaction_Transfer-Ownership")) {
//                            dumpFeature(features);
//                        }
                    }
                } else {
                    featuresAndClass.add(Pair.with(features, OTHER_TYPE));
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
            rankList.add(Pair.with(dist[i], classesToPredict.get(i - 1)));
        }

        Pair<Double, String> currentBest = rankList.poll();
        Pair<Double, String> secondBest = rankList.poll();
        Pair<Double, String> thirdBest = rankList.poll();


//            System.err.println("Current Best" + currentBest);
//            System.err.println("Second Best" + secondBest);
//            System.err.println("Third Best" + thirdBest);


        return currentBest;

//        if (currentBest.getValue1().equals(OTHER_TYPE) && currentBest.getValue0() < 0.8) {
//            if (secondBest.getValue0() > thirdBest.getValue0() * 2) {
//                return secondBest;
//            } else {
//                return currentBest;
//            }
//        } else {
//            return currentBest;
//        }


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
            addFeature("HeadWordSurfaceBrownCluster_" + brownClusters.get(triggerWord.getCoveredText()), features);
        }

        if (triggerWord.getNerTag() != null) {
            addFeature("HeadNer_" + triggerWord.getNerTag(), features);
        }

        if (triggerWord.getHeadDependencyRelations() != null) {
            for (Dependency dep : FSCollectionFactory.create(triggerWord.getHeadDependencyRelations(), Dependency.class)) {
//                addFeature("HeadDepType_" + dep.getDependencyType(), features);
//                addFeature("HeadDepLemma_" + dep.getHead().getLemma(), features);
                String headLemma = dep.getHead().getLemma();
                String headSurface = dep.getHead().getCoveredText();

                addFeature("HeadDepLemma_" + dep.getDependencyType() + "_" + headLemma, features);
                if (dep.getHead().getNerTag() != null) {
                    addFeature("HeadDepNer_" + dep.getDependencyType() + "_" + dep.getHead().getNerTag(), features);
                }

                addFeature("HeadDepPos_" + dep.getDependencyType() + "_" + dep.getHead().getPos(), features);

                if (brownClusters.containsKey(headLemma)) {
                    addFeature("HeadDepLemmaBrownCluster_" + brownClusters.get(headLemma), features);
                }

                if (brownClusters.containsKey(headSurface)) {
                    addFeature("HeadDepSurfaceBrownCluster_" + brownClusters.get(headSurface), features);
                }

                for (String superType : getInterestingSupertype(headLemma.toLowerCase())) {
                    String superTypeWordFeatupre = "HeadDepLemmaSuperType_" + superType +
                            "_get_" + triggerWord.getLemma().toLowerCase();
                    String superTypeDepWordFeature = "HeadDepLemmaSuperType_" + superType + "_" +
                            dep.getDependencyType() + "_" + triggerWord.getLemma().toLowerCase();

                    addFeature("HeadDepLemmaSuperType_" + superType, features);
                    addFeature(superTypeDepWordFeature, features);
                    addFeature(superTypeWordFeatupre, features);

                }
            }
        }

        if (triggerWord.getChildDependencyRelations() != null) {
            for (Dependency dep : FSCollectionFactory.create(triggerWord.getChildDependencyRelations(), Dependency.class)) {
//                addFeature("ChildDepType_" + dep.getDependencyType(), features);
//                addFeature("ChildDepLemma_" + dep.getChild().getLemma(), features);

                String childLemma = dep.getChild().getLemma();
                String childSurface = dep.getChild().getCoveredText();

                addFeature("ChildDepLemma_" + dep.getDependencyType() + "_" + childLemma, features);
                if (dep.getChild().getNerTag() != null) {
                    addFeature("ChildDepNer_" + dep.getDependencyType() + "_" + dep.getChild().getNerTag(), features);
                }

                addFeature("ChildDepPos_" + dep.getDependencyType() + "_" + dep.getChild().getPos(), features);

                if (brownClusters.containsKey(childLemma)) {
                    addFeature("ChildDepLemmaBrownCluster_" + brownClusters.get(childLemma), features);
                }

                if (brownClusters.containsKey(childSurface)) {
                    addFeature("ChildDepSurfaceBrownCluster_" + brownClusters.get(childSurface), features);
                }

                for (String superType : getInterestingSupertype(childLemma.toLowerCase())) {
                    String superTypeFeature = "ChildDepLemmaSuperType_" + superType;
                    String superTypeAndWordFeature = "ChildDepLemmaSuperType_" + superType + "_get_" + triggerWord.getLemma().toLowerCase();
                    String superTypeDepWordFeature = "ChildDepLemmaSuperType_" + superType + "_" +
                            dep.getDependencyType() + "_" + triggerWord.getLemma().toLowerCase();

                    addFeature(superTypeFeature, features);
                    addFeature(superTypeAndWordFeature, features);
                    addFeature(superTypeDepWordFeature, features);
                }
            }
        }
    }

    private List<String> getInterestingSupertype(String word) {
        List<String> interestTypes = new ArrayList<>();
        for (String interestingWordType : injuryRelatedSenses) {
            for (Set<String> hypernyms : wns.getAllHypernymsForAllSense(word)) {
                if (hypernyms.contains(interestingWordType)) {
                    interestTypes.add(interestingWordType);
                }
            }
        }
        return interestTypes;
    }

    private void addSurroundingWordFeatures(Word word, int windowSize, TIntDoubleMap features) {
        int centerId = wordIds.get(word);
        int leftLimit = centerId - windowSize > 0 ? centerId - windowSize : 0;
        int rightLimit = centerId + windowSize < allWords.size() - 1 ? centerId + windowSize : allWords.size() - 1;
        for (int i = centerId; i >= leftLimit; i--) {
            addWindowWordFeature(allWords.get(i), features);
        }
        for (int i = centerId; i <= rightLimit; i++) {
            addWindowWordFeature(allWords.get(i), features);
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

    private void addWindowWordFeature(StanfordCorenlpToken word, TIntDoubleMap features) {
//        addFeature("WindowPOS_" + word.getPos(), features);

        if (!word.getPos().equals(".") && !word.getPos().equals(",") && !word.getPos().equals(":")) {
            addFeature("WindowLemma_" + word.getLemma(), features);
        }

        if (word.getNerTag() != null) {
            addFeature("WindowNer_" + word.getNerTag(), features);
        }

        for (String wordType : getInterestingSupertype(word.getLemma().toLowerCase())) {
            addFeature("WindowSuperType_" + wordType, features);
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
            boolean allHuman = true;
            for (CandidateEventMentionArgument argument : FSCollectionFactory.create(argumentFs, CandidateEventMentionArgument.class)) {
                StanfordCorenlpToken argumentHeadWord = argument.getHeadWord();
                addFeature("FrameArgument_" + argumentHeadWord.getLemma().toLowerCase(), 1.0, features);

                addFeature("SubPhrase_" + mention.getHeadWord().getLemma().toLowerCase() + "_" + argumentHeadWord.getLemma().toLowerCase(), 1.0, features);

                addFeature("FrameArgumentRole_" + argument.getRoleName(), features);

                int objectStatus = isPhysicalArtifacts(argumentHeadWord.getLemma().toLowerCase());

                if (objectStatus == 1) {
                    addFeature("FrameArgument_isPhysical", features);
                } else if (objectStatus == -1) {
                    addFeature("FrameArgument_isIntangible", features);
                }

                if (isHumanProunoun(argumentHeadWord)) {
                    addFeature("FrameArgument_isHuman", features);
                    addFeature("FrameArgument_isHuman_" + argument.getRoleName(), features);
                }

                if (brownClusters.containsKey(argumentHeadWord.getLemma())) {
                    addFeature("FrameArgumentBrownCluster_" + brownClusters.get(argumentHeadWord.getLemma()), 1.0, features);
                }
                if (argumentHeadWord.getNerTag() != null) {
                    addFeature("FrameArgumentHeadNer_" + argumentHeadWord.getNerTag(), 1.0, features);
                }

                addFeature("FrameArgument_POS_" + argumentHeadWord.getPos(), 1.0, features);

                for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, argument)) {
                    if (token.getNerTag() != null) {
                        addFeature("FrameArgumentSpanNer_" + token.getNerTag(), 1.0, features);
                    }
                }
            }
        }
    }

    private int isPhysicalArtifacts(String word) {
        for (Set<String> hypernyms : wns.getAllHypernymsForAllSense(word)) {
            for (String intangible : intangibleAssets) {
                if (hypernyms.contains(intangible)) {
                    return -1;
                }
            }

            for (String physical : physicalSense) {
                if (hypernyms.contains(physical)) {
                    return 1;
                }
            }
        }
        return 0;
    }

    private boolean isHumanProunoun(StanfordCorenlpToken token) {
        if (token.getPos().equals("PPS") && !token.getLemma().equals("it")) {
            return true;
        }
        return false;
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