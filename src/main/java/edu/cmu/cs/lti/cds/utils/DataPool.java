package edu.cmu.cs.lti.cds.utils;

import edu.cmu.cs.lti.cds.annotators.script.FastEventMentionHeadCounter;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.TLongBasedFeatureTable;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
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

    //will be update by the trainer
    //learnt parameters
    public static final TObjectDoubleMap<String> weights = new TObjectDoubleHashMap<>();
    public static TObjectDoubleMap<String> adaGradDelGradientSq = new TObjectDoubleHashMap<>();

    //a more compact form in storing such parameters
    public static final TLongBasedFeatureTable trainingUsedCompactWeights = new TLongBasedFeatureTable();

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
    public static Set<String> blackListedArticleId;

    public static TObjectIntMap<TIntList> cooccCountMaps;

    //global event head statistics
    public static TIntLongMap headTfMap;
    public static TLongLongMap headPairMap;

    public static void loadEventHeadTfMap(String dbPath, String dbName, String mentoinHeadTfName) throws Exception {
        String mapPath = new File(dbPath, dbName + "_" + mentoinHeadTfName).getAbsolutePath();
        headTfMap = (TIntLongMap) SerializationHelper.read(mapPath);
        System.err.println("Loaded " + headTfMap.size() + " predicate heads");
    }

    public static void loadEventHeadPairMap(String dbPath, String dbName, String mentionHeadPairName) throws Exception {
        String mapPath = new File(dbPath, dbName + "_" + mentionHeadPairName).getAbsolutePath();
        headPairMap = (TLongLongMap) SerializationHelper.read(mapPath);
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

    public static void loadHeadStatistics(String dbPath, String dbName, String headIdMapName, boolean loadHeadPair) throws Exception {
        //id to head word
        headIdMap = (TObjectIntMap<String>) SerializationHelper.read(new File(dbPath, dbName + "_" + headIdMapName).getAbsolutePath());
        // word to id
        headWords = new String[headIdMap.size()];

        System.err.println(String.format("Number of verb heads Loaded: %d", headIdMap.size()));

        loadEventHeadTfMap(dbPath, FastEventMentionHeadCounter.defaultDBName, FastEventMentionHeadCounter.defaultMentionHeadCountMapName);
        for (TObjectIntIterator<String> iter = headIdMap.iterator(); iter.hasNext(); ) {
            iter.advance();
            predicateTotalCount += getPredicateFreq(iter.value());
            headWords[iter.value()] = iter.key();
        }

        System.err.println(String.format("Total predicate counts: %d", predicateTotalCount));

        if (loadHeadPair) {
            loadEventHeadPairMap(dbPath, FastEventMentionHeadCounter.defaultDBName, FastEventMentionHeadCounter.defaultMentionPairCountName);
            System.err.println(String.format("Number of event pairs loaded: %d", headPairMap.size()));
        }
    }

    public static void loadHeadIds(String dbPath, String dbName, String headIdMapName) throws Exception {
        headIdMap = (TObjectIntMap<String>) SerializationHelper.read(new File(dbPath, dbName + "_" + headIdMapName).getAbsolutePath());
    }

    public static void loadEventUnigramCounts(String dbPath, String dbName, String unigramMapName) throws Exception {
        String mapPath = new File(dbPath, dbName + "_" + unigramMapName).getAbsolutePath();
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

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration(new File(args[0]));

        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
    }
}
