package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.SemanticRelation;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
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
 * Date: 1/25/17
 * Time: 1:16 PM
 *
 * @author Zhengzhong Liu
 */
public class ArgCoreferenceFeatures extends AbstractMentionPairFeatures {
    TObjectIntMap<Word> head2Entity;

    public ArgCoreferenceFeatures(Configuration generalConfig, Configuration featureConfig) {
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
        Word firstHead = candidates.get(firstCandidateId).getHeadWord();
        Word secondHead = candidates.get(secondCandidateId).getHeadWord();

        Map<Word, String> firstDeps = getDependencies(firstHead);
        Map<Word, String> secondDeps = getDependencies(secondHead);

        pairCorefFeatures(firstDeps, secondDeps, featuresNoLabel);

        List<Function<SemanticRelation, String>> possibleRoles = new ArrayList<>();
        possibleRoles.add(SemanticRelation::getPropbankRoleName);
        possibleRoles.add(SemanticRelation::getFrameElementName);

        for (Function<SemanticRelation, String> role : possibleRoles) {
            Map<Word, String> firstRoles = getPbArgs(firstHead, role);
            Map<Word, String> secondRoles = getPbArgs(secondHead, role);
            pairCorefFeatures(firstRoles, secondRoles, featuresNoLabel);
        }
    }

    private void pairCorefFeatures(Map<Word, String> first, Map<Word, String> second, TObjectDoubleMap<String>
            features) {
        for (Map.Entry<Word, String> firstArgType : first.entrySet()) {
            Word firstHead = firstArgType.getKey();
            String firstType = firstArgType.getValue();
            int firstEntity = -1;
            if (head2Entity.containsKey(firstHead)) {
                firstEntity = head2Entity.get(firstHead);
            }

            for (Map.Entry<Word, String> secondArgType : second.entrySet()) {
                Word secondHead = secondArgType.getKey();
                String secondType = secondArgType.getValue();
                int secondEntity = -1;
                if (head2Entity.containsKey(secondHead)) {
                    secondEntity = head2Entity.get(secondHead);
                }

                if (firstEntity != -1 && secondEntity != -1) {
                    if (firstEntity == secondEntity) {
                        features.put(FeatureUtils.formatFeatureName("CorefOn", firstType + "_" + secondType), 1);
                    }
                } else {
                    if (firstHead.equals(secondHead)) {
                        features.put(FeatureUtils.formatFeatureName("CorefOn", firstType + "_" + secondType), 1);
                    }
                }
            }
        }
    }

    private Map<Word, String> getDependencies(Word headWord) {
        Map<Word, String> dep2Type = new HashMap<>();

        FSList depsFS = headWord.getChildDependencyRelations();
        if (depsFS != null) {
            for (Dependency dependency : FSCollectionFactory.create(depsFS, Dependency.class)) {
                dep2Type.put(dependency.getChild(), dependency.getDependencyType());
            }
        }
        return dep2Type;
    }

    private Map<Word, String> getPbArgs(Word headWord, Function<SemanticRelation, String> getRole) {
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

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                   List<MentionCandidate> candidates, NodeKey firstNodeKey, NodeKey secondNodeKey) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate candidate) {

    }

    @Override
    public void extractNodeRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel, MentionCandidate
            secondCandidate, NodeKey secondNodeKey) {

    }
}
