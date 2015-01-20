package edu.cmu.cs.lti.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/18/15
 * Time: 3:31 PM
 */
public abstract class TwoLevelFeatureTable implements Serializable {
    private static final long serialVersionUID = 1431921441554123194L;

    BiMap<String, Short> secondaryFeatureLookupMap = HashBiMap.create();

    short nextKey = Short.MIN_VALUE;

    public TwoLevelFeatureTable() {

    }

    public TwoLevelFeatureTable(TwoLevelFeatureTable baseTable) {
        this.secondaryFeatureLookupMap = baseTable.getFeatureMap();
    }

    public TwoLevelFeatureTable(BiMap<String, Short> secondaryFeatureLookupMap) {
        this.secondaryFeatureLookupMap = secondaryFeatureLookupMap;
    }

    /**
     * Will automatic assign a new numeric value for a new feature
     *
     * @param featureName
     * @return
     */
    public short getOrPutFeatureIndex(String featureName) {
        if (secondaryFeatureLookupMap.containsKey(featureName)) {
            return secondaryFeatureLookupMap.get(featureName);
        } else {
            secondaryFeatureLookupMap.put(featureName, nextKey);
            short currentKey = nextKey;
            nextKey++;
            if (nextKey == Short.MIN_VALUE) {
                //this will only happen when we circuit around
                throw new IllegalStateException("You have used up all shorts for features!");
            }
            return currentKey;
        }
    }


    public Short getFeatureIndex(String featureName) {
        return secondaryFeatureLookupMap.get(featureName);
    }

    public String getFeatureName(short featureIndex) {
        return secondaryFeatureLookupMap.inverse().get(featureIndex);
    }

    public BiMap<String, Short> getFeatureMap() {
        return secondaryFeatureLookupMap;
    }

    public BiMap<Short, String> getFeatureNameMap() {
        return secondaryFeatureLookupMap.inverse();
    }


    public abstract int getNumRows();

    /**
     * The data are stored in primitive, returning an object for easy null-check
     *
     * @param rowKey
     * @param colKey
     * @return return the value as Double object
     */
    public abstract Double get(long rowKey, short colKey);

    public abstract void put(long rowKey, short colKey, double value);

    public abstract boolean contains(long rowKey, short colKey);

    public abstract boolean containsRow(long rowKey);

    public abstract boolean adjust(long rowKey, short colKey, double value);

    /**
     * Adjusts the primitive value mapped to the key if the key pair is present in the map. Otherwise, the initial_value is put in the map.
     *
     * @param rowKey       the row key of the value to increment
     * @param colKey       the column key of the value to increment
     * @param adjustAmount the amount to increment the value by
     * @param putAmount    the value put into the map if the key is not initial present
     * @return the value present in the map after the adjustment or put operation
     */
    public abstract double adjustOrPutValue(long rowKey, short colKey, double adjustAmount, double putAmount);

    public abstract double dotProd(TLongShortDoubleHashTable features);

    public abstract void adjustBy(TLongShortDoubleHashTable adjustVect, double mul);
}
