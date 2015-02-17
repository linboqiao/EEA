package edu.cmu.cs.lti.utils;

import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.iterator.TShortDoubleIterator;
import gnu.trove.map.TShortDoubleMap;
import gnu.trove.map.hash.TShortDoubleHashMap;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/20/15
 * Time: 2:39 PM
 */
public class ArrayBasedTwoLevelFeatureTable extends TwoLevelFeatureTable {
    private static final long serialVersionUID = 5335403133116054451L;

    TShortDoubleMap[][] table;

    int numRows = 0;

    int numFeatures = 0;

    public ArrayBasedTwoLevelFeatureTable(int vocabularySize) {
        table = new TShortDoubleMap[vocabularySize][vocabularySize];
    }

    @Override
    public int getNumRows() {
        return numRows;
    }

    public TShortDoubleMap getRow(long rowKey) {
        Pair<Integer, Integer> tableKeys = BitUtils.get2IntFromLong(rowKey);
        return table[tableKeys.getLeft()][tableKeys.getRight()];
    }


    private void newRow(int row, int col) {
        table[row][col] = new TShortDoubleHashMap();
        numRows++;
    }

    private TShortDoubleMap getOrNewRow(long rowKey) {
        Pair<Integer, Integer> tableKeys = BitUtils.get2IntFromLong(rowKey);
        if (table[tableKeys.getLeft()][tableKeys.getRight()] == null) {
            newRow(tableKeys.getLeft(), tableKeys.getRight());
        }
        return table[tableKeys.getLeft()][tableKeys.getRight()];
    }

    @Override
    public Double get(long rowKey, short colKey) {
        return getRow(rowKey).get(colKey);
    }


    @Override
    public void put(long rowKey, short colKey, double value) {
        TShortDoubleMap row = getOrNewRow(rowKey);
        row.put(colKey, value);
    }

    @Override
    public boolean contains(long rowKey, short colKey) {
        TShortDoubleMap row = getRow(rowKey);
        return row != null && row.containsKey(colKey);
    }

    @Override
    public boolean containsRow(long rowKey) {
        return getRow(rowKey) != null;
    }

    @Override
    public boolean adjust(long rowKey, short colKey, double value) {
        TShortDoubleMap row = getRow(rowKey);
        if (row != null) {
            return row.adjustValue(colKey, value);
        } else {
            return false;
        }
    }

    @Override
    public double adjustOrPutValue(long rowKey, short colKey, double adjustAmount, double putAmount) {
        TShortDoubleMap row = getOrNewRow(rowKey);
        return row.adjustOrPutValue(colKey, adjustAmount, putAmount);
    }

    @Override
    public double dotProd(TLongShortDoubleHashTable features) {
        double dotProd = 0;
        for (TLongObjectIterator<TShortDoubleMap> firstLevelIter = features.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            if (containsRow(featureRowKey)) {
                TShortDoubleMap weightsRow = getRow(featureRowKey);
                TShortDoubleMap secondLevelFeatures = firstLevelIter.value();
                for (TShortDoubleIterator secondLevelIter = secondLevelFeatures.iterator(); secondLevelIter.hasNext(); ) {
                    secondLevelIter.advance();
                    if (weightsRow.containsKey(secondLevelIter.key())) {
                        dotProd += secondLevelIter.value() * weightsRow.get(secondLevelIter.key());
                    }
                }
            }
        }
        return dotProd;
    }

    public double dotProd(TLongShortDoubleHashTable features, String[] headWords) {
        double dotProd = 0;
        for (TLongObjectIterator<TShortDoubleMap> firstLevelIter = features.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            if (containsRow(featureRowKey)) {
                Pair<Integer, Integer> wordIndexPair = BitUtils.get2IntFromLong(featureRowKey);

                TShortDoubleMap weightsRow = getRow(featureRowKey);
                TShortDoubleMap secondLevelFeatures = firstLevelIter.value();
                for (TShortDoubleIterator secondLevelIter = secondLevelFeatures.iterator(); secondLevelIter.hasNext(); ) {
                    secondLevelIter.advance();
                    if (weightsRow.containsKey(secondLevelIter.key())) {
                        dotProd += secondLevelIter.value() * weightsRow.get(secondLevelIter.key());
                        System.err.println(
                                String.format("\t#Feature hit [%s-%s:%s] = %.2f * %.2f", headWords[wordIndexPair.getLeft()],
                                        headWords[wordIndexPair.getRight()], getFeatureName(secondLevelIter.key()),
                                        weightsRow.get(secondLevelIter.key()), secondLevelIter.value())
                        );
                    }
                }
            }
        }
        return dotProd;
    }

    @Override
    public void adjustBy(TLongShortDoubleHashTable adjustVect, double mul) {
        for (TLongObjectIterator<TShortDoubleMap> firstLevelIter = adjustVect.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            TShortDoubleMap row = getOrNewRow(featureRowKey);

            for (TShortDoubleIterator secondLevelIter = firstLevelIter.value().iterator(); secondLevelIter.hasNext(); ) {
                secondLevelIter.advance();
                row.adjustOrPutValue(secondLevelIter.key(), secondLevelIter.value() * mul, secondLevelIter.value() * mul);
            }
        }
    }


}