package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.event_coref.annotators.DDEventTypeCorefAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.misc.DifferentTypeCorefCollector;
import edu.cmu.cs.lti.event_coref.annotators.postprocessors.MentionTypeAndClusterSplitter;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/12/16
 * Time: 3:03 PM
 *
 * @author Zhengzhong Liu
 */
public class DDMentionCorefModelRunner extends AbstractMentionModelRunner {
    public DDMentionCorefModelRunner(Configuration mainConfig, TypeSystemDescription typeSystemDescription) {
        super(mainConfig, typeSystemDescription);
    }

    public CollectionReaderDescription ddMentionDetectionAndCoreference(
            Configuration config, CollectionReaderDescription reader, String mentionModelDir, String latentTreeDir,
            File corefRuleFile, String mainDir, String outputBase, boolean skipJointTest)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        logger.info("Running joint mention detection and coreference, output at " + outputBase);

        if (skipJointTest && new File(mainDir, outputBase).exists()) {
            logger.info("Skipping running Joint, using existing results.");
            return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, mainDir, outputBase);
        } else {
            AnalysisEngineDescription ddDecoder = AnalysisEngineFactory.createEngineDescription(
                    DDEventTypeCorefAnnotator.class, typeSystemDescription,
                    DDEventTypeCorefAnnotator.PARAM_CONFIG_PATH, config.getConfigFile(),
                    DDEventTypeCorefAnnotator.PARAM_MENTION_MODEL_DIRECTORY, mentionModelDir,
                    DDEventTypeCorefAnnotator.PARAM_COREF_MODEL_DIRECTORY, latentTreeDir,
                    DDEventTypeCorefAnnotator.PARAM_COREF_RULE_FILE, corefRuleFile
            );

            AnalysisEngineDescription mentionSplitter = AnalysisEngineFactory.createEngineDescription(
                    MentionTypeAndClusterSplitter.class, typeSystemDescription,
                    MentionTypeAndClusterSplitter.PARAM_COREF_RULE_FILE, corefRuleFile
            );

            return new BasicPipeline(reader, mainDir, outputBase, ddDecoder, mentionSplitter).run().getOutput();
        }
    }

    private void collectCorefConstraint(CollectionReaderDescription reader, File corefRuleFile)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        // Get type rules.
        AnalysisEngineDescription collector = AnalysisEngineFactory.createEngineDescription(
                DifferentTypeCorefCollector.class, typeSystemDescription,
                DifferentTypeCorefCollector.PARAM_COREFERENCE_ALLOWED_TYPES, corefRuleFile
        );

        new BasicPipeline(reader, collector).run();
    }
}
