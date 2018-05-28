package edu.cmu.cs.lti.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/11/14
 * Time: 1:52 PM
 */
public class BitUtils {
    public static long store2Int(int x, int y) {
        long l = x;
        l = (l << 32) | (y & 0xffffffffL);
        return l;
    }

    public static Pair<Integer, Integer> get2IntFromLong(long l) {
        return Pair.of((int) (l >> 32), (int) l);
    }

    public static void main(String[] args) {
        //testing
        Random rand = new Random();

        int x = rand.nextInt();
        int y = rand.nextInt();

        x = 46;
        y = 5;

        System.out.println(x + " " + y);

        long l = store2Int(x, y);

        System.out.println(store2Int(x, y));
        System.out.println(get2IntFromLong(l));
    }
}
