package edu.cmu.cs.lti.script.runners;

import edu.cmu.cs.lti.annotators.DiscourseParserAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/30/14
 * Time: 9:37 PM
 */
public class DiscourseParserRunner {
    private static String className = DiscourseParserRunner.class.getSimpleName();

    static Logger logger = Logger.getLogger(className);

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        logger.log(Level.INFO, className + " started...");

        if (args.length < 3){
            logger.log(Level.INFO, "Please provide input and output directory and step number");
            System.exit(1);
        }

        // ///////////////////////// Parameter Setting ////////////////////////////
        // Note that you should change the parameters below for your configuration.
        // //////////////////////////////////////////////////////////////////////////
        // Parameters for the reader
        String paramInputDir = args[0]; //"data/01_event_tuples";
        int inputStepNum = Integer.parseInt(args[1]);

//        String paramInputDir = "data/test";

        // Parameters for the writer
        String paramParentOutputDir = "data";
        String paramBaseOutputDirName = args[2]; //"discourse_parsed";
        String paramOutputFileSuffix = null;

        boolean quiet = false;
        // Quiet or not
        if (args.length >=4) {
             quiet = args[3].equals("quiet");
        }
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        int outputStepNum = inputStepNum + 1;

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, paramInputDir, false);

        AnalysisEngineDescription discourseParser = CustomAnalysisEngineFactory.createAnalysisEngine(
                DiscourseParserAnnotator.class, typeSystemDescription, DiscourseParserAnnotator.PARAM_KEEP_QUIET, quiet);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzipWriter(
                paramParentOutputDir, paramBaseOutputDirName, outputStepNum, paramOutputFileSuffix, null);

        SimplePipeline.runPipeline(reader, discourseParser, writer);
    }
}