package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sentence.FeatureUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.HashMap;
import java.util.Map;

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

    private Map<EventMention, StanfordCorenlpSentence> mention2Sentence;

    public DistanceFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        mention2Sentence = new HashMap<>();
        int sentenceId = 0;
        for (StanfordCorenlpSentence sent : JCasUtil.select(context, StanfordCorenlpSentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                mention2Sentence.put(mention, sent);
            }
            sent.setIndex(sentenceId++);
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures,
                        EventMention firstAnno, EventMention secondAnno) {
//        thresholdedTokenDistance(rawFeatures, tokensInBetween);
        thresholdedSentenceDistance(rawFeatures, firstAnno, secondAnno);
        thresholdedMentionDistance(documentContext, rawFeatures, firstAnno, secondAnno);
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention secondAnno) {
        // Currently nothing to say about the distance between root and a mention. Probably can have features about
        // what is the closest mention it should refer to.
    }

    private void thresholdedSentenceDistance(TObjectDoubleMap<String> rawFeatures, EventMention firstAnno,
                                             EventMention secondAnno) {
        StanfordCorenlpSentence firstSentence = mention2Sentence.get(firstAnno);
        StanfordCorenlpSentence secondSentence = mention2Sentence.get(secondAnno);

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

    private void thresholdedMentionDistance(JCas documentContext, TObjectDoubleMap<String> rawFeatures,
                                            EventMention firstAnno, EventMention secondAnno) {
        int mentionInBetween = JCasUtil.selectBetween(documentContext, EventMention.class, firstAnno, secondAnno)
                .size();
        for (int mentionThreshold : mentionThresholds) {
            if (mentionInBetween <= mentionThreshold) {
                rawFeatures.put(FeatureUtils.formatFeatureName("MentionDistance", "i<=" + mentionThreshold), 1);
                return;
            }
        }
        rawFeatures.put(FeatureUtils.formatFeatureName("MentionDistance", "i>" + lastMentionTreshold), 1);
    }

}
