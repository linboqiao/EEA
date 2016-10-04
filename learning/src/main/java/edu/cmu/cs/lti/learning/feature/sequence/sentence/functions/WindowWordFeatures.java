package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.ResourceUtils;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
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

    private int posWindowSize;
    private int lemmaWindowSize;
    private int nerWindowSize;
    private int lemmaPosWindowSize;
    private int posBigramWindow;
    private int lemmaBigramWindow;

    private boolean useCoarsePos;
    private boolean useFinePos;

    private boolean useHeadCilin;

    private Map<String, String> word2CilinId;

    private Set<String> puncPos;

    public WindowWordFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);

        posWindowSize = getIntFromConfig(featureConfig, "PosWindow", -1);
        lemmaWindowSize = getIntFromConfig(featureConfig, "LemmaWindow", -1);
        lemmaPosWindowSize = getIntFromConfig(featureConfig, "LemmaPosWindow", -1);
        nerWindowSize = getIntFromConfig(featureConfig, "NerWindow", -1);
        posBigramWindow = getIntFromConfig(featureConfig, "PosBigramWindow", -1);
        lemmaBigramWindow = getIntFromConfig(featureConfig, "LemmaBigramWindow", -1);
        useCoarsePos = getBoolFromConfig(featureConfig, "Coarse", true);
        useFinePos = getBoolFromConfig(featureConfig, "Fine", true);
        useHeadCilin = getBoolFromConfig(featureConfig, "HeadCiLin", false);

        if (useHeadCilin) {
            word2CilinId = ResourceUtils.readCilin(new File(generalConfig.get("edu.cmu.cs.lti.resource.dir"),
                    generalConfig.get("edu.cmu.cs.lti.synonym.cilin")));
        }

        puncPos = new HashSet<>();
        puncPos.add("pu");
        puncPos.add(".");
        puncPos.add("\"");
    }

    private int getIntFromConfig(Configuration config, String paramName, int defaultVal) {
        return config.getInt(featureConfigKey(paramName), defaultVal);
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
                addOffsetFeatures(sequence, focus, nodeFeatures, StanfordCorenlpToken::getPos,
                        this::surroundingPunctFilter, "Pos", posWindowSize);
                addWindowFeatures(sequence, focus, nodeFeatures, StanfordCorenlpToken::getPos,
                        this::surroundingPunctFilter, "Pos", posWindowSize, false);
            }

            if (useCoarsePos) {
                addOffsetFeatures(sequence, focus, nodeFeatures, this::getCoarsePOS, this::surroundingPunctFilter,
                        "CoarsePos", posWindowSize);
                addWindowFeatures(sequence, focus, nodeFeatures, this::getCoarsePOS, this::surroundingPunctFilter,
                        "CoarsePos", posWindowSize, false);
            }
        }

        if (posBigramWindow > 0) {
            if (useFinePos) {
                addNgramFeatureWithOffsetRange(sequence, focus, -posWindowSize, posWindowSize, "Pos",
                        StanfordCorenlpToken::getPos, nodeFeatures, 2);
            }

            if (useCoarsePos) {
                addNgramFeatureWithOffsetRange(sequence, focus, -posWindowSize, posWindowSize, "CoarsePos",
                        this::getCoarsePOS, nodeFeatures, 2);
            }
        }

        if (lemmaBigramWindow > 0) {
            addNgramFeatureWithOffsetRange(sequence, focus, -lemmaBigramWindow, lemmaBigramWindow, "Lemma_",
                    StanfordCorenlpToken::getLemma, nodeFeatures, 2);
        }

        if (lemmaWindowSize >= 0) {
            addOffsetFeatures(sequence, focus, nodeFeatures, StanfordCorenlpToken::getLemma,
                    this::surroundingPunctFilter, "Lemma", lemmaWindowSize);
//            addWindowFeatures(sequence, focus, nodeFeatures, StanfordCorenlpToken::getLemma,
//                    this::surroundingPunctFilter, "Lemma", lemmaWindowSize, true);
            addWindowFeatures(sequence, focus, nodeFeatures, StanfordCorenlpToken::getLemma,
                    this::surroundingPunctFilter, "Lemma", lemmaWindowSize, false);
        }

        if (nerWindowSize >= 0) {
            addOffsetFeatures(sequence, focus, nodeFeatures, StanfordCorenlpToken::getNerTag,
                    this::indexFilter, "Ner", nerWindowSize);
            addWindowFeatures(sequence, focus, nodeFeatures, StanfordCorenlpToken::getNerTag, this::indexFilter,
                    "Ner", nerWindowSize, false);
        }

        if (lemmaPosWindowSize >= 0) {
            if (useFinePos) {
                addOffsetFeatures(sequence, focus, nodeFeatures, this::getLemmaAndPos, this::surroundingPunctFilter,
                        "LemmaPos", lemmaPosWindowSize);
//                addWindowFeatures(sequence, focus, nodeFeatures, this::getLemmaAndPos, this::surroundingPunctFilter,
//                        "LemmaPos", lemmaPosWindowSize, true);
                addWindowFeatures(sequence, focus, nodeFeatures, this::getLemmaAndPos, this::surroundingPunctFilter,
                        "LemmaPos", lemmaPosWindowSize, false);
            }
            if (useCoarsePos) {
                addOffsetFeatures(sequence, focus, nodeFeatures, this::getLemmaAndCoarsePos,
                        this::surroundingPunctFilter, "LemmaPos", lemmaPosWindowSize);
//                addWindowFeatures(sequence, focus, nodeFeatures, this::getLemmaAndCoarsePos,
//                        this::surroundingPunctFilter, "LemmaPos", lemmaPosWindowSize, true);
                addWindowFeatures(sequence, focus, nodeFeatures, this::getLemmaAndCoarsePos,
                        this::surroundingPunctFilter, "LemmaPos", lemmaPosWindowSize, false);
            }
        }

        if (useHeadCilin) {
            if (focus > 0 && focus < sequence.size()) {
                String lemma = sequence.get(focus).getLemma().toLowerCase();
                if (word2CilinId.containsKey(lemma)) {
                    String entryId = word2CilinId.get(lemma);
                    addToFeatures(nodeFeatures, FeatureUtils.formatFeatureName("CilinEntryId", entryId), 1);
                }
            }
        }
    }

    private boolean indexFilter(List<StanfordCorenlpToken> tokens, int index) {
        return index >= 0 && index < tokens.size();
    }

    private boolean surroundingPunctFilter(List<StanfordCorenlpToken> tokens, int index) {
        return indexFilter(tokens, index) && !puncPos.contains(tokens.get(index).getPos().toLowerCase());
    }

    private String getLemmaAndPos(StanfordCorenlpToken token) {
        return token.getLemma() + "_" + getCoarsePOS(token);
    }

    private String getLemmaAndCoarsePos(StanfordCorenlpToken token) {
        return token.getLemma() + "_" + getCoarsePOS(token);
    }

    private String getCoarsePOS(StanfordCorenlpToken token) {
        String pos = token.getPos();
        return pos.length() > 2 ? token.getPos().substring(0, 2) : pos;
    }

    @Override
    public void extractGlobal(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String>
            globalFeatures, List<MentionKey> knownStates, MentionKey currentState) {

    }

    private void addOffsetFeatures(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features,
                                   Function<StanfordCorenlpToken, String> operator,
                                   BiFunction<List<StanfordCorenlpToken>, Integer, Boolean> filter,
                                   String featureType, int windowSize) {
        String currentFeature = computeWordOffsetFeature(sentence, featureType, operator, focus, 0);
        if (currentFeature != null) {
            addToFeatures(features, currentFeature, 1);
        }

        addOffsetFeatureWithRange(sentence, focus, -windowSize, -1, featureType, operator, filter, features);
        addOffsetFeatureWithRange(sentence, focus, 1, windowSize, featureType, operator, filter, features);
    }

    private void addWindowFeatures(List<StanfordCorenlpToken> sentence, int focus, TObjectDoubleMap<String> features,
                                   Function<StanfordCorenlpToken, String> operator,
                                   BiFunction<List<StanfordCorenlpToken>, Integer, Boolean> filter,
                                   String featureType, int maxWindowSize, boolean withDirection) {
        IntStream.rangeClosed(1, maxWindowSize).forEach(windowSize -> {
            String leftPrefix = withDirection ? featureType + "_left" : featureType;
            String rightPrefix = withDirection ? featureType + "_right" : featureType;

            IntStream.rangeClosed(1, windowSize).filter(offset -> filter.apply(sentence, focus + offset))
                    .mapToObj(offset -> computeWindowWordFeature(sentence, rightPrefix, operator, windowSize, focus,
                            offset))
                    .filter(f -> f != null).forEach(f -> addToFeatures(features, f, 1));

            IntStream.rangeClosed(-windowSize, -1).filter(offset -> filter.apply(sentence, focus + offset))
                    .mapToObj(offset -> computeWindowWordFeature(sentence, leftPrefix, operator, windowSize, focus,
                            offset))
                    .filter(f -> f != null).forEach(f -> addToFeatures(features, f, 1));
        });
    }

    private void addOffsetFeatureWithRange(List<StanfordCorenlpToken> sentence, int focus, int begin, int end,
                                           String prefix, Function<StanfordCorenlpToken, String> operator,
                                           BiFunction<List<StanfordCorenlpToken>, Integer, Boolean> filter,
                                           TObjectDoubleMap<String> features) {
        IntStream.rangeClosed(begin, end).filter(offset -> filter.apply(sentence, focus + offset))
                .mapToObj(offset -> computeWordOffsetFeature(sentence, prefix, operator, focus, offset))
                .filter(f -> f != null).forEach(f -> addToFeatures(features, f, 1));
    }

    private void addNgramFeatureWithOffsetRange(List<StanfordCorenlpToken> sentence, int focus, int begin, int end,
                                                String prefix, Function<StanfordCorenlpToken, String> operator,
                                                TObjectDoubleMap<String> features, int n) {
        int left = Math.max(focus + begin, 0);
        int right = Math.min(focus + end, sentence.size());

        int[] runners = new int[n];
        for (int i = 0; i < n; i++) {
            runners[i] = left + i;
            if (runners[i] > right) {
                return;
            }
        }

        String ngramPrefix = "window_" + n + "gram_" + prefix;

        while (true) {
            StringBuilder sb = new StringBuilder();
            // This line add the begin offset information to the N-Gram. For example, a bigram start from -1, means
            // the previous word and current word; a bigram start from 0 means the current word and next word.
            sb.append("left_offset_").append(runners[0] - focus);
            for (int i = 0; i < runners.length; i++) {
                sb.append("_");

                // Operate with outside will ensure we get a feature value. However, we do not allow outside here, this
                // function will simply create <outside> for tokens without NER.
                String val = operateWithOutsideLowerCase(sentence, operator, runners[i]);

                sb.append(val);

                if (runners[i] > right) {
                    return;
                }
                runners[i]++;
            }
//            logger.info("Adding feature " + sb.toString());
            addToFeatures(features, FeatureUtils.formatFeatureName(ngramPrefix, sb.toString()), 1);
        }
    }

    private String computeWordOffsetFeature(List<StanfordCorenlpToken> sentence, String prefix,
                                            Function<StanfordCorenlpToken, String> operator,
                                            int focus, int offset) {
        // NOTE all word features are lowercased.
        String value = operateWithOutsideLowerCase(sentence, operator, focus + offset);
        if (value != null) {
            return FeatureUtils.formatFeatureName(String.format("%s_offset=%d", prefix, offset), value);
        } else {
            return null;
        }
//
//        return Pair.with(String.format("%s_offset=%d", prefix, offset), operateWithOutsideLowerCase(sentence,
// operator,
//                focus + offset));
    }

    private String computeWindowWordFeature(List<StanfordCorenlpToken> sentence, String
            prefix, Function<StanfordCorenlpToken, String> operator, int windowSize, int focus, int offset) {
        // NOTE all word features are lowercased.
        String featureValue = operateWithOutsideLowerCase(sentence, operator, focus + offset);
        if (featureValue != null) {
            return FeatureUtils.formatFeatureName(String.format("%s_window=%d", prefix, windowSize), featureValue);
        } else {
            return null;
        }
    }
}