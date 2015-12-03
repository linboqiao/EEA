package edu.cmu.cs.lti.emd.annotators.crf;

import edu.cmu.cs.lti.emd.utils.MentionTypeUtils;
import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.sentence.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sentence.extractor.UimaSequenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.training.AveragePerceptronTrainer;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MultiKeyDiskCacher;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.io.FileUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 3:56 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionTypeCrfTrainer extends AbstractLoggingAnnotator {
    public static final String PARAM_CACHE_DIRECTORY = "cacheDirectory";

    public static final String PARAM_CONFIGURATION_FILE = "configurationFile";

    @ConfigurationParameter(name = PARAM_CACHE_DIRECTORY)
    private File cacheDir;

    @ConfigurationParameter(name = PARAM_CONFIGURATION_FILE)
    private Configuration config;

    private static AveragePerceptronTrainer trainer;

    private UimaSequenceFeatureExtractor sentenceExtractor;

    private ClassAlphabet classAlphabet;

    public static final String MODEL_NAME = "crfModel";

    private TrainingStats trainingStats;

    private ViterbiDecoder decoder;

    private static MultiKeyDiskCacher<ArrayList<TIntObjectMap<FeatureVector[]>>> featureCacher;

    private static MultiKeyDiskCacher<ArrayList<Pair<GraphFeatureVector, SequenceSolution>>> goldCacher;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Preparing the token level type trainer ...");

        int alphabetBits = config.getInt("edu.cmu.cs.lti.mention.feature.alphabet_bits", 24);
        double stepSize = config.getDouble("edu.cmu.cs.lti.perceptron.stepsize", 0.01);
        int printLossOverPreviousN = config.getInt("edu.cmu.cs.lti.avergelossN", 50);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.mention.readableModel", false);
        boolean discardAfter = config.getBoolean("edu.cmu.cs.lti.coref.mention.cache.discard_after", true);
        long weightLimit = config.getLong("edu.cmu.cs.lti.mention.cache.document.num", 1000);

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

        classAlphabet = new ClassAlphabet(classes, true, true);

        HashAlphabet alphabet = new HashAlphabet(alphabetBits, readableModel);
        trainingStats = new TrainingStats(printLossOverPreviousN);

        try {
            featureCacher = new MultiKeyDiskCacher<>(cacheDir.getPath(), (strings, featureVectors) -> 1,
                    weightLimit, discardAfter, "feature_cache");
        } catch (IOException e) {
            e.printStackTrace();
        }

        decoder = new ViterbiDecoder(alphabet, classAlphabet);
        String featureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.spec");
        trainer = new AveragePerceptronTrainer(decoder, classAlphabet, alphabet, featureSpec, stepSize);

        try {
            sentenceExtractor = new SentenceFeatureExtractor(alphabet, config,
                    new FeatureSpecParser(config.get("edu.cmu.cs.lti.feature.sentence.package.name"))
                            .parseFeatureFunctionSpecs(featureSpec));
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }

        logger.info("Initializing gold cacher with " + cacheDir.getAbsolutePath());

        try {
            goldCacher = new MultiKeyDiskCacher<>(cacheDir.getPath(), (strings, fv) -> 1,
                    weightLimit, discardAfter, "gold_cache");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        sentenceExtractor.initWorkspace(aJCas);

        String documentKey = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();

        List<TIntObjectMap<FeatureVector[]>> documentCacheFeatures = featureCacher.get(documentKey);
        ArrayList<TIntObjectMap<FeatureVector[]>> featuresToCache = new ArrayList<>();

        List<Pair<GraphFeatureVector, SequenceSolution>> cachedGold = goldCacher.get(documentKey);
        ArrayList<Pair<GraphFeatureVector, SequenceSolution>> goldToCache = new ArrayList<>();

        int sentenceId = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentenceExtractor.resetWorkspace(aJCas, sentence);

            Map<StanfordCorenlpToken, String> tokenTypes = new HashMap<>();
            List<EventMention> mentions = JCasUtil.selectCovered(aJCas, EventMention.class, sentence);
            Map<Span, String> mergedMentions = MentionTypeUtils.mergeMentionTypes(mentions);
            for (Map.Entry<Span, String> spanToType : mergedMentions.entrySet()) {
                Span span = spanToType.getKey();
                String type = spanToType.getValue();
                for (StanfordCorenlpToken token : JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, span
                        .getBegin(), span.getEnd())) {
                    tokenTypes.put(token, type);
                }
            }

            if (mentions.size() == 0) {
                // TODO what if we also train empty type sentences?
                continue;
            }

            SequenceSolution goldSolution;
            GraphFeatureVector goldFv;

            if (cachedGold != null) {
                Pair<GraphFeatureVector, SequenceSolution> goldSolutionAndFeatures = cachedGold.get(sentenceId);
                goldFv = goldSolutionAndFeatures.getLeft();
                goldSolution = goldSolutionAndFeatures.getRight();
            } else {
                goldSolution = getGoldSequence(sentence, tokenTypes);
                goldFv = decoder.getSolutionFeatures(sentenceExtractor, goldSolution);
                goldToCache.add(Pair.of(goldFv, goldSolution));
            }

            TIntObjectMap<FeatureVector[]> sentenceFeatures;
            if (documentCacheFeatures != null) {
                sentenceFeatures = documentCacheFeatures.get(sentenceId);
            } else {
                sentenceFeatures = new TIntObjectHashMap<>();
            }

            double loss = trainer.trainNext(goldSolution, goldFv, sentenceExtractor, 0, sentenceFeatures);
            trainingStats.addLoss(logger, loss);
            sentenceId++;

            if (documentCacheFeatures == null) {
                featuresToCache.add(sentenceFeatures);
            }
        }

        if (documentCacheFeatures == null) {
            featureCacher.addWithMultiKey(featuresToCache, documentKey);
        }

        if (cachedGold == null) {
            goldCacher.addWithMultiKey(goldToCache, documentKey);
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
        } else {
            throw new IOException(String.format("Cannot create directory : [%s]", modelOutputDirectory.toString()));
        }
    }

    /**
     * At loop finish, do some clean up work.
     *
     * @throws IOException
     */
    public static void loopStopActions() throws IOException {
        featureCacher.close();
        goldCacher.close();
    }

}
