package edu.cmu.cs.lti.collections;

import edu.cmu.cs.lti.model.MutableDouble;
import edu.cmu.cs.lti.utils.BitUtils;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.iterator.TShortDoubleIterator;
import gnu.trove.map.TShortDoubleMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * The secondary map of this table is a tree map, which does not use additional
 * array space. The value is implemented as a MutableDouble for fast modification
 * <p/>
 * Created with ShortelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/11/14
 * Time: 3:49 PM
 */
public class TLongShortDoubleTreeTable implements Serializable {
    private static final long serialVersionUID = 6390995626236546140L;
    TLongObjectHashMap<TreeMap<Short, MutableDouble>> table = new TLongObjectHashMap<>();


    public TLongShortDoubleTreeTable() {

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (long key : table.keys()) {
            sb.append(sep);
            sep = " ; ";
            Pair<Integer, Integer> keyPair = BitUtils.get2IntFromLong(key);
            sb.append(keyPair.toString());
        }

        return String.format("Features: lexical item , %s", sb.toString());
    }

    public int getNumRows() {
        return table.size();
    }

    /**
     * The data are stored in primitive, returning an object for easy null-check
     *
     * @param rowKey
     * @param colKey
     * @return return the value as Double object
     */
    public Double get(long rowKey, short colKey) {
        TreeMap<Short, MutableDouble> row = table.get(rowKey);
        if (row != null) {
            MutableDouble value = row.get(colKey);
            return value == null ? null : value.get();
        }
        return null;
    }

    public TreeMap<Short, MutableDouble> getRow(long rowKey) {
        return table.get(rowKey);
    }

    public void put(long rowKey, short colKey, double value) {
        TreeMap<Short, MutableDouble> row = table.get(rowKey);
        MutableDouble mutableVal = new MutableDouble(value);
        if (row != null) {
            row.put(colKey, mutableVal);
        } else {
            row = new TreeMap<>();
            row.put(colKey, mutableVal);
            table.put(rowKey, row);
        }
    }

    public boolean contains(long rowKey, short colKey) {
        return table.containsKey(rowKey) && table.get(rowKey).containsKey(colKey);
    }

    public boolean containsRow(long rowKey) {
        return table.containsKey(rowKey);
    }

    public boolean adjust(long rowKey, short colKey, double value) {
        if (contains(rowKey, colKey)) {
            TreeMap<Short, MutableDouble> row = table.get(rowKey);
            return adjustRow(row, colKey, value);
        } else {
            return false;
        }
    }

    /**
     * Adjusts the primitive value mapped to the key if the key pair is present in the map. Otherwise, the initial_value is put in the map.
     * This is pretty expensive because contains and put are log(N) operations here
     *
     * @param rowKey       the row key of the value to increment
     * @param colKey       the column key of the value to increment
     * @param adjustAmount the amount to increment the value by
     * @param putAmount    the value put shorto the map if the key is not initial present
     * @return the value present in the map after the adjustment or put operation
     */
    public double adjustOrPutValue(long rowKey, short colKey, double adjustAmount, double putAmount) {
        double newValue;
        if (table.containsKey(rowKey)) {
            TreeMap<Short, MutableDouble> row = table.get(rowKey);
            newValue = adjustOrPutValueToRow(row, colKey, adjustAmount, putAmount);
        } else {
            TreeMap<Short, MutableDouble> row = new TreeMap<>();
            row.put(colKey, new MutableDouble(putAmount));
            newValue = putAmount;
            table.put(rowKey, row);
        }
        return newValue;
    }

    private double adjustOrPutValueToRow(TreeMap<Short, MutableDouble> row, short colKey, double adjustAmount, double putAmount) {
        double newValue;
        MutableDouble originValue = row.get(colKey);
        if (originValue == null) {
            row.put(colKey, new MutableDouble(putAmount));
            newValue = putAmount;
        } else {
            newValue = originValue.increment(adjustAmount);
        }

        return newValue;
    }

    private boolean adjustRow(TreeMap<Short, MutableDouble> row, short colKey, double adjustAmount) {
        MutableDouble originValue = row.get(colKey);
        if (originValue == null) {
            return false;
        } else {
            originValue.increment(adjustAmount);
            return true;
        }
    }

    public TLongObjectIterator<TreeMap<Short, MutableDouble>> iterator() {
        return table.iterator();
    }

    public void clear() {
        table.clear();
    }

    public double dotProd(TLongShortDoubleHashTable features) {
        double dotProd = 0;
        for (TLongObjectIterator<TShortDoubleMap> firstLevelIter = features.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            if (table.containsKey(featureRowKey)) {
                TreeMap<Short, MutableDouble> weightsRow = table.get(featureRowKey);
                TShortDoubleMap secondLevelFeatures = firstLevelIter.value();
                for (TShortDoubleIterator secondLevelIter = secondLevelFeatures.iterator(); secondLevelIter.hasNext(); ) {
                    secondLevelIter.advance();
                    if (weightsRow.containsKey(secondLevelIter.key())) {
                        dotProd += secondLevelIter.value() * weightsRow.get(secondLevelIter.key()).get();
                    }
                }
            }
        }
        return dotProd;
    }

    public void multiplyBy(double weight) {
        for (TLongObjectIterator<TreeMap<Short, MutableDouble>> firstLevelIter = table.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            TreeMap<Short, MutableDouble> row = firstLevelIter.value();

            for (Map.Entry<Short, MutableDouble> entry : row.entrySet()) {
                entry.getValue().multiply(weight);
            }
        }
    }

    public void adjustBy(TLongShortDoubleHashTable adjustVec, double mul) {
        for (TLongObjectIterator<TShortDoubleMap> firstLevelIter = adjustVec.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            if (table.containsKey(featureRowKey)) {
                TreeMap<Short, MutableDouble> weightsRow = table.get(featureRowKey);
                for (TShortDoubleIterator secondLevelIter = firstLevelIter.value().iterator(); secondLevelIter.hasNext(); ) {
                    secondLevelIter.advance();
                    adjustOrPutValueToRow(weightsRow, secondLevelIter.key(), -secondLevelIter.value(), secondLevelIter.value() * mul);
                }
            } else {
                TreeMap<Short, MutableDouble> newRow = new TreeMap<>();
                table.put(featureRowKey, newRow);
                for (TShortDoubleIterator secondLevelIter = firstLevelIter.value().iterator(); secondLevelIter.hasNext(); ) {
                    secondLevelIter.advance();
                    newRow.put(secondLevelIter.key(), new MutableDouble(secondLevelIter.value() * mul));
                }
            }
        }
    }

    public void adjustBy(TLongShortDoubleTreeTable adjustVec, double mul) {
        for (TLongObjectIterator<TreeMap<Short, MutableDouble>> firstLevelIter = adjustVec.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            if (table.containsKey(featureRowKey)) {
                TreeMap<Short, MutableDouble> weightsRow = table.get(featureRowKey);
                for (Map.Entry<Short, MutableDouble> cellEntry : firstLevelIter.value().entrySet()) {
                    adjustOrPutValueToRow(weightsRow, cellEntry.getKey(), -cellEntry.getValue().get(), -cellEntry.getValue().get() * mul);
                }
            } else {
                TreeMap<Short, MutableDouble> newRow = new TreeMap<>();
                table.put(featureRowKey, newRow);
                for (Map.Entry<Short, MutableDouble> cellEntry : firstLevelIter.value().entrySet()) {
                    adjustOrPutValueToRow(newRow, cellEntry.getKey(), -cellEntry.getValue().get(), -cellEntry.getValue().get() * mul);
                }
            }
        }
    }

    public void minusBy(TLongShortDoubleHashTable minusVec) {
        adjustBy(minusVec, -1);
    }
}
