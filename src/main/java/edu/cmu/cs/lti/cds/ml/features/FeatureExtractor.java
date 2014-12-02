package edu.cmu.cs.lti.cds.ml.features;

import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.model.ContextElement;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.cds.utils.DataPool;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.mapdb.Fun;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/2/14
 * Time: 4:47 PM
 */
public class FeatureExtractor {
    private TObjectIntMap<TIntList> positiveObservations;
    private TObjectIntMap<String> headMap;

    public FeatureExtractor() {
        this.positiveObservations = DataPool.cooccCountMaps;
        this.headMap = DataPool.headIdMap;
    }

    public FeatureExtractor(TObjectIntMap<TIntList> cooccCounts, TObjectIntMap<String> headMap) {
        this.positiveObservations = cooccCounts;
        this.headMap = headMap;
    }

    public TObjectDoubleMap<String> getFeatures(List<ContextElement> chain, ContextElement targetMention, int index, int skipGramN, boolean breakOnConflict) {
        TObjectDoubleMap<String> allFeatures = new TObjectDoubleHashMap<>();
        //ngram features
        for (Pair<ContextElement, ContextElement> ngram : getSkippedNgrams(chain, targetMention, index, skipGramN)) {
            Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> subsitutedForm = KarlMooneyScriptCounter.
                    firstBasedSubstitution(ngram.getLeft().getMention(), ngram.getRight().getMention());
            TIntLinkedList compactPair = compactEvmPairSubstituiton(subsitutedForm, headMap);

            if (breakOnConflict && positiveObservations.containsKey(compactPair)) {
                return null;
            }

            allFeatures.put("m_" + connectTuple(subsitutedForm.a) + "_" + connectTuple(subsitutedForm.b), 1);
        }
        return allFeatures;
    }

    public <T extends Object> List<Pair<T, T>> getSkippedNgrams(List<T> sequence, T target, int index, int skipgramN) {
        List<Pair<T, T>> formerPairs = getFormerSkipGram(sequence, target, index, skipgramN);
        List<Pair<T, T>> latterPairs = getLatterSkipGram(sequence, target, index, skipgramN);

        List<Pair<T, T>> allPairs = new ArrayList<>();
        allPairs.addAll(formerPairs);
        allPairs.addAll(latterPairs);

        return allPairs;
    }

    private String connectMooneyTuple(MooneyEventRepre rep) {
        return rep.getPredicate() + "_" + rep.getArg0() + "_" + rep.getArg1() + "_" + +rep.getArg2();
    }

    private String connectTuple(Fun.Tuple4<String, Integer, Integer, Integer> t) {
        return t.a + "_" + t.b + "_" + t.c + "_" + t.d;
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


    public static TIntLinkedList compactEvmPairSubstituiton(Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> evmPair,
                                                            TObjectIntMap<String> headMap) {
        TIntLinkedList compactRep = new TIntLinkedList();

        compactRep.add(headMap.get(evmPair.a.a));
        compactRep.add(evmPair.a.b);
        compactRep.add(evmPair.a.c);
        compactRep.add(evmPair.a.d);

        compactRep.add(headMap.get(evmPair.b.a));
        compactRep.add(evmPair.b.b);
        compactRep.add(evmPair.b.c);
        compactRep.add(evmPair.b.d);

        return compactRep;
    }
}