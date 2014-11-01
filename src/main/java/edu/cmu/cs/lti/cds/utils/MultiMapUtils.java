package edu.cmu.cs.lti.cds.utils;

import org.mapdb.Fun;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/31/14
 * Time: 11:16 PM
 */
public class MultiMapUtils {
    public static int getTf(Map<String, Fun.Tuple2<Integer, Integer>>[] maps, String key) {
        int tfSum = 0;
        for (Map<String, Fun.Tuple2<Integer, Integer>> map : maps) {
            if (map.containsKey(key)) {
                tfSum += map.get(key).a;
            }
        }
        return tfSum;
    }
}
