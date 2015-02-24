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
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/25/14
 * Time: 11:42 AM
 */
public class TopCappedUnigramEventDist {
    AliasMethod alias;
    private static double[] probabilities;

    private static TIntObjectMap<TIntList> unigramId = new TIntObjectHashMap<>();

    public TopCappedUnigramEventDist(TObjectIntMap<TIntList> unigramCounts, int topKtoCap) {
        alias = new AliasMethod(unigramProb(unigramCounts, topKtoCap), new Random());
    }

    private static double[] unigramProb(TObjectIntMap<TIntList> unigramCounts, int topKtoCap) {
        probabilities = new double[unigramCounts.size()];
        int index = 0;

        Queue<Pair<Integer, String>> topUnigrams = new PriorityQueue<>();

        int eventUnigramTotalCount = 0;

        for (TObjectIntIterator<TIntList> iter = unigramCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            eventUnigramTotalCount += iter.value();
            topUnigrams.add(Pair.of(iter.value(), iter.key().toString()));

            if (topUnigrams.size() > topKtoCap) {
                topUnigrams.poll();
            }
        }

        int cappedSize = 0;
        Map<String, Integer> cappingDict = new HashMap<>();

        while (!topUnigrams.isEmpty()) {
            Pair<Integer, String> topUnigramCount = topUnigrams.poll();
            cappedSize += topUnigramCount.getKey();
            cappingDict.put(topUnigramCount.getRight(), cappedSize);
        }

        System.err.println(eventUnigramTotalCount + " " + cappedSize);

        for (TObjectIntIterator<TIntList> iter = unigramCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            if (!cappingDict.containsKey(iter.key().toString())) {
                probabilities[index] = iter.value() * 1.0 / (eventUnigramTotalCount - cappedSize);
                unigramId.put(index, iter.key());
                index++;
            } else {
                System.err.println(DataPool.headWords[iter.key().get(0)] + " " + iter.key() + " is ignored, count is " +cappingDict.get(iter.key().toString()));
            }
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
        TopCappedUnigramEventDist noiseDist = new TopCappedUnigramEventDist(DataPool.unigramCounts, 500);

        while (true) {
            System.out.println(noiseDist.draw());
            Utils.pause();
        }
    }
}
