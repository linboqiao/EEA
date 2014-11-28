package edu.cmu.cs.lti.cds.utils;

import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import edu.cmu.cs.lti.utils.TLongBasedFeatureTable;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.io.FileUtils;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/4/14
 * Time: 11:05 AM
 */
public class DataPool {

    //will be update by the trainer
    //learnt parameters
    public static final TObjectDoubleMap<String> weights = new TObjectDoubleHashMap<>();
    public static TObjectDoubleMap<String> adaGradDelGradientSq = new TObjectDoubleHashMap<>();

    //a more compact form in storing such parameters
    public static final TLongBasedFeatureTable compactWeights = new TLongBasedFeatureTable();

    //ada grad memory
    public static final TLongShortDoubleHashTable compactAdaGradMemory = new TLongShortDoubleHashTable();

    //ada delta memory
    public static TObjectDoubleMap<String> deltaVarSq = new TObjectDoubleHashMap<>();
    public static TObjectDoubleMap<String> deltaGradientSq = new TObjectDoubleHashMap<>();

    //sample counter
    public static long numSampleProcessed = 0;

    //data used by the trainer
    public static long predicateTotalCount = 0;
    public static long eventUnigramTotalCount = 0;
    public static String[] headWords;
    public static TObjectIntMap<String> headIdMap;
    public static TObjectIntMap<TIntList> unigramCounts;
    public static Map[] headTfDfMaps;
    public static Set<String> blackListedArticleId;

    public static TObjectIntMap<TIntList> cooccCountMaps;


    //Load some of these large maps that might be shared static

    public static void loadCooccMap(String dbPath, String dbName, String cooccName) throws Exception {
        String mapPath = new File(dbPath, dbName + "_" + cooccName).getAbsolutePath();
        cooccCountMaps = (TObjectIntMap<TIntList>) SerializationHelper.read(mapPath);
    }

    public static void loadData(String dbPath, String dbName, String headIdMapName, String[] countingDbFileNames) throws Exception {
        loadHeadIds(dbPath, dbName, headIdMapName);
        loadHeadCounts(dbPath, countingDbFileNames);
    }

    public static void loadHeadIds(String dbPath, String dbName, String headIdMapName) throws Exception {
        String mapPath = new File(dbPath, dbName + "_" + headIdMapName).getAbsolutePath();
        headIdMap = (TObjectIntMap<String>) SerializationHelper.read(mapPath);
        headWords = new String[headIdMap.size()];
    }

    public static void loadHeadCounts(String dbPath, String[] countingDbFileNames) {
        headTfDfMaps = DbManager.getMaps(dbPath, countingDbFileNames, EventMentionHeadCounter.defaultMentionHeadMapName);
        for (TObjectIntIterator<String> iter = headIdMap.iterator(); iter.hasNext(); ) {
            iter.advance();
            predicateTotalCount += getPredicateFreq(iter.key());
            headWords[iter.value()] = iter.key();
        }
    }

    public static void loadEventUnigramCounts(String dbPath, String dbName, String unigramMapName) throws Exception {
        String mapPath = new File(dbPath, dbName + "_" + unigramMapName).getAbsolutePath();
        unigramCounts = (TObjectIntMap<TIntList>) SerializationHelper.read(mapPath);
        for (TObjectIntIterator<TIntList> iter = unigramCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            eventUnigramTotalCount += iter.value();
        }
    }

    public static int getPredicateFreq(String predicate) {
        return MultiMapUtils.getTf(headTfDfMaps, predicate);
    }

    public static double getPredicateProb(String predicate) {
        return MultiMapUtils.getTf(headTfDfMaps, predicate) * 1.0 / predicateTotalCount;
    }

    public static void readBlackList(File blackListFile) throws IOException {
        blackListedArticleId = new HashSet<>();
        for (String line : FileUtils.readLines(blackListFile)) {
            blackListedArticleId.add(line.trim());
        }
    }
}
