package edu.cmu.cs.lti.emd.pipeline;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.emd.annotators.EventMentionRealisLearner;
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
import weka.classifiers.functions.SMO;
import weka.core.*;
import weka.core.converters.ArffSaver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class RealisTrainer {
    private static String className = RealisTrainer.class.getSimpleName();

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

    private void trainAndTest(Instances trainingSet, List<Instances> testSets, File modelOutPath, List<String> allClasses) throws Exception {
        for (Classifier classifier : getClassifiers()) {
            Evaluation eval = new Evaluation(trainingSet);
            System.out.println("Building model");
            classifier.buildClassifier(trainingSet);
            String classifierName = classifier.getClass().getName();

            System.out.println("Evaluating model");
            for (Instances testSet : testSets) {
                eval.evaluateModel(classifier, testSet);
                System.out.println("=== Setup ===");
                System.out.println("Classifier: " + classifierName);
                System.out.println("Training set size: " + trainingSet.numInstances());
                System.out.println("Test set size: " + testSet.numInstances());
                System.out.println();
                System.out.println(eval.toSummaryString("=== Evaluation Results ===", false));
                System.out.println("Prec\tRecall\tF1\tTotal");
                for (int i = 0; i < allClasses.size(); i++) {
                    int realClassIndex = i + 1;
                    double numInThisClass = eval.numTruePositives(realClassIndex) + eval.numFalseNegatives(realClassIndex);
                    System.out.println(String.format("%.4f\t%.4f\t%.4f\t%.2f\t%s",
                            eval.precision(realClassIndex), eval.recall(realClassIndex), eval.fMeasure(realClassIndex), numInThisClass, allClasses.get(i)));
                }
            }

            if (modelOutPath != null) {
                String modelStoringPath = new File(modelOutPath, classifierName).getCanonicalPath();
                System.out.println("Saving model to : " + modelStoringPath);
                SerializationHelper.write(modelStoringPath, classifier);
            }
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
                                  int stepNum, String semLinkDataPath,
                                  String bwClusterPath, String wordnetDataPath,
                                  boolean isTraining, String modelDir, boolean keep_quite) throws UIMAException, IOException {
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(inputDir, baseInputDirName, stepNum, false);
        AnalysisEngineDescription ana = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionRealisLearner.class, typeSystemDescription,
                EventMentionRealisLearner.PARAM_SEM_LINK_DIR, semLinkDataPath,
                EventMentionRealisLearner.PARAM_IS_TRAINING, isTraining,
                EventMentionRealisLearner.PARAM_ONLINE_TEST, false,
                EventMentionRealisLearner.PARAM_MODEL_FOLDER, modelDir,
                EventMentionRealisLearner.PARAM_BROWN_CLUSTERING_PATH, bwClusterPath,
                EventMentionRealisLearner.PARAM_KEEP_QUIET, keep_quite
        );
        SimplePipeline.runPipeline(reader, ana);
    }


    private void buildModels(TypeSystemDescription typeSystemDescription,
                             String parentInput,
                             String modelBaseDir,
                             String trainingBaseDir,
                             String[] devBaseDirs,
                             String semLinkDataPath,
                             String bwClusterPath,
                             String wordnetDataPath) throws Exception {
        File modelOutputDir = new File(parentInput, modelBaseDir);
        if (!modelOutputDir.exists() || !modelOutputDir.isDirectory()) {
            modelOutputDir.mkdirs();
        }

        System.out.println("Preparing training dataset");
        generateFeatures(typeSystemDescription, parentInput, trainingBaseDir, 1, semLinkDataPath, bwClusterPath, wordnetDataPath, true, null, true);
        BiMap<String, Integer> featureNameMap = EventMentionRealisLearner.featureNameMap;
        List<Pair<TIntDoubleMap, String>> trainingFeatures = EventMentionRealisLearner.featuresAndClass;
        ArrayList<String> allClasses = new ArrayList<>(EventMentionRealisLearner.allTypes);
        configFeatures(featureNameMap, allClasses, modelOutputDir);

        Instances trainingDataset = prepareDataSet(trainingFeatures, new File(modelOutputDir, "training.arff").getCanonicalPath());
        System.out.println("Number of training instances : " + trainingFeatures.size());

        System.out.println("Saving feature config");
        SerializationHelper.write(new File(modelOutputDir, featureConfigOutputName).getCanonicalPath(), featureConfiguration);

        System.out.println("Preparing dev datasets");
        List<Instances> testSets = new ArrayList<>();
        for (String devBaseDir : devBaseDirs) {
            generateFeatures(typeSystemDescription, parentInput, devBaseDir, 1,
                    semLinkDataPath, bwClusterPath, wordnetDataPath, false, modelOutputDir.getCanonicalPath(), true);
            List<Pair<TIntDoubleMap, String>> devFeatures = EventMentionRealisLearner.featuresAndClass;
            Instances devDataset = prepareDataSet(devFeatures, new File(modelOutputDir, "test.arff").getCanonicalPath());
            testSets.add(devDataset);
            System.out.println("Number of dev instances : " + devFeatures.size());
        }
        System.out.println("Conducting evaluation on dev sets");
        trainAndTest(trainingDataset, testSets, modelOutputDir, allClasses);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");
        String paramInputDir = "event-mention-detection/data/Event-mention-detection-2014";
        String paramTypeSystemDescriptor = "TypeSystem";
        String semLinkDataPath = "data/resources/SemLink_1.2.2c";
        String wordnetDataPath = "data/resources/wnDict";
        String brownClusteringDataPath = "data/resources/TDT5_BrownWC.txt";
        String trainingBaseDir = args[0];//"train_data";
        String modelBasePath = args[1]; //"models";

        String[] devBaseDir = {"dev_data", "test_data"};

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        RealisTrainer trainer = new RealisTrainer();
        trainer.buildModels(typeSystemDescription, paramInputDir, modelBasePath, trainingBaseDir, devBaseDir, semLinkDataPath, brownClusteringDataPath, wordnetDataPath);

        System.out.println(className + " finished...");
    }
}