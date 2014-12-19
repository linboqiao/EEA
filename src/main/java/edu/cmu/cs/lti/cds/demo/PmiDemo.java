package edu.cmu.cs.lti.cds.demo;

import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.utils.BitUtils;
import edu.cmu.cs.lti.utils.Configuration;

import java.io.File;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 12/19/14
 * Time: 12:56 PM
 */
public class PmiDemo {
    private static final double log2Val = Math.log(2);

    public static void main(String args[]) throws Exception {
        Configuration config = new Configuration(new File(args[0]));
        DataPool.loadHeadStatistics(config, true);

        Scanner in = new Scanner(System.in);

        System.out.println("Please input word to calculate PMI");

        while (in.hasNext()) {
            String words = in.nextLine();
            String[] wordPair = words.split(" ");

            if (wordPair.length >= 2) {
                String word1 = wordPair[0];
                String word2 = wordPair[1];
                System.out.println("PMI is : " + getPmi(word1, word2));
            } else {
                System.out.println("Input invalid");
            }
            System.out.println("Please input word to calculate PMI");
        }

    }

    private static double getPmi(String predicate1, String predicate2) {
        //log_2 P(w1,w2) / P(w1) P(w2)
        // = log_2 C(w1,w2)/N / ( C(w1)  C(w2) / N^2 )
        // = log_2 C(w1,w2) * N / (C(w1) C(w2))

        if (!(DataPool.headIdMap.containsKey(predicate1) && DataPool.headIdMap.containsKey(predicate2))) {
            System.err.println("Warning: predicate not found : " + predicate1 + " " + predicate2);
            return 0;
        }

        int id1 = DataPool.headIdMap.get(predicate1);
        int id2 = DataPool.headIdMap.get(predicate2);

        double jointCount = DataPool.headPairMap.get(BitUtils.store2Int(id1, id2));
        jointCount += DataPool.headPairMap.get(BitUtils.store2Int(id2, id1));

        double count1 = DataPool.getPredicateFreq(id1);
        double count2 = DataPool.getPredicateFreq(id2);

        System.out.println(jointCount + " " + count1 + " " + count2 + " " + DataPool.predicateTotalCount);

        return log2(jointCount * DataPool.predicateTotalCount / (count1 * count2));
    }

    private static double log2(double n) {
        return Math.log(n) / log2Val;
    }
}
