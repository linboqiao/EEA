package edu.cmu.cs.lti.script.dist;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Random;

/**
 * Code adopted and modified from Keith Schwarz (http://www.keithschwarz.com/)
 * <p/>
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/25/14
 * Time: 2:42 PM
 */
public class AliasMethod {

    double[] probability;
    int[] alias;
    Random random;

    /**
     * Constructs a new AliasMethod to sample from a discrete distribution and
     * hand back outcomes based on the probability distribution.
     * <p/>
     * Given as input a list of probabilities corresponding to outcomes 0, 1,
     * ..., n - 1, along with the random number generator that should be used
     * as the underlying generator, this constructor creates the probability
     * and alias tables needed to efficiently sample from this distribution.
     *
     * @param probabilities The list of probabilities.
     * @param random        The random number generator
     */
    public AliasMethod(double[] probabilities, Random random) {
        /* Allocate space for the probability and alias tables. */
        probability = new double[probabilities.length];
        alias = new int[probabilities.length];

        /* Store the underlying generator. */
        this.random = random;

        /* Compute the average probability and cache it for later use. */
        final double average = 1.0 / probabilities.length;

        /* Make a copy of the probabilities list, since we will be making
         * changes to it.
         */
        double[] workingProbabilities = Arrays.copyOf(probabilities, probabilities.length);

        /* Create two stacks to act as worklists as we populate the tables. */
        Deque<Integer> small = new ArrayDeque<Integer>();
        Deque<Integer> large = new ArrayDeque<Integer>();

        /* Populate the stacks with the input probabilities. */
        for (int i = 0; i < workingProbabilities.length; ++i) {
            /* If the probability is below the average probability, then we add
             * it to the small list; otherwise we add it to the large list.
             */
            if (workingProbabilities[i] >= average)
                large.add(i);
            else
                small.add(i);
        }

        /* As a note: in the mathematical specification of the algorithm, we
         * will always exhaust the small list before the big list.  However,
         * due to floating point inaccuracies, this is not necessarily true.
         * Consequently, this inner loop (which tries to pair small and large
         * elements) will have to check that both lists aren't empty.
         */
        while (!small.isEmpty() && !large.isEmpty()) {
            /* Get the index of the small and the large probabilities. */
            int less = small.removeLast();
            int more = large.removeLast();

            /* These probabilities have not yet been scaled up to be such that
             * 1/n is given weight 1.0.  We do this here instead.
             */
            probability[less] = workingProbabilities[less] * workingProbabilities.length;
            alias[less] = more;

            /* Decrease the probability of the larger one by the appropriate
             * amount.
             */
            workingProbabilities[more] = (workingProbabilities[more] + workingProbabilities[less]) - average;

            /* If the new probability is less than the average, add it into the
             * small list; otherwise add it to the large list.
             */
            if (workingProbabilities[more] >= 1.0 / workingProbabilities.length)
                large.add(more);
            else
                small.add(more);
        }

        /* At this point, everything is in one list, which means that the
         * remaining probabilities should all be 1/n.  Based on this, set them
         * appropriately.  Due to numerical issues, we can't be sure which
         * stack will hold the entries, so we empty both.
         */
        while (!small.isEmpty())
            probability[small.removeLast()] = 1.0;
        while (!large.isEmpty())
            probability[large.removeLast()] = 1.0;
    }

    /**
     * Samples a value from the underlying distribution.
     *
     * @return A random value sampled from the underlying distribution.
     */
    public int next() {
        /* Generate a fair die roll to determine which column to inspect. */
        int column = random.nextInt(probability.length);

        /* Generate a biased coin toss to determine which option to pick. */
        boolean coinToss = random.nextDouble() < probability[column];

        /* Based on the outcome, return either the column or its alias. */
        return coinToss ? column : alias[column];
    }
}
