package edu.cmu.cs.lti.emd.annotators.crf;

import edu.cmu.cs.lti.learning.model.GraphFeatureVector;
import edu.cmu.cs.lti.learning.model.SequenceSolution;
import edu.cmu.cs.lti.learning.model.Solution;
import org.apache.commons.lang3.SerializationUtils;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/31/15
 * Time: 11:21 PM
 *
 * @author Zhengzhong Liu
 */
class GoldCacher {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String GOLD_SOLUTION_CACHE_NAME = "goldSolution";
    private static final String GOLD_FEATURE_CACHE_NAME = "goldCache";

    private HashMap<Pair<String, Integer>, Solution> goldSolutions;
    private HashMap<Pair<String, Integer>, GraphFeatureVector> goldFeatures;

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
        } else {
            logger.info("Gold Caches loading failed.");
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

    public void addGoldFeatures(String documentKey, int sequenceKey, GraphFeatureVector featureVector) {
        goldFeatures.put(Pair.with(documentKey, sequenceKey), featureVector);
    }

    public void addGoldSolutions(String documentKey, int sequenceKey, SequenceSolution solution) {
        goldSolutions.put(Pair.with(documentKey, sequenceKey), solution);
    }

    public GraphFeatureVector getGoldFeature(String documentKey, int sequenceKey) {
        return goldFeatures.getOrDefault(Pair.with(documentKey, sequenceKey), null);
    }

    public Solution getGoldSolution(String documentKey, int sequenceKey) {
        return goldSolutions.getOrDefault(Pair.with(documentKey, sequenceKey), null);
    }

    public boolean isGoldLoaded() {
        return goldLoaded;
    }
}