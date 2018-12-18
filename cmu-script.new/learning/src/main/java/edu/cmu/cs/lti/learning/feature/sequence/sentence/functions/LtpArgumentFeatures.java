package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.ResourceUtils;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/22/16
 * Time: 2:27 PM
 *
 * @author Zhengzhong Liu
 */
public class LtpArgumentFeatures extends SequenceFeatureWithFocus<StanfordCorenlpToken> {
    private final Map<String, String> word2CilinId;
    private Map<StanfordCorenlpToken, LtpToken> s2l;

    private Map<StanfordCorenlpToken, String> tokenStanfordNerType;

    private Map<StanfordCorenlpToken, String> tokenLtpNerType;

    public LtpArgumentFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);

        word2CilinId = ResourceUtils.readCilin(new File(generalConfig.get("edu.cmu.cs.lti.resource.dir"),
                generalConfig.get("edu.cmu.cs.lti.synonym.cilin")));
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        TokenAlignmentHelper helper = new TokenAlignmentHelper();
        s2l = helper.getType2TypeMapping(context, StanfordCorenlpToken.class, LtpToken.class);


        tokenStanfordNerType = new HashMap<>();
        for (StanfordEntityMention em : JCasUtil.select(context, StanfordEntityMention.class)) {
            StanfordCorenlpToken emHead = UimaNlpUtils.findHeadFromStanfordAnnotation(em);
            if (emHead != null) {
                tokenStanfordNerType.put(emHead, em.getEntityType());
            }
        }

        tokenLtpNerType = new HashMap<>();
        for (LtpEntityMention em : JCasUtil.select(context, LtpEntityMention.class)) {
            StanfordCorenlpToken emHead = UimaNlpUtils.findHeadFromStanfordAnnotation(em);
            if (emHead != null) {
                tokenLtpNerType.put(emHead, em.getEntityType());
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

        LtpToken focusToken = s2l.get(sequence.get(focus));

        if (focusToken != null) {
            extractArgumentFeatures(focusToken, nodeFeatures);
        }
        // There are quite a lot segmentation inconsistency.
//        else {
//            logger.warn("Cannot find focus for " + sequence.get(focus).getCoveredText() + " " + sequence.get(focus)
//                    .getBegin() + " " + sequence.get(focus).getEnd());
//        }
    }

    @Override
    public void extractGlobal(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String>
            globalFeatures, List<MentionKey> knownStates, MentionKey currentState) {

    }

    private <T extends AnnotationFS> void extractArgumentFeatures(LtpToken focusToken, TObjectDoubleMap<String>
            nodeFeatures) {
        FSList childSemanticRelations = focusToken.getChildSemanticRelations();

        boolean hasCoreArgument = false;

        if (childSemanticRelations != null) {
            Collection<LtpSemanticRelation> relations = FSCollectionFactory.create(childSemanticRelations,
                    LtpSemanticRelation.class);
            for (LtpSemanticRelation relation : relations) {
                SemanticArgument argument = relation.getChild();
                String relationType = relation.getPropbankRoleName();
                addToFeatures(nodeFeatures, "ContainsArgumentType_" + relationType, 1);

                if (relationType.equals("A0") || relationType.equals("A1") || relationType.equals("A3")) {
                    hasCoreArgument = true;
                }

                StanfordCorenlpToken argumentHead = UimaNlpUtils.findHeadFromStanfordAnnotation(argument);

                if (argumentHead != null) {
                    String argumentHeadText = argumentHead.getCoveredText();
                    addToFeatures(nodeFeatures, String.format("%s_Arghead=%s", relationType, argumentHeadText), 1);

                    if (word2CilinId.containsKey(argumentHeadText)) {
                        String id = word2CilinId.get(argumentHeadText);
                        for (String lv : getCilinLevels(id)) {
                            addToFeatures(nodeFeatures, String.format("%s_ArgHeadCilinId=%s", relationType, lv), 1);
                        }
                    }
                }

                String argumentHeadStanfordNerType = tokenStanfordNerType.get(argumentHead);
                String argumentHeadLtpNerType = tokenLtpNerType.get(argumentHead);

                if (argumentHeadStanfordNerType != null) {
                    addToFeatures(nodeFeatures, String.format("%s_ArgHeadStanfordNer=%s", relationType,
                            argumentHeadStanfordNerType), 1);
                }
                if (argumentHeadLtpNerType != null) {
                    addToFeatures(nodeFeatures, String.format("%s_ArgHeadLtpNer=%s", relationType,
                            argumentHeadLtpNerType), 1);
                }

                Set<String> containedEmType = JCasUtil.selectCovered(EntityMention.class, argument).stream().map
                        (EntityMention::getEntityType).collect(Collectors.toSet());

                for (String type : containedEmType) {
                    addToFeatures(nodeFeatures, String.format("%s_ArgContainsNer_%s", relationType, type), 1);
                }


            }
        }

        if (hasCoreArgument) {
            addToFeatures(nodeFeatures, "HasCoreArgument", 1);
        }
    }

    private List<String> getCilinLevels(String fullId) {
        List<String> levelIds = new ArrayList<>();
        levelIds.add("Level2_" + fullId.substring(0, 2));
        levelIds.add("Level3_" + fullId.substring(0, 4));
        levelIds.add("Level4_" + fullId.substring(0, 5));
        levelIds.add("full_" + fullId);
        return levelIds;
    }
}
