package edu.cmu.cs.lti.event_coref.annotators.prepare;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.model.SemaforConstants;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.javatuples.Pair;

import java.util.*;

/**
 * Merge Frame arguments generated into unified arguments. For easy extraction
 *
 * @author Zhengzhong Liu
 */
public class ArgumentMerger extends AbstractLoggingAnnotator {
    public static final String COMPONENT_ID = ArgumentMerger.class.getSimpleName();

    TokenAlignmentHelper helper = new TokenAlignmentHelper(true);

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        helper.loadFanse2Stanford(aJCas);

        Map<StanfordCorenlpToken, Pair<String, Map<StanfordCorenlpToken, Pair<String, Span>>>> semaforFrames =
                getSemaforArguments(aJCas);
        Map<StanfordCorenlpToken, Pair<String, Map<StanfordCorenlpToken, Pair<String, Span>>>> fanseFrames =
                getFanseArguments(aJCas);

        ArrayListMultimap<StanfordCorenlpToken, SemanticRelation> argumentByChild = ArrayListMultimap.create();
        // Create argument for each token.
        for (StanfordCorenlpToken token : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
            // Move annotations to stanford tokens.

            Map<StanfordCorenlpToken, SemanticRelation> mergedArguments = new HashMap<>();

            Pair<String, Map<StanfordCorenlpToken, Pair<String, Span>>> semaforFrame = semaforFrames.get(token);
            Pair<String, Map<StanfordCorenlpToken, Pair<String, Span>>> fanseFrame = fanseFrames.get(token);

            String fanseLexicalSense = null;
            if (fanseFrame != null) {
                fanseLexicalSense = fanseFrame.getValue0();
                token.setPropbankSense(fanseLexicalSense);
                for (Map.Entry<StanfordCorenlpToken, Pair<String, Span>> fanseArg : fanseFrame.getValue1().entrySet()) {
                    StanfordCorenlpToken argHead = fanseArg.getKey();
                    String argRoleName = fanseArg.getValue().getValue0();
                    Span frameSpan = fanseArg.getValue().getValue1();

                    SemanticRelation relation = new SemanticRelation(aJCas);

                    relation.setPropbankRoleName(argRoleName);
                    relation.setChildHead(argHead);
                    ComponentAnnotation childSpan = new ComponentAnnotation(aJCas, frameSpan.getBegin(), frameSpan
                            .getEnd());
                    relation.setChildSpan(childSpan);
                    mergedArguments.put(argHead, relation);
                }
            }

            if (semaforFrame != null) {
                String frameNetName = semaforFrame.getValue0();
                for (Map.Entry<StanfordCorenlpToken, Pair<String, Span>> semaforArg : semaforFrame.getValue1()
                        .entrySet()) {
                    StanfordCorenlpToken argHead = semaforArg.getKey();
                    String feName = semaforArg.getValue().getValue0();
                    Span frameSpan = semaforArg.getValue().getValue1();
                    SemanticRelation relation = mergedArguments.containsKey(argHead) ? mergedArguments.get(argHead) :
                            new SemanticRelation(aJCas);

                    relation.setFrameElementName(feName);
                    relation.setChildHead(argHead);

                    ComponentAnnotation childSpan = new ComponentAnnotation(aJCas, frameSpan.getBegin(), frameSpan
                            .getEnd());
                    relation.setChildSpan(childSpan);
                    mergedArguments.put(semaforArg.getKey(), relation);
                }
                token.setFrameName(frameNetName);
            }


            for (Map.Entry<StanfordCorenlpToken, SemanticRelation> argument : mergedArguments.entrySet()) {
                SemanticRelation relation = argument.getValue();
                UimaAnnotationUtils.finishTop(relation, COMPONENT_ID, 0, aJCas);
                UimaAnnotationUtils.finishAnnotation(relation.getChildSpan(), COMPONENT_ID, 0, aJCas);

                argumentByChild.put(argument.getKey(), relation);
            }
            token.setChildSemanticRelations(FSCollectionFactory.createFSList(aJCas, mergedArguments.values()));
        }

        for (Map.Entry<StanfordCorenlpToken, Collection<SemanticRelation>> headArguments : argumentByChild.asMap()
                .entrySet()) {
            StanfordCorenlpToken childToken = headArguments.getKey();

            childToken.setHeadSemanticRelations(FSCollectionFactory.createFSList(aJCas, headArguments.getValue()));
        }
    }

    private Map<StanfordCorenlpToken, Pair<String, Map<StanfordCorenlpToken, Pair<String, Span>>>>
    getSemaforArguments(JCas aJCas) {

        Map<SemaforLabel, Collection<StanfordCorenlpToken>> labelCovered = JCasUtil.indexCovered(aJCas, SemaforLabel
                .class, StanfordCorenlpToken.class);
        Map<SemaforLabel, Collection<StanfordCorenlpToken>> labelCovering = JCasUtil.indexCovering(aJCas,
                SemaforLabel.class, StanfordCorenlpToken.class);

        // Map<FrameHead, Pair<FrameName, Map<ArgumentHead, Pair<ArgumentName, ArgumentSpan>>>>
        Map<StanfordCorenlpToken, Pair<String, Map<StanfordCorenlpToken, Pair<String, Span>>>> semaforFrames = new
                HashMap<>();

        for (SemaforAnnotationSet annotationSet : JCasUtil.select(aJCas, SemaforAnnotationSet.class)) {
            SemaforLabel targetLabel = null;
            Map<StanfordCorenlpToken, Pair<String, Span>> roleLabels = new HashMap<>();

            for (SemaforLayer layer : JCasUtil.select(annotationSet.getLayers(), SemaforLayer.class)) {
                String layerName = layer.getName();
                if (layerName.equals(SemaforConstants.TARGET_LAYER_NAME)) {
                    for (SemaforLabel label : JCasUtil.select(layer.getLabels(), SemaforLabel.class)) {
                        targetLabel = label;
                    }
                } else if (layerName.equals(SemaforConstants.FRAME_ELEMENT_LAYER_NAME)) {
                    for (SemaforLabel label : JCasUtil.select(layer.getLabels(), SemaforLabel.class)) {
                        StanfordCorenlpToken argumentHead = UimaNlpUtils.findHeadFromAnnotation(label);

//                        if (argumentHead == null){
//                            System.out.println("Cannot find stanford token for " + label.get);
//                        }

                        roleLabels.put(argumentHead, Pair.with(label.getName(), Span.of(label.getBegin(), label
                                .getEnd())));
                    }
                }
            }

            if (targetLabel != null) {
                StanfordCorenlpToken targetToken = null;

                if (labelCovered.containsKey(targetLabel)) {
                    Collection<StanfordCorenlpToken> coveredTokens = labelCovered.get(targetLabel);
                    for (StanfordCorenlpToken token : coveredTokens) {
                        targetToken = token;
                        break; // Take first one.
                    }
                }

                if (targetToken == null) {
                    Collection<StanfordCorenlpToken> coveringTokens = labelCovering.get(targetLabel);
                    for (StanfordCorenlpToken token : coveringTokens) {
                        targetToken = token;
                        break; // Take first one.
                    }
                }

                if (targetToken != null) {
                    semaforFrames.put(targetToken, Pair.with(annotationSet.getFrameName(), roleLabels));
                }
            }
        }
        return semaforFrames;
    }

    private Map<StanfordCorenlpToken, Pair<String, Map<StanfordCorenlpToken, Pair<String, Span>>>> getFanseArguments
            (JCas aJCas) {
        // Map<FrameHead, Pair<FrameName, Map<ArgumentHead, Pair<ArgumentName, Span>>>
        Map<StanfordCorenlpToken, Pair<String, Map<StanfordCorenlpToken, Pair<String, Span>>>> fanseFrames = new
                HashMap<>();

        for (FanseToken token : JCasUtil.select(aJCas, FanseToken.class)) {
            StanfordCorenlpToken predicateHead = helper.getStanfordToken(token);
            if (predicateHead != null) {
                Map<StanfordCorenlpToken, Pair<String, Span>> fanseFrame = new HashMap<>();

                FSList semanticRelationsFS = token.getChildSemanticRelations();
                if (semanticRelationsFS != null) {
                    for (FanseSemanticRelation childRelation : JCasUtil.select(semanticRelationsFS,
                            FanseSemanticRelation.class)) {

                        Pair<StanfordCorenlpToken, Span> realSpan = findFanseSpan(childRelation);

                        StanfordCorenlpToken argumentHead = realSpan.getValue0();

                        if (argumentHead != null) {
                            fanseFrame.put(argumentHead, Pair.with(childRelation.getSemanticAnnotation(),
                                    realSpan.getValue1()));
                        }
                    }
                }

                String lexicalSense = token.getLexicalSense();

                fanseFrames.put(predicateHead, Pair.with(lexicalSense, fanseFrame));
            }
        }
        return fanseFrames;
    }

    private Pair<StanfordCorenlpToken, Span> findFanseSpan(FanseSemanticRelation childRelation) {
        FanseToken fanseChild = (FanseToken) childRelation.getChildHead();

        Set<String> lightPosSet = new HashSet<>();
        lightPosSet.add("IN");
        lightPosSet.add("TO");

        String childPos = fanseChild.getPos();

        Pair<StanfordCorenlpToken, Span> realSpan = null;

        if (lightPosSet.contains(childPos)) {
            FSList childDependencies = fanseChild.getChildDependencyRelations();

            if (childDependencies != null) {
                for (FanseDependencyRelation dep : JCasUtil.select(childDependencies, FanseDependencyRelation
                        .class)) {

                    String depType = dep.getDependencyType();
                    FanseToken depChild = (FanseToken) dep.getChild();

                    if (realSpan == null || depType.endsWith("obj")) {
                        realSpan = Pair.with(helper.getStanfordToken(depChild), Span.of(depChild.getBegin(), depChild
                                .getEnd()));
                    }
                }
            }
        }

        if (realSpan == null) {
            realSpan = Pair.with(helper.getStanfordToken(fanseChild), Span.of(fanseChild.getBegin(),
                    fanseChild.getEnd()));
        }

        return realSpan;
    }
}
