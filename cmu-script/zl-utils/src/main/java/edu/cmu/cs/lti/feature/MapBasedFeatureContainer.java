package edu.cmu.cs.lti.feature;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/1/15
 * Time: 3:14 PM
 *
 * @author Zhengzhong Liu
 */
public class MapBasedFeatureContainer implements Serializable {
    private static final long serialVersionUID = 2212499903248517259L;

    private TObjectIntMap<String> featureNameMap;

    private TIntDoubleMap latestWeights;

    private TIntDoubleMap totalWeights;

    private TIntIntMap lastUpdateTimes;

    private TIntDoubleMap averageWeights;

    private int featureCounter = 0;

    private int numUpdates = 0;

    public static final int DEFAULT_NO_FEATURE_INDEX = -1;

    public MapBasedFeatureContainer() {
        this.featureNameMap = new TObjectIntHashMap<>();
        this.latestWeights = new TIntDoubleHashMap();
        this.totalWeights = new TIntDoubleHashMap();
        this.lastUpdateTimes = new TIntIntHashMap();
        this.averageWeights = new TIntDoubleHashMap();
    }

    public void update(TObjectDoubleMap<String> realFeatures, TObjectDoubleMap<String> wrongFeatures, double updateStep) {
        TObjectDoubleMap<String> jointFeatures = new TObjectDoubleHashMap<>();
        jointFeatures.putAll(realFeatures);
        for (TObjectDoubleIterator<String> featureIter = wrongFeatures.iterator(); featureIter.hasNext(); ) {
            featureIter.advance();
            jointFeatures.adjustOrPutValue(featureIter.key(), -featureIter.value(), -featureIter.value());
        }
        update(jointFeatures, updateStep);
    }

    public void update(TObjectDoubleMap<String> features, double updateStep) {
        for (TObjectDoubleIterator<String> featureIter = features.iterator(); featureIter.hasNext(); ) {
            featureIter.advance();
            int fIndex = getFeatureIndex(featureIter.key(), true);
            latestWeights.adjustOrPutValue(fIndex, updateStep, updateStep);
            double cumulativeUpdateSum = (numUpdates - lastUpdateTimes.get(fIndex)) * updateStep;
            totalWeights.adjustOrPutValue(fIndex, cumulativeUpdateSum, cumulativeUpdateSum);
            lastUpdateTimes.put(fIndex, numUpdates);
        }
        numUpdates++;
    }

    public void computeAverageWeights() {
        for (TIntDoubleIterator weightIter = latestWeights.iterator(); weightIter.hasNext(); ) {
            weightIter.advance();
            double total = totalWeights.get(weightIter.key());
            //accumulate the residual weights
            total += (numUpdates - lastUpdateTimes.get(weightIter.key())) * weightIter.value();
            double average = total / numUpdates;

            if (average != 0) {
                averageWeights.put(weightIter.key(), average);
            }
        }
    }

    public double score(TObjectDoubleMap<String> features) {
        return dotProd(features);
    }

    public double dotProd(TObjectDoubleMap<String> features) {
        return dotProd(features, latestWeights);
    }

    public double averageScore(TObjectDoubleMap<String> features) {
        return dotProd(features, averageWeights);
    }

    public double dotProd(TObjectDoubleMap<String> features, TIntDoubleMap weights) {
        double dotProd = 0;
        for (TObjectDoubleIterator<String> featureIter = features.iterator(); featureIter.hasNext(); ) {
            featureIter.advance();
            int key = featureNameMap.get(featureIter.key());
            double featureValue = featureIter.value();
            dotProd += featureValue * weights.get(key);
        }
        return dotProd;
    }

    public TIntDoubleMap getWeights() {
        return latestWeights;
    }

    public TIntDoubleMap getAverageWeights() {
        return averageWeights;
    }

    public void serialize(File outputFile) throws FileNotFoundException {
        File parent = outputFile.getParentFile();
        if (!parent.isDirectory()) {
            parent.mkdirs();
        }
        SerializationUtils.serialize(outputFile, new FileOutputStream(outputFile));
    }

    private int getFeatureIndex(String featureName, boolean create) {
        if (featureNameMap.containsKey(featureName)) {
            return featureNameMap.get(create);
        } else {
            if (create) {
                int featureIndex = featureCounter++;
                featureNameMap.put(featureName, featureIndex);
                return featureIndex;
            } else {
                return DEFAULT_NO_FEATURE_INDEX;
            }
        }
    }

    private int getFeatureIndex(String featureName) {
        return getFeatureIndex(featureName, false);
    }
}