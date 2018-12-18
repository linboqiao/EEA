package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/17
 * Time: 9:30 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionDistanceFeatures extends AbstractMentionPairFeatures {
    private int[] mentionThresholds = {0, 1, 3, 5};

    private int lastMentionTreshold = mentionThresholds[mentionThresholds.length - 1];


    public MentionDistanceFeatures(Configuration generalConfig, Configuration featureConfig) {
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
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, List<MentionCandidate> candidates, NodeKey firstNodeKey, NodeKey secondNodeKey) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate candidate) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, MentionCandidate secondCandidate, NodeKey secondNodeKey) {

    }


    private void thresholdedMentionDistance(TObjectDoubleMap<String> rawFeatures, List<MentionCandidate> candidates,
                                            NodeKey firstNode, NodeKey secondNode) {
        int firstIndex = firstNode.getNodeIndex();
        int secondIndex = secondNode.getNodeIndex();

        int left = Math.min(firstIndex, secondIndex);
        int right = Math.max(firstIndex, secondIndex);

        int numMentionInBetween = 0;
        int lastBegin = -1;
        for (int i = left + 1; i < right; i++) {
            logger.debug(i + " is " + candidates.get(i).getMentionType() + " " + candidates.get(i).isEvent());
            MentionCandidate c = candidates.get(i);
            if (c.isEvent()) {
                if (c.getBegin() != lastBegin) {
                    lastBegin = c.getBegin();
                    numMentionInBetween++;
                }
            }
        }

        for (int mentionThreshold : mentionThresholds) {
            if (numMentionInBetween <= mentionThreshold) {
                rawFeatures.put(FeatureUtils.formatFeatureName("MentionDistance",
                        "i<=" + mentionThreshold), 1);
                return;
            }
        }
        rawFeatures.put(FeatureUtils.formatFeatureName("MentionDistance", "i>" + lastMentionTreshold), 1);
    }
}
