package edu.cmu.cs.lti.emd.pipeline;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.emd.annotators.EventMentionCandidateFeatureGenerator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Pair;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.RandomForest;
import weka.core.*;
import weka.core.converters.ArffSaver;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class EventMentionTrainer {
    private static String className = EventMentionTrainer.class.getSimpleName();

    private ArrayList<Attribute> featureConfiguration;

    private ArffSaver saver = new ArffSaver();

    public static final String featureConfigOutputName = "featureConfig";

    public static final String featureNamePath = "featureNames";

    public static final String predictionLabels = "labelNames";

    public void configFeatures(BiMap<String, Integer> featureNameMap, List<String> allClasses, File outputDir) throws Exception {
        featureConfiguration = new ArrayList<>();
        ArrayList<Map.Entry<String, Integer>> featureNames = new ArrayList<>(featureNameMap.entrySet());
        declareFeatures(featureNames, featureConfiguration);
        declareClass(allClasses, featureConfiguration);
        System.out.println("Number of features : " + featureNames.size() + ". Number of classes : " + allClasses.size());

        if (outputDir != null) {
            String featureNameSavingPath = new File(outputDir, featureNamePath).getCanonicalPath();
            System.out.println("Saving feature names to : " + featureNameSavingPath);
            SerializationHelper.write(new File(outputDir, featureNamePath).getCanonicalPath(), featureNameMap);

            String classNameSavingPath = new File(outputDir, predictionLabels).getCanonicalPath();
            System.out.println("Saving class names");
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

    private List<Classifier> getClassifiers() {
        List<Classifier> classifiers = new ArrayList<>();
        classifiers.add(new RandomForest());
        classifiers.add(new Logistic());
        classifiers.add(new SMO());
//        classifiers.add(new NaiveBayes());
//        classifiers.add(new J48());
        return classifiers;
    }

    private void trainAndTest(Instances trainingSet, Instances testSet, File modelOutPath) throws Exception {
        for (Classifier classifier : getClassifiers()) {
            Evaluation eval = new Evaluation(trainingSet);
            System.out.println("Building model");
            classifier.buildClassifier(trainingSet);
            System.out.println("Evaluating model");
            eval.evaluateModel(classifier, testSet);

            String classifierName = classifier.getClass().getName();

            System.out.println("=== Setup ===");
            System.out.println("Classifier: " + classifier.getClass().getName());
            System.out.println("Training set size: " + trainingSet.numInstances());
            System.out.println("Test set size: " + testSet.numInstances());
            System.out.println();
            System.out.println(eval.toSummaryString("=== Evaluation Results ===", false));

            if (modelOutPath != null) {
                String modelStoringPath = new File(modelOutPath, classifierName).getCanonicalPath();
                System.out.println("Saving model to : " + modelStoringPath);
                SerializationHelper.write(modelStoringPath, classifier);
            }
        }
    }

    private void test(Instances testSet, String pretrainedModelPath) throws Exception {
        Classifier classifier = (Classifier) SerializationHelper.read(pretrainedModelPath);

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
                System.out.println("Building model for fold " + n);
                classifier.buildClassifier(trainSplit);
                System.out.println("Evaluating model for fold " + n);
                eval.evaluateModel(classifier, testSplit);
            }

            String classifierName = classifier.getClass().getName();

            System.out.println("=== Setup ===");
            System.out.println("Classifier: " + classifier.getClass().getName());
            System.out.println("Data set size: " + dataSet.numInstances());
            System.out.println("Folds: " + folds);
            System.out.println();
            System.out.println(eval.toSummaryString("=== " + folds + "-fold Cross-validation ===", false));

            SerializationHelper.write(new File(modelOutDir, classifierName).getCanonicalPath(), classifier);
        }
    }


    private Instances prepareDataSet(List<Pair<TIntDoubleMap, String>> featuresAndClass, String dataSetOutputPath) throws Exception {
        Instances dataSet = new Instances("event_type_detection", featureConfiguration, featuresAndClass.size());
        dataSet.setClass(featureConfiguration.get(featureConfiguration.size() - 1));

        System.out.println("Adding instances");
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

        System.out.println("Number of instances stored : " + dataSet.numInstances());

        if (dataSetOutputPath != null) {
            System.out.println("Saving dataset to : " + dataSetOutputPath);
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
                                  int stepNum, String semLinkDataPath, boolean isTraining,
                                  String modelDir, boolean keep_quite) throws UIMAException, IOException {
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(inputDir, baseInputDirName, stepNum, false);
        AnalysisEngineDescription ana = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionCandidateFeatureGenerator.class, typeSystemDescription,
                EventMentionCandidateFeatureGenerator.PARAM_SEM_LINK_DIR, semLinkDataPath,
                EventMentionCandidateFeatureGenerator.PARAM_IS_TRAINING, isTraining,
                EventMentionCandidateFeatureGenerator.PARAM_ONLINE_TEST, false,
                EventMentionCandidateFeatureGenerator.PARAM_MODEL_FOLDER, modelDir,
                EventMentionCandidateFeatureGenerator.PARAM_KEEP_QUIET, keep_quite
        );
        SimplePipeline.runPipeline(reader, ana);
    }


    private void buildModels(TypeSystemDescription typeSystemDescription,
                             String parentInput,
                             String modelBaseDir,
                             String trainingBaseDir,
                             String devBaseDir,
                             String semLinkDataPath) throws Exception {
        File modelOutputDir = new File(parentInput, modelBaseDir);
        if (!modelOutputDir.exists() || !modelOutputDir.isDirectory()) {
            modelOutputDir.mkdirs();
        }

        System.out.println("Preparing training dataset");
        generateFeatures(typeSystemDescription, parentInput, trainingBaseDir, 1, semLinkDataPath, true, null, true);
        BiMap<String, Integer> featureNameMap = EventMentionCandidateFeatureGenerator.featureNameMap;
        List<Pair<TIntDoubleMap, String>> trainingFeatures = EventMentionCandidateFeatureGenerator.featuresAndClass;
        ArrayList<String> allClasses = new ArrayList<>(EventMentionCandidateFeatureGenerator.allTypes);
        configFeatures(featureNameMap, allClasses, modelOutputDir);

        Instances trainingDataset = prepareDataSet(trainingFeatures, new File(modelOutputDir, "training.arff").getCanonicalPath());
        System.out.println("Number of training instances : " + trainingFeatures.size());

        System.out.println("Saving feature config");
        SerializationHelper.write(new File(modelOutputDir, featureConfigOutputName).getCanonicalPath(), featureConfiguration);

        System.out.println("Preparing dev dataset");
        generateFeatures(typeSystemDescription, parentInput, devBaseDir, 1, semLinkDataPath, false, modelOutputDir.getCanonicalPath(), true);
        List<Pair<TIntDoubleMap, String>> devFeatures = EventMentionCandidateFeatureGenerator.featuresAndClass;
        Instances devDataset = prepareDataSet(devFeatures, new File(modelOutputDir, "test.arff").getCanonicalPath());
        System.out.println("Number of dev instances : " + devFeatures.size());

        System.out.println("Conducting evaluation on dev");
        trainAndTest(trainingDataset, devDataset, modelOutputDir);
    }


    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");

        String paramInputDir = "event-mention-detection/data/Event-mention-detection-2014";
        String devBaseDir = "dev_data";
        String paramTypeSystemDescriptor = "TypeSystem";
        String semLinkDataPath = "data/resources/SemLink_1.2.2c";
        String modelBasePath = "models_new";

        String trainingBaseDir = args[0];//"train_data";
        File modelOutputDir = new File(args[1]);
//                new File("event-mention-detection/data/Event-mention-detection-2014/models");
        if (!modelOutputDir.exists() || !modelOutputDir.isDirectory()) {
            modelOutputDir.mkdirs();
        }

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        EventMentionTrainer trainer = new EventMentionTrainer();
        trainer.buildModels(typeSystemDescription, paramInputDir, modelBasePath, trainingBaseDir, devBaseDir, semLinkDataPath);
    }
}