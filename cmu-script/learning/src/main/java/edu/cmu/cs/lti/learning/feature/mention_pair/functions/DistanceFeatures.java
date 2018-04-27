package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/27/15
 * Time: 5:30 PM
 *
 * @author Zhengzhong Liu
 */
public class DistanceFeatures extends AbstractMentionPairFeatures {

    private int[] sentenceThresholds = {0, 1, 3, 5};
    private int[] mentionThresholds = {0, 1, 3, 5};

    private int lastSentenceThreshold = sentenceThresholds[sentenceThresholds.length - 1];
    private int lastMentionTreshold = mentionThresholds[mentionThresholds.length - 1];

    private String documentType = "UNKNOWN";

    public DistanceFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        documentType = getDocumentType(context);
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, int firstCandidateId, int secondCandidateId) {
        MentionCandidate firstCandidate = candidates.get(firstCandidateId);
        MentionCandidate secondCandidate = candidates.get(secondCandidateId);
        thresholdedSentenceDistance(featuresNoLabel, firstCandidate, secondCandidate);
    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                   List<MentionCandidate> candidates, NodeKey firstNodeKey, NodeKey secondNodeKey) {
        thresholdedMentionDistance(featuresNeedLabel, candidates, firstNodeKey, secondNodeKey);
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            candidate) {
    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featureNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNodeKey) {
    }

    private void thresholdedSentenceDistance(TObjectDoubleMap<String> rawFeatures, MentionCandidate firstCandidate,
                                             MentionCandidate secondCandidate) {
        Sentence firstSentence = firstCandidate.getContainedSentence();
        Sentence secondSentence = secondCandidate.getContainedSentence();

        if (firstSentence.getIndex() == 0 && secondSentence.getIndex() == 1) {
            addBoolean(rawFeatures, "TitleAndFirstSent");
        }

        // TODO: Edit 3 using wrong sentence and mention distance:
        addBoolean(rawFeatures, FeatureUtils.formatFeatureName("SentenceDistance", "i<=0"));

//        int sentenceInBetween = Math.abs(firstSentence.getIndex() - secondSentence.getIndex());
//        for (int sentenceThreshold : sentenceThresholds) {
//            if (sentenceInBetween <= sentenceThreshold) {
//                addBoolean(rawFeatures, FeatureUtils.formatFeatureName("SentenceDistance", "i<=" +
// sentenceThreshold));
//                return;
//            }
//        }
//
//        addBoolean(rawFeatures, FeatureUtils.formatFeatureName("SentenceDistance", "i>" + lastSentenceThreshold));
    }

    private void thresholdedMentionDistance(TObjectDoubleMap<String> rawFeatures, List<MentionCandidate> candidates,
                                            NodeKey firstNode, NodeKey secondNode) {
        int firstIndex = firstNode.getNodeIndex();
        int secondIndex = secondNode.getNodeIndex();

        int left, right;
        if (firstIndex < secondIndex) {
            left = firstIndex;
            right = secondIndex;
        } else {
            left = secondIndex;
            right = firstIndex;
        }

        int mentionInBetween = 0;
        for (int i = left + 1; i < right; i++) {
            if (!candidates.get(i).isEvent()) {
                mentionInBetween++;
            }
        }

        // TODO: Edit 3 using wrong sentence and mention distance:
        addBoolean(rawFeatures, FeatureUtils.formatFeatureName("MentionDistance", "i<=0"));

//        for (int mentionThreshold : mentionThresholds) {
//            if (mentionInBetween <= mentionThreshold) {
//                addBoolean(rawFeatures, FeatureUtils.formatFeatureName("MentionDistance",
//                        "i<=" + mentionThreshold));
//                return;
//            }
//        }
//        addBoolean(rawFeatures, FeatureUtils.formatFeatureName("MentionDistance",
//                "i>" + lastMentionTreshold));
    }

    private String getDocumentType(JCas context) {
        JCas originalContext = JCasUtil.getView(context, UimaConst.inputViewName, context);

        if (!originalContext.getViewName().equals(UimaConst.inputViewName)){
            logger.warn("Cannot find the input view, use current view.");
        }

        String originalText = originalContext.getDocumentText();
        if (originalText.contains("<post")) {
            return "Forum";
        } else {
            return "News";
        }
    }

}
