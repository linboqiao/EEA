package edu.cmu.cs.lti.event_coref.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.event_coref.decoding.BeamLatentTreeDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.decoding.NodeLinkingState;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MentionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.cmu.cs.lti.learning.model.ModelConstants.COREF_MODEL_NAME;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/30/15
 * Time: 12:02 AM
 *
 * @author Zhengzhong Liu
 */
public class BeamEventCorefAnnotator extends AbstractLoggingAnnotator {
    public static final String PARAM_CONFIG_PATH = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    public static final String PARAM_MODEL_DIRECTORY = "modelDirectory";
    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    File modelDirectory;

//    public static final String PARAM_MERGE_MENTION = "mergeMention";
//    @ConfigurationParameter(name = PARAM_MERGE_MENTION, defaultValue = "True")
//    private boolean mergeMention;

    public static final String PARAM_BEAM_SIZE = "beamSize";
    @ConfigurationParameter(name = PARAM_BEAM_SIZE)
    private int beamSize;

    private BeamLatentTreeDecoder decoder;

    private GraphWeightVector corefWeights;
    private PairFeatureExtractor corefExtractor;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        logger.info("Initialize event coreference annotator.");

        prepareCorefModel();

        try {
            decoder = new BeamLatentTreeDecoder(corefWeights, corefExtractor, beamSize);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | InstantiationException e) {
            e.printStackTrace();
        }

        logger.info(String.format("Beam size : %d.", beamSize));
    }


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<EventMention> allMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));

        corefExtractor.initWorkspace(aJCas);

//        Pair<MentionGraph, List<MentionCandidate>> graphAndCands = mergeMention ? getCombinedGraph(aJCas, allMentions) :
//                getSeparateGraph(aJCas, allMentions);

        List<MentionCandidate> candidates = MentionUtils.getSpanBasedCandidates(aJCas);
        MentionGraph mentionGraph = MentionUtils.createMentionGraph(aJCas, candidates, corefExtractor, false);

        NodeLinkingState decodingState = decoder.decode(aJCas, mentionGraph, candidates);

        ArrayListMultimap<Pair<Integer, String>, EventMention> node2Mention = ArrayListMultimap.create();

        Map<Pair<Span, String>, EventMention> mentionMap = new HashMap<>();

        for (EventMention mention : allMentions) {
            mentionMap.put(Pair.of(Span.of(mention.getBegin(), mention.getEnd()), mention.getEventType()), mention);
        }

        List<MentionKey> nodeResults = decodingState.getNodeResults();

        for (int nodeIndex = 0; nodeIndex < nodeResults.size(); nodeIndex++) {
            MentionKey nodeKey = nodeResults.get(nodeIndex);

            if (nodeKey.isRoot()) {
                continue;
            }

            for (NodeKey result : nodeKey) {
                if (!result.getMentionType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                    EventMention mention = mentionMap.get(Pair.of(Span.of(result.getBegin(), result.getEnd()),
                            result.getMentionType()));

                    if (mention == null) {
                        logger.warn("Cannot find mention at " + result);
                    }

                    node2Mention.put(Pair.of(nodeIndex, result.getMentionType()), mention);
                }
            }
        }
        annotatePredictedCoreference(aJCas, decodingState.getDecodingTree(), node2Mention);
    }

    private void annotatePredictedCoreference(JCas aJCas, MentionSubGraph predictedTree,
                                              ArrayListMultimap<Pair<Integer, String>, EventMention> node2Mention) {
        predictedTree.resolveGraph();
        List<Pair<Integer, String>>[] corefChains = predictedTree.getCorefChains();

        for (List<Pair<Integer, String>> corefChain : corefChains) {
            List<EventMention> predictedChain = new ArrayList<>();
            Map<Span, EventMention> span2Mentions = new HashMap<>();

            for (Pair<Integer, String> typedNode : corefChain) {
                List<EventMention> mentions = node2Mention.get(typedNode);

                if (mentions == null) {
                    logger.error(typedNode + " is not mapped.");
                }

                // Add an additional filtering layer to remove coreference on the same mention.
                for (EventMention mention : mentions) {
                    Span mentionSpan = Span.of(mention.getBegin(), mention.getEnd());
                    if (!span2Mentions.containsKey(mentionSpan)) {
                        span2Mentions.put(mentionSpan, mention);
                        predictedChain.add(mention);
                    }
                }
//                predictedChain.addAll(mentions);
            }

            if (predictedChain.size() > 1) {
                Event event = new Event(aJCas);
                event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, predictedChain));
                UimaAnnotationUtils.finishTop(event, COMPONENT_ID, 0, aJCas);
                for (EventMention eventMention : predictedChain) {
                    eventMention.setReferringEvent(event);
                }
            }
        }

        logger.info(String.format("Found %d clusters in document %s", corefChains.length, UimaConvenience
                .getShortDocumentName(aJCas)));
    }

    private void prepareCorefModel() throws ResourceInitializationException {
        logger.info("Loading coreference model from " + modelDirectory);
        String featureSpec;
        FeatureAlphabet corefFeatureAlphabet;
        ClassAlphabet corefClassAlphabet;


        try {
            corefWeights = SerializationUtils.deserialize(new FileInputStream(new File(modelDirectory,
                    COREF_MODEL_NAME)));
            corefFeatureAlphabet = corefWeights.getFeatureAlphabet();
            corefClassAlphabet = corefWeights.getClassAlphabet();
            featureSpec = corefWeights.getFeatureSpec();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        try {
            String currentFeatureSpec = config.get("edu.cmu.cs.lti.features.coref.spec");
            specWarning(featureSpec, currentFeatureSpec);

            Configuration featureConfig = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.pair.package.name")
            ).parseFeatureFunctionSpecs(featureSpec);

            corefExtractor = new PairFeatureExtractor(corefFeatureAlphabet, corefClassAlphabet,
                    config, featureConfig);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            throw new ResourceInitializationException(e);
        }
    }

//    private Pair<MentionGraph, List<MentionCandidate>> getSeparateGraph(JCas aJCas, List<EventMention> allMentions) {
//        List<MentionCandidate> candidates = MentionUtils.createCandidates(aJCas, allMentions);
//
//        // Each candidate can correspond to multiple nodes.
//        SetMultimap<Integer, Integer> candidate2SplitNodes = HashMultimap.create();
//        // A gold mention has a one to one mapping to a node in current case.
//        TIntIntMap mention2SplitNodes = new TIntIntHashMap();
//        for (int i = 0; i < allMentions.size(); i++) {
//            candidate2SplitNodes.put(i, i);
//            mention2SplitNodes.put(i, i);
//        }
//
//        MentionGraph graph = MentionUtils.createMentionGraph(aJCas, corefExtractor, false);
//
//        return Pair.of(graph, candidates);
//    }
//
//    private Pair<MentionGraph, List<MentionCandidate>> getCombinedGraph(JCas aJCas, List<EventMention> allMentions) {
//        List<MentionCandidate> candidates = MentionUtils.createMergedCandidates(aJCas, allMentions);
//
//        // A candidate is unique on a specific span.
//        // Multiple nodes with different types can be on the same span.
//
//        // A map from the candidate id to node id, a candidate can correspond to more than one node.
//        SetMultimap<Integer, Integer> candidate2Node = HashMultimap.create();
//        // The type of each node, indexed.
//        List<String> nodeTypes = new ArrayList<>();
//        // A map from the event mention to the node id.
//        TIntIntMap mention2Node = new TIntIntHashMap();
//        int numNodes = processCandidates(allMentions, candidates, candidate2Node, mention2Node, nodeTypes);
//
//        // Convert mention clusters to split candidate clusters.
//        Map<Integer, Integer> mention2event = MentionUtils.groupEventClusters(allMentions);
//        Map<Integer, Integer> node2EventId = mapCandidate2Events(numNodes, mention2Node, mention2event);
//
//        Map<Pair<Integer, Integer>, String> relations = MentionUtils.indexRelations(aJCas, mention2Node,
//                allMentions);
//
//        MentionGraph graph = new MentionGraph(candidates, candidate2Node, nodeTypes, node2EventId, relations,
//                corefExtractor, false);
//
//
//        return Pair.of(graph, candidates);
//    }

    private void specWarning(String oldSpec, String newSpec) {
        if (!oldSpec.equals(newSpec)) {
            logger.warn("Current feature specification is not the same with the trained model.");
            logger.warn("Will use the stored specification, it might create unexpected errors");
            logger.warn("Using Spec:" + oldSpec);
        }
    }
}
