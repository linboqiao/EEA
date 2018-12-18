package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/17
 * Time: 10:19 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeaturesWithSentenceConstraint extends AbstractSequenceFeatures {
    public SequenceFeaturesWithSentenceConstraint(Configuration generalConfig, Configuration featureConfig) {
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

        int firstSentenceIndex = firstCandidate.getContainedSentence().getIndex();
        int secondSentenceIndex = secondCandidate.getContainedSentence().getIndex();

        String firstRealis = firstNodeKey.getRealis();

        int sentDist = Math.abs(secondSentenceIndex - firstSentenceIndex);

        if (utils.sentenceWindowConstraint(firstCandidate, secondCandidate, 3)) {
            if (utils.strictEqualRealisConstraint(firstNodeKey, secondNodeKey)) {
                for (Map.Entry<String, Double> compatibleFeature : utils.generateScriptCompabilityFeatures
                        (firstCandidate, secondCandidate).entrySet()) {
                    String compatibleFeatureName = compatibleFeature.getKey();
                    double compatibleScore = compatibleFeature.getValue();
                    addWithScore(featuresNeedLabel, String.format("%s_SentDist=%d_Realis=%s",
                            compatibleFeatureName, sentDist, firstRealis), compatibleScore);
                }
            }
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate candidate) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, MentionCandidate
            secondCandidate, NodeKey secondNodeKey) {

    }
}
