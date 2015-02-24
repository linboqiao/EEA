package edu.cmu.cs.lti.utils;

import edu.cmu.cs.lti.collections.TLongIntDoubleHashTable;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/20/15
 * Time: 2:39 PM
 */
public class ArrayBasedTwoLevelFeatureTable extends TwoLevelFeatureTable {
    private static final long serialVersionUID = 5335403133116054451L;

    TIntDoubleMap[][] table;

    int numRows = 0;

    int numFeatures = 0;

    public ArrayBasedTwoLevelFeatureTable(int vocabularySize) {
        table = new TIntDoubleMap[vocabularySize][vocabularySize];
    }


    @Override
    public int getNumRows() {
        return numRows;
    }

    public TIntDoubleMap getRow(long rowKey) {
        Pair<Integer, Integer> tableKeys = BitUtils.get2IntFromLong(rowKey);
        return table[tableKeys.getLeft()][tableKeys.getRight()];
    }


    private void newRow(int row, int col) {
        table[row][col] = new TIntDoubleHashMap();
        numRows++;
    }

    private TIntDoubleMap getOrNewRow(long rowKey) {
        Pair<Integer, Integer> tableKeys = BitUtils.get2IntFromLong(rowKey);
        if (table[tableKeys.getLeft()][tableKeys.getRight()] == null) {
            newRow(tableKeys.getLeft(), tableKeys.getRight());
        }
        return table[tableKeys.getLeft()][tableKeys.getRight()];
    }

    @Override
    public Double get(long rowKey, int colKey) {
        return getRow(rowKey).get(colKey);
    }


    @Override
    public void put(long rowKey, int colKey, double value) {
        TIntDoubleMap row = getOrNewRow(rowKey);
        row.put(colKey, value);
    }

    @Override
    public boolean contains(long rowKey, int colKey) {
        TIntDoubleMap row = getRow(rowKey);
        return row != null && row.containsKey(colKey);
    }

    @Override
    public boolean containsRow(long rowKey) {
        return getRow(rowKey) != null;
    }

    @Override
    public boolean adjust(long rowKey, int colKey, double value) {
        TIntDoubleMap row = getRow(rowKey);
        if (row != null) {
            return row.adjustValue(colKey, value);
        } else {
            return false;
        }
    }

    @Override
    public double adjustOrPutValue(long rowKey, int colKey, double adjustAmount, double putAmount) {
        TIntDoubleMap row = getOrNewRow(rowKey);
        return row.adjustOrPutValue(colKey, adjustAmount, putAmount);
    }

    @Override
    public double dotProd(TLongIntDoubleHashTable features) {
        double dotProd = 0;
        for (TLongObjectIterator<TIntDoubleMap> firstLevelIter = features.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            if (containsRow(featureRowKey)) {
                TIntDoubleMap weightsRow = getRow(featureRowKey);
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

    public double dotProd(TLongIntDoubleHashTable features, String[] headWords) {
        double dotProd = 0;
        for (TLongObjectIterator<TIntDoubleMap> firstLevelIter = features.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            if (containsRow(featureRowKey)) {
                Pair<Integer, Integer> wordIndexPair = BitUtils.get2IntFromLong(featureRowKey);

                TIntDoubleMap weightsRow = getRow(featureRowKey);
                TIntDoubleMap secondLevelFeatures = firstLevelIter.value();
                for (TIntDoubleIterator secondLevelIter = secondLevelFeatures.iterator(); secondLevelIter.hasNext(); ) {
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
    public void adjustBy(TLongIntDoubleHashTable adjustVect, double mul) {
        for (TLongObjectIterator<TIntDoubleMap> firstLevelIter = adjustVect.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            TIntDoubleMap row = getOrNewRow(featureRowKey);

            for (TIntDoubleIterator secondLevelIter = firstLevelIter.value().iterator(); secondLevelIter.hasNext(); ) {
                secondLevelIter.advance();
                row.adjustOrPutValue(secondLevelIter.key(), secondLevelIter.value() * mul, secondLevelIter.value() * mul);
            }
        }
    }


}