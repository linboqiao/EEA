package edu.cmu.cs.lti.script.utils;

import edu.cmu.cs.lti.script.annotators.learn.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.script.annotators.learn.train.UnigramScriptCounter;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TObjectIntMap;
import org.apache.commons.io.FileUtils;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/4/14
 * Time: 11:05 AM
 */
public class DataPool {
//    //a more compact form in storing such parameters
//    public static final TLongBasedFeatureTable trainingUsedCompactWeights = new TLongBasedFeatureTable();
//
//    //ada grad memory
//    public static final TLongShortDoubleHashTable compactAdaGradMemory = new TLongShortDoubleHashTable();
//
//    //sample counter
//    public static long numSampleProcessed = 0;

    //data used by the trainer
    public static long predicateTotalCount = 0;
    public static long eventUnigramTotalCount = 0;
    public static String[] headWords;
    public static TObjectIntMap<String> headIdMap;
    public static TObjectIntMap<TIntList> unigramCounts;
    public static Set<String> blackListedArticleId;

    public static TObjectIntMap<TIntList> cooccCountMaps;

    //global event head statistics
    public static TIntLongMap headTfMap;
    public static TLongLongMap headPairMap;

    public static void loadEventHeadTfMap(String dbPath, String headCountMapName) throws Exception {
        headTfMap = (TIntLongMap) SerializationHelper.read(new File(dbPath, headCountMapName).getAbsolutePath());
        System.err.println("Loaded " + headTfMap.size() + " predicate heads");
    }

    public static void loadEventHeadPairMap(String dbPath, String headPairMapName) throws Exception {
        System.err.println("Loading head word pair count from " + headPairMapName);
        headPairMap = (TLongLongMap) SerializationHelper.read(new File(dbPath, headPairMapName).getAbsolutePath());
        System.err.println("Loaded " + headPairMap.size() + " predicate pairs");
    }

    //Load some of these large maps that might be shared static
    public static void loadKmCooccMap(String dbPath, String dbName, String cooccName) {
        String mapPath = new File(dbPath, dbName + "_" + cooccName).getAbsolutePath();
        try {
            cooccCountMaps = (TObjectIntMap<TIntList>) SerializationHelper.read(mapPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadHeadStatistics(Configuration config, boolean loadPairCount) throws Exception {
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath");
        String dbName = config.getList("edu.cmu.cs.lti.cds.db.basenames")[0];
        String headIdMapName = KarlMooneyScriptCounter.defaltHeadIdMapName;
        String headCountMapName = config.get("edu.cmu.cs.lti.cds.db.predicate.count");

        //id to head word
        loadHeadIds(dbPath, dbName, headIdMapName);
        // word to id
        headWords = new String[headIdMap.size()];

        loadEventHeadTfMap(dbPath, headCountMapName);
        for (TObjectIntIterator<String> iter = headIdMap.iterator(); iter.hasNext(); ) {
            iter.advance();
            predicateTotalCount += getPredicateFreq(iter.value());
            headWords[iter.value()] = iter.key();
        }

        System.err.println(String.format("Total predicate counts: %d", predicateTotalCount));

        if (loadPairCount) {
            String headPairCountMapName = config.get("edu.cmu.cs.lti.cds.db.predicte.pair.count");
            loadEventHeadPairMap(dbPath, headPairCountMapName);
        }
    }

    public static void loadHeadIds(String dbPath, String dbName, String headIdMapName) throws Exception {
        headIdMap = (TObjectIntMap<String>) SerializationHelper.read(new File(dbPath, dbName + "_" + headIdMapName).getAbsolutePath());
        System.err.println(String.format("Number of verb heads Loaded: %d", headIdMap.size()));
    }

    public static void loadEventUnigramCounts(Configuration config) throws Exception {
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        String unigramMapName = UnigramScriptCounter.defaultUnigramMapName;

        String mapPath = new File(dbPath, dbNames[0] + "_" + unigramMapName).getAbsolutePath();
        unigramCounts = (TObjectIntMap<TIntList>) SerializationHelper.read(mapPath);
        for (TObjectIntIterator<TIntList> iter = unigramCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            eventUnigramTotalCount += iter.value();
        }
    }

    public static long getPredicateFreq(int predicateId) {
        return headTfMap.get(predicateId);
    }

    public static long getPredicateFreq(String headWord) {
        return getPredicateFreq(headIdMap.get(headWord));
    }

    public static double getPredicateProb(String headWord) {
        return getPredicateFreq(headIdMap.get(headWord));
    }

    public static double getPredicateProb(int predicateId) {
        return getPredicateFreq(predicateId) * 1.0 / predicateTotalCount;
    }

    public static void readBlackList(File blackListFile) throws IOException {
        blackListedArticleId = new HashSet<>();
        for (String line : FileUtils.readLines(blackListFile)) {
            blackListedArticleId.add(line.trim());
        }
    }

    public static boolean isBlackList(JCas aJCas, Logger logger) {
        Article article = JCasUtil.selectSingle(aJCas, Article.class);
        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            //ignore this blacklisted file;
            logger.info("Ignored black listed file");
            return true;
        }
        return false;
    }

    public static boolean lowFreqFilter(int predicateId) {
        return getPredicateFreq(predicateId) < 50;
    }

    public static boolean lowFreqFilter(String predicate) {
        return lowFreqFilter(headIdMap.get(predicate));
    }
}
