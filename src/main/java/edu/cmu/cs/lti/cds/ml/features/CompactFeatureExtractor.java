package edu.cmu.cs.lti.cds.ml.features;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
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
import java.util.Arrays;
import java.util.List;

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

    public CompactFeatureExtractor(TLongBasedFeatureTable featureTable) {
        this.featureTable = featureTable;
        this.positiveObservations = DataPool.cooccCountMaps;
        this.headMap = DataPool.headIdMap;
    }

    public TLongShortDoubleHashTable getFeatures(List<ChainElement> chain, ChainElement targetMention, int index, int skipGramN, boolean breakOnConflict) {
        TLongShortDoubleHashTable featureTable = new TLongShortDoubleHashTable();

        //ngram features
        for (Pair<ChainElement, ChainElement> ngram : getSkippedNgrams(chain, targetMention, index, skipGramN)) {
            Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> subsitutedForm = KarlMooneyScriptCounter.
                    firstBasedSubstitution(ngram.getLeft().getMention(), ngram.getRight().getMention());

            TIntLinkedList compactPair = FeatureExtractor.compactEvmPairSubstituiton(subsitutedForm, headMap);
            getMooneyLikeFeatures(featureTable, subsitutedForm.a.a, subsitutedForm.b.a, getLast3IntFromTuple(subsitutedForm.a), getLast3IntFromTuple(subsitutedForm.b));

            if (breakOnConflict && positiveObservations.containsKey(compactPair)) {
                return null;
            }
        }
        return featureTable;
    }

    private void getMooneyLikeFeatures(TLongShortDoubleHashTable featureTable, String p1, String p2, int[] arg1s, int[] arg2s) {
        long predicatePair = getCompactPredicatePair(p1, p2);
        //mooney features
        featureTable.put(predicatePair, getMooneyArgumentFeature(arg1s, arg2s), 1);

        //regular argument features
//        for (short f : getRegularArgumentFeatures(arg1s, arg2s)) {
//            featureTable.put(predicatePair, f, 1);
//        }
    }

    private short getMooneyArgumentFeature(int[] args1, int[] args2) {
        String featureName = "m_arg" + "_" + asArgumentStr(args1) + "_" + asArgumentStr(args2);
        return featureTable.getOrPutFeatureIndex(featureName);
    }

    private int[] getLast3IntFromTuple(Fun.Tuple4<String, Integer, Integer, Integer> t) {
        int[] a = new int[3];
        a[0] = t.b;
        a[1] = t.c;
        a[2] = t.d;
        return a;
    }

    private String asArgumentStr(int[] args) {
        List<String> argList = Arrays.asList(Arrays.toString(args));
        return Joiner.on("_").join(argList);
    }

    public <T extends Object> List<Pair<T, T>> getSkippedNgrams(List<T> sequence, T target, int index, int skipgramN) {
        List<Pair<T, T>> formerPairs = getFormerSkipGram(sequence, target, index, skipgramN);
        List<Pair<T, T>> latterPairs = getLatterSkipGram(sequence, target, index, skipgramN);

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

    public <T extends Object> List<Pair<T, T>> getFormerSkipGram(List<T> sequence, T target, int index, int skipgramN) {
        List<Pair<T, T>> skipGrams = new ArrayList<>();

        int count = 0;
        for (int i = index + 1; i < sequence.size(); i++) {
            skipGrams.add(Pair.of(target, sequence.get(i)));
            count++;
            if (count > skipgramN) {
                break;
            }
        }
        return skipGrams;
    }

    private <T extends Object> List<Pair<T, T>> getLatterSkipGram(List<T> mentions,
                                                                  T targetMention, int index, int k) {
        List<Pair<T, T>> skipGrams = new ArrayList<>();

        int count = 0;
        for (int i = index - 1; i > 0; i--) {
            skipGrams.add(Pair.of(targetMention, mentions.get(i)));
            count++;
            if (count > k) {
                break;
            }
        }
        return skipGrams;
    }
}