package edu.cmu.cs.lti.event_coref.annotators.train;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import edu.cmu.cs.lti.emd.utils.MentionUtils;
import edu.cmu.cs.lti.event_coref.decoding.BeamCrfLatentTreeDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.update.DiscriminativeUpdater;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.List;
import java.util.Map;

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

//    public static final String PARAM_WARM_START_MENTION_MODEL = "pretrainedMentionModelDirectory";
//    @ConfigurationParameter(name = PARAM_WARM_START_MENTION_MODEL, mandatory = false)
//    private File warmStartMentionModel;

    public static final String PARAM_USE_WARM_START = "useWarmStart";
    @ConfigurationParameter(name = PARAM_USE_WARM_START, defaultValue = "false")
    private boolean warmStart;

    public static final String PARAM_MENTION_LOSS_TYPE = "mentionLossType";
    @ConfigurationParameter(name = PARAM_MENTION_LOSS_TYPE)
    private String mentionLossType;

    public static final String PARAM_BEAM_SIZE = "beamSize";
    @ConfigurationParameter(name = PARAM_BEAM_SIZE)
    private int beamSize;

    public static final String PARAM_USE_LASO = "useLaso";
    @ConfigurationParameter(name = PARAM_USE_LASO)
    private boolean useLaSO;

    private WekaModel realisModel;

    private SentenceFeatureExtractor realisExtractor;
    private SentenceFeatureExtractor crfExtractor;
    private PairFeatureExtractor mentionPairExtractor;
    private BeamCrfLatentTreeDecoder decoder;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        logger.info("Preparing the Delayed LaSO Trainer...");
        super.initialize(context);

        updater = new DiscriminativeUpdater(true, true, true, mentionLossType);

        // Doing warm start.
        if (warmStart) {
//            logger.info("Starting delayered LaSO trainer with label warm start.");
//            logger.info("Warm start model is " + warmStartMentionModel);
//            updater.addWeightVector(TYPE_MODEL_NAME, usePretrainedCrfWeights());
            throw new IllegalArgumentException("Not implemented warm start.");
        } else {
            updater.addWeightVector(TYPE_MODEL_NAME, prepareCrfWeights());
        }

        updater.addWeightVector(COREF_MODEL_NAME, preareCorefWeights());

        prepareRealis();

        try {
            this.realisExtractor = initializeRealisExtractor(config);
            this.crfExtractor = initializeCrfExtractor(config);
            this.mentionPairExtractor = initializeMentionPairExtractor(config);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | InstantiationException e) {
            e.printStackTrace();
        }

        try {
            decoder = new BeamCrfLatentTreeDecoder(updater.getWeightVector(TYPE_MODEL_NAME), realisModel,
                    updater.getWeightVector(COREF_MODEL_NAME), realisExtractor, crfExtractor,
                    updater, mentionLossType, beamSize, useLaSO);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | InstantiationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        UimaConvenience.printProcessLog(aJCas, logger);

        List<StanfordCorenlpToken> allTokens = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpToken.class));
        List<EventMention> allMentions = MentionUtils.clearDuplicates(
                new ArrayList<>(JCasUtil.select(aJCas, EventMention.class))
        );

        // Each token is a candidate.
        List<MentionCandidate> systemCandidates = MentionUtils.createCandidatesFromTokens(aJCas, allTokens);
        List<MentionCandidate> goldCandidates = MentionUtils.createCandidatesFromTokens(aJCas, allTokens);

        // A candidate can corresponds to multiple types.
        SetMultimap<Integer, Integer> candidate2Split = HashMultimap.create();
        List<String> splitCandidateTypes = new ArrayList<>();
        TIntIntMap mention2SplitCandidate = new TIntIntHashMap();
        int numSplitCandidates = MentionUtils.processCandidates(allMentions, goldCandidates, candidate2Split,
                mention2SplitCandidate, splitCandidateTypes);

        // Convert mention clusters to split candidate clusters.
        Map<Integer, Integer> mention2event = MentionUtils.groupEventClusters(allMentions);
        Map<Integer, Integer> splitCandidate2EventId = MentionUtils.mapCandidate2Events(numSplitCandidates,
                mention2SplitCandidate, mention2event);

        // Read the relations.
        Map<Pair<Integer, Integer>, String> relations = MentionUtils.indexRelations(aJCas, mention2SplitCandidate,
                allMentions);

        // Init here so that we can extract features for mention graph.
        this.mentionPairExtractor.initWorkspace(aJCas);
        MentionGraph mentionGraph = new MentionGraph(goldCandidates, candidate2Split, splitCandidateTypes,
                splitCandidate2EventId, relations, mentionPairExtractor, true);

//        logger.debug("Starting decoding.");
        decoder.decode(aJCas, mentionGraph, systemCandidates, goldCandidates, false);
//        logger.debug("Done decoding last one.");
    }

    public static void saveModels(File modelOutputDirectory) throws FileNotFoundException {
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(modelOutputDirectory);
        updater.getWeightVector(COREF_MODEL_NAME).write(new File(modelOutputDirectory, COREF_MODEL_NAME));
        updater.getWeightVector(TYPE_MODEL_NAME).write(new File(modelOutputDirectory, TYPE_MODEL_NAME));
    }

    public static void finish() {

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
        File classFile = config.getFile("edu.cmu.cs.lti.mention.classes.path");

        String[] classes;
        if (classFile != null) {
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

        ClassAlphabet classAlphabet = new ClassAlphabet(classes, false, true);
        HashAlphabet featureAlphabet = new HashAlphabet(alphabetBits, readableModel);

        return new GraphWeightVector(classAlphabet, featureAlphabet,
                FeatureUtils.joinFeatureSpec(sentFeatureSpec, docFeatureSpec));
    }

//    private GraphWeightVector usePretrainedCrfWeights() throws ResourceInitializationException {
//        GraphWeightVector weightVector;
//        try {
//            weightVector = SerializationUtils.deserialize(new FileInputStream((warmStartMentionModel)));
//        } catch (FileNotFoundException e) {
//            throw new ResourceInitializationException(e);
//        }
//        return weightVector;
//    }

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


    private SentenceFeatureExtractor initializeRealisExtractor(Configuration config) throws ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        String featurePackageName = config.get("edu.cmu.cs.lti.feature.sentence.package.name");
        String featureSpec = config.get("edu.cmu.cs.lti.features.realis.spec");

        FeatureSpecParser parser = new FeatureSpecParser(featurePackageName);
        Configuration realisSpec = parser.parseFeatureFunctionSpecs(featureSpec);

        // Currently no document level realis features.
        Configuration placeHolderSpec = new Configuration();
        return new SentenceFeatureExtractor(realisModel.getAlphabet(), config, realisSpec, placeHolderSpec, false);
    }

    private SentenceFeatureExtractor initializeCrfExtractor(Configuration config) throws ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        String sentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.sentence.spec");
        String docFeatureSpec = config.getOrElse("edu.cmu.cs.lti.features.type.lv1.doc.spec", "");

        Configuration sentFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.sentence.package.name")
        ).parseFeatureFunctionSpecs(sentFeatureSpec);

        Configuration docFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.document.package.name")
        ).parseFeatureFunctionSpecs(docFeatureSpec);
        return new SentenceFeatureExtractor(updater.getWeightVector(TYPE_MODEL_NAME).getFeatureAlphabet(), config,
                sentFeatureConfig, docFeatureConfig, false /**use state feature?**/);
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

}
