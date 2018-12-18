package edu.cmu.cs.lti.emd.annotators;

import com.google.common.collect.Iterables;
import edu.cmu.cs.lti.emd.annotators.train.TokenLevelEventMentionCrfTrainer;
import edu.cmu.cs.lti.learning.decoding.SequenceDecoder;
import edu.cmu.cs.lti.learning.decoding.ViterbiDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.extractor.UimaSequenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.script.type.CharacterAnnotation;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractConfigAnnotator;
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
import java.util.*;

/**
 * We provide gold standard to this annotator, so it will know when some mention is missing or invented, we can
 * analyze why by looking at the features.
 *
 * @author Zhengzhong Liu
 */
public class TokenBasedMentionErrorAnalyzer extends AbstractConfigAnnotator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private UimaSequenceFeatureExtractor sentenceExtractor;
    private ClassAlphabet classAlphabet;
    private GraphWeightVector weightVector;
    private static SequenceDecoder decoder;

    public static final String PARAM_MODEL_DIRECTORY = "modelDirectory";
    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    private File modelDirectory;

//    public static final String PARAM_CONFIG = "configuration";
//    @ConfigurationParameter(name = PARAM_CONFIG)
//    private Configuration config;

    // A dummy lagrangian variable.
    private DummyCubicLagrangian lagrangian = new DummyCubicLagrangian();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Loading models to check for token based mention model error.");

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
            FeatureSpecParser sentFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.sentence.package.name"));

            FeatureSpecParser docFeatureSpecParser = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.document.package.name")
            );

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

    private void warning(String savedSpec, String oldSpec) {
        if (!oldSpec.equals(savedSpec)) {
            logger.warn("Current feature specification is not the same with the trained model.");
            logger.warn("Will use the stored specification, it might create unexpected errors");
            logger.warn("Using Spec:" + savedSpec);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);

        sentenceExtractor.initWorkspace(aJCas);

        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, aJCas);

        Map<CharacterAnnotation, Collection<StanfordCorenlpToken>> c2t = JCasUtil.indexCovering(aJCas,
                CharacterAnnotation.class, StanfordCorenlpToken.class);

        Map<StanfordCorenlpToken, String> goldTypes = new HashMap<>();

        for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
            List<CharacterAnnotation> mChars = JCasUtil.selectCovered(aJCas, CharacterAnnotation.class,
                    mention.getBegin(), mention.getEnd());
            for (CharacterAnnotation c : mChars) {
                goldTypes.put(Iterables.get(c2t.get(c), 0), mention.getEventType());
            }
        }

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentenceExtractor.resetWorkspace(aJCas, sentence.getBegin(), sentence.getEnd());

            logger.debug(sentence.getCoveredText());

            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            // Extract features for this sentence and send in.
            decoder.decode(sentenceExtractor, weightVector, tokens.size(), lagrangian, lagrangian, true);

            SequenceSolution prediction = decoder.getDecodedPrediction();
            FeatureVector[] bestFvAtEachIndex = decoder.getBestVectorAtEachIndex();

            boolean foundDifferences = false;
            for (int i = 0; i < prediction.getSequenceLength(); i++) {
                int tag = prediction.getClassAt(i);
                String predictedType = classAlphabet.getClassName(tag);

                StanfordCorenlpToken token = tokens.get(i);
                String goldType = goldTypes.get(token);
                if (goldType == null) {
                    goldType = ClassAlphabet.noneOfTheAboveClass;
                }

                if (tag == classAlphabet.getNoneOfTheAboveClassIndex()) {
                    // Prediction say no mention.
                    if (!goldType.equals(ClassAlphabet.noneOfTheAboveClass)) {
                        logger.info("Prediction miss this mention.");
                        logger.info(String.format("Surface: %s, Span: [%d,%d], Gold Type: [%s], Predicted: None",
                                token.getCoveredText(), token.getBegin(), token.getEnd(), goldType));
                        logger.info("Features at the current index for gold class " + goldType);
                        weightVector.dotProdAverDebug(bestFvAtEachIndex[i], goldType, logger);

                        logger.info("Features at the current index for predicted class " + predictedType);
                        weightVector.dotProdAverDebug(bestFvAtEachIndex[i], predictedType, logger);
                        foundDifferences = true;
                    }
                } else {
                    if (goldType.equals(ClassAlphabet.noneOfTheAboveClass)) {
                        // Prediction say mention but gold say no.
                        logger.info("Prediction invent this mention.");
                        logger.info(String.format("Surface: %s, Span: [%d,%d], Gold Type: None, Predicted: [%s]",
                                token.getCoveredText(), token.getBegin(), token.getEnd(), predictedType));
                        logger.info("Features at the current index for gold class " + goldType);
                        weightVector.dotProdAverDebug(bestFvAtEachIndex[i], goldType, logger);

                        logger.info("Features at the current index for predicted class " + predictedType);
                        weightVector.dotProdAverDebug(bestFvAtEachIndex[i], predictedType, logger);
                        foundDifferences = true;
                    } else {
                        if (!predictedType.equals(goldType)) {
                            logger.info("Prediction get the type wrong.");
                            logger.info(String.format("Surface: %s, Span: [%d,%d], Gold Type: [%s], Predicted: [%s]",
                                    token.getCoveredText(), token.getBegin(), token.getEnd(), goldType, predictedType));
                            logger.info("Features at the current index for gold class " + goldType);
                            weightVector.dotProdAverDebug(bestFvAtEachIndex[i], goldType, logger);

                            logger.info("Features at the current index for predicted class " + predictedType);
                            weightVector.dotProdAverDebug(bestFvAtEachIndex[i], predictedType, logger);
                            foundDifferences = true;
                        }
                    }
                }
            }

            if (foundDifferences) {
                logger.info("Final prediction is :");
                logger.info(prediction.toString());
                DebugUtils.pause();
            }
        }
    }

    private List<Triplet<Integer, Integer, String>> convertTypeTagsToChunks(SequenceSolution solution) {
        List<Triplet<Integer, Integer, String>> chunkEndPoints = new ArrayList<>();

        for (int i = 0; i < solution.getSequenceLength(); i++) {
            int tag = solution.getClassAt(i);
            if (tag != classAlphabet.getNoneOfTheAboveClassIndex()) {
                String className = classAlphabet.getClassName(tag);
                if (chunkEndPoints.size() > 0) {
                    Triplet<Integer, Integer, String> lastChunk = chunkEndPoints.get(chunkEndPoints.size() - 1);
                    if (lastChunk.getValue1() == i - 1) {
                        if (lastChunk.getValue2().equals(className)) {
                            // Update endpoint.
                            lastChunk.setAt1(i);
                            continue;
                        }
                    }
                }
                // If not adjacent to previous chunks.
                chunkEndPoints.add(Triplet.with(i, i, className));
            }
        }
        return chunkEndPoints;
    }
}
