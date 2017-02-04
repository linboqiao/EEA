package edu.cmu.cs.lti.event_coref.annotators;

import edu.cmu.cs.lti.event_coref.annotators.train.PaLatentTreeTrainer;
import edu.cmu.cs.lti.event_coref.decoding.BestFirstLatentTreeDecoder;
import edu.cmu.cs.lti.learning.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.utils.DummyCubicLagrangian;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MentionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/30/15
 * Time: 12:02 AM
 *
 * @author Zhengzhong Liu
 */
public class EventCorefAnnotator extends AbstractLoggingAnnotator {
    public static final String PARAM_CONFIG_PATH = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    public static final String PARAM_MODEL_DIRECTORY = "modelDirectory";
    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    File modelDirectory;

    private PairFeatureExtractor extractor;
    private LatentTreeDecoder decoder;

    // The weights.
    private GraphWeightVector weights;

    // Dummy lagrangians.
    private DummyCubicLagrangian lagrangian = new DummyCubicLagrangian();

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        logger.info("Initialize event coreference annotator.");

        decoder = new BestFirstLatentTreeDecoder();

        String featureSpec;
        FeatureAlphabet featureAlphabet;

        ClassAlphabet classAlphabet;
        try {
            logger.info("Loading coreference model from " + modelDirectory);
            weights = SerializationUtils.deserialize(new FileInputStream(
                    new File(modelDirectory, PaLatentTreeTrainer.MODEL_NAME)));
            featureAlphabet = weights.getFeatureAlphabet();
            classAlphabet = weights.getClassAlphabet();
            featureSpec = weights.getFeatureSpec();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        try {
            String currentFeatureSpec = config.get("edu.cmu.cs.lti.features.coref.spec");
            specWarning(featureSpec, currentFeatureSpec);

            Configuration featureConfig = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.pair.package.name")
            ).parseFeatureFunctionSpecs(featureSpec);
            extractor = new PairFeatureExtractor(featureAlphabet, classAlphabet, config,
                    featureConfig);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
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
        List<EventMention> allMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));
//        logger.info("Clustering " + allMentions.size() + " mentions.");

        List<MentionCandidate> candidates = MentionUtils.createCandidates(aJCas, allMentions);

//        for (MentionCandidate candidate : candidates) {
//            logger.info(candidate.toString());
//        }

        extractor.initWorkspace(aJCas);
        MentionGraph mentionGraph = new MentionGraph(candidates, extractor, true);
        MentionSubGraph predictedTree = decoder.decode(mentionGraph, candidates, weights, false);

        predictedTree.resolveCoreference();
        List<NodeKey>[] corefChains = predictedTree.getCorefChains();

//        UimaConvenience.printProcessLog(aJCas, logger);
//
//        logger.info(predictedTree.toString());

        for (List<NodeKey> corefChain : corefChains) {
            List<EventMention> predictedChain = new ArrayList<>();
//            Map<Span, EventMention> span2Mentions = new HashMap<>();

            for (NodeKey typedNode : corefChain) {
                int mentionIndex = MentionGraph.getCandidateIndex(typedNode.getNodeIndex());
                EventMention mention = allMentions.get(mentionIndex);
                predictedChain.add(mention);
//                Span mentionSpan = Span.of(mention.getBegin(), mention.getEnd());
//                if (!span2Mentions.containsKey(mentionSpan)) {
//                    span2Mentions.put(mentionSpan, mention);
//                    predictedChain.add(mention);
//                }
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

}
