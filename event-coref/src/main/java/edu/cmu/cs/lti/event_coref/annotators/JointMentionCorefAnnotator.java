package edu.cmu.cs.lti.event_coref.annotators;

import edu.cmu.cs.lti.emd.utils.MentionUtils;
import edu.cmu.cs.lti.event_coref.decoding.BeamCrfLatentTreeDecoder;
import edu.cmu.cs.lti.event_coref.decoding.model.NodeLinkingState;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.event_coref.annotators.train.DelayedLaSOJointTrainer;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.SentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.script.type.Event;
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
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.javatuples.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/10/16
 * Time: 10:09 PM
 *
 * @author Zhengzhong Liu
 */
public class JointMentionCorefAnnotator extends AbstractLoggingAnnotator {
    public static final String PARAM_CONFIG_PATH = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    public static final String PARAM_MODEL_DIRECTORY = "jointModeLDirectory";
    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    File jointModelDir;

    public static final String PARAM_REALIS_MODEL_DIRECTORY = "realisModelDirectory";
    @ConfigurationParameter(name = PARAM_REALIS_MODEL_DIRECTORY)
    File realisModelDirectory;

    private GraphWeightVector crfWeights;
    private GraphWeightVector corefWeights;
    private WekaModel realisModel;
    private SentenceFeatureExtractor realisExtractor;
    private PairFeatureExtractor corefExtractor;
    private SentenceFeatureExtractor mentionExtractor;
    private BeamCrfLatentTreeDecoder decoder;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Initialize Joint Span, Coreference annotator.");

        prepareMentionModel();
        prepareCorefModel();
        prepareRealis();

        try {
            decoder = new BeamCrfLatentTreeDecoder(crfWeights, realisModel,
                    corefWeights, realisExtractor, mentionExtractor, corefExtractor);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | InstantiationException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<StanfordCorenlpToken> allTokens = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpToken.class));
        List<MentionCandidate> candidates = MentionUtils.createCandidatesFromTokens(aJCas, allTokens);
        MentionGraph mentionGraph = new MentionGraph(candidates, corefExtractor, true);

        NodeLinkingState decodedState = decoder.decode(aJCas, mentionGraph, candidates, true);

        Map<Pair<Integer, String>, EventMention> node2Mention = new HashMap<>();

        List<List<NodeKey>> nodeResults = decodedState.getNodeResults();

        for (int nodeIndex = 0; nodeIndex < nodeResults.size(); nodeIndex++) {
            List<NodeKey> nodeKey = nodeResults.get(nodeIndex);

            if (MentionCandidate.isRootKey(nodeKey)) {
                continue;
            }

            for (NodeKey result : nodeKey) {
                if (!result.getMentionType().equals(ClassAlphabet.noneOfTheAboveClass)) {
                    EventMention mention = new EventMention(aJCas, result.getBegin(), result.getEnd());
                    mention.setRealisType(result.getRealis());
                    mention.setEventType(result.getMentionType());
                    UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, 0, aJCas);
                    node2Mention.put(Pair.with(nodeIndex, result.getMentionType()), mention);
                }
            }
        }

        annotatePredictedCoreference(aJCas, decodedState.getDecodingTree(), node2Mention);
    }

    private void annotatePredictedCoreference(JCas aJCas, MentionSubGraph predictedTree,
                                              Map<Pair<Integer, String>, EventMention> node2Mention) {
        predictedTree.resolveCoreference();
        List<Pair<Integer, String>>[] corefChains = predictedTree.getCorefChains();

        for (List<Pair<Integer, String>> corefChain : corefChains) {
            List<EventMention> predictedChain = new ArrayList<>();

            for (Pair<Integer, String> typedNode : corefChain) {
                EventMention mention = node2Mention.get(typedNode);
                predictedChain.add(mention);
            }

            if (predictedChain.size() > 1) {
                Event event = new Event(aJCas);
                event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, predictedChain));
                UimaAnnotationUtils.finishTop(event, COMPONENT_ID, 0, aJCas);
                for (EventMention eventMention : predictedChain) {
                    eventMention.setReferringEvent(event);
                }
            }
        }

        logger.info(String.format("Found %d clusters in document %s", corefChains.length, UimaConvenience
                .getShortDocumentName(aJCas)));
    }

    private void prepareMentionModel() throws ResourceInitializationException {
        logger.info("Loading mention model from " + jointModelDir);
        String featureSpec;
        FeatureAlphabet alphabet;
        try {
            crfWeights = SerializationUtils.deserialize(new FileInputStream(new File
                    (jointModelDir, DelayedLaSOJointTrainer.TYPE_MODEL_NAME)));
            alphabet = crfWeights.getFeatureAlphabet();
//            classAlphabet = crfWeights.getClassAlphabet();
            featureSpec = crfWeights.getFeatureSpec();
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

            specWarning(savedSentFeatureSpec, currentSentFeatureSpec);
            specWarning(savedDocFeatureSpec, currentDocFeatureSpepc);

            logger.info("Sent feature spec : " + savedSentFeatureSpec);
            logger.info("Doc feature spec : " + savedDocFeatureSpec);

            Configuration sentFeatureConfig = sentFeatureSpecParser.parseFeatureFunctionSpecs(savedSentFeatureSpec);
            Configuration docFeatureConfig = docFeatureSpecParser.parseFeatureFunctionSpecs(savedDocFeatureSpec);

            mentionExtractor = new SentenceFeatureExtractor(alphabet, config, sentFeatureConfig, docFeatureConfig,
                    false);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void prepareCorefModel() throws ResourceInitializationException {
        logger.info("Loading coreference model from " + jointModelDir);
        String featureSpec;
        FeatureAlphabet corefFeatureAlphabet;
        ClassAlphabet corefClassAlphabet;

        boolean useBinaryFeatures = config.getBoolean("edu.cmu.cs.lti.coref.binaryFeature", false);

        try {
            corefWeights = SerializationUtils.deserialize(new FileInputStream(new File(jointModelDir,
                    DelayedLaSOJointTrainer.COREF_MODEL_NAME)));
            corefFeatureAlphabet = corefWeights.getFeatureAlphabet();
            corefClassAlphabet = corefWeights.getClassAlphabet();
            featureSpec = corefWeights.getFeatureSpec();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        try {
            String currentFeatureSpec = config.get("edu.cmu.cs.lti.features.coref.spec");
            specWarning(featureSpec, currentFeatureSpec);

            Configuration featureConfig = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.pair.package.name")
            ).parseFeatureFunctionSpecs(featureSpec);

            corefExtractor = new PairFeatureExtractor(corefFeatureAlphabet, corefClassAlphabet,
                    config, featureConfig);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            throw new ResourceInitializationException(e);
        }
    }

    private void prepareRealis() throws ResourceInitializationException {
        logger.info("Loading Realis models ...");
        try {
            realisModel = new WekaModel(realisModelDirectory);
        } catch (Exception e) {
            throw new ResourceInitializationException(e);
        }

        String featurePackageName = config.get("edu.cmu.cs.lti.feature.sentence.package.name");
        String featureSpec = config.get("edu.cmu.cs.lti.features.realis.spec");

        FeatureSpecParser parser = new FeatureSpecParser(featurePackageName);
        Configuration realisSpec = parser.parseFeatureFunctionSpecs(featureSpec);

        // Currently no document level realis features.
        Configuration placeHolderSpec = new Configuration();
        try {
            realisExtractor = new SentenceFeatureExtractor(realisModel.getAlphabet(), config, realisSpec,
                    placeHolderSpec, false);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                InvocationTargetException e) {
            throw new ResourceInitializationException(e);
        }
    }


    private void specWarning(String oldSpec, String newSpec) {
        if (!oldSpec.equals(newSpec)) {
            logger.warn("Current feature specification is not the same with the trained model.");
            logger.warn("Will use the stored specification, it might create unexpected errors");
            logger.warn("Using Spec:" + oldSpec);
        }
    }
}
