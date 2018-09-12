package edu.cmu.cs.lti.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/8/14
 * Time: 2:24 PM
 */
public class MathUtils {
    public static List<int[]> getCombination(int chooseN, int fromM) {
        System.out.println("choose " + chooseN + " from " + fromM);
        byte[] bits = new byte[fromM];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = i < chooseN ? (byte) 1 : (byte) 0;
        }


        List<int[]> combs = new ArrayList<>();

        boolean found = true;
        while (found) {
            int index = 0;
            int[] comb = new int[chooseN];
            for (int j = 0; j < bits.length; j++) {
                if (bits[j] == (byte) 1) {
                    comb[index] = j;
                    index++;
                }
            }
            combs.add(comb);

            found = false;
            for (int i = 0; i < fromM - 1; i++) {
                if (bits[i] == 1 && bits[i + 1] == 0) {
                    found = true;
                    bits[i] = 0;
                    bits[i + 1] = 1;

                    if (bits[0] == 0) {
                        for (int k = 0, j = 0; k < i; k++) {
                            if (bits[k] == 1) {
                                byte temp = bits[k];
                                bits[k] = bits[j];
                                bits[j] = temp;
                                j++;
                            }
                        }
                    }
                    break;
                }
            }
        }

        return combs;
    }
}
