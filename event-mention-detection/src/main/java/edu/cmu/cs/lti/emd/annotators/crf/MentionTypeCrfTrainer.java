package edu.cmu.cs.lti.emd.annotators.crf;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.emd.annotators.EventMentionTypeClassPrinter;
import edu.cmu.cs.lti.emd.utils.GoldCacher;
import edu.cmu.cs.lti.learning.cache.CrfFeatureCacher;
import edu.cmu.cs.lti.learning.cache.CrfState;
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
import org.apache.commons.io.FileUtils;
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

    public static final String FEATURE_SPEC_FILE = "featureSpec";

    private static String featureSpec;

    private TrainingStats trainingStats;

    private CrfFeatureCacher cacher;

    private ViterbiDecoder decoder;

    private GoldCacher goldCacher;


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Starting iteration ...");

        int alphabetBits = config.getInt("edu.cmu.cs.lti.mention.feature.alphabet_bits", 24);
        double stepSize = config.getDouble("edu.cmu.cs.lti.perceptron.stepsize", 0.01);
        int printLossOverPreviousN = config.getInt("edu.cmu.cs.lti.avergelossN", 50);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.mention.readableModel", false);
        boolean invalidate = config.getBoolean("edu.cmu.cs.lti.mention.cache.invalidate", true);

        File classFile = config.getFile("edu.cmu.cs.lti.mention.classes.path");
        String[] classes = new String[0];
        try {
            classes = FileUtils.readLines(classFile).stream().map(l -> l.split("\t"))
                    .filter(p -> p.length >= 1).map(p -> p[0]).toArray(String[]::new);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String c : classes) {
            logger.info("Register class " + c);
        }

        classAlphabet = new ClassAlphabet(classes, true, true);
        HashAlphabet alphabet = HashAlphabet.getInstance(alphabetBits, readableModel);
        trainingStats = new TrainingStats(printLossOverPreviousN);

        try {
            cacher = new CrfFeatureCacher(cacheDir, invalidate);
        } catch (IOException e) {
            e.printStackTrace();
        }
        decoder = new ViterbiDecoder(alphabet, classAlphabet, cacher);
        trainer = new AveragePerceptronTrainer(decoder, classAlphabet, stepSize, alphabet);
        featureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.spec");

        try {
            sentenceExtractor = new SentenceFeatureExtractor(alphabet, config,
                    new FeatureSpecParser(config.get("edu.cmu.cs.lti.feature.sentence.package.name"))
                            .parseFeatureFunctionSpecs(featureSpec));
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException
                e) {
            e.printStackTrace();
        }


        logger.info("Initializing gold cacher with " + cacheDir.getAbsolutePath());

        goldCacher = new GoldCacher(cacheDir);
        try {
            goldCacher.loadGoldSolutions();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        UimaConvenience.printProcessLog(aJCas, logger);
        sentenceExtractor.initWorkspace(aJCas);

        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, aJCas);

        String documentKey = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();
        CrfState key = new CrfState();
        key.setDocumentKey(documentKey);

        int sentenceId = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
//            logger.info(String.format("Extracting from sentence %d of document %s", sentenceId, documentKey));
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

//            logger.debug(String.format("%d mentions, %d merged mentions, %d tokens are labelled", mentions.size(),
//                    mergedMentions.size(), tokenTypes.size()));

            SequenceSolution goldSolution;
            GraphFeatureVector goldFv;

            goldSolution = (SequenceSolution) goldCacher.getGoldSolution(documentKey, sentenceId);
            if (goldSolution == null) {
                goldSolution = getGoldSequence(sentence, tokenTypes);
                goldCacher.addGoldSolutions(documentKey, sentenceId, goldSolution);
            }

            goldFv = goldCacher.getGoldFeature(documentKey, sentenceId);
            if (goldFv == null) {
                goldFv = decoder.getSolutionFeatures(sentenceExtractor, goldSolution);
                goldCacher.addGoldFeatures(documentKey, sentenceId, goldFv);
            }

//            logger.debug("Training this sentence.");
//            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
//            for (int i = 0; i < tokens.size(); i++) {
//                System.out.print(i + ": " + tokens.get(i).getCoveredText());
//                if (i == tokens.size() - 1) {
//                    System.out.println();
//                } else {
//                    System.out.print(" ");
//                }
//            }

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
        if (!goldCacher.isGoldLoaded()) {
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
            org.apache.commons.io.FileUtils.write(new File(modelOutputDirectory, FEATURE_SPEC_FILE), featureSpec);
        } else {
            throw new IOException(String.format("Cannot create directory : [%s]", modelOutputDirectory.toString()));
        }
    }
}
