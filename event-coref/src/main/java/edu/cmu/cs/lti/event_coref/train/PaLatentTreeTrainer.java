package edu.cmu.cs.lti.event_coref.train;

import edu.cmu.cs.lti.event_coref.decoding.BestFirstLatentTreeDecoder;
import edu.cmu.cs.lti.event_coref.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge;
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
import edu.cmu.cs.lti.utils.ObjectCacher;
import gnu.trove.iterator.TIntObjectIterator;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/29/15
 * Time: 7:27 PM
 *
 * @author Zhengzhong Liu
 */
public class PaLatentTreeTrainer extends AbstractLoggingAnnotator {
    public static final String MODEL_NAME = "latentTreeModel";
    public static final String FEATURE_SPEC_FILE = "featureSpec";

    public static final String PARAM_CONFIG_PATH = "configPath";

    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    private PairFeatureExtractor extractor;
    private ClassAlphabet classAlphabet;
    private FeatureAlphabet featureAlphabet;
    private LatentTreeDecoder decoder;
    private ObjectCacher objectCacher;
    private TrainingStats trainingStats;

    // The resulting weights.
    private static GraphWeightVector weights;
    private static String featureSpec;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        logger.info("Initialize perceptron trainer");

        int alphabetBits = config.getInt("edu.cmu.cs.lti.coref.feature.alphabet_bits", 22);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.coref.readableModel", false);
        boolean useBinaryFeatures = config.getBoolean("edu.cmu.cs.lti.coref.binaryFeature", false);
        File corefGraphCacheDir = config.getFile("edu.cmu.cs.lti.coref.cache.dir");

        objectCacher = new ObjectCacher(corefGraphCacheDir);
        try {
            objectCacher.invalidate();
        } catch (IOException e) {
            e.printStackTrace();
        }
        classAlphabet = new ClassAlphabet();
        for (EdgeType edgeType : EdgeType.values()) {
            classAlphabet.addClass(edgeType.name());
        }
        featureAlphabet = HashAlphabet.getInstance(alphabetBits, readableModel);
        decoder = new BestFirstLatentTreeDecoder();
        weights = new GraphWeightVector(classAlphabet, featureAlphabet);

        featureSpec = config.get("edu.cmu.cs.lti.features.coref.spec");
        try {
            logger.debug(featureSpec);
            Configuration featureConfig = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.pair.package.name")
            ).parseFeatureFunctionSpecs(featureSpec);
            extractor = new PairFeatureExtractor(featureAlphabet, classAlphabet, useBinaryFeatures, config,
                    featureConfig);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
        trainingStats = new TrainingStats(5);
        logger.info("Latent Tree trainer initialized.");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        printProcessInfo(aJCas, logger);
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
        MentionGraph mentionGraph = null;
        try {
            mentionGraph = objectCacher.loadCachedObject(cacheKey);
        } catch (FileNotFoundException ignored) {

        }

        if (mentionGraph == null) {
            mentionGraph = new MentionGraph(allMentions, allMentionRelations);
        }

        // Decoding.
        MentionSubGraph predictedTree = decoder.decode(mentionGraph, weights, extractor);

        if (!graphMatch(predictedTree, mentionGraph)) {

            MentionSubGraph latentTree = mentionGraph.getLatentTree(weights, extractor);

//            logger.info("Predicted cluster.");
//            logger.info(Arrays.deepToString(predictedTree.getCorefChains()));
//
//            logger.info("Gold cluster.");
//            logger.info(Arrays.deepToString(mentionGraph.getCorefChains()));
//
//            logger.info("Predicted Tree is ");
//            logger.info(predictedTree.toString());
//
//            logger.info("Latent Tree is ");
//            logger.info(latentTree.toString());

            double loss = predictedTree.getLoss(latentTree);

            trainingStats.addLoss(logger, loss / mentionGraph.getMentionNodes().length);
//            trainingStats.addLoss(logger, loss);

            update(predictedTree, latentTree, extractor);

//            DebugUtils.pause();
        }

        try {
            objectCacher.writeCacheObject(mentionGraph, cacheKey);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the weights by the difference of the predicted tree and the gold latent tree.
     *
     * @param predictedTree The predicted tree.
     * @param latentTree    The gold latent tree.
     */
    private void update(MentionSubGraph predictedTree, MentionSubGraph latentTree, PairFeatureExtractor extractor) {
        passiveAggressiveUpdate(predictedTree, latentTree, extractor);
    }

    /**
     * A vanilla update that update by the difference of the predicted tree and the gold latent tree, with a fixed
     * step size.
     *
     * @param predictedTree The predicted tree.
     * @param latentTree    The gold latent tree.
     */
    private void vanillaUpdate(MentionSubGraph predictedTree, MentionSubGraph latentTree,
                               PairFeatureExtractor extractor) {
        GraphFeatureVector delta = latentTree.getDelta(predictedTree, extractor);
        logger.info("Delta between the features are: ");
        logger.info(delta.readableNodeVector());
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
    private void passiveAggressiveUpdate(MentionSubGraph predictedTree, MentionSubGraph latentTree,
                                         PairFeatureExtractor extractor) {
        GraphFeatureVector delta = latentTree.getDelta(predictedTree, extractor);

//        logger.info("Delta between the features are: ");
//        logger.info(delta.readableNodeVector());
        double loss = predictedTree.getLoss(latentTree);
        double l2 = getFeatureL2(delta);
        double deltaDotProd = weights.dotProd(delta);
//        double tau = (deltaDotProd - loss) / l2;
        double tau = loss / l2;


//        logger.info("Loss is " + loss + " PA update rate is " + tau + " dot prod is " + deltaDotProd + " l2 is " +
// l2);

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

    private boolean graphMatch(MentionSubGraph predictedTree, MentionGraph mentionGraph) {
        predictedTree.resolveTree();

        // Variable indicating whether the coreference clusters are matched.
        boolean corefMatch = Arrays.deepEquals(predictedTree.getCorefChains(), mentionGraph.getCorefChains());

        // Variable indicating whether the other mention links are matched.
        boolean linkMatch = true;
        for (Map.Entry<MentionGraphEdge.EdgeType, int[][]> predictEdgesWithType : predictedTree.getResolvedRelations()
                .entrySet()) {
            int[][] actualEdges = mentionGraph.getResolvedRelations().get(predictEdgesWithType.getKey());
            if (!Arrays.deepEquals(predictEdgesWithType.getValue(), actualEdges)) {
                linkMatch = false;
            }
        }
        return corefMatch && linkMatch;
    }

    public static void saveModels(File modelOutputDirectory) throws IOException {
        FileUtils.ensureDirectory(modelOutputDirectory);
        weights.write(new File(modelOutputDirectory, MODEL_NAME));
        org.apache.commons.io.FileUtils.write(new File(modelOutputDirectory, FEATURE_SPEC_FILE), featureSpec);
    }

}
