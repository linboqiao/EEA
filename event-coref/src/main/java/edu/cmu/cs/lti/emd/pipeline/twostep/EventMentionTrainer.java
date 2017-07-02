package edu.cmu.cs.lti.emd.pipeline.twostep;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.emd.annotators.twostep.EventMentionTypeLearner;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import weka.core.*;
import weka.core.converters.ArffSaver;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;


public class EventMentionTrainer {
    private static String className = EventMentionTrainer.class.getSimpleName();

    private ArrayList<Attribute> featureConfiguration;

    private ArffSaver saver = new ArffSaver();

    public static final String featureConfigOutputName = "featureConfig";

    public static final String featureNamePath = "featureNames";

    public static final String predictionLabels = "labelNames";

    public static final String trainingFeatureName = "training.arff";

    private static Logger logger = Logger.getLogger(EventMentionTrainer.className);

    String semLinkDataPath;
    String bwClusterPath;
    String wordnetDataPath;
    TypeSystemDescription typeSystemDescription;

    public EventMentionTrainer(String semLinkDataPath,
                               String bwClusterPath,
                               String wordnetDataPath,
                               TypeSystemDescription typeSystemDescription) {
        this.semLinkDataPath = semLinkDataPath;
        this.bwClusterPath = bwClusterPath;
        this.wordnetDataPath = wordnetDataPath;
        this.typeSystemDescription = typeSystemDescription;
    }

    public void configFeatures(BiMap<String, Integer> featureNameMap, List<String> allClasses, File outputDir) throws Exception {
        featureConfiguration = new ArrayList<>();
        ArrayList<Map.Entry<String, Integer>> featureNames = new ArrayList<>(featureNameMap.entrySet());
        declareFeatures(featureNames, featureConfiguration);
        declareClass(allClasses, featureConfiguration);
        logger.info("Number of features : " + featureNames.size() + ". Number of classes : " + allClasses.size());

        if (outputDir != null) {
            String featureNameSavingPath = new File(outputDir, featureNamePath).getCanonicalPath();
            logger.info("Saving feature names to : " + featureNameSavingPath);
            SerializationHelper.write(new File(outputDir, featureNamePath).getCanonicalPath(), featureNameMap);

            String classNameSavingPath = new File(outputDir, predictionLabels).getCanonicalPath();
            logger.info("Saving class names");
            SerializationHelper.write(classNameSavingPath, allClasses);
        }
    }

    private void declareFeatures(ArrayList<Map.Entry<String, Integer>> featureNames, List<Attribute> featureVector) {
        Attribute[] featureArray = new Attribute[featureNames.size()];
        for (BiMap.Entry<String, Integer> featureEntry : featureNames) {
            featureArray[featureEntry.getValue()] = new Attribute(featureEntry.getKey());
        }
        Collections.addAll(featureVector, featureArray);
    }

    private void declareClass(List<String> allClasses, List<Attribute> featureVector) {
        List<String> fixedClasses = new ArrayList<>();
        //a bug related to the sparse vector
        fixedClasses.add("dummy_class");
        fixedClasses.addAll(allClasses);
        featureVector.add(new Attribute("event_types", fixedClasses));
    }


    private Classifier configClassifier(AbstractClassifier cls, String[] options) throws Exception {
        cls.setOptions(options);
        return cls;
    }

    private Map<String, Classifier> getClassifiers() throws Exception {
        Map<String, Classifier> classifiers = new HashMap<>();
        classifiers.put("LibSvm_linear", configClassifier(new LibSVM(), new String[]{"-K", "0"}));
        classifiers.put("LibSvm_poly_square", configClassifier(new LibSVM(), new String[]{"-K", "1", "-D", "2", "-G", "1.0"}));
//        classifiers.put("LibSvm_Gaussian", configClassifier(new LibSVM(), new String[]{"-G","1.0"}));
//        classifiers.put("LibSvm_poly_cubic", configClassifier(new LibSVM(), new String[]{"-K", "1", "-G", "1.0"}));


//        classifiers.put("SMO_linear", configClassifier(new SMO(), new String[]{}));
//        classifiers.put("SMO_poly", configClassifier(new SMO(), new String[]{"-K", "weka.classifiers.functions.supportVector.PolyKernel -E 2.0"}));
//        classifiers.put("SMO_gaussian", configClassifier(new SMO(), new String[]{"-K", "weka.classifiers.functions.supportVector.RBFKernel"}));

//        classifiers.put("logistic_ridge", configClassifier(new Logistic(), new String[]{"-R", "1.0"}));
//        classifiers.put("linear", configClassifier(new LinearRegression(), new String[]{}));

//        classifiers.add(new RandomForest());
//        classifiers.put("NB", new NaiveBayes());
//        classifiers.add(new J48());
        return classifiers;
    }

    /**
     * Train multiple models, return the best weighted F1 score these models
     *
     * @param trainingSet
     * @return
     * @throws Exception
     */
    private Map<String, Classifier> train(Instances trainingSet, String classifierSuffix) throws Exception {
        Map<String, Classifier> classifiersByName = new HashMap<>();

        for (Map.Entry<String, Classifier> classifierByName : getClassifiers().entrySet()) {
            Classifier classifier = classifierByName.getValue();
            String baseName = classifierByName.getKey();
            logger.info("Building a model with " + baseName);
            classifier.buildClassifier(trainingSet);
            String classifierName = baseName + "_" + classifierSuffix;
            classifiersByName.put(classifierName, classifier);
        }
        return classifiersByName;
    }

    private double test(Instances trainingSet, Instances testSet, Classifier classifier, List<String> allClasses, File resultDir) throws Exception {
        Evaluation eval = new Evaluation(trainingSet);
        String classifierName = classifier.getClass().getName();

        List<String> evalResultLines = new ArrayList<>();

        double weightedF1 = 0;
        int totalInstances = 0;

        eval.evaluateModel(classifier, testSet);
        logger.info("=== Setup ===");
        logger.info("Classifier: " + classifierName);
        logger.info("Training set size: " + trainingSet.numInstances());
        logger.info("Test set size: " + testSet.numInstances());

        String evalSummary = eval.toSummaryString("=== Evaluation Results ===", false);
        evalResultLines.add(evalSummary);

        evalResultLines.add("Prec\tRecall\tF1\tTotal");

        for (int i = 0; i < allClasses.size(); i++) {
            int realClassIndex = i + 1;
            double numInThisClass = eval.numTruePositives(realClassIndex) + eval.numFalseNegatives(realClassIndex);

            String fString = String.format("%.4f\t%.4f\t%.4f\t%.2f\t%s",
                    eval.precision(realClassIndex), eval.recall(realClassIndex), eval.fMeasure(realClassIndex), numInThisClass, allClasses.get(i));
            evalResultLines.add(fString);

            if (!allClasses.get(i).equals(EventMentionTypeLearner.OTHER_TYPE)) {
                weightedF1 += eval.fMeasure(realClassIndex) * numInThisClass;
                totalInstances += numInThisClass;
            }
        }

        //calculate Weighted F1 scores averaged on all dataset tested
        weightedF1 /= totalInstances;

        File resultStoringFile = new File(resultDir, "per_class_results_with_" + classifierName);
        logger.info("Saving eval results to : " + resultStoringFile);
        FileUtils.writeLines(resultStoringFile, evalResultLines);

        return weightedF1;
    }

    private Instances convertToInstances(List<Pair<TIntDoubleMap, String>> featuresAndClass, String dataSetOutputPath) throws Exception {
        Instances dataSet = new Instances("event_type_detection", featureConfiguration, featuresAndClass.size());
        dataSet.setClass(featureConfiguration.get(featureConfiguration.size() - 1));

        logger.info("Adding instances");
        double[] emptyVector = new double[featureConfiguration.size()];

        for (Pair<TIntDoubleMap, String> rawData : featuresAndClass) {
            //initialize the sparse vector to be empty
            Instance trainingInstance = new SparseInstance(1, emptyVector);
            TIntDoubleMap featureValues = rawData.getKey();
            trainingInstance.setDataset(dataSet);
            String classValue = rawData.getValue();

            for (TIntDoubleIterator fIter = featureValues.iterator(); fIter.hasNext(); ) {
                fIter.advance();
                int featureId = fIter.key();
                double featureVal = fIter.value();
                trainingInstance.setValue(featureConfiguration.get(featureId), featureVal);
            }
            //set class
            trainingInstance.setClassValue(classValue);
            dataSet.add(trainingInstance);
        }

        logger.info("Number of instances stored : " + dataSet.numInstances());

        if (dataSetOutputPath != null) {
            logger.info("Saving dataset to : " + dataSetOutputPath);
            saveDataSet(dataSet, dataSetOutputPath);
        }
        return dataSet;
    }

    public void saveDataSet(Instances dataset, String path) throws IOException {
        saver.setInstances(dataset);
        saver.setFile(new File(path));
        saver.writeBatch();
    }

    private void generateFeatures(String inputDir, String baseInputDirName,
                                  int stepNum, boolean isTraining, String modelDir, boolean keep_quite,
                                  Set<String> featureSubset) throws UIMAException, IOException {
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(inputDir, baseInputDirName, false);
        AnalysisEngineDescription ana = AnalysisEngineFactory.createEngineDescription(
                EventMentionTypeLearner.class, typeSystemDescription,
                EventMentionTypeLearner.PARAM_SEM_LINK_DIR, semLinkDataPath,
                EventMentionTypeLearner.PARAM_IS_TRAINING, isTraining,
                EventMentionTypeLearner.PARAM_ONLINE_TEST, false,
                EventMentionTypeLearner.PARAM_MODEL_FOLDER, modelDir,
                EventMentionTypeLearner.PARAM_BROWN_CLUSTERING_PATH, bwClusterPath,
                EventMentionTypeLearner.PARAM_WORDNET_PATH, wordnetDataPath,
                EventMentionTypeLearner.PARAM_KEEP_QUIET, keep_quite,
                EventMentionTypeLearner.PARAM_FEAUTRE_SUBSET, featureSubset
        );
        SimplePipeline.runPipeline(reader, ana);
    }


    public Instances getDataSet(String parentInput,
                                String dataBaseDir,
                                Set<String> featureSubset,
                                File modelOutputDir) throws Exception {
        generateFeatures(parentInput, dataBaseDir, 1,
                false, modelOutputDir.getCanonicalPath(), true, featureSubset);
        List<Pair<TIntDoubleMap, String>> devFeatures = EventMentionTypeLearner.featuresAndClass;
        return convertToInstances(devFeatures, new File(modelOutputDir, "test.arff").getCanonicalPath());
    }


    public Map<String, Classifier> buildModels(String parentInput,
                                               String modelBaseDir,
                                               String trainingBaseDir,
                                               Set<String> featureSubset,
                                               Map<String, Instances> testSets,
                                               String resultDir) throws Exception {
        File modelOutputDir = new File(parentInput, modelBaseDir);
        if (!modelOutputDir.exists() || !modelOutputDir.isDirectory()) {
            modelOutputDir.mkdirs();
        }

        logger.info("Preparing training dataset");
        generateFeatures(parentInput, trainingBaseDir, 1, true, null, true, featureSubset);
        BiMap<String, Integer> featureNameMap = EventMentionTypeLearner.featureNameMap;
        List<Pair<TIntDoubleMap, String>> trainingFeatures = EventMentionTypeLearner.featuresAndClass;
        ArrayList<String> allClasses = new ArrayList<>(EventMentionTypeLearner.allTypes);
        configFeatures(featureNameMap, allClasses, modelOutputDir);

        Instances trainingDataset = convertToInstances(trainingFeatures, new File(modelOutputDir, trainingFeatureName).getCanonicalPath());
        logger.info("Number of training instances : " + trainingFeatures.size());

        logger.info("Saving feature featureConfig");
        SerializationHelper.write(new File(modelOutputDir, featureConfigOutputName).getCanonicalPath(), featureConfiguration);

        Map<String, Classifier> classifiers = train(trainingDataset, trainingBaseDir);

        for (Map.Entry<String, Classifier> classifierByName : classifiers.entrySet()) {
            String classifierName = classifierByName.getKey();
            Classifier classifier = classifierByName.getValue();
            String modelStoringPath = new File(modelOutputDir, classifierName).getCanonicalPath();
            logger.info("Saving model to : " + modelStoringPath);
            SerializationHelper.write(modelStoringPath, classifier);

            logger.info("Conducting evaluation on dev sets");
            for (Map.Entry<String, Instances> testSet : testSets.entrySet()) {
                test(trainingDataset, testSet.getValue(), classifier, allClasses, new File(resultDir, classifierName + "_" + testSet.getKey()));
            }
        }

        return classifiers;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");
        String workingDir = "event-mention-detection/data/Event-mention-detection-2014";
        String typeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";
        String semLinkDataPath = "data/resources/SemLink_1.2.2c";
        String wordnetDataPath = "data/resources/wnDict";
        String brownClusteringDataPath = "data/resources/TDT5_BrownWC.txt";
        String trainingBaseDir = args[0];//"train_data";
        String modelBasePath = args[1]; //"models/train_split";
        String resultBaseDir = workingDir + "/results";

        String[] testBaseDirs = {"dev_data", "test_data"};

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemDescriptor);

        Set<String> featureSubset = new HashSet<>();

        EventMentionTrainer trainer = new EventMentionTrainer(semLinkDataPath, brownClusteringDataPath, wordnetDataPath, typeSystemDescription);
        Map<String, Instances> testSets = new HashMap<>();
        for (String testBaseDir : testBaseDirs) {
            logger.info("Preparing test data");
            testSets.put(new File(testBaseDir).getName(), trainer.getDataSet(workingDir, testBaseDir, featureSubset, new File(workingDir, modelBasePath)));
        }

        trainer.buildModels(workingDir, modelBasePath, trainingBaseDir, featureSubset, testSets, resultBaseDir);
        System.out.println(className + " finished...");
    }
}