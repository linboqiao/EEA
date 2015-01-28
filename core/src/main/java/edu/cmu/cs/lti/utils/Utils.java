package edu.cmu.cs.lti.utils;

import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/14/14
 * Time: 9:03 PM
 */
public class Utils {
    public static boolean tfDfFilter(int tf, int df) {
        return documentFrequencyFilter(df);
    }

    public static boolean termFrequencyFilter(int tf) {
        return tf / 50 == 0;
    }

    public static boolean termFrequencyFilter(long tf) {
        return tf / 50 == 0;
    }

    public static boolean documentFrequencyFilter(int df) {
        return df / 10 == 0;
    }

    public static void pause() {
        Scanner in = new Scanner(System.in);
        System.out.println("Enter to continue");
        in.nextLine();
    }
}
