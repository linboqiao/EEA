package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
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

    @Override
    public void initDocumentWorkspace(JCas context) {
        for (StanfordEntityMention mention : JCasUtil.select(context, StanfordEntityMention.class)) {
            for (StanfordCorenlpToken token : JCasUtil.selectCovering(StanfordCorenlpToken.class, mention)) {
                token.setNerTag(mention.getEntityType());
            }
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention firstAnno,
                        EventMention secondAnno) {
        for (EventMentionArgumentLink firstLink : getArgumentLinks(firstAnno)) {
            for (EventMentionArgumentLink secondLink : getArgumentLinks(secondAnno)) {
                extractLinkFeatures(documentContext, rawFeatures, firstLink, secondLink);
            }
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention secondAnno) {

    }

    private void extractLinkFeatures(JCas documentContext, TObjectDoubleMap<String> rawFeatures,
                                     EventMentionArgumentLink firstLink, EventMentionArgumentLink secondLink) {
        addWhenEqualNotNull(rawFeatures, "BothHaveFrameRole", firstLink.getFrameElementName(), secondLink
                .getFrameElementName());

        addWhenEqualNotNull(rawFeatures, "BothHavePropbankRole", firstLink.getPropbankRoleName(), secondLink
                .getPropbankRoleName());

        addPairNotNull(rawFeatures, "ArgumentFrameRole", firstLink.getFrameElementName(), secondLink
                .getFrameElementName());

        addPairNotNull(rawFeatures, "ArgumentPropbankRole", firstLink.getPropbankRoleName(), secondLink
                .getPropbankRoleName());


        EntityMention argument1 = firstLink.getArgument();
        EntityMention argument2 = secondLink.getArgument();

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

    private List<EventMentionArgumentLink> getArgumentLinks(EventMention mention) {
        FSList firstAnnoFs = mention.getAgentLinks();
        if (firstAnnoFs != null) {
            return new ArrayList<>(FSCollectionFactory.create(firstAnnoFs, EventMentionArgumentLink.class));
        } else {
            return new ArrayList<>();
        }
    }

    private String getArgumentRoleName(EventMentionArgumentLink link) {
        String roleName = link.getFrameElementName();
        if (roleName == null) {
            roleName = link.getPropbankRoleName();
        }

        return roleName;
    }

    private String getArgumentNer(EntityMention argument1) {
        String ner1 = null;
        for (StanfordCorenlpToken token : JCasUtil.selectCovering(StanfordCorenlpToken.class, argument1)) {
            if (token.getNerTag() != null) {
                ner1 = token.getNerTag();
            }
        }
        return ner1;
    }
}
