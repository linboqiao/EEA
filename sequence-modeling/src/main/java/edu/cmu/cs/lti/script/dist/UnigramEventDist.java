package edu.cmu.cs.lti.script.dist;

import edu.cmu.cs.lti.script.model.MooneyEventRepre;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
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

    public UnigramEventDist(TObjectIntMap<TIntList> unigramCounts, long eventUnigramTotalCount) {
        alias = new AliasMethod(unigramProb(unigramCounts, eventUnigramTotalCount), new Random());
    }

    private static double[] unigramProb(TObjectIntMap<TIntList> unigramCounts, long eventUnigramTotalCount) {
        probabilities = new double[unigramCounts.size()];
        int index = 0;

        for (TObjectIntIterator<TIntList> iter = unigramCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            probabilities[index] = iter.value() * 1.0 / eventUnigramTotalCount;

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

        //prepare data
        DataPool.loadHeadStatistics(config, false);
        DataPool.loadEventUnigramCounts(config);
        UnigramEventDist noiseDist = new UnigramEventDist(DataPool.unigramCounts, DataPool.eventUnigramTotalCount);

        while (true) {
            System.out.println(noiseDist.draw());
            Utils.pause();
        }
    }
}
