package edu.cmu.cs.lti.emd.annotators.crf;

import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.MultiSentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.SequenceSolution;
import edu.cmu.cs.lti.learning.training.SequenceDecoder;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/28/15
 * Time: 4:39 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionSequenceAnnotator extends AbstractLoggingAnnotator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private MultiSentenceFeatureExtractor<EventMention> featureExtractor;
    private ClassAlphabet classAlphabet;
    private GraphWeightVector weightVector;
    private SequenceDecoder decoder;

    private DummyCubicLagrangian dummyCubicLagrangian = new DummyCubicLagrangian();

    public static final String PARAM_MODEL_DIRECTORY = "modelDirectory";
    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    private File modelDirectory;

    public static final String PARAM_CONFIG = "configuration";
    @ConfigurationParameter(name = PARAM_CONFIG)
    private Configuration config;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Loading Mention sequence model ...");

        String savedDocFeatureSpec;
        String savedSentFeatureSpec;
        FeatureAlphabet alphabet;
        try {
            weightVector = SerializationUtils.deserialize(new FileInputStream(new File
                    (modelDirectory, MentionSequenceCrfTrainer.MODEL_NAME)));
            alphabet = weightVector.getFeatureAlphabet();
            classAlphabet = weightVector.getClassAlphabet();
            String[] featureSpec = FeatureUtils.splitFeatureSpec(weightVector.getFeatureSpec());
            savedSentFeatureSpec = featureSpec[0];
            savedDocFeatureSpec = featureSpec[1];
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        logger.info("Model loaded");
        try {
            FeatureSpecParser sentFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.sentence.package.name"));

            FeatureSpecParser docFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.document.package.name")
            );

            String currentMentionFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv2.doc.spec");
            String currentWordFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv2.sentence.spec");

            boolean useStateFeature = config.getBoolean("edu.cmu.cs.lti.mention.use_state", true);

            specWarning(savedDocFeatureSpec, currentMentionFeatureSpec);
            specWarning(savedDocFeatureSpec, currentWordFeatureSpec);

            Configuration sentFeatureConfig = sentFeatureSpecParser.parseFeatureFunctionSpecs(savedSentFeatureSpec);
            Configuration mentionFeatureConfig =
                    docFeatureSpecParser.parseFeatureFunctionSpecs(savedDocFeatureSpec);

            featureExtractor = new MultiSentenceFeatureExtractor<>(alphabet, config, sentFeatureConfig,
                    mentionFeatureConfig, useStateFeature, EventMention.class);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
        decoder = new ViterbiDecoder(alphabet, classAlphabet);
    }

    private void specWarning(String oldSpec, String newSpec) {
        if (!oldSpec.equals(newSpec)) {
            logger.warn("Current feature specification is not the same with the trained model.");
            logger.warn("Will use the stored specification, it might create unexpected errors");
            logger.warn("Using Spec:" + oldSpec);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas);

        featureExtractor.initWorkspace(aJCas);

        DocumentAnnotation document = JCasUtil.selectSingle(aJCas, DocumentAnnotation.class);
        featureExtractor.resetWorkspace(aJCas, document.getBegin(), document.getEnd());

        Collection<EventMention> predictedMentions = JCasUtil.select(aJCas, EventMention.class);

        decoder.decode(featureExtractor, weightVector, predictedMentions.size(), dummyCubicLagrangian,
                dummyCubicLagrangian, true /**Use averaged feature vector**/);

        SequenceSolution prediction = decoder.getDecodedPrediction();

        int index = 0;
        for (EventMention mention : predictedMentions) {
            String className = classAlphabet.getClassName(prediction.getClassAt(index));
            mention.setEventType(className);
            index++;
        }
    }
}
