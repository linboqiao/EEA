package edu.cmu.cs.lti.cds.runners;

import edu.cmu.cs.lti.cds.annotators.annos.*;
import edu.cmu.cs.lti.cds.annotators.clustering.WhRcModResoluter;
import edu.cmu.cs.lti.cds.annotators.patches.HeadWordFixer;
import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
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

        String blackListFile = args[3]; //"duplicate.count.tail"

        String dbNamePrefix = args[4]; //"00-02"

        String paramOutputFileSuffix = null;
        // ////////////////////////////////////////////////////////////////

        DataPool.readBlackList(new File(blackListFile));

        String paramTypeSystemDescriptor = "TypeSystem";

        int outputStepNum = 2;

        System.out.println("Reading from " + paramInputDir);

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
//        CollectionReaderDescription reader =
//                CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, paramInputDir, false);

        //use a unsorted fast reader
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createGzippedXmiReader(typeSystemDescription, paramInputDir, false);

        AnalysisEngineDescription fixer = CustomAnalysisEngineFactory.createAnalysisEngine(HeadWordFixer.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription whLinker = CustomAnalysisEngineFactory.createAnalysisEngine(WhRcModResoluter.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription tupleExtractor = CustomAnalysisEngineFactory.createAnalysisEngine(EventMentionTupleExtractor.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription syntaticDirectArgumentExtractor = CustomAnalysisEngineFactory.createAnalysisEngine(SyntacticDirectArgumentFixer.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription syntacticArgumentPropagater = CustomAnalysisEngineFactory.createAnalysisEngine(SyntacticArgumentPropagateAnnotator.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription goalMentionAnnotator = CustomAnalysisEngineFactory.createAnalysisEngine(GoalMentionAnnotator.class, typeSystemDescription, AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        String[] needIdTops = {Entity.class.getName()};

        AnalysisEngineDescription idAssignRunner = CustomAnalysisEngineFactory.createAnalysisEngine(
                IdAssigner.class, typeSystemDescription,
                IdAssigner.PARAM_TOP_NAMES_TO_ASSIGN, needIdTops,
                AbstractLoggingAnnotator.PARAM_KEEP_QUIET, true);

        AnalysisEngineDescription kmScriptCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                KarlMooneyScriptCounter.class, typeSystemDescription,
                KarlMooneyScriptCounter.PARAM_DB_DIR_PATH, "data/_db/",
                KarlMooneyScriptCounter.PARAM_SKIP_BIGRAM_N, 2,
                KarlMooneyScriptCounter.PARAM_DB_NAME, "occs_" + dbNamePrefix,
                AbstractLoggingAnnotator.PARAM_KEEP_QUIET, false);

        AnalysisEngineDescription headCounter = CustomAnalysisEngineFactory.createAnalysisEngine(
                EventMentionHeadCounter.class, typeSystemDescription,
                EventMentionHeadCounter.PARAM_DB_DIR_PATH, "data/_db/",
                EventMentionHeadCounter.PARAM_DB_NAME, "headcounts_" + dbNamePrefix,
                AbstractLoggingAnnotator.PARAM_KEEP_QUIET, false
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzipWriter(
                paramParentOutputDir, paramBaseOutputDirName, outputStepNum, paramOutputFileSuffix, null);

        // Run the pipeline.
        SimplePipeline.runPipeline(reader, fixer, whLinker, tupleExtractor, syntaticDirectArgumentExtractor, syntacticArgumentPropagater, goalMentionAnnotator, idAssignRunner, headCounter, writer);

        System.out.println(className + " successfully completed.");
    }
}