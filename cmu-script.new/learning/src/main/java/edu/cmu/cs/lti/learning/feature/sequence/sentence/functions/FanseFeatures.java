package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/12/15
 * Time: 3:10 PM
 *
 * @author Zhengzhong Liu
 */
public class FanseFeatures extends SequenceFeatureWithFocus<StanfordCorenlpToken> {
    private TokenAlignmentHelper align;

    private List<BiConsumer<TObjectDoubleMap<String>, FanseToken>> headTemplates;

    private List<BiConsumer<TObjectDoubleMap<String>, FanseSemanticRelation>> argumentTemplates;

    private boolean loadWordnetSenseTokens;

    private boolean loadNerTokens;

    private Map<Word, String> fanseToken2WordnetType;

    public FanseFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);

        align = new TokenAlignmentHelper();

        headTemplates = new ArrayList<>();
        argumentTemplates = new ArrayList<>();

        for (String templateName : featureConfig.getList(featureConfigKey("templates"))) {
            switch (templateName) {
                case "FanseHeadSense":
                    headTemplates.add(this::fanseHeadSenseTemplate);
                    break;
                case "FanseArgumentRole":
                    argumentTemplates.add(this::fanseArgumentRoles);
                    break;
                case "FanseArgumentNer":
                    argumentTemplates.add(this::fanseArgumentNer);
                    loadNerTokens = true;
                    break;
                case "FanseArgumentLemma":
                    argumentTemplates.add(this::fanseArgumentLemma);
                    break;
                case "FanseArgumentWordNetSense":
                    argumentTemplates.add((this::fanseArgumentWordNetSense));
                    loadWordnetSenseTokens = true;
                    break;
                default:
                    logger.warn(String.format("Template [%s] not recognized.", templateName));
            }
        }
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        align.loadStanford2Fanse(context);
        // Set types to each token for easy feature extraction.
        if (loadNerTokens) {
            for (StanfordEntityMention mention : JCasUtil.select(context, StanfordEntityMention.class)) {
                String entityType = mention.getEntityType();
                for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
                    token.setNerTag(entityType);
                }
            }
        }

        if (loadWordnetSenseTokens) {
            fanseToken2WordnetType = new HashMap<>();
            for (WordNetBasedEntity anno : JCasUtil.select(context, WordNetBasedEntity.class)) {
                for (FanseToken token : JCasUtil.selectCovered(FanseToken.class, anno)) {
                    fanseToken2WordnetType.put(token, anno.getSense());
                }
            }
        }
    }

    @Override
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> nodeFeatures,
                        Table<Pair<Integer, Integer>, String, Double> edgeFeatures) {
        if (focus > sequence.size() - 1 || focus < 0) {
            return;
        }
        StanfordCorenlpToken token = sequence.get(focus);

        FanseToken fanseToken = align.getFanseToken(token);

        if (fanseToken != null) {
            headTemplates.forEach(t -> t.accept(nodeFeatures, fanseToken));
        }

        FSList childRelations = token.getChildSemanticRelations();

        if (childRelations != null) {
            for (FanseSemanticRelation r : FSCollectionFactory.create(childRelations, FanseSemanticRelation.class)) {
                argumentTemplates.forEach(t -> t.accept(nodeFeatures, r));
            }
        }
    }

    private void fanseHeadLemmaTemplate(TObjectDoubleMap<String> features, FanseToken token) {
        addToFeatures(features, FeatureUtils.formatFeatureName("FanseHeadLemma", token.getLemma()), 1);
    }

    private void fanseHeadSenseTemplate(TObjectDoubleMap<String> features, FanseToken token) {
        if (token.getLexicalSense() != null) {
            addToFeatures(features, FeatureUtils.formatFeatureName("FanseHeadSense", token.getLexicalSense()), 1);
        }
    }

    private void fanseArgumentRoles(TObjectDoubleMap<String> features, FanseSemanticRelation relation) {
        addToFeatures(features,
                FeatureUtils.formatFeatureName("FanseArgumentRole", relation.getSemanticAnnotation()), 1);
    }


    private void fanseArgumentLemma(TObjectDoubleMap<String> features, FanseSemanticRelation relation) {
        addToFeatures(features,
                FeatureUtils.formatFeatureName("FanseArgumentLemma", relation.getChild().getHead().getLemma()), 1);
    }

    private void fanseArgumentNer(TObjectDoubleMap<String> features, FanseSemanticRelation relation) {
        addToFeatures(features, FeatureUtils.formatFeatureName("FanseArgumentNer",
                relation.getChild().getHead().getNerTag()), 1);
    }

    private void fanseArgumentWordNetSense(TObjectDoubleMap<String> features, FanseSemanticRelation relation) {
        Word child = relation.getChild().getHead();
        if (fanseToken2WordnetType.containsKey(child)) {
            addToFeatures(features, FeatureUtils.formatFeatureName("FanseArgumentWordNetSense",
                    fanseToken2WordnetType.get(child)), 1);
        }
    }

    @Override
    public void extractGlobal(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String>
            globalFeatures, List<MentionKey> knownStates, MentionKey currentState) {

    }
}
