package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.SemanticRelation;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/31/17
 * Time: 1:53 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeaturesWithArgumentConstraint extends AbstractMentionPairFeatures {
    private TObjectIntHashMap<Word> head2Entity;

    public SequenceFeaturesWithArgumentConstraint(Configuration generalConfig, Configuration featureConfig) {
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

        MentionCandidate firstCandidate = candidates.get(MentionGraph.getCandidateIndex(firstNodeKey.getNodeIndex()));
        MentionCandidate secondCandidate = candidates.get(MentionGraph.getCandidateIndex(secondNodeKey.getNodeIndex()));

        int firstSentenceIndex = firstCandidate.getContainedSentence().getIndex();
        int secondSentenceIndex = secondCandidate.getContainedSentence().getIndex();

        String firstRealis = firstNodeKey.getRealis();
        String secondRealis = secondNodeKey.getRealis();

        int sentDist = Math.abs(secondSentenceIndex - firstSentenceIndex);

        if (sentDist < 3) {
            if (!firstNodeKey.getRealis().equals("Other") && firstRealis.equals(secondRealis)) {

                Word firstHead = firstCandidate.getHeadWord();
                Word secondHead = secondCandidate.getHeadWord();

                List<Function<SemanticRelation, String>> possibleRoles = new ArrayList<>();
                possibleRoles.add(SemanticRelation::getPropbankRoleName);
                possibleRoles.add(SemanticRelation::getFrameElementName);

                for (Function<SemanticRelation, String> role : possibleRoles) {
                    Map<Word, String> firstRoles = getArgs(firstHead, role);
                    Map<Word, String> secondRoles = getArgs(secondHead, role);

                    pairCorefFeatures(firstRoles, secondRoles,
                            String.format("MentionTypePair_%s:%s", firstType, secondType),
                            featuresNeedLabel);
                    pairCorefFeatures(firstRoles, secondRoles,
                            String.format("HeadWordPair_%s:%s",
                                    firstHead.getLemma().toLowerCase(), secondHead.getLemma().toLowerCase()),
                            featuresNeedLabel);
                    // Analysis seems to show that forward backward does not matter here.
                }
            }
        }
    }

    private void pairCorefFeatures(Map<Word, String> first, Map<Word, String> second, String baseFeatureName,
                                   TObjectDoubleMap<String> features) {
        for (Map.Entry<Word, String> firstArgType : first.entrySet()) {
            Word firstHead = firstArgType.getKey();
            String firstRole = firstArgType.getValue();
            int firstEntity = -1;
            if (head2Entity.containsKey(firstHead)) {
                firstEntity = head2Entity.get(firstHead);
            }

            for (Map.Entry<Word, String> secondArgType : second.entrySet()) {
                Word secondHead = secondArgType.getKey();
                String secondRole = secondArgType.getValue();
                int secondEntity = -1;
                if (head2Entity.containsKey(secondHead)) {
                    secondEntity = head2Entity.get(secondHead);
                }

                if (firstEntity != -1 && secondEntity != -1) {
                    if (firstEntity == secondEntity) {
                        addBoolean(features, String.format("%s::CorefOn::%s:%s", baseFeatureName, firstRole,
                                secondRole));
                    }
                } else {
                    if (firstHead.equals(secondHead)) {
                        addBoolean(features, String.format("%s::CorefOn::%s:%s", baseFeatureName, firstRole,
                                secondRole));
                    }
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

    private Map<Word, String> getArgs(Word headWord, Function<SemanticRelation, String> getRole) {
        Map<Word, String> arg2Type = new HashMap<>();

        FSList argsFS = headWord.getChildSemanticRelations();

        if (argsFS != null) {
            for (SemanticRelation relation : FSCollectionFactory.create(argsFS, SemanticRelation.class)) {
                String role = getRole.apply(relation);
                if (role != null) {
                    arg2Type.put(relation.getChild().getHead(), role);
                }
            }
        }
        return arg2Type;
    }
}
