package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.SemanticArgument;
import edu.cmu.cs.lti.script.type.SemanticRelation;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/17
 * Time: 12:32 PM
 *
 * @author Zhengzhong Liu
 */
public class TemporalFeatures extends AbstractMentionPairFeatures {
    public TemporalFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, int firstCandidateId, int secondCandidateId) {
        MentionCandidate firstCandidate = candidates.get(firstCandidateId);
        MentionCandidate secondCandidate = candidates.get(secondCandidateId);

        Map<String, SemanticArgument> firstPbArgs = new HashMap<>();
        Map<String, SemanticArgument> secondPbArgs = new HashMap<>();

        Map<String, SemanticArgument> firstFnArgs = new HashMap<>();
        Map<String, SemanticArgument> secondFnArgs = new HashMap<>();

        getChildSemanticLinks(firstCandidate.getHeadWord(), firstPbArgs, firstFnArgs);
        getChildSemanticLinks(secondCandidate.getHeadWord(), secondPbArgs, secondFnArgs);

        temporalArgumentFeatures(firstPbArgs, secondPbArgs, "ARGM-TMP", featuresNoLabel);
        temporalArgumentFeatures(firstFnArgs, secondFnArgs, "Time", featuresNoLabel);

        extractTensePairFeatures(firstCandidate.getHeadWord(), secondCandidate.getHeadWord(), featuresNoLabel);
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

    private void getChildSemanticLinks(Word token, Map<String, SemanticArgument> propbankArgs,
                                       Map<String, SemanticArgument> framenetArgs) {
        FSList childSemanticFS = token.getChildSemanticRelations();
        if (childSemanticFS != null) {
            Collection<SemanticRelation> childSemanticLinks = FSCollectionFactory.create(childSemanticFS,
                    SemanticRelation.class);
            for (SemanticRelation childSemanticLink : childSemanticLinks) {
                propbankArgs.put(childSemanticLink.getFrameElementName(), childSemanticLink.getChild());
                framenetArgs.put(childSemanticLink.getPropbankRoleName(), childSemanticLink.getChild());
            }
        }
    }

    private void extractTensePairFeatures(Word firstHead, Word secondHead, TObjectDoubleMap<String> features) {
        Word firstAntecedent = getAntecedent(firstHead, 2);
        Word secondAntecedent = getAntecedent(secondHead, 2);

        features.put(FeatureUtils.formatFeatureName("AntecedentPosPair",
                firstAntecedent.getPos() + "_" + secondAntecedent.getPos()), 1);
    }

    private Word getAntecedent(Word word, int level) {
        if (level > 0) {
            FSList headDepFS = word.getHeadDependencyRelations();
            if (headDepFS != null) {
                for (Dependency dependency : FSCollectionFactory.create(headDepFS, Dependency.class)) {
                    if (dependency.getHead().getPos().startsWith("V")) {
                        return getAntecedent(dependency.getHead(), level - 1);
                    }
                }
            }
        }
        return word;
    }

    private void temporalArgumentFeatures(Map<String, SemanticArgument> firstArgs,
                                          Map<String, SemanticArgument> secondArgs,
                                          String targetArg,
                                          TObjectDoubleMap<String> features) {
        String firstArgTmp = null;
        if (firstArgs.containsKey(targetArg)) {
            firstArgTmp = firstArgs.get(targetArg).getHead().getLemma();
            features.put(FeatureUtils.formatFeatureName("First" + targetArg, firstArgTmp), 1);
            features.put("FirstHas" + targetArg, 1);
        }

        String secondArgTmp = null;
        if (secondArgs.containsKey(targetArg)) {
            secondArgTmp = secondArgs.get(targetArg).getHead().getLemma();
            features.put(FeatureUtils.formatFeatureName("Second" + targetArg, secondArgTmp), 1);
            features.put("SecondHas" + targetArg, 1);
        }

        if (firstArgTmp != null && secondArgTmp != null) {
            features.put("BothHas" + targetArg, 1);
            features.put(FeatureUtils.formatFeatureName(targetArg + "Pair", firstArgTmp + "_" + secondArgTmp), 1);
        }
    }
}
