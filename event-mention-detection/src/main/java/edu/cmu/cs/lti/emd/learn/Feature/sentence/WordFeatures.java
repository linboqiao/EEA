package edu.cmu.cs.lti.emd.learn.feature.sentence;

import edu.cmu.cs.lti.emd.learn.feature.FeatureUtils;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.util.DependencyUtils;
import gnu.trove.map.TObjectDoubleMap;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 10:13 PM
 *
 * @author Zhengzhong Liu
 */
public class WordFeatures extends SentenceFeatureWithFocus {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static String outsideValue = "<OUTSIDE>";
    public static String startPlaceholder = "<START>";
    public static String endPlaceholder = "<END>";

    @Override
    public void extract(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features,
                        TObjectDoubleMap<String> featuresNeedForState) {
        addLabelFeatures(sentence, focus, features);
        addPosFeatures(sentence, focus, features);
        addLemmaFeatures(sentence, focus, features);
    }

    public String outsideProtection(List<StanfordCorenlpToken> sentence, Function<StanfordCorenlpToken, String>
            operator, int index) {
        if (index < -1) {
            return outsideValue;
        } else if (index > sentence.size()) {
            return outsideValue;
        }

        if (index == -1) {
            return startPlaceholder;
        }

        if (index == sentence.size()) {
            return endPlaceholder;
        }

        return operator.apply(sentence.get(index));
    }

    public void addPosFeatures(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features) {
        Function<StanfordCorenlpToken, String> operator = StanfordCorenlpToken::getPos;
        features.put(FeatureUtils.formatFeatureName(computeWordFeature(sentence, "Pos", operator, focus, 0)), 1);
        addWordFeatureWithOffsetRange(sentence, focus, -3, -1, "PosBefore", operator, features);
        addWordFeatureWithOffsetRange(sentence, focus, 1, 3, "PosAfter", operator, features);
    }

    public void addLemmaFeatures(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features) {
        Function<StanfordCorenlpToken, String> operator = StanfordCorenlpToken::getLemma;
        features.put(FeatureUtils.formatFeatureName(computeWordFeature(sentence, "Lemma", operator, focus, 0)), 1);
        addWordFeatureWithOffsetRange(sentence, focus, -3, -1, "LemmaBefore", operator, features);
        addWordFeatureWithOffsetRange(sentence, focus, 1, 3, "LemmaAfter", operator, features);
    }

    public void addLabelFeatures(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features) {
        Function<StanfordCorenlpToken, String> operator = DependencyUtils::getTokenParentDependency;
        features.put(FeatureUtils.formatFeatureName(computeWordFeature(sentence, "Label", operator, focus, 0)), 1);
        addWordFeatureWithOffsetRange(sentence, focus, -3, -1, "LabelBefore", operator, features);
        addWordFeatureWithOffsetRange(sentence, focus, 1, 3, "LabelAfter", operator, features);
    }

    public void addWordFeatureWithOffsetRange(List<StanfordCorenlpToken> sentence, int focus, int begin, int end,
                                              String prefix, Function<StanfordCorenlpToken, String> operator,
                                              TObjectDoubleMap<String> features) {
        IntStream.rangeClosed(begin, end)
                .mapToObj(offset -> computeWordFeature(sentence, prefix, operator, focus, offset))
                .filter(pair -> !pair.getValue1().equals(outsideValue))
                .forEach(featureTypeAndName -> features.put(FeatureUtils.formatFeatureName(featureTypeAndName), 1));
    }

    public Pair<String, String> computeWordFeature(List<StanfordCorenlpToken> sentence, String
            prefix, Function<StanfordCorenlpToken, String> operator, int focus, int offset) {
        return Pair.with(String.format("%s_i=%d", prefix, offset), outsideProtection(sentence, operator, focus +
                offset));
    }
}