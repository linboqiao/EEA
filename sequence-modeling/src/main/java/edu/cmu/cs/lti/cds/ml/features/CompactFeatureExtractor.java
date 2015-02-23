package edu.cmu.cs.lti.cds.ml.features;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.utils.BitUtils;
import edu.cmu.cs.lti.utils.TwoLevelFeatureTable;
import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/2/14
 * Time: 4:47 PM
 */
public class CompactFeatureExtractor {
    TwoLevelFeatureTable featureTable;

    static Logger logger = Logger.getLogger(CompactFeatureExtractor.class.getName());

    private TObjectIntMap<TIntList> positiveObservations;
    private TObjectIntMap<String> headMap;

    private List<PairwiseFeature> singleFeatureImpls;
    private List<GlobalFeature> globalFeatureImpls;

    public <T extends TwoLevelFeatureTable> CompactFeatureExtractor(T featureTable, List<PairwiseFeature> singleFeatureImpls, List<GlobalFeature> globalFeatureImpls) {
        this.featureTable = featureTable;
        this.positiveObservations = DataPool.cooccCountMaps;
        this.headMap = DataPool.headIdMap;
        this.singleFeatureImpls = singleFeatureImpls;
        this.globalFeatureImpls = globalFeatureImpls;

        for (Feature featureImpl : singleFeatureImpls) {
            logger.info("Single feature registered: " + featureImpl.getClass().getSimpleName());
        }

        for (Feature featureImpl : globalFeatureImpls) {
            logger.info("Global feature registered: " + featureImpl.getClass().getSimpleName());
        }


        logger.info("Feature table lexical size: " + featureTable.getNumRows());
        logger.info("Feature table feature type size: " + featureTable.getFeatureNameMap().size());
    }

    public <T extends TwoLevelFeatureTable> CompactFeatureExtractor(T featureTable, String[] featureImplNames)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this(featureTable, featuresByName(featureImplNames).getKey(), featuresByName(featureImplNames).getRight());
    }

    private static Pair<List<PairwiseFeature>, List<GlobalFeature>> featuresByName(String[] featureImplNames)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        List<PairwiseFeature> pairwiseFeatureImpls = new ArrayList<>();
        List<GlobalFeature> globalFeatureImpls = new ArrayList<>();
        for (String featureImplName : featureImplNames) {
            Class<?> c = Class.forName(featureImplName);
            Feature f = (Feature) c.newInstance();

            if (f instanceof PairwiseFeature) {
                pairwiseFeatureImpls.add((PairwiseFeature) f);
            } else if (f instanceof GlobalFeature) {
                globalFeatureImpls.add((GlobalFeature) f);
            }

        }

        return Pair.of(pairwiseFeatureImpls, globalFeatureImpls);
    }

    /**
     * precompute stuff for global features
     *
     * @param chain
     */
    public void prepareGlobalFeatures(List<ContextElement> chain) {
        for (GlobalFeature globalFeature : globalFeatureImpls) {
            globalFeature.preprocessChain(chain);
        }
    }

    public TLongShortDoubleHashTable getFeatures(List<ContextElement> chain, ContextElement targetMention, int index, int maxSkippedGramN) {
        TLongShortDoubleHashTable extractedFeatures = new TLongShortDoubleHashTable();
        targetMention.setIsTarget(true);
        for (Triple<ContextElement, ContextElement, Integer> ngram : getSkippedNgrams(chain, targetMention, index, maxSkippedGramN)) {
            extractSingleFeatures(extractedFeatures, ngram.getLeft(), ngram.getMiddle(), ngram.getRight());
        }
        //the chain itself must be preprocessed
        extractGlobalFeatures(extractedFeatures, targetMention, index);
        return extractedFeatures;
    }

    private void extractSingleFeatures(TLongShortDoubleHashTable extractedFeatures, ContextElement elementLeft, ContextElement elementRight, int skip) {
        long predicatePair = getCompactPredicatePair(elementLeft.getMention().getMentionHead(), elementRight.getMention().getMentionHead());
        for (PairwiseFeature featureImpl : singleFeatureImpls) {
            if (featureImpl.isLexicalized()) {
                for (Map.Entry<String, Double> f : featureImpl.getFeature(elementLeft, elementRight, skip).entrySet()) {
                    short featureIndex = featureTable.getOrPutFeatureIndex(f.getKey());
                    extractedFeatures.put(predicatePair, featureIndex, f.getValue());
                }
            }
        }
    }

    private void extractGlobalFeatures(TLongShortDoubleHashTable extractedFeatures, ContextElement targetElement, int targetIndex) {
        int lastInt = DataPool.headWords.length;
        long specialPair = BitUtils.store2Int(lastInt, lastInt);
        for (GlobalFeature featureImpl : globalFeatureImpls) {
            for (Map.Entry<String, Double> f : featureImpl.getFeature(targetElement, targetIndex).entrySet()) {
                short featureIndex = featureTable.getOrPutFeatureIndex(f.getKey());
                extractedFeatures.put(specialPair, featureIndex, f.getValue());
            }
        }
    }


    public <T extends Object> List<Triple<T, T, Integer>> getSkippedNgrams(List<T> sequence, T target, int index, int skipgramN) {
        List<Triple<T, T, Integer>> formerPairs = getSkipGramBefore(sequence, target, index, skipgramN);
        List<Triple<T, T, Integer>> latterPairs = getSkipGramsAfter(sequence, target, index, skipgramN);

        List<Triple<T, T, Integer>> allPairs = new ArrayList<>();
        allPairs.addAll(formerPairs);
        allPairs.addAll(latterPairs);

        return allPairs;
    }

    private long getCompactPredicatePair(String word1, String word2) {
        int pre1 = DataPool.headIdMap.get(word1);
        int pre2 = DataPool.headIdMap.get(word2);

        return BitUtils.store2Int(pre1, pre2);
    }

    public <T extends Object> List<Triple<T, T, Integer>> getSkipGramsAfter(List<T> sequence, T target, int index, int k) {
        List<Triple<T, T, Integer>> skipGrams = new ArrayList<>();

        int count = 0;
        for (int i = index + 1; i < sequence.size(); i++) {
            skipGrams.add(Triple.of(target, sequence.get(i), count));
            count++;
            if (count > k) {
                break;
            }
        }
        return skipGrams;
    }

    private <T extends Object> List<Triple<T, T, Integer>> getSkipGramBefore(List<T> sequence, T target, int index, int k) {
        List<Triple<T, T, Integer>> skipGrams = new ArrayList<>();

        int count = 0;
        for (int i = index - 1; i > 0; i--) {
            skipGrams.add(Triple.of(sequence.get(i), target, count));
            count++;
            if (count > k) {
                break;
            }
        }
        return skipGrams;
    }

    public String getFeatureName(short featureIndex) {
        return featureTable.getFeatureName(featureIndex);
    }

    public Short getFeatureIndex(String featureName) {
        return featureTable.getFeatureIndex(featureName);
    }

    public BiMap<Short, String> getFeatureNamesByIndex() {
        return featureTable.getFeatureNameMap();
    }

}