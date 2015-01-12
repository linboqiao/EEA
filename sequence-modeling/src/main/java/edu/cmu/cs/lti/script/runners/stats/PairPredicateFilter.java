package edu.cmu.cs.lti.script.runners.stats;

import edu.cmu.cs.lti.script.annotators.stats.EventMentionCooccCounter;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.utils.BitUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TLongLongIterator;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongLongHashMap;
import org.apache.commons.lang3.tuple.Pair;
import weka.core.SerializationHelper;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 12/19/14
 * Time: 12:56 PM
 */
public class PairPredicateFilter {
    public static void main(String args[]) throws Exception {
        TLongLongMap trimedMap = new TLongLongHashMap();
        Configuration config = new Configuration(new File(args[0]));
        DataPool.loadHeadStatistics(config, true);
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath");
        for (TLongLongIterator iter = DataPool.headPairMap.iterator(); iter.hasNext(); ) {
            iter.advance();
            long pairId = iter.key();

            Pair<Integer, Integer> wordIds = BitUtils.get2IntFromLong(pairId);
            if (!DataPool.lowFreqFilter(wordIds.getLeft()) && !DataPool.lowFreqFilter(wordIds.getRight())) {
                trimedMap.put(pairId, iter.value());
            }
        }

        System.err.println("Total pairs after trimming: " + trimedMap.size());
        try {
            SerializationHelper.write(
                    new File(dbPath, EventMentionCooccCounter.defaultDBName +
                            "_" + EventMentionCooccCounter.defaultMentionPairCountName
                            + "_trimmed").getAbsolutePath(), trimedMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
