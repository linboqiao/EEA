package edu.cmu.cs.lti.learning.annotators;

import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.extractor.UimaSequenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.update.SeqLoss;
import edu.cmu.cs.lti.uima.annotator.AbstractConfigAnnotator;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MultiKeyDiskCacher;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/9/15
 * Time: 1:39 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AbstractCrfTrainer extends AbstractConfigAnnotator {
    public static final String PARAM_CACHE_DIRECTORY = "cacheDirectory";

    public static final String PARAM_CONFIGURATION_FILE = "configurationFile";

    public static final String PARAM_USE_PA_UPDATE = "usePaUpdate";

    public static final String PARAM_LOSS_TYPE = "lossType";

    public static final String PARAM_IGNORE_UNANNOTATED_SENTENCE = "ignoreUnannotatedSentence";

    public static final String PARAM_CLASS_FILE = "classTypeFile";

    @ConfigurationParameter(name = PARAM_CACHE_DIRECTORY)
    private File cacheDir;

    @ConfigurationParameter(name = PARAM_CLASS_FILE)
    private File classFile;

    @ConfigurationParameter(name = PARAM_CONFIGURATION_FILE)
    protected Configuration config;

    @ConfigurationParameter(name = PARAM_USE_PA_UPDATE)
    protected boolean usePaUpdate;

    @ConfigurationParameter(name = PARAM_LOSS_TYPE)
    private String lossType;

    @ConfigurationParameter(name = PARAM_IGNORE_UNANNOTATED_SENTENCE, defaultValue = "false")
    protected boolean ignoreUnannotatedSentence;

    protected HashAlphabet featureAlphabet;

    protected ClassAlphabet classAlphabet;

    protected TrainingStats trainingStats;

    protected ViterbiDecoder decoder;

    protected UimaSequenceFeatureExtractor featureExtractor;

    protected static GraphWeightVector weightVector;

    private SeqLoss seqLoss;

    protected String featureSpec;

    // This doesn't really change the weight descent speed, but it may affect the granularity of the weights, as long
    // as the weight does not overflow, it will be fine.
    private double defaultStepSize = 1;


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        int alphabetBits = config.getInt("edu.cmu.cs.lti.mention.feature.alphabet_bits", 24);
        int printLossOverPreviousN = config.getInt("edu.cmu.cs.lti.avergelossN", 50);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.mention.readableModel", false);

        String[] classes = new String[0];

        logger.info(classFile.getPath());
        if (classFile.exists()) {
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

        classAlphabet = initClassAlphabet(classes);


        featureAlphabet = new HashAlphabet(alphabetBits, readableModel);
        trainingStats = new TrainingStats(printLossOverPreviousN);

        decoder = new ViterbiDecoder(featureAlphabet, classAlphabet);

        parseFeatureSpec();

        weightVector = new GraphWeightVector(classAlphabet, featureAlphabet, featureSpec);

        seqLoss = SeqLoss.getLoss(lossType);

        logger.info("Initializing gold cacher with " + cacheDir.getAbsolutePath());

        if (ignoreUnannotatedSentence) {
            logger.info("The training process will ignore the unannotated sentences.");
        } else {
            logger.info("The training process will use all sentences.");
        }

    }

    protected abstract void parseFeatureSpec();

    protected abstract ClassAlphabet initClassAlphabet(String[] classes);

    protected <T extends Serializable> MultiKeyDiskCacher<T> createGoldCacher(long maxCachedInstance, boolean
            discardAfter) throws IOException {
        return new MultiKeyDiskCacher<>(cacheDir.getPath(), (strings, fv) -> 1,
                maxCachedInstance, discardAfter, "gold_cache");
    }

    protected <T extends Serializable> MultiKeyDiskCacher<T> createFeatureCacher(long maxCachedInstance, boolean
            discardAfter) throws IOException {
        return new MultiKeyDiskCacher<>(cacheDir.getPath(), (strings, featureVectors) -> 1,
                maxCachedInstance, discardAfter, "feature_cache");
    }

    public double trainNext(SequenceSolution goldSolution, SequenceSolution prediction, GraphFeatureVector goldFv) {
        double loss = seqLoss.compute(goldSolution.asIntArray(), prediction.asIntArray(),
                classAlphabet.getNoneOfTheAboveClassIndex());

        if (loss != 0) {
            GraphFeatureVector bestDecodingFeatures = decoder.getBestDecodingFeatures();
            GraphFeatureVector delta = goldFv.newGraphFeatureVector();
            goldFv.diff(bestDecodingFeatures, delta);

            if (usePaUpdate) {
                double l2 = delta.getFeatureL2();
                double tau = loss / l2;
                updateWeights(goldFv, bestDecodingFeatures, tau);
            } else {
                updateWeights(goldFv, bestDecodingFeatures, defaultStepSize);
            }
        }

        return loss;
    }


    private void updateWeights(GraphFeatureVector goldFv, GraphFeatureVector predictedFv, double stepSize) {
        weightVector.updateWeightsBy(goldFv, stepSize);
        weightVector.updateWeightsBy(predictedFv, -stepSize);
        weightVector.updateAverageWeights();
    }


    public static void saveModels(File modelOutputDirectory, String modelName) throws IOException {
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(modelOutputDirectory);
        weightVector.write(new File(modelOutputDirectory, modelName));
    }

}
