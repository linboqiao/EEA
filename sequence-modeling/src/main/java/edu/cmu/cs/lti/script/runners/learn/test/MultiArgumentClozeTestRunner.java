package edu.cmu.cs.lti.script.runners.learn.test;

import edu.cmu.cs.lti.script.annotators.learn.test.CompactLogLinearTester;
import edu.cmu.cs.lti.script.annotators.learn.test.ConditionProbablityTester;
import edu.cmu.cs.lti.script.annotators.learn.test.MultiArgumentClozeTest;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/14/15
 * Time: 1:51 AM
 */
public class MultiArgumentClozeTestRunner {
    private static String className = MultiArgumentClozeTestRunner.class.getSimpleName();

    private static Logger logger = Logger.getLogger(className);

//    public static List<Integer> allK;
//    public static String outputPath;
//    public static int[] recallCounts;

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws Exception {
        logger.info(className + " started...");
        Configuration config = new Configuration(new File(args[0]));

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.heldout.path"); //"data/02_event_tuples";
        String clozePath = config.get("edu.cmu.cs.lti.cds.cloze.path"); // "cloze"
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath");
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
//        String modelPath = config.get("edu.cmu.cs.lti.cds.negative.model.testing.path");
        String[] modelPaths = config.getList("edu.cmu.cs.lti.cds.negative.model.testing.path");
        String evalLogPath = config.get("edu.cmu.cs.lti.cds.eval.log.path");

        boolean ignoreLowFreq = config.getBoolean("edu.cmu.cs.lti.cds.filter.lowfreq");
        String[] featureNames = config.getList("edu.cmu.cs.lti.cds.features");
        String featurePackage = config.get("edu.cmu.cs.lti.cds.features.packagename");
        int skipgramN = config.getInt("edu.cmu.cs.lti.cds.skipgram.n");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;

        //make complete class name
        for (int i = 0; i < featureNames.length; i++) {
            featureNames[i] = featurePackage + "." + featureNames[i];
        }

        //prepare data
        logger.info("Loading data");
        DataPool.loadHeadStatistics(config, false);
        DataPool.readBlackList(new File(blackListFile));
        logger.info("# predicates " + DataPool.headIdMap.size());

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        //initialize eval parameter
        int[] allK = config.getIntList("edu.cmu.cs.lti.cds.eval.rank.k");
        String evalResultBasePath = config.get("edu.cmu.cs.lti.cds.eval.result.path");


        for (String modelPath : modelPaths) {

            AnalysisEngineDescription logLinearPredictor = CustomAnalysisEngineFactory.createAnalysisEngine(
                    CompactLogLinearTester.class, typeSystemDescription,
                    MultiArgumentClozeTest.PARAM_CLOZE_DIR_PATH, clozePath,
                    MultiArgumentClozeTest.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                    MultiArgumentClozeTest.PARAM_EVAL_RESULT_PATH, evalResultBasePath,
                    MultiArgumentClozeTest.PARAM_EVAL_RANKS, allK,
                    MultiArgumentClozeTest.PARAM_EVAL_LOG_DIR, evalLogPath,

                    CompactLogLinearTester.PARAM_DB_DIR_PATH, dbPath,
                    CompactLogLinearTester.PARAM_MODEL_PATH, modelPath,
                    CompactLogLinearTester.PARAM_KEEP_QUIET, false,
                    CompactLogLinearTester.PARAM_SKIP_GRAM_N, skipgramN,
                    CompactLogLinearTester.PARAM_FEATURE_NAMES, featureNames
            );

            SimplePipeline.runPipeline(reader, logLinearPredictor);

        }

        AnalysisEngineDescription conditionalProbabilityPredictor = CustomAnalysisEngineFactory.createAnalysisEngine(
                ConditionProbablityTester.class, typeSystemDescription,
                MultiArgumentClozeTest.PARAM_CLOZE_DIR_PATH, clozePath,
                MultiArgumentClozeTest.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                MultiArgumentClozeTest.PARAM_EVAL_RESULT_PATH, evalResultBasePath,
                MultiArgumentClozeTest.PARAM_EVAL_RANKS, allK,
                MultiArgumentClozeTest.PARAM_EVAL_LOG_DIR, evalLogPath,

                ConditionProbablityTester.PARAM_DB_DIR_PATH, dbPath,
                ConditionProbablityTester.PARAM_DB_NAMES, dbNames,
                ConditionProbablityTester.PARAM_SMOOTHING, 1
        );

        SimplePipeline.runPipeline(reader, conditionalProbabilityPredictor);
    }
}
