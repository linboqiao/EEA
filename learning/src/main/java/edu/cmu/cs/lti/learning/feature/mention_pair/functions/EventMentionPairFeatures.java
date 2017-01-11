package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/12/16
 * Time: 7:26 PM
 *
 * @author Zhengzhong Liu
 */
public class EventMentionPairFeatures extends AbstractMentionPairFeatures {
    public EventMentionPairFeatures(Configuration generalConfig, Configuration featureConfig) {
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
                                   List<MentionCandidate> candidates, NodeKey firstNodeKey, NodeKey
                                           secondNodeKey) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            candidate) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                   MentionCandidate secondCandidate, NodeKey secondNodeKey) {

    }
}
