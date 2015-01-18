package edu.cmu.cs.lti.utils;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import gnu.trove.iterator.TLongObjectIterator;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/18/15
 * Time: 3:31 PM
 */
public abstract interface FeatureTable {
    public abstract int getNumRows();

    /**
     * Will automatic assign a new numeric value for a new feature
     *
     * @param featureName
     * @return
     */
    public abstract short getOrPutFeatureIndex(String featureName);

    public abstract TLongObjectIterator iterator();

    public abstract Short getFeatureIndex(String featureName);

    public abstract String getFeatureName(short featureIndex);

    public abstract BiMap<String, Short> getFeatureMap();

    public abstract BiMap<Short, String> getFeatureNameMap();

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

    public abstract double dotProd(TLongShortDoubleHashTable features, Map<Short, String> featureNames, String[] headWords);
}
