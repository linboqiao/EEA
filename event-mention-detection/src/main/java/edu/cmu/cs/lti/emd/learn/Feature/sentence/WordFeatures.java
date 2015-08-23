package edu.cmu.cs.lti.emd.learn.feature.sentence;

import edu.cmu.cs.lti.emd.learn.feature.FeatureUtils;
import edu.cmu.cs.lti.learning.model.HashedFeatureVector;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.util.DependencyUtils;
import org.javatuples.Pair;

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
    public static String outsideValue = "<OUTSIDE>";
    public static String startPlaceholder = "<START>";
    public static String endPlaceholder = "<END>";

    @Override
    public void extract(HashedFeatureVector fv, List<StanfordCorenlpToken> sentence, int focus, int
            previousStateValue) {
        addLemmaFeatures(fv, sentence, focus);
        addPosFeatures(fv, sentence, focus);
        addLabelFeatures(fv, sentence, focus);
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

    public void addPosFeatures(HashedFeatureVector fv, List<StanfordCorenlpToken> sentence, int focus) {
        Function<StanfordCorenlpToken, String> operator = StanfordCorenlpToken::getPos;
        fv.addFeature(FeatureUtils.formatFeatureName(computeWordFeature(sentence, "Pos", operator, focus)), 1);
        addWordFeatureWithOffsetRange(fv, sentence, focus, -1, -3, "PosBefore", operator);
        addWordFeatureWithOffsetRange(fv, sentence, focus, 1, 3, "PosAfter", operator);
    }

    public void addLemmaFeatures(HashedFeatureVector fv, List<StanfordCorenlpToken> sentence, int focus) {
        Function<StanfordCorenlpToken, String> operator = StanfordCorenlpToken::getLemma;
        fv.addFeature(FeatureUtils.formatFeatureName(computeWordFeature(sentence, "Lemma", operator, focus)), 1);
        addWordFeatureWithOffsetRange(fv, sentence, focus, -1, -3, "LemmaBefore", operator);
        addWordFeatureWithOffsetRange(fv, sentence, focus, 1, 3, "LemmaAfter", operator);
    }

    public void addLabelFeatures(HashedFeatureVector fv, List<StanfordCorenlpToken> sentence, int focus) {
        Function<StanfordCorenlpToken, String> operator = DependencyUtils::getTokenParentDependency;
        fv.addFeature(FeatureUtils.formatFeatureName(computeWordFeature(sentence, "Label", operator, focus)), 1);
        addWordFeatureWithOffsetRange(fv, sentence, focus, -1, -3, "LabelBefore", operator);
        addWordFeatureWithOffsetRange(fv, sentence, focus, 1, 3, "LabelAfter", operator);
    }

    public void addWordFeatureWithOffsetRange(HashedFeatureVector fv, List<StanfordCorenlpToken> sentence, int focus,
                                              int begin, int end, String prefix,
                                              Function<StanfordCorenlpToken, String> operator) {
        IntStream.rangeClosed(begin, end)
                .mapToObj(offset -> computeWordFeature(sentence, prefix, operator, focus + offset))
                .filter(pair -> pair.getValue1().equals(outsideValue))
                .forEach(featureTypeAndName -> fv.addFeature(FeatureUtils.formatFeatureName(featureTypeAndName), 1));
    }

    public Pair<String, String> computeWordFeature(List<StanfordCorenlpToken> sentence, String
            prefix, Function<StanfordCorenlpToken, String> operator, int index) {
        return Pair.with(String.format("%s_%d", prefix, index), outsideProtection(sentence, operator, index));
    }
}
