package edu.cmu.cs.lti.utils;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.iterator.TShortDoubleIterator;
import gnu.trove.map.TShortDoubleMap;
import org.apache.commons.lang3.tuple.Pair;

import java.io.PrintWriter;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/11/14
 * Time: 3:49 PM
 */
public class TLongBasedFeatureHashTable extends TwoLevelFeatureTable {
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
    TLongShortDoubleHashTable table = new TLongShortDoubleHashTable();


    public TLongBasedFeatureHashTable() {

    }

    public TLongBasedFeatureHashTable(TwoLevelFeatureTable table) {
        super(table);
    }

    public TLongShortDoubleHashTable getUnderlyingTable() {
        return table;
    }

    public int getNumRows() {
        return table.getNumRows();
    }

    public TLongObjectIterator<TShortDoubleMap> iterator() {
        return table.iterator();
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

    public TShortDoubleMap getRow(long rowKey) {
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

    public double dotProd(TLongShortDoubleHashTable features, String[] headWords) {
        double dotProd = 0;
        for (TLongObjectIterator<TShortDoubleMap> firstLevelIter = features.iterator(); firstLevelIter.hasNext(); ) {
            firstLevelIter.advance();
            long featureRowKey = firstLevelIter.key();
            if (table.containsRow(featureRowKey)) {
                Pair<Integer, Integer> wordIndexPair = BitUtils.get2IntFromLong(featureRowKey);

                TShortDoubleMap weightsRow = getRow(featureRowKey);
                TShortDoubleMap secondLevelFeatures = firstLevelIter.value();
                for (TShortDoubleIterator secondLevelIter = secondLevelFeatures.iterator(); secondLevelIter.hasNext(); ) {
                    secondLevelIter.advance();
                    if (weightsRow.containsKey(secondLevelIter.key())) {
                        dotProd += secondLevelIter.value() * weightsRow.get(secondLevelIter.key());
                        System.err.println("\t#Feature hit " + headWords[wordIndexPair.getLeft()] + " " +
                                headWords[wordIndexPair.getRight()] + " " + secondaryFeatureLookupMap.inverse().get(secondLevelIter.key()) + " : " +
                                weightsRow.get(secondLevelIter.key()));
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
        for (TLongObjectIterator<TShortDoubleMap> rowIter = table.iterator(); rowIter.hasNext(); ) {
            rowIter.advance();
            numLexicalPairs++;
            Pair<Integer, Integer> wordIds = BitUtils.get2IntFromLong(rowIter.key());
            writer.write(headWords[wordIds.getKey()] + " " + headWords[wordIds.getValue()] + " " + wordIds.getKey() + " " + wordIds.getValue() + "\n");

            for (TShortDoubleIterator cellIter = rowIter.value().iterator(); cellIter.hasNext(); ) {
                cellIter.advance();
                writer.write(featureNames.get(cellIter.key()) + " " + cellIter.key() + " " + cellIter.value() + "\n");
                numFeatures++;
            }
        }
        return Pair.of(numFeatures, numLexicalPairs);
    }

}
