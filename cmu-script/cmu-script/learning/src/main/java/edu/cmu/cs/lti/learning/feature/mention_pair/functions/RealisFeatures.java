package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/30/15
 * Time: 4:57 AM
 *
 * @author Zhengzhong Liu
 */
public class RealisFeatures extends AbstractMentionPairFeatures {
    public RealisFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, NodeKey firstNode, NodeKey secondNode) {
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                        List<MentionCandidate> candidates, NodeKey firstNode, NodeKey secondNode) {
        String[] realisTypes = {firstNode.getRealis(), secondNode.getRealis()};
        Arrays.sort(realisTypes);
        addBoolean(featuresNeedLabel, FeatureUtils.formatFeatureName("RealisPair", Joiner.on(":").join(realisTypes)));
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featureNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNode) {
        addBoolean(featureNoLabel, FeatureUtils.formatFeatureName("SingleRealis", secondCandidate.getRealis()));
    }


    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel,
                                        MentionCandidate secondCandidate, NodeKey secondNode) {

    }
}
