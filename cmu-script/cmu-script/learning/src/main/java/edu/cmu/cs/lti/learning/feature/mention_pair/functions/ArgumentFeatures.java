package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.SimilarityUtils;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/30/15
 * Time: 5:38 AM
 *
 * @author Zhengzhong Liu
 */
public class ArgumentFeatures extends AbstractMentionPairFeatures {
    public ArgumentFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    //TODO add entity coreference

    // TODO this feature is very slow right now!

    @Override
    public void initDocumentWorkspace(JCas context) {
        for (StanfordEntityMention mention : JCasUtil.select(context, StanfordEntityMention.class)) {
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
                token.setNerTag(mention.getEntityType());
            }
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures,
                        List<MentionCandidate> candidates, NodeKey firstNode, NodeKey secondNode) {
        MentionCandidate firstCandidate = candidates.get(firstNode.getIndex());
        MentionCandidate secondCandidate = candidates.get(secondNode.getIndex());

        for (SemanticRelation firstLink : getArgumentLinks(firstCandidate.getHeadWord())) {
            for (SemanticRelation secondLink : getArgumentLinks(secondCandidate.getHeadWord())) {
                extractLinkFeatures(documentContext, rawFeatures, firstLink, secondLink);
            }
        }
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                        List<MentionCandidate> candidates, NodeKey firstNode, NodeKey secondNode) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNode) {

    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> rawFeatures, MentionCandidate
            secondCandidate, NodeKey secondNode) {

    }

    private void extractLinkFeatures(JCas documentContext, TObjectDoubleMap<String> rawFeatures,
                                     SemanticRelation firstLink, SemanticRelation secondLink) {
        addWhenEqualNotNull(rawFeatures, "BothHaveFrameRole", firstLink.getFrameElementName(), secondLink
                .getFrameElementName());

        addWhenEqualNotNull(rawFeatures, "BothHavePropbankRole", firstLink.getPropbankRoleName(), secondLink
                .getPropbankRoleName());

        addPairNotNull(rawFeatures, "ArgumentFrameRole", firstLink.getFrameElementName(), secondLink
                .getFrameElementName());

        addPairNotNull(rawFeatures, "ArgumentPropbankRole", firstLink.getPropbankRoleName(), secondLink
                .getPropbankRoleName());


        ComponentAnnotation argument1 = firstLink.getChildSpan();
        ComponentAnnotation argument2 = secondLink.getChildSpan();

        String argumentText1 = argument1.getCoveredText();
        String argumentText2 = argument2.getCoveredText();

        String roleName1 = getArgumentRoleName(firstLink);
        String roleName2 = getArgumentRoleName(secondLink);

        if (roleName1 != null && roleName2 != null) {
            String rolePair = FeatureUtils.sortedJoin(roleName1, roleName2);
            double argumentDice = SimilarityUtils.getDiceCoefficient(argumentText1, argumentText2);
            if (argumentDice > 0.5) {
                if (roleName1.equals(roleName2)) {
                    addBoolean(rawFeatures, FeatureUtils.formatFeatureName("SimilarArgument", roleName1));
                }
                addBoolean(rawFeatures, FeatureUtils.formatFeatureName("SimilarArgumentWithPair", rolePair));
            }

            String ner1 = getArgumentNer(argument1);
            String ner2 = getArgumentNer(argument2);

            if (ner1 != null && ner2 != null) {
                if (ner1.equals(ner2)) {
                    addBoolean(rawFeatures, FeatureUtils.formatFeatureName("SameNerForRole", rolePair));
                }
                addBoolean(rawFeatures, FeatureUtils.formatFeatureName("RoleNerPair", FeatureUtils.sortedJoin
                        (roleName1 + "_" + ner1, roleName2 + "_" + ner2)
                ));
            }
        }
    }

    private void addWhenEqualNotNull(TObjectDoubleMap<String> rawFeatures, String name, String a, String b) {
        if (a != null && b != null) {
            if (a.equals(b)) {
                addBoolean(rawFeatures, FeatureUtils.formatFeatureName(name, a));
            }
        }
    }

    private void addPairNotNull(TObjectDoubleMap<String> rawFeatures, String name, String a, String b) {
        if (a != null && b != null) {
            addBoolean(rawFeatures, FeatureUtils.formatFeatureName(name, FeatureUtils.sortedJoin(a, b)));
        }
    }

    private List<SemanticRelation> getArgumentLinks(Word token) {
        FSList firstAnnoFs = token.getChildSemanticRelations();
        if (firstAnnoFs != null) {
            return new ArrayList<>(FSCollectionFactory.create(firstAnnoFs, SemanticRelation.class));
        } else {
            return new ArrayList<>();
        }
    }

    private String getArgumentRoleName(SemanticRelation link) {
        String roleName = link.getFrameElementName();
        if (roleName == null) {
            roleName = link.getPropbankRoleName();
        }
        return roleName;
    }

    private String getArgumentNer(ComponentAnnotation argument) {
        String ner = null;
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, argument)) {
            if (token.getNerTag() != null) {
                ner = token.getNerTag();
            }
        }
        return ner;
    }
}
