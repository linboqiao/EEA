package edu.cmu.cs.lti.cds.ml.features;

import com.google.common.collect.BiMap;
import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.ml.features.impl.MooneyFeature;
import edu.cmu.cs.lti.cds.model.ChainElement;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import edu.cmu.cs.lti.utils.BitUtils;
import edu.cmu.cs.lti.utils.TLongBasedFeatureTable;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TObjectIntMap;
import org.apache.commons.lang3.tuple.Pair;
import org.mapdb.Fun;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/2/14
 * Time: 4:47 PM
 */
public class CompactFeatureExtractor {
    TLongBasedFeatureTable featureTable;

    private TObjectIntMap<TIntList> positiveObservations;
    private TObjectIntMap<String> headMap;

    private List<Feature> featureImpls;

    public CompactFeatureExtractor(TLongBasedFeatureTable featureTable, List<Feature> featureImpls) {
        this.featureTable = featureTable;
        this.positiveObservations = DataPool.cooccCountMaps;
        this.headMap = DataPool.headIdMap;
        this.featureImpls = featureImpls;
    }

    public CompactFeatureExtractor(TLongBasedFeatureTable featureTable) {
        this(featureTable, defaultFeatures());
    }

    private static List<Feature> defaultFeatures() {
        List<Feature> featureImpls = new ArrayList<>();
        featureImpls.add(new MooneyFeature());
//        featureImpls.add(new ArgumentCorefFeature());
        return featureImpls;
    }

    public TLongShortDoubleHashTable getFeatures(List<ChainElement> chain, ChainElement targetMention, int index, int skipGramN, boolean breakOnConflict) {
        TLongShortDoubleHashTable extractedFeatures = new TLongShortDoubleHashTable();
        //ngram features
        for (Pair<ChainElement, ChainElement> ngram : getSkippedNgrams(chain, targetMention, index, skipGramN)) {
            Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> subsitutedForm = KarlMooneyScriptCounter.
                    firstBasedSubstitution(ngram.getLeft().getMention(), ngram.getRight().getMention());

            TIntLinkedList compactPair = FeatureExtractor.compactEvmPairSubstituiton(subsitutedForm, headMap);
            extractFeatures(extractedFeatures, ngram.getLeft(), ngram.getRight());
            if (breakOnConflict && positiveObservations.containsKey(compactPair)) {
                return null;
            }
        }
        return extractedFeatures;
    }

    private void extractFeatures(TLongShortDoubleHashTable extractedFeatures, ChainElement elementLeft, ChainElement elementRight) {
//        Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> subsitutedForm = KarlMooneyScriptCounter.
//                firstBasedSubstitution(elementLeft.getMention(), elementRight.getMention());
//        String p1 = subsitutedForm.a.a;
//        String p2 = subsitutedForm.b.a;
//
//        int[] arg1s = getLast3IntFromTuple(subsitutedForm.a);
//        int[] arg2s = getLast3IntFromTuple(subsitutedForm.b);
//
        long predicatePair = getCompactPredicatePair(elementLeft.getMention().getMentionHead(), elementRight.getMention().getMentionHead());
//
//        extractedFeatures.put(predicatePair, getMooneyArgumentFeature(arg1s, arg2s), 1);

        for (Feature featureImpl : featureImpls) {
            if (featureImpl.isLexicalized()) {
                for (Map.Entry<String, Double> f : featureImpl.getFeature(elementLeft, elementRight).entrySet()) {
                    short featureIndex = featureTable.getOrPutFeatureIndex(f.getKey());
                    extractedFeatures.put(predicatePair, featureIndex, f.getValue());
                }
            }
        }
    }

//    private short getMooneyArgumentFeature(int[] args1, int[] args2) {
//        String featureName = "m_arg" + "_" + asArgumentStr(args1) + "_" + asArgumentStr(args2);
//        return featureTable.getOrPutFeatureIndex(featureName);
//    }
//
//    private short[] getRegularArgumentFeatures(int[] args1, int[] args2) {
//        TShortArrayList regularArgumentFeatures = new TShortArrayList();
//        for (int slotId1 = 0; slotId1 < args1.length; slotId1++) {
//            int slotId1Eid = args1[slotId1];
//            if (slotId1Eid == -1 || slotId1Eid == 0) {
//                continue;
//            }
//            for (int slotId2 = 0; slotId2 < args2.length; slotId2++) {
//                int slotId2Eid = args2[slotId2];
//                if (slotId2Eid == -1 || slotId2Eid == 0) {
//                    continue;
//                }
//
//                if (slotId1Eid == slotId2Eid) {
//                    String featureName = "r_arg" + "_" + slotId1 + "_" + slotId2;
//                    short fIndex = featureTable.getOrPutFeatureIndex(featureName);
//                    regularArgumentFeatures.add(fIndex);
//                }
//            }
//        }
//        return regularArgumentFeatures.toArray();
//    }
//
//    private int[] getLast3IntFromTuple(Fun.Tuple4<String, Integer, Integer, Integer> t) {
//        int[] a = new int[3];
//        a[0] = t.b;
//        a[1] = t.c;
//        a[2] = t.d;
//        return a;
//    }
//
//    private String asArgumentStr(int[] args) {
//        List<String> argList = Arrays.asList(Arrays.toString(args));
//        return Joiner.on("_").join(argList);
//    }

    public <T extends Object> List<Pair<T, T>> getSkippedNgrams(List<T> sequence, T target, int index, int skipgramN) {
        List<Pair<T, T>> formerPairs = getSkipGramBefore(sequence, target, index, skipgramN);
        List<Pair<T, T>> latterPairs = getSkipGramsAfter(sequence, target, index, skipgramN);

        List<Pair<T, T>> allPairs = new ArrayList<>();
        allPairs.addAll(formerPairs);
        allPairs.addAll(latterPairs);

        return allPairs;
    }

    private long getCompactPredicatePair(String word1, String word2) {
        int pre1 = DataPool.headIdMap.get(word1);
        int pre2 = DataPool.headIdMap.get(word2);

        return BitUtils.store2Int(pre1, pre2);
    }

    public <T extends Object> List<Pair<T, T>> getSkipGramsAfter(List<T> sequence, T target, int index, int k) {
        List<Pair<T, T>> skipGrams = new ArrayList<>();

        int count = 0;
        for (int i = index + 1; i < sequence.size(); i++) {
            skipGrams.add(Pair.of(target, sequence.get(i)));
            count++;
            if (count > k) {
                break;
            }
        }
        return skipGrams;
    }

    private <T extends Object> List<Pair<T, T>> getSkipGramBefore(List<T> sequence, T target, int index, int k) {
        List<Pair<T, T>> skipGrams = new ArrayList<>();

        int count = 0;
        for (int i = index - 1; i > 0; i--) {
            skipGrams.add(Pair.of(sequence.get(i), target));
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