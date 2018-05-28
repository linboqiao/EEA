package edu.cmu.cs.lti.feature;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/3/15
 * Time: 3:05 PM
 *
 * @author Zhengzhong Liu
 */
public class VectorUtils {
    public static double dotProd(TObjectDoubleMap<String> smallVec, TObjectDoubleMap<String> largerVec) {
        double dotProd = 0;
        for (TObjectDoubleIterator<String> it = smallVec.iterator(); it.hasNext(); ) {
            it.advance();
            if (largerVec.containsKey(it.key())) {
                dotProd += it.value() * largerVec.get(it.key());
            }
        }
        return dotProd;
    }

    public static void scalarProductInPlace(TObjectDoubleMap<String> vector, double scalar) {
        for (TObjectDoubleIterator<String> it = vector.iterator(); it.hasNext(); ) {
            it.advance();
            it.setValue(it.value() * scalar);
        }
    }


    public static void minusInplace(TObjectDoubleMap<String> v1, TObjectDoubleMap<String> v2) {
        weightedSumInPlace(v1, v2, 1, -1);
    }

    public static void sumInPlace(TObjectDoubleMap<String> v1, TObjectDoubleMap<String> v2) {
        weightedSumInPlace(v1, v2, 1, 1);
    }

    public static void weightedSumInPlace(TObjectDoubleMap<String> v1, TObjectDoubleMap<String> v2,
                                          double v1Weight, double v2Weight) {
        for (TObjectDoubleIterator<String> it = v1.iterator(); it.hasNext(); ) {
            it.advance();
            if (v2.containsKey(it.key())) {
                double value = it.value() * v1Weight + v2.get(it.key()) * v2Weight;
                if (value == 0) {
                    it.remove();
                } else {
                    it.setValue(value);
                }
                v2.remove(it.key());
            }
        }

        for (TObjectDoubleIterator<String> it = v2.iterator(); it.hasNext(); ) {
            it.advance();
            v1.put(it.key(), it.value() * v2Weight);
        }
    }

    public static TObjectDoubleMap<String> scalarProduct(TObjectDoubleMap<String> vector, double scalar) {
        TObjectDoubleMap<String> result = new TObjectDoubleHashMap<>();
        for (TObjectDoubleIterator<String> it = vector.iterator(); it.hasNext(); ) {
            it.advance();
            result.put(it.key(), it.value() * scalar);
        }
        return result;
    }

    public static TObjectDoubleMap<String> minus(TObjectDoubleMap<String> v1, TObjectDoubleMap<String> v2) {
        return weightedSum(v1, v2, 1, -1);
    }

    public static TObjectDoubleMap<String> sum(TObjectDoubleMap<String> v1, TObjectDoubleMap<String> v2) {
        return weightedSum(v1, v2, 1, 1);
    }

    public static TObjectDoubleMap<String> weightedSum
            (TObjectDoubleMap<String> v1, final TObjectDoubleMap<String> v2,
             final double v1Weight, final double v2Weight) {
        final TObjectDoubleMap<String> result = new TObjectDoubleHashMap<>();
        Set<String> usedKey = new HashSet<>();

        v1.forEachEntry((a, b) -> {
            if (v2.containsKey(a)) {
                double value = b * v1Weight + v2.get(a) * v2Weight;
                if (value != 0) {
                    result.put(a, value);
                }
                usedKey.add(a);
            }
            return true;
        });

        v2.forEachEntry((a, b) -> {
            if (!usedKey.contains(a)) {
                result.put(a, b * v2Weight);
            }
            return true;
        });
        return result;
    }

    public static double vectorL2Sq(TObjectDoubleMap<String> v) {
        return dotProd(v, v);
    }

    public static double vectorL2(TObjectDoubleMap<String> v) {
        return Math.sqrt(vectorL2Sq(v));
    }

}