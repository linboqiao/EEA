package edu.cmu.cs.lti.event_coref.annotators.train;

import edu.cmu.cs.lti.event_coref.decoding.BestFirstLatentTreeDecoder;
import edu.cmu.cs.lti.event_coref.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import edu.cmu.cs.lti.utils.MentionUtils;
import edu.cmu.cs.lti.utils.MultiKeyDiskCacher;
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
        trainingStats = new TrainingStats(5, "PlainLatentTree");
        logger.info("Latent Tree trainer initialized.");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<EventMention> allMentions = MentionUtils.clearDuplicates(
                new ArrayList<>(JCasUtil.select(aJCas, EventMention.class))
        );

        List<MentionCandidate> candidates = MentionUtils.createCandidates(aJCas, allMentions);

        extractor.initWorkspace(aJCas);

        MentionGraph mentionGraph = getMentionGraph(aJCas, allMentions);

        // Decoding.
        MentionSubGraph predictedTree = decoder.decode(mentionGraph, candidates, weights, extractor);

        if (!predictedTree.graphMatch()) {
//            logger.debug("Found unmatched graph");

            MentionSubGraph latentTree = mentionGraph.getLatentTree(weights, candidates);

//            logger.debug("Best Gold Tree.");
//            logger.debug(latentTree.toString());
//
//            logger.debug("Best Decoding Tree.");
//            logger.debug(predictedTree.toString());

            double loss = update(predictedTree, latentTree);

            trainingStats.addLoss(logger, loss / mentionGraph.numNodes());

//            logger.debug("Loss is " + loss);
//            logger.debug("Averaged Loss is " + loss / mentionGraph.numNodes());
//
//            DebugUtils.pause(logger);
        } else {
            trainingStats.addLoss(logger, 0);
        }
    }

    private MentionGraph getMentionGraph(JCas aJCas, List<EventMention> allMentions){
        int eventIdx = 0;
        for (Event event : JCasUtil.select(aJCas, Event.class)) {
            event.setIndex(eventIdx++);
        }

        String cacheKey = UimaConvenience.getShortDocumentNameWithOffset(aJCas);

        // A mention graph represent all the mentions and contains features among them.
        MentionGraph mentionGraph = graphCacher.get(cacheKey);

        if (mentionGraph == null) {
            List<MentionCandidate> candidates = MentionUtils.getSpanBasedCandidates(aJCas);
             mentionGraph = MentionUtils.createMentionGraph(aJCas, candidates, extractor, false);
            graphCacher.addWithMultiKey(mentionGraph, cacheKey);
        }
        return mentionGraph;
    }

    /**
     * Update the weights by the difference of the predicted tree and the gold latent tree.
     *
     * @param predictedTree The predicted tree.
     * @param latentTree    The gold latent tree.
     */
    private double update(MentionSubGraph predictedTree, MentionSubGraph latentTree) {
        return passiveAggressiveUpdate(predictedTree, latentTree);
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
    private double passiveAggressiveUpdate(MentionSubGraph predictedTree, MentionSubGraph latentTree) {
//        logger.info(predictedTree.toString());
//
//        logger.info(latentTree.toString());

        GraphFeatureVector delta = latentTree.getDelta(predictedTree, classAlphabet, featureAlphabet);

        double loss = predictedTree.getLoss(latentTree);
        double l2Sqaure = delta.getFeatureSquare();

        if (l2Sqaure != 0) {
            double tau = loss / l2Sqaure;
            weights.updateWeightsBy(delta, tau);
            weights.updateAverageWeights();
        }
        return loss;
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
