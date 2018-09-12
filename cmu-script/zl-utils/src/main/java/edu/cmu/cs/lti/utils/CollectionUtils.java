package edu.cmu.cs.lti.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/21/14
 * Time: 1:16 PM
 */
public class CollectionUtils {
    public static <T> List<Pair<T, T>> nSkippedBigrams(Collection<T> collection, int n) {
        List<Pair<T, T>> resultBigrams = new ArrayList<>();

        List<Iterator<T>> followerIters = new LinkedList<>();

        for (T second : collection) {
            for (Iterator<T> followerIter : followerIters) {
                resultBigrams.add(Pair.of(followerIter.next(), second));
            }

            if (n >= 0) {
                //add at the begining so that the earlist bigram is output first
                followerIters.add(0, collection.iterator());
                n--;
            }
        }

        return resultBigrams;
    }
}
