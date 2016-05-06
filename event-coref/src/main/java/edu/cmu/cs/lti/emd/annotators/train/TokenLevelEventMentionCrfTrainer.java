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
import edu.cmu.cs.lti.learning.training.AveragePerceptronTrainer;
import edu.cmu.cs.lti.learning.utils.CubicLagrangian;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MultiKeyDiskCacher;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/22/15
 * Time: 3:56 PM
 *
 * @author Zhengzhong Liu
 */
public class TokenLevelEventMentionCrfTrainer extends AbstractCrfTrainer {
    //TODO rename this class and model name.
    public static final String MODEL_NAME = "crfModel";

    public static final String PARAM_USE_PA_UPDATE = "usePaUpdate";

    public static final String PARAM_LOSS_TYPE = "lossType";

    @ConfigurationParameter(name = PARAM_USE_PA_UPDATE)
    private boolean usePaUpdate;

    @ConfigurationParameter(name = PARAM_LOSS_TYPE)
    private String lossType;

    protected static MultiKeyDiskCacher<ArrayList<TIntObjectMap<
            Pair<FeatureVector, HashBasedTable<Integer, Integer, FeatureVector>>>>> featureCacher;

    protected static MultiKeyDiskCacher<ArrayList<Pair<GraphFeatureVector, SequenceSolution>>> goldCacher;

    private CubicLagrangian dummyLagrangian = new DummyCubicLagrangian();

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

        String sentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv1.sentence.spec");
        String docFeatureSpec = config.getOrElse("edu.cmu.cs.lti.features.type.lv1.doc.spec", "");

        Configuration sentFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.sentence.package.name")
        ).parseFeatureFunctionSpecs(sentFeatureSpec);


        Configuration docFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.document.package.name")
        ).parseFeatureFunctionSpecs(docFeatureSpec);

        try {
            featureExtractor = new SentenceFeatureExtractor(featureAlphabet, config, sentFeatureConfig,
                    docFeatureConfig,
                    false);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }

        trainer = new AveragePerceptronTrainer(decoder, classAlphabet, featureAlphabet,
                FeatureUtils.joinFeatureSpec(sentFeatureSpec, docFeatureSpec), usePaUpdate, lossType);

        logger.info("Training with the following specification: ");
        logger.info("[Sentence Spec]" + sentFeatureSpec);
        logger.info("[Document Spec]" + docFeatureSpec);
    }

    @Override
    protected ClassAlphabet initClassAlphabet(String[] classes) {
        return new ClassAlphabet(classes, true, true);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        featureExtractor.initWorkspace(aJCas);

        String documentKey = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();

        ArrayList<TIntObjectMap<Pair<FeatureVector, HashBasedTable<Integer, Integer, FeatureVector>>>>
                documentCacheFeatures = featureCacher.get(documentKey);
        ArrayList<TIntObjectMap<Pair<FeatureVector, HashBasedTable<Integer, Integer, FeatureVector>>>>
                featuresToCache = new ArrayList<>();

        List<Pair<GraphFeatureVector, SequenceSolution>> cachedGold = goldCacher.get(documentKey);
        ArrayList<Pair<GraphFeatureVector, SequenceSolution>> goldToCache = new ArrayList<>();

        int sentenceId = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            featureExtractor.resetWorkspace(aJCas, sentence);

            Map<StanfordCorenlpToken, String> tokenTypes = new HashMap<>();
            List<EventMention> mentions = JCasUtil.selectCovered(aJCas, EventMention.class, sentence);
            Map<Span, String> mergedMentions = MentionTypeUtils.mergeMentionTypes(mentions);
            for (Map.Entry<Span, String> spanToType : mergedMentions.entrySet()) {
                Span span = spanToType.getKey();
                String type = spanToType.getValue();
                for (StanfordCorenlpToken token : JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, span
                        .getBegin(), span.getEnd())) {
                    tokenTypes.put(token, type);
                }
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


            TIntObjectMap<Pair<FeatureVector, HashBasedTable<Integer, Integer, FeatureVector>>> sentenceFeatures;
            if (documentCacheFeatures != null) {
                sentenceFeatures = documentCacheFeatures.get(sentenceId);
            } else {
                sentenceFeatures = new TIntObjectHashMap<>();
            }

            double loss = trainer.trainNext(goldSolution, goldFv, featureExtractor, dummyLagrangian, dummyLagrangian,
                    sentenceFeatures);

//            logger.info("Loss is " + loss);

            trainingStats.addLoss(logger, loss);
            sentenceId++;

            if (documentCacheFeatures == null) {
                featuresToCache.add(sentenceFeatures);
            }
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
