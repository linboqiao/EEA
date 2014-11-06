package edu.cmu.cs.lti.cds.utils;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/4/14
 * Time: 10:03 PM
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

    public static void vectorScalarProduct(TObjectDoubleMap<String> vector, double scalar) {
        for (TObjectDoubleIterator<String> it = vector.iterator(); it.hasNext(); ) {
            it.advance();
            it.setValue(it.value() * scalar);
        }
    }

    public static void vectorMinus(TObjectDoubleMap<String> v1, TObjectDoubleMap<String> v2) {
        for (TObjectDoubleIterator<String> it = v1.iterator(); it.hasNext(); ) {
            it.advance();
            if (v2.containsKey(it.key())) {
                it.setValue(it.value() - v2.get(it.key()));
                v2.remove(it.key());
            }
        }

        for (TObjectDoubleIterator<String> it = v2.iterator(); it.hasNext(); ) {
            it.advance();
            v1.put(it.key(), -it.value());
        }
    }
}
