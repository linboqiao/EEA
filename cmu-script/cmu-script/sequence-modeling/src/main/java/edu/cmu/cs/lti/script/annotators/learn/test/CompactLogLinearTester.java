package edu.cmu.cs.lti.script.annotators.learn.test;

import edu.cmu.cs.lti.cds.ml.features.CompactFeatureExtractor;
import edu.cmu.cs.lti.collections.TLongIntDoubleHashTable;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.model.MooneyEventRepre;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.utils.Comparators;
import edu.cmu.cs.lti.utils.TwoLevelFeatureTable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import weka.core.SerializationHelper;

import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/14/15
 * Time: 1:16 AM
 */
public class CompactLogLinearTester extends MultiArgumentClozeTest {
    public static final String PARAM_DB_DIR_PATH = "dbLocation";
    public static final String PARAM_MODEL_PATH = "modelPath";
    public static final String PARAM_MAX_SKIP_GRAM_N = "maxSkipgramN";
    public static final String PARAM_FEATURE_NAMES = "featureNames";
    public static final String PARAM_USE_TEST_MODE = "useAllContext";

    private CompactFeatureExtractor extractor;
    private TwoLevelFeatureTable compactWeights;
    private int skipGramN;
    private String[] allPredicates;

    @Override
    protected String initializePredictor(UimaContext aContext) {
        String[] featureImplNames = (String[]) aContext.getConfigParameterValue(PARAM_FEATURE_NAMES);

        skipGramN = (Integer) aContext.getConfigParameterValue(PARAM_MAX_SKIP_GRAM_N);

        String modelPath = (String) aContext.getConfigParameterValue(PARAM_MODEL_PATH);

        boolean useAllContext = (Boolean) aContext.getConfigParameterValue(PARAM_USE_TEST_MODE);

        String predictorName = FilenameUtils.getBaseName(modelPath);

        if (useAllContext) {
            predictorName += "_full_context";
        }

        logger.info("Initializing tester : " + predictorName);
        logger.info("Loading from " + modelPath);


        try {
            compactWeights = (TwoLevelFeatureTable) SerializationHelper.read(modelPath);
            extractor = new CompactFeatureExtractor(compactWeights, featureImplNames, useAllContext);
        } catch (Exception e) {
            e.printStackTrace();
        }

        allPredicates = DataPool.headWords;
        return predictorName;
    }


    @Override
    protected PriorityQueue<Pair<MooneyEventRepre, Double>> predict(List<ContextElement> chain, Set<Integer> entities, int testIndex, int numArguments) {
        ContextElement answer = chain.get(testIndex);
        logEvalInfo("Answer is " + answer.getMention());

        PriorityQueue<Pair<MooneyEventRepre, Double>> rankedEvents = new PriorityQueue<>(allPredicates.length,
                new Comparators.DescendingScoredPairComparator<MooneyEventRepre, Double>());

        Set<Integer> mooneyEntities = getRewritedEntitiesFromChain(chain);

        ContextElement realElement = chain.get(testIndex);

        extractor.prepareGlobalFeatures(chain);

        for (String head : allPredicates) {
            List<MooneyEventRepre> candidateMooeyEvms = MooneyEventRepre.generateTuples(head, mooneyEntities);
            for (MooneyEventRepre candidateEvm : candidateMooeyEvms) {
//                ContextElement candidate = ContextElement.fromMooney(realElement.getJcas(), realElement.getSent(), realElement.getHead(), candidateEvm);
                ContextElement candidate = ContextElement.eraseGoldStandard(realElement, candidateEvm);

                TLongIntDoubleHashTable features = extractor.getFeatures(chain, candidate, testIndex, skipGramN);

                double score = compactWeights.dotProd(features);

                if (score > 0) {
                    logEvalInfo("Candidate is " + candidate.getMention() + "\t" + candidate.getMention().toMooneyMention());
                    logEvalInfo("Feature score " + score);
//                    System.err.println("Showing dot product details for positive scored ones: ");
//                    compactWeights.dotProd(features, DataPool.headWords);
//                    logEvalInfo("Candidate features : ");
//                    logEvalInfo(features.dump(DataPool.headWords, extractor.getFeatureNamesByIndex()));
                }

                if (candidate.getMention().mooneyMatch(answer.getMention())) {
                    logEvalInfo("Answer candidate appears: " + candidate.getMention() + "\t" + candidate.getMention().toMooneyMention());
                    logEvalInfo("Feature score " + score);
                    logEvalInfo("Answer features : ");
                    logEvalInfo(features.dump(DataPool.headWords, extractor.getFeatureNamesByIndex()));
                    logEvalResult(String.format("Answer score for %s is %.2f", candidateEvm, score));
//                    System.err.println("Showing dot product details for answer: ");
//                    System.err.println("Score is : " + compactWeights.dotProd(features, DataPool.headWords));
                }
                rankedEvents.add(Pair.of(candidateEvm, score));
            }
        }
        return rankedEvents;
    }

    public static Set<Integer> getRewritedEntitiesFromChain(List<ContextElement> chain) {
        Set<Integer> entities = new HashSet<>();
        for (ContextElement rep : chain) {
            for (LocalArgumentRepre arg : rep.getMention().getArgs()) {
                if (arg != null && !arg.isOther()) {
                    entities.add(arg.getRewrittenId());
                }
            }
        }
        return entities;
    }
}