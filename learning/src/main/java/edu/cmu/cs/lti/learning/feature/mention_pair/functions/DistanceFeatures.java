package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            candidates, NodeKey firstNode, NodeKey secondNode) {
        MentionCandidate firstCandidate = candidates.get(firstNode.getCandidateIndex());
        MentionCandidate secondCandidate = candidates.get(secondNode.getCandidateIndex());
        thresholdedSentenceDistance(featuresNoLabel, firstCandidate, secondCandidate);
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                        List<MentionCandidate> candidates, NodeKey firstNode, NodeKey secondNode) {
        thresholdedMentionDistance(featuresNeedLabel, candidates, firstNode, secondNode);
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNode) {
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featureNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNode) {
    }

    private void thresholdedSentenceDistance(TObjectDoubleMap<String> rawFeatures, MentionCandidate firstCandidate,
                                             MentionCandidate secondCandidate) {
        Sentence firstSentence = firstCandidate.getContainedSentence();
        Sentence secondSentence = secondCandidate.getContainedSentence();

        if (firstSentence.getIndex() == 0 && secondSentence.getIndex() == 1) {
            addBoolean(rawFeatures, "TitleAndFirstSent");
        }

        int sentenceInBetween = Math.abs(firstSentence.getIndex() - secondSentence.getIndex());
        for (int sentenceThreshold : sentenceThresholds) {
            if (sentenceInBetween <= sentenceThreshold) {
                addBoolean(rawFeatures, FeatureUtils.formatFeatureName("SentenceDistance", "i<=" + sentenceThreshold));
                return;
            }
        }

        rawFeatures.put(FeatureUtils.formatFeatureName("SentenceDistance", "i>" + lastSentenceThreshold), 1);
    }

    private void thresholdedMentionDistance(TObjectDoubleMap<String> rawFeatures, List<MentionCandidate> candidates,
                                            NodeKey firstNode, NodeKey secondNode) {
        int firstIndex = firstNode.getCandidateIndex();
        int secondIndex = secondNode.getCandidateIndex();

        int left, right;
        if (firstIndex < secondIndex) {
            left = firstIndex;
            right = secondIndex;
        } else {
            left = secondIndex;
            right = firstIndex;
        }

        Set<Span> mentionsInBetween = new HashSet<>();

        for (int i = left + 1; i < right; i++) {
//            logger.debug(i + " is " + candidates.get(i).getMentionType() + " " + candidates.get(i).isEvent());
            MentionCandidate c = candidates.get(i);
            if (c.isEvent()) {
                mentionsInBetween.add(Span.of(c.getBegin(), c.getEnd()));
            }
        }

        int numMentionInBetween = mentionsInBetween.size();

//        logger.debug("Mention distance is " + mentionInBetween + " between " + left + " " + right);

        for (int mentionThreshold : mentionThresholds) {
            if (numMentionInBetween <= mentionThreshold) {
                rawFeatures.put(FeatureUtils.formatFeatureName("MentionDistance",
                        "i<=" + mentionThreshold), 1);
                return;
            }
        }
        rawFeatures.put(FeatureUtils.formatFeatureName("MentionDistance", "i>" + lastMentionTreshold), 1);
    }

    private String getDocumentType(JCas context) {
        JCas originalContext = JCasUtil.getView(context, "original", false);
        String originalText = originalContext.getDocumentText();
        if (originalText.contains("<post")) {
            return "Forum";
        } else {
            return "News";
        }
    }

}
