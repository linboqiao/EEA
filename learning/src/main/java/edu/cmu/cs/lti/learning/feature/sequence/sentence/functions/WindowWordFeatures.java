package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MentionKey;
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
public class WindowWordFeatures extends SequenceFeatureWithFocus<StanfordCorenlpToken> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private  int posWindowSize;
    private  int lemmaWindowSize;
    private  int nerWindowSize;
    private  boolean useBigram;

    private  boolean useCoarsePos;
    private  boolean useFinePos;

    public WindowWordFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);

        posWindowSize = getIntFromConfig(featureConfig, "PosWindowSize", -1);
        lemmaWindowSize = getIntFromConfig(featureConfig, "LemmaWindowSize", -1);
        nerWindowSize = getIntFromConfig(featureConfig, "NerWindowSize", -1);
        useBigram = getBoolFromConfig(featureConfig, "Bigram", false);
        useCoarsePos = getBoolFromConfig(featureConfig, "Coarse", true);
        useFinePos = getBoolFromConfig(featureConfig, "Fine", true);
    }

    private int getIntFromConfig(Configuration config, String paramName, int defaultVal) {
        return config.getInt(this.getClass().getSimpleName() + "." + paramName, defaultVal);
    }

    private boolean getBoolFromConfig(Configuration config, String paramName, boolean defaultVal) {
        return config.getBoolean(this.getClass().getSimpleName() + "." + paramName, defaultVal);
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
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> nodeFeatures,
                        Table<org.apache.commons.lang3.tuple.Pair<Integer, Integer>, String, Double> edgeFeatures) {
        if (posWindowSize >= 0) {
            if (useFinePos) {
                addWindowFeatures(sequence, focus, nodeFeatures, StanfordCorenlpToken::getPos, "Pos", posWindowSize);
            }

            if (useCoarsePos) {
                addWindowFeatures(sequence, focus, nodeFeatures, this::getCoarsePOS, "CoarsePos", posWindowSize);
            }

//            // POS conjoined with previous state, we are conservative about window size here.
//            addWindowFeatures(sequence, focus, featuresNeedForState, StanfordCorenlpToken::getPos, "Pos", 0);
            if (useBigram) {
                if (useFinePos) {
                    addNgramFeatureWithOffsetRange(sequence, focus, -posWindowSize, posWindowSize, "Pos",
                            StanfordCorenlpToken::getPos, nodeFeatures, 2);
                }

                if (useCoarsePos) {
                    addNgramFeatureWithOffsetRange(sequence, focus, -posWindowSize, posWindowSize, "CoarsePos",
                            this::getCoarsePOS, nodeFeatures, 2);
                }
            }
        }
        if (lemmaWindowSize >= 0) {
            addWindowFeatures(sequence, focus, nodeFeatures, StanfordCorenlpToken::getLemma, "Lemma", lemmaWindowSize);
            if (useBigram) {
                addNgramFeatureWithOffsetRange(sequence, focus, -lemmaWindowSize, lemmaWindowSize, "Lemma",
                        StanfordCorenlpToken::getLemma, nodeFeatures, 2);
            }
        }
        if (nerWindowSize >= 0) {
            addWindowFeatures(sequence, focus, nodeFeatures, StanfordCorenlpToken::getNerTag, "Ner", nerWindowSize);
        }
    }

    private String getCoarsePOS(StanfordCorenlpToken token) {
        String pos = token.getPos();
        return pos.length() > 2 ? token.getPos().substring(0, 2) : pos;
    }

    @Override
    public void extractGlobal(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String>
            globalFeatures, List<MentionKey> knownStates, MentionKey currentState) {

    }

    public void addWindowFeatures(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features,
                                  Function<StanfordCorenlpToken, String> operator, String featureType, int windowSize) {
        putWithoutOutside(features, computeWordFeature(sentence, featureType, operator, focus, 0));

        addPositionFeatureWithOffsetRange(sentence, focus, -windowSize, -1, featureType, operator, features);
        addPositionFeatureWithOffsetRange(sentence, focus, 1, windowSize, featureType, operator, features);

        addWindowFeatureWithOffsetRange(sentence, focus, 1, windowSize, featureType, operator, features);
//        addWindowFeatureWithOffsetRange(sentence, focus, 1, windowSize, featureType, operator, features);
    }

    public void addPositionFeatureWithOffsetRange(List<StanfordCorenlpToken> sentence, int focus, int begin, int end,
                                                  String prefix, Function<StanfordCorenlpToken, String> operator,
                                                  TObjectDoubleMap<String> features) {
        IntStream.rangeClosed(begin, end)
                .mapToObj(offset -> computeWordFeature(sentence, prefix, operator, focus, offset))
                .forEach(featureTypeAndName -> putWithoutOutside(features, featureTypeAndName));
    }

    public void addWindowFeatureWithOffsetRange(List<StanfordCorenlpToken> sentence, int focus,
                                                int limitStart, int limitEnd, String prefix,
                                                Function<StanfordCorenlpToken, String> operator,
                                                TObjectDoubleMap<String> features) {
        IntStream.rangeClosed(limitStart, limitEnd).forEach(windowLimit -> {
            IntStream.rangeClosed(1, windowLimit)
                    .mapToObj(offset -> computeWindowWordFeature(sentence, prefix, operator, windowLimit, focus,
                            offset))
                    .forEach(featureTypeAndName -> putWithoutOutside(features, featureTypeAndName));
            IntStream.rangeClosed(-windowLimit, -1)
                    .mapToObj(offset -> computeWindowWordFeature(sentence, prefix, operator, windowLimit, focus,
                            offset))
                    .forEach(featureTypeAndName -> putWithoutOutside(features, featureTypeAndName));
        });
    }

    public void addNgramFeatureWithOffsetRange(List<StanfordCorenlpToken> sentence, int focus, int begin, int end,
                                               String prefix, Function<StanfordCorenlpToken, String> operator,
                                               TObjectDoubleMap<String> features, int n) {
        int left = Math.min(focus + begin, -1);
        int right = Math.max(focus + end, sentence.size());

        int[] runners = new int[n];
        for (int i = 0; i < n; i++) {
            runners[i] = left + i;
            if (runners[i] > right) {
                return;
            }
        }

        String ngramPrefix = "window_" + n + "gram" + prefix;

        while (true) {
            StringBuilder sb = new StringBuilder();
            String ngramSep = "";
            for (int i = 0; i < runners.length; i++) {
                sb.append(ngramSep);
                ngramSep = "_";
                // Operate with outside will ensure we get a feature value. However, we do not allow outside here, this
                // function will simply create <outside> for tokens without NER.
                String val = operateWithOutsideLowerCase(sentence, operator, runners[i]);
                sb.append(val);
                runners[i]++;
                if (runners[i] > right) {
                    return;
                }
            }
            addToFeatures(features, FeatureUtils.formatFeatureName(ngramPrefix, sb.toString()), 1);
        }
    }

    public void putWithoutOutside(TObjectDoubleMap<String> features, Pair<String, String> featureTypeAndName) {
        if (!featureTypeAndName.getValue1().equals(outsideValue)) {
            addToFeatures(features, FeatureUtils.formatFeatureName(featureTypeAndName), 1);
        }
    }

    public Pair<String, String> computeWordFeature(List<StanfordCorenlpToken> sentence, String
            prefix, Function<StanfordCorenlpToken, String> operator, int focus, int offset) {
        // NOTE all word features are lowercased.
        return Pair.with(String.format("%s_offset=%d", prefix, offset), operateWithOutsideLowerCase(sentence, operator,
                focus + offset));
    }

    public Pair<String, String> computeWindowWordFeature(List<StanfordCorenlpToken> sentence, String
            prefix, Function<StanfordCorenlpToken, String> operator, int windowSize, int focus, int offset) {
        // NOTE all word features are lowercased.
        return Pair.with(String.format("%s_window=%d", prefix, windowSize), operateWithOutsideLowerCase(sentence,
                operator, focus + offset));
    }
}