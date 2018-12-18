package edu.cmu.cs.lti.utils;

import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/14/17
 * Time: 11:59 PM
 *
 * @author Zhengzhong Liu
 */
public class ExperimentUtils {
    final private String preprocessBase = "preprocessed";
    final private String prepredTrainingBase = "prepared_training";
    final private String rawBase = "raw";
    final private String trialBase = "trial";
    final private String processOut;
    final private String resultBase;
    final private String middleResults;

    final static String fullRunSuffix = "all";

    private Logger logger;

    public ExperimentUtils(Logger logger, String processOutputDir) {
        this.processOut = processOutputDir;
        this.resultBase = processOutputDir + "/results";
        this.middleResults = processOutputDir + "/intermediate";
        this.logger = logger;
    }

    public String getResultDir(String workingDir, String suffix) {
        return FileUtils.joinPaths(workingDir, resultBase, suffix);
    }

    public String getRawBase() {
        return rawBase;
    }

    public String getPreprocessBase() {
        return preprocessBase;
    }

    public File getPreprocessPath(String workingDirPath) {
        return new File(workingDirPath, preprocessBase);
    }

    public CollectionReaderDescription getPreprocessReader(TypeSystemDescription typeSystemDescription,
                                                           String workingDir)
            throws ResourceInitializationException {
        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, preprocessBase);
    }

    public CollectionReaderDescription getPreprocessReader(TypeSystemDescription typeSystemDescription,
                                                           String workingDir, File blackList)
            throws ResourceInitializationException {
        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, preprocessBase,
                blackList);
    }

    public CollectionReaderDescription getPreprocessReader(TypeSystemDescription typeSystemDescription,
                                                           String workingDir, File blackList, File whiteList)
            throws ResourceInitializationException {
        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, preprocessBase,
                blackList, whiteList);
    }

    public CollectionReaderDescription randomPreparedTraining(TypeSystemDescription typeSystemDescription,
                                                              String workingDir, int seed)
            throws ResourceInitializationException {
        return CustomCollectionReaderFactory.createRandomizedXmiReader(
                typeSystemDescription, workingDir, prepredTrainingBase, seed);
    }

    public Pair<CollectionReaderDescription, CollectionReaderDescription> crossValidationReader(
            TypeSystemDescription typeSystemDescription, String workingDir, int seed, int numSplit, int slice) throws
            ResourceInitializationException {
        CollectionReaderDescription trainingSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                typeSystemDescription, workingDir, prepredTrainingBase, false, seed, numSplit, slice);
        CollectionReaderDescription testSliceReader = CustomCollectionReaderFactory.createCrossValidationReader(
                typeSystemDescription, workingDir, prepredTrainingBase, true, seed, numSplit, slice);

        return Pair.of(trainingSliceReader, testSliceReader);
    }

    public boolean preprocessExists(String workingDir) {
        File preprocessDir = new File(workingDir, preprocessBase);

        if (preprocessDir.exists()) {
            logger.info("Preprocess data exists at : " + preprocessDir);
            return true;
        }
        return false;
    }

    public String getMiddleOutputPath(String suffix, String outputName) {
        return FileUtils.joinPaths(middleResults, suffix, outputName);
    }

    public String getTrialBase() {
        return trialBase;
    }

    public static Configuration getModelConfig(Configuration taskConfig, String modelConfigDir,
                                               String modelConfigKey) throws IOException {
        String modelConfigName = taskConfig.get(modelConfigKey);
        if (modelConfigName == null || modelConfigName.isEmpty()) {
            return null;
        }
        Configuration modelConfig = new Configuration(new File(modelConfigDir, modelConfigName + ".properties"));
        // Make resource config at one place.
        modelConfig.set("edu.cmu.cs.lti.resource.dir", taskConfig.get("edu.cmu.cs.lti.resource.dir"));
        modelConfig.set("edu.cmu.cs.lti.model.event.dir", taskConfig.get("edu.cmu.cs.lti.model.event.dir"));

        return modelConfig;
    }
}
