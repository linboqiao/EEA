package edu.cmu.cs.lti.emd.pipeline.twostep;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;
import edu.cmu.cs.lti.emd.annotators.EvaluationResultWriter;
import edu.cmu.cs.lti.emd.annotators.twostep.EventMentionTypeLearner;
import edu.cmu.cs.lti.emd.eval.EventMentionEvalRunner;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.pipeline.SimplePipeline;
import weka.classifiers.Classifier;
import weka.core.Instances;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/5/15
 * Time: 4:23 PM
 */
public class EventMentionTypeFeatureAblation {
    String semLinkDataPath;
    String bwClusterPath;
    String wordnetDataPath;
    TypeSystemDescription typeSystemDescription;
    EventMentionEvalRunner runner = new EventMentionEvalRunner();


    public EventMentionTypeFeatureAblation(String semLinkDataPath,
                                           String bwClusterPath,
                                           String wordnetDataPath,
                                           TypeSystemDescription typeSystemDescription) {
        this.semLinkDataPath = semLinkDataPath;
        this.bwClusterPath = bwClusterPath;
        this.wordnetDataPath = wordnetDataPath;
        this.typeSystemDescription = typeSystemDescription;
    }

    private Map<String, Set<String>> configFeatureSubset() {
        Map<String, Set<String>> allSubsets = new HashMap<>();

        Set<String> basicFeatures = Sets.newHashSet("TriggerHeadLemma", "HeadPOS", "HeadWordLemmaBrownCluster", "HeadWordSurfaceBrownCluster", "HeadNer");
        Set<String> basicFeaturesWithoutPos = Sets.newHashSet("TriggerHeadLemma", "HeadPOS", "HeadWordLemmaBrownCluster", "HeadWordSurfaceBrownCluster", "HeadNer");
        Set<String> dictionaryFeatures = Sets.newHashSet("TriggerHeadLemma", "FrameName");

        Set<String> headHeadDepFeatures = Sets.newHashSet("HeadDepLemma", "HeadDepNer", "HeadDepPos", "HeadDepLemmaBrownCluster", "HeadDepSurfaceBrownCluster",
                "HeadDepLemmaSuperType", "HeadDepLemmaSuperTypeWithLemma", "HeadDepLemmaSuperTypeWithDepLemma");
        headHeadDepFeatures.addAll(basicFeatures);

        Set<String> headChildDepFeatures = Sets.newHashSet("ChildDepLemma", "ChildDepNer", "ChildDepPos", "ChildDepLemmaBrownCluster",
                "ChildDepSurfaceBrownCluster", "ChildDepLemmaSuperTypeWithLemma", "ChildDepLemmaSuperTypeWithDepLemma");
        headChildDepFeatures.addAll(basicFeatures);

        Set<String> windowFeatures = Sets.newHashSet("WindowLemma", "WindowNer", "WindowSuperType");
        windowFeatures.addAll(basicFeatures);

//        Set<String> allHeadDepFeatures = new HashSet<>();
//        allHeadDepFeatures.addAll(headHeadDepFeatures);
//        allHeadDepFeatures.addAll(headChildDepFeatures);

        Set<String> frameArgumentFeatures = Sets.newHashSet("FrameArgument", "FrameArgumentSubPhrase", "FrameArgumentRole", "FrameArgumentSense",
                "FrameArgumentBrownCluster", "FrameArgumentHeadNer", "FrameArgumentPOS", "FrameArgumentSpanNer");
        frameArgumentFeatures.addAll(basicFeatures);

        Set<String> frameTargetFeatures = Sets.newHashSet("FrameName");
        frameTargetFeatures.addAll(basicFeatures);

//        Set<String> allFrameFeatures = new HashSet<>();
//        allFrameFeatures.addAll(frameArgumentFeatures);
//        allFrameFeatures.addAll(frameTargetFeatures);

//        Set<String> allFeatures = new HashSet<>();
//        allFeatures.addAll(basicFeatures);
//        allFeatures.addAll(allFrameFeatures);
//        allFeatures.addAll(windowFeatures);
//        allFeatures.addAll(headChildDepFeatures);
//        allFeatures.addAll(headChildDepFeatures);

//        Set<String> withWindowAndFrame = new HashSet<>();
//        withWindowAndFrame.addAll(basicFeatures);
//        withWindowAndFrame.addAll(windowFeatures);
//        withWindowAndFrame.addAll(allFrameFeatures);

//        Set<String> withWindowAndChild = new HashSet<>();
//        withWindowAndChild.addAll(basicFeatures);
//        withWindowAndChild.addAll(windowFeatures);
//        withWindowAndChild.addAll(headChildDepFeatures);

        Set<String> allFeatures = joinFeatures(basicFeatures, windowFeatures, frameArgumentFeatures,
                frameTargetFeatures, headChildDepFeatures, headHeadDepFeatures);

//        allSubsets.put("basic", basicFeatures);
//        allSubsets.put("dictionary_and_window", joinFeatures(dictionaryFeatures, windowFeatures));
//        allSubsets.put("withHeadDep", headHeadDepFeatures);
//        allSubsets.put("withChildep", headChildDepFeatures);
//        allSubsets.put("withWindowWords", windowFeatures);
        allSubsets.put("withAllHeadDepFeatures", joinFeatures(basicFeatures, headHeadDepFeatures, headChildDepFeatures));
        Set<String> noPosWithWindowAndDictionaryAndHeadDepLemma = joinFeatures(basicFeaturesWithoutPos, dictionaryFeatures);
        noPosWithWindowAndDictionaryAndHeadDepLemma.add("HeadDepLemma");
        allSubsets.put("withWindowAndDictionaryAndHeadDepLemma", noPosWithWindowAndDictionaryAndHeadDepLemma);
//        allSubsets.put("withFrameArgument", frameArgumentFeatures);
//        allSubsets.put("withFrameTarget", frameTargetFeatures);
//        allSubsets.put("withallFrameFeatures", joinFeatures(basicFeatures, frameArgumentFeatures, frameTargetFeatures));
        allSubsets.put("allFeatures", allFeatures);
//        allSubsets.put("withWindowAndFrame", joinFeatures(basicFeatures, windowFeatures, headChildDepFeatures));
//        allSubsets.put("withWindowAndChild", joinFeatures(basicFeatures, windowFeatures, headChildDepFeatures));
//
//        allSubsets.put("no_HeadPos_withWindowsWords", joinFeatures(basicFeaturesWithoutPos, windowFeatures));
//        allSubsets.put("no_HeadPos_withDictionary", joinFeatures(basicFeaturesWithoutPos, dictionaryFeatures));
//        allSubsets.put("no_HeadPos_withWindowsWords_withDictionary", joinFeatures(basicFeaturesWithoutPos, windowFeatures, dictionaryFeatures));

//        allSubsets.putAll(removeOneGeneration(basicFeaturesWithoutPos, "basic_no_HeadPos"));
//        allSubsets.putAll(removeOneGeneration(allFeatures, "allFeatures"));

        return allSubsets;
    }

    private Set<String> joinFeatures(Set<String>... featureSets) {
        Set<String> joinedFeatures = new HashSet<>();
        for (Set<String> featureSet : featureSets) {
            joinedFeatures.addAll(featureSet);
        }

        return joinedFeatures;

    }


    private Map<String, Set<String>> removeOneGeneration(Set<String> baseFeatures, String basename) {
        Map<String, Set<String>> removedFeatures = new HashMap<>();

        for (String removed : baseFeatures) {
            Set<String> removedSet = new HashSet<>();
            removedSet.addAll(baseFeatures);
            removedSet.remove(removed);
            removedFeatures.put(basename + "_without_" + removed, removedSet);
        }

        return removedFeatures;
    }


    private void test(String workingDir,
                      String baseDataDir,
                      String modelBaseDir,
                      String modelName,
                      String responseOutputPath) throws IOException, UIMAException {

        String modelDir = new File(workingDir, modelBaseDir).getCanonicalPath();

        AnalysisEngineDescription mention = AnalysisEngineFactory.createEngineDescription(
                EventMentionTypeLearner.class, typeSystemDescription,
                EventMentionTypeLearner.PARAM_SEM_LINK_DIR, semLinkDataPath,
                EventMentionTypeLearner.PARAM_IS_TRAINING, false,
                EventMentionTypeLearner.PARAM_MODEL_FOLDER, modelDir,
                EventMentionTypeLearner.PARAM_MODEL_NAME_FOR_TEST, modelName,
                EventMentionTypeLearner.PARAM_ONLINE_TEST, true,
                EventMentionTypeLearner.PARAM_TRAINING_DATASET_PATH, new File(modelDir, EventMentionTrainer.trainingFeatureName).getCanonicalPath(),
                EventMentionTypeLearner.PARAM_BROWN_CLUSTERING_PATH, bwClusterPath,
                EventMentionTypeLearner.PARAM_WORDNET_PATH, wordnetDataPath
        );

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(workingDir, baseDataDir, 1, false);
        AnalysisEngineDescription response = AnalysisEngineFactory.createEngineDescription(EvaluationResultWriter.class, typeSystemDescription,
                EvaluationResultWriter.PARAM_OUTPUT_PATH, responseOutputPath);
        SimplePipeline.runPipeline(reader, mention, response);
    }

    public void doAblation(String workingDir,
                           String modelBasePath,
                           String trainingBaseDir,
                           String[] testBaseDirs,
                           String[] goldStandards,
                           String tokenBasePath,
                           String evalScript,
                           String resultBaseDir
    ) throws Exception {
        EventMentionTrainer trainer = new EventMentionTrainer(semLinkDataPath, bwClusterPath, wordnetDataPath, typeSystemDescription);
//        Map<String, Double> officialResults = new HashMap<>();
        ArrayListMultimap<String, Double> officialResults = ArrayListMultimap.create();

        Map<String, Instances> testSets = new HashMap<>();

        for (Map.Entry<String, Set<String>> namedFeatureSet : configFeatureSubset().entrySet()) {
            String featureSetName = namedFeatureSet.getKey();
            Set<String> featureSet = namedFeatureSet.getValue();

            String modelPath = modelBasePath + "/" + featureSetName;

            File resultDir = new File(workingDir, resultBaseDir);
            if (!resultDir.isDirectory()) {
                resultDir.mkdirs();
            }

            Map<String, Classifier> classifers = trainer.buildModels(workingDir, modelPath,
                    trainingBaseDir, featureSet, testSets, resultDir.getCanonicalPath());

            String tokenPath = new File(workingDir, tokenBasePath).getCanonicalPath();
            for (Map.Entry<String, Classifier> classifierByName : classifers.entrySet()) {
                String classifierName = classifierByName.getKey();

                for (int i = 0; i < testBaseDirs.length; i++) {
                    String testBaseDir = testBaseDirs[i];
                    String goldStandardPath = new File(workingDir, goldStandards[i]).getCanonicalPath();
                    String responsePath = new File(resultDir, testBaseDir + "_" + featureSetName + "_" + classifierName + ".tbf").getCanonicalPath();
                    String resultPath = new File(resultDir, testBaseDir + "_" + featureSetName + "_" + classifierName + ".out").getCanonicalPath();
                    test(workingDir, testBaseDir, modelPath, classifierByName.getKey(), responsePath);
                    runner.runEval(evalScript, goldStandardPath, responsePath, tokenPath, resultPath);
                    double officialSpanF1 = runner.getMicroSpanF1();
                    double officialTypeF1 = runner.getMicroTypeF1();
                    officialResults.put(namedFeatureSet.getKey() + "_" + classifierName, officialSpanF1);
                    officialResults.put(namedFeatureSet.getKey() + "_" + classifierName, officialTypeF1);
                }
            }
        }

        System.out.println("====Summary of feature selection results====");
        System.out.println("Method\tSpan F1\tType F1");
        for (Map.Entry<String, Collection<Double>> featureSetResult : officialResults.asMap().entrySet()) {
            System.out.println(featureSetResult.getKey() + "\t" + Joiner.on("\t").join(featureSetResult.getValue()));
        }
    }

    public static void main(String[] args) throws Exception {
        String workingDir = "event-mention-detection/data/Event-mention-detection-2014";
        String tyepSystemName = "TaskEventMentionDetectionTypeSystem";
        String semLinkDataPath = "data/resources/SemLink_1.2.2c";
        String wordnetDataPath = "data/resources/wnDict";
        String brownClusteringDataPath = "data/resources/TDT5_BrownWC.txt";
        String trainingBaseDir = args[0];//"train_data";
        String modelBasePath = args[1]; //"models/train_split";
        String resultBasePath = "results";

        String[] testBaseDirs = args[2].split(","); //{"dev_data,test_data"};
        String[] goldStandardFiles = args[3].split(",");
        String tokenPath = args[4];

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(tyepSystemName);

        String evalPath = "/Users/zhengzhongliu/Documents/projects/EvmEval/scorer_v1.2.py";

        EventMentionTypeFeatureAblation ablation = new EventMentionTypeFeatureAblation(semLinkDataPath, brownClusteringDataPath, wordnetDataPath, typeSystemDescription);
        ablation.doAblation(workingDir, modelBasePath, trainingBaseDir, testBaseDirs, goldStandardFiles, tokenPath, evalPath, resultBasePath);
    }
}