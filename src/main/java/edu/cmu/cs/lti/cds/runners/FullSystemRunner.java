package edu.cmu.cs.lti.cds.runners;

import edu.cmu.cs.lti.cds.annotators.annos.EventMentionTupleExtractor;
import edu.cmu.cs.lti.cds.annotators.annos.GoalMentionAnnotator;
import edu.cmu.cs.lti.cds.annotators.annos.SyntacticArgumentPropagateAnnotator;
import edu.cmu.cs.lti.cds.annotators.annos.SyntacticDirectArgumentFixer;
import edu.cmu.cs.lti.cds.annotators.clustering.WhRcModResoluter;
import edu.cmu.cs.lti.cds.annotators.patches.HeadWordFixer;
import edu.cmu.cs.lti.cds.annotators.script.karlmooney.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
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
        String paramBaseOutputDirName = args[2]; // "event_tuples"
        String paramOutputFileSuffix = null;
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        int outputStepNum = 0;

        System.out.println("Reading from " + paramInputDir);

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, paramInputDir, false);

        AnalysisEngineDescription fixer = CustomAnalysisEngineFactory.createAnalysisEngine(HeadWordFixer.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription whLinker = CustomAnalysisEngineFactory.createAnalysisEngine(WhRcModResoluter.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription tupleExtractor = CustomAnalysisEngineFactory.createAnalysisEngine(EventMentionTupleExtractor.class, typeSystemDescription);

        AnalysisEngineDescription syntaticDirectArgumentExtractor = CustomAnalysisEngineFactory.createAnalysisEngine(SyntacticDirectArgumentFixer.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription syntacticArgumentPropagater = CustomAnalysisEngineFactory.createAnalysisEngine(SyntacticArgumentPropagateAnnotator.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription goalMentionAnnotator = CustomAnalysisEngineFactory.createAnalysisEngine(GoalMentionAnnotator.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription kmScriptCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                KarlMooneyScriptCounter.class, typeSystemDescription,
                KarlMooneyScriptCounter.PARAM_DB_DIR_PATH, "data/_db/",
                KarlMooneyScriptCounter.PARAM_SKIP_BIGRAM_N, 2,
                AbstractLoggingAnnotator.PARAM_KEEP_QUIET, false);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzipWriter(
                paramParentOutputDir, paramBaseOutputDirName, outputStepNum, paramOutputFileSuffix, null);


        // Run the pipeline.
        SimplePipeline.runPipeline(reader, fixer, whLinker, tupleExtractor, syntaticDirectArgumentExtractor, syntacticArgumentPropagater, goalMentionAnnotator, kmScriptCounter, writer);

        System.out.println(className + " successfully completed.");
    }
}