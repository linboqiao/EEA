package edu.cmu.cs.lti.script.dist;

import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.script.utils.DataPool;
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
public class GlobalUnigrmHwLocalUniformArgumentDist extends BaseEventDist {
//    private final Random random;
//
//    /* The probability and alias tables. */
//    private final int[] alias;
//    private final double[] probability;

    private AliasMethod am;

    private Random random;

    public GlobalUnigrmHwLocalUniformArgumentDist(int numArguments) {
        super(numArguments);
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
//     * Draw a predicate and create a mention with given arguments.
//     *
//     * @param providedArguments
//     * @return
//     */
//    public Pair<LocalEventMentionRepre, Double> draw(LocalArgumentRepre[] providedArguments) {
//        String predicate = drawPredicate();
//        LocalEventMentionRepre repre = new LocalEventMentionRepre(predicate, providedArguments.clone());
//        return Pair.of(repre, DataPool.getPredicateProb(predicate));
//    }

    @Override
    public Pair<LocalEventMentionRepre, Double> draw(List<LocalArgumentRepre> candidateArguments) {
        String predicate = drawPredicate();

        //TODO do not let two argument share the same entity
        LocalArgumentRepre[] arguments = new LocalArgumentRepre[numArguments];
        for (int i = 0; i < numArguments; i++) {
            LocalArgumentRepre randomArgument = candidateArguments.get(random.nextInt(candidateArguments.size()));
            arguments[i] = randomArgument;
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

        //prepare data
        DataPool.loadHeadStatistics(config, false);
        GlobalUnigrmHwLocalUniformArgumentDist noiseDist = new GlobalUnigrmHwLocalUniformArgumentDist(3);
    }
}
