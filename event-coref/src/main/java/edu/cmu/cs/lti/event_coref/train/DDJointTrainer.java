package edu.cmu.cs.lti.event_coref.train;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.emd.annotators.crf.MentionSequenceCrfTrainer;
import edu.cmu.cs.lti.emd.utils.MentionTypeUtils;
import edu.cmu.cs.lti.event_coref.decoding.DDLatentTreeCrfDecoder;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge;
import edu.cmu.cs.lti.event_coref.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.MultiSentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TIntObjectIterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/29/15
 * Time: 1:11 AM
 *
 * @author Zhengzhong Liu
 */
public class DDJointTrainer extends AbstractLoggingAnnotator {
//    public static final String COREF_MODEL_NAME = "latentTreeModel";
//
//    public static final String MENTION_MODEL_NAME = "crfModel";

    public static final String PARAM_CONFIG_PATH = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    public static final String PARAM_COREF_RULE_FILE = "corefRuleFile";
    @ConfigurationParameter(name = PARAM_COREF_RULE_FILE)
    File corefRuleFile;

    FeatureAlphabet mentionFeatureAlphabet;
    ClassAlphabet mentionClassAlphabet;

    DDLatentTreeCrfDecoder decoder;

    MultiSentenceFeatureExtractor<EventMention> mentionFeatureExtractor;
    PairFeatureExtractor corefFeatureExtractor;

    static GraphWeightVector mentionWeights;
    static GraphWeightVector corefWeights;

    private TrainingStats corefTrainingStats;
    private TrainingStats typeTrainingStats;

    private double mentionStepSize;


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        logger.info("Initialize latent tree perceptron trainer");

        prepareMentionTraining();
        prepareCorefTraining();

        ArrayListMultimap<Integer, Integer> allowedCorefs = null;

        try {
            allowedCorefs = MentionTypeUtils.findAllowedCorefTypes(mentionClassAlphabet,
                    FileUtils.readLines(corefRuleFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        decoder = new DDLatentTreeCrfDecoder(mentionFeatureAlphabet, mentionClassAlphabet, allowedCorefs);

        logger.info("Joint trainer initialized.");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger, true);
        List<EventMentionRelation> allMentionRelations = new ArrayList<>(
                JCasUtil.select(aJCas, EventMentionRelation.class));

        mentionFeatureExtractor.initWorkspace(aJCas);
        corefFeatureExtractor.initWorkspace(aJCas);

        DocumentAnnotation document = JCasUtil.selectSingle(aJCas, DocumentAnnotation.class);
        mentionFeatureExtractor.resetWorkspace(aJCas, document.getBegin(), document.getEnd());

        // Get the gold sequence before decoding, because the decoding procedure change the JCas.
        SequenceSolution goldSolution = getGoldSequence(aJCas);

        logger.debug("Gold solution.");
        logger.debug(goldSolution.toString());

        List<EventMention> allMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));
        Map<Integer, Integer> mentionId2EventId = groupEventClusters(allMentions);

        MentionGraph mentionGraph = new MentionGraph(allMentions.size(), mentionId2EventId, allMentionRelations);

        Pair<SequenceSolution, MentionSubGraph> decodeResult = decoder.decode(mentionFeatureExtractor,
                mentionWeights, mentionGraph, allMentions, corefFeatureExtractor, corefWeights, false);

        // Decoding.
        MentionSubGraph predictedTree = decodeResult.getRight();

        // Update coreference
        if (!graphMatch(predictedTree, mentionGraph)) {
            MentionSubGraph latentTree = mentionGraph.getLatentTree(corefWeights, corefFeatureExtractor);
            double loss = predictedTree.getLoss(latentTree);
            corefTrainingStats.addLoss(logger, loss / mentionGraph.getMentionNodes().length);
            passiveCorefAggressiveUpdate(predictedTree, latentTree, corefFeatureExtractor);
        } else {
            corefTrainingStats.addLoss(logger, 0);
        }

        // Update CRF
        GraphFeatureVector goldFv = getSolutionFeatures(mentionFeatureExtractor, goldSolution);

        SequenceSolution prediction = decodeResult.getLeft();
        double loss = goldSolution.loss(prediction);
        if (loss != 0) {
            GraphFeatureVector bestDecodingFeatures = decoder.getBestDecodingFeatures();
            mentionTypeUpdate(goldFv, bestDecodingFeatures);
        }

        typeTrainingStats.addLoss(logger, loss);
    }

    private void prepareMentionTraining() throws ResourceInitializationException {
        int alphabetBits = config.getInt("edu.cmu.cs.lti.coref.feature.alphabet_bits", 22);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.coref.readableModel", false);

        File classFile = config.getFile("edu.cmu.cs.lti.mention.classes.path");
        String[] classes = new String[0];

        if (classFile != null) {
            try {
                classes = FileUtils.readLines(classFile).stream().map(l -> l.split("\t"))
                        .filter(p -> p.length >= 1).map(p -> p[0]).toArray(String[]::new);
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.info(String.format("Registered %d classes.", classes.length));
        } else {
            throw new ResourceInitializationException(new Throwable("No classes provided for training"));
        }

        mentionStepSize = config.getDouble("edu.cmu.cs.lti.perceptron.stepsize", 0.01);

        mentionClassAlphabet = new ClassAlphabet(classes, false, true);
        mentionFeatureAlphabet = new HashAlphabet(alphabetBits, readableModel);

        try {
            FeatureSpecParser sentFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.sentence.package.name"));

            FeatureSpecParser docFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.document.package.name")
            );

            String docFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv2.doc.spec");
            String sentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv2.sentence.spec");

            logger.info("Document feature spec : " + docFeatureSpec);
            logger.info("Sentence feature spec : " + sentFeatureSpec);

            boolean useStateFeature = config.getBoolean("edu.cmu.cs.lti.mention.use_state", true);

            Configuration sentFeatureConfig = sentFeatureSpecParser.parseFeatureFunctionSpecs(sentFeatureSpec);
            Configuration mentionFeatureConfig =
                    docFeatureSpecParser.parseFeatureFunctionSpecs(docFeatureSpec);

            mentionWeights = new GraphWeightVector(mentionClassAlphabet, mentionFeatureAlphabet,
                    FeatureUtils.joinFeatureSpec(sentFeatureSpec, docFeatureSpec));

            logger.info("Mention feature alphabet size " + mentionFeatureAlphabet.getAlphabetSize());

            mentionFeatureExtractor = new MultiSentenceFeatureExtractor<>(mentionFeatureAlphabet, config,
                    sentFeatureConfig, mentionFeatureConfig, useStateFeature, EventMention.class);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }

        typeTrainingStats = new TrainingStats(5, "CRF");

        logger.info("Mention training prepared.");
    }

    private void prepareCorefTraining() {
        int alphabetBits = config.getInt("edu.cmu.cs.lti.coref.feature.alphabet_bits", 22);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.coref.readableModel", false);
        boolean useBinaryFeatures = config.getBoolean("edu.cmu.cs.lti.coref.binaryFeature", false);

        ClassAlphabet classAlphabet = new ClassAlphabet();
        for (MentionGraphEdge.EdgeType edgeType : MentionGraphEdge.EdgeType.values()) {
            classAlphabet.addClass(edgeType.name());
        }

        FeatureAlphabet featureAlphabet = new HashAlphabet(alphabetBits, readableModel);
        String featureSpec = config.get("edu.cmu.cs.lti.features.coref.spec");

        corefWeights = new GraphWeightVector(classAlphabet, featureAlphabet, featureSpec);

        try {
            logger.debug(featureSpec);
            Configuration featureConfig = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.pair.package.name")
            ).parseFeatureFunctionSpecs(featureSpec);
            corefFeatureExtractor = new PairFeatureExtractor(featureAlphabet, classAlphabet, useBinaryFeatures, config,
                    featureConfig);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }

        corefTrainingStats = new TrainingStats(5, "LatentTree");

        logger.info("Latent Tree training prepared.");
    }

    /**
     * @return Map from the mention to the event index it refers.
     */
    private Map<Integer, Integer> groupEventClusters(List<EventMention> mentions) {
        Map<Integer, Integer> mentionId2EventId = new HashMap<>();
        int eventIndex = 0;

        Map<Event, Integer> eventIndices = new HashMap<>();

        for (int i = 0; i < mentions.size(); i++) {
            Event referringEvent = mentions.get(i).getReferringEvent();
            if (referringEvent == null) {
                mentionId2EventId.put(i, eventIndex);
                eventIndex++;
            } else if (eventIndices.containsKey(referringEvent)) {
                Integer referringIndex = eventIndices.get(referringEvent);
                mentionId2EventId.put(i, referringIndex);
            } else {
                mentionId2EventId.put(i, eventIndex);
                eventIndices.put(referringEvent, eventIndex);
                eventIndex++;
            }
        }
        return mentionId2EventId;
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

    /**
     * Update the weights by the difference of the predicted tree and the gold latent tree using Passive-Aggressive
     * algorithm.
     *
     * @param predictedTree The predicted tree.
     * @param latentTree    The gold latent tree.
     */
    private void passiveCorefAggressiveUpdate(MentionSubGraph predictedTree, MentionSubGraph latentTree,
                                              PairFeatureExtractor extractor) {
        GraphFeatureVector delta = latentTree.getDelta(predictedTree, extractor);

//        logger.info("Delta between the features are: ");
//        logger.info(delta.readableNodeVector());
        double loss = predictedTree.getLoss(latentTree);
        double l2 = getFeatureL2(delta);
//        double deltaDotProd = weights.dotProd(delta);
//        double tau = (deltaDotProd - loss) / l2;
        double tau = loss / l2;

//        logger.info("Loss is " + loss + " update rate is " + tau + " dot prod is " + deltaDotProd + " l2 is " + l2);

        corefWeights.updateWeightsBy(delta, tau);
        corefWeights.updateAverageWeights();
    }

    private void mentionTypeUpdate(GraphFeatureVector goldFv, GraphFeatureVector predictedFv) {
        mentionWeights.updateWeightsBy(goldFv, mentionStepSize);
        mentionWeights.updateWeightsBy(predictedFv, -mentionStepSize);
        mentionWeights.updateAverageWeights();
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


    private SequenceSolution getGoldSequence(JCas aJCas) {
        Collection<EventMention> mentions = JCasUtil.select(aJCas, EventMention.class);

        int[] goldSequence = new int[mentions.size()];

        int sequenceIdx = 0;

        for (EventMention mention : mentions) {
            goldSequence[sequenceIdx] = mentionClassAlphabet.getClassIndex(mention.getEventType());
            sequenceIdx++;
        }

        return new SequenceSolution(mentionClassAlphabet, goldSequence);
    }

    private GraphFeatureVector getSolutionFeatures(ChainFeatureExtractor extractor, SequenceSolution solution) {
        GraphFeatureVector fv = new GraphFeatureVector(mentionClassAlphabet, mentionFeatureAlphabet, true);

        for (int solutionIndex = 0; solutionIndex <= solution.getSequenceLength(); solutionIndex++) {
            FeatureVector nodeFeatures = new RealValueHashFeatureVector(mentionFeatureAlphabet);
            FeatureVector edgeFeatures = new RealValueHashFeatureVector(mentionFeatureAlphabet);

            extractor.extract(solutionIndex, nodeFeatures, edgeFeatures);

            int classIndex = solution.getClassAt(solutionIndex);

            fv.extend(nodeFeatures, classIndex);
        }
        return fv;
    }

    public static void finish() {
    }

    public static void saveModels(File modelOutputDirectory) throws IOException {
        boolean directoryExist = true;
        if (!modelOutputDirectory.exists()) {
            if (!modelOutputDirectory.mkdirs()) {
                directoryExist = false;
            }
        }

        if (directoryExist) {
            mentionWeights.write(new File(modelOutputDirectory, MentionSequenceCrfTrainer.MODEL_NAME));
            corefWeights.write(new File(modelOutputDirectory, PaLatentTreeTrainer.MODEL_NAME));
        } else {
            throw new IOException(String.format("Cannot create directory : [%s]", modelOutputDirectory.toString()));
        }
    }
}
