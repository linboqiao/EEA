package edu.cmu.cs.lti.emd.pipeline;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.emd.annotators.EventMentionTypeLearner;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Pair;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
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

    private Logger logger = Logger.getLogger(EventMentionTrainer.className);

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

    private Classifier getSMO() throws Exception {
        SMO smo = new SMO();
//        String[] options = {"-M"};
//        smo.setOptions(options);
        return smo;
    }

    private List<Classifier> getClassifiers() throws Exception {
        List<Classifier> classifiers = new ArrayList<>();
        classifiers.add(getSMO());
//        classifiers.add(new RandomForest());
//        classifiers.add(new Logistic());
//        classifiers.add(new NaiveBayes());
//        classifiers.add(new J48());
        return classifiers;
    }

    /**
     * Train multiple models, return the best weighted F1 score these models
     *
     * @param trainingSet
     * @param testSets
     * @param modelOutPath
     * @param allClasses
     * @return
     * @throws Exception
     */
    private double trainAndTest(Instances trainingSet, List<Instances> testSets, File modelOutPath, List<String> allClasses) throws Exception {
        double bestWeightedF1 = 0;

        for (Classifier classifier : getClassifiers()) {
            Evaluation eval = new Evaluation(trainingSet);
            logger.info("Building model");
            classifier.buildClassifier(trainingSet);
            String classifierName = classifier.getClass().getName();

            logger.info("Evaluating model");

            double weightedF1 = 0;
            int totalInstances = 0;

            List<String> evalResultLines = new ArrayList<>();

            for (Instances testSet : testSets) {
                eval.evaluateModel(classifier, testSet);
                logger.info("=== Setup ===");
                logger.info("Classifier: " + classifierName);
                logger.info("Training set size: " + trainingSet.numInstances());
                logger.info("Test set size: " + testSet.numInstances());

                String evalSummary = eval.toSummaryString("=== Evaluation Results ===", false);
//                logger.info(evalSummary);
                evalResultLines.add(evalSummary);

//                logger.info("Prec\tRecall\tF1\tTotal");
                evalResultLines.add("Prec\tRecall\tF1\tTotal");

                for (int i = 0; i < allClasses.size(); i++) {
                    int realClassIndex = i + 1;
                    double numInThisClass = eval.numTruePositives(realClassIndex) + eval.numFalseNegatives(realClassIndex);

                    String fString = String.format("%.4f\t%.4f\t%.4f\t%.2f\t%s",
                            eval.precision(realClassIndex), eval.recall(realClassIndex), eval.fMeasure(realClassIndex), numInThisClass, allClasses.get(i));
//                    logger.info(fString);
                    evalResultLines.add(fString);

                    if (!allClasses.get(i).equals(EventMentionTypeLearner.OTHER_TYPE)) {
                        weightedF1 += eval.fMeasure(realClassIndex) * numInThisClass;
                        totalInstances += numInThisClass;
                    }
                }
            }

            if (modelOutPath != null) {
                String modelStoringPath = new File(modelOutPath, classifierName).getCanonicalPath();
                logger.info("Saving model to : " + modelStoringPath);
                SerializationHelper.write(modelStoringPath, classifier);

                File resultStoringFile = new File(modelOutPath, "results_with_" + classifierName);
                logger.info("Saving eval results to : " + resultStoringFile);
                FileUtils.writeLines(resultStoringFile, evalResultLines);
            }


            //calculate Weighted F1 scores averaged on all dataset tested
            weightedF1 /= totalInstances;
            if (weightedF1 > bestWeightedF1) {
                bestWeightedF1 = weightedF1;
            }
        }

        return bestWeightedF1;
    }

    private void crossValidation(Instances dataSet, File modelOutDir) throws Exception {
        Random rand = new Random(0);   // create seeded number generator
        Instances randData = new Instances(dataSet);   // create copy of original data
        randData.randomize(rand);

        int folds = 5;
        Evaluation eval = new Evaluation(randData);

        for (Classifier classifier : getClassifiers()) {
            for (int n = 0; n < folds; n++) {
                Instances trainSplit = randData.trainCV(folds, n);
                Instances testSplit = randData.testCV(folds, n);
                logger.info("Building model for fold " + n);
                classifier.buildClassifier(trainSplit);
                logger.info("Evaluating model for fold " + n);
                eval.evaluateModel(classifier, testSplit);
            }

            String classifierName = classifier.getClass().getName();

            logger.info("=== Setup ===");
            logger.info("Classifier: " + classifier.getClass().getName());
            logger.info("Data set size: " + dataSet.numInstances());
            logger.info("Folds: " + folds);
            logger.info(eval.toSummaryString("=== " + folds + "-fold Cross-validation ===", false));

            SerializationHelper.write(new File(modelOutDir, classifierName).getCanonicalPath(), classifier);
        }
    }


    private Instances prepareDataSet(List<Pair<TIntDoubleMap, String>> featuresAndClass, String dataSetOutputPath) throws Exception {
        Instances dataSet = new Instances("event_type_detection", featureConfiguration, featuresAndClass.size());
        dataSet.setClass(featureConfiguration.get(featureConfiguration.size() - 1));

        logger.info("Adding instances");
        double[] emptyVector = new double[featureConfiguration.size()];

        for (Pair<TIntDoubleMap, String> rawData : featuresAndClass) {
            //initialize the sparse vector to be empty
            Instance trainingInstance = new SparseInstance(1, emptyVector);
            TIntDoubleMap featureValues = rawData.getValue0();
            trainingInstance.setDataset(dataSet);
            String classValue = rawData.getValue1();

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

    private void generateFeatures(TypeSystemDescription typeSystemDescription,
                                  String inputDir, String baseInputDirName,
                                  int stepNum, String semLinkDataPath,
                                  String bwClusterPath, String wordnetDataPath,
                                  boolean isTraining, String modelDir, boolean keep_quite,
                                  Set<String> featureSubset) throws UIMAException, IOException {
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(inputDir, baseInputDirName, stepNum, false);
        AnalysisEngineDescription ana = CustomAnalysisEngineFactory.createAnalysisEngine(
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


    public double buildModels(TypeSystemDescription typeSystemDescription,
                              String parentInput,
                              String modelBaseDir,
                              String trainingBaseDir,
                              String[] testBaseDirs,
                              String semLinkDataPath,
                              String bwClusterPath,
                              String wordnetDataPath,
                              Set<String> featureSubset) throws Exception {
        File modelOutputDir = new File(parentInput, modelBaseDir);
        if (!modelOutputDir.exists() || !modelOutputDir.isDirectory()) {
            modelOutputDir.mkdirs();
        }

        logger.info("Preparing training dataset");
        generateFeatures(typeSystemDescription, parentInput, trainingBaseDir, 1, semLinkDataPath, bwClusterPath, wordnetDataPath, true, null, true, featureSubset);
        BiMap<String, Integer> featureNameMap = EventMentionTypeLearner.featureNameMap;
        List<Pair<TIntDoubleMap, String>> trainingFeatures = EventMentionTypeLearner.featuresAndClass;
        ArrayList<String> allClasses = new ArrayList<>(EventMentionTypeLearner.allTypes);
        configFeatures(featureNameMap, allClasses, modelOutputDir);

        Instances trainingDataset = prepareDataSet(trainingFeatures, new File(modelOutputDir, "training.arff").getCanonicalPath());
        logger.info("Number of training instances : " + trainingFeatures.size());

        logger.info("Saving feature config");
        SerializationHelper.write(new File(modelOutputDir, featureConfigOutputName).getCanonicalPath(), featureConfiguration);

        List<Instances> testSets = new ArrayList<>();
        for (String devBaseDir : testBaseDirs) {
            logger.info("Preparing test data");
            generateFeatures(typeSystemDescription, parentInput, devBaseDir, 1,
                    semLinkDataPath, bwClusterPath, wordnetDataPath, false, modelOutputDir.getCanonicalPath(), true, featureSubset);
            List<Pair<TIntDoubleMap, String>> devFeatures = EventMentionTypeLearner.featuresAndClass;
            Instances devDataset = prepareDataSet(devFeatures, new File(modelOutputDir, "test.arff").getCanonicalPath());
            testSets.add(devDataset);
            logger.info("Number of dev instances : " + devFeatures.size());
        }
        logger.info("Conducting evaluation on dev sets");
        return trainAndTest(trainingDataset, testSets, modelOutputDir, allClasses);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");
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

        Set<String> featureSubset = new HashSet<>();

        EventMentionTrainer trainer = new EventMentionTrainer();
        trainer.buildModels(typeSystemDescription, paramInputDir, modelBasePath, trainingBaseDir, testBaseDirs, semLinkDataPath, brownClusteringDataPath, wordnetDataPath, featureSubset);

        System.out.println(className + " finished...");
    }
}