package edu.cmu.cs.lti.emd.annotators.train;

import com.google.common.collect.HashBasedTable;
import edu.cmu.cs.lti.learning.annotators.AbstractCrfTrainer;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.MultiSentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.GraphFeatureVector;
import edu.cmu.cs.lti.learning.model.SequenceSolution;
import edu.cmu.cs.lti.learning.train.AveragePerceptronTrainer;
import edu.cmu.cs.lti.learning.utils.CubicLagrangian;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MultiKeyDiskCacher;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * This is the second layer of the CRF model, that model directly on sequence of mentions, which try to capture the
 * dependencies among mentions sequences.
 *
 * @author Zhengzhong Liu
 */
public class MentionLevelEventMentionCrfTrainer extends AbstractCrfTrainer {
    public static final String MODEL_NAME = "mentionSequenceModel";

    public static final String PARAM_LOSS_TYPE = "lossType";

    @ConfigurationParameter(name = PARAM_LOSS_TYPE)
    private String lossType;

    private static MultiKeyDiskCacher<TIntObjectHashMap<Pair<FeatureVector, HashBasedTable<Integer, Integer,
            FeatureVector>>>> featureCacher;

    private static MultiKeyDiskCacher<Pair<GraphFeatureVector, SequenceSolution>> goldCacher;

    private CubicLagrangian dummayLagrangian = new DummyCubicLagrangian();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Preparing the mention level type trainer ...");

        boolean discardAfter = config.getBoolean("edu.cmu.cs.lti.coref.mention.cache.discard_after", true);
        long weightLimit = config.getLong("edu.cmu.cs.lti.mention.cache.document.num", 1000);

        try {
            featureCacher = createFeatureCacher(weightLimit, discardAfter);
            goldCacher = createGoldCacher(weightLimit, discardAfter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String sentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv2.sentence.spec");
        String docFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv2.doc.spec");

        Configuration sentFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.sentence.package.name")
        ).parseFeatureFunctionSpecs(sentFeatureSpec);

        Configuration docFeatureConfig = new FeatureSpecParser(
                config.get("edu.cmu.cs.lti.feature.document.package.name")
        ).parseFeatureFunctionSpecs(docFeatureSpec);

        boolean useStateFeature = config.getBoolean("edu.cmu.cs.lti.mention.use_state", true);

        try {
            featureExtractor = new MultiSentenceFeatureExtractor<>(featureAlphabet, config, sentFeatureConfig,
                    docFeatureConfig, useStateFeature, EventMention.class);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                IllegalAccessException e) {
            e.printStackTrace();
        }

        trainer = new AveragePerceptronTrainer(decoder, classAlphabet, featureAlphabet,
                FeatureUtils.joinFeatureSpec(sentFeatureSpec, docFeatureSpec), false, lossType);

        logger.info("Training with the following specification: ");
        logger.info("[Sentence Spec]" + sentFeatureSpec);
        logger.info("[Document Spec]" + docFeatureSpec);
    }


    @Override
    protected ClassAlphabet initClassAlphabet(String[] classes) {
        return new ClassAlphabet(classes, false, true);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger, true);

        featureExtractor.initWorkspace(aJCas);

        DocumentAnnotation document = JCasUtil.selectSingle(aJCas, DocumentAnnotation.class);

        featureExtractor.resetWorkspace(aJCas, document.getBegin(), document.getEnd());

        String documentKey = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();

        TIntObjectHashMap<Pair<FeatureVector, HashBasedTable<Integer, Integer, FeatureVector>>> sequenceFeatures =
                featureCacher.get(documentKey);
        Pair<GraphFeatureVector, SequenceSolution> cachedGold = goldCacher.get(documentKey);

        SequenceSolution goldSolution;
        GraphFeatureVector goldFv;

        boolean newGold = false;
        if (cachedGold != null) {
            goldFv = cachedGold.getLeft();
            goldSolution = cachedGold.getRight();
        } else {
            goldSolution = getGoldSequence(aJCas);
            goldFv = decoder.getSolutionFeatures(featureExtractor, goldSolution);
            cachedGold = Pair.of(goldFv, goldSolution);
            newGold = true;
        }


        boolean newSequenceFeatures = false;
        if (sequenceFeatures == null) {
            newSequenceFeatures = true;
            sequenceFeatures = new TIntObjectHashMap<>();
        }

        double loss = trainer.trainNext(goldSolution, goldFv, featureExtractor, dummayLagrangian, dummayLagrangian,
                sequenceFeatures);
        trainingStats.addLoss(logger, loss);

        if (newSequenceFeatures) {
            featureCacher.addWithMultiKey(sequenceFeatures, documentKey);
        }

        if (newGold) {
            goldCacher.addWithMultiKey(cachedGold, documentKey);
        }
    }

    private SequenceSolution getGoldSequence(JCas aJCas) {
        Collection<EventMention> mentions = JCasUtil.select(aJCas, EventMention.class);

        int[] goldSequence = new int[mentions.size()];

        int sequenceIdx = 0;

        for (EventMention mention : mentions) {
            goldSequence[sequenceIdx] = classAlphabet.getClassIndex(mention.getEventType());
            sequenceIdx++;
        }

        return new SequenceSolution(classAlphabet, goldSequence);
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
