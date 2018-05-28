package edu.cmu.cs.lti.utils;

import net.ricecode.similarity.DiceCoefficientStrategy;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityServiceImpl;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/29/15
 * Time: 11:44 PM
 *
 * @author Zhengzhong Liu
 */
public class SimilarityUtils {
    private static SimilarityStrategy strategy = new DiceCoefficientStrategy();
    private static StringSimilarityServiceImpl service = new StringSimilarityServiceImpl(strategy);

    public static Double relaxedDiceTest(String str1, String str2) {
        str1 = str1.toLowerCase();
        str2 = str2.toLowerCase();

        return Math.max(getSuffixDiceCoefficient(str1, str2),
                Math.max(getDiceCoefficient(str1, str2), getPrefixDiceCoefficient(str1, str2)));
    }

    public static Double getDiceCoefficient(String text1, String text2) {
        return service.score(text1.toLowerCase(), text2.toLowerCase());
    }

    public static Double getPrefixDiceCoefficient(String text1, String text2) {
        int n = Math.min(text1.length(), text2.length());
        return service.score(text1.substring(0, n), text2.substring(0, n));
    }

    public static Double getSuffixDiceCoefficient(String text1, String text2) {
        int len1 = text1.length();
        int len2 = text2.length();
        int n = Math.min(len1, len2);
        return service.score(text1.substring(len1 - n, len1), text2.substring(len2 - n, len2));
    }


}
