package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/30/15
 * Time: 5:26 AM
 *
 * @author Zhengzhong Liu
 */
public class ForumRepeatFeature extends AbstractMentionPairFeatures {
    public ForumRepeatFeature(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate> candidates, int firstIndex, int secondIndex) {
        MentionCandidate firstCandidate = candidates.get(firstIndex);
        MentionCandidate secondCandidate = candidates.get(secondIndex);

        StanfordCorenlpSentence sentence1 = (StanfordCorenlpSentence) firstCandidate.getContainedSentence();
        StanfordCorenlpSentence sentence2 = (StanfordCorenlpSentence) secondCandidate.getContainedSentence();

        if (sentence1 != null && sentence2 != null) {
            String sentStr1 = sentence1.getCoveredText().trim();
            String sentStr2 = sentence2.getCoveredText().trim();

            if (sentStr1.equals(sentStr2)) {
                int firstOffset = firstCandidate.getBegin() - sentence1.getBegin();
                int secondOffset = secondCandidate.getBegin() - sentence2.getBegin();
                if (firstOffset == secondOffset) {
                    addBoolean(featuresNoLabel, "ForumRepeat");
                }
            }
        }
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, List<MentionCandidate> candidates, int firstIndex, int

            secondIndex) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            secondCandidate) {

    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featureNoLabel, MentionCandidate
            secondCandidate) {

    }
}
