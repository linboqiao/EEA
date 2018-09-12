package edu.cmu.cs.lti.learning.debug;

import edu.cmu.cs.lti.learning.model.AveragedWeightVector;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.HashAlphabet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/3/15
 * Time: 4:14 PM
 *
 * @author Zhengzhong Liu
 */
public class HashedFeatureInspector {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private HashAlphabet featureAlphabet;
    private ClassAlphabet classAlphabet;
    private GraphWeightVector weightVector;
    private ReverseFeatureComparator comp;

    public HashedFeatureInspector(GraphWeightVector weightVector) {
        this.weightVector = weightVector;
        this.classAlphabet = weightVector.getClassAlphabet();
        comp = new ReverseFeatureComparator();

        if (!(weightVector.getFeatureAlphabet() instanceof HashAlphabet)) {
            logger.error("Hash inspector only inspect hashing alphabet.");
            System.exit(0);
        }

        this.featureAlphabet = (HashAlphabet) weightVector.getFeatureAlphabet();

        if (featureAlphabet.isStoreReadable()) {
            logger.info("This alphabet stores readable model.");
        } else {
            logger.error("No readable model stored!");
            System.exit(0);
        }

        featureAlphabet.computeConflictRates();
    }

    public PriorityQueue<Triple<Integer, String, Double>> loadTopKAverageStateFeatures(int k) {
        PriorityQueue<Triple<Integer, String, Double>> topK = new PriorityQueue<>(k, comp);
        for (Iterator<Triplet<Integer, Integer, AveragedWeightVector>> iter = weightVector.edgeWeightIterator(); iter
                .hasNext(); ) {
            Triplet<Integer, Integer, AveragedWeightVector> r = iter.next();
            loadFeatures(r.getValue2()::getAverageWeightAt, r.getValue0(), r.getValue1(), topK, k);
        }
        return topK;
    }

    public PriorityQueue<Triple<Integer, String, Double>> loadTopKAverageFeatures(int k) {
        PriorityQueue<Triple<Integer, String, Double>> topK = new PriorityQueue<>(k, comp);
        for (Iterator<Pair<Integer, AveragedWeightVector>> iter = weightVector.nodeWeightIterator(); iter.hasNext(); ) {
            Pair<Integer, AveragedWeightVector> r = iter.next();
            loadFeatures(r.getValue1()::getAverageWeightAt, r.getValue0(), topK, k);
        }
        return topK;
    }

    public PriorityQueue<Triple<Integer, String, Double>> loadTopKFinalFeatures(int k) {
        PriorityQueue<Triple<Integer, String, Double>> topK = new PriorityQueue<>(k, comp);
        for (Iterator<Pair<Integer, AveragedWeightVector>> iter = weightVector.nodeWeightIterator(); iter.hasNext(); ) {
            Pair<Integer, AveragedWeightVector> r = iter.next();
            loadFeatures(r.getValue1()::getWeightAt, r.getValue0(), topK, k);
        }
        return topK;
    }

    public PriorityQueue<Triple<Integer, String, Double>> loadAllAverageStateFeatures() {
        PriorityQueue<Triple<Integer, String, Double>> all = new PriorityQueue<>();
        for (Iterator<Triplet<Integer, Integer, AveragedWeightVector>> iter = weightVector.edgeWeightIterator(); iter
                .hasNext(); ) {
            Triplet<Integer, Integer, AveragedWeightVector> r = iter.next();
            loadFeatures(r.getValue2()::getAverageWeightAt, r.getValue0(), r.getValue1(), all, -1);
        }
        return all;
    }

    public PriorityQueue<Triple<Integer, String, Double>> loadAllAverageFeatures() {
        PriorityQueue<Triple<Integer, String, Double>> all = new PriorityQueue<>();
        for (Iterator<Pair<Integer, AveragedWeightVector>> iter = weightVector.nodeWeightIterator(); iter.hasNext(); ) {
            Pair<Integer, AveragedWeightVector> r = iter.next();
            loadFeatures(r.getValue1()::getAverageWeightAt, r.getValue0(), all, -1);
        }
        return all;
    }

    public PriorityQueue<Triple<Integer, String, Double>> loadAllFinalFeatures() {
        PriorityQueue<Triple<Integer, String, Double>> all = new PriorityQueue<>();
        for (Iterator<Pair<Integer, AveragedWeightVector>> iter = weightVector.nodeWeightIterator(); iter.hasNext(); ) {
            Pair<Integer, AveragedWeightVector> r = iter.next();
            loadFeatures(r.getValue1()::getWeightAt, r.getValue0(), all, -1);
        }
        return all;
    }

    private void loadFeatures(Function<Integer, Double> getWeights, int classIndex,
                              PriorityQueue<Triple<Integer, String, Double>> features, int k) {
        for (int i = 0; i < weightVector.getFeatureDimension(); i++) {
            double weight = getWeights.apply(i);
            if (featureAlphabet.storeFeatureName(i)) {
                features.add(Triple.of(i, classAlphabet.getClassName(classIndex) + "_" + featureAlphabet
                        .getMappedFeatureCounters(i), weight));
            }
            if (k > 0 && features.size() > k) {
                features.poll();
            }
        }
    }

    private void loadFeatures(Function<Integer, Double> getWeights, int nextClassIndex, int previousClassIndex,
                              PriorityQueue<Triple<Integer, String, Double>> features, int k) {
        for (int i = 0; i < weightVector.getFeatureDimension(); i++) {
            double weight = getWeights.apply(i);
            if (weight != 0) {
                if (featureAlphabet.storeFeatureName(i)) {
                    features.add(Triple.of(i, classAlphabet.getClassName(previousClassIndex) + "->" +
                            classAlphabet.getClassName(nextClassIndex) + "_" +
                            featureAlphabet.getMappedFeatureCounters(i), weight));
                }
                if (k > 0 && features.size() > k) {
                    features.poll();
                }
            }
        }
    }


    private class ReverseFeatureComparator implements Comparator<Triple<Integer, String, Double>> {
        @Override
        public int compare(Triple<Integer, String, Double> f1, Triple<Integer, String, Double> f2) {
            if (f1.getRight() > f2.getRight()) {
                return 1;
            } else if (f2.getRight() > f1.getRight()) {
                return -1;
            } else {
                return f2.getLeft() - f1.getLeft();
            }
        }
    }

    public void writeInspects(File outputFile, PriorityQueue<Triple<Integer, String, Double>> features) throws
            IOException {
        LinkedList<String> lines = new LinkedList<>();
        while (!features.isEmpty()) {
            Triple<Integer, String, Double> feature = features.poll();
//            if (feature.getRight() > 0) {
            lines.addFirst(String.format("%d\t%s\t%.8f",
                    feature.getLeft(), feature.getMiddle(), feature.getRight()));
//            }
        }
        lines.addFirst("Feature Id\tFeature Name\tFeature Weight");
        FileUtils.writeLines(outputFile, lines);
    }

    public static void main(String[] args) throws IOException {
        String modelFile = args[0];
        String outputDirectory = args[1];

        GraphWeightVector model = SerializationUtils.deserialize(new FileInputStream(new File(modelFile)));

        System.out.println("Writing out feature specification first");
        FileUtils.writeLines(new File(outputDirectory, "featureSpec"),
                Arrays.asList(model.getFeatureSpec().split(";")));
        System.out.println("Writing out features.");

        HashedFeatureInspector inspector = new HashedFeatureInspector(model);

        inspector.writeInspects(new File(outputDirectory, "top100Aver"), inspector.loadTopKAverageFeatures(100));
        inspector.writeInspects(new File(outputDirectory, "top100Final"), inspector.loadTopKFinalFeatures(100));
        inspector.writeInspects(new File(outputDirectory, "allAver"), inspector.loadAllAverageFeatures());
        inspector.writeInspects(new File(outputDirectory, "allFinal"), inspector.loadAllFinalFeatures());

        inspector.writeInspects(new File(outputDirectory, "top100AverState"), inspector.loadTopKAverageStateFeatures
                (100));
        inspector.writeInspects(new File(outputDirectory, "allAverState"), inspector.loadAllAverageStateFeatures());

    }
}
