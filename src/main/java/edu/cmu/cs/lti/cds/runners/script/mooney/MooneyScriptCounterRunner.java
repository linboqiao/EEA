/**
 *
 */
package edu.cmu.cs.lti.cds.runners.script.mooney;

import edu.cmu.cs.lti.cds.annotators.script.karlmooney.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.runners.FullSystemRunner;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author zhengzhongliu
 */
public class MooneyScriptCounterRunner {
    private static String className = MooneyScriptCounterRunner.class.getSimpleName();

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));
        String occSuffix = args.length > 1 ? args[1] : ""; //e.g. 00-02, full

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //data/_db
        String[] headCountFileNames = config.getList("edu.cmu.cs.lti.cds.headcount.files"); //"headcounts"
        boolean ignoreLowFreq = config.getBoolean("edu.cmu.cs.lti.cds.filter.lowfreq");
        int skipGramN = config.getInt("edu.cmu.cs.lti.cds.skipgram.n");

        // ////////////////////////////////////////////////////////////////

        FullSystemRunner.readBlackList(new File(blackListFile));

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription kmScriptCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                KarlMooneyScriptCounter.class, typeSystemDescription,
                KarlMooneyScriptCounter.PARAM_DB_DIR_PATH, dbPath,
                KarlMooneyScriptCounter.PARAM_SKIP_BIGRAM_N, skipGramN,
                KarlMooneyScriptCounter.PARAM_DB_NAME, "occs_" + occSuffix,
                KarlMooneyScriptCounter.PARAM_HEAD_COUNT_DB_NAMES, headCountFileNames,
                KarlMooneyScriptCounter.PARAM_IGNORE_LOW_FREQ, ignoreLowFreq,
                AbstractLoggingAnnotator.PARAM_KEEP_QUIET, false);

        SimplePipeline.runPipeline(reader, kmScriptCounter);

        System.out.println(className + " completed.");
    }
}
