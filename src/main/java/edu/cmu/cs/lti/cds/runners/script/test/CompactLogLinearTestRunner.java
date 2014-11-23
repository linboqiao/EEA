/**
 *
 */
package edu.cmu.cs.lti.cds.runners.script.test;

import edu.cmu.cs.lti.cds.annotators.script.test.CompactLogLinearTester;
import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.utils.DataPool;
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
 * @author zhengzhongliu
 */
public class CompactLogLinearTestRunner {
    private static String className = CompactLogLinearTestRunner.class.getSimpleName();

    private static Logger logger = Logger.getLogger(className);

    public static int[] allK;
    public static String outputPath;
    public static double mrr = 0;
    public static double totalCount = 0;
    public static int[] recallCounts;

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws Exception {
        logger.info(className + " started...");

        Configuration config = new Configuration(new File(args[0]));

        String subPath = args.length > 1 ? "_" + args[1] : "_ll";

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.heldout.path"); //"data/02_event_tuples";
        String clozePath = config.get("edu.cmu.cs.lti.cds.cloze.path"); // "cloze"
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath");
        String[] headCountFileNames = config.getList("edu.cmu.cs.lti.cds.headcount.files"); //"headcounts"
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
        //use negative sampling models
        String modelPath = config.get("edu.cmu.cs.lti.cds.negative.model.testing.path");
//        String modelPath = config.get("edu.cmu.cs.lti.cds.negative.model.path") + (config.getInt("edu.cmu.cs.lti.cds.sgd.iter") - 1) + config.get("edu.cmu.cs.lti.cds.model.suffix");
//        String modelPath = config.get("edu.cmu.cs.lti.cds.nce.model.path") + config.get("edu.cmu.cs.lti.cds.testing.model");
        boolean ignoreLowFreq = config.getBoolean("edu.cmu.cs.lti.cds.filter.lowfreq");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;

        //prepare data
        logger.info("Loading data");
        DataPool.loadHeadIds(dbPath, dbNames[0], KarlMooneyScriptCounter.defaltHeadIdMapName);
        DataPool.loadHeadCounts(dbPath, headCountFileNames, true);
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
        allK = config.getIntList("edu.cmu.cs.lti.cds.eval.rank.k");
        outputPath = config.get("edu.cmu.cs.lti.cds.eval.result.path") + subPath;
        recallCounts = new int[allK.length];

        AnalysisEngineDescription tester = CustomAnalysisEngineFactory.createAnalysisEngine(
                CompactLogLinearTester.class, typeSystemDescription,
                CompactLogLinearTester.PARAM_CLOZE_DIR_PATH, clozePath,
                CompactLogLinearTester.PARAM_DB_DIR_PATH, dbPath,
                CompactLogLinearTester.PARAM_HEAD_COUNT_DB_NAMES, headCountFileNames,
                CompactLogLinearTester.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                CompactLogLinearTester.PARAM_MODEL_PATH, modelPath,
                CompactLogLinearTester.PARAM_KEEP_QUIET, false
        );

        SimplePipeline.runPipeline(reader, tester);


        for (int kPos = 0; kPos < allK.length; kPos++) {
            logger.info(String.format("Recall at %d : %.4f", allK[kPos], recallCounts[kPos] * 1.0 / totalCount));
        }

        logger.info(String.format("MRR is : %.4f", mrr / totalCount));

        logger.info("Completed.");
    }
}