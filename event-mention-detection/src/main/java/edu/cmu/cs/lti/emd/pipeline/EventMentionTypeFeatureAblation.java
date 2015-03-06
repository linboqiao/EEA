package edu.cmu.cs.lti.emd.pipeline;

import com.google.common.collect.Sets;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/5/15
 * Time: 4:23 PM
 */
public class EventMentionTypeFeatureAblation {

    private Map<String, Set<String>> configFeatureSubset() {
        Map<String, Set<String>> allSubsets = new HashMap<>();

        Set<String> basicFeatures = Sets.newHashSet("TriggerHeadLemma", "HeadPOS", "HeadWordLemmaBrownCluster", "HeadWordSurfaceBrownCluster", "HeadNer");
        Set<String> headHeadDepFeatures = Sets.newHashSet("HeadDepLemma", "HeadDepNer", "HeadDepPos", "HeadDepLemmaBrownCluster", "HeadDepSurfaceBrownCluster",
                "HeadDepLemmaSuperType", "HeadDepLemmaSuperTypeWithLemma", "HeadDepLemmaSuperTypeWithDepLemma");
        headHeadDepFeatures.addAll(basicFeatures);

        Set<String> headChildDepFeatures = Sets.newHashSet("ChildDepLemma", "ChildDepNer", "ChildDepPos", "ChildDepLemmaBrownCluster",
                "ChildDepSurfaceBrownCluster", "ChildDepLemmaSuperTypeWithLemma", "ChildDepLemmaSuperTypeWithDepLemma");
        headChildDepFeatures.addAll(basicFeatures);


        Set<String> windowFeatures = Sets.newHashSet("WindowLemma", "WindowNer", "WindowSuperType");
        windowFeatures.addAll(basicFeatures);


        Set<String> allHeadDepFeatures = new HashSet<>();
        allHeadDepFeatures.addAll(headHeadDepFeatures);
        allHeadDepFeatures.addAll(headChildDepFeatures);

        Set<String> frameArgumentFeatures = Sets.newHashSet("FrameArgument", "FrameArgumentSubPhrase", "FrameArgumentRole", "FrameArgumentSense",
                "FrameArgumentBrownCluster", "FrameArgumentHeadNer", "FrameArgumentPOS", "FrameArgumentSpanNer");
        frameArgumentFeatures.addAll(basicFeatures);

        Set<String> frameTargetFeatures = Sets.newHashSet("FrameName");
        frameTargetFeatures.addAll(basicFeatures);

        Set<String> allFrameFeatures = new HashSet<>();
        allFrameFeatures.addAll(frameArgumentFeatures);
        allFrameFeatures.addAll(frameTargetFeatures);


        Set<String> allFeatures = new HashSet<>();

        allSubsets.put("basic", basicFeatures);
        allSubsets.put("withHeadDep", headHeadDepFeatures);
        allSubsets.put("withChildep", headChildDepFeatures);
        allSubsets.put("withWindowWords", windowFeatures);
        allSubsets.put("withAllHeadDepFeatures", allHeadDepFeatures);
        allSubsets.put("withFrameArgument", frameArgumentFeatures);
        allSubsets.put("withFrameTarget", frameTargetFeatures);
        allSubsets.put("withallFrameFeatures", allFrameFeatures);
        allSubsets.put("allFeatures", allFeatures);

        return allSubsets;
    }

    public void doAblation(TypeSystemDescription typeSystemDescription,
                           String parentInput,
                           String modelBaseDir,
                           String trainingBaseDir,
                           String[] testBaseDirs,
                           String semLinkDataPath,
                           String bwClusterPath,
                           String wordnetDataPath
    ) throws Exception {
        EventMentionTrainer trainer = new EventMentionTrainer();


        Map<String, Double> featureSetResults = new HashMap<>();

        for (Map.Entry<String, Set<String>> namedFeatureSet : configFeatureSubset().entrySet()) {
            String featureSetName = namedFeatureSet.getKey();
            Set<String> featureSet = namedFeatureSet.getValue();
            double weightedF1 = trainer.buildModels(typeSystemDescription, parentInput, modelBaseDir + "/" + featureSetName,
                    trainingBaseDir, testBaseDirs, semLinkDataPath, bwClusterPath, wordnetDataPath, featureSet);
            featureSetResults.put(namedFeatureSet.getKey(), weightedF1);
        }

        System.out.println("====Summary of feature selection results====");
        for (Map.Entry<String, Double> featureSetResult : featureSetResults.entrySet()) {
            System.out.println(featureSetResult.getKey() + "\t" + featureSetResult.getValue());
        }
    }


    public static void main(String[] args) throws Exception {
        String paramInputDir = "event-mention-detection/data/Event-mention-detection-2014";
        String paramTypeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";
        String semLinkDataPath = "data/resources/SemLink_1.2.2c";
        String wordnetDataPath = "data/resources/wnDict";
        String brownClusteringDataPath = "data/resources/TDT5_BrownWC.txt";
        String trainingBaseDir = args[0];//"train_data";
        String modelBasePath = args[1]; //"models/train_split";

        String[] testBaseDirs = {"dev_data", "test_data"};
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);


        EventMentionTypeFeatureAblation ablation = new EventMentionTypeFeatureAblation();
        ablation.doAblation(typeSystemDescription, paramInputDir, modelBasePath, trainingBaseDir, testBaseDirs, semLinkDataPath, brownClusteringDataPath, wordnetDataPath);

    }
}