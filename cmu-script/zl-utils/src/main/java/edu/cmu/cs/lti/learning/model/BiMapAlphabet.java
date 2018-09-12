package edu.cmu.cs.lti.learning.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/11/15
 * Time: 11:26 AM
 *
 * @author Zhengzhong Liu
 */
public class BiMapAlphabet extends FeatureAlphabet {
    private static final long serialVersionUID = -4988790738326139279L;
    private BiMap<String, Integer> featureBiMap;

    private AtomicInteger featureIdCounter;

    private boolean fixMap = false;

    public BiMapAlphabet() {
        super();
        featureIdCounter = new AtomicInteger();
        featureBiMap = HashBiMap.create();
    }

    public void fixMap() {
        fixMap = true;
    }

    @Override
    public int getFeatureId(String featureName) {
        int featureId;
        if (featureBiMap.containsKey(featureName)) {
            featureId = featureBiMap.get(featureName);
        } else if (fixMap) {
            return -1;
        } else {
            featureId = featureIdCounter.getAndIncrement();
            featureBiMap.put(featureName, featureId);
        }
        return featureId;
    }

    @Override
    public String[] getFeatureNames(int featureIndex) {
        return new String[]{featureBiMap.inverse().get(featureIndex)};
    }

    @Override
    public String getFeatureNameRepre(int featureIndex) {
        return featureBiMap.inverse().get(featureIndex);
    }

    @Override
    public int getAlphabetSize() {
        return featureBiMap.size();
    }

    public Set<Map.Entry<String, Integer>> getAllFeatureNames() {
        return featureBiMap.entrySet();
    }
}
