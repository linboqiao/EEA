package edu.cmu.cs.lti.emd.pipeline;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.emd.annotators.EventMentionCandidateFeatureGenerator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Pair;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.*;
import weka.core.converters.ArffSaver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class EventMentionTrainer {
    private static String className = EventMentionTrainer.class.getSimpleName();

    private ArrayList<Attribute> featureConfiguration;

    private void declareFeatureVector(ArrayList<Map.Entry<String, Integer>> featureNames, List<String> allClasses) {
        featureConfiguration = new ArrayList<>();
        declareFeatures(featureNames, featureConfiguration);
        declareClass(allClasses, featureConfiguration);
    }

    private void declareFeatures(ArrayList<Map.Entry<String, Integer>> featureNames, List<Attribute> featureVector) {
        Attribute[] featureArray = new Attribute[featureNames.size()];
        for (BiMap.Entry<String, Integer> featureEntry : featureNames) {
            featureArray[featureEntry.getValue()] = new Attribute(featureEntry.getKey());
        }

        for (Attribute featureAttr : featureArray) {
            featureVector.add(featureAttr);
        }
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
        classifiers.add(new SMO());
        classifiers.add(new NaiveBayes());
        classifiers.add(new J48());
        return classifiers;
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

    private Instances prepareDataSet(BiMap<String, Integer> featureNameMap, List<String> allClasses, List<Pair<TIntDoubleMap, String>> featuresAndClass) throws Exception {
        //fix the iteration sequence;
        ArrayList<Map.Entry<String, Integer>> featureNames = new ArrayList<>(featureNameMap.entrySet());

        declareFeatureVector(featureNames, allClasses);

        System.out.println("Number of features : " + featureNames.size() + ". Number of classes : " + allClasses.size());

        Instances dataSet = new Instances("event_type_detection", featureConfiguration, featuresAndClass.size());
        dataSet.setClass(featureConfiguration.get(featureConfiguration.size() - 1));

        //TODO add data point is too slow
        System.out.println("Adding instance");
//        int fcounter = 0;

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

//            System.out.print(" " + fcounter++);

            //set class
            trainingInstance.setClassValue(classValue);
            dataSet.add(trainingInstance);
        }

//        System.out.println();
        System.out.println("Number of instances stored : " + dataSet.numInstances());
        return dataSet;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");

        // Parameters for the writer
        String paramInputDir = "event-mention-detection/data/Event-mention-detection-2014";
        String paramBaseOutputDirName = "candidate_annotated";

        String paramTypeSystemDescriptor = "TypeSystem";

        String semLinkDataPath = "data/resources/SemLink_1.2.2c";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(paramInputDir, paramBaseOutputDirName, 1, false);

        AnalysisEngineDescription ana = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionCandidateFeatureGenerator.class, typeSystemDescription,
                EventMentionCandidateFeatureGenerator.PARAM_SEM_LINK_DIR, semLinkDataPath,
                EventMentionCandidateFeatureGenerator.PARAM_IS_TRAINING, true);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, ana);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        //start training
        BiMap<String, Integer> featureNameMap = EventMentionCandidateFeatureGenerator.featureNameMap;
        List<Pair<TIntDoubleMap, String>> featuresAndClass = EventMentionCandidateFeatureGenerator.featuresAndClass;
        ArrayList<String> allClasses = new ArrayList<>(EventMentionCandidateFeatureGenerator.allTypes);

        EventMentionTrainer trainer = new EventMentionTrainer();

        System.out.println("Preparing dataset");
        System.out.println("Number of training instances : " + featuresAndClass.size());
        Instances dataset = trainer.prepareDataSet(featureNameMap, allClasses, featuresAndClass);

        System.out.println("Saving");

        ArffSaver saver = new ArffSaver();
        saver.setInstances(dataset);
        saver.setFile(new File("event-mention-detection/data/Event-mention-detection-2014/training.arff"));
        saver.writeBatch();

        System.out.println("Conducting CV");

        File modelOutputDir = new File("event-mention-detection/data/Event-mention-detection-2014/models");
        if (!modelOutputDir.exists() || !modelOutputDir.isDirectory()) {
            modelOutputDir.mkdirs();
        }

        trainer.crossValidation(dataset, modelOutputDir);
    }
}