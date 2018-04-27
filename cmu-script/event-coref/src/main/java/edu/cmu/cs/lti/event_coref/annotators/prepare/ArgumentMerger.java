package edu.cmu.cs.lti.event_coref.annotators.prepare;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.model.SemaforConstants;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;
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
                    SemanticArgument arg = new SemanticArgument(aJCas, frameSpan.getBegin(), frameSpan.getEnd());
                    arg.setHead(argHead);
                    relation.setChild(arg);
                    UimaAnnotationUtils.finishAnnotation(arg, COMPONENT_ID, 0, aJCas);
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
                    SemanticArgument arg = new SemanticArgument(aJCas, frameSpan.getBegin(), frameSpan.getEnd());
                    arg.setHead(argHead);
                    relation.setChild(arg);

                    mergedArguments.put(semaforArg.getKey(), relation);
                }
                token.setFrameName(frameNetName);
            }


            for (Map.Entry<StanfordCorenlpToken, SemanticRelation> argument : mergedArguments.entrySet()) {
                SemanticRelation relation = argument.getValue();
                UimaAnnotationUtils.finishTop(relation, COMPONENT_ID, 0, aJCas);
                UimaAnnotationUtils.finishAnnotation(relation.getChild(), COMPONENT_ID, 0, aJCas);

                argumentByChild.put(argument.getKey(), relation);
            }
            token.setChildSemanticRelations(FSCollectionFactory.createFSList(aJCas, mergedArguments.values()));
        }

//        for (Map.Entry<StanfordCorenlpToken, Collection<SemanticRelation>> headArguments : argumentByChild.asMap()
//                .entrySet()) {
//            StanfordCorenlpToken childToken = headArguments.getKey();
//
//            childToken.setHeadSemanticRelations(FSCollectionFactory.createFSList(aJCas, headArguments.getValue()));
//        }
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
                        StanfordCorenlpToken argumentHead = UimaNlpUtils.findHeadFromStanfordAnnotation(label);

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
        FanseToken fanseChild = (FanseToken) childRelation.getChild().getHead();

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

    public static void main(String argv[]) throws IOException, UIMAException {
        final Logger logger = LoggerFactory.getLogger(ArgumentMerger.class);

        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription
                (typeSystemName);

        String workingDir = argv[0];
        String inputDir = argv[1];

        CollectionReaderDescription inputReader = CustomCollectionReaderFactory.createXmiReader(workingDir, inputDir);
        AnalysisEngineDescription merger = AnalysisEngineFactory.createEngineDescription(ArgumentMerger.class,
                typeSystemDescription);

        String tbfOutPath = FileUtils.joinPaths(workingDir, "tbf", "arguments.tbf");

        logger.info("Output tbf to " + tbfOutPath);

        AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                TbfStyleEventWriter.class, typeSystemDescription,
                TbfStyleEventWriter.PARAM_OUTPUT_PATH, tbfOutPath,
                TbfStyleEventWriter.PARAM_SYSTEM_ID, "cold_start",
                TbfStyleEventWriter.PARAM_GOLD_TOKEN_COMPONENT_ID, TbfEventDataReader.COMPONENT_ID,
                TbfStyleEventWriter.PARAM_USE_CHARACTER_OFFSET, true,
                TbfStyleEventWriter.PARAM_ADD_SEMANTIC_ROLE, true
        );

        AnalysisEngineDescription headWordExtractor = AnalysisEngineFactory.createEngineDescription(
                EventHeadWordAnnotator.class, typeSystemDescription
        );

        SimplePipeline.runPipeline(inputReader, merger, headWordExtractor, resultWriter);
    }
}
