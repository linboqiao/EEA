package edu.cmu.cs.lti.script.annotators;

import edu.cmu.cs.lti.script.model.SemaforConstants;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quick and dirty argument extractor based on Semafor and Fanse parsers.
 *
 * @author Zhengzhong Liu
 */
public class EnglishSrlArgumentExtractor extends AbstractLoggingAnnotator {
    public static final String PARAM_ADD_FANSE = "addFanse";
    @ConfigurationParameter(name = PARAM_ADD_FANSE)
    private boolean addFanse;

    public static final String PARAM_ADD_SEMAFOR = "addSemafor";
    @ConfigurationParameter(name = PARAM_ADD_SEMAFOR)
    private boolean addSemafor;

    public static final String PARAM_ADD_DEPENDENCY = "addDependency";
    @ConfigurationParameter(name = PARAM_ADD_DEPENDENCY)
    private boolean addDependency;

    private TokenAlignmentHelper helper = new TokenAlignmentHelper();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        helper.loadStanford2Fanse(aJCas);
        helper.loadFanse2Stanford(aJCas);

        List<Entity> emptyEntities = new ArrayList<>();
        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            if (entity.getEntityMentions().size() == 0) {
                emptyEntities.add(entity);
            }
        }

        for (Entity emptyEntity : emptyEntities) {
            emptyEntity.removeFromIndexes();
        }

        Map<Word, EntityMention> h2Entities = UimaNlpUtils.indexEntityMentions(aJCas);
        Map<SemaforLabel, Pair<String, Map<String, SemaforLabel>>> semaforArguments = getSemaforArguments(aJCas);

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            if (mention.getHeadWord() == null) {
                mention.setHeadWord(UimaNlpUtils.findHeadFromStanfordAnnotation(mention));
            }

            StanfordCorenlpToken headWord = (StanfordCorenlpToken) mention.getHeadWord();
            Map<Word, EventMentionArgumentLink> head2Args = UimaNlpUtils.indexArgs(mention);

            List<EventMentionArgumentLink> argumentLinks = new ArrayList<>(head2Args.values());

            List<SemaforLabel> coveredSemaforLabel = JCasUtil.selectCovered(SemaforLabel.class, headWord);
            Map<StanfordCorenlpToken, String> semafordHeadWord2Role = new HashMap<>();
            Map<String, SemaforLabel> semaforRoles = new HashMap<>();
            for (SemaforLabel label : coveredSemaforLabel) {
                if (semaforArguments.containsKey(label)) {
                    Pair<String, Map<String, SemaforLabel>> frameNameAndRoles = semaforArguments.get(label);
                    semaforRoles = frameNameAndRoles.getValue1();
                    mention.setFrameName(frameNameAndRoles.getValue0());
                    for (Map.Entry<String, SemaforLabel> aSemaforArgument : semaforRoles.entrySet()) {
                        StanfordCorenlpToken argumentHead = UimaNlpUtils.findHeadFromStanfordAnnotation(
                                aSemaforArgument.getValue());
                        String roleName = aSemaforArgument.getKey();
                        if (argumentHead != null) {
                            semafordHeadWord2Role.put(argumentHead, roleName);
                        }
                    }
                }
            }

            if (addFanse) {
                FanseToken headFanseToken = helper.getFanseToken(headWord);
                FSList childSemanticRelations = headFanseToken.getChildSemanticRelations();

                if (childSemanticRelations != null) {
                    for (FanseSemanticRelation childRelation : JCasUtil.select(
                            childSemanticRelations, FanseSemanticRelation.class)) {
                        FanseToken fanseChild = (FanseToken) childRelation.getChild().getHead();
                        StanfordCorenlpToken argumentHead = helper.getStanfordToken(fanseChild);

                        if (argumentHead != null) {
                            EventMentionArgumentLink argumentLink;

                            if (head2Args.containsKey(argumentHead)) {
                                argumentLink = head2Args.get(argumentHead);
                            } else {
                                argumentLink = UimaNlpUtils.createArg(aJCas, h2Entities, mention,
                                        argumentHead.getBegin(), argumentHead.getEnd(), COMPONENT_ID);
                                argumentLinks.add(argumentLink);
                            }
                            argumentLink.setPropbankRoleName(childRelation.getSemanticAnnotation());
                        }
                    }
                }
            }

            if (addSemafor) {
                for (Map.Entry<StanfordCorenlpToken, String> semaforRoleHead : semafordHeadWord2Role.entrySet()) {
                    StanfordCorenlpToken headToken = semaforRoleHead.getKey();
                    String semaforRoleName = semaforRoleHead.getValue();
                    SemaforLabel argumentAnnotation = semaforRoles.get(semaforRoleName);

                    EventMentionArgumentLink argumentLink;

                    if (head2Args.containsKey(headToken)) {
                        argumentLink = head2Args.get(headToken);
                    } else {
                        argumentLink = UimaNlpUtils.createArg(aJCas, h2Entities, mention,
                                argumentAnnotation.getBegin(), argumentAnnotation.getEnd(), COMPONENT_ID);
                        argumentLinks.add(argumentLink);
                    }
                    argumentLink.setFrameElementName(semaforRoleName);
                }
            }

            if (addDependency) {
                VerbBasedEventDetector.createDependencyArgs(aJCas, mention, h2Entities, COMPONENT_ID);
            }

            mention.setArguments(FSCollectionFactory.createFSList(aJCas, argumentLinks));
//            logger.info(String.format("%d arguments for %s", argumentLinks.size(), mention.getCoveredText()));
//            DebugUtils.pause();
        }

        UimaNlpUtils.fixEntityMentions(aJCas, new ArrayList<>(JCasUtil.select(aJCas, EntityMention.class)),
                COMPONENT_ID);
    }

    private Map<SemaforLabel, Pair<String, Map<String, SemaforLabel>>> getSemaforArguments(JCas aJCas) {
        Map<SemaforLabel, Pair<String, Map<String, SemaforLabel>>> semaforArguments = new HashMap<>();

        for (SemaforAnnotationSet annotationSet : JCasUtil.select(aJCas, SemaforAnnotationSet.class)) {
            SemaforLabel targetLabel = null;
            Map<String, SemaforLabel> roleLabels = new HashMap<>();

            for (SemaforLayer layer : JCasUtil.select(annotationSet.getLayers(), SemaforLayer.class)) {
                String layerName = layer.getName();
                if (layerName.equals(SemaforConstants.TARGET_LAYER_NAME)) {
                    for (SemaforLabel label : JCasUtil.select(layer.getLabels(), SemaforLabel.class)) {
                        targetLabel = label;
                    }
                } else if (layerName.equals(SemaforConstants.FRAME_ELEMENT_LAYER_NAME)) {
                    for (SemaforLabel label : JCasUtil.select(layer.getLabels(), SemaforLabel.class)) {
                        roleLabels.put(label.getName(), label);
                    }
                }
            }
            semaforArguments.put(targetLabel, Pair.with(annotationSet.getFrameName(), roleLabels));
        }
        return semaforArguments;
    }
}