package edu.cmu.cs.lti.emd.annotators.crf;

import edu.cmu.cs.lti.learning.model.HashedFeatureVector;
import edu.cmu.cs.lti.learning.model.SequenceSolution;
import edu.cmu.cs.lti.learning.model.Solution;
import org.apache.commons.lang3.SerializationUtils;
import org.javatuples.Pair;

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
    private static final String GOLD_SOLUTION_CACHE_NAME = "goldSolution";
    private static final String GOLD_FEATURE_CACHE_NAME = "goldCache";

    private MentionTypeCrfTrainer mentionTypeCrfTrainer;
    private HashMap<Pair<String, Integer>, Solution> goldSolutions;
    private HashMap<Pair<String, Integer>, HashedFeatureVector> goldFeatures;

    private boolean goldLoaded;

    private File goldSolutionFile;
    private File goldFeaturesFile;

    public GoldCacher(MentionTypeCrfTrainer mentionTypeCrfTrainer, File cacheDirectory) {
        this.mentionTypeCrfTrainer = mentionTypeCrfTrainer;
        this.goldLoaded = false;
        this.goldSolutions = new HashMap<>();
        this.goldFeatures = new HashMap<>();

        goldSolutionFile = new File(cacheDirectory, GOLD_SOLUTION_CACHE_NAME);
        goldFeaturesFile = new File(cacheDirectory, GOLD_FEATURE_CACHE_NAME);
    }

    public void loadGoldSolutions() throws FileNotFoundException {
        if (goldSolutionFile.exists() && goldFeaturesFile.exists()) {
            mentionTypeCrfTrainer.logger.info(String.format("Loading solutions from %s and %s .", goldFeaturesFile.getAbsolutePath(),
                    goldSolutionFile.getAbsolutePath()));
            goldSolutions = SerializationUtils.deserialize(new FileInputStream(goldSolutionFile));
            goldFeatures = SerializationUtils.deserialize(new FileInputStream(goldFeaturesFile));
            goldLoaded = true;
            mentionTypeCrfTrainer.logger.info("Gold Caches of solutions loaded.");
        }
    }

    public void saveGoldSolutions() throws FileNotFoundException {
        if (!goldLoaded) {
            SerializationUtils.serialize(goldSolutions, new FileOutputStream(goldSolutionFile));
            SerializationUtils.serialize(goldFeatures, new FileOutputStream(goldFeaturesFile));
            mentionTypeCrfTrainer.logger.info(goldSolutionFile.getAbsolutePath());
            mentionTypeCrfTrainer.logger.info("Writing gold caches.");
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
