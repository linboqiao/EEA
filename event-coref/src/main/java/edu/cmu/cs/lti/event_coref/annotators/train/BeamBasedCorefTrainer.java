package edu.cmu.cs.lti.event_coref.annotators.train;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import edu.cmu.cs.lti.emd.utils.MentionUtils;
import edu.cmu.cs.lti.event_coref.decoding.BeamLatentTreeDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.update.DiscriminativeUpdater;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.cmu.cs.lti.emd.utils.MentionUtils.mapCandidate2Events;
import static edu.cmu.cs.lti.emd.utils.MentionUtils.processCandidates;
import static edu.cmu.cs.lti.learning.model.ModelConstants.COREF_MODEL_NAME;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/18/16
 * Time: 4:56 PM
 *
 * @author Zhengzhong Liu
 */
public class BeamBasedCorefTrainer extends AbstractLoggingAnnotator {
    private static DiscriminativeUpdater updater;

    public static final String PARAM_CONFIGURATION_FILE = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIGURATION_FILE)
    private Configuration config;

    public static final String PARAM_DELAYED_LASO = "delayedLaso";
    @ConfigurationParameter(name = PARAM_DELAYED_LASO)
    private boolean delayedLaso;

    public static final String PARAM_MERGE_MENTION = "mergeMention";
    @ConfigurationParameter(name = PARAM_MERGE_MENTION)
    private boolean mergeMention;

    public static final String PARAM_USE_LASO = "useLaso";
    @ConfigurationParameter(name = PARAM_USE_LASO)
    private boolean useLaSO;

    public static final String PARAM_BEAM_SIZE = "beamSize";
    @ConfigurationParameter(name = PARAM_BEAM_SIZE)
    private int beamSize;

    private BeamLatentTreeDecoder decoder;

    private PairFeatureExtractor mentionPairExtractor;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        logger.info("Preparing the Beam based Trainer for coreference ...");
        super.initialize(context);

        logger.info(String.format("Beam Trainer using PA update : %s, use LaSO: %s, Delayed LaSO : %s, Loss Type : " +
                        "%s, Merge mentions : %s, Beam size : %d", true, useLaSO, delayedLaso, "hamming", mergeMention,
                beamSize));

        updater = new DiscriminativeUpdater(false, true, true, "hamming");
        updater.addWeightVector(ModelConstants.COREF_MODEL_NAME, preareCorefWeights());

        try {
            this.mentionPairExtractor = initializeMentionPairExtractor(config);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | InstantiationException e) {
            e.printStackTrace();
        }

        try {
            decoder = new BeamLatentTreeDecoder(updater.getWeightVector(ModelConstants.COREF_MODEL_NAME),
                    mentionPairExtractor, updater, useLaSO, delayedLaso, beamSize);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | InstantiationException e) {
            e.printStackTrace();
        }
    }

    private PairFeatureExtractor initializeMentionPairExtractor(Configuration config) throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String featureSpec = config.get("edu.cmu.cs.lti.features.coref.spec");

        Configuration featureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.pair.package.name")
        ).parseFeatureFunctionSpecs(featureSpec);

        ClassAlphabet classAlphabet = new ClassAlphabet();
        for (EdgeType edgeType : EdgeType.values()) {
            classAlphabet.addClass(edgeType.name());
        }

        return new PairFeatureExtractor(updater.getWeightVector(COREF_MODEL_NAME).getFeatureAlphabet(),
                classAlphabet, config, featureConfig);
    }

    private GraphWeightVector preareCorefWeights() {
        logger.info("Initializing Coreference weights.");

        ClassAlphabet classAlphabet = new ClassAlphabet();
        for (EdgeType edgeType : EdgeType.values()) {
            classAlphabet.addClass(edgeType.name());
        }

        int alphabetBits = config.getInt("edu.cmu.cs.lti.coref.feature.alphabet_bits", 22);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.coref.readableModel", false);

        HashAlphabet featureAlphabet = new HashAlphabet(alphabetBits, readableModel);
        String featureSpec = config.get("edu.cmu.cs.lti.features.coref.spec");

        return new GraphWeightVector(classAlphabet, featureAlphabet, featureSpec);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        UimaConvenience.printProcessLog(aJCas, logger);

        List<EventMention> allMentions = MentionUtils.clearDuplicates(
                new ArrayList<>(JCasUtil.select(aJCas, EventMention.class))
        );

        mentionPairExtractor.initWorkspace(aJCas);

        Pair<MentionGraph, List<MentionCandidate>> graphAndCands = mergeMention ? getCombinedGraph(aJCas, allMentions) :
                getSeparateGraph(aJCas, allMentions);

//        logger.info("Number of mentions " + allMentions.size());
//        logger.info("Number of nodes " + graphAndCands.getKey().numNodes());

        decoder.decode(aJCas, graphAndCands.getKey(), graphAndCands.getValue());
    }

    private Pair<MentionGraph, List<MentionCandidate>> getSeparateGraph(JCas aJCas, List<EventMention> allMentions) {
        List<MentionCandidate> candidates = MentionUtils.createCandidates(aJCas, allMentions);

        // Each candidate can correspond to multiple nodes.
        SetMultimap<Integer, Integer> candidate2SplitNodes = HashMultimap.create();
        // A gold mention has a one to one mapping to a node in current case.
        TIntIntMap mention2SplitNodes = new TIntIntHashMap();
        for (int i = 0; i < allMentions.size(); i++) {
            candidate2SplitNodes.put(i, i);
            mention2SplitNodes.put(i, i);
        }

        Map<Pair<Integer, Integer>, String> relations = MentionUtils.indexRelations(aJCas, mention2SplitNodes,
                allMentions);

        List<String> mentionTypes = allMentions.stream().map(EventMention::getEventType).collect(Collectors.toList());

        Map<Integer, Integer> mentionId2EventId = MentionUtils.groupEventClusters(allMentions);

        MentionGraph graph = new MentionGraph(candidates, candidate2SplitNodes, mentionTypes, mentionId2EventId,
                relations, mentionPairExtractor, true);

        return Pair.of(graph, candidates);
    }

    private Pair<MentionGraph, List<MentionCandidate>> getCombinedGraph(JCas aJCas, List<EventMention> allMentions) {
        List<MentionCandidate> candidates = MentionUtils.createMergedCandidates(aJCas, allMentions);

//        logger.info("Showing merged candidates.");
//        for (MentionCandidate candidate : candidates) {
//            logger.info(candidate.getHeadWord().getCoveredText() + " " + candidate.getMentionType());
//        }

        // A candidate is unique on a specific span.
        // Multiple nodes with different types can be on the same span.

        // A map from the candidate id to node id, a candidate can correspond to more than one node.
        SetMultimap<Integer, Integer> candidate2Node = HashMultimap.create();
        // The type of each node, indexed.
        List<String> nodeTypes = new ArrayList<>();
        // A map from the event mention to the node id.
        TIntIntMap mention2Node = new TIntIntHashMap();
        int numNodes = processCandidates(allMentions, candidates, candidate2Node, mention2Node, nodeTypes);

        // Convert mention clusters to split candidate clusters.
        Map<Integer, Integer> mention2event = MentionUtils.groupEventClusters(allMentions);
        Map<Integer, Integer> node2EventId = mapCandidate2Events(numNodes, mention2Node, mention2event);

        Map<Pair<Integer, Integer>, String> relations = MentionUtils.indexRelations(aJCas, mention2Node, allMentions);

        MentionGraph graph = new MentionGraph(candidates, candidate2Node, nodeTypes, node2EventId, relations,
                mentionPairExtractor, true);

        return Pair.of(graph, candidates);
    }

    public static void saveModels(File modelOutputDirectory) throws IOException {
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(modelOutputDirectory);
        updater.getWeightVector(ModelConstants.COREF_MODEL_NAME).write(
                new File(modelOutputDirectory, ModelConstants.COREF_MODEL_NAME)
        );
    }
}
