package edu.cmu.cs.lti.learning.feature.sequence.document.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/24/16
 * Time: 2:41 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionPairFeatures extends SequenceFeatureWithFocus<StanfordCorenlpToken> {
    private TObjectIntMap<StanfordCorenlpToken> token2EntityId;
    private Map<StanfordCorenlpToken, String> triggerToFrameName;
    private Set<String> targetDeps = new HashSet<>();

    private TObjectIntMap<StanfordCorenlpToken> token2SentenceId;

    private boolean sameLemmaFeature;
    private boolean shareFrameFeature;
    private boolean shareArgumentFeature;
    private boolean dependencyFeature;

    public MentionPairFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);

//        targetDeps.add("dobj");
        targetDeps.add("advcl");
        targetDeps.add("conj:and");
        targetDeps.add("xcomp");

        for (String templateName : featureConfig.getList(this.getClass().getSimpleName() + ".templates")) {
            switch (templateName) {
                case "SameLemma":
                    sameLemmaFeature = true;
                    break;
                case "ShareFrame":
                    shareFrameFeature = true;
                    break;
                case "ShareArgument":
                    shareArgumentFeature = true;
                    break;
                case "Dependency":
                    dependencyFeature = true;
                    break;
                default:
                    logger.warn("Template " + templateName + " not recognized.");
            }
        }
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        token2EntityId = new TObjectIntHashMap<>();

        int entityId = 0;
        for (Entity entity : JCasUtil.select(context, Entity.class)) {
            FSArray entityMentionsFS = entity.getEntityMentions();
            if (entityMentionsFS != null) {
                for (StanfordEntityMention mention : FSCollectionFactory.create(entityMentionsFS,
                        StanfordEntityMention.class)) {
                    token2EntityId.put((StanfordCorenlpToken) mention.getHead(), entityId);
                }
            }
            entityId++;
        }
        readFrames(context);


        token2SentenceId = new TObjectIntHashMap<>();
        int sentId = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(context, StanfordCorenlpSentence.class)) {
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence)) {
                token2SentenceId.put(token, sentId);
            }
            sentId++;
        }

    }

    @Override
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> nodeFeatures,
                        Table<Pair<Integer, Integer>, String, Double> edgeFeatures) {

    }

    @Override
    public void extractGlobal(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> globalFeatures,
                              List<MentionKey> knownStates, MentionKey currentState) {
        if (currentState.getCombinedType().equals(ClassAlphabet.noneOfTheAboveClass)) {
            // Only when this is a non-NONE mention, we start extracting features.
            return;
        }

        StanfordCorenlpToken focusHead = sequence.get(focus);

        List<MentionKey> sharedDepMentions = new ArrayList<>();
        List<MentionKey> corefDepMentions = new ArrayList<>();

        findSharedDeps(focusHead, knownStates, "dobj", sharedDepMentions, corefDepMentions);

        if (shareArgumentFeature) {
            for (MentionKey sharedDepMention : sharedDepMentions) {
                addToFeatures(globalFeatures, FeatureUtils.formatFeatureName("MentionPair_ShareDep_dobj",
                                sharedDepMention.getCombinedType()), 1);

//                logger.info("Their object shared");
//                logger.info(focusHead.getCoveredText());
//                logger.info(sharedDepMention.getHeadWord().getCoveredText());

            }

            for (MentionKey corefDepMention : corefDepMentions) {
                addToFeatures(globalFeatures, FeatureUtils.formatFeatureName("MentionPair_CorefOnDep_dobj",
                                corefDepMention.getCombinedType()), 1);
//                logger.info("Their object corefer");
//                logger.info(focusHead.getCoveredText());
//                logger.info(corefDepMention.getHeadWord().getCoveredText());

            }
        }

        findSharedDeps(focusHead, knownStates, "nsubj", sharedDepMentions, corefDepMentions);

        List<MentionKey> sameSentMentions = findSameSentence(focusHead, knownStates);

        Set<MentionKey> allMentions = new HashSet<>();
        allMentions.addAll(sharedDepMentions);
        allMentions.addAll(corefDepMentions);
        allMentions.addAll(sameSentMentions);

        if (allMentions.size() == 0) {
            return;
        }

        if (dependencyFeature) {
            Map<StanfordCorenlpToken, String> children = getDependencyChildren(focusHead);
            Map<StanfordCorenlpToken, String> govners = getDependencyGovners(focusHead);
            addDependencyFeatures(children, govners, allMentions, globalFeatures);
        }
        if (sameLemmaFeature) {
            sameLemma(focusHead, allMentions, globalFeatures);
        }
        if (shareFrameFeature) {
            shareFrame(focusHead, allMentions, globalFeatures);
        }
    }

    private void shareFrame(StanfordCorenlpToken focusHead, Collection<MentionKey> knownStates,
                            TObjectDoubleMap<String> globalFeatures) {
        for (MentionKey previousMention : knownStates) {
            if (previousMention.getCombinedType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                continue;
            }

            StanfordCorenlpToken previousHead = (StanfordCorenlpToken) previousMention.getHeadWord();

            String previousFrame = triggerToFrameName.get(previousHead);
            String focusFrame = triggerToFrameName.get(focusHead);

            if (previousFrame != null && focusFrame != null && previousFrame.equals(focusFrame)) {
                addToFeatures(globalFeatures,
                        FeatureUtils.formatFeatureName("MentionPair_ShareFrameType", previousMention.getCombinedType()),
                        1);
            }
        }
    }

    private void sameLemma(StanfordCorenlpToken focusHead, Collection<MentionKey> knownStates,
                           TObjectDoubleMap<String> globalFeatures) {
        for (MentionKey previousMention : knownStates) {
            if (previousMention.getCombinedType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                continue;
            }

            StanfordCorenlpToken previousHead = (StanfordCorenlpToken) previousMention.getHeadWord();
            if (previousHead.getLemma().equals(focusHead.getLemma())) {
                addToFeatures(globalFeatures,
                        FeatureUtils.formatFeatureName("MentionPair_SameLemmaType", previousMention.getCombinedType()),
                        1);
            }
        }
    }

    private void findSharedDeps(StanfordCorenlpToken focusHead, List<MentionKey> knownStates, String sharedDep,
                                List<MentionKey> shareDepMentions, List<MentionKey> corefDepMentions) {
        StanfordCorenlpToken focusChild = findChildOfDep(focusHead, sharedDep);

        for (MentionKey previousMention : knownStates) {
            if (previousMention.getCombinedType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                continue;
            }

            StanfordCorenlpToken previousChild = findChildOfDep(
                    (StanfordCorenlpToken) previousMention.getHeadWord(), sharedDep
            );

            if (previousChild != null && focusChild != null) {
                if (focusChild.equals(previousChild)) {
                    shareDepMentions.add(previousMention);
                } else if (token2EntityId.containsKey(focusChild) && token2EntityId.containsKey(previousChild)) {
                    corefDepMentions.add(previousMention);
                }
            }
        }
    }


    private void addDependencyFeatures(Map<StanfordCorenlpToken, String> children,
                                       Map<StanfordCorenlpToken, String> govners, Collection<MentionKey> knownStates,
                                       TObjectDoubleMap<String> globalFeatures) {

        for (MentionKey previousMention : knownStates) {
            if (previousMention.getCombinedType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                continue;
            }

            StanfordCorenlpToken previousHead = (StanfordCorenlpToken) previousMention.getHeadWord();

            if (children.containsKey(previousHead)) {
                String dep = children.get(previousHead);
                if (targetDeps.contains(dep)) {
                    String previousType = previousMention.getCombinedType();
                    addToFeatures(globalFeatures,
                            FeatureUtils.formatFeatureName("MentionPair_ChildMentionType_" + dep, previousType), 1
                    );

                }
            }

            if (govners.containsKey(previousHead)) {
                String dep = govners.get(previousHead);
                if (targetDeps.contains(dep)) {
                    String previousType = previousMention.getCombinedType();
                    addToFeatures(globalFeatures,
                            FeatureUtils.formatFeatureName("MentionPair_GovnerMentionType_" + dep, previousType), 1
                    );
                }
            }
        }
    }

    private List<MentionKey> findSameSentence(StanfordCorenlpToken focusHead, List<MentionKey> knownStates) {
        List<MentionKey> sameSentMentions = new ArrayList<>();
        for (MentionKey previousMention : knownStates) {
            if (previousMention.getCombinedType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                continue;
            }

            StanfordCorenlpToken previousHead = (StanfordCorenlpToken) previousMention.getHeadWord();

            if (token2SentenceId.get(focusHead) == token2SentenceId.get(previousHead)) {
                sameSentMentions.add(previousMention);
            }
        }

        return sameSentMentions;
    }

    private StanfordCorenlpToken findChildOfDep(StanfordCorenlpToken head, String dependency) {
        FSList childrenFs = head.getChildDependencyRelations();
        if (childrenFs != null) {
            for (StanfordDependencyRelation dep :
                    FSCollectionFactory.create(childrenFs, StanfordDependencyRelation.class)) {
                if (dep.getDependencyType().equals(dependency)) {
                    return (StanfordCorenlpToken) dep.getChild();
                }
            }
        }
        return null;
    }

    private Map<StanfordCorenlpToken, String> getDependencyChildren(StanfordCorenlpToken head) {
        FSList childrenFs = head.getChildDependencyRelations();
        Map<StanfordCorenlpToken, String> children = new HashMap<>();

        if (childrenFs != null) {
            for (StanfordDependencyRelation dep :
                    FSCollectionFactory.create(childrenFs, StanfordDependencyRelation.class)) {
                children.put((StanfordCorenlpToken) dep.getChild(), dep.getDependencyType());
            }
        }
        return children;
    }

    private Map<StanfordCorenlpToken, String> getDependencyGovners(StanfordCorenlpToken head) {
        FSList govenerFS = head.getHeadDependencyRelations();
        Map<StanfordCorenlpToken, String> govners = new HashMap<>();

        if (govenerFS != null) {
            for (StanfordDependencyRelation dep :
                    FSCollectionFactory.create(govenerFS, StanfordDependencyRelation.class)) {
                govners.put((StanfordCorenlpToken) dep.getHead(), dep.getDependencyType());
            }
        }
        return govners;
    }

    private void readFrames(JCas jCas) {
        triggerToFrameName = new HashMap<>();
        for (SemaforAnnotationSet annoSet : JCasUtil.select(jCas, SemaforAnnotationSet.class)) {
            String frameName = annoSet.getFrameName();

            SemaforLabel trigger = null;

            for (SemaforLayer layer : FSCollectionFactory.create(annoSet.getLayers(), SemaforLayer.class)) {
                String layerName = layer.getName();
                if (layerName.equals("Target")) {// Target that invoke the frame
                    trigger = layer.getLabels(0);
                }
            }

            StanfordCorenlpToken triggerHead = UimaNlpUtils.findHeadFromStanfordAnnotation(trigger);

            if (triggerHead != null) {
                triggerToFrameName.put(triggerHead, frameName);
            }
        }
    }
}
