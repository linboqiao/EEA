package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.DiscourseParserAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/30/14
 * Time: 9:37 PM
 */
public class DiscourseParserRunner {
    //TODO make sure this can be run outside, figure out the scala version problem
    private static String className = DiscourseParserRunner.class.getSimpleName();

//    static Logger logger = Logger.getLogger(className);

    static Logger logger = LoggerFactory.getLogger(DiscourseParserRunner.className);

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        logger.info(className + " started...");

        if (args.length < 2) {
            logger.info("Please provide parent, base input directory");
            System.exit(1);
        }

        String parentInput = args[0]; //"data";

        // Parameters for the writer
        String baseInput = args[1]; //"01_event_tuples"

        String paramBaseOutputDirName = "discourse_parsed";

        Integer outputStepNum = null;
        if (args.length >= 4) {
            outputStepNum = Integer.parseInt(args[3]);
        }

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, parentInput, baseInput);

        AnalysisEngineDescription discourseParser = AnalysisEngineFactory.createEngineDescription(
                DiscourseParserAnnotator.class, typeSystemDescription);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                parentInput, paramBaseOutputDirName, outputStepNum);

        SimplePipeline.runPipeline(reader, discourseParser, writer);
    }
}