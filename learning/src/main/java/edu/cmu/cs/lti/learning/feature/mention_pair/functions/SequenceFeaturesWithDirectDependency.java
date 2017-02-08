package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/17
 * Time: 10:19 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeaturesWithDirectDependency extends AbstractSequenceFeatures {
    public SequenceFeaturesWithDirectDependency(Configuration generalConfig, Configuration featureConfig) {
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
                String forwardDep = checkDirectDependency(firstCandidate, secondCandidate);

                if (forwardDep != null) {
                    Map<String, Double> compatibleFeatures =
                            utils.generateScriptCompabilityFeatures(firstCandidate, secondCandidate, true);

                    for (Map.Entry<String, Double> compatibleFeature : compatibleFeatures.entrySet()) {
                        String compatibleFeatureName = compatibleFeature.getKey();
                        double compatibleScore = compatibleFeature.getValue();
                        addWithScore(featuresNeedLabel, String.format("%s_Realis=%s_ForwardDep=%s",
                                compatibleFeatureName, firstRealis, forwardDep), compatibleScore);
                    }
                } else {
                    String backwardDep = checkDirectDependency(secondCandidate, firstCandidate);
                    if (backwardDep != null) {
                        Map<String, Double> compatibleFeatures =
                                utils.generateScriptCompabilityFeatures(firstCandidate, secondCandidate, true);

                        for (Map.Entry<String, Double> compatibleFeature : compatibleFeatures.entrySet()) {
                            String compatibleFeatureName = compatibleFeature.getKey();
                            double compatibleScore = compatibleFeature.getValue();
                            addWithScore(featuresNeedLabel, String.format("%s_Realis=%s_BackwardDep=%s",
                                    compatibleFeatureName, firstRealis, backwardDep), compatibleScore);
                        }
                    }
                }
            }
        }
    }

    private String checkDirectDependency(MentionCandidate firstCandidate, MentionCandidate secondCandidate) {
        Word firstHead = firstCandidate.getHeadWord();
        Word secondHead = secondCandidate.getHeadWord();
        return checkDep(firstHead, secondHead);
    }

    private String checkDep(Word firstWord, Word secondWord) {
        FSList firstChildren = firstWord.getChildDependencyRelations();

        if (firstChildren != null) {
            for (Dependency dep : FSCollectionFactory.create(firstChildren, Dependency.class)) {
                if (dep.getChild().equals(secondWord)) {
                    return dep.getDependencyType();
                }
            }
        }
        return null;
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate candidate) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, MentionCandidate
            secondCandidate, NodeKey secondNodeKey) {

    }
}
