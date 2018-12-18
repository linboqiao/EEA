package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/31/17
 * Time: 1:53 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeaturesWithMentionInBetween extends AbstractSequenceFeatures {
    private TObjectIntHashMap<Word> head2Entity;

    public SequenceFeaturesWithMentionInBetween(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        head2Entity = new TObjectIntHashMap<>();
        int entityId = 0;
        for (Entity entity : JCasUtil.select(context, Entity.class)) {
            for (int i = 0; i < entity.getEntityMentions().size(); i++) {
                head2Entity.put(entity.getEntityMentions(i).getHead(), entityId);
            }
            entityId++;
        }
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

        int firstCandIndex = MentionGraph.getCandidateIndex(firstNodeKey.getNodeIndex());
        int secondCandIndex = MentionGraph.getCandidateIndex(secondNodeKey.getNodeIndex());
        MentionCandidate firstCandidate = candidates.get(firstCandIndex);
        MentionCandidate secondCandidate = candidates.get(secondCandIndex);


        String firstRealis = firstNodeKey.getRealis();

        int leftCandIndex = Math.min(firstCandIndex, secondCandIndex);
        int rightCandIndex = Math.max(firstCandIndex, secondCandIndex);

        if (utils.sentenceWindowConstraint(firstCandidate, secondCandidate, 3)) {
            if (utils.strictEqualRealisConstraint(firstNodeKey, secondNodeKey)) {
                Map<String, Double> compatibleFeatures = utils.generateScriptCompabilityFeatures(
                        firstCandidate, secondCandidate, true);

                int numMib = rightCandIndex - leftCandIndex - 1;
                for (int mibIndex = leftCandIndex + 1; mibIndex < rightCandIndex; mibIndex++) {
                    for (NodeKey nodeKey : candidates.get(mibIndex).asKey()) {
                        String mibType = nodeKey.getMentionType();

                        for (Map.Entry<String, Double> compatibleFeature : compatibleFeatures.entrySet()) {
                            String compatibleFeatureName = compatibleFeature.getKey();
                            double compatibleScore = compatibleFeature.getValue();
                            addWithScore(featuresNeedLabel,String.format("%s_Realis=%s_MentionInBetween=%s",
                                    compatibleFeatureName, firstRealis, mibType), compatibleScore);
                        }
                    }
                }

                for (Map.Entry<String, Double> compatibleFeature : compatibleFeatures.entrySet()) {
                    String compatibleFeatureName = compatibleFeature.getKey();
                    double compatibleScore = compatibleFeature.getValue();
                    addWithScore(featuresNeedLabel, String.format("%s_Realis=%s_NumMib=%d",
                            compatibleFeatureName, firstRealis, numMib), compatibleScore);
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
