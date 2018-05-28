package edu.cmu.cs.lti.feature;

import gnu.trove.iterator.TObjectFloatIterator;
import gnu.trove.map.TObjectFloatMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/29/15
 * Time: 7:29 PM
 *
 * @author Zhengzhong Liu
 */
public class ArrayBasedFeatureContainer {
    private TObjectIntMap<String> featureNameMap;

    private float[] weightVector;

    private int newFeatureIndex = 0;

    public ArrayBasedFeatureContainer() {
        this.featureNameMap = new TObjectIntHashMap<>();
    }

    public void addFeatureName(String featureName) {
        featureNameMap.putIfAbsent(featureName, newFeatureIndex++);
    }

    public TObjectIntMap getFeatureConfig() {
        return featureNameMap;
    }

    public float[] getWeights() {
        return weightVector;
    }

    public void finishConfig() {
        weightVector = new float[featureNameMap.size()];
    }

    public void update(TObjectFloatMap<String> features) {
        for (TObjectFloatIterator<String> iter = features.iterator(); iter.hasNext(); ) {
            iter.advance();
            weightVector[featureNameMap.get(iter.key())] = iter.value();
        }
    }
}
