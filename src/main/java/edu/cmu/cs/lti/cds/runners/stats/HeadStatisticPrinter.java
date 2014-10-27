package edu.cmu.cs.lti.cds.runners.stats;

import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.utils.DbManager;
import org.mapdb.DB;
import org.mapdb.Fun;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/27/14
 * Time: 4:42 PM
 */
public class HeadStatisticPrinter {
    private Map<String, Fun.Tuple2<Integer, Integer>> headCountMap;

    public HeadStatisticPrinter() {

    }

    private void loadCounts(String dbPath, String countingDbFileName) {
        DB headCountDb = DbManager.getDB(dbPath, countingDbFileName);
        headCountMap = headCountDb.getHashMap(EventMentionHeadCounter.defaultMentionHeadMapName);
    }

    public static void main(String[] args) {
        HeadStatisticPrinter printer = new HeadStatisticPrinter();
        printer.loadCounts("data/_db", "headcounts_94-96");
        System.out.println(printer.headCountMap.size());
    }
}
