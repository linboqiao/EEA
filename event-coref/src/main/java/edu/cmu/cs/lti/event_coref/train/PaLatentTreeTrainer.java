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
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import gnu.trove.iterator.TIntObjectIterator;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileNotFoundException;
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

    public static final String PARAM_CONFIG_PATH = "configPath";

    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    private static PairFeatureExtractor extractor;
    private static GraphWeightVector weights;
    private static ClassAlphabet classAlphabet;
    private static FeatureAlphabet featureAlphabet;
    private static LatentTreeDecoder decoder;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        weights = new GraphWeightVector(classAlphabet, featureAlphabet);
        logger.info("Initialize perceptron trainer");

        int alphabetBits = config.getInt("edu.cmu.cs.lti.coref.feature.alphabet_bits", 22);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.coref.readableModel", false);
        boolean useBinaryFeatures = config.getBoolean("edu.cmu.cs.lti.coref.binaryFeature", false);
        classAlphabet = new ClassAlphabet();
        for (EdgeType edgeType : EdgeType.values()) {
            classAlphabet.addClass(edgeType.name());
        }
        featureAlphabet = HashAlphabet.getInstance(alphabetBits, readableModel);
        decoder = new BestFirstLatentTreeDecoder();

        String featureSpec = config.get("edu.cmu.cs.lti.features.coref.spec");

        try {
            extractor = new PairFeatureExtractor(featureAlphabet, useBinaryFeatures, config, new FeatureSpecParser
                    (config
                            .get("edu.cmu.cs.lti.feature.sentence.package.name"))
                    .parseFeatureFunctionSpecs(featureSpec));
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        printProcessInfo(aJCas, logger);
        List<EventMention> allMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));
        List<EventMentionRelation> allMentionRelations = new ArrayList<>(JCasUtil.select(aJCas, EventMentionRelation
                .class));

        // Feed the extractor with document information.

        // A mention graph represent all the mentions and contains features among them.
        MentionGraph mentionGraph = new MentionGraph(aJCas, allMentions, allMentionRelations, featureAlphabet,
                classAlphabet, extractor);

        //  Decoding.
        MentionSubGraph predictedTree = decoder.decode(mentionGraph, featureAlphabet, classAlphabet, weights,
                extractor);

        logger.info("Predicted Tree is ");
        logger.info(predictedTree.toString());

//        logger.info("Labelled feature for the predicted Tree");
//        logger.info(predictedTree.getAllEdgeFeatures().toString());

        if (!graphMatch(predictedTree, mentionGraph)) {
            MentionSubGraph latentTree = mentionGraph.getLatentTree(weights);
//            logger.info("Labelled feature for the latent Tree");
//            logger.info(latentTree.toString());
//            logger.info("Latent Tree is ");
//            logger.info(latentTree.getAllEdgeFeatures().toString());
//            logger.info("Labelled feature for the predicted Tree is now");
//            logger.info(predictedTree.getAllEdgeFeatures().toString());

            GraphFeatureVector delta = latentTree.getDelta(predictedTree);
            logger.info(delta.toString());
            double loss = predictedTree.getLoss(latentTree);
            double tau = getUpdateWeight(delta, loss);
            weights.updateWeightsBy(delta, tau);
            weights.updateAverageWeights();
        }
    }

    /**
     * Compute the update weight. The current implementation is a Passive-Aggressive weight.
     *
     * @param delta The difference of the gold standard and prediction
     * @param loss  The loss computed by the loss function.
     * @return The update weight.
     */
    public double getUpdateWeight(GraphFeatureVector delta, double loss) {
        return (weights.dotProd(delta) + loss) / getFeatureL2(delta);
    }


    /**
     * Compute the L2 norm of the feature vector.
     *
     * @param typedFeatures A vector that store features with each type separated
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
        for (Map.Entry<MentionGraphEdge.EdgeType, int[][]> predictEdgesWithType : predictedTree.getEdgeAdjacentList()
                .entrySet()) {
            int[][] actualEdges = mentionGraph.getNonEquivalentEdges().get(predictEdgesWithType.getKey());
            if (!Arrays.deepEquals(predictEdgesWithType.getValue(), actualEdges)) {
                linkMatch = false;
            }
        }
        return corefMatch && linkMatch;
    }

    public static void saveModels(File modelOutputDirectory) throws FileNotFoundException {
        FileUtils.ensureDirectory(modelOutputDirectory);
        weights.write(new File(modelOutputDirectory, MODEL_NAME));
    }

}
