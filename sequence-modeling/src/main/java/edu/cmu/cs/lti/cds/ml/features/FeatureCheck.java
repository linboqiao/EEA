package edu.cmu.cs.lti.cds.ml.features;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.utils.ArrayBasedTwoLevelFeatureTable;
import edu.cmu.cs.lti.utils.BitUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TShortDoubleIterator;
import gnu.trove.map.TShortDoubleMap;
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

            TShortDoubleMap row = compactWeights.getRow(rowKey);

            BiMap<Short, String> secondKeyMap = compactWeights.getFeatureNameMap();


            StringBuilder sb = new StringBuilder();

            for (TShortDoubleIterator secondIter = row.iterator(); secondIter.hasNext(); ) {
                secondIter.advance();
                short secondKey = secondIter.key();
                String secondKeyName = secondKeyMap.get(secondKey);
                sb.append("\t").append(secondKey).append(". ").append(secondKeyName).append(":").append(secondIter.value()).append("\n");
            }

            System.out.println(sb.toString());
        }

    }
}
