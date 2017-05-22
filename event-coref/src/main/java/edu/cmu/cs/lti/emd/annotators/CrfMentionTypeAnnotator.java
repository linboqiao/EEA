package edu.cmu.cs.lti.emd.annotators;

import edu.cmu.cs.lti.emd.annotators.train.TokenLevelEventMentionCrfTrainer;
import edu.cmu.cs.lti.learning.decoding.SequenceDecoder;
import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.extractor.UimaSequenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.DebugUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.javatuples.Triplet;
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
public class CrfMentionTypeAnnotator extends AbstractLoggingAnnotator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private UimaSequenceFeatureExtractor sentenceExtractor;
    private ClassAlphabet classAlphabet;
    private GraphWeightVector weightVector;
    private static SequenceDecoder decoder;

    public static final String PARAM_MODEL_DIRECTORY = "modelDirectory";
    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    private File modelDirectory;

    public static final String PARAM_CONFIG = "configuration";
    @ConfigurationParameter(name = PARAM_CONFIG)
    private String configPath;

    private Configuration config;

    // A dummy lagrangian variable.
    private DummyCubicLagrangian lagrangian = new DummyCubicLagrangian();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        try {
            config = new Configuration(configPath);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        logger.info("Loading models ...");

        String featureSpec;
        FeatureAlphabet alphabet;
        try {
            weightVector = SerializationUtils.deserialize(new FileInputStream(new File
                    (modelDirectory, TokenLevelEventMentionCrfTrainer.MODEL_NAME)));
            alphabet = weightVector.getFeatureAlphabet();
            classAlphabet = weightVector.getClassAlphabet();
            featureSpec = weightVector.getFeatureSpec();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        logger.info("Model loaded");
        try {
            logger.info("");

            String sentenceFeaturePackage = "edu.cmu.cs.lti.feature.sentence.package.name";
            String documentFeaturePackage = "edu.cmu.cs.lti.feature.document.package.name";

            FeatureSpecParser sentFeatureSpecParser = new FeatureSpecParser(sentenceFeaturePackage);
            FeatureSpecParser docFeatureSpecParser = new FeatureSpecParser(documentFeaturePackage);

            logger.debug(featureSpec);

            String[] savedFeatureSpecs = FeatureUtils.splitFeatureSpec(featureSpec);
            String savedSentFeatureSpec = savedFeatureSpecs[0];
            String savedDocFeatureSpec = (savedFeatureSpecs.length == 2) ? savedFeatureSpecs[1] : "";

            String currentSentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.sentence.spec");
            String currentDocFeatureSpepc = config.get("edu.cmu.cs.lti.features.type.lv1.doc.spec");

            warning(savedSentFeatureSpec, currentSentFeatureSpec);
            warning(savedDocFeatureSpec, currentDocFeatureSpepc);

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
        decoder = new ViterbiDecoder(alphabet, classAlphabet);
    }

    private void warning(String savedSpec, String currentSpec) {
        if (savedSpec == null) {
            logger.warn("The model did not save any feature specifications!");
            return;
        }

        if (currentSpec == null) {
            logger.warn("The configuration did not provide any feature specifications!");
            return;
        }

        if (!currentSpec.equals(savedSpec)) {
            logger.warn("Current feature specification is not the same with the trained model.");
            logger.warn("Will use the stored specification, it might create unexpected errors: ");
            logger.warn("Saved spec will be used:");
            logger.warn(savedSpec);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger, true);
        sentenceExtractor.initWorkspace(aJCas);

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentenceExtractor.resetWorkspace(aJCas, sentence.getBegin(), sentence.getEnd());

            logger.debug(sentence.getCoveredText());

            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            // Extract features for this sentence and send in.
            decoder.decode(sentenceExtractor, weightVector, tokens.size(), lagrangian, lagrangian, true);

//            debug();

            SequenceSolution prediction = decoder.getDecodedPrediction();

            double[] probs = prediction.getSoftMaxLabelProbs();

            logger.debug(prediction.toString());

            List<Triplet<Span, String, Double>> mentionChunks = convertTypeTagsToChunks(prediction, probs);

            for (Triplet<Span, String, Double> chunk : mentionChunks) {
                StanfordCorenlpToken firstToken = tokens.get(chunk.getValue0().getBegin());
                StanfordCorenlpToken lastToken = tokens.get(chunk.getValue0().getEnd());

                EventMention predictedMention = new EventMention(aJCas);
                predictedMention.setEventType(chunk.getValue1());
                predictedMention.setEventTypeConfidence(chunk.getValue2());

                UimaAnnotationUtils.finishAnnotation(predictedMention, firstToken.getBegin(), lastToken
                        .getEnd(), COMPONENT_ID, 0, aJCas);

//                logger.info("Adding event mention: " + predictedMention.getCoveredText() + " of type " +
//                        predictedMention.getEventType());
            }
        }
    }

    private void debug() {
        SequenceSolution prediction = decoder.getDecodedPrediction();

        FeatureVector[] bestFvAtEachIndex = decoder.getBestVectorAtEachIndex();

        logger.info(prediction.toString());

        for (int i = 0; i < prediction.getSequenceLength(); i++) {
            int tag = prediction.getClassAt(i);
            if (tag != classAlphabet.getNoneOfTheAboveClassIndex()) {
                logger.info(String.format("Predicting %s because of following features.", classAlphabet.getClassName
                        (tag)));
                logger.info(bestFvAtEachIndex[i].readableString());
            }
        }

        DebugUtils.pause();
    }

    private List<Triplet<Span, String, Double>> convertTypeTagsToChunks(SequenceSolution solution, double[] probs) {
        List<Triplet<Span, String, Double>> mentionChunks = new ArrayList<>();

        int begin = -1;
        int end = -1;
        String previousType = null;
        double prob = 1;

        for (int i = 0; i < solution.getSequenceLength(); i++) {
            int tag = solution.getClassAt(i);
            if (tag != classAlphabet.getNoneOfTheAboveClassIndex()) {
                String currentType = classAlphabet.getClassName(tag);
                if (previousType != null) {
                    if (end == i - 1 && previousType.equals(currentType)) {
                        // Update endpoint.
                        end = i;
                    } else {
                        // If not adjacent to previous chunks.
                        mentionChunks.add(Triplet.with(Span.of(begin, end), previousType, prob));
                        previousType = null;
                    }
                } else {
                    previousType = currentType;
                    begin = i;
                    end = i;
                    prob *= probs[i];
                }
            }
        }

        // Add the last found mention.
        if (previousType != null) {
            mentionChunks.add(Triplet.with(Span.of(begin, end), previousType, prob));
        }

        return mentionChunks;
    }
}
