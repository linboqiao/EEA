package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sentence.FeatureUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/27/15
 * Time: 5:18 PM
 *
 * @author Zhengzhong Liu
 */
public class HeadWordPairFeatures extends AbstractMentionPairFeatures {
    public HeadWordPairFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    public void initDocumentWorkspace(JCas context) {

    }

    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures,
                        EventMention firstAnno, EventMention secondAnno) {
        String firstLemma = getLemma(firstAnno);
        String secondLemma = getLemma(secondAnno);

        lemmaPairFeature(rawFeatures, firstLemma, secondLemma);
        lemmaMatchFeature(rawFeatures, firstLemma, secondLemma);
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention secondAnno) {
// Disable because there are no much we can do to derive whether a mention is coreferent with something by looking at
// its lemma.
    }

    private String getLemma(EventMention mention) {
        StanfordCorenlpToken head = UimaNlpUtils.findHeadFromTreeAnnotation(mention);

        if (head == null) {
            return mention.getCoveredText().toLowerCase();
        }

        String lemma = head.getLemma();
        return lemma.toLowerCase();
    }

    private void lemmaPairFeature(TObjectDoubleMap<String> rawFeatures, String firstLemma, String secondLemma) {
        String lemmaPair;
        if (firstLemma.compareTo(secondLemma) > 0) {
            lemmaPair = firstLemma + "_" + secondLemma;
        } else {
            lemmaPair = secondLemma + "_" + firstLemma;
        }

        rawFeatures.put(FeatureUtils.formatFeatureName("HeadLemmaPair", lemmaPair), 1);
    }

    private void lemmaMatchFeature(TObjectDoubleMap<String> rawFeatures, String firstLemma, String secondLemma) {
        if (firstLemma.equals(secondLemma)) {
            rawFeatures.put("LemmaMatch", 1);
        }
    }

    private void lemmaSubstringFeature(TObjectDoubleMap<String> rawFeatures, String firstLemma, String secondLemma) {
        if (firstLemma.contains(secondLemma) || secondLemma.contains(firstLemma)) {
            rawFeatures.put("LemmaSubString", 1);
        }
    }

}
