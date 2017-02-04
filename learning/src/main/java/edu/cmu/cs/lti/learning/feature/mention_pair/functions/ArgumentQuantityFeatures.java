package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.SemanticRelation;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import edu.stanford.nlp.ie.NumberNormalizer;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/17
 * Time: 1:43 PM
 *
 * @author Zhengzhong Liu
 */
public class ArgumentQuantityFeatures extends AbstractMentionPairFeatures {
    public ArgumentQuantityFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, int firstCandidateId, int secondCandidateId) {
        Word firstHead = candidates.get(firstCandidateId).getHeadWord();
        Word secondHead = candidates.get(secondCandidateId).getHeadWord();

        Map<String, Number> firstArgs = getNumberArgument(firstHead);
        Map<String, Number> secondArgs = getNumberArgument(secondHead);

        boolean firstHasNumber = false;

        if (!firstArgs.isEmpty()) {
            featuresNoLabel.put("FirstHasNumber", 1);

            for (Map.Entry<String, Number> firstArgNumber : firstArgs.entrySet()) {
                String arg = firstArgNumber.getKey();
                Number firstNum = firstArgNumber.getValue();

                if (secondArgs.containsKey(arg)) {
                    Number secondNum = secondArgs.get(arg);

                    if (firstNum.doubleValue() > secondNum.doubleValue()) {
                        featuresNoLabel.put(FeatureUtils.formatFeatureName("FirstHasLarger", arg), 1);
                    } else if (firstNum.doubleValue() < secondNum.doubleValue()) {
                        featuresNoLabel.put(FeatureUtils.formatFeatureName("SecondHasLarger", arg), 1);
                    } else {
                        featuresNoLabel.put(FeatureUtils.formatFeatureName("HaveEqual", arg), 1);
                    }
                }
            }
            firstHasNumber = true;
        }

        if (!secondArgs.isEmpty()) {
            featuresNoLabel.put("SecondHasNumber", 1);
            if (firstHasNumber) {
                featuresNoLabel.put("BothHasNumber", 1);
            }
        }

    }

    private Map<String, Number> getNumberArgument(Word head) {
        Map<String, Number> relationWithNumber = new HashMap<>();

        FSList childSemanticRelations = head.getChildSemanticRelations();
        if (childSemanticRelations != null) {
            for (SemanticRelation relation : FSCollectionFactory.create(childSemanticRelations, SemanticRelation
                    .class)) {
                Word relationChild = relation.getChild().getHead();
                Number argNumber = getNumberModifier(relationChild);

                if (argNumber != null) {
                    String frameName = relation.getFrameElementName();
                    String pbName = relation.getPropbankRoleName();

                    if (frameName != null) {
                        relationWithNumber.put(frameName, argNumber);
                    }

                    if (pbName != null) {
                        relationWithNumber.put(pbName, argNumber);
                    }
                }
            }
        }

        return relationWithNumber;
    }

    private Number getNumberModifier(Word argHead) {
        FSList childFs = argHead.getChildDependencyRelations();

        if (childFs != null) {
            for (Dependency dependency : FSCollectionFactory.create(childFs, Dependency.class)) {
                String depType = dependency.getDependencyType();
                if (depType.equals("nummod")) {
                    try {
                        return NumberNormalizer.wordToNumber(dependency.getChild().getLemma());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                } else if (depType.equals("det")) {
                    return 1;
                }
            }
        }
        return null;
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
