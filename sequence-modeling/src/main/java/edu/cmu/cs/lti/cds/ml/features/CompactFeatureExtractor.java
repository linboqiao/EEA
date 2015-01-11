package edu.cmu.cs.lti.cds.ml.features;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.script.annotators.learn.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.ml.features.impl.MooneyFeature;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import edu.cmu.cs.lti.utils.BitUtils;
import edu.cmu.cs.lti.utils.TLongBasedFeatureTable;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TObjectIntMap;
import org.apache.commons.lang3.tuple.Triple;
import org.mapdb.Fun;

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
    TLongBasedFeatureTable featureTable;

    static Logger logger = Logger.getLogger(CompactFeatureExtractor.class.getName());

    private TObjectIntMap<TIntList> positiveObservations;
    private TObjectIntMap<String> headMap;

    private List<Feature> featureImpls;

    public CompactFeatureExtractor(TLongBasedFeatureTable featureTable, List<Feature> featureImpls) {
        this.featureTable = featureTable;
        this.positiveObservations = DataPool.cooccCountMaps;
        this.headMap = DataPool.headIdMap;
        this.featureImpls = featureImpls;
        for (Feature featureImpl : featureImpls) {
            logger.info("Feature registered: " + featureImpl.getClass().getSimpleName());
        }
        logger.info("Feature table lexical size: " + featureTable.getNumRows());
        logger.info("Feature table feature type size: " + featureTable.getFeatureNameMap().size());
    }

    public CompactFeatureExtractor(TLongBasedFeatureTable featureTable, String[] featureImplNames) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this(featureTable, featuresByName(featureImplNames));
    }

    public CompactFeatureExtractor(TLongBasedFeatureTable featureTable) {
        this(featureTable, defaultFeatures());
    }

    private static List<Feature> featuresByName(String[] featureImplNames) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        List<Feature> featureImpls = new ArrayList<>();
        for (String featureImplName : featureImplNames) {
            Class c = Class.forName(featureImplName);
            Feature f = (Feature) c.newInstance();
            featureImpls.add(f);
        }
        return featureImpls;
    }

    private static List<Feature> defaultFeatures() {
        List<Feature> featureImpls = new ArrayList<>();
        featureImpls.add(new MooneyFeature());
        return featureImpls;
    }

    public TLongShortDoubleHashTable getFeatures(List<ContextElement> chain, ContextElement targetMention, int index, int skipGramN, boolean breakOnConflict) {
        TLongShortDoubleHashTable extractedFeatures = new TLongShortDoubleHashTable();
        //ngram features
        for (Triple<ContextElement, ContextElement, Integer> ngram : getSkippedNgrams(chain, targetMention, index, skipGramN)) {
            Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> subsitutedForm = KarlMooneyScriptCounter.
                    firstBasedSubstitution(ngram.getLeft().getMention(), ngram.getMiddle().getMention());
            TIntLinkedList compactPair = FeatureExtractor.compactEvmPairSubstituiton(subsitutedForm, headMap);
            if (breakOnConflict && positiveObservations.containsKey(compactPair)) {
                return null;
            }
            extractFeatures(extractedFeatures, ngram.getLeft(), ngram.getMiddle(), ngram.getRight());
        }
        return extractedFeatures;
    }

    private void extractFeatures(TLongShortDoubleHashTable extractedFeatures, ContextElement elementLeft, ContextElement elementRight, int skip) {
        long predicatePair = getCompactPredicatePair(elementLeft.getMention().getMentionHead(), elementRight.getMention().getMentionHead());
        for (Feature featureImpl : featureImpls) {
            if (featureImpl.isLexicalized()) {
                for (Map.Entry<String, Double> f : featureImpl.getFeature(elementLeft, elementRight, skip).entrySet()) {
                    short featureIndex = featureTable.getOrPutFeatureIndex(f.getKey());
                    extractedFeatures.put(predicatePair, featureIndex, f.getValue());
                }
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
            count++;
            skipGrams.add(Triple.of(target, sequence.get(i), count));
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
            count++;
            skipGrams.add(Triple.of(sequence.get(i), target, count));
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