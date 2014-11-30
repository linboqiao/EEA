/**
 *
 */
package edu.cmu.cs.lti.cds.runners.script.train;

import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.annotators.script.train.UnigramScriptCounter;
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

/**
 * @author zhengzhongliu
 */
public class MooneyUnigramCounterRunner {
    private static String className = MooneyUnigramCounterRunner.class.getSimpleName();

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws Exception {
        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));
        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //data/_db

        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        String occSuffix = dbNames[0]; //e.g. 00-02, full

        String headIdMapName = KarlMooneyScriptCounter.defaltHeadIdMapName;

        // ////////////////////////////////////////////////////////////////

        DataPool.readBlackList(new File(blackListFile));
        DataPool.loadHeadIds(dbPath, dbNames[0], headIdMapName);

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription unigramCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                UnigramScriptCounter.class, typeSystemDescription,
                UnigramScriptCounter.PARAM_DB_DIR_PATH, dbPath,
                UnigramScriptCounter.PARAM_DB_NAME, "occs_" + occSuffix,
                UnigramScriptCounter.PARAM_KEEP_QUIET, false);

        SimplePipeline.runPipeline(reader, unigramCounter);

        System.out.println(className + " completed.");
    }
}
