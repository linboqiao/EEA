/**
 *
 */
package edu.cmu.cs.lti.utils;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author zhengzhongliu
 */
public class StringUtils {
    public static Joiner spaceJoiner = Joiner.on(" ");

    public static String text2CsvField(String text) {
        return text.replace(",", ".").replace(":", "_").replace("\n", " ");
    }

    public static String removeEnd(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (str.endsWith(remove)) {
            return str.substring(0, str.length() - remove.length());
        }
        return str;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static List<String> characterSkipBigram(String str) {
        List<String> skipBigrams = new ArrayList<String>();

        if (str.length() < 2) {
            return skipBigrams;
        }

        for (int i = 0; i < str.length() - 2; i++) {
            char[] nonskip = new char[2];
            char[] skip = new char[2];

            nonskip[0] = str.charAt(i);
            nonskip[1] = str.charAt(i + 1);

            skip[0] = str.charAt(i);
            skip[1] = str.charAt(i + 2);

            skipBigrams.add(new String(nonskip));
            skipBigrams.add(new String(skip));
        }

        char[] lastone = new char[2];
        lastone[0] = str.charAt(str.length() - 2);
        lastone[1] = str.charAt(str.length() - 1);

        skipBigrams.add(new String(lastone));

        return skipBigrams;
    }

    public static double strDice(Map<String, Double> featureMap1, Map<String, Double> featureMap2) {
        int length1 = 0;
        int length2 = 0;

        double intersect = 0;

        for (Entry<String, Double> feature1 : featureMap1.entrySet()) {
            String featureName = feature1.getKey();
            double val1 = feature1.getValue();
            length1 += val1;
            if (featureMap2.containsKey(featureName)) {
                double val2 = featureMap2.get(featureName);
                intersect += val1 > val2 ? val2 : val1;
            }
        }

        for (Entry<String, Double> feature2 : featureMap2.entrySet()) {
            double val2 = feature2.getValue();
            length2 += val2;
        }

        if (length1 == 0 || length2 == 0) {
            return 0;
        }

        // System.out.println(intersect + " " + length1 + " " + length2);

        return 2 * intersect / (length1 + length2);
    }

    public static void main(String[] args) {
        Map<String, Double> m1 = new HashMap<String, Double>();
        for (String sb : characterSkipBigram("Petersens")) {
            System.out.println(sb);
            if (m1.containsKey(sb)) {
                m1.put(sb, m1.get(sb));
            } else {
                m1.put(sb, 1.0);
            }
        }

        Map<String, Double> m2 = new HashMap<String, Double>();
        for (String sb : characterSkipBigram("Petersen")) {
            System.out.println(sb);
            if (m2.containsKey(sb)) {
                m2.put(sb, m2.get(sb));
            } else {
                m2.put(sb, 1.0);
            }
        }

        System.out.println("Dice " + strDice(m1, m2));
    }
}
