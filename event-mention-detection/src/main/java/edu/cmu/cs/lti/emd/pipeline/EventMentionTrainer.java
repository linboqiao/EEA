package edu.cmu.cs.lti.emd.pipeline;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.emd.annotators.EventMentionCandidateFeatureGenerator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import gnu.trove.map.TIntDoubleMap;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Triplet;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
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
        for (BiMap.Entry<String, Integer> featureEntry : featureNames) {
            featureVector.add(new Attribute(featureEntry.getKey()));
        }
    }

    private void declareClass(List<String> allClasses, List<Attribute> featureVector) {
        featureVector.add(new Attribute("event_types", allClasses));
    }

    private void crossValidation(List<String> allClasses, Instances dataSet) throws Exception {
        Random rand = new Random(0);   // create seeded number generator
        Instances randData = new Instances(dataSet);   // create copy of original data
        randData.randomize(rand);

        Classifier classifier = new Logistic();

        for (int n = 0; n < 10; n++) {
            Instances trainSplit = randData.trainCV(10, n);
            Instances testSplit = randData.testCV(10, n);

            classifier.buildClassifier(trainSplit);

            Evaluation eval = new Evaluation(trainSplit);
            eval.evaluateModel(classifier, testSplit);

            System.out.println("==================");
            System.out.println("CV iter %d : " + n);

            for (int i = 0; i < allClasses.size(); i++) {
                System.out.println(
                        String.format("Class [%s], f-score: [%.4f], TP [%.2f], FP [%.2f], TN [%.2f], FN [%.2f]",
                                allClasses.get(i),
                                eval.fMeasure(i),
                                eval.truePositiveRate(i),
                                eval.falsePositiveRate(i),
                                eval.trueNegativeRate(i),
                                eval.falseNegativeRate(i)
                        ));
            }
            System.out.println("==================");
        }
    }

    private Instances prepareDataSet(BiMap<String, Integer> featureNameMap, List<String> allClasses, List<Triplet<String, TIntDoubleMap, String>> data) throws Exception {
        //fix the iteration sequence;
        ArrayList<Map.Entry<String, Integer>> featureNames = new ArrayList<>(featureNameMap.entrySet());

        declareFeatureVector(featureNames, allClasses);

        Instances dataSet = new Instances("event_type_detection", featureConfiguration, data.size());

        for (Triplet<String, TIntDoubleMap, String> rawData : data) {
            Instance trainingInstance = new DenseInstance(featureConfiguration.size());
            TIntDoubleMap featureValues = rawData.getValue1();
            for (int i = 0; i < featureNames.size(); i++) {
                int featureId = featureNames.get(i).getValue();
                double featureValue = featureValues.containsValue(featureId) ? featureValues.get(featureId) : 0;
                trainingInstance.setValue(featureConfiguration.get(i), featureValue);
            }
            trainingInstance.setValue(featureConfiguration.get(featureConfiguration.size() - 1), rawData.getValue2());
            dataSet.add(trainingInstance);
        }
        dataSet.setClassIndex(dataSet.numAttributes() - 1);
        return dataSet;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");

        // Parameters for the writer
        String paramInputDir = "event-mention-detection/data/Event-mention-detection-2014";
        String paramBaseOutputDirName = "semafor_processed";

        String paramTypeSystemDescriptor = "TypeSystem";

        String semLinkDataPath = "data/resources/SemLink_1.2.2c";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(paramInputDir, paramBaseOutputDirName, 0, false);

        AnalysisEngineDescription ana = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionCandidateFeatureGenerator.class, typeSystemDescription,
                EventMentionCandidateFeatureGenerator.PARAM_SEM_LINK_DIR, semLinkDataPath,
                EventMentionCandidateFeatureGenerator.PARAM_HAS_GOLD, true);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, ana);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        //start training

        BiMap<String, Integer> featureNameMap = EventMentionCandidateFeatureGenerator.featureNameMap;
        List<Triplet<String, TIntDoubleMap, String>> instances = EventMentionCandidateFeatureGenerator.instances;
        ArrayList<String> allClasses = new ArrayList<>(EventMentionCandidateFeatureGenerator.allTypes);

        EventMentionTrainer trainer = new EventMentionTrainer();

        System.out.println("Preparing dataset");

        Instances dataset = trainer.prepareDataSet(featureNameMap, allClasses, instances);

        System.out.println("Saving");

        ArffSaver saver = new ArffSaver();
        saver.setInstances(dataset);
        saver.setFile(new File("event-mention-detection/data/Event-mention-detection-2014/training.arff"));
        saver.writeBatch();

        System.out.println("Conducting CV");


        trainer.crossValidation(allClasses, dataset);
    }
}