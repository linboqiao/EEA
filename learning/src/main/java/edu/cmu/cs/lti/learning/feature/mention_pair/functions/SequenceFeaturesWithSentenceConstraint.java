package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/17
 * Time: 10:19 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeaturesWithSentenceConstraint extends AbstractMentionPairFeatures {
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
        String firstType = firstNodeKey.getMentionType();
        String secondType = secondNodeKey.getMentionType();

        MentionCandidate firstCandidate = candidates.get(MentionGraph.getCandidateIndex(firstNodeKey.getNodeIndex()));
        MentionCandidate secondCandidate = candidates.get(MentionGraph.getCandidateIndex(secondNodeKey.getNodeIndex()));

        int firstSentenceIndex = firstCandidate.getContainedSentence().getIndex();
        int secondSentenceIndex = secondCandidate.getContainedSentence().getIndex();

        String firstRealis = firstNodeKey.getRealis();
        String secondRealis = secondNodeKey.getRealis();

        int sentDist = Math.abs(secondSentenceIndex - firstSentenceIndex);

        if (sentDist < 3) {
            if (!firstNodeKey.getRealis().equals("Other") && firstRealis.equals(secondRealis)) {
                if (firstCandidate.getBegin() < secondCandidate.getBegin()) {
                    addBoolean(featuresNeedLabel,
                            String.format("ForwardMentionTypePair_SentDist=%d_Realis=%s::%s:%s", sentDist,
                                    firstRealis, firstType, secondType)
                    );
                    addBoolean(featuresNeedLabel,
                            String.format("ForwardHeadwordPair_SentDist=%d_Realis=%s::%s:%s", sentDist, firstRealis,
                                    firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                    secondCandidate.getHeadWord().getLemma().toLowerCase())
                    );
                } else {
                    addBoolean(featuresNeedLabel,
                            String.format("BackwardMentionTypePair_SentDist=%d_Realis=%s::%s:%s", sentDist,
                                    firstRealis, firstType, secondType)
                    );
                    addBoolean(featuresNeedLabel,
                            String.format("BackwardHeadwordPair_SentDist=%d_Realis=%s::%s:%s", sentDist, firstRealis,
                                    firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                    secondCandidate.getHeadWord().getLemma().toLowerCase())
                    );
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
