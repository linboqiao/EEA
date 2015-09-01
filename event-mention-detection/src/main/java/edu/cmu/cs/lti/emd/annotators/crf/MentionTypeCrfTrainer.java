package edu.cmu.cs.lti.emd.annotators.crf;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.emd.learn.UimaSentenceFeatureExtractor;
import edu.cmu.cs.lti.emd.learn.feature.sentence.SentenceFeatureWithFocus;
import edu.cmu.cs.lti.emd.learn.feature.sentence.WordFeatures;
import edu.cmu.cs.lti.learning.cache.CrfFeatureCacher;
import edu.cmu.cs.lti.learning.cache.CrfState;
import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.training.AveragePerceptronTrainer;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 3:56 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionTypeCrfTrainer extends AbstractLoggingAnnotator {
    public static final String PARAM_GOLD_CACHE_DIRECTORY = "GoldCacheDirectory";

    @ConfigurationParameter(name = PARAM_GOLD_CACHE_DIRECTORY)
    private File goldCacheDirectory;

    private static AveragePerceptronTrainer trainer;

    private static UimaSentenceFeatureExtractor sentenceExtractor;

    private static ClassAlphabet classAlphabet;

    private static Alphabet alphabet;

    public static final String MODEL_NAME = "crfWeights";

    public static final String CLASS_ALPHABET_NAME = "classAlphabet";

    public static final String ALPHABET_NAME = "alphabet";

    private static TrainingStats trainingStats;

    private static CrfFeatureCacher cacher;

    private static ViterbiDecoder decoder;

    private GoldCacher goldCacher;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Starting iteration ...");
        logger.info("Initializing gold cacher with " + goldCacheDirectory.getAbsolutePath());

        if (!goldCacheDirectory.exists()) {
            goldCacheDirectory.mkdirs();
            logger.info("Create a gold directory at : " + goldCacheDirectory.getAbsolutePath());
        }

        goldCacher = new GoldCacher(goldCacheDirectory);
        try {
            goldCacher.loadGoldSolutions();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        UimaConvenience.printProcessLog(aJCas, logger);
        sentenceExtractor.init(aJCas);

        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, aJCas);

        String documentKey = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();
        CrfState key = new CrfState();
        key.setDocumentKey(documentKey);

        int sentenceId = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
//            logger.info(String.format("Extracting from sentence %d of document %s", sentenceId, documentKey));
            sentenceExtractor.resetWorkspace(sentence);
            key.setSequenceId(sentenceId);

            Map<StanfordCorenlpToken, String> tokenTypes = new HashMap<>();
            List<EventMention> mentions = JCasUtil.selectCovered(goldView, EventMention.class, sentence.getBegin(),
                    sentence.getEnd());
            Map<Span, String> mergedMentions = mergeMentionByTypes(mentions);
            for (Map.Entry<Span, String> spanToType : mergedMentions.entrySet()) {
                Span span = spanToType.getKey();
                String type = spanToType.getValue();
                for (StanfordCorenlpToken token : JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, span
                        .getBegin(), span.getEnd())) {
                    tokenTypes.put(token, type);
                }
            }

            if (mentions.size() == 0) {
                continue;
            }

//            logger.debug(String.format("%d mentions, %d merged mentions, %d tokens are labelled", mentions.size(),
//                    mergedMentions.size(), tokenTypes.size()));

            SequenceSolution goldSolution;
            HashedFeatureVector goldFv;

            if (goldCacher.isGoldLoaded()) {
                goldFv = goldCacher.getGoldFeature(documentKey, sentenceId);
                goldSolution = (SequenceSolution) goldCacher.getGoldSolution(documentKey, sentenceId);
            } else {
                goldSolution = getGoldSequence(sentence, tokenTypes);
                goldFv = decoder.getSolutionFeatures(sentenceExtractor, goldSolution);
                goldCacher.addGoldSolutions(documentKey, sentenceId, goldSolution, goldFv);
            }

//            logger.debug("Training this sentence.");
//            logger.debug(sentence.getCoveredText().replaceAll("\n", " "));
            double loss = trainer.trainNext(goldSolution, goldFv, sentenceExtractor, 0, key);
//            logger.info("Sentence loss is " + loss);
            trainingStats.addLoss(logger, loss);
            sentenceId++;
        }

        try {
            cacher.flush(documentKey);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        if (!goldCacher.goldLoaded) {
            try {
                goldCacher.saveGoldSolutions();
            } catch (FileNotFoundException e) {
                logger.error("Gold Cacher cannot find places to cache.");
                throw new AnalysisEngineProcessException(e);
            }
        }
    }

    private Map<Span, String> mergeMentionByTypes(List<EventMention> mentions) {
        ArrayListMultimap<Span, String> mergedMentionTypes = ArrayListMultimap.create();
        for (EventMention mention : mentions) {
            mergedMentionTypes.put(Span.of(mention.getBegin(), mention.getEnd()), mention.getEventType());
        }

        Map<Span, String> mentionWithMergedTypes = new HashMap<>();

        for (Span span : mergedMentionTypes.keySet()) {
            TreeSet<String> uniqueSortedTypes = new TreeSet<>(mergedMentionTypes.get(span));
            mentionWithMergedTypes.put(span, Joiner.on(";").join(uniqueSortedTypes));
        }

        return mentionWithMergedTypes;
    }

    class GoldCacher {
        private static final String GOLD_SOLUTION_CACHE_NAME = "goldSolution";
        private static final String GOLD_FEATURE_CACHE_NAME = "goldCache";

        private HashMap<Pair<String, Integer>, Solution> goldSolutions;
        private HashMap<Pair<String, Integer>, HashedFeatureVector> goldFeatures;

        private boolean goldLoaded;

        private File goldSolutionFile;
        private File goldFeaturesFile;

        public GoldCacher(File cacheDirectory) {
            this.goldLoaded = false;
            this.goldSolutions = new HashMap<>();
            this.goldFeatures = new HashMap<>();

            goldSolutionFile = new File(cacheDirectory, GOLD_SOLUTION_CACHE_NAME);
            goldFeaturesFile = new File(cacheDirectory, GOLD_FEATURE_CACHE_NAME);
        }

        public void loadGoldSolutions() throws FileNotFoundException {
            if (goldSolutionFile.exists() && goldFeaturesFile.exists()) {
                logger.info(String.format("Loading solutions from %s and %s .", goldFeaturesFile.getAbsolutePath(),
                        goldSolutionFile.getAbsolutePath()));
                goldSolutions = SerializationUtils.deserialize(new FileInputStream(goldSolutionFile));
                goldFeatures = SerializationUtils.deserialize(new FileInputStream(goldFeaturesFile));
                goldLoaded = true;
                logger.info("Gold Caches of solutions loaded.");
            }
        }

        public void saveGoldSolutions() throws FileNotFoundException {
            if (!goldLoaded) {
                SerializationUtils.serialize(goldSolutions, new FileOutputStream(goldSolutionFile));
                SerializationUtils.serialize(goldFeatures, new FileOutputStream(goldFeaturesFile));
                logger.info(goldSolutionFile.getAbsolutePath());
                logger.info("Writing gold caches.");
            }
        }

        public void addGoldSolutions(String documentKey, int sequenceKey, SequenceSolution solution,
                                     HashedFeatureVector featureVector) {
            goldSolutions.put(Pair.with(documentKey, sequenceKey), solution);
            goldFeatures.put(Pair.with(documentKey, sequenceKey), featureVector);
        }

        public HashedFeatureVector getGoldFeature(String documentKey, int sequenceKey) {
            return goldFeatures.getOrDefault(Pair.with(documentKey, sequenceKey), null);
        }

        public Solution getGoldSolution(String documentKey, int sequenceKey) {
            return goldSolutions.getOrDefault(Pair.with(documentKey, sequenceKey), null);
        }

        public boolean isGoldLoaded() {
            return goldLoaded;
        }
    }

    private SequenceSolution getGoldSequence(StanfordCorenlpSentence sentence, Map<StanfordCorenlpToken, String>
            tokenTypes) {
        List<StanfordCorenlpToken> sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

        int sequenceLength = sentenceTokens.size();

        // Fill the gold sequence.
        int[] goldSequence = new int[sequenceLength];

        int seqIndex = 0;
        for (StanfordCorenlpToken token : sentenceTokens) {
            if (tokenTypes.containsKey(token)) {
                goldSequence[seqIndex] = classAlphabet.getClassIndex(tokenTypes.get(token));
            } else {
                goldSequence[seqIndex] = classAlphabet.getNoneOfTheAboveClassIndex();
            }
            seqIndex++;
        }

        return new SequenceSolution(decoder.getClassAlphabet(), goldSequence);
    }

    public static void saveModels(File modelOutputDirectory) throws IOException {
        boolean directoryExist = true;
        if (!modelOutputDirectory.exists()) {
            if (!modelOutputDirectory.mkdirs()) {
                directoryExist = false;
            }
        }

        if (directoryExist) {
            trainer.write(new File(modelOutputDirectory, MODEL_NAME));
            classAlphabet.write(new File(modelOutputDirectory, CLASS_ALPHABET_NAME));
            alphabet.write(new File(modelOutputDirectory, ALPHABET_NAME));
        } else {
            throw new IOException(String.format("Cannot create directory : [%s]", modelOutputDirectory.toString()));
        }
    }

    public static void setup(String[] classes, int featureDimension, double stepSize, int printLossOverPreviousN,
                             boolean readableModel, File cacheDirectory) {
        classAlphabet = new ClassAlphabet(classes, true);
        alphabet = new Alphabet(featureDimension, readableModel);
        trainingStats = new TrainingStats(printLossOverPreviousN);
        cacher = new CrfFeatureCacher(cacheDirectory);
        decoder = new ViterbiDecoder(alphabet, classAlphabet, cacher);
        trainer = new AveragePerceptronTrainer(decoder, stepSize, featureDimension);

        List<SentenceFeatureWithFocus> featureFunctions = new ArrayList<>();
        featureFunctions.add(new WordFeatures());

        sentenceExtractor = new UimaSentenceFeatureExtractor(alphabet) {
            private final Logger logger = LoggerFactory.getLogger(getClass());

            List<StanfordCorenlpToken> sentenceTokens;

            @Override
            public void init(JCas context) {
                super.init(context);
            }

            @Override
            public void resetWorkspace(StanfordCorenlpSentence sentence) {
                super.resetWorkspace(sentence);
                sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
            }

            @Override
            public void extract(int focus, TObjectDoubleMap<String> featuresNoState,
                                TObjectDoubleMap<String> featuresNeedForState) {
//                logger.info("Extracting features at focus : " + focus);
                featureFunctions.forEach(ff -> ff.extract(sentenceTokens, focus, featuresNoState,
                        featuresNeedForState));
//                logger.info("Done extracting : " + focus);
            }
        };
    }
}
