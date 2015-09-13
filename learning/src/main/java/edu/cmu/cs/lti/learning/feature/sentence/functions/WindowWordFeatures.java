package edu.cmu.cs.lti.learning.feature.sentence.functions;

import edu.cmu.cs.lti.learning.feature.sentence.FeatureUtils;
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
public class WindowWordFeatures extends SequenceFeatureWithFocus {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int posWindowSize;
    private int lemmaWindowSize;
    private int nerWindowSize;

    public WindowWordFeatures(Configuration config) {
        super(config);
        posWindowSize = getParamFromConfig(config, "posWindowSize", -1);
        lemmaWindowSize = getParamFromConfig(config, "lemmaWindowSize", -1);
        nerWindowSize = getParamFromConfig(config, "nerWindowSize", -1);
    }

    private int getParamFromConfig(Configuration config, String paramName, int defaultVal) {
        return config.getInt(this.getClass().getSimpleName() + "." + paramName, defaultVal);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        // Set types to each token for easy feature extraction.
        for (StanfordEntityMention mention : JCasUtil.select(context, StanfordEntityMention.class)) {
            String entityType = mention.getEntityType();
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
                token.setNerTag(entityType);
            }
        }
    }

    @Override
    public void resetWorkspace(JCas aJCas, int begin, int end) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> features,
                        TObjectDoubleMap<String> featuresNeedForState) {
        if (posWindowSize > 0) {
            addWindowFeatures(sequence, focus, features, StanfordCorenlpToken::getPos, "Pos", posWindowSize);
        }
        if (lemmaWindowSize > 0) {
            addWindowFeatures(sequence, focus, features, StanfordCorenlpToken::getLemma, "Lemma", lemmaWindowSize);
        }
        if (nerWindowSize > 0) {
            addWindowFeatures(sequence, focus, features, StanfordCorenlpToken::getNerTag, "Ner", nerWindowSize);
        }
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
        return Pair.with(String.format("%s_i=%d", prefix, offset), operateWithOutside(sentence, operator, focus +
                offset));
    }
}