package edu.cmu.cs.lti.script.annotators.learn.test;

import edu.cmu.cs.lti.script.annotators.learn.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.model.KmTargetConstants;
import edu.cmu.cs.lti.script.model.MooneyEventRepre;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.script.utils.MultiMapUtils;
import edu.cmu.cs.lti.utils.Comparators;
import edu.cmu.cs.lti.utils.Utils;
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

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/14/15
 * Time: 1:53 AM
 */
public class ConditionProbablityPredictor extends MultiArgumentClozeTest {

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String PARAM_DB_NAMES = "dbNames";

    public static final String PARAM_SMOOTHING = "smoothingParameter";

    private TObjectIntMap<TIntList>[] cooccCountMaps;
    private TObjectIntMap<TIntList>[] occCountMaps;
    private TObjectIntMap<String>[] headIdMaps;

    private double smoothingParameter;

    private String[] allPredicates = DataPool.headWords;

    @Override
    protected String initializePredictor(UimaContext aContext) {
        Utils.printMemInfo(logger, "Initial memory information ");

        String dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);

        List<String> dbNameList = ((List<String>) aContext.getConfigParameterValue(PARAM_DB_NAMES));

        String[] dbNames = dbNameList.toArray(new String[dbNameList.size()]);

        smoothingParameter = (Double) aContext.getConfigParameterValue(PARAM_SMOOTHING);

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

        Utils.printMemInfo(logger, "Memory info after all loading");

        return this.getClass().getSimpleName();
    }

    @Override
    protected PriorityQueue<Pair<MooneyEventRepre, Double>> predict(List<ContextElement> chain, List entities, int testIndex, int numArguments) {
        PriorityQueue<Pair<MooneyEventRepre, Double>> rankedEvents = new PriorityQueue<>(allPredicates.length, new Comparators.DescendingScoredPairComparator<MooneyEventRepre, Double>());

        MooneyEventRepre answer = chain.get(testIndex).getMention().toMooneyMention();

        logger.info("Answer is " + answer);

        for (String head : allPredicates) {
            List<MooneyEventRepre> candidateEvms = MooneyEventRepre.generateTuples(head, entities);
            int numTotalEvents = candidateEvms.size() * allPredicates.length;

            for (MooneyEventRepre candidateEvm : candidateEvms) {
                boolean sawAnswer = false;
                if (answer.equals(candidateEvm)) {
                    sawAnswer = true;
                    logger.info("Answer candidate appears: " + candidateEvm);
                }

                double score = 0;
                for (int i = 0; i < testIndex; i++) {
                    Pair<MooneyEventRepre, MooneyEventRepre> transformedTuples = formerBasedTransform(chain.get(i).getMention().toMooneyMention(), candidateEvm);
                    double precedingScore = conditionalFollowing(transformedTuples.getLeft(), transformedTuples.getRight(), smoothingParameter, numTotalEvents);
                    score += precedingScore;
                }

                for (int i = testIndex + 1; i < chain.size(); i++) {
                    Pair<MooneyEventRepre, MooneyEventRepre> transformedTuples = formerBasedTransform(candidateEvm, chain.get(i).getMention().toMooneyMention());
                    double followingScore = conditionalFollowing(transformedTuples.getLeft(), transformedTuples.getRight(), smoothingParameter, numTotalEvents);
                    score += followingScore;
                }

                if (sawAnswer) {
                    String record = String.format("Answer score for %s is %.2f", candidateEvm, score);
                    logger.info(record);
                    evalLog(record);
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

    //this is correct but slow.
    private double conditionalFollowing(MooneyEventRepre former, MooneyEventRepre latter, double laplacianSmoothingParameter, int numTotalEvents) {
        Pair<Integer, Integer> counts = MultiMapUtils.getCounts(former, latter, cooccCountMaps, occCountMaps, headIdMaps);

        double cooccCountSmoothed = counts.getRight() + laplacianSmoothingParameter;
        double formerOccCountSmoothed = counts.getLeft() + numTotalEvents * laplacianSmoothingParameter;

        if (cooccCountSmoothed > laplacianSmoothingParameter) {
            logger.fine("Probability of seeing " + former + " before " + latter);
            logger.fine(cooccCountSmoothed / formerOccCountSmoothed + " " + counts.getRight() + "/" + counts.getLeft());
        }

        //add one smoothing
        return Math.log(cooccCountSmoothed / formerOccCountSmoothed);
    }


}
