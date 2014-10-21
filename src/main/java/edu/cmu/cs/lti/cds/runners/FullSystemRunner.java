package edu.cmu.cs.lti.cds.runners;

import edu.cmu.cs.lti.cds.annotators.annos.DiscourseParserAnnotator;
import edu.cmu.cs.lti.cds.annotators.annos.EventMentionTupleExtractor;
import edu.cmu.cs.lti.cds.annotators.annos.SingletonAnnotator;
import edu.cmu.cs.lti.collection_reader.AgigaCollectionReader;
import edu.cmu.cs.lti.script.annotators.FanseAnnotator;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;

public class FullSystemRunner {
    static String className = FullSystemRunner.class.getName();

    public static void main(String[] args) throws UIMAException, IOException {
        System.out.println(className + " started...");

        // ///////////////////////// Parameter Setting ////////////////////////////
        // Note that you should change the parameters below for your configuration.
        // //////////////////////////////////////////////////////////////////////////
        // Parameters for the reader
        String paramInputDir = args[0];// "/Users/zhengzhongliu/Documents/data/agiga_sample/"

        // Parameters for the writer
        String paramParentOutputDir = args[1]; // "data";
        String paramOutputFileSuffix = null;

        String paramFanseModelBaseDirectory = args[2];// "/Users/zhengzhongliu/Documents/projects/uimafied-tools/fanse-parser/src/main/resources/"
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        int outputStepNum = 0;

        boolean quiet = false;
        // Quiet or not
        if (args.length >=4) {
            quiet = args[3].equals("quiet");
        }

        System.out.println("Reading from " + paramInputDir);

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                AgigaCollectionReader.class, typeSystemDescription,
                AgigaCollectionReader.PARAM_INPUTDIR, paramInputDir);

        AnalysisEngineDescription aWriter = CustomAnalysisEngineFactory.createXmiWriter(
                paramParentOutputDir, "agiga", outputStepNum, paramOutputFileSuffix);

        outputStepNum++;

        AnalysisEngineDescription fanseParser = CustomAnalysisEngineFactory.createAnalysisEngine(
                FanseAnnotator.class, typeSystemDescription, FanseAnnotator.PARAM_MODEL_BASE_DIR,
                paramFanseModelBaseDirectory, FanseAnnotator.PARAM_KEEP_QUIET, quiet);

        AnalysisEngineDescription fWriter = CustomAnalysisEngineFactory.createGzipWriter(
                paramParentOutputDir, "parsed", outputStepNum, paramOutputFileSuffix, null);

        outputStepNum++;

        AnalysisEngineDescription discourseParser = CustomAnalysisEngineFactory.createAnalysisEngine(
                DiscourseParserAnnotator.class, typeSystemDescription, DiscourseParserAnnotator.PARAM_KEEP_QUIET, quiet);

        AnalysisEngineDescription dWriter = CustomAnalysisEngineFactory.createGzipWriter(
                paramParentOutputDir, "discourse", outputStepNum, paramOutputFileSuffix, null);

        outputStepNum++;

        AnalysisEngineDescription singletonCreator = CustomAnalysisEngineFactory.createAnalysisEngine(
                SingletonAnnotator.class, typeSystemDescription, SingletonAnnotator.PARAM_KEEP_QUIET, quiet);

        AnalysisEngineDescription tupleExtractor = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionTupleExtractor.class, typeSystemDescription, EventMentionTupleExtractor.PARAM_KEEP_QUIET, quiet);

        AnalysisEngineDescription tWriter = CustomAnalysisEngineFactory.createGzipWriter(
                paramParentOutputDir, "event_tuples", outputStepNum, paramOutputFileSuffix, null);


        // Run the pipeline.
        SimplePipeline.runPipeline(reader, aWriter, fanseParser, fWriter, discourseParser, dWriter, singletonCreator, tupleExtractor, tWriter);

        System.out.println(className + " successfully completed.");
    }
}