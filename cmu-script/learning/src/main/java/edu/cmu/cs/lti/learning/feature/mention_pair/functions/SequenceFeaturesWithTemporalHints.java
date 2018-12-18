package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.script.type.SemanticRelation;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/31/17
 * Time: 9:06 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeaturesWithTemporalHints extends AbstractSequenceFeatures {
    public SequenceFeaturesWithTemporalHints(Configuration generalConfig, Configuration featureConfig) {
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

        if (utils.sentenceWindowConstraint(firstCandidate, secondCandidate, 3)) {
            if (utils.strictEqualRealisConstraint(firstNodeKey, secondNodeKey)) {
                if (hasTemporalHint(firstCandidate) && hasTemporalHint(secondCandidate)) {

                    utils.generateScriptCompabilityFeatures(firstCandidate, secondCandidate);

                    for (Map.Entry<String, Double> compatibleFeature :
                            utils.generateScriptCompabilityFeatures(firstCandidate, secondCandidate).entrySet()) {
                        String compatibleFeatureName = compatibleFeature.getKey();
                        double compatibleScore = compatibleFeature.getValue();
                        addWithScore(featuresNeedLabel, String.format("%s_Realis=%s_WithTime",
                                compatibleFeatureName, firstRealis), compatibleScore);
                    }
                }
            }
        }
    }

    private boolean hasTemporalHint(MentionCandidate candidate) {
        // If there is a time expression some where in the sentence.
        for (StanfordEntityMention em : JCasUtil.selectCovered(StanfordEntityMention.class,
                candidate.getContainedSentence())) {
            if (em.getEntityType() != null && em.getEntityType().equals("DATE")) {
                return true;
            }
        }

        FSList argumentsFS = candidate.getHeadWord().getChildSemanticRelations();

        if (argumentsFS != null) {
            for (SemanticRelation relation : FSCollectionFactory.create(argumentsFS, SemanticRelation.class)) {
                if (relation.getPropbankRoleName() != null && relation.getPropbankRoleName().equals("ARGM-TMP")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate candidate) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, MentionCandidate
            secondCandidate, NodeKey secondNodeKey) {

    }
}
