package edu.cmu.cs.lti.cds.debug;

import edu.cmu.cs.lti.cds.annotators.stats.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.utils.DbManager;
import org.mapdb.Fun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/22/14
 * Time: 4:48 PM
 */
public class CheckHeadCount {
    public static void main(String[] args) {
        Map<String, Fun.Tuple2<Integer, Integer>>[] headTfDfMaps;

        String[] mapNames = new String[1];
        mapNames[0] = "headcounts";

        headTfDfMaps = DbManager.getMaps("data/_db", mapNames, EventMentionHeadCounter.defaultMentionHeadMapName);

        List<String> headwords = new ArrayList<>();

        for (Map<String, Fun.Tuple2<Integer, Integer>> map : headTfDfMaps) {
            for (Map.Entry<String, Fun.Tuple2<Integer, Integer>> entry : map.entrySet()) {
                headwords.add(entry.getKey());
            }
            System.out.println(map.size());
        }

        Collections.sort(headwords);

        System.out.println("Number of words " + headwords.size());
    }
}
