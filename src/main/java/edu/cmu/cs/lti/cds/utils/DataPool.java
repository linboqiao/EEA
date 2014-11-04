package edu.cmu.cs.lti.cds.utils;

import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import weka.core.SerializationHelper;

import java.io.File;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/4/14
 * Time: 11:05 AM
 */
public class DataPool {

    //will be update by the trainer
    public static final TObjectDoubleMap<String> weights = new TObjectDoubleHashMap<String>();
    public static final TObjectIntMap<String> headIdMap = new TObjectIntHashMap<String>();
    public static Map[] headTfDfMaps;
    public static long sum = 0;

    //Load some of these large maps that will might be shared static
    //use only one unified head id map here.
    public static void loadHeadIds(String dbPath, String dbNames, String headIdMapName) throws Exception {
        String mapPath = new File(dbPath, dbNames + "_" + headIdMapName).getAbsolutePath();
        TObjectIntMap<String> map = (TObjectIntMap<String>) SerializationHelper.read(mapPath);
    }

    public static void loadHeadCounts(String dbPath, String[] countingDbFileNames, boolean useProb) {
        headTfDfMaps = DbManager.getMaps(dbPath, countingDbFileNames, EventMentionHeadCounter.defaultMentionHeadMapName);
        if (useProb) {
            calUnigramSum();
        }
    }

    public static void calUnigramSum() {
        for (TObjectIntIterator<String> iter = headIdMap.iterator(); iter.hasNext(); iter.advance()) {
            sum += getPredicateFreq(iter.key());
        }
    }

    public static int getPredicateFreq(String predicate) {
        return MultiMapUtils.getTf(headTfDfMaps, predicate);
    }

    public static double getPredicateProb(String predicate) {
        return MultiMapUtils.getTf(headTfDfMaps, predicate) * 1.0 / sum;
    }
}
