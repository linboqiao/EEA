package edu.cmu.cs.lti.event_coref.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.emd.annotators.crf.MentionSequenceCrfTrainer;
import edu.cmu.cs.lti.emd.utils.MentionTypeUtils;
import edu.cmu.cs.lti.event_coref.decoding.DDLatentTreeCrfDecoder;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionNode;
import edu.cmu.cs.lti.event_coref.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.event_coref.train.PaLatentTreeTrainer;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.extractor.MultiSentenceFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.feature.sequence.FeatureUtils;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.SequenceSolution;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.DebugUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/24/15
 * Time: 3:48 PM
 *
 * @author Zhengzhong Liu
 */
public class DDEventTypeCorefAnnotator extends AbstractLoggingAnnotator {
    public static final String PARAM_CONFIG_PATH = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    public static final String PARAM_MENTION_MODEL_DIRECTORY = "mentionModelDirectory";
    @ConfigurationParameter(name = PARAM_MENTION_MODEL_DIRECTORY)
    File mentionModelDir;

    public static final String PARAM_COREF_MODEL_DIRECTORY = "corefModelDirectory";
    @ConfigurationParameter(name = PARAM_COREF_MODEL_DIRECTORY)
    File corefModelDir;

    public static final String PARAM_COREF_RULE_FILE = "corefRuleFile";
    @ConfigurationParameter(name = PARAM_COREF_RULE_FILE)
    File corefRuleFile;

    FeatureAlphabet mentionFeatureAlphabet;
    ClassAlphabet mentionClassAlphabet;

    DDLatentTreeCrfDecoder decoder;

    MultiSentenceFeatureExtractor<EventMention> mentionFeatureExtractor;
    PairFeatureExtractor corefFeatureExtractor;

    GraphWeightVector crfWeights;
    GraphWeightVector corefWeights;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Initialize joint type + coreference annotator.");
        loadMentionModel();
        loadCorefModel();

        ArrayListMultimap<Integer, Integer> allowedCorefs = null;

        try {
            allowedCorefs = MentionTypeUtils.findAllowedCorefTypes(crfWeights
                    .getClassAlphabet(), FileUtils.readLines(corefRuleFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        decoder = new DDLatentTreeCrfDecoder(mentionFeatureAlphabet, mentionClassAlphabet, allowedCorefs);
    }

    private void loadMentionModel() throws ResourceInitializationException {
        logger.info("Loading mention model from " + mentionModelDir);
        String savedDocFeatureSpec;
        String savedSentFeatureSpec;
        try {
            crfWeights = SerializationUtils.deserialize(new FileInputStream(new File
                    (mentionModelDir, MentionSequenceCrfTrainer.MODEL_NAME)));
            mentionFeatureAlphabet = crfWeights.getFeatureAlphabet();
            mentionClassAlphabet = crfWeights.getClassAlphabet();
            String[] featureSpec = FeatureUtils.splitFeatureSpec(crfWeights.getFeatureSpec());
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

            String currentDocFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv2.doc.spec");
            String currentSentFeatureSpec = config.get("edu.cmu.cs.lti.features.type.lv2.sentence.spec");

            logger.info("Document feature spec : " + currentDocFeatureSpec);
            logger.info("Sentence feature spec : " + currentSentFeatureSpec);

            boolean useStateFeature = config.getBoolean("edu.cmu.cs.lti.mention.use_state", true);

            specWarning(savedSentFeatureSpec, currentSentFeatureSpec);
            specWarning(savedDocFeatureSpec, currentDocFeatureSpec);

            Configuration sentFeatureConfig = sentFeatureSpecParser.parseFeatureFunctionSpecs(savedSentFeatureSpec);
            Configuration mentionFeatureConfig =
                    docFeatureSpecParser.parseFeatureFunctionSpecs(savedDocFeatureSpec);

            logger.info("Mention feature alphabet size " + mentionFeatureAlphabet.getAlphabetSize());

            mentionFeatureExtractor = new MultiSentenceFeatureExtractor<>(mentionFeatureAlphabet, config,
                    sentFeatureConfig, mentionFeatureConfig, useStateFeature, EventMention.class);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void loadCorefModel() throws ResourceInitializationException {
        logger.info("Loading coreference model from " + corefModelDir);
        String featureSpec;
        FeatureAlphabet corefFeatureAlphabet;
        ClassAlphabet corefClassAlphabet;

        boolean useBinaryFeatures = config.getBoolean("edu.cmu.cs.lti.coref.binaryFeature", false);

        try {
            corefWeights = SerializationUtils.deserialize(new FileInputStream(new File(corefModelDir,
                    PaLatentTreeTrainer.MODEL_NAME)));
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

            corefFeatureExtractor = new PairFeatureExtractor(corefFeatureAlphabet, corefClassAlphabet,
                    useBinaryFeatures, config, featureConfig);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
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

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        UimaConvenience.printProcessLog(aJCas, logger);

        mentionFeatureExtractor.initWorkspace(aJCas);
        corefFeatureExtractor.initWorkspace(aJCas);

        DocumentAnnotation document = JCasUtil.selectSingle(aJCas, DocumentAnnotation.class);
        mentionFeatureExtractor.resetWorkspace(aJCas, document.getBegin(), document.getEnd());

        List<EventMention> allMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));

        MentionGraph mentionGraph = new MentionGraph(allMentions.size(), true);

        Pair<SequenceSolution, MentionSubGraph> decodeResult = decoder.decode(mentionFeatureExtractor,
                crfWeights, mentionGraph, allMentions, corefFeatureExtractor, corefWeights, true);

        annotatePredictedTypes(decodeResult.getLeft(), allMentions);
        annotatePredictedCoreference(aJCas, mentionGraph, decodeResult.getRight(), allMentions);

        DebugUtils.pause(logger);
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        logger.info(String.format("Optimal decoding %d / %d, among which %d start as optimal",
                decoder.getOptimalCounter(), decoder.getDecodingCounter(), decoder.getStartWithOptimal()));
    }

    // TODO separate clusters merged by joint types.
    private void annotatePredictedCoreference(JCas aJCas, MentionGraph mentionGraph, MentionSubGraph predictedTree,
                                              List<EventMention> allMentions) {
        predictedTree.resolveTree();
        int[][] corefChains = predictedTree.getCorefChains();

        for (int[] corefChain : corefChains) {
            List<EventMention> predictedChain = new ArrayList<>();
            Map<Span, EventMention> span2Mentions = new HashMap<>();

            for (int nodeId : corefChain) {
                MentionNode node = mentionGraph.getNode(nodeId);
                EventMention mention = allMentions.get(node.getMentionIndex());
                Span mentionSpan = Span.of(mention.getBegin(), mention.getEnd());
                if (!span2Mentions.containsKey(mentionSpan)) {
                    span2Mentions.put(Span.of(mention.getBegin(), mention.getEnd()), mention);
                    predictedChain.add(mention);
                }
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
    }

    private void annotatePredictedTypes(SequenceSolution prediction, Collection<EventMention> predictedMentions) {
        int index = 0;
        for (EventMention mention : predictedMentions) {
            String className = mentionClassAlphabet.getClassName(prediction.getClassAt(index));
            mention.setEventType(className);
            index++;
            logger.debug(String.format("Predicted type for %s is %s : %d", mention.getCoveredText(), className,
                    prediction.getClassAt(index)));
        }
    }
}
