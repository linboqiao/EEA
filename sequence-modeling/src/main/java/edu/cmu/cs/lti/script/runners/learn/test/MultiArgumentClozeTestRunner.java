package edu.cmu.cs.lti.script.runners.learn.test;

import edu.cmu.cs.lti.script.annotators.learn.test.CompactLogLinearTester;
import edu.cmu.cs.lti.script.annotators.learn.test.ConditionProbabilityTester;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
        String evalLogDirectoryPath = config.get("edu.cmu.cs.lti.cds.eval.log.path");

        boolean ignoreLowFreq = config.getBoolean("edu.cmu.cs.lti.cds.filter.lowfreq");
        String featurePackage = config.get("edu.cmu.cs.lti.cds.features.packagename");
        int maxSkippedGramN = config.getInt("edu.cmu.cs.lti.cds.max.n");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;

        float smoothingParameter = config.getInt("edu.cmu.cs.lti.cds.conditional.smoothing");

        Set<String> methods = new HashSet<>(Arrays.asList(config.getList("edu.cmu.cs.lti.cds.methods")));

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

        if (methods.contains("loglinear")) {
            String[] modelPaths = config.getList("edu.cmu.cs.lti.cds.loglinear.model");
            String modelPathBase = config.get("edu.cmu.cs.lti.cds.perceptron.model.path");
            String semLinkPath = config.get("edu.cmu.cs.lti.cds.db.semlink.path");
            boolean testMode = config.getBoolean("edu.cmu.cs.lti.cds.test.mode");

            DataPool.loadSemLinkData(semLinkPath);

            for (String modelPath : modelPaths) {
//                String[] featureNames = modelPath.replaceAll("^" + modelPathBase + "_", "").replaceAll("_\\d.ser$", "").split("_");
                String[] featureNames = config.getList("edu.cmu.cs.lti.cds.features");
                //make complete class name
                for (int i = 0; i < featureNames.length; i++) {
                    featureNames[i] = featurePackage + "." + featureNames[i];
                    logger.info("Register feature : " + featureNames[i]);
                }

                AnalysisEngineDescription logLinearPredictor = CustomAnalysisEngineFactory.createAnalysisEngine(
                        CompactLogLinearTester.class, typeSystemDescription,
                        MultiArgumentClozeTest.PARAM_CLOZE_DIR_PATH, clozePath,
                        MultiArgumentClozeTest.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                        MultiArgumentClozeTest.PARAM_EVAL_RESULT_PATH, evalResultBasePath,
                        MultiArgumentClozeTest.PARAM_EVAL_RANKS, allK,
                        MultiArgumentClozeTest.PARAM_EVAL_LOG_DIR, evalLogDirectoryPath,

                        CompactLogLinearTester.PARAM_DB_DIR_PATH, dbPath,
                        CompactLogLinearTester.PARAM_MODEL_PATH, modelPath,
                        CompactLogLinearTester.PARAM_KEEP_QUIET, false,
                        CompactLogLinearTester.PARAM_MAX_SKIP_GRAM_N, maxSkippedGramN,
                        CompactLogLinearTester.PARAM_FEATURE_NAMES, featureNames,
                        CompactLogLinearTester.PARAM_USE_TEST_MODE, testMode
                );

                SimplePipeline.runPipeline(reader, logLinearPredictor);
            }
        }

        if (methods.contains("conditional")) {
            //mooney model
            AnalysisEngineDescription conditionalProbabilityPredictor = CustomAnalysisEngineFactory.createAnalysisEngine(
                    ConditionProbabilityTester.class, typeSystemDescription,
                    MultiArgumentClozeTest.PARAM_CLOZE_DIR_PATH, clozePath,
                    MultiArgumentClozeTest.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                    MultiArgumentClozeTest.PARAM_EVAL_RESULT_PATH, evalResultBasePath,
                    MultiArgumentClozeTest.PARAM_EVAL_RANKS, allK,
                    MultiArgumentClozeTest.PARAM_EVAL_LOG_DIR, evalLogDirectoryPath,

                    ConditionProbabilityTester.PARAM_DB_DIR_PATH, dbPath,
                    ConditionProbabilityTester.PARAM_DB_NAMES, dbNames,
                    ConditionProbabilityTester.PARAM_SMOOTHING, smoothingParameter
            );

            SimplePipeline.runPipeline(reader, conditionalProbabilityPredictor);
        }
    }
}