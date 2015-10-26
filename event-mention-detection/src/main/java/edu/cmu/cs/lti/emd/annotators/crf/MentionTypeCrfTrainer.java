package edu.cmu.cs.lti.emd.annotators.crf;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.emd.annotators.EventMentionTypeClassPrinter;
import edu.cmu.cs.lti.learning.cache.CrfSequenceKey;
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
import edu.cmu.cs.lti.utils.MultiStringDiskBackedCacher;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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

    private static MultiStringDiskBackedCacher<FeatureVector[]> featureCacher;

    private static MultiStringDiskBackedCacher<Pair<GraphFeatureVector, SequenceSolution>> goldCacher;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Starting iteration ...");

        int alphabetBits = config.getInt("edu.cmu.cs.lti.mention.feature.alphabet_bits", 24);
        double stepSize = config.getDouble("edu.cmu.cs.lti.perceptron.stepsize", 0.01);
        int printLossOverPreviousN = config.getInt("edu.cmu.cs.lti.avergelossN", 50);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.mention.readableModel", false);
        boolean discardAfter = config.getBoolean("edu.cmu.cs.lti.coref.mention.cache.discard_after", true);
        long weightLimit = config.getLong("edu.cmu.cs.lti.mention.cache.sentence.num", 20000);

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
            logger.info("No class file provided, will accumulate classes during training.");
        }

        classAlphabet = new ClassAlphabet(classes, true, true);

        HashAlphabet alphabet = new HashAlphabet(alphabetBits, readableModel);
        trainingStats = new TrainingStats(printLossOverPreviousN);

        try {
            featureCacher = new MultiStringDiskBackedCacher<>(cacheDir.getPath(), (strings, featureVectors) -> 1,
                    20000, discardAfter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        decoder = new ViterbiDecoder(alphabet, classAlphabet, featureCacher);
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
            goldCacher = new MultiStringDiskBackedCacher<>(cacheDir.getPath(), (strings, fv) -> 1,
                    weightLimit, discardAfter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        sentenceExtractor.initWorkspace(aJCas);

        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, aJCas);

        String documentKey = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();
        CrfSequenceKey key = new CrfSequenceKey();
        key.setDocumentKey(documentKey);

        int sentenceId = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentenceExtractor.resetWorkspace(aJCas, sentence);
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

            SequenceSolution goldSolution;
            GraphFeatureVector goldFv;
            Pair<GraphFeatureVector, SequenceSolution> goldSolutionAndFeatures = goldCacher.get(documentKey,
                    String.valueOf(sentenceId));

            if (goldSolutionAndFeatures != null) {
                goldFv = goldSolutionAndFeatures.getLeft();
                goldSolution = goldSolutionAndFeatures.getRight();
            } else {
                goldSolution = getGoldSequence(sentence, tokenTypes);
                goldFv = decoder.getSolutionFeatures(sentenceExtractor, goldSolution);
                goldCacher.addWithMultiKey(Pair.of(goldFv, goldSolution), documentKey, String.valueOf(sentenceId));
            }

            double loss = trainer.trainNext(goldSolution, goldFv, sentenceExtractor, 0, key);
            trainingStats.addLoss(logger, loss);
            sentenceId++;
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
            mentionWithMergedTypes.put(span, EventMentionTypeClassPrinter.joinMultipleTypes(uniqueSortedTypes));
        }

        return mentionWithMergedTypes;
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
