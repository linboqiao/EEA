package edu.cmu.cs.lti.event_coref.train;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import edu.cmu.cs.lti.emd.annotators.crf.MentionTypeCrfTrainer;
import edu.cmu.cs.lti.emd.utils.MentionUtils;
import edu.cmu.cs.lti.event_coref.decoding.BeamCrfLatentTreeDecoder;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge;
import edu.cmu.cs.lti.event_coref.update.DiscriminativeUpdater;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.javatuples.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Use Beam Search to do joint training of event head detection and event corefrence, using delayed LaSO to make use
 * of more training examples.
 *
 * @author Zhengzhong Liu
 */
public class DelayedLaSOJointTrainer extends AbstractLoggingAnnotator {
    private static DiscriminativeUpdater updater;

    public static final String TYPE_MODEL_NAME = "CRF";

    public static final String COREF_MODEL_NAME = "COREF";

    public static final String PARAM_CONFIG_PATH = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    public static final String PARAM_REALIS_MODEL_DIRECTORY = "realisModelDirectory";
    @ConfigurationParameter(name = PARAM_REALIS_MODEL_DIRECTORY)
    private File realisModelDirectory;

    public static final String PARAM_PRETRAINED_MENTION_MODEL_DIRECTORY = "pretrainedMentionModelDirectory";
    @ConfigurationParameter(name = PARAM_PRETRAINED_MENTION_MODEL_DIRECTORY)
    private File pretrainedMentionModelDirectory;

    public static final String PARAM_USE_WARM_START = "useWarmStart";
    @ConfigurationParameter(name = PARAM_USE_WARM_START)
    private boolean warmStart;

    private WekaModel realisModel;

    private SentenceFeatureExtractor realisExtractor;
    private SentenceFeatureExtractor crfExtractor;
    private PairFeatureExtractor mentionPairExtractor;
    private BeamCrfLatentTreeDecoder decoder;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        logger.info("Preparing the Delayed LaSO Trainer...");
        super.initialize(context);

        updater = new DiscriminativeUpdater();

        // Doing warm start.
        if (warmStart) {
            logger.info("Starting delayered LaSO trainer with label warm start.");
            updater.addWeightVector(TYPE_MODEL_NAME, usePretrainedCrfWeights());
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
                    updater.getWeightVector(COREF_MODEL_NAME), realisExtractor, crfExtractor, mentionPairExtractor,
                    updater);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | InstantiationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<StanfordCorenlpToken> allTokens = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpToken.class));
        List<EventMention> allMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));

        // Each token is a candidate.
        List<MentionCandidate> systemCandidates = MentionUtils.createCandidatesFromTokens(aJCas, allTokens);
        List<MentionCandidate> goldCandidates = MentionUtils.createCandidatesFromTokens(aJCas, allTokens);

        // A candidate can corresponds to multiple types.
        SetMultimap<Integer, Integer> candidate2Split = HashMultimap.create();
        List<String> splitCandidateTypes = new ArrayList<>();
        TIntIntMap mention2SplitCandidate = new TIntIntHashMap();
        int numSplitCandidates = processCandidates(allMentions, goldCandidates, candidate2Split,
                mention2SplitCandidate, splitCandidateTypes);

        // Convert mention clusters to split candidate clusters.
        Map<Integer, Integer> mention2event = MentionUtils.groupEventClusters(allMentions);
        Map<Integer, Integer> splitCandidate2EventId = mapCandidate2Events(numSplitCandidates,
                mention2SplitCandidate, mention2event);

        // Read the relations.
        Map<Pair<Integer, Integer>, String> relations = MentionUtils.indexRelations(aJCas, mention2SplitCandidate,
                allMentions);

        this.mentionPairExtractor.initWorkspace(aJCas);

//        UimaConvenience.printProcessLog(aJCas, logger);
//        logger.debug("Creating mention graph.");

        MentionGraph mentionGraph = new MentionGraph(goldCandidates, candidate2Split, splitCandidateTypes,
                splitCandidate2EventId, relations, mentionPairExtractor, true);

//        logger.debug("Starting decoding.");
        decoder.decode(aJCas, mentionGraph, systemCandidates, goldCandidates, false);
//        logger.debug("Done decoding last one.");
    }

    private int processCandidates(List<EventMention> mentions, List<MentionCandidate> goldCandidates,
                                  SetMultimap<Integer, Integer> candidate2Split, TIntIntMap mention2SplitCandidate,
                                  List<String> splitCandidateTypes) {
        SetMultimap<Word, Integer> head2Mentions = HashMultimap.create();

        for (int i = 0; i < mentions.size(); i++) {
            head2Mentions.put(mentions.get(i).getHeadWord(), i);
        }

        int splitCandidateId = 0;
        for (int candidateIndex = 0; candidateIndex < goldCandidates.size(); candidateIndex++) {
            MentionCandidate candidate = goldCandidates.get(candidateIndex);
            Word candidateHead = candidate.getHeadWord();
            if (head2Mentions.containsKey(candidateHead)) {

                Set<Integer> correspondingMentions = head2Mentions.get(candidateHead);
                String mentionType = MentionTypeUtils.joinMultipleTypes(correspondingMentions.stream()
                        .map(mentions::get).map(EventMention::getEventType).collect(Collectors.toList()));
                candidate.setMentionType(mentionType);

//                logger.debug(String.format("Candidate Id : %d, split type : %d", candidateIndex, splitCandidateId));
//                logger.debug(String.format("%s types: %s", correspondingMentions.size(), mentionType));

                for (Integer mentionId : head2Mentions.get(candidateHead)) {
                    EventMention mention = mentions.get(mentionId);
                    candidate.setRealis(mention.getRealisType());
                    candidate2Split.put(candidateIndex, splitCandidateId);
                    splitCandidateTypes.add(mention.getEventType());
                    mention2SplitCandidate.put(mentionId, splitCandidateId);
                    splitCandidateId++;
                }
            } else {
                candidate.setMentionType(ClassAlphabet.noneOfTheAboveClass);
                candidate.setRealis(ClassAlphabet.noneOfTheAboveClass);
                splitCandidateTypes.add(ClassAlphabet.noneOfTheAboveClass);
                candidate2Split.put(candidateIndex, splitCandidateId);
                splitCandidateId++;
            }
        }

//        logger.debug(String.format("Number of mentions %d, number of candidates %d, number of split types %d",
//                mentions.size(), goldCandidates.size(), splitCandidateId));

        return splitCandidateId;
    }

    private Map<Integer, Integer> mapCandidate2Events(int numCandidates, TIntIntMap mention2Candidate,
                                                      Map<Integer, Integer> mention2event) {
        Map<Integer, Integer> candidate2Events = new HashMap<>();

        final MutableInt maxEventId = new MutableInt(0);
        mention2Candidate.forEachEntry((mentionId, candidateId) -> {
            int eventId = mention2event.get(mentionId);
            candidate2Events.put(candidateId, eventId);
            if (eventId > maxEventId.getValue()) {
                maxEventId.setValue(eventId);
            }
//            logger.debug(candidateId + " is linked to mention " + mentionId + ", which is linked to event " +
// eventId);
            return true;
        });


        for (int i = 0; i < numCandidates; i++) {
            if (!candidate2Events.containsKey(i)) {
                maxEventId.increment();
                candidate2Events.put(i, maxEventId.getValue());
            }
        }

        return candidate2Events;
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

    private GraphWeightVector usePretrainedCrfWeights() throws ResourceInitializationException {
        GraphWeightVector weightVector;
        try {
            weightVector = SerializationUtils.deserialize(new FileInputStream(new File
                    (pretrainedMentionModelDirectory, MentionTypeCrfTrainer.MODEL_NAME)));
        } catch (FileNotFoundException e) {
            throw new ResourceInitializationException(e);
        }
        return weightVector;
    }

    private GraphWeightVector preareCorefWeights() {
        logger.info("Initializing Coreference weights.");

        ClassAlphabet classAlphabet = new ClassAlphabet();
        for (MentionGraphEdge.EdgeType edgeType : MentionGraphEdge.EdgeType.values()) {
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
        for (MentionGraphEdge.EdgeType edgeType : MentionGraphEdge.EdgeType.values()) {
            classAlphabet.addClass(edgeType.name());
        }

        return new PairFeatureExtractor(updater.getWeightVector(COREF_MODEL_NAME).getFeatureAlphabet(),
                classAlphabet, config, featureConfig);
    }

}
