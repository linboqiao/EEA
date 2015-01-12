package edu.cmu.cs.lti.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import edu.cmu.cs.lti.collections.TLongShortDoubleTreeTable;
import edu.cmu.cs.lti.model.MutableDouble;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.iterator.TShortDoubleIterator;
import gnu.trove.map.TShortDoubleMap;
import org.apache.commons.lang3.tuple.Pair;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/11/14
 * Time: 3:49 PM
 */
public class TLongBasedFeatureTable implements Serializable {
    private static final long serialVersionUID = 1574621409197680994L;
    /**
     * Long is the main key, which can encode two integers (then map to two words)
     * <p/>
     * Int is the secondary key, which point to next specific feature, maintained
     * in a separate feature lookup table, which should be pretty small,do not
     * populate a lot of word features over there
     * <p/>
     * Double is the feature value to be update
     */
    TLongShortDoubleTreeTable table = new TLongShortDoubleTreeTable();

    //    TObjectShortHashMap<String> secondaryFeatureLookupMap = new TObjectShortHashMap<>();
    BiMap<String, Short> secondaryFeatureLookupMap = HashBiMap.create();

    short nextKey = Short.MIN_VALUE;

    public TLongBasedFeatureTable() {

    }

    public TLongBasedFeatureTable(TLongShortDoubleTreeTable table, BiMap<String, Short> secondaryFeatureLookupMap) {
        this.table = table;
        this.secondaryFeatureLookupMap = secondaryFeatureLookupMap;
    }


    public int getNumRows() {
        return table.getNumRows();
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

    public TLongObjectIterator<TreeMap<Short, MutableDouble>> iterator() {
        return table.iterator();
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
//        TShortObjectMap<String> featureNames = new TShortObjectHashMap<>();
//        for (TObjectShortIterator<String> iter = secondaryFeatureLookupMap.iterator(); iter.hasNext(); ) {
//            iter.advance();
//            featureNames.put(iter.value(), iter.key());
//        }
//        return featureNames;
        return secondaryFeatureLookupMap.inverse();
    }

    /**
     * The data are stored in primitive, returning an object for easy null-check
     *
     * @param rowKey
     * @param colKey
     * @return return the value as Double object
     */
    public Double get(long rowKey, short colKey) {
        return table.get(rowKey, colKey);
    }

    public TreeMap<Short, MutableDouble> getRow(long rowKey) {
        return table.getRow(rowKey);
    }

    public void put(long rowKey, short colKey, double value) {
        table.put(rowKey, colKey, value);
    }

    public boolean contains(long rowKey, short colKey) {
        return table.contains(rowKey, colKey);
    }

    public boolean containsRow(long rowKey) {
        return table.containsRow(rowKey);
    }

    public boolean adjust(long rowKey, short colKey, double value) {
        return table.adjust(rowKey, colKey, value);
    }

    /**
     * Adjusts the primitive value mapped to the key if the key pair is present in the map. Otherwise, the initial_value is put in the map.
     *
     * @param rowKey       the row key of the value to increment
     * @param colKey       the column key of the value to increment
     * @param adjustAmount the amount to increment the value by
     * @param putAmount    the value put into the map if the key is not initial present
     * @return the value present in the map after the adjustment or put operation
     */
    public double adjustOrPutValue(long rowKey, short colKey, double adjustAmount, double putAmount) {
        return table.adjustOrPutValue(rowKey, colKey, adjustAmount, putAmount);
    }

    public double dotProd(TLongShortDoubleHashTable features) {
        return table.dotProd(features);
    }

    public void adjustBy(TLongShortDoubleHashTable adjustVect, double mul) {
        table.adjustBy(adjustVect, mul);
    }

    public double dotProd(TLongShortDoubleHashTable features, Map<Short, String> featureNames, String[] headWords) {
        double dotProd = 0;
        for (TLongObjectIterator<TShortDoubleMap> firstLevelIter = features.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            if (table.containsRow(featureRowKey)) {
                TreeMap<Short, MutableDouble> weightsRow = table.getRow(featureRowKey);
                TShortDoubleMap secondLevelFeatures = firstLevelIter.value();
                for (TShortDoubleIterator secondLevelIter = secondLevelFeatures.iterator(); secondLevelIter.hasNext(); ) {
                    secondLevelIter.advance();
                    Pair<Integer, Integer> wordIndexPair = BitUtils.get2IntFromLong(featureRowKey);
//                    System.err.println("Feature is " + DataPool.headWords[wordIndexPair.getLeft()] + " " +
//                            DataPool.headWords[wordIndexPair.getRight()] + " " + wordIndexPair.getLeft() + " " + wordIndexPair.getRight() + " " + featureNames.get(secondLevelIter.key()) + ":" + secondLevelIter.key());
                    if (weightsRow.containsKey(secondLevelIter.key())) {
                        dotProd += secondLevelIter.value() * weightsRow.get(secondLevelIter.key()).get();
                        System.err.println("Feature hit " + headWords[wordIndexPair.getLeft()] + " " +
                                headWords[wordIndexPair.getRight()] + " " + featureNames.get(secondLevelIter.key()) + " : " +
                                weightsRow.get(secondLevelIter.key()).get());
                    }
                }
            }
        }
        return dotProd;
    }

    public Pair<Integer, Integer> dump(PrintWriter writer, String[] headWords) {
        int numFeatures = 0;
        int numLexicalPairs = 0;
        BiMap<Short, String> featureNames = getFeatureNameMap();
        for (TLongObjectIterator<TreeMap<Short, MutableDouble>> rowIter = table.iterator(); rowIter.hasNext(); ) {
            rowIter.advance();
            numLexicalPairs++;
            Pair<Integer, Integer> wordIds = BitUtils.get2IntFromLong(rowIter.key());
            writer.write(headWords[wordIds.getKey()] + " " + headWords[wordIds.getValue()] + " " + wordIds.getKey() + " " + wordIds.getValue() + "\n");

            for (Map.Entry<Short, MutableDouble> cell : rowIter.value().entrySet()) {
                writer.write(featureNames.get(cell.getKey()) + " " + cell.getKey() + " " + cell.getValue() + "\n");
                numFeatures++;
            }
        }
//        System.out.println("Number of lexical pairs " + numLexicalPairs + " , num of features " + numFeatures);
        return Pair.of(numFeatures, numLexicalPairs);
    }

}
