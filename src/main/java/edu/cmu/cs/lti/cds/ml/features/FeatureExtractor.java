package edu.cmu.cs.lti.cds.ml.features;

import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.model.ChainElement;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.cds.runners.script.test.KarlMooneyPredictor;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import gnu.trove.map.TObjectDoubleMap;
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
    public TObjectDoubleMap<String> getFeaturesFromMooneyStyleEvents(List<MooneyEventRepre> chain, MooneyEventRepre targetMention, int index, int skipGramN) {
        TObjectDoubleMap<String> allFeatures = new TObjectDoubleHashMap<>();
        //ngram features
        List<Pair<MooneyEventRepre, MooneyEventRepre>> ngrams = getSkippedNgrams(chain, targetMention, index, skipGramN);
        for (Pair<MooneyEventRepre, MooneyEventRepre> ngram : ngrams) {
            Pair<MooneyEventRepre, MooneyEventRepre> subsibutedForm = KarlMooneyPredictor.formerBasedTransform(ngram.getKey(), ngram.getValue());
            allFeatures.put("m_" + connectMooneyTuple(subsibutedForm.getLeft()) + "_" + connectMooneyTuple(subsibutedForm.getRight()), 1);
        }
        return allFeatures;
    }

    public TObjectDoubleMap<String> getFeatures(List<ChainElement> chain, TokenAlignmentHelper align, ChainElement targetMention, int index, int skipGramN) {
        TObjectDoubleMap<String> allFeatures = new TObjectDoubleHashMap<>();
        //ngram features
        for (Pair<ChainElement, ChainElement> ngram : getSkippedNgrams(chain, targetMention, index, skipGramN)) {
            Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> subsitutedForm = KarlMooneyScriptCounter.
                    firstBasedSubstitution(ngram.getLeft().getMention(), ngram.getRight().getMention());
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
}