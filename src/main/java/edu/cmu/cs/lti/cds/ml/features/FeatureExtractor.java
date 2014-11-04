package edu.cmu.cs.lti.cds.ml.features;

import edu.cmu.cs.lti.cds.annotators.script.karlmooney.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.model.ChainElement;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/2/14
 * Time: 4:47 PM
 */
public class FeatureExtractor {
    public TObjectDoubleMap<String> getFeatures(List<ChainElement> chain, TokenAlignmentHelper align, ChainElement targetMention, int index, int skipGramN) {
        TObjectDoubleMap<String> allFeatures = new TObjectDoubleHashMap<>();
        //ngram features
        allFeatures.putAll(ngramFeatures(chain, align, targetMention, index, skipGramN));
        return allFeatures;
    }

    public TObjectDoubleMap<String> ngramFeatures(List<ChainElement> mentions, TokenAlignmentHelper align,
                                                  ChainElement targetMention, int index, int skipgramN) {
        List<Pair<ChainElement, ChainElement>> formerPairs = getFormerSkipGram(mentions, targetMention, index, skipgramN);
        List<Pair<ChainElement, ChainElement>> latterPairs = getLatterSkipGram(mentions, targetMention, index, skipgramN);

        List<Pair<ChainElement, ChainElement>> allPairs = new ArrayList<>();
        allPairs.addAll(formerPairs);
        allPairs.addAll(latterPairs);

        TObjectDoubleMap<String> features = new TObjectDoubleHashMap<>();

        for (String mf : mooneyFeatures(allPairs, align)) {
            features.put(mf, 1);
        }

        return features;
    }

    private List<String> mooneyFeatures(List<Pair<ChainElement, ChainElement>> skipgrams, TokenAlignmentHelper align) {
        List<String> features = new ArrayList<>();
        for (Pair<ChainElement, ChainElement> skipgram : skipgrams) {
            String featureName = "mooney_" + KarlMooneyScriptCounter.
                    firstBasedSubstitution(align, skipgram.getLeft().getMention(), skipgram.getRight().getMention());
            features.add(featureName);
        }

        return features;
    }

    private List<Pair<ChainElement, ChainElement>> getFormerSkipGram(List<ChainElement> mentions,
                                                                     ChainElement targetMention, int index, int k) {
        List<Pair<ChainElement, ChainElement>> skipGrams = new ArrayList<>();
        int count = 0;
        for (int i = index + 1; i < mentions.size(); i++) {
            skipGrams.add(Pair.of(targetMention, mentions.get(i)));
            count++;
            if (count >= k) {
                break;
            }
        }

        return skipGrams;
    }

    private List<Pair<ChainElement, ChainElement>> getLatterSkipGram(List<ChainElement> mentions,
                                                                     ChainElement targetMention, int index, int k) {
        List<Pair<ChainElement, ChainElement>> skipGrams = new ArrayList<>();

        int count = 0;
        for (int i = index - 1; i > 0; i--) {
            skipGrams.add(Pair.of(targetMention, mentions.get(i)));
            count++;
            if (count >= k) {
                break;
            }
        }
        return skipGrams;
    }
}