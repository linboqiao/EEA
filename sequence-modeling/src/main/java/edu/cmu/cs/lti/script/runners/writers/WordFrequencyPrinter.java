package edu.cmu.cs.lti.script.runners.writers;

import edu.cmu.cs.lti.script.annotators.stats.EventMentionHeadTfDfCounter;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import weka.core.SerializationHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/19/15
 * Time: 10:07 PM
 */
public class WordFrequencyPrinter {
    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration(new File(args[0]));
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath");

        File tfPlainOut = new File(args[1]);
        File dfPlainOut = new File(args[2]);

        Writer tfWriter = new BufferedWriter(new FileWriter(tfPlainOut));
        Writer dfWriter = new BufferedWriter(new FileWriter(dfPlainOut));

        File tfFile = new File(dbPath, EventMentionHeadTfDfCounter.defaultDBName + "_" + EventMentionHeadTfDfCounter.defaultMentionHeadTfMapName);
        File dfFile = new File(dbPath, EventMentionHeadTfDfCounter.defaultDBName + "_" + EventMentionHeadTfDfCounter.defaultMentionHeadDfMapName);

        TIntIntMap tfCounts = (TIntIntMap) SerializationHelper.read(tfFile.getAbsolutePath());
        TIntIntMap dfCounts = (TIntIntMap) SerializationHelper.read(dfFile.getAbsolutePath());

        for (TIntIntIterator iter = tfCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            tfWriter.write(iter.key() + " " + iter.value() + "\n");
        }

        for (TIntIntIterator iter = dfCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            dfWriter.write(iter.key() + " " + iter.value() + "\n");
        }

    }
}
