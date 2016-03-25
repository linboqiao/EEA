package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
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
            candidates, int firstIndex, int secondIndex) {
        MentionCandidate firstCandidate = candidates.get(firstIndex);
        MentionCandidate secondCandidate = candidates.get(secondIndex);
        thresholdedSentenceDistance(featuresNoLabel, firstCandidate, secondCandidate);
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                        List<MentionCandidate> candidates, int firstIndex, int secondIndex) {
        thresholdedMentionDistance(featuresNeedLabel, candidates, firstIndex, secondIndex);
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            secondCandidate) {
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featureNoLabel, MentionCandidate
            secondCandidate) {
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
                addBoolean(rawFeatures, FeatureUtils.formatFeatureName("SentenceDistance",
                        "i<=" + sentenceThreshold));
                return;
            }
        }

        rawFeatures.put(FeatureUtils.formatFeatureName("SentenceDistance", "i>" +
                lastSentenceThreshold), 1);
    }

    private void thresholdedMentionDistance(TObjectDoubleMap<String> rawFeatures, List<MentionCandidate> candidates,
                                            int firstIndex, int secondIndex) {
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
            if (!candidates.get(i).getMentionType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                mentionInBetween++;
            }
        }

        for (int mentionThreshold : mentionThresholds) {
            if (mentionInBetween <= mentionThreshold) {
                rawFeatures.put(FeatureUtils.formatFeatureName("MentionDistance",
                        "i<=" + mentionThreshold), 1);
                return;
            }
        }
        rawFeatures.put(FeatureUtils.formatFeatureName("MentionDistance",
                "i>" + lastMentionTreshold), 1);
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
