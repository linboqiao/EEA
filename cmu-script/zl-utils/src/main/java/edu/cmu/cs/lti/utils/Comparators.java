package edu.cmu.cs.lti.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/8/14
 * Time: 2:25 PM
 */
public class Comparators {
    public static class DescendingScoredPairComparator<T extends Object, S extends Comparable<S>> implements Comparator<Pair<T, S>> {
        @Override
        public int compare(Pair<T, S> o1, Pair<T, S> o2) {
            return -o1.getValue().compareTo(o2.getValue());
        }
    }
}
