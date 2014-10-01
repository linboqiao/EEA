package edu.cmu.cs.lti.cds.runners;

import edu.cmu.cs.lti.cds.annotators.DiscourseParserAnnotator;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.examples.xmi.XmiCollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/30/14
 * Time: 9:37 PM
 */
public class DiscourseParserRunner {
    private static String className = DiscourseParserRunner.class.getSimpleName();

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        System.out.println(className + " started...");

        // ///////////////////////// Parameter Setting ////////////////////////////
        // Note that you should change the parameters below for your configuration.
        // //////////////////////////////////////////////////////////////////////////
        // Parameters for the reader
        String paramInputDir = "data/01_event_tuples";

        // Parameters for the writer
        String paramParentOutputDir = "data";
        String paramBaseOutputDirName = "discourse_parsed";
        String paramOutputFileSuffix = null;
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                XmiCollectionReader.class, typeSystemDescription, XmiCollectionReader.PARAM_INPUTDIR,
                paramInputDir);

        AnalysisEngineDescription discourseParser = CustomAnalysisEngineFactory.createAnalysisEngine(
                DiscourseParserAnnotator.class, typeSystemDescription);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, 2, paramOutputFileSuffix);

        SimplePipeline.runPipeline(reader, discourseParser, writer);
    }
}