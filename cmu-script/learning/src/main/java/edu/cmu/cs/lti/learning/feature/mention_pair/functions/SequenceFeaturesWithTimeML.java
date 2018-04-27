package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.script.timeml.TemporalLink;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/31/17
 * Time: 9:06 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeaturesWithTimeML extends AbstractSequenceFeatures {
    private Table<Word, Word, String> timemlRelations;

    public SequenceFeaturesWithTimeML(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        timemlRelations = HashBasedTable.create();

        Collection<TemporalLink> temporalLinks = JCasUtil.select(context, TemporalLink.class);

        for (TemporalLink temporalLink : temporalLinks) {
            StanfordCorenlpToken firstToken = UimaNlpUtils.findFirstToken(temporalLink.getSource(),
                    StanfordCorenlpToken.class);
            StanfordCorenlpToken secondToken = UimaNlpUtils.findFirstToken(temporalLink.getTarget(),
                    StanfordCorenlpToken.class);

            if (firstToken != null && secondToken != null) {
                timemlRelations.put(firstToken, secondToken, temporalLink.getRelationType());
            }
        }
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

        String relation = getTemporalRelation(firstCandidate, secondCandidate);

        if (utils.sentenceWindowConstraint(firstCandidate, secondCandidate, 3)) {
            if (utils.strictEqualRealisConstraint(firstNodeKey, secondNodeKey)) {
                if (relation != null) {
                    utils.generateScriptCompabilityFeatures(firstCandidate, secondCandidate, true);

                    for (Map.Entry<String, Double> compatibleFeature :
                            utils.generateScriptCompabilityFeatures(firstCandidate, secondCandidate).entrySet()) {
                        String compatibleFeatureName = compatibleFeature.getKey();
                        double compatibleScore = compatibleFeature.getValue();
                        addWithScore(featuresNeedLabel, String.format("%s_Realis=%s_TimeML=%s",
                                compatibleFeatureName, firstRealis, relation), compatibleScore);
//                        logger.info("Adding feature: ");
//                        logger.info(String.format("%s_Realis=%s_TimeML=%s", compatibleFeatureName, firstRealis,
//                                relation));
//                        DebugUtils.pause();
                    }
                }
            }
        }
    }

    private String getTemporalRelation(MentionCandidate firstCandidate, MentionCandidate secondCandidate) {
        Word firstWord = firstCandidate.getHeadWord();
        Word secondWord = secondCandidate.getHeadWord();

        if (timemlRelations.contains(firstWord, secondWord)) {
            return timemlRelations.get(firstWord, secondWord);
        } else {
            return timemlRelations.get(secondWord, firstWord);
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
