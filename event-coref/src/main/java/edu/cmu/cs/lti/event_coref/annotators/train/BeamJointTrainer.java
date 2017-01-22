package edu.cmu.cs.lti.event_coref.annotators.train;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.event_coref.decoding.BeamCrfLatentTreeDecoder;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.graph.LabelledMentionGraphEdge;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.MentionGraphEdge;
import edu.cmu.cs.lti.learning.update.DiscriminativeUpdater;
import edu.cmu.cs.lti.learning.utils.LearningUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MentionUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static edu.cmu.cs.lti.learning.model.ModelConstants.COREF_MODEL_NAME;
import static edu.cmu.cs.lti.learning.model.ModelConstants.TYPE_MODEL_NAME;

/**
 * Use Beam Search to do joint training of event head detection and event corefrence, using delayed LaSO to make use
 * of more training examples.
 *
 * @author Zhengzhong Liu
 */
public class BeamJointTrainer extends AbstractLoggingAnnotator {
    private static DiscriminativeUpdater updater;

    public static final String PARAM_CONFIG_PATH = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    public static final String PARAM_REALIS_MODEL_DIRECTORY = "realisModelDirectory";
    @ConfigurationParameter(name = PARAM_REALIS_MODEL_DIRECTORY)
    private File realisModelDirectory;

    public static final String PARAM_STRATEGY_TYPE = "strategyType";
    @ConfigurationParameter(name = PARAM_STRATEGY_TYPE, defaultValue = "0")
    private int strategyType;

    public static final String PARAM_WARM_START_MENTION_MODEL = "pretrainedMentionModelDirectory";
    @ConfigurationParameter(name = PARAM_WARM_START_MENTION_MODEL, mandatory = false)
    private File warmStartModelDir;

    public static final String PARAM_MENTION_LOSS_TYPE = "mentionLossType";
    @ConfigurationParameter(name = PARAM_MENTION_LOSS_TYPE)
    private String mentionLossType;

    public static final String PARAM_BEAM_SIZE = "beamSize";
    @ConfigurationParameter(name = PARAM_BEAM_SIZE)
    private int beamSize;

    public static final String PARAM_CACHE_DIR = "cacheDir";
    @ConfigurationParameter(name = PARAM_CACHE_DIR)
    private File cacheDir;

    public static final String PARAM_TWO_LAYER = "twoLayer";
    @ConfigurationParameter(name = PARAM_TWO_LAYER)
    private boolean useTwoLayer;

    public static final String PARAM_WARM_START_ITER = "warmStartIter";
    @ConfigurationParameter(name = PARAM_WARM_START_ITER)
    private int warmStartIter;

    private WekaModel realisModel;

    private SentenceFeatureExtractor realisExtractor;
    private SentenceFeatureExtractor crfExtractor;
    private PairFeatureExtractor corefExtractor;
    private BeamCrfLatentTreeDecoder decoder;

    private static int numIters = 0;

    private Map<String, MentionGraph> graphCache;

    private boolean warmStartNotProvided = false;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        logger.info("Preparing the Delayed LaSO Joint Trainer...");
        super.initialize(context);

        updater = new DiscriminativeUpdater(true, true, true, mentionLossType, 0, strategyType);

        // Doing warm start.
        if (warmStartModelDir != null && warmStartModelDir.exists()) {
            logger.info("Starting delayered LaSO trainer with label warm start.");
            logger.info("Warm start model is " + warmStartModelDir);
            updater.addWeightVector(TYPE_MODEL_NAME, usePretrainedCrfWeights());
            warmStartNotProvided = false;
        } else {
            logger.info("Warm start model not provided or not exists, will run warm start myself.");
            updater.addWeightVector(TYPE_MODEL_NAME, prepareCrfWeights());
            warmStartNotProvided = true;
        }

        updater.addWeightVector(COREF_MODEL_NAME, LearningUtils.preareGraphWeights(config,
                "edu.cmu.cs.lti.features.coref.spec"));

        prepareRealis();

        this.realisExtractor = LearningUtils.initializeRealisExtractor(config, "edu.cmu.cs.lti.features.realis.spec",
                realisModel.getAlphabet());

        this.crfExtractor = LearningUtils.initializeCrfExtractor(config,
                "edu.cmu.cs.lti.features.type.lv1.sentence.spec",
                "edu.cmu.cs.lti.features.type.lv1.doc.spec",
                updater.getWeightVector(TYPE_MODEL_NAME).getFeatureAlphabet()
        );
        this.corefExtractor = LearningUtils.initializeMentionPairExtractor(config, "edu.cmu.cs.lti.features.coref.spec",
                updater.getWeightVector(COREF_MODEL_NAME).getFeatureAlphabet());


        decoder = new BeamCrfLatentTreeDecoder(updater.getWeightVector(TYPE_MODEL_NAME), realisModel,
                updater.getWeightVector(COREF_MODEL_NAME), realisExtractor, crfExtractor,
                updater, mentionLossType, beamSize);


        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(cacheDir);
        logger.info("Cache will be put at " + cacheDir.getAbsolutePath());

        graphCache = new HashMap<>();

        logger.info("Will not use in memory cache.");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);

        List<StanfordCorenlpToken> allTokens = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpToken.class));
        List<EventMention> allMentions = MentionUtils.clearDuplicates(
                new ArrayList<>(JCasUtil.select(aJCas, EventMention.class))
        );

        // Each token is a candidate.
        List<MentionCandidate> systemCandidates = MentionUtils.createCandidatesFromTokens(aJCas, allTokens);
        List<MentionCandidate> goldCandidates = MentionUtils.createCandidatesFromTokens(aJCas, allTokens);

        // A candidate can corresponds to multiple types.
        SetMultimap<Integer, Integer> candidate2Node = HashMultimap.create();
        List<String> nodeTypes = new ArrayList<>();
        TIntIntMap mention2Node = new TIntIntHashMap();
        int numNodes = MentionUtils.labelTokenCandidates(allMentions, goldCandidates, candidate2Node, mention2Node,
                nodeTypes);

//        // Convert mention clusters to split candidate clusters.
//        Map<Integer, Integer> mention2event = MentionUtils.groupEventClusters(allMentions);
//        Map<Integer, Integer> node2EventId = MentionUtils.mapCandidate2Events(numNodes, mention2Node, mention2event);
//
//        // Read the relations.
//        Map<Pair<Integer, Integer>, String> relations = MentionUtils.indexRelations(aJCas, mention2Node, allMentions);

        // Init here so that we can extract features for mention graph.
        this.corefExtractor.initWorkspace(aJCas);

        //TODO need to use token based candidates.
        List<MentionCandidate> candidates = MentionUtils.getSpanBasedCandidates(aJCas);
        MentionGraph mentionGraph = MentionUtils.createSpanBasedMentionGraph(aJCas, candidates, corefExtractor, true);

//        MentionGraph mentionGraph = new MentionGraph(goldCandidates, candidate2Node, nodeTypes, node2EventId,
//                relations, mentionPairExtractor, true);

//        logger.debug("Starting decoding.");

        String name = UimaConvenience.getShortDocumentNameWithOffset(aJCas);

//        logger.info("Reading cache.");
//        try {
//            preloadFeature(cacheDir, name, mentionGraph);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        if (graphCache.containsKey(name)) {
//            mentionGraph = graphCache.get(name);
//        }
//        logger.info("Done reading.");

        // If we do not have warm start, we will skip the first few iteration to train mention alone.
        boolean skipCoref = numIters < warmStartIter && warmStartNotProvided;

        // TODO debug purpose.
        if (skipCoref) {
            DiscriminativeUpdater.debugger = false;
        } else {
            DiscriminativeUpdater.debugger = true;
        }

//        if (skipCoref) {
//            logger.info("Skipping coreference training for this round to wait for stable: numIters =" + numIters);
//        }

//        logger.info("Start deocding.");
        decoder.decode(aJCas, mentionGraph, systemCandidates, goldCandidates, useTwoLayer, skipCoref);
//        logger.info("Done decoding last one.");

//        graphCache.put(name, mentionGraph);

//        logger.info("Writing features.");
//        try {
//            saveFeatures(cacheDir, name, mentionGraph, goldCandidates, systemCandidates);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        logger.info("Done writing.");

//        UimaConvenience.printProcessLog(aJCas, logger);
    }

    private void saveFeatures(File cacheDir, String fileName, MentionGraph mentionGraph,
                              List<MentionCandidate> gold, List<MentionCandidate> system) throws IOException {
        int numNodes = mentionGraph.numNodes();
        List[][] allFeatures = new List[numNodes][];

        for (int gov = 0; gov < numNodes - 1; gov++) {
            allFeatures[gov] = new List[numNodes];
            for (int dep = 1; dep < numNodes; dep++) {
                Table<NodeKey, NodeKey, FeatureVector> featureTable = HashBasedTable.create();
                MentionGraphEdge edge = mentionGraph.getEdge(dep, gov);
                storeEdgeFeature(gov, dep, gold, edge, featureTable);
                storeEdgeFeature(gov, dep, system, edge, featureTable);

                allFeatures[gov][dep] = new ArrayList<>();

                for (Table.Cell<NodeKey, NodeKey, FeatureVector> featureItem : featureTable.cellSet()) {
                    allFeatures[gov][dep].add(featureItem);
                }
            }
        }

        File featureCahe = new File(cacheDir, fileName + ".ser");
        SerializationUtils.serialize(allFeatures, new GZIPOutputStream(new FileOutputStream(featureCahe, false)));
    }

    private void storeEdgeFeature(int gov, int dep, List<MentionCandidate>
            candidates, MentionGraphEdge edge, Table<NodeKey, NodeKey, FeatureVector> featureTable) {
        MentionKey govKeys = gov == 0 ? MentionKey.rootKey() : candidates.get(MentionGraph
                .getCandidateIndex(gov)).asKey();
        MentionKey depKeys = candidates.get(MentionGraph.getCandidateIndex(dep)).asKey();

        for (NodeKey govKey : govKeys) {
            for (NodeKey depKey : depKeys) {
                if (!featureTable.contains(govKey, depKey)) {
                    LabelledMentionGraphEdge existingEdge = edge.getExistingLabelledEdge(govKey, depKey);
                    if (existingEdge != null) {
                        FeatureVector features = edge.getExistingLabelledEdge(govKey, depKey).getFeatureVector();
                        featureTable.put(govKey, depKey, features);
                    }
                }
            }
        }
    }

//    private void preloadFeature(File cacheDir, String fileName, MentionGraph mentionGraph) throws IOException {
//        File featureCahe = new File(cacheDir, fileName + ".ser");
//
//        if (!featureCahe.exists()) {
//            return;
//        }
//
//        List[][] allFeatures = SerializationUtils.deserialize(new GZIPInputStream(new FileInputStream(featureCahe)));
//
//        FeatureAlphabet alphabet = updater.getWeightVector(COREF_MODEL_NAME).getFeatureAlphabet();
//
//        int numNodes = mentionGraph.numNodes();
//
//        for (int gov = 0; gov < numNodes - 1; gov++) {
//            for (int dep = 1; dep < numNodes; dep++) {
//                MentionGraphEdge edge = mentionGraph.getMentionGraphEdge(dep, gov);
//                List<Table.Cell<NodeKey, NodeKey, FeatureVector>> labelledFeatures = allFeatures[gov][dep];
//
//                for (Table.Cell<NodeKey, NodeKey, FeatureVector> labelledFeature : labelledFeatures) {
//                    NodeKey govKey = labelledFeature.getRowKey();
//                    NodeKey depKey = labelledFeature.getColumnKey();
//                    FeatureVector fv = labelledFeature.getValue();
//                    fv.setAlpabhet(alphabet);
//                    edge.createLabelledEdgeWithFeatures(govKey, depKey, labelledFeature.getValue());
//                }
//            }
//        }
//    }

    public static void saveModels(File modelOutputDirectory) throws FileNotFoundException {
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(modelOutputDirectory);
        updater.getWeightVector(COREF_MODEL_NAME).write(new File(modelOutputDirectory, COREF_MODEL_NAME));
        updater.getWeightVector(TYPE_MODEL_NAME).write(new File(modelOutputDirectory, TYPE_MODEL_NAME));
    }

    public static void loopAction() {
        numIters++;
    }

    public static void finish() {
        numIters = 0;
    }

    private void prepareRealis() throws ResourceInitializationException {
        logger.info("Loading Realis models ...");
        try {
            realisModel = new WekaModel(realisModelDirectory);
        } catch (Exception e) {
            throw new ResourceInitializationException(e);
        }
    }

    private GraphWeightVector prepareCrfWeights() throws ResourceInitializationException {
        logger.info("Initializing labeling weights.");

        int alphabetBits = config.getInt("edu.cmu.cs.lti.mention.feature.alphabet_bits", 24);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.mention.readableModel", false);
        String sentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.sentence.spec");
        String docFeatureSpec = config.getOrElse("edu.cmu.cs.lti.features.type.lv1.doc.spec", "");
        File classFile = new File(edu.cmu.cs.lti.utils.FileUtils.joinPaths(
                config.get("edu.cmu.cs.lti.training.working.dir"), "mention_types.txt"));

        String[] classes;
        if (classFile.exists()) {
            try {
                classes = FileUtils.readLines(classFile).stream().map(l -> l.split("\t"))
                        .filter(p -> p.length >= 1).map(p -> p[0]).toArray(String[]::new);
                logger.info(String.format("Registered %d classes.", classes.length));
            } catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
        } else {
            throw new ResourceInitializationException(new Throwable("No classes provided for training"));
        }

        ClassAlphabet classAlphabet = new ClassAlphabet(classes, true, true);
        HashAlphabet featureAlphabet = new HashAlphabet(alphabetBits, readableModel);

        return new GraphWeightVector(classAlphabet, featureAlphabet,
                FeatureUtils.joinFeatureSpec(sentFeatureSpec, docFeatureSpec));
    }

    private GraphWeightVector usePretrainedCrfWeights() throws ResourceInitializationException {
        GraphWeightVector weightVector;
        try {
            weightVector = SerializationUtils.deserialize(new FileInputStream((new File(warmStartModelDir,
                    TYPE_MODEL_NAME))));
        } catch (FileNotFoundException e) {
            throw new ResourceInitializationException(e);
        }
        return weightVector;
    }
}
