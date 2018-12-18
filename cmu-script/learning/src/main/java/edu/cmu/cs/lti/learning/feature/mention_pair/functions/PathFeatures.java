package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/17
 * Time: 2:21 PM
 *
 * @author Zhengzhong Liu
 */
public class PathFeatures extends AbstractMentionPairFeatures{
    public PathFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, int firstCandidateId, int secondCandidateId) {
        MentionCandidate firstCandidate = candidates.get(firstCandidateId);
        MentionCandidate secondCandidate = candidates.get(secondCandidateId);

        boolean otherEventInBetween = false;
        for (int i = firstCandidateId + 1; i < secondCandidateId; i++) {
            for (NodeKey nodeKey : candidates.get(i).asKey()) {
                for (NodeKey secondKey : secondCandidate.asKey()) {
                    if (nodeKey.getRealis().equals("Actual")) {
                        if (secondKey.getMentionType().equals(nodeKey.getMentionType())) {
                            addBoolean(featuresNoLabel, "SameTypeActuallyHappenBefore");
                        }
                    }
                }
            }

            if (candidates.get(i).isEvent()){
                otherEventInBetween = true;
            }
        }

        if (otherEventInBetween){
            featuresNoLabel.put("HasOtherEventsInBetween", 1);
        }


        if (firstCandidate.getContainedSentence() == secondCandidate.getContainedSentence()){
            functionWordPathFeatures(documentContext, firstCandidate.getHeadWord(), secondCandidate.getHeadWord(),
                    featuresNoLabel);
        }

        if (firstCandidateId < secondCandidateId){
            featuresNoLabel.put("firstComesFirst", 1);
        }else if (firstCandidateId > secondCandidateId){
            featuresNoLabel.put("secondComesFirst", 1);
        }
    }

    private void functionWordPathFeatures(JCas context, Word firstHead, Word secondHead, TObjectDoubleMap<String> features){
        for (StanfordCorenlpToken word : JCasUtil.selectCovered(context, StanfordCorenlpToken.class, firstHead.getEnd(), secondHead.getBegin())) {
            if (Pattern.matches("\\p{Punct}", word.getCoveredText())) {
                continue;
            }

            String pos = word.getPos();
            if (!pos.startsWith("N") && !pos.startsWith("V") && !pos.startsWith("J") && !pos.startsWith("RB") && !pos.startsWith("D")){
                features.put(FeatureUtils.formatFeatureName("NonContentWordInBetween", word.getLemma()), 1);
            }
        }
    }

    private void dependencyPathFeature(Word firstHead, Word secondHead){

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
}
