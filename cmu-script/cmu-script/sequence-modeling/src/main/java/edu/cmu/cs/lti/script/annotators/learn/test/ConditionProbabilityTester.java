package edu.cmu.cs.lti.script.annotators.learn.test;

import edu.cmu.cs.lti.script.annotators.learn.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.model.KmTargetConstants;
import edu.cmu.cs.lti.script.model.MooneyEventRepre;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.script.utils.MultiMapUtils;
import edu.cmu.cs.lti.uima.util.BasicConvenience;
import edu.cmu.cs.lti.utils.Comparators;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/14/15
 * Time: 1:53 AM
 */
public class ConditionProbabilityTester extends MultiArgumentClozeTest {

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String PARAM_DB_NAMES = "dbNames";

    public static final String PARAM_SMOOTHING = "smoothingParameter";

    private TObjectIntMap<TIntList>[] cooccCountMaps;
    private TObjectIntMap<TIntList>[] occCountMaps;
    private TObjectIntMap<String>[] headIdMaps;

    private float smoothingParameter;

    private String[] allPredicates = DataPool.headWords;

    private long numTotalEvents = DataPool.predicateTotalCount;

    @Override
    protected String initializePredictor(UimaContext aContext) {
        BasicConvenience.printMemInfo(logger, "Initial memory information ");

        String dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);

        String[] dbNames = (String[]) aContext.getConfigParameterValue(PARAM_DB_NAMES);

        smoothingParameter = (Float) aContext.getConfigParameterValue(PARAM_SMOOTHING);

        String cooccName = KarlMooneyScriptCounter.defaultCooccMapName;

        String occName = KarlMooneyScriptCounter.defaultOccMapName;

        String headIdMapName = KarlMooneyScriptCounter.defaltHeadIdMapName;

        try {
            cooccCountMaps = MultiMapUtils.loadMaps(dbPath, dbNames, cooccName, logger, "Loading coocc");
            occCountMaps = MultiMapUtils.loadMaps(dbPath, dbNames, occName, logger, "Loading occ");
            headIdMaps = MultiMapUtils.loadMaps(dbPath, dbNames, headIdMapName, logger, "Loading head ids");
        } catch (Exception e) {
            e.printStackTrace();
        }

        BasicConvenience.printMemInfo(logger, "Memory info after all loading");

        logger.info(String.format("Will smooth the distribution with laplace [%.1f] and N [%d]", smoothingParameter, numTotalEvents));

        return this.getClass().getSimpleName();
    }

    @Override
    protected PriorityQueue<Pair<MooneyEventRepre, Double>> predict(List<ContextElement> chain, Set<Integer> entities, int testIndex, int numArguments) {
        PriorityQueue<Pair<MooneyEventRepre, Double>> rankedEvents = new PriorityQueue<>(allPredicates.length, new Comparators.DescendingScoredPairComparator<MooneyEventRepre, Double>());

        MooneyEventRepre answer = chain.get(testIndex).getMention().toMooneyMention();

        logEvalInfo("Answer is " + answer);

        for (String head : allPredicates) {
            List<MooneyEventRepre> candidateEvms = MooneyEventRepre.generateTuples(head, entities);

//            int numTotalEvents = candidateEvms.size() * allPredicates.length;

            for (MooneyEventRepre candidateEvm : candidateEvms) {
                boolean sawAnswer = false;
                if (answer.equals(candidateEvm)) {
                    sawAnswer = true;
                    logEvalInfo("Answer candidate appears: " + candidateEvm);
                }

                double score = 0;
                for (int i = 0; i < testIndex; i++) {
                    MooneyEventRepre previousMention = chain.get(i).getMention().toMooneyMention();
                    Pair<MooneyEventRepre, MooneyEventRepre> transformedTuples = formerBasedTransform(previousMention, candidateEvm);

//                    double precedingScore = conditionalFollowing(transformedTuples.getLeft(), transformedTuples.getRight(), smoothingParameter, numTotalEvents);
                    Pair<Integer, Integer> counts = followingCounts(transformedTuples.getLeft(), transformedTuples.getRight());
                    double precedingScore = computeConditionalProbability(counts.getLeft(), counts.getRight(), smoothingParameter, numTotalEvents);

                    score += precedingScore;

                    if (sawAnswer) {
                        if (counts.getRight() != 0) {
                            logEvalInfo(String.format("Saw preceding training example : [%s] with occ %d and coocc %d", previousMention, counts.getLeft(), counts.getRight()));
                        }
                        logEvalInfo(String.format("Preceding score with %s is %.3f", previousMention, precedingScore));
                    }
                }

                for (int i = testIndex + 1; i < chain.size(); i++) {
                    MooneyEventRepre followingMention = chain.get(i).getMention().toMooneyMention();
                    Pair<MooneyEventRepre, MooneyEventRepre> transformedTuples = formerBasedTransform(candidateEvm, followingMention);

//                    double followingScore = conditionalFollowing(transformedTuples.getLeft(), transformedTuples.getRight(), smoothingParameter, numTotalEvents);

                    Pair<Integer, Integer> counts = followingCounts(transformedTuples.getLeft(), transformedTuples.getRight());
                    double followingScore = computeConditionalProbability(counts.getLeft(), counts.getRight(), smoothingParameter, numTotalEvents);

                    score += followingScore;

                    if (sawAnswer) {
                        if (counts.getRight() != 0) {
                            logEvalInfo(String.format("Saw following training example : [%s] with occ %d and coocc %d", followingMention, counts.getLeft(), counts.getRight()));
                            logEvalInfo(String.format("Occ count : %d, coocc count : %d, laplacian : %.4f, total events : %d",
                                    counts.getLeft(), counts.getRight(), smoothingParameter, numTotalEvents));
                        }
                        logEvalInfo(String.format("Following score with %s is %.3f", followingMention, followingScore));
                    }
                }

                if (sawAnswer) {
                    String record = String.format("Answer score for %s is %.2f", candidateEvm, score);
                    logEvalInfo(record);
                    logEvalResult(record);
                }
                rankedEvents.add(Pair.of(candidateEvm, score));
            }
        }
        return rankedEvents;
    }


    public static Pair<MooneyEventRepre, MooneyEventRepre> formerBasedTransform(MooneyEventRepre former, MooneyEventRepre latter) {
        TIntIntMap transformMap = new TIntIntHashMap();

        //TODO change representation to general array
        int[] formerArguments = former.getAllArguments();
        int[] latterArguments = latter.getAllArguments();

        MooneyEventRepre transformedFormer = new MooneyEventRepre();
        MooneyEventRepre transformedLatter = new MooneyEventRepre();

        transformedFormer.setPredicate(former.getPredicate());
        transformedLatter.setPredicate(latter.getPredicate());

        for (int formerSlotId = 0; formerSlotId < formerArguments.length; formerSlotId++) {
            int originFormerId = formerArguments[formerSlotId];
            if (originFormerId == 0 || originFormerId == -1) {
                transformedFormer.setArgument(formerSlotId, originFormerId);
            } else {
                int formerBasedArgumentId = KmTargetConstants.slotIndexToArgMarker(formerSlotId);
                transformMap.put(originFormerId, formerBasedArgumentId);
                transformedFormer.setArgument(formerSlotId, formerBasedArgumentId);
            }
        }

        TIntSet mask = new TIntHashSet();
        for (int latterSlotId = 0; latterSlotId < latterArguments.length; latterSlotId++) {
            int latterArgumentId = latterArguments[latterSlotId];
            if (latterArgumentId == 0 || latterArgumentId == -1) {
                transformedLatter.setArgument(latterSlotId, latterArgumentId);
            } else {
                int transformedId = transformMap.get(latterArgumentId);
                transformedLatter.setArgument(latterSlotId, transformedId);
                mask.add(transformedId);
            }
        }

        int[] transformedFormerArgument = transformedFormer.getAllArguments();
        for (int formerSlotId = 0; formerSlotId < transformedFormerArgument.length; formerSlotId++) {
            int formerArgumentId = transformedFormerArgument[formerSlotId];
            if (formerArgumentId != 0 && formerArgumentId != -1) {
                if (!mask.contains(formerArgumentId)) {
                    transformedFormer.setArgument(formerSlotId, 0);
                }
            }
        }
        return Pair.of(transformedFormer, transformedLatter);
    }

    private Pair<Integer, Integer> followingCounts(MooneyEventRepre former, MooneyEventRepre latter) {
        return MultiMapUtils.getCounts(former, latter, cooccCountMaps, occCountMaps, headIdMaps);
    }

    private double computeConditionalProbability(int occCount, int cooccCount, double laplaceSmoothingParameter, long numTotalEvents) {
        return Math.log((cooccCount + laplaceSmoothingParameter) / (occCount + numTotalEvents * laplaceSmoothingParameter));
    }

//    private double conditionalFollowing(MooneyEventRepre former, MooneyEventRepre latter, double laplacianSmoothingParameter, int numTotalEvents) {
//        Pair<Integer, Integer> counts = MultiMapUtils.getCounts(former, latter, cooccCountMaps, occCountMaps, headIdMaps);
//
//        double cooccCountSmoothed = counts.getRight() + laplacianSmoothingParameter;
//        double formerOccCountSmoothed = counts.getLeft() + numTotalEvents * laplacianSmoothingParameter;
//
//        //add one smoothing
//        return Math.log(cooccCountSmoothed / formerOccCountSmoothed);
//    }
}
