package edu.cmu.cs.lti.emd.annotators;

import edu.cmu.cs.lti.emd.decoding.BeamCrfDecoder;
import edu.cmu.cs.lti.emd.utils.MentionUtils;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.decoding.NodeLinkingState;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/31/15
 * Time: 4:03 PM
 *
 * @author Zhengzhong Liu
 */
public class BeamTypeAnnotator extends AbstractLoggingAnnotator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private SentenceFeatureExtractor sentenceExtractor;
    //    private static SequenceDecoder decoder;

    private BeamCrfDecoder decoder;

    public static final String PARAM_MODEL_DIRECTORY = "modelDirectory";
    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    private File modelDirectory;

    public static final String PARAM_CONFIG = "configuration";
    @ConfigurationParameter(name = PARAM_CONFIG)
    private Configuration config;

    public static final String PARAM_BEAM_SIZE = "beamSize";
    @ConfigurationParameter(name = PARAM_BEAM_SIZE)
    private int beamSize;

//    public static final String PARAM_LOSS_TYPE = "lossType";
//    @ConfigurationParameter(name = PARAM_LOSS_TYPE)
//    private String lossType;

    // A dummy lagrangian variable.
    private DummyCubicLagrangian lagrangian = new DummyCubicLagrangian();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Loading models ...");

        String featureSpec;
        FeatureAlphabet alphabet;
        GraphWeightVector weightVector;
        try {
            weightVector = SerializationUtils.deserialize(new FileInputStream(new File
                    (modelDirectory, ModelConstants.TYPE_MODEL_NAME)));
            alphabet = weightVector.getFeatureAlphabet();
            featureSpec = weightVector.getFeatureSpec();
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

            String[] savedFeatureSpecs = FeatureUtils.splitFeatureSpec(featureSpec);
            String savedSentFeatureSpec = savedFeatureSpecs[0];
            String savedDocFeatureSpec = (savedFeatureSpecs.length == 2) ? savedFeatureSpecs[1] : "";

            String currentSentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.sentence.spec");
            String currentDocFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.doc.spec");

            warning(savedSentFeatureSpec, currentSentFeatureSpec);
            warning(savedDocFeatureSpec, currentDocFeatureSpec);

            logger.info("Sent feature spec : " + savedSentFeatureSpec);
            logger.info("Doc feature spec : " + savedDocFeatureSpec);

            Configuration sentFeatureConfig = sentFeatureSpecParser.parseFeatureFunctionSpecs(savedSentFeatureSpec);
            Configuration docFeatureConfig = docFeatureSpecParser.parseFeatureFunctionSpecs(savedDocFeatureSpec);

            sentenceExtractor = new SentenceFeatureExtractor(alphabet, config, sentFeatureConfig, docFeatureConfig,
                    false);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }

        decoder = new BeamCrfDecoder(weightVector, sentenceExtractor, beamSize);

    }

    private void warning(String savedSpec, String oldSpec) {
        if (!oldSpec.equals(savedSpec)) {
            logger.warn("Current feature specification is not the same with the trained model.");
            logger.warn("Will use the stored specification, it might create unexpected errors");
            logger.warn("Using Spec:" + savedSpec);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger, true);
        sentenceExtractor.initWorkspace(aJCas);

        List<StanfordCorenlpToken> allTokens = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpToken.class));
        List<MentionCandidate> systemCandidates = MentionUtils.createCandidatesFromTokens(aJCas, allTokens);

        NodeLinkingState decodeResult = decoder.decode(aJCas, systemCandidates, new ArrayList<>(), true);

//        int numMentions = 0;
        for (MultiNodeKey nodeKey : decodeResult.getActualNodeResults()) {
            if (!nodeKey.getCombinedType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                for (NodeKey key : nodeKey.getKeys()) {
                    EventMention mention = new EventMention(aJCas);
                    mention.setEventType(key.getMentionType());
                    UimaAnnotationUtils.finishAnnotation(mention, key.getBegin(), key.getEnd(), COMPONENT_ID, 0, aJCas);
                }
            }
        }
//        logger.info("Number of event mentions is " + numMentions);
    }
}
