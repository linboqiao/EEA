package edu.cmu.cs.lti.script.runners.writers;

import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TObjectIntIterator;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/5/15
 * Time: 4:23 PM
 */
public class PredicateOccurrencePrinter {
    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration(new File(args[0]));
        DataPool.loadHeadStatistics(config, false);

        File outFile = new File("predicate_stats");

        for (TObjectIntIterator<String> iter = DataPool.headIdMap.iterator(); iter.hasNext(); ) {
            iter.advance();
            long freq = DataPool.getPredicateFreq(iter.value());
            String predicate = iter.key();
            FileUtils.writeStringToFile(outFile, predicate + " " + freq + "\n", true);
        }
    }
}
