package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;

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

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate> candidates, int firstIndex, int secondIndex) {
        MentionCandidate firstCandidate = candidates.get(firstIndex);
        MentionCandidate secondCandidate = candidates.get(secondIndex);

        Word firstHead = firstCandidate.getHeadWord();
        Word secondHead = secondCandidate.getHeadWord();

        String firstLemma = firstHead.getLemma().toLowerCase();
        String secondLemma = secondHead.getLemma().toLowerCase();

        lemmaPairFeature(featuresNoLabel, firstLemma, secondLemma);
        lemmaMatchFeature(featuresNoLabel, firstLemma, secondLemma);
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, List<MentionCandidate> candidates, int firstIndex, int

            secondIndex) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel,
                        MentionCandidate secondCandidate) {

    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featureNoLabel, MentionCandidate secondCandidate) {

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
