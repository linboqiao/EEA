package edu.cmu.cs.lti.cds.runners.stats;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.utils.DbManager;
import org.apache.commons.io.FileUtils;
import org.mapdb.DB;
import org.mapdb.Fun;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/27/14
 * Time: 4:42 PM
 */
public class HeadStatisticPrinter {
    private Map<String, Fun.Tuple2<Integer, Integer>> headTfDfMap;

    public HeadStatisticPrinter() {

    }

    private void loadCounts(String dbPath, String countingDbFileName) {
        DB headCountDb = DbManager.getDB(dbPath, countingDbFileName);
        headTfDfMap = headCountDb.getHashMap(EventMentionHeadCounter.defaultMentionHeadMapName);
    }

    private void writeTfStatistics(File statDir) throws IOException {
        ArrayListMultimap<Integer, String> stats = getTfStatistics();
        for (int i = 0; i < 5; i++) {
            FileUtils.writeStringToFile(new File(statDir, "head_stats_94-96_infrequent_words"), i + "\t" + Joiner.on(" ").join(stats.get(i)) + "\n", true);
        }

        for (Map.Entry<Integer, Collection<String>> stat : stats.asMap().entrySet()) {
            FileUtils.writeStringToFile(new File(statDir, "head_stats_94-96_dist"), stat.getKey() + "\t" + stat.getValue().size() + "\n", true);

            if (stat.getValue().size() == 1) {
                FileUtils.writeStringToFile(new File(statDir, "head_stats_94-96_long_tail"), Joiner.on(" ").join(stat.getValue()) + "\n", true);
            }
        }
    }

    private ArrayListMultimap<Integer, String> getTfStatistics() {
        ArrayListMultimap<Integer, String> countBin2HeadCounts = ArrayListMultimap.create();
        for (Map.Entry<String, Fun.Tuple2<Integer, Integer>> headCounts : headTfDfMap.entrySet()) {
            countBin2HeadCounts.put(headCounts.getValue().a / 10, headCounts.getKey());
        }
        return countBin2HeadCounts;
    }

    private void printTfStatistics() {
        ArrayListMultimap<Integer, String> countBin2HeadCounts = ArrayListMultimap.create();
        for (Map.Entry<String, Fun.Tuple2<Integer, Integer>> headCounts : headTfDfMap.entrySet()) {
            countBin2HeadCounts.put(headCounts.getValue().b, headCounts.getKey());
        }
    }

    public static void main(String[] args) throws IOException {
        HeadStatisticPrinter printer = new HeadStatisticPrinter();
        printer.loadCounts("data/_db", "headcounts_94-96");
        System.out.println("Total number of terms: " + printer.headTfDfMap.size());

        printer.writeTfStatistics(new File("stats"));

    }
}
