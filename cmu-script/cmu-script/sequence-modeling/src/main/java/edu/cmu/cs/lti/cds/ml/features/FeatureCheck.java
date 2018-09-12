package edu.cmu.cs.lti.cds.ml.features;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.utils.ArrayBasedTwoLevelFeatureTable;
import edu.cmu.cs.lti.utils.BitUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import weka.core.SerializationHelper;

import java.io.File;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/17/15
 * Time: 6:30 PM
 */
public class FeatureCheck {
    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration(new File("settings.properties"));
        DataPool.loadHeadStatistics(config, false);
        String modelPath = config.get("edu.cmu.cs.lti.cds.loglinear.model");
        ArrayBasedTwoLevelFeatureTable compactWeights = (ArrayBasedTwoLevelFeatureTable) SerializationHelper.read(modelPath);

        Scanner scan = new Scanner(System.in);

        System.out.println("Number of lexical pairs " + compactWeights.getNumRows());

        while (true) {
            System.out.print("Word 1 : ");
            String word1 = scan.nextLine();

            int word1Id = DataPool.headIdMap.get(word1);

            System.out.println(word1Id);

            System.out.print("Word 2 : ");
            String word2 = scan.nextLine();

            int word2Id = DataPool.headIdMap.get(word2);

            System.out.println(word2Id);

            long rowKey = BitUtils.store2Int(word1Id, word2Id);


            BiMap<Integer, String> secondKeyMap = compactWeights.getFeatureNameMap();

            StringBuilder sb = new StringBuilder();

            if (compactWeights.containsRow(rowKey)) {
                TIntDoubleMap row = compactWeights.getRow(rowKey);
                for (TIntDoubleIterator secondIter = row.iterator(); secondIter.hasNext(); ) {
                    secondIter.advance();
                    int secondKey = secondIter.key();
                    String secondKeyName = secondKeyMap.get(secondKey);
                    sb.append("\t").append(secondKey).append(". ").append(secondKeyName).append(":").append(secondIter.value()).append("\n");
                }
            }else{
                System.out.println("Do not have this lexical pair");
            }

            System.out.println(sb.toString());
        }

    }
}
