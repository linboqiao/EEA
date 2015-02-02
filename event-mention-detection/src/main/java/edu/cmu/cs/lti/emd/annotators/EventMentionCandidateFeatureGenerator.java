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

    public static BiMap<String, Integer> featureNameMap;

    public static List<Pair<TIntDoubleMap, String>> featuresAndClass;

    public static Set<String> allTypes;

    private double[] emptyVector;

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

    private void loadModel() throws Exception {
        featureNameMap = (BiMap<String, Integer>) SerializationHelper.read(new File(modelDirPath, EventMentionTrainer.featureNamePath).getCanonicalPath());

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
            logger.info("Training class id : " + classId);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);

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

            if (isTraining) {
                if (goldType != null) {
                    featuresAndClass.add(Pair.with(features, goldType));
                    allTypes.add(goldType);
                } else {
                    featuresAndClass.add(Pair.with(features, OTHER_TYPE));
                }
            } else if (isOnlineTest) {
                try {
                    candidateEventMention.setPredictedType(predict(features));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    private String predict(TIntDoubleMap features) throws Exception {
        Instance instance = createInstance(features);
        double prediction = pretrainedClassifier.classifyInstance(instance);
        return classesToPredict.get((int) prediction - 1);
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
        addFeature("TriggerHeadLemma_" + triggerWord.getLemma().toLowerCase(), features);
        addFeature("HeadPOS_" + triggerWord.getPos(), features);
        if (triggerWord.getNerTag() != null) {
            addFeature("HeadNer_" + triggerWord.getNerTag(), features);
        }

        if (triggerWord.getHeadDependencyRelations() != null) {
            for (Dependency dep : FSCollectionFactory.create(triggerWord.getHeadDependencyRelations(), Dependency.class)) {
                addFeature("HeadDepType_" + dep.getDependencyType(), features);
                addFeature("HeadDepLemma_" + dep.getHead().getLemma(), features);
            }
        }

        if (triggerWord.getChildDependencyRelations() != null) {
            for (Dependency dep : FSCollectionFactory.create(triggerWord.getChildDependencyRelations(), Dependency.class)) {
                addFeature("ChildDepType_" + dep.getDependencyType(), features);
                addFeature("ChildDepLemma_" + dep.getHead().getLemma(), features);
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
                addFeature("FrameArgument_" + argument.getHeadWord().getLemma().toLowerCase(), 1.0, features);
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