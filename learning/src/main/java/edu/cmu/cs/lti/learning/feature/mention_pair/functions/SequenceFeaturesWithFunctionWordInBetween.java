package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/17
 * Time: 10:19 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeaturesWithFunctionWordInBetween extends AbstractMentionPairFeatures {
    public SequenceFeaturesWithFunctionWordInBetween(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, int firstCandidateId, int secondCandidateId) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                   List<MentionCandidate> candidates, NodeKey firstNodeKey, NodeKey secondNodeKey) {
        String firstType = firstNodeKey.getMentionType();
        String secondType = secondNodeKey.getMentionType();

        MentionCandidate firstCandidate = candidates.get(MentionGraph.getCandidateIndex(firstNodeKey.getNodeIndex()));
        MentionCandidate secondCandidate = candidates.get(MentionGraph.getCandidateIndex(secondNodeKey.getNodeIndex()));

        int firstSentenceIndex = firstCandidate.getContainedSentence().getIndex();
        int secondSentenceIndex = secondCandidate.getContainedSentence().getIndex();

        String firstRealis = firstNodeKey.getRealis();
        String secondRealis = secondNodeKey.getRealis();


        if (secondSentenceIndex - firstSentenceIndex == 0) {
            for (String word : findWordInBetween(firstCandidate, secondCandidate)) {
                addBoolean(featuresNeedLabel,
                        String.format("MentionTypePair_Realis=%s_WordInBetween=%s::%s:%s",
                                firstRealis, word, firstType, secondType)
                );
                addBoolean(featuresNeedLabel,
                        String.format("HeadwordPair_Realis=%s_WordInBetween=%s::%s:%s",
                                firstRealis, word,
                                firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                secondCandidate.getHeadWord().getLemma().toLowerCase())
                );

                if (!firstNodeKey.getRealis().equals("Other") && firstRealis.equals(secondRealis)) {
                    if (firstCandidate.getBegin() < secondCandidate.getBegin()) {
                        addBoolean(featuresNeedLabel,
                                String.format("ForwardMentionTypePair_Realis=%s_WordInBetween=%s::%s:%s",
                                        firstRealis, word, firstType, secondType)
                        );
                        addBoolean(featuresNeedLabel,
                                String.format("ForwardHeadwordPair_Realis=%s_WordInBetween=%s::%s:%s",
                                        firstRealis, word,
                                        firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                        secondCandidate.getHeadWord().getLemma().toLowerCase())
                        );
                    } else {
                        addBoolean(featuresNeedLabel,
                                String.format("BackwardMentionTypePair_Realis=%s_WordInBetween=%s::%s:%s",
                                        firstRealis, word, firstType, secondType)
                        );
                        addBoolean(featuresNeedLabel,
                                String.format("BackwardHeadwordPair_Realis=%s_WordInBetween=%s::%s:%s",
                                        firstRealis, word,
                                        firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                        secondCandidate.getHeadWord().getLemma().toLowerCase())
                        );
                    }
                }
            }
        }
    }

    private List<String> findWordInBetween(MentionCandidate firstCandidate, MentionCandidate secondCandidate){
        int left, right;

        if (firstCandidate.getEnd() < secondCandidate.getBegin()) {
            left = firstCandidate.getEnd();
            right = secondCandidate.getBegin();
        } else {
            left = secondCandidate.getEnd();
            right = firstCandidate.getBegin();
        }

        List<String> tokensInBetween = new ArrayList<>();

        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class,
                firstCandidate.getContainedSentence())) {
            if (token.getBegin() > left && token.getEnd() < right){
                String pos = token.getPos();
                if (!Pattern.matches("\\p{Punct}", token.getCoveredText())) {
                    if (!pos.startsWith("N") && !pos.startsWith("V") && !pos.startsWith("J")
                            && !pos.startsWith("RB") && !pos.startsWith("D") && !pos.startsWith("CD")) {
                        tokensInBetween.add(token.getLemma().toLowerCase());
                    }
                }
            }
        }

        return tokensInBetween;
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate candidate) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, MentionCandidate
            secondCandidate, NodeKey secondNodeKey) {

    }
}
