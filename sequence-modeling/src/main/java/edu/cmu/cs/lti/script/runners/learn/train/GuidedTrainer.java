package edu.cmu.cs.lti.script.runners.learn.train;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.script.annotators.learn.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.script.annotators.learn.train.PerceptronTraining;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.BasicConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import weka.core.SerializationHelper;

import java.io.File;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/25/15
 * Time: 4:37 AM
 */
public class GuidedTrainer {

    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger(GuidedTrainer.class.getName());
        Configuration config = new Configuration(new File(args[0]));
        logger.info("Start training mooney...");
        trainMooney(config);
        logger.info("Start training perceptron...");
        trainPerceptron(config);
    }

    public static void trainMooney(Configuration config) throws Exception {
        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //data/_db
        String dbBaseName = config.get("edu.cmu.cs.lti.cds.db.basenames"); //occ_dev, occ_full
        boolean ignoreLowFreq = config.getBoolean("edu.cmu.cs.lti.cds.filter.lowfreq");
        int skipGramN = config.getInt("edu.cmu.cs.lti.cds.mooney.skipgram.n");

        DataPool.readBlackList(new File(blackListFile));

        if (ignoreLowFreq) {
            DataPool.loadHeadStatistics(config, false);
        }

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, true);

        AnalysisEngineDescription kmScriptCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                KarlMooneyScriptCounter.class, typeSystemDescription,
                KarlMooneyScriptCounter.PARAM_DB_DIR_PATH, dbPath,
                KarlMooneyScriptCounter.PARAM_DB_NAME, dbBaseName,
                KarlMooneyScriptCounter.PARAM_SKIP_BIGRAM_N, skipGramN,
                KarlMooneyScriptCounter.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                AbstractLoggingAnnotator.PARAM_KEEP_QUIET, false);

        SimplePipeline.runPipeline(reader, kmScriptCounter);
    }

    public static void trainPerceptron(Configuration config) throws Exception {
        Logger logger = Logger.getLogger(PerceptronTraining.class.getName());
        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path");
        int maxIter = config.getInt("edu.cmu.cs.lti.cds.sgd.iter");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String blackListFileName = config.get("edu.cmu.cs.lti.cds.blacklist");
        String modelStoragePath = config.get("edu.cmu.cs.lti.cds.perceptron.model.path");
        int miniBatchNum = config.getInt("edu.cmu.cs.lti.cds.minibatch");
        String modelExt = config.get("edu.cmu.cs.lti.cds.model.ext");
        String[] featureNames = config.getList("edu.cmu.cs.lti.cds.features");
        String featurePackage = config.get("edu.cmu.cs.lti.cds.features.packagename");
        String semLinkPath = config.get("edu.cmu.cs.lti.cds.db.semlink.path");
        int maxSkipN = config.getInt("edu.cmu.cs.lti.cds.max.n");

        boolean guided = config.getBoolean("edu.cmu.cs.lti.cds.perceptron.guided");
        int topRankToOptimize = config.getInt("edu.cmu.cs.lti.cds.perceptron.top.rank.optimize");

        int rankListSize = config.getInt("edu.cmu.cs.lti.cds.perceptron.ranklist.size");
        float smoothingParameter = config.getInt("edu.cmu.cs.lti.cds.conditional.smoothing");

        String modelSuffix = topRankToOptimize + "_" + Joiner.on("_").join(featureNames);

        if (guided) {
            modelSuffix = "guided_" + modelSuffix;
        }

        logger.info("Model will be stored with suffix : " + modelSuffix);


        //make complete class name
        for (int i = 0; i < featureNames.length; i++) {
            featureNames[i] = featurePackage + "." + featureNames[i];
        }

        String paramTypeSystemDescriptor = "TypeSystem";

        //prepare data
        logger.info("Loading data.");
        DataPool.loadHeadStatistics(config, false);
        DataPool.readBlackList(new File(blackListFileName));
//        DataPool.loadKmCooccMap(dbPath, dbNames[0], KarlMooneyScriptCounter.defaultCooccMapName);
        DataPool.loadEventUnigramCounts(config);
        DataPool.loadSemLinkData(semLinkPath);
        logger.info("Finish data loading.");

        logger.info("# predicates " + DataPool.headIdMap.size());
        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        logger.info("Running " + PerceptronTraining.class.getName());


        AnalysisEngineDescription trainer = CustomAnalysisEngineFactory.createAnalysisEngine(PerceptronTraining.class, typeSystemDescription,
                PerceptronTraining.PARAM_RANK_LIST_SIZE, rankListSize,
                PerceptronTraining.PARAM_MINI_BATCH_SIZE, miniBatchNum,
                PerceptronTraining.PARAM_FEATURE_NAMES, featureNames,
                PerceptronTraining.PARAM_MAX_SKIP_GRAM_N, maxSkipN,
                PerceptronTraining.PARAM_PSEUDO_GUIDE, guided,
                PerceptronTraining.PARAM_TOP_RANK_TO_OPTIMIZE, topRankToOptimize,
                PerceptronTraining.PARAM_DB_DIR_PATH, dbPath,
                PerceptronTraining.PARAM_DB_NAMES, dbNames,
                PerceptronTraining.PARAM_SMOOTHING, smoothingParameter
        );

        PerceptronTraining.initializeParameters();
        BasicConvenience.printMemInfo(logger, "Beginning memory");

        for (int i = 0; i < maxIter; i++) {
            String modelOutputPath = modelStoragePath + "_" + modelSuffix + "_" + i + modelExt;
//            String averageModelOutputPath = modelOutputPath + "_average";

            SimplePipeline.runPipeline(reader, trainer);
            File modelDirParent = new File(modelStoragePath).getParentFile();

            if (!modelDirParent.exists()) {
                modelDirParent.mkdirs();
            }

            logger.info("Storing this model to " + modelOutputPath);
            SerializationHelper.write(modelOutputPath, PerceptronTraining.trainingFeatureTable);
        }
    }
}
