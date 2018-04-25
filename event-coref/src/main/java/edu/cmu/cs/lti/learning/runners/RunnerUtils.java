package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.annotators.GoldStandardEventMentionAnnotator;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.event_coref.annotators.misc.GoldRemover;
import edu.cmu.cs.lti.event_coref.annotators.prepare.MergedArgumentAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.prepare.EventHeadWordAnnotator;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.uima.io.writer.DocumentTextWriter;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/12/16
 * Time: 1:56 PM
 *
 * @author Zhengzhong Liu
 */
public class RunnerUtils {
    private static final Logger logger = LoggerFactory.getLogger(RunnerUtils.class);

    public static void writeResults(CollectionReaderDescription processedResultReader,
                                    TypeSystemDescription typeSystemDescription, String tbfOutput,
                                    String systemId, boolean useCharOffset, boolean addSemanticRole)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        logger.info("Writing results to " + tbfOutput);
        AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                TbfStyleEventWriter.class, typeSystemDescription,
                TbfStyleEventWriter.PARAM_OUTPUT_PATH, tbfOutput,
                TbfStyleEventWriter.PARAM_SYSTEM_ID, systemId,
                TbfStyleEventWriter.PARAM_GOLD_TOKEN_COMPONENT_ID, UimaConst.goldComponentName,
                TbfStyleEventWriter.PARAM_USE_CHARACTER_OFFSET, useCharOffset,
                TbfStyleEventWriter.PARAM_ADD_SEMANTIC_ROLE, addSemanticRole
        );
        new BasicPipeline(processedResultReader, resultWriter).run();
    }

    public static void addMentionPostprocessors(List<AnalysisEngineDescription> annotators,
                                                TypeSystemDescription typeSystemDescription,
                                                String language) throws
            ResourceInitializationException {
        AnalysisEngineDescription headWordExtractor = AnalysisEngineFactory.createEngineDescription(
                EventHeadWordAnnotator.class, typeSystemDescription
        );

        if (language.equals("zh")) {
            annotators.add(headWordExtractor);
        } else {
            AnalysisEngineDescription argumentExtractor = AnalysisEngineFactory.createEngineDescription(
                    MergedArgumentAnnotator.class, typeSystemDescription
            );
            annotators.add(headWordExtractor);
            annotators.add(argumentExtractor);

//            AnalysisEngineDescription caevo = AnalysisEngineFactory.createEngineDescription(
//                    CaevoAnnotator.class
//            );
//            annotators.add(caevo);
        }
    }

    public static AnalysisEngineDescription getGoldAnnotator(boolean copyType, boolean copyRealis, boolean copyCluster,
                                                             boolean copyRelations)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                GoldStandardEventMentionAnnotator.class,
                GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS, new String[]{CAS.NAME_DEFAULT_SOFA},
                GoldStandardEventMentionAnnotator.PARAM_COPY_MENTION_TYPE, copyType,
                GoldStandardEventMentionAnnotator.PARAM_COPY_REALIS, copyRealis,
                GoldStandardEventMentionAnnotator.PARAM_COPY_CLUSTER, copyCluster,
                GoldStandardEventMentionAnnotator.PARAM_COPY_RELATIONS, copyRelations
        );
    }

    public static void writeGold(CollectionReaderDescription reader, String goldTbfOutput, boolean useCharOffset) throws
            UIMAException, IOException, CpeDescriptorException, SAXException {
        AnalysisEngineDescription goldCopier = getGoldAnnotator(true, true, true, true);
        AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                TbfStyleEventWriter.class,
                TbfStyleEventWriter.PARAM_OUTPUT_PATH, goldTbfOutput,
                TbfStyleEventWriter.PARAM_SYSTEM_ID, "gold",
                TbfStyleEventWriter.PARAM_GOLD_TOKEN_COMPONENT_ID, TbfEventDataReader.COMPONENT_ID,
                TbfStyleEventWriter.PARAM_USE_CHARACTER_OFFSET, useCharOffset,
                TbfStyleEventWriter.PARAM_TARGET_VIEW_NAME, UimaConst.goldViewName
        );
        new BasicPipeline(reader, goldCopier, resultWriter).run();
    }

    public static CollectionReaderDescription removeGold(CollectionReaderDescription trainingData, String mainDir,
                                                         String baseDir)
            throws SAXException, UIMAException, CpeDescriptorException, IOException {
        AnalysisEngineDescription goldRemover = AnalysisEngineFactory.createEngineDescription(
                GoldRemover.class);
        return new BasicPipeline(trainingData, mainDir, baseDir, goldRemover).run().getOutput();
    }

    public static void writeText(CollectionReaderDescription reader, String parentDir, String baseDir) throws
            IOException, UIMAException {
        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                DocumentTextWriter.class,
                DocumentTextWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentDir,
                DocumentTextWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseDir
        );
        SimplePipeline.runPipeline(reader, writer);
    }
}
