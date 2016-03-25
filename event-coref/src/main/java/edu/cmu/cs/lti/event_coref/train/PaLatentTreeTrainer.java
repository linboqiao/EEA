package edu.cmu.cs.lti.event_coref.train;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import edu.cmu.cs.lti.emd.utils.MentionUtils;
import edu.cmu.cs.lti.event_coref.decoding.BestFirstLatentTreeDecoder;
import edu.cmu.cs.lti.event_coref.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge.EdgeType;
import edu.cmu.cs.lti.event_coref.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import edu.cmu.cs.lti.utils.MultiKeyDiskCacher;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.javatuples.Pair;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/29/15
 * Time: 7:27 PM
 *
 * @author Zhengzhong Liu
 */
public class PaLatentTreeTrainer extends AbstractLoggingAnnotator {
    public static final String MODEL_NAME = "latentTreeModel";

    public static final String PARAM_CONFIG_PATH = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    public static final String PARAM_CACHE_DIRECTORY = "cacheDirectory";
    @ConfigurationParameter(name = PARAM_CACHE_DIRECTORY)
    private String cacheDir;

    private PairFeatureExtractor extractor;
    private LatentTreeDecoder decoder;
    private static MultiKeyDiskCacher<MentionGraph> graphCacher;
    private TrainingStats trainingStats;

    // The resulting weights.
    private static GraphWeightVector weights;

    private ClassAlphabet classAlphabet;
    private HashAlphabet featureAlphabet;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        logger.info("Initialize latent tree perceptron trainer");

        int alphabetBits = config.getInt("edu.cmu.cs.lti.coref.feature.alphabet_bits", 22);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.coref.readableModel", false);
        boolean discardAfter = config.getBoolean("edu.cmu.cs.lti.coref.cache.discard_after", true);
        long weightLimit = config.getLong("edu.cmu.cs.lti.coref.weightlimit", 1250000);

        try {
            logger.info("Initialize auto-eviction cache with weight limit of " + weightLimit);
            graphCacher = new MultiKeyDiskCacher<>(
                    cacheDir, (k, g) -> g.numNodes() * g.numNodes(), weightLimit, discardAfter
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        classAlphabet = new ClassAlphabet();
        for (EdgeType edgeType : EdgeType.values()) {
            classAlphabet.addClass(edgeType.name());
        }

        featureAlphabet = new HashAlphabet(alphabetBits, readableModel);
        String featureSpec = config.get("edu.cmu.cs.lti.features.coref.spec");

        decoder = new BestFirstLatentTreeDecoder();
        weights = new GraphWeightVector(classAlphabet, featureAlphabet, featureSpec);

        try {
            logger.debug(featureSpec);
            Configuration featureConfig = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.pair.package.name")
            ).parseFeatureFunctionSpecs(featureSpec);
            extractor = new PairFeatureExtractor(featureAlphabet, classAlphabet, config, featureConfig);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
        trainingStats = new TrainingStats(5);
        logger.info("Latent Tree trainer initialized.");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<EventMention> allMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));
        List<EventMentionRelation> allMentionRelations = new ArrayList<>(
                JCasUtil.select(aJCas, EventMentionRelation.class));

        int eventIdx = 0;
        for (Event event : JCasUtil.select(aJCas, Event.class)) {
            event.setIndex(eventIdx++);
        }

        extractor.initWorkspace(aJCas);

        String cacheKey = UimaConvenience.getShortDocumentNameWithOffset(aJCas);

        // A mention graph represent all the mentions and contains features among them.
        MentionGraph mentionGraph = graphCacher.get(cacheKey);

        Map<Integer, Integer> mentionId2EventId = MentionUtils.groupEventClusters(allMentions);

        TIntIntMap mention2Candidate = new TIntIntHashMap();
        List<MentionCandidate> candidates = MentionUtils.createCandidates(aJCas, allMentions, mention2Candidate);

        SetMultimap<Integer, Integer> candidate2SplitMentions = HashMultimap.create();

        // Mention and candidate have a one to one match in this case, but you must provide the actual mentions (do
        // not merge types)
        mention2Candidate.forEachEntry((mentionId, candidateId) -> {
            candidate2SplitMentions.put(mentionId, candidateId);
            return true;
        });

        Map<Pair<Integer, Integer>, String> relations = MentionUtils.indexRelations(aJCas,
                mention2Candidate, allMentions);

        List<String> mentionTypes = allMentions.stream().map(EventMention::getEventType).collect(Collectors.toList());


        if (mentionGraph == null) {
            mentionGraph = new MentionGraph(candidates, candidate2SplitMentions, mentionTypes, mentionId2EventId,
                    relations, extractor);
            graphCacher.addWithMultiKey(mentionGraph, cacheKey);
        }

        // Decoding.
        MentionSubGraph predictedTree = decoder.decode(mentionGraph, candidates, weights, extractor);
        if (!predictedTree.graphMatch()) {
            MentionSubGraph latentTree = mentionGraph.getLatentTree(weights, candidates);
            double loss = predictedTree.getLoss(latentTree);
            trainingStats.addLoss(logger, loss / mentionGraph.numNodes());
            update(predictedTree, latentTree);

//            logger.debug("Loss is " + loss);
//            logger.debug("Predicted tree.");
//            logger.debug(predictedTree.toString());
//            logger.debug("Actual tree.");
//            logger.debug(latentTree.toString());
//            DebugUtils.pause(logger);
        } else {
            trainingStats.addLoss(logger, 0);
        }
    }

    /**
     * Update the weights by the difference of the predicted tree and the gold latent tree.
     *
     * @param predictedTree The predicted tree.
     * @param latentTree    The gold latent tree.
     */
    private void update(MentionSubGraph predictedTree, MentionSubGraph latentTree) {
        passiveAggressiveUpdate(predictedTree, latentTree);
    }

    /**
     * A vanilla update that update by the difference of the predicted tree and the gold latent tree, with a fixed
     * step size.
     *
     * @param predictedTree The predicted tree.
     * @param latentTree    The gold latent tree.
     */
    private void vanillaUpdate(MentionSubGraph predictedTree, MentionSubGraph latentTree) {
        GraphFeatureVector delta = latentTree.getDelta(predictedTree, classAlphabet, featureAlphabet);
//        logger.info("Delta between the features are: ");
//        logger.info(delta.readableNodeVector());
        weights.updateWeightsBy(delta, 0.1);
        weights.updateAverageWeights();
    }

    /**
     * Update the weights by the difference of the predicted tree and the gold latent tree using Passive-Aggressive
     * algorithm.
     *
     * @param predictedTree The predicted tree.
     * @param latentTree    The gold latent tree.
     */
    private void passiveAggressiveUpdate(MentionSubGraph predictedTree, MentionSubGraph latentTree) {
//        logger.info(predictedTree.toString());
//
//        logger.info(latentTree.toString());

        GraphFeatureVector delta = latentTree.getDelta(predictedTree, classAlphabet, featureAlphabet);

//        logger.info("Delta between the features are: ");
//        logger.info(delta.readableNodeVector());
        double loss = predictedTree.getLoss(latentTree);
        double l2 = getFeatureL2(delta);
//        double deltaDotProd = weights.dotProd(delta);
//        double tau = (deltaDotProd - loss) / l2;
        double tau = loss / l2;

//        logger.info("Loss is " + loss + " update rate is " + tau + " l2 is " + l2);

        weights.updateWeightsBy(delta, tau);
        weights.updateAverageWeights();
    }

    /**
     * Compute the L2 norm of the feature vector.
     *
     * @param typedFeatures A vector that store features with each type separated.
     * @return The L2 norm of the feature vector.
     */
    public double getFeatureL2(GraphFeatureVector typedFeatures) {
        double l2Sq = 0;

        // We can consider this operation by flatten the whole feature into a long vector and compute its L2 norm.
        for (TIntObjectIterator<FeatureVector> iter = typedFeatures.nodeFvIter(); iter.hasNext(); ) {
            iter.advance();
            FeatureVector fv = iter.value();
            l2Sq += fv.dotProd(fv);
        }
        return Math.sqrt(l2Sq);
    }

    public static void saveModels(File modelOutputDirectory) throws IOException {
        FileUtils.ensureDirectory(modelOutputDirectory);
        weights.write(new File(modelOutputDirectory, MODEL_NAME));
    }

    public static void finish() {
        try {
            graphCacher.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
