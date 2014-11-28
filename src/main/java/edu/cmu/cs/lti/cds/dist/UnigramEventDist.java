package edu.cmu.cs.lti.cds.dist;

import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.annotators.script.train.UnigramScriptCounter;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/25/14
 * Time: 11:42 AM
 */
public class UnigramEventDist {
    AliasMethod alias;
    private static double[] probabilities;

    private static TIntObjectMap<TIntList> unigramId = new TIntObjectHashMap<>();

    public UnigramEventDist() {
        alias = new AliasMethod(unigramProb(), new Random());
    }

    private static double[] unigramProb() {
        probabilities = new double[DataPool.unigramCounts.size()];
        int index = 0;

        for (TObjectIntIterator<TIntList> iter = DataPool.unigramCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            probabilities[index] = iter.value() * 1.0 / DataPool.eventUnigramTotalCount;

            unigramId.put(index, iter.key());
            index++;
        }

        return probabilities;
    }

    /**
     * Draw a predicate and create a mention with given arguments.
     *
     * @return
     */
    public Pair<MooneyEventRepre, Double> draw() {
        int nextEventIndex = alias.next();
        TIntList nextEvent = unigramId.get(nextEventIndex);
        MooneyEventRepre repre = new MooneyEventRepre(DataPool.headWords[nextEvent.get(0)], nextEvent.get(1), nextEvent.get(2), nextEvent.get(3));
        return Pair.of(repre, probabilities[nextEventIndex]);
    }


    public static void main(String[] args) throws Exception {
        //test the unigram draw
        Configuration config = new Configuration(new File(args[0]));

        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String[] countingDbFileNames = config.getList("edu.cmu.cs.lti.cds.headcount.files");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;

        //prepare data
        DataPool.loadHeadCounts(dbPath, dbNames[0], KarlMooneyScriptCounter.defaltHeadIdMapName, countingDbFileNames);
        DataPool.loadEventUnigramCounts(dbPath, dbNames[0], UnigramScriptCounter.defaultUnigramMapName);
        UnigramEventDist noiseDist = new UnigramEventDist();

        while (true) {
            System.out.println(noiseDist.draw());
            Utils.pause();
        }
    }
}
