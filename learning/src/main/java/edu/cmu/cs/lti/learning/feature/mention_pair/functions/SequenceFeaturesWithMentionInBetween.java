package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/31/17
 * Time: 1:53 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeaturesWithMentionInBetween extends AbstractMentionPairFeatures {
    private TObjectIntHashMap<Word> head2Entity;

    public SequenceFeaturesWithMentionInBetween(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        head2Entity = new TObjectIntHashMap<>();
        int entityId = 0;
        for (Entity entity : JCasUtil.select(context, Entity.class)) {
            for (int i = 0; i < entity.getEntityMentions().size(); i++) {
                head2Entity.put(entity.getEntityMentions(i).getHead(), entityId);
            }
            entityId++;
        }
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

        int firstCandIndex = MentionGraph.getCandidateIndex(firstNodeKey.getNodeIndex());
        int secondCandIndex = MentionGraph.getCandidateIndex(secondNodeKey.getNodeIndex());
        MentionCandidate firstCandidate = candidates.get(firstCandIndex);
        MentionCandidate secondCandidate = candidates.get(secondCandIndex);

        int firstSentenceIndex = firstCandidate.getContainedSentence().getIndex();
        int secondSentenceIndex = secondCandidate.getContainedSentence().getIndex();

        String firstRealis = firstNodeKey.getRealis();
        String secondRealis = secondNodeKey.getRealis();

        int sentDist = Math.abs(secondSentenceIndex - firstSentenceIndex);

        int leftCandIndex = Math.min(firstCandIndex, secondCandIndex);
        int rightCandIndex = Math.max(firstCandIndex, secondCandIndex);

        if (sentDist < 3) {
            if (!firstNodeKey.getRealis().equals("Other") && firstRealis.equals(secondRealis)) {
                int numMib = rightCandIndex - leftCandIndex - 1;
                for (int mibIndex = leftCandIndex + 1; mibIndex < rightCandIndex; mibIndex++) {
                    for (NodeKey nodeKey : candidates.get(mibIndex).asKey()) {
                        String mibType = nodeKey.getMentionType();

                        addBoolean(featuresNeedLabel,
                                String.format("MentionTypePair_Realis=%s_MentionInBetween=%s::%s:%s",
                                        firstRealis, mibType, firstType, secondType)
                        );
                        addBoolean(featuresNeedLabel,
                                String.format("HeadwordPair_Realis=%s_MentionInBetween=%s::%s:%s",
                                        firstRealis, mibType,
                                        firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                        secondCandidate.getHeadWord().getLemma().toLowerCase())
                        );

                        if (firstCandidate.getBegin() < secondCandidate.getBegin()) {
                            addBoolean(featuresNeedLabel,
                                    String.format("ForwardMentionTypePair_Realis=%s_MentionInBetween=%s::%s:%s",
                                            firstRealis, mibType, firstType, secondType)
                            );
                            addBoolean(featuresNeedLabel,
                                    String.format("ForwardHeadwordPair_Realis=%s_MentionInBetween=%s::%s:%s",
                                            firstRealis, mibType,
                                            firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                            secondCandidate.getHeadWord().getLemma().toLowerCase())
                            );
                        } else {
                            addBoolean(featuresNeedLabel,
                                    String.format("BackwardMentionTypePair_Realis=%s_MentionInBetween=%s::%s:%s",
                                            firstRealis, mibType, firstType, secondType)
                            );
                            addBoolean(featuresNeedLabel,
                                    String.format("BackwardHeadwordPair_Realis=%s_MentionInBetween=%s::%s:%s",
                                            firstRealis, mibType,
                                            firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                            secondCandidate.getHeadWord().getLemma().toLowerCase())
                            );
                        }
                    }
                }

                addBoolean(featuresNeedLabel,
                        String.format("MentionTypePair_Realis=%s_NumMib=%d::%s:%s",
                                firstRealis, numMib, firstType, secondType)
                );
                addBoolean(featuresNeedLabel,
                        String.format("HeadwordPair_Realis=%s_NumMib=%d::%s:%s",
                                firstRealis, numMib,
                                firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                secondCandidate.getHeadWord().getLemma().toLowerCase())
                );


                if (firstCandidate.getBegin() < secondCandidate.getBegin()) {
                    addBoolean(featuresNeedLabel,
                            String.format("ForwardMentionTypePair_Realis=%s_NumMib=%d::%s:%s",
                                    firstRealis, numMib, firstType, secondType)
                    );
                    addBoolean(featuresNeedLabel,
                            String.format("ForwardHeadwordPair_Realis=%s_NumMib=%d::%s:%s",
                                    firstRealis, numMib,
                                    firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                    secondCandidate.getHeadWord().getLemma().toLowerCase())
                    );
                }else{
                    addBoolean(featuresNeedLabel,
                            String.format("BackwardMentionTypePair_Realis=%s_NumMib=%d::%s:%s",
                                    firstRealis, numMib, firstType, secondType)
                    );
                    addBoolean(featuresNeedLabel,
                            String.format("BackwardHeadwordPair_Realis=%s_NumMib=%d::%s:%s",
                                    firstRealis, numMib,
                                    firstCandidate.getHeadWord().getLemma().toLowerCase(),
                                    secondCandidate.getHeadWord().getLemma().toLowerCase())
                    );
                }
            }
        }
    }


    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate candidate) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, MentionCandidate
            secondCandidate, NodeKey secondNodeKey) {

    }
}
