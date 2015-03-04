package edu.cmu.cs.lti.script.runners;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.script.annotators.learn.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.script.annotators.learn.train.PerceptronTraining;
import edu.cmu.cs.lti.script.annotators.stats.EventMentionHeadTfDfCounter;
import edu.cmu.cs.lti.script.runners.learn.test.MultiArgumentClozeTestRunner;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.BasicConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/3/15
 * Time: 9:02 PM
 * <p/>
 * Given a processed dataset, a trainer that train all the models.
 */
public class FullSystemRunner {
    private static String className = FullSystemRunner.class.getSimpleName();
    private static Logger logger = Logger.getLogger(className);

    private boolean doTesting = false;

    private TypeSystemDescription typeSystemDescription;

    private void validate(Configuration config) {
        String testPath = config.get("edu.cmu.cs.lti.cds.event_tuple.heldout.path"); //"data/02_event_tuples";
        String clozePath = config.get("edu.cmu.cs.lti.cds.cloze.path"); // "cloze"

        if (!new File(testPath).exists() || !new File(clozePath).exists()) {
            logger.info("Testing path not found, will not do testing.");
            doTesting = false;
        }
    }

    private void initialize(Configuration config) throws IOException {
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
        DataPool.readBlackList(new File(blackListFile));

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

    }

    private void process(Configuration config) throws Exception {
        validate(config);
        initialize(config);
        runTraining(config);
        if (doTesting) {
            runTesting(config);
        }
    }

    private void basicPredicateCount(Configuration config) throws Exception {
        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath");
        String predicateTfName = config.get("edu.cmu.cs.lti.cds.db.predicate.tf");
        String predicateDfName = config.get("edu.cmu.cs.lti.cds.db.predicate.df");

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        // The Tf Df counter
        AnalysisEngineDescription headTfDfCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionHeadTfDfCounter.class, typeSystemDescription,
                EventMentionHeadTfDfCounter.PARAM_DB_DIR_PATH, dbPath,
                EventMentionHeadTfDfCounter.PARAM_KEEP_QUIET, false,
                EventMentionHeadTfDfCounter.PARAM_PREDICATE_DF_PATH, predicateDfName,
                EventMentionHeadTfDfCounter.PARAM_PREDICATE_TF_PATH, predicateTfName);

        SimplePipeline.runPipeline(reader, headTfDfCounter);
    }

    private void unigramTrain(String eventTuplePath, String dbPath, String ignoreLowFreq) {

    }

    private void bigramTrain(String eventTuplePath, String dbPath, String dbName, boolean ignoreLowFreq, int skipgramN) throws UIMAException, IOException {
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, eventTuplePath, true);

        AnalysisEngineDescription kmScriptCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                KarlMooneyScriptCounter.class, typeSystemDescription,
                KarlMooneyScriptCounter.PARAM_DB_DIR_PATH, dbPath,
                KarlMooneyScriptCounter.PARAM_SKIP_BIGRAM_N, skipgramN,
                KarlMooneyScriptCounter.PARAM_DB_NAME, dbName,
                KarlMooneyScriptCounter.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                AbstractLoggingAnnotator.PARAM_KEEP_QUIET, false);
        SimplePipeline.runPipeline(reader, kmScriptCounter);
    }


    private void perceptronTrain(
            String eventTuplePath,
            String featurePackage,
            String[] featureNames,
            int maxIter,
            String modelStoragePath,
            String modelExt,
            int maxSkipN,
            int miniBatchNum,
            int rankListSize,
            int topRankToOptimize) throws Exception {

        String modelSuffix = Joiner.on("_").join(featureNames);
        //make complete class name
        for (int i = 0; i < featureNames.length; i++) {
            featureNames[i] = featurePackage + "." + featureNames[i];
        }

        PerceptronTraining.initializeParameters();
        BasicConvenience.printMemInfo(logger, "Beginning memory for perceptron training");

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, eventTuplePath, false);


        AnalysisEngineDescription trainer = CustomAnalysisEngineFactory.createAnalysisEngine(PerceptronTraining.class, typeSystemDescription,
                PerceptronTraining.PARAM_RANK_LIST_SIZE, rankListSize,
                PerceptronTraining.PARAM_MINI_BATCH_SIZE, miniBatchNum,
                PerceptronTraining.PARAM_FEATURE_NAMES, featureNames,
                PerceptronTraining.PARAM_MAX_SKIP_GRAM_N, maxSkipN,
                PerceptronTraining.PARAM_TOP_RANK_TO_OPTIMIZE, topRankToOptimize
        );


        for (int i = 0; i < maxIter; i++) {
            String modelOutputPath = modelStoragePath + "_" + modelSuffix + "_" + i + modelExt;
            SimplePipeline.runPipeline(reader, trainer);
            File modelDirParent = new File(modelStoragePath).getParentFile();

            if (!modelDirParent.exists()) {
                modelDirParent.mkdirs();
            }

            logger.info("Storing this model to " + modelOutputPath);
            SerializationHelper.write(modelOutputPath, PerceptronTraining.trainingFeatureTable);
        }
    }

    private void runTraining(Configuration config) throws Exception {
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String eventTuplePath = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String dbName = config.get("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        boolean ignoreLowFreq = config.getBoolean("edu.cmu.cs.lti.cds.filter.lowfreq");
        int skipGramN = config.getInt("edu.cmu.cs.lti.cds.mooney.skipgram.n");

        int maxIter = config.getInt("edu.cmu.cs.lti.cds.sgd.iter");
        int miniBatchNum = config.getInt("edu.cmu.cs.lti.cds.minibatch");
        String modelExt = config.get("edu.cmu.cs.lti.cds.model.ext");
        String[] featureNames = config.getList("edu.cmu.cs.lti.cds.features");
        String featurePackage = config.get("edu.cmu.cs.lti.cds.features.packagename");
        String semLinkPath = config.get("edu.cmu.cs.lti.cds.db.semlink.path");
        int maxSkipN = config.getInt("edu.cmu.cs.lti.cds.max.n");

        int topRankToOptimize = config.getInt("edu.cmu.cs.lti.cds.perceptron.top.rank.optimize");
        int rankListSize = config.getInt("edu.cmu.cs.lti.cds.perceptron.ranklist.size");

        String modelStoragePath = config.get("edu.cmu.cs.lti.cds.perceptron.model.path");

        logger.info("Started bigram training...");
        bigramTrain(eventTuplePath, dbPath, dbName, ignoreLowFreq, skipGramN);
        logger.info("End bigram training.");

        DataPool.loadHeadIds(config, false);

        logger.info("Start counting predicates..");
        basicPredicateCount(config);
        logger.info("End counting.");

        logger.info("Loading data for features...");
        DataPool.loadHeadCounts(config);
        DataPool.loadSemLinkData(semLinkPath);
        logger.info("Done loading.");

        logger.info("# predicates " + DataPool.headIdMap.size());

        logger.info("Started perceptron training...");
        perceptronTrain(eventTuplePath, featurePackage, featureNames, maxIter, modelStoragePath, modelExt, maxSkipN, miniBatchNum, rankListSize, topRankToOptimize);
        logger.info("End perceptron training...");
    }

    private void runTesting(Configuration config) throws Exception {
        MultiArgumentClozeTestRunner.test(config);
    }

    public static void main(String[] args) throws Exception {
        logger.info("System started...");
        Configuration config = new Configuration(new File(args[0]));
        FullSystemRunner runner = new FullSystemRunner();
        runner.process(config);
    }

}
