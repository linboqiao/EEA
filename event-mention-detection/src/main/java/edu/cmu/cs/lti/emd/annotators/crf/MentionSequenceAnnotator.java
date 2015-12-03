package edu.cmu.cs.lti.emd.annotators.crf;

import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.sentence.extractor.MultiSentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.SequenceSolution;
import edu.cmu.cs.lti.learning.training.SequenceDecoder;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
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

    private MultiSentenceFeatureExtractor<CandidateEventMention> featureExtractor;
    private ClassAlphabet classAlphabet;
    private GraphWeightVector weightVector;
    private SequenceDecoder decoder;

    public static final String PARAM_MODEL_DIRECTORY = "modelDirectory";
    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    private File modelDirectory;

    public static final String PARAM_VERBOSE = "verbose";
    @ConfigurationParameter(name = PARAM_VERBOSE, defaultValue = "false")
    private boolean verbose;

    public static final String PARAM_CONFIG = "configuration";
    @ConfigurationParameter(name = PARAM_CONFIG)
    private Configuration config;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Loading models ...");

        String featureSpec;
        FeatureAlphabet alphabet;
        try {
            weightVector = SerializationUtils.deserialize(new FileInputStream(new File
                    (modelDirectory, MentionSequenceCrfTrainer.MODEL_NAME)));
            alphabet = weightVector.getFeatureAlphabet();
            classAlphabet = weightVector.getClassAlphabet();
            featureSpec = weightVector.getFeatureSpec();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        logger.info("Model loaded");
        try {
            FeatureSpecParser specParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.sentence.package.name"));

            String currentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv2.spec");

            if (!currentFeatureSpec.equals(featureSpec)) {
                logger.warn("Current feature specification is not the same with the trained model.");
                logger.warn("Will use the stored specification, it might create unexpected errors");
                logger.warn("Using Spec:" + featureSpec);
            }
            Configuration typeFeatureConfig = specParser.parseFeatureFunctionSpecs(featureSpec);

            featureExtractor = new MultiSentenceFeatureExtractor<>(alphabet, config,
                    typeFeatureConfig, CandidateEventMention.class);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
        decoder = new ViterbiDecoder(alphabet, classAlphabet);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas);

        featureExtractor.initWorkspace(aJCas);

        Collection<EventMention> predictedMentions = JCasUtil.select(aJCas, EventMention.class);

        decoder.decode(featureExtractor, weightVector, predictedMentions.size(), 0 /**No lagrangian in testing**/,
                true /**Use averaged feature vector**/);

        SequenceSolution prediction = decoder.getDecodedPrediction();

        if (verbose) {
            logger.debug(prediction.toString());
        }

        int index = 0;
        for (EventMention mention : predictedMentions) {
            String className = classAlphabet.getClassName(prediction.getClassAt(index));
            mention.setEventType(className);
            index++;
        }
    }
}
