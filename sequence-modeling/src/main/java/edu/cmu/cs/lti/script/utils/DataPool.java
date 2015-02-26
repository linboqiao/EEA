package edu.cmu.cs.lti.script.utils;

import edu.cmu.cs.lti.ling.FrameDataReader;
import edu.cmu.cs.lti.script.annotators.learn.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.script.annotators.learn.train.UnigramScriptCounter;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TObjectIntMap;
import org.apache.commons.io.FileUtils;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.javatuples.Pair;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/4/14
 * Time: 11:05 AM
 */
public class DataPool {
    //data used by the trainer
    public static long predicateTotalCount = 0;
    public static String[] headWords;
    public static TObjectIntMap<String> headIdMap;
    public static TObjectIntMap<TIntList> unigramCounts;
    public static Set<String> blackListedArticleId;

    public static TObjectIntMap<TIntList> cooccCountMaps;

    public static Map<String, String> pb2FnFrameMapping;
    public static Map<String, String> pb2FnFrameRoleMapping;

    //global event head statistics
    public static TIntIntMap headTfMap;
    public static TLongLongMap headPairMap;

    public static void loadEventHeadTfMap(String dbPath, String headCountMapName) throws Exception {
        headTfMap = (TIntIntMap) SerializationHelper.read(new File(dbPath, headCountMapName).getAbsolutePath());
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
        System.err.println("Loading coocc map at : " + mapPath);

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
            System.err.println("Loading pair counts, may take a while.");
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

        System.err.println("Loading unigram counts " + mapPath);
        unigramCounts = (TObjectIntMap<TIntList>) SerializationHelper.read(mapPath);
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

    public static void loadSemLinkData(String semLinkDirPath) {
        System.err.println("Loading SemLink data");
        Map<String, String> vn2Fn = FrameDataReader.getFN2VNFrameMap(semLinkDirPath + "/vn-fn/VN-FNRoleMapping.txt", true);
        Map<String, String> pb2Vn = FrameDataReader.getVN2PBFrameMap(semLinkDirPath + "/vn-pb/vnpbMappings", true);

        Map<Pair<String, String>, Pair<String, String>> vn2FnRole = FrameDataReader.getFN2VNRoleMap(semLinkDirPath + "/vn-fn/VN-FNRoleMapping.txt", true);
        //TODO: this one is not read successfully
        Map<Pair<String, String>, Pair<String, String>> pb2VnRole = FrameDataReader.getVN2PBRoleMap(semLinkDirPath + "/vn-pb/vnpbMappings", true);

        pb2FnFrameMapping = new HashMap<>();

        for (Map.Entry<String, String> pbvn : pb2Vn.entrySet()) {
            String pbFrame = pbvn.getKey();
            String vnFrame = pbvn.getValue();
            String fnFrame = vn2Fn.get(vnFrame);
            if (fnFrame != null) {
                pb2FnFrameMapping.put(pbFrame, fnFrame);
            }
        }

        pb2FnFrameRoleMapping = new HashMap<>();

        for (Map.Entry<Pair<String, String>, Pair<String, String>> pbvn : pb2VnRole.entrySet()) {
            Pair<String, String> pbRole = pbvn.getKey();
            Pair<String, String> vnRole = pbvn.getValue();
            Pair<String, String> fnRole = vn2FnRole.get(vnRole);
            System.out.println(pbRole);

            if (fnRole != null) {
                System.out.println(pbRole);
                pb2FnFrameRoleMapping.put(pbRole.getValue0() + "_ARG" + pbRole.getValue1(), fnRole.getValue0() + "_" + fnRole.getValue1());
            }
        }

    }
}
