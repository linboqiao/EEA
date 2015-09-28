package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sentence.FeatureUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/27/15
 * Time: 5:30 PM
 *
 * @author Zhengzhong Liu
 */
public class DistanceFeatures extends MentionPairFeatures {
    private int[] tokenThresholds = {3, 5, 10};

    private int[] sentenceThresholds = {0, 1, 3, 5};

    private int lastTokenThreshold = tokenThresholds[tokenThresholds.length - 1];

    private int lastSentenceThreshold = sentenceThresholds[sentenceThresholds.length - 1];

    private Map<EventMention, StanfordCorenlpSentence> mention2Sentence;

    public DistanceFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
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
        List<StanfordCorenlpToken> tokensInBetween = JCasUtil.selectBetween(documentContext, StanfordCorenlpToken
                .class, firstAnno, secondAnno);
        thresholdedTokenDistance(rawFeatures, tokensInBetween);
        thresholdedSentenceDistance(rawFeatures, firstAnno, secondAnno);
    }

    private void thresholdedTokenDistance(TObjectDoubleMap<String> rawFeatures, List<StanfordCorenlpToken>
            tokensInBetween) {
        for (int t : tokenThresholds) {
            if (tokensInBetween.size() <= t) {
                rawFeatures.put(FeatureUtils.formatFeatureName("TokenDistance", "i<=" + t), 1);
                return;
            }
        }
        rawFeatures.put(FeatureUtils.formatFeatureName("TokenDistance", "i>" + lastTokenThreshold), 1);
    }

    private void thresholdedSentenceDistance(TObjectDoubleMap<String> rawFeatures, EventMention firstAnno,
                                             EventMention secondAnno) {
        int sentenceInBetween = Math.abs(mention2Sentence.get(firstAnno).getIndex() - mention2Sentence.get
                (secondAnno).getIndex());
        for (int sentenceThreshold : sentenceThresholds) {
            if (sentenceInBetween <= sentenceThreshold) {
                rawFeatures.put(FeatureUtils.formatFeatureName("SentenceDistance", "i<=" + sentenceThreshold), 1);
                return;
            }
        }

        rawFeatures.put(FeatureUtils.formatFeatureName("SentenceDistance", "i>" + lastSentenceThreshold), 1);
    }

}
