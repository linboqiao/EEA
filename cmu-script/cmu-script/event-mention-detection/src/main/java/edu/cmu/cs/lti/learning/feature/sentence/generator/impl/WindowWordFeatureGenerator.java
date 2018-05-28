package edu.cmu.cs.lti.learning.feature.sentence.generator.impl;

import edu.cmu.cs.lti.learning.feature.sentence.generator.EventMentionFeatureGenerator;
import edu.cmu.cs.lti.emd.utils.WordNetSenseIdentifier;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/5/15
 * Time: 2:00 PM
 */
public class WindowWordFeatureGenerator extends EventMentionFeatureGenerator {

    private int windowSize;
    private WordNetSenseIdentifier wnsi;
    private Map<Word, Integer> wordIds;
    private ArrayList<StanfordCorenlpToken> allWords;

    public WindowWordFeatureGenerator(int windowSize, WordNetSenseIdentifier wnsi, ArrayList<StanfordCorenlpToken> allWords, Set<String> featureSubset) {
        super(featureSubset);
        this.windowSize = windowSize;
        this.wnsi = wnsi;
        this.allWords = allWords;
        indexWords();
    }


    @Override
    public Map<String, Double> genFeatures(CandidateEventMention mention) {
        StanfordCorenlpToken candidateHead = mention.getHeadWord();
        Map<String, Double> features = new HashMap<>();
        int centerId = wordIds.get(candidateHead);
        int leftLimit = centerId - windowSize > 0 ? centerId - windowSize : 0;
        int rightLimit = centerId + windowSize < allWords.size() - 1 ? centerId + windowSize : allWords.size() - 1;
        for (int i = centerId; i >= leftLimit; i--) {
            addWindowFeature(allWords.get(i), features);
        }
        for (int i = centerId; i <= rightLimit; i++) {
            addWindowFeature(allWords.get(i), features);
        }
        return features;
    }

    private void addWindowFeature(StanfordCorenlpToken word, Map<String, Double> features) {
        if (!word.getPos().equals(".") && !word.getPos().equals(",") && !word.getPos().equals(":")) {
            addFeature("WindowLemma", word.getLemma().toLowerCase(), features);
        }

        if (word.getNerTag() != null) {
            addFeature("WindowNer", word.getNerTag(), features);
        }

        for (String wordType : wnsi.getInterestingSupertype(word.getLemma().toLowerCase())) {
            addFeature("WindowSuperType", wordType, features);
        }
    }

    private void indexWords() {
        wordIds = new HashMap<>();
        int i = 0;
        for (Word word : allWords) {
            wordIds.put(word, i++);
        }
    }
}