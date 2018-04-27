package edu.cmu.cs.lti.learning.feature.sequence.document.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/17/15
 * Time: 2:07 PM
 *
 * @author Zhengzhong Liu
 */
public class SurroundingVerbFeatures<T extends Annotation> extends SequenceFeatureWithFocus<T> {
    TObjectIntMap<StanfordCorenlpToken> word2Index = new TObjectIntHashMap<>();
    List<StanfordCorenlpToken> allWords;
    Map<Integer, Integer> leftJumps;
    Map<Integer, Integer> rightJumps;

    public SurroundingVerbFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        allWords = new ArrayList<>();
        leftJumps = new HashMap<>();
        rightJumps = new HashMap<>();

        int firstLeftVerbIndex = -1;
        List<Integer> leftoverWords = new ArrayList<>();

        int wordIndex = 0;
        for (StanfordCorenlpToken word : JCasUtil.select(context, StanfordCorenlpToken.class)) {
            allWords.add(word);
            word2Index.put(word, wordIndex);
            leftJumps.put(wordIndex, firstLeftVerbIndex);

            if (word.getPos().startsWith("V")) {
                firstLeftVerbIndex = wordIndex;
                for (int leftoverWord : leftoverWords) {
                    rightJumps.put(leftoverWord, wordIndex);
                }
                leftoverWords = new ArrayList<>();
            }

            leftoverWords.add(wordIndex);
            wordIndex++;
        }
    }

    private List<StanfordCorenlpToken> findVerbs(StanfordCorenlpToken token, int k, Map<Integer, Integer> jumps) {
        int wordIndex = word2Index.get(token);

        List<StanfordCorenlpToken> leftKVerbs = new ArrayList<>();

        Integer currentIndex = wordIndex;
        int count = 0;
        while (count < k) {
            currentIndex = jumps.get(currentIndex);
            if (currentIndex == null || currentIndex < 0 || currentIndex >= allWords.size()) {
                break;
            }
            leftKVerbs.add(allWords.get(currentIndex));
            count++;
        }

        return leftKVerbs;
    }

    @Override
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extract(List<T> sequence, int focus, TObjectDoubleMap<String> nodeFeatures,
                        Table<Pair<Integer, Integer>, String, Double> edgeFeatures) {
        if (focus < 0 || focus >= sequence.size()) {
            return;
        }

        StanfordCorenlpToken token = UimaNlpUtils.findFirstToken(sequence.get(focus), StanfordCorenlpToken.class);

        for (StanfordCorenlpToken lVerb : findVerbs(token, 2, leftJumps)) {
            addToFeatures(nodeFeatures, FeatureUtils.formatFeatureName("Left2Verb", lVerb.getLemma().toLowerCase()), 1);
        }

        for (StanfordCorenlpToken rVerb : findVerbs(token, 2, rightJumps)) {
            addToFeatures(nodeFeatures, FeatureUtils.formatFeatureName("Right2Verb", rVerb.getLemma().toLowerCase()),
                    1);
        }
    }

    @Override
    public void extractGlobal(List<T> sequence, int focus, TObjectDoubleMap<String> globalFeatures,
                              List<MentionKey> knownStates, MentionKey currentState) {

    }


}
