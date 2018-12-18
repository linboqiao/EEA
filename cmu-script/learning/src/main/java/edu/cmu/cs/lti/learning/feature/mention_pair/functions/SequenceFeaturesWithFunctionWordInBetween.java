package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/17
 * Time: 10:19 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeaturesWithFunctionWordInBetween extends AbstractSequenceFeatures {
    public SequenceFeaturesWithFunctionWordInBetween(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, int firstCandidateId, int secondCandidateId) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                   List<MentionCandidate> candidates, NodeKey firstNodeKey, NodeKey secondNodeKey) {
        MentionCandidate firstCandidate = candidates.get(MentionGraph.getCandidateIndex(firstNodeKey.getNodeIndex()));
        MentionCandidate secondCandidate = candidates.get(MentionGraph.getCandidateIndex(secondNodeKey.getNodeIndex()));

        String firstRealis = firstNodeKey.getRealis();

        if (utils.sentenceWindowConstraint(firstCandidate, secondCandidate, 0)) {
            if (utils.strictEqualRealisConstraint(firstNodeKey, secondNodeKey)) {
                Map<String, Double> compatibleFeatures =
                        utils.generateScriptCompabilityFeatures(firstCandidate, secondCandidate, true);

                for (String word : findWordInBetween(firstCandidate, secondCandidate)) {
                    for (Map.Entry<String, Double> compatibleFeature : compatibleFeatures.entrySet()) {
                        String compatibleFeatureName = compatibleFeature.getKey();
                        double compatibleScore = compatibleFeature.getValue();
                        addWithScore(featuresNeedLabel, String.format("%s_Realis=%s_WordInBetween=%s",
                                compatibleFeatureName, firstRealis, word), compatibleScore);
                    }
                }
            }
        }
    }

    private List<String> findWordInBetween(MentionCandidate firstCandidate, MentionCandidate secondCandidate) {
        int left, right;

        if (firstCandidate.getEnd() < secondCandidate.getBegin()) {
            left = firstCandidate.getEnd();
            right = secondCandidate.getBegin();
        } else {
            left = secondCandidate.getEnd();
            right = firstCandidate.getBegin();
        }

        List<String> tokensInBetween = new ArrayList<>();

        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class,
                firstCandidate.getContainedSentence())) {
            if (token.getBegin() > left && token.getEnd() < right) {
                String pos = token.getPos();
                if (!Pattern.matches("\\p{Punct}", token.getCoveredText())) {
                    if (!pos.startsWith("N") && !pos.startsWith("V") && !pos.startsWith("J")
                            && !pos.startsWith("RB") && !pos.startsWith("D") && !pos.startsWith("CD")) {
                        tokensInBetween.add(token.getLemma().toLowerCase());
                    }
                }
            }
        }

        return tokensInBetween;
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate candidate) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, MentionCandidate
            secondCandidate, NodeKey secondNodeKey) {

    }
}
