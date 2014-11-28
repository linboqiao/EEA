package edu.cmu.cs.lti.cds.dist;

import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.model.LocalArgumentRepre;
import edu.cmu.cs.lti.cds.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TObjectIntIterator;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/3/14
 * Time: 3:39 PM
 */
public class GlobalUnigrmHwLocalUniformArgumentDist {
//    private final Random random;
//
//    /* The probability and alias tables. */
//    private final int[] alias;
//    private final double[] probability;

    private AliasMethod am;

    private Random random;

    public GlobalUnigrmHwLocalUniformArgumentDist() {
//        this(headCounts2Probs(), new Random());
        random = new Random();
        am = new AliasMethod(headCounts2Probs(), random);
    }

    private static double[] headCounts2Probs() {
        double[] probabilities = new double[DataPool.headIdMap.size()];
        for (TObjectIntIterator<String> iter = DataPool.headIdMap.iterator(); iter.hasNext(); ) {
            iter.advance();
            probabilities[iter.value()] = DataPool.getPredicateProb(iter.key());
        }
        return probabilities;
    }

//    /**
//     * Constructs a new AliasMethod to sample from a discrete distribution and
//     * hand back outcomes based on the probability distribution.
//     * <p/>
//     * Given as input a list of probabilities corresponding to outcomes 0, 1,
//     * ..., n - 1, along with the random number generator that should be used
//     * as the underlying generator, this constructor creates the probability
//     * and alias tables needed to efficiently sample from this distribution.
//     *
//     * @param probabilities The list of probabilities.
//     * @param random        The random number generator
//     */
//    public GlobalUnigrmHwLocalUniformArgumentDist(double[] probabilities, Random random) {
//        /* Allocate space for the probability and alias tables. */
//        probability = new double[probabilities.length];
//        alias = new int[probabilities.length];
//
//        /* Store the underlying generator. */
//        this.random = random;
//
//        /* Compute the average probability and cache it for later use. */
//        final double average = 1.0 / probabilities.length;
//
//        /* Make a copy of the probabilities list, since we will be making
//         * changes to it.
//         */
//        double[] workingProbabilities = Arrays.copyOf(probabilities, probabilities.length);
//
//        /* Create two stacks to act as worklists as we populate the tables. */
//        Deque<Integer> small = new ArrayDeque<Integer>();
//        Deque<Integer> large = new ArrayDeque<Integer>();
//
//        /* Populate the stacks with the input probabilities. */
//        for (int i = 0; i < workingProbabilities.length; ++i) {
//            /* If the probability is below the average probability, then we add
//             * it to the small list; otherwise we add it to the large list.
//             */
//            if (workingProbabilities[i] >= average)
//                large.add(i);
//            else
//                small.add(i);
//        }
//
//        /* As a note: in the mathematical specification of the algorithm, we
//         * will always exhaust the small list before the big list.  However,
//         * due to floating point inaccuracies, this is not necessarily true.
//         * Consequently, this inner loop (which tries to pair small and large
//         * elements) will have to check that both lists aren't empty.
//         */
//        while (!small.isEmpty() && !large.isEmpty()) {
//            /* Get the index of the small and the large probabilities. */
//            int less = small.removeLast();
//            int more = large.removeLast();
//
//            /* These probabilities have not yet been scaled up to be such that
//             * 1/n is given weight 1.0.  We do this here instead.
//             */
//            probability[less] = workingProbabilities[less] * workingProbabilities.length;
//            alias[less] = more;
//
//            /* Decrease the probability of the larger one by the appropriate
//             * amount.
//             */
//            workingProbabilities[more] = (workingProbabilities[more] + workingProbabilities[less]) - average;
//
//            /* If the new probability is less than the average, add it into the
//             * small list; otherwise add it to the large list.
//             */
//            if (workingProbabilities[more] >= 1.0 / workingProbabilities.length)
//                large.add(more);
//            else
//                small.add(more);
//        }
//
//        /* At this point, everything is in one list, which means that the
//         * remaining probabilities should all be 1/n.  Based on this, set them
//         * appropriately.  Due to numerical issues, we can't be sure which
//         * stack will hold the entries, so we empty both.
//         */
//        while (!small.isEmpty())
//            probability[small.removeLast()] = 1.0;
//        while (!large.isEmpty())
//            probability[large.removeLast()] = 1.0;
//    }
//
//    /**
//     * Samples a value from the underlying distribution.
//     *
//     * @return A random value sampled from the underlying distribution.
//     */
//    public int next() {
//        /* Generate a fair die roll to determine which column to inspect. */
//        int column = random.nextInt(probability.length);
//
//        /* Generate a biased coin toss to determine which option to pick. */
//        boolean coinToss = random.nextDouble() < probability[column];
//
//        /* Based on the outcome, return either the column or its alias. */
//        return coinToss ? column : alias[column];
//    }

    /**
     * Draw a predicate and create a mention with given arguments.
     *
     * @param providedArguments
     * @return
     */
    public Pair<LocalEventMentionRepre, Double> draw(LocalArgumentRepre[] providedArguments) {
        String predicate = drawPredicate();
        LocalEventMentionRepre repre = new LocalEventMentionRepre(predicate, providedArguments.clone());
        return Pair.of(repre, DataPool.getPredicateProb(predicate));
    }

    public Pair<LocalEventMentionRepre, Double> draw(List<LocalArgumentRepre> candidateArguments, int numArguments) {
        String predicate = drawPredicate();

        //TODO do not let two argument share the same entity
        LocalArgumentRepre[] arguments = new LocalArgumentRepre[numArguments];
        for (int i = 0; i < numArguments; i++) {
            LocalArgumentRepre randomArgument = candidateArguments.get(random.nextInt(candidateArguments.size()));
        }
        LocalEventMentionRepre repre = new LocalEventMentionRepre(predicate, arguments);
        return Pair.of(repre, probOf(repre, candidateArguments.size(), numArguments));
    }

    public String drawPredicate() {
        return DataPool.headWords[am.next()];
    }

    public Double probOf(LocalEventMentionRepre evm, int numCandidates, int numArguments) {
        double probOfEvm = DataPool.getPredicateProb(evm.getMentionHead());
        double probOfArguments = Math.pow(1.0 / numCandidates, numArguments);
        return probOfEvm * probOfArguments;
    }

    public static void main(String[] args) throws Exception {
        //test the unigram draw
        Configuration config = new Configuration(new File(args[0]));

        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String[] countingDbFileNames = config.getList("edu.cmu.cs.lti.cds.headcount.files");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;

        //prepare data
        DataPool.loadData(dbPath, dbNames[0], KarlMooneyScriptCounter.defaltHeadIdMapName, countingDbFileNames);
        GlobalUnigrmHwLocalUniformArgumentDist noiseDist = new GlobalUnigrmHwLocalUniformArgumentDist();
    }
}
