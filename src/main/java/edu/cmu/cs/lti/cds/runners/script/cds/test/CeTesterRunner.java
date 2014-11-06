/**
 *
 */
package edu.cmu.cs.lti.cds.runners.script.cds.test;

import edu.cmu.cs.lti.cds.annotators.script.CeTester;
import edu.cmu.cs.lti.cds.annotators.script.karlmooney.KarlMooneyScriptCounter;
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
public class CeTesterRunner {
    private static String className = CeTesterRunner.class.getSimpleName();

    private static Logger logger = Logger.getLogger(className);

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));
        String subPath = args.length > 1 ? args[1] : "";

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path") + "/" + subPath; //"data/02_event_tuples";
        String clozePath = config.get("edu.cmu.cs.lti.cds.cloze.path") + "_" + subPath; // "cloze"
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath");
        String[] headCountFileNames = config.getList("edu.cmu.cs.lti.cds.headcount.files"); //"headcounts"
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
        String modelPath = config.get("edu.cmu.cs.lti.cds.nce.model.path") + config.get("edu.cmu.cs.lti.cds.testing.model");
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
                CustomCollectionReaderFactory.createGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createAnalysisEngine(
                CeTester.class, typeSystemDescription,
                CeTester.PARAM_CLOZE_DIR_PATH, clozePath,
                CeTester.PARAM_DB_DIR_PATH, dbPath,
                CeTester.PARAM_HEAD_COUNT_DB_NAMES, headCountFileNames,
                CeTester.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                CeTester.PARAM_MODEL_PATH, modelPath,
                CeTester.PARAM_KEEP_QUIET, false
        );

        SimplePipeline.runPipeline(reader, writer);

        System.out.println(className + " completed.");
    }
}