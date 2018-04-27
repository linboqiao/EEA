package edu.cmu.cs.lti.script.dist;

import edu.cmu.cs.lti.script.model.KmTargetConstants;
import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.script.model.MooneyEventRepre;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.DebugUtils;
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
public class TopCappedUnigramEventDist extends BaseEventDist {
    AliasMethod alias;
    private static double[] probabilities;
    private Random random;

    private static TIntObjectMap<TIntList> unigramId = new TIntObjectHashMap<>();

    public TopCappedUnigramEventDist(TObjectIntMap<TIntList> unigramCounts, int topKtoCap, int numArguments) {
        super(numArguments);
        random = new Random();
        alias = new AliasMethod(unigramProb(unigramCounts, topKtoCap), random);
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

        int maxUnigramId = 0;

        for (TObjectIntIterator<TIntList> iter = unigramCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            if (!cappingDict.containsKey(iter.key().toString())) {
                probabilities[index] = iter.value() * 1.0 / (eventUnigramTotalCount - cappedSize);
                unigramId.put(index, iter.key());
                if (iter.key().get(0) > maxUnigramId) {
                    maxUnigramId = iter.key().get(0);
                }
                index++;
            } else {
//                System.err.println(DataPool.headWords[iter.key().get(0)] + " " + iter.key() + " is ignored, count is " + cappingDict.get(iter.key().toString()));
            }
        }

        System.err.println("Max unigram id is " + maxUnigramId);

        return probabilities;
    }

    /**
     * Draw a predicate and create a mention with given arguments.
     *
     * @return
     */
    public Pair<LocalEventMentionRepre, Double> draw(List<LocalArgumentRepre> candidateArguments) {
        int nextEventIndex = alias.next();
        TIntList nextEvent = unigramId.get(nextEventIndex);

        String predicate = DataPool.headWords[nextEvent.get(0)];

        Map<Integer, LocalArgumentRepre> chosenArgumentMap = new HashMap<>();
        for (int i = 1; i < nextEvent.size(); i++) {
            int argumentId = nextEvent.get(i);
            if (argumentId != KmTargetConstants.nullArgMarker) {
                chosenArgumentMap.put(i - 1, candidateArguments.get(random.nextInt(candidateArguments.size())));
            }
        }

        LocalArgumentRepre[] arguments = new LocalArgumentRepre[numArguments];

        for (int i = 0; i < numArguments; i++) {
            arguments[0] = chosenArgumentMap.get(i);
        }

        LocalEventMentionRepre repre = new LocalEventMentionRepre(predicate, arguments);
        double probOfArguments = Math.pow(1.0 / candidateArguments.size(), chosenArgumentMap.size());
        return Pair.of(repre, probabilities[nextEventIndex] * probOfArguments);
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
        TopCappedUnigramEventDist noiseDist = new TopCappedUnigramEventDist(DataPool.unigramCounts, 500, 3);

        while (true) {
            System.out.println(noiseDist.draw());
            DebugUtils.pause();
        }
    }
}
