package edu.cmu.cs.lti.emd.annotators.crf;

import edu.cmu.cs.lti.emd.utils.MentionTypeUtils;
import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.sentence.extractor.MultiSentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.training.AveragePerceptronTrainer;
import edu.cmu.cs.lti.learning.training.SequenceDecoder;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MultiKeyDiskCacher;
import gnu.trove.map.hash.TIntObjectHashMap;
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
import java.util.Map;

/**
 * This is the second layer of the CRF model, that model directly on sequence of mentions, which try to capture the
 * dependencies among mentions sequences.
 *
 * @author Zhengzhong Liu
 */
public class MentionSequenceCrfTrainer extends AbstractLoggingAnnotator {
    public static final String PARAM_CACHE_DIRECTORY = "cacheDirectory";

    public static final String PARAM_CONFIGURATION_FILE = "configurationFile";

    @ConfigurationParameter(name = PARAM_CACHE_DIRECTORY)
    private File cacheDir;

    @ConfigurationParameter(name = PARAM_CONFIGURATION_FILE)
    private Configuration config;

    private static AveragePerceptronTrainer trainer;

    private MultiSentenceFeatureExtractor<EventMention> featureExtractor;

    private ClassAlphabet classAlphabet;

    private TrainingStats trainingStats;

    public static final String MODEL_NAME = "mentionSequenceModel";

    private SequenceDecoder decoder;

    private static MultiKeyDiskCacher<TIntObjectHashMap<FeatureVector[]>> featureCacher;

    private static MultiKeyDiskCacher<Pair<GraphFeatureVector, SequenceSolution>> goldCacher;

    // TODO the initialization part is almost copied from MentionpTypeCrfTrainer, which is not nice.
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Preparing the mention level type trainer ...");

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

        classAlphabet = new ClassAlphabet(classes, false /*do not use none class*/, true);

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
            featureExtractor = new MultiSentenceFeatureExtractor<>(alphabet, config,
                    new FeatureSpecParser(config.get("edu.cmu.cs.lti.feature.sentence.package.name"))
                            .parseFeatureFunctionSpecs(featureSpec), EventMention.class);
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
        featureExtractor.initWorkspace(aJCas);

        DocumentAnnotation document = JCasUtil.selectSingle(aJCas, DocumentAnnotation.class);
        featureExtractor.resetWorkspace(aJCas, document.getBegin(), document.getEnd());

        Map<Span, String> mentionWithTypes = MentionTypeUtils.mergeMentionTypes(JCasUtil.select(aJCas,
                EventMention.class));

        String documentKey = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();

        TIntObjectHashMap<FeatureVector[]> sequenceFeatures = featureCacher.get(documentKey);
        Pair<GraphFeatureVector, SequenceSolution> cachedGold = goldCacher.get(documentKey);

        SequenceSolution goldSolution;
        GraphFeatureVector goldFv;

        boolean newGold = false;
        if (cachedGold != null) {
            goldFv = cachedGold.getLeft();
            goldSolution = cachedGold.getRight();
        } else {
            goldSolution = getGoldSequence(mentionWithTypes);
            goldFv = decoder.getSolutionFeatures(featureExtractor, goldSolution);
            cachedGold = Pair.of(goldFv, goldSolution);
            newGold = true;
        }

        boolean newSequenceFeatures = false;
        if (sequenceFeatures == null) {
            newSequenceFeatures = true;
            sequenceFeatures = new TIntObjectHashMap<>();
        }

        double loss = trainer.trainNext(goldSolution, goldFv, featureExtractor, 0/* No lagrangian value now*/,
                sequenceFeatures);
        trainingStats.addLoss(logger, loss);

        if (newSequenceFeatures) {
            featureCacher.addWithMultiKey(sequenceFeatures, documentKey);
        }

        if (newGold) {
            goldCacher.addWithMultiKey(cachedGold, documentKey);
        }
    }


    private SequenceSolution getGoldSequence(Map<Span, String> mentionTypes) {
        // Fill the gold sequence.
        int[] goldSequence = new int[mentionTypes.size()];

        int sequenceIdx = 0;
        for (Map.Entry<Span, String> spanAndType : mentionTypes.entrySet()) {
            int classIndex = classAlphabet.getClassIndex(spanAndType.getValue());
            goldSequence[sequenceIdx] = classIndex;
            sequenceIdx++;
        }

        return new SequenceSolution(classAlphabet, goldSequence);
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
