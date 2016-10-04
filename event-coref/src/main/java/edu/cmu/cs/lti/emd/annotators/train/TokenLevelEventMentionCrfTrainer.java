package edu.cmu.cs.lti.emd.annotators.train;

import com.google.common.collect.HashBasedTable;
import edu.cmu.cs.lti.learning.annotators.AbstractCrfTrainer;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.GraphFeatureVector;
import edu.cmu.cs.lti.learning.model.SequenceSolution;
import edu.cmu.cs.lti.learning.utils.CubicLagrangian;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.DebugUtils;
import edu.cmu.cs.lti.utils.MultiKeyDiskCacher;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 3:56 PM
 *
 * @author Zhengzhong Liu
 */
public class TokenLevelEventMentionCrfTrainer extends AbstractCrfTrainer {
    public static final String MODEL_NAME = "crfModel";

    private static MultiKeyDiskCacher<ArrayList<TIntObjectMap<
            Pair<FeatureVector, HashBasedTable<Integer, Integer, FeatureVector>>>>> featureCacher;

    private static MultiKeyDiskCacher<ArrayList<Pair<GraphFeatureVector, SequenceSolution>>> goldCacher;

    private CubicLagrangian dummyLagrangian = new DummyCubicLagrangian();

    public static boolean TOGGLE_CHECK_UPDATE = false;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        logger.info("Preparing the token level type trainer ...");
        super.initialize(aContext);

        logger.info("Using PA update : " + usePaUpdate);

        boolean discardAfter = config.getBoolean("edu.cmu.cs.lti.coref.mention.cache.discard_after", true);
        long weightLimit = config.getLong("edu.cmu.cs.lti.mention.cache.document.num", 1000);

        try {
            featureCacher = createFeatureCacher(weightLimit, discardAfter);
            goldCacher = createGoldCacher(weightLimit, discardAfter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected ClassAlphabet initClassAlphabet(String[] classes) {
        return new ClassAlphabet(classes, true, true);
    }

    protected void parseFeatureSpec() {
        String sentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.sentence.spec");
        String docFeatureSpec = config.getOrElse("edu.cmu.cs.lti.features.type.lv1.doc.spec", "");
        featureSpec = FeatureUtils.joinFeatureSpec(sentFeatureSpec, docFeatureSpec);

        Configuration sentFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.sentence.package.name")
        ).parseFeatureFunctionSpecs(sentFeatureSpec);

        Configuration docFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.document.package.name")
        ).parseFeatureFunctionSpecs(docFeatureSpec);

        try {
            featureExtractor = new SentenceFeatureExtractor(featureAlphabet, config, sentFeatureConfig,
                    docFeatureConfig, false);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }

        logger.info("Training with the following specification: ");
        logger.info("[Sentence Spec]" + sentFeatureSpec);
        logger.info("[Document Spec]" + docFeatureSpec);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        if (TOGGLE_CHECK_UPDATE) {
            UimaConvenience.printProcessLog(aJCas, logger);
        }
        featureExtractor.initWorkspace(aJCas);

        String documentKey = UimaConvenience.getShortDocumentNameWithOffset(aJCas);

        ArrayList<TIntObjectMap<Pair<FeatureVector, HashBasedTable<Integer, Integer, FeatureVector>>>>
                documentCacheFeatures = featureCacher.get(documentKey);
        ArrayList<TIntObjectMap<Pair<FeatureVector, HashBasedTable<Integer, Integer, FeatureVector>>>>
                featuresToCache = new ArrayList<>();

        List<Pair<GraphFeatureVector, SequenceSolution>> cachedGold = goldCacher.get(documentKey);
        ArrayList<Pair<GraphFeatureVector, SequenceSolution>> goldToCache = new ArrayList<>();

        Set<String> watchingTypes = new HashSet<>();
        watchingTypes.add("transaction_transfermoney");

        int sentenceId = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            featureExtractor.resetWorkspace(aJCas, sentence);

            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            if (logger.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for (StanfordCorenlpToken token : tokens) {
                    sb.append(i++).append(":").append(token.getCoveredText()).append(" ");
                }
                logger.debug(sb.toString());
            }

            Map<StanfordCorenlpToken, String> tokenTypes = MentionTypeUtils.getTokenTypes(aJCas, sentence);

            if (ignoreUnannotatedSentence && tokenTypes.size() == 0) {
                continue;
            }

            SequenceSolution goldSolution;
            GraphFeatureVector goldFv;

            if (cachedGold != null) {
                Pair<GraphFeatureVector, SequenceSolution> goldSolutionAndFeatures = cachedGold.get(sentenceId);
                goldFv = goldSolutionAndFeatures.getLeft();
                goldSolution = goldSolutionAndFeatures.getRight();
            } else {
                goldSolution = getGoldSequence(sentence, tokenTypes);
                goldFv = decoder.getSolutionFeatures(featureExtractor, goldSolution);
                goldToCache.add(Pair.of(goldFv, goldSolution));
            }

            TIntObjectMap<Pair<FeatureVector, HashBasedTable<Integer, Integer, FeatureVector>>> cachedSentFeatures;
            if (documentCacheFeatures != null) {
                cachedSentFeatures = documentCacheFeatures.get(sentenceId);
            } else {
                cachedSentFeatures = new TIntObjectHashMap<>();
            }

            decoder.decode(featureExtractor, weightVector, goldSolution.getSequenceLength(), dummyLagrangian,
                    dummyLagrangian, cachedSentFeatures);
            SequenceSolution prediction = decoder.getDecodedPrediction();

            boolean foundError = false;
            if (TOGGLE_CHECK_UPDATE) {
                foundError = checkUpdateReason(aJCas, goldSolution, prediction, watchingTypes,
                        decoder.getBestVectorAtEachIndex(), tokens, false);
            }

            double loss = trainNext(goldSolution, prediction, goldFv);

            if (TOGGLE_CHECK_UPDATE) {
                checkUpdateReason(aJCas, goldSolution, prediction, watchingTypes,
                        decoder.getBestVectorAtEachIndex(), tokens, true);
            }

            if (foundError) {
                DebugUtils.pause();
            }

            trainingStats.addLoss(logger, loss);

            if (documentCacheFeatures == null) {
                featuresToCache.add(cachedSentFeatures);
            }

            sentenceId++;
        }

        if (documentCacheFeatures == null) {
            featureCacher.addWithMultiKey(featuresToCache, documentKey);
        }

        if (cachedGold == null) {
            goldCacher.addWithMultiKey(goldToCache, documentKey);
        }
    }

    private SequenceSolution getGoldSequence(StanfordCorenlpSentence sentence, Map<StanfordCorenlpToken, String>
            tokenTypes) {
        List<StanfordCorenlpToken> sentenceTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

        int sequenceLength = sentenceTokens.size();

        // Fill the gold sequence.
        int[] goldSequence = new int[sequenceLength];

        int seqIndex = 0;
        for (StanfordCorenlpToken token : sentenceTokens) {
            if (tokenTypes.containsKey(token)) {
                goldSequence[seqIndex] = classAlphabet.getClassIndex(tokenTypes.get(token));
            } else {
                goldSequence[seqIndex] = classAlphabet.getNoneOfTheAboveClassIndex();
            }
            seqIndex++;
        }

        return new SequenceSolution(decoder.getClassAlphabet(), goldSequence);
    }


    private boolean checkUpdateReason(JCas aJCas, SequenceSolution goldSolution, SequenceSolution prediction,
                                      Set<String> targetTypes, FeatureVector[] bestFvAtEachIndex,
                                      List<StanfordCorenlpToken> tokens, boolean afterUpdate) {
        boolean foundDifferences = false;
        boolean precisionError = false;
        for (int i = 0; i < prediction.getSequenceLength(); i++) {
            int tag = prediction.getClassAt(i);
            String predictedType = classAlphabet.getClassName(tag);

            int goldTag = goldSolution.getClassAt(i);
            String goldType = classAlphabet.getClassName(goldTag);

            StanfordCorenlpToken token = tokens.get(i);

            if (tag == classAlphabet.getNoneOfTheAboveClassIndex()) {
                // Prediction say no mention.
                if (goldTag != classAlphabet.getNoneOfTheAboveClassIndex()) {
                    if (targetTypes.contains(goldType)) {
//                        logger.info("Prediction miss this mention.");
//                        logger.info(String.format("Surface: %s, Span: [%d,%d], Gold Type: [%s], Predicted: None",
//                                token.getCoveredText(), token.getBegin(), token.getEnd(), goldType));
//                        logger.info("Features at the current index for gold class " + goldType);
//                        weightVector.dotProdAverDebug(bestFvAtEachIndex[i], goldType, logger);
//
//                        logger.info("Features at the current index for predicted class " + predictedType);
//                        weightVector.dotProdAverDebug(bestFvAtEachIndex[i], predictedType, logger);
//                        foundDifferences = true;
                    }
                }
            } else {
                if (goldTag == classAlphabet.getNoneOfTheAboveClassIndex()) {
                    // Prediction say mention but gold say no.
                    if (targetTypes.contains(predictedType)) {
                        logger.info("Prediction invent this mention.");
                        logger.info(String.format("Surface: %s, Span: [%d,%d], Gold Type: None, Predicted: [%s]",
                                token.getCoveredText(), token.getBegin(), token.getEnd(), predictedType));
                        logger.info("Features at the current index for gold class " + goldType);
                        weightVector.dotProdAverDebug(bestFvAtEachIndex[i], goldType, logger);

                        logger.info("Features at the current index for predicted class " + predictedType);
                        weightVector.dotProdAverDebug(bestFvAtEachIndex[i], predictedType, logger);
                        foundDifferences = true;
                        precisionError = true;
                    }
                } else {
                    if (tag != goldTag) {
                        if (targetTypes.contains(predictedType) || targetTypes.contains(goldType)) {
                            logger.info("Prediction get the type wrong.");
                            logger.info(String.format("Surface: %s, Span: [%d,%d], Gold Type: [%s], Predicted: [%s]",
                                    token.getCoveredText(), token.getBegin(), token.getEnd(), goldType, predictedType));
                            logger.info("Features at the current index for gold class " + goldType);
                            weightVector.dotProdAverDebug(bestFvAtEachIndex[i], goldType, logger);

                            logger.info("Features at the current index for predicted class " + predictedType);
                            weightVector.dotProdAverDebug(bestFvAtEachIndex[i], predictedType, logger);
                            foundDifferences = true;
                            precisionError = true;
                        }
                    }
                }
            }
        }

        if (precisionError) {
            if (afterUpdate) {
                logger.info("Just checked updated results.");
            } else {
                logger.info("Just checked update reasons.");
            }
            logger.info("Final prediction is :");
            logger.info(prediction.toString());
        }

        return precisionError;
    }


    /**
     * At loop finish, do some clean up work.
     *
     * @throws IOException
     */
    public static void loopStopActions() throws IOException {
        featureCacher.close();
        goldCacher.close();
    }

}
