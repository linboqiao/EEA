package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/17
 * Time: 10:19 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeaturesWithDirectDependency extends AbstractMentionPairFeatures {
    public SequenceFeaturesWithDirectDependency(Configuration generalConfig, Configuration featureConfig) {
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

        int sentDist = Math.abs(secondSentenceIndex - firstSentenceIndex);

        if (sentDist == 0) {
            if (!firstNodeKey.getRealis().equals("Other") && firstRealis.equals(secondRealis)) {
                String forwardDep = checkDirectDependency(firstCandidate, secondCandidate);

                if (forwardDep != null){
                    addBoolean(featuresNeedLabel,
                            String.format("MentionTypePair_ForwardDep=%s_Realis=%s::%s:%s", forwardDep,
                                    firstRealis, firstType, secondType)
                    );
                    addBoolean(featuresNeedLabel,
                            String.format("HeadwordPair_ForwardDep=%s_Realis=%s::%s:%s", forwardDep, firstRealis,
                                    firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                    secondCandidate.getHeadWord().getLemma().toLowerCase())
                    );
                }else{
                    String backwardDep = checkDirectDependency(secondCandidate, firstCandidate);
                    if (backwardDep != null){
                        addBoolean(featuresNeedLabel,
                                String.format("MentionTypePair_BackwardDep=%s_Realis=%s::%s:%s", backwardDep,
                                        firstRealis, firstType, secondType)
                        );
                        addBoolean(featuresNeedLabel,
                                String.format("HeadwordPair_BackwardDep=%s_Realis=%s::%s:%s", backwardDep, firstRealis,
                                        firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                        secondCandidate.getHeadWord().getLemma().toLowerCase())
                        );
                    }
                }
            }
        }
    }

    private String checkDirectDependency(MentionCandidate firstCandidate, MentionCandidate secondCandidate) {
        Word firstHead = firstCandidate.getHeadWord();
        Word secondHead = secondCandidate.getHeadWord();
        return checkDep(firstHead, secondHead);
    }

    private String checkDep(Word firstWord, Word secondWord) {
        FSList firstChildren = firstWord.getChildDependencyRelations();

        if (firstChildren != null) {
            for (Dependency dep : FSCollectionFactory.create(firstChildren, Dependency.class)) {
                if (dep.getChild().equals(secondWord)) {
                    return dep.getDependencyType();
                }
            }
        }
        return null;
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate candidate) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, MentionCandidate
            secondCandidate, NodeKey secondNodeKey) {

    }
}
