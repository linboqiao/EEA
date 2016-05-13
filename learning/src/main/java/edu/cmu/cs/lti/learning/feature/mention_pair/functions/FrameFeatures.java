package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/30/15
 * Time: 5:16 AM
 *
 * @author Zhengzhong Liu
 */
public class FrameFeatures extends AbstractMentionPairFeatures {
    public FrameFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate> candidates, NodeKey firstNode, NodeKey secondNode) {
        MentionCandidate firstCandidate = candidates.get(firstNode.getCandidateIndex());
        MentionCandidate secondCandidate = candidates.get(secondNode.getCandidateIndex());

        Word firstHead = firstCandidate.getHeadWord();
        Word secondHead = secondCandidate.getHeadWord();

        if (firstHead.getFrameName() != null && secondHead.getFrameName() != null) {
            addBoolean(featuresNoLabel, FeatureUtils.formatFeatureName("FramePair", FeatureUtils.sortedJoin(
                    firstHead.getFrameName(), secondHead.getFrameName())));
        }
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                        List
                                                <MentionCandidate> candidates, NodeKey firstNode, NodeKey secondNode) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNode) {

    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featureNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNode) {

    }
}
