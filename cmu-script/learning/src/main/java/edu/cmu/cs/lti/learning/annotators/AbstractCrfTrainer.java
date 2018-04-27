package edu.cmu.cs.lti.learning.annotators;

import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.extractor.UimaSequenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.HashAlphabet;
import edu.cmu.cs.lti.learning.model.TrainingStats;
import edu.cmu.cs.lti.learning.training.AveragePerceptronTrainer;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
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
public abstract class AbstractCrfTrainer extends AbstractLoggingAnnotator {
    public static final String PARAM_CACHE_DIRECTORY = "cacheDirectory";

    public static final String PARAM_CONFIGURATION_FILE = "configurationFile";

    @ConfigurationParameter(name = PARAM_CACHE_DIRECTORY)
    private File cacheDir;

    @ConfigurationParameter(name = PARAM_CONFIGURATION_FILE)
    protected Configuration config;

    protected HashAlphabet featureAlphabet;

    protected ClassAlphabet classAlphabet;

    protected TrainingStats trainingStats;

    protected ViterbiDecoder decoder;

    protected UimaSequenceFeatureExtractor featureExtractor;

    protected double stepSize;

    protected static AveragePerceptronTrainer trainer;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        int alphabetBits = config.getInt("edu.cmu.cs.lti.mention.feature.alphabet_bits", 24);
        int printLossOverPreviousN = config.getInt("edu.cmu.cs.lti.avergelossN", 50);
        boolean readableModel = config.getBoolean("edu.cmu.cs.lti.mention.readableModel", false);
        stepSize = config.getDouble("edu.cmu.cs.lti.perceptron.stepsize", 0.01);

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

        classAlphabet = initClassAlphabet(classes);

        featureAlphabet = new HashAlphabet(alphabetBits, readableModel);
        trainingStats = new TrainingStats(printLossOverPreviousN);

        decoder = new ViterbiDecoder(featureAlphabet, classAlphabet);

        logger.info("Initializing gold cacher with " + cacheDir.getAbsolutePath());
    }

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

    public static void saveModels(File modelOutputDirectory, String modelName) throws IOException {
        boolean directoryExist = true;
        if (!modelOutputDirectory.exists()) {
            if (!modelOutputDirectory.mkdirs()) {
                directoryExist = false;
            }
        }

        if (directoryExist) {
            trainer.write(new File(modelOutputDirectory, modelName));
        } else {
            throw new IOException(String.format("Cannot create directory : [%s]", modelOutputDirectory.toString()));
        }
    }

}
