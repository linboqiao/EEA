package edu.cmu.cs.lti.emd.learn.feature.sentence;

import edu.cmu.cs.lti.emd.learn.feature.FeatureUtils;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
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
public class WindowWordFeatures extends SentenceFeatureWithFocus {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static String outsideValue = "<OUTSIDE>";
    public static String startPlaceholder = "<START>";
    public static String endPlaceholder = "<END>";

    public WindowWordFeatures(Configuration config) {
        super(config);
    }

    @Override
    public void initWorkspace(JCas context) {
        // Set types to each token for easy feature extraction.
        for (StanfordEntityMention mention : JCasUtil.select(context, StanfordEntityMention.class)) {
            String entityType = mention.getEntityType();
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
                token.setNerTag(entityType);
            }
        }
    }

    @Override
    public void resetWorkspace(StanfordCorenlpSentence sentence) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features,
                        TObjectDoubleMap<String> featuresNeedForState) {
        addWindowFeatures(sentence, focus, features, StanfordCorenlpToken::getPos, "Pos", 1);
        addWindowFeatures(sentence, focus, features, StanfordCorenlpToken::getLemma, "Lemma", 3);
        addWindowFeatures(sentence, focus, features, StanfordCorenlpToken::getNerTag, "Ner", 3);
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

        String operatedValue = operator.apply(sentence.get(index));

        return operatedValue == null ? outsideValue : operatedValue;
    }

    public void addWindowFeatures(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features,
                                  Function<StanfordCorenlpToken, String> operator, String featureType, int windowSize) {
        putWithoutOutside(features, computeWordFeature(sentence, featureType, operator, focus, 0));
        addWordFeatureWithOffsetRange(sentence, focus, -windowSize, -1, featureType, operator, features);
        addWordFeatureWithOffsetRange(sentence, focus, 1, windowSize, featureType, operator, features);
    }

    public void addWordFeatureWithOffsetRange(List<StanfordCorenlpToken> sentence, int focus, int begin, int end,
                                              String prefix, Function<StanfordCorenlpToken, String> operator,
                                              TObjectDoubleMap<String> features) {
        IntStream.rangeClosed(begin, end)
                .mapToObj(offset -> computeWordFeature(sentence, prefix, operator, focus, offset))
                .forEach(featureTypeAndName -> putWithoutOutside(features, featureTypeAndName));
    }

    public void putWithoutOutside(TObjectDoubleMap<String> features, Pair<String, String> featureTypeAndName) {
        if (!featureTypeAndName.getValue1().equals(outsideValue)) {
            features.put(FeatureUtils.formatFeatureName(featureTypeAndName), 1);
        }
    }

    public Pair<String, String> computeWordFeature(List<StanfordCorenlpToken> sentence, String
            prefix, Function<StanfordCorenlpToken, String> operator, int focus, int offset) {
        return Pair.with(String.format("%s_i=%d", prefix, offset), outsideProtection(sentence, operator, focus +
                offset));
    }
}