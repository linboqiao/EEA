package edu.cmu.cs.lti.learning.model;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.SerializationUtils;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/16/15
 * Time: 8:28 PM
 *
 * @author Zhengzhong Liu
 */
public class GraphWeightVector implements Serializable {
    private static final long serialVersionUID = 5181873403599233786L;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TIntObjectMap<AveragedWeightVector> nodeWeights;

    // <Current Key, Previous Key, Vector>
    private Table<Integer, Integer, AveragedWeightVector> edgeWeights;

    private final ClassAlphabet classAlphabet;

    private final FeatureAlphabet featureAlphabet;

    // Specification of the feature extractors, used to validate whether the weights are trained using the same set
    // of features.
    private final String featureSpec;

    private int averageUpdateCount;

    public GraphWeightVector(ClassAlphabet classAlphabet, FeatureAlphabet featureAlphabet, String featureSpec) {
        nodeWeights = new TIntObjectHashMap<>();
        edgeWeights = HashBasedTable.create();

        this.featureAlphabet = featureAlphabet;
        this.classAlphabet = classAlphabet;
        this.featureSpec = featureSpec;

        averageUpdateCount = 0;
    }

    private AveragedWeightVector newWeightVector() {
        return new HashBasedAveragedWeightVector(averageUpdateCount);
    }


    public AveragedWeightVector getNodeWeights(String className) {
        return nodeWeights.get(classAlphabet.getClassIndex(className));
    }

    public AveragedWeightVector getNodeWeights(int classIndex) {
        return nodeWeights.get(classIndex);
    }

    public AveragedWeightVector getEdgeWeights(int currentKey, int previousKey) {
        return edgeWeights.get(currentKey, previousKey);
    }

    private synchronized AveragedWeightVector getOrCreateNodeWeights(int classIndex) {
        if (nodeWeights.containsKey(classIndex)) {
            return nodeWeights.get(classIndex);
        } else {
            AveragedWeightVector v = newWeightVector();
            nodeWeights.put(classIndex, v);
            return v;
        }
    }

    private synchronized AveragedWeightVector getOrCreateEdgeWeights(int currentKey, int previousKey) {
        if (edgeWeights.contains(currentKey, previousKey)) {
            return edgeWeights.get(currentKey, previousKey);
        } else {
            AveragedWeightVector v = newWeightVector();
            edgeWeights.put(currentKey, previousKey, v);
            return v;
        }
    }

    public Iterator<Pair<Integer, AveragedWeightVector>> nodeWeightIterator() {
        TIntObjectIterator<AveragedWeightVector> iter = nodeWeights.iterator();

        return new Iterator<Pair<Integer, AveragedWeightVector>>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Pair<Integer, AveragedWeightVector> next() {
                iter.advance();
                return Pair.with(iter.key(), iter.value());
            }
        };
    }

    public Iterator<Triplet<Integer, Integer, AveragedWeightVector>> edgeWeightIterator() {
        Iterator<Table.Cell<Integer, Integer, AveragedWeightVector>> iter = edgeWeights.cellSet().iterator();

        return new Iterator<Triplet<Integer, Integer, AveragedWeightVector>>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Triplet<Integer, Integer, AveragedWeightVector> next() {
                Table.Cell<Integer, Integer, AveragedWeightVector> cell = iter.next();
                return Triplet.with(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
            }
        };
    }

    public synchronized void updateWeightsBy(FeatureVector fv, int currentKey, double multiplier) {
        AveragedWeightVector weightVector = getOrCreateNodeWeights(currentKey);
        weightVector.updateWeightsBy(fv, multiplier);
    }

    public synchronized void updateWeightsBy(FeatureVector fv, int currentKey, int previousKey, double multiplier) {
//        logger.debug("Updating features for " + classAlphabet.getClassName(currentKey) + " and " + classAlphabet
//                .getClassName(previousKey) + " by " + multiplier);
//        logger.debug(fv.readableString());
        getOrCreateEdgeWeights(currentKey, previousKey).updateWeightsBy(fv, multiplier);
    }

    public synchronized void updateWeightsBy(GraphFeatureVector updateVector, double multiplier) {
        for (TIntObjectIterator<FeatureVector> iter = updateVector.nodeFvIter(); iter.hasNext(); ) {
            iter.advance();
            updateWeightsBy(iter.value(), iter.key(), multiplier);
        }

        for (Iterator<Table.Cell<Integer, Integer, FeatureVector>> iter = updateVector.edgeFvIter(); iter.hasNext(); ) {
            Table.Cell<Integer, Integer, FeatureVector> v = iter.next();
            updateWeightsBy(v.getValue(), v.getRowKey(), v.getColumnKey(), multiplier);
        }
    }

    public double dotProd(FeatureVector fv, String classLabel) {
        return dotProd(fv, classAlphabet.getClassIndex(classLabel));
    }

    public double dotProd(FeatureVector fv, int nodeKey) {
        AveragedWeightVector weights = getOrCreateNodeWeights(nodeKey);
        return weights.dotProd(fv);
    }

    public double dotProd(GraphFeatureVector fv) {
        double prod = 0;
        for (TIntObjectIterator<FeatureVector> iter = fv.nodeFvIter(); iter.hasNext(); ) {
            iter.advance();
            prod += dotProd(iter.value(), iter.key());
        }

        for (Iterator<Table.Cell<Integer, Integer, FeatureVector>> iter = fv.edgeFvIter(); iter.hasNext(); ) {
            Table.Cell<Integer, Integer, FeatureVector> cell = iter.next();
            prod += dotProd(cell.getValue(), cell.getRowKey(), cell.getColumnKey());
        }

        return prod;
    }

    public double dotProd(FeatureVector fv, int currentKey, int previousKey) {
        AveragedWeightVector weights = getOrCreateEdgeWeights(currentKey, previousKey);
        if (weights != null) {
            return weights.dotProd(fv);
        } else {
            return 0;
        }
    }

    public double dotProdAver(FeatureVector fv, String classLabel) {
        return dotProdAver(fv, classAlphabet.getClassIndex(classLabel));
    }

    public double dotProdAver(FeatureVector fv, int nodeKey) {
        AveragedWeightVector weights = getOrCreateNodeWeights(nodeKey);
        return weights.dotProdAver(fv);
    }

    public double dotProdAver(FeatureVector fv, int currentKey, int previousKey) {
        AveragedWeightVector weights = getOrCreateEdgeWeights(currentKey, previousKey);
        return weights.dotProdAver(fv);
    }

    public void write(File outputFile) throws FileNotFoundException {
        consolidate();
        SerializationUtils.serialize(this, new FileOutputStream(outputFile));
        deconsolidate();
    }

    private synchronized void consolidate() {
        logger.info("Consolidating graph weights.");
        applyToAll(AveragedWeightVector::consolidate);
    }

    public synchronized void deconsolidate() {
        logger.info("Deconsolidating graph weights.");
        applyToAll(AveragedWeightVector::deconsolidate);
    }

    private void applyToAll(Consumer<AveragedWeightVector> oper) {
        for (AveragedWeightVector nodeWeight : nodeWeights.valueCollection()) {
            if (nodeWeight != null) {
                oper.accept(nodeWeight);
            }
        }

        for (AveragedWeightVector edgeWeightVector : edgeWeights.values()) {
            if (edgeWeightVector != null) {
                oper.accept(edgeWeightVector);
            }
        }
    }

    public FeatureAlphabet getFeatureAlphabet() {
        return featureAlphabet;
    }

    public ClassAlphabet getClassAlphabet() {
        return classAlphabet;
    }

    public int getFeatureDimension() {
        return featureAlphabet.getAlphabetSize();
    }

    public void updateAverageWeights() {
        applyToAll(AveragedWeightVector::updateAverageWeight);
        averageUpdateCount++;
    }

    public String getFeatureSpec() {
        return featureSpec;
    }
}
