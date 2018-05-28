package edu.cmu.cs.lti.collections;

import edu.cmu.cs.lti.utils.BitUtils;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/11/14
 * Time: 3:49 PM
 */
public class TLongIntDoubleHashTable implements Serializable {
    private static final long serialVersionUID = 6390995626236546140L;
    TLongObjectHashMap<TIntDoubleMap> table = new TLongObjectHashMap<>();

    public TLongIntDoubleHashTable() {

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

        return String.format("keys: %s", sb.toString());
    }

    public String dump(String[] keyMap, Map<Integer, String> secondKeyMap) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (TLongObjectIterator<TIntDoubleMap> iter = table.iterator(); iter.hasNext(); ) {
            iter.advance();
            long key = iter.key();
            sb.append(sep);
            sep = " ; ";
            Pair<Integer, Integer> keyPair = BitUtils.get2IntFromLong(key);

            String leftKey = keyPair.getLeft() < keyMap.length ? keyMap[keyPair.getLeft()] : "-";
            String rightKey = keyPair.getRight() < keyMap.length ? keyMap[keyPair.getRight()] : "-";

            sb.append(leftKey).append(",").append(rightKey).append("\n");

            for (TIntDoubleIterator secondIter = iter.value().iterator(); secondIter.hasNext(); ) {
                secondIter.advance();
                int secondKey = secondIter.key();
                String secondKeyName = secondKeyMap.get(secondKey);
                sb.append("\t").append(secondKey).append(". ").append(secondKeyName).append(":").append(secondIter.value()).append("\n");
            }
        }

        return String.format("Two-level map:\n %s", sb.toString());
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
    public Double get(long rowKey, int colKey) {
        TIntDoubleMap row = table.get(rowKey);

        if (row != null) {
            //using no_entry_value in trove sounds dangerous, let's explicitly test
            if (row.containsKey(colKey)) {
                return row.get(colKey);
            }
        }
        return null;
    }

    public TIntDoubleMap getRow(long rowKey) {
        return table.get(rowKey);
    }

    public void put(long rowKey, int colKey, double value) {
        TIntDoubleMap row = table.get(rowKey);
        if (row != null) {
            row.put(colKey, value);
        } else {
            row = new TIntDoubleHashMap();
            row.put(colKey, value);
            table.put(rowKey, row);
        }
    }

    public boolean contains(long rowKey, int colKey) {
        return table.containsKey(rowKey) && table.get(rowKey).containsKey(colKey);
    }

    public boolean adjust(long rowKey, int colKey, double value) {
        if (contains(rowKey, colKey)) {
            table.get(rowKey).adjustValue(colKey, value);
            return true;
        } else {
            return false;
        }
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
    public double adjustOrPutValue(long rowKey, int colKey, double adjustAmount, double putAmount) {
        double newValue;
        if (table.containsKey(rowKey)) {
            newValue = table.get(rowKey).adjustOrPutValue(colKey, adjustAmount, putAmount);
        } else {
            TIntDoubleMap row = new TIntDoubleHashMap();
            row.put(colKey, putAmount);
            newValue = putAmount;
            table.put(rowKey, row);
        }
        return newValue;
    }

    public TLongObjectIterator<TIntDoubleMap> iterator() {
        return table.iterator();
    }

    public void clear() {
        table.clear();
    }

    public double dotProd(TLongIntDoubleHashTable features) {
        double dotProd = 0;
        for (TLongObjectIterator<TIntDoubleMap> firstLevelIter = features.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            if (table.containsKey(featureRowKey)) {
                TIntDoubleMap weightsRow = table.get(featureRowKey);
                TIntDoubleMap secondLevelFeatures = firstLevelIter.value();
                for (TIntDoubleIterator secondLevelIter = secondLevelFeatures.iterator(); secondLevelIter.hasNext(); ) {
                    secondLevelIter.advance();
                    if (weightsRow.containsKey(secondLevelIter.key())) {
                        dotProd += secondLevelIter.value() * weightsRow.get(secondLevelIter.key());
                    }
                }
            }
        }
        return dotProd;
    }

    public void multiplyBy(double weight) {
        for (TLongObjectIterator<TIntDoubleMap> firstLevelIter = table.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            TIntDoubleMap row = firstLevelIter.value();
            for (TIntDoubleIterator rowIter = row.iterator(); rowIter.hasNext(); ) {
                rowIter.advance();
                rowIter.setValue(rowIter.value() * weight);
            }
        }
    }

    public void adjustBy(TLongIntDoubleHashTable adjustVec, double mul) {
        for (TLongObjectIterator<TIntDoubleMap> firstLevelIter = adjustVec.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            if (table.containsKey(featureRowKey)) {
                TIntDoubleMap weightsRow = table.get(featureRowKey);
                for (TIntDoubleIterator secondLevelIter = firstLevelIter.value().iterator(); secondLevelIter.hasNext(); ) {
                    secondLevelIter.advance();
                    weightsRow.adjustOrPutValue(secondLevelIter.key(), secondLevelIter.value() * mul, secondLevelIter.value() * mul);
                }
            } else {
                TIntDoubleMap newMap = new TIntDoubleHashMap();
                table.put(featureRowKey, newMap);

                for (TIntDoubleIterator secondLevelIter = firstLevelIter.value().iterator(); secondLevelIter.hasNext(); ) {
                    secondLevelIter.advance();
                    newMap.put(secondLevelIter.key(), secondLevelIter.value() * mul);
                }
            }
        }
    }

    public void minusBy(TLongIntDoubleHashTable minusVec) {
        adjustBy(minusVec, -1);
    }

}
