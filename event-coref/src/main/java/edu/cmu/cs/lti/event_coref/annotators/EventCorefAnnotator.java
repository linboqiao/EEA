package edu.cmu.cs.lti.event_coref.annotators;

import com.google.common.io.Files;
import edu.cmu.cs.lti.emd.annotators.crf.MentionTypeCrfTrainer;
import edu.cmu.cs.lti.event_coref.decoding.BestFirstLatentTreeDecoder;
import edu.cmu.cs.lti.event_coref.decoding.LatentTreeDecoder;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionNode;
import edu.cmu.cs.lti.event_coref.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.event_coref.train.PaLatentTreeTrainer;
import edu.cmu.cs.lti.learning.feature.FeatureSpecParser;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static final String PARAM_USE_AVERAGE = "useAverage";
    @ConfigurationParameter(name = PARAM_USE_AVERAGE)
    boolean useAverage;

    private PairFeatureExtractor extractor;
    private LatentTreeDecoder decoder;

    // The resulting weights.
    private static GraphWeightVector weights;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        logger.info("Initialize event coreference annotator.");

        boolean useBinaryFeatures = config.getBoolean("edu.cmu.cs.lti.coref.binaryFeature", false);

        decoder = new BestFirstLatentTreeDecoder();

        String featureSpec;
        FeatureAlphabet featureAlphabet;

        ClassAlphabet classAlphabet;
        try {
            weights = SerializationUtils.deserialize(new FileInputStream(
                    new File(modelDirectory, PaLatentTreeTrainer.MODEL_NAME)));
            featureAlphabet = weights.getFeatureAlphabet();
            classAlphabet = weights.getClassAlphabet();
            featureSpec = Files.readFirstLine(new File(modelDirectory, MentionTypeCrfTrainer.FEATURE_SPEC_FILE),
                    Charset.defaultCharset());
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        try {
            logger.debug(featureSpec);
            Configuration featureConfig = new FeatureSpecParser(
                    config.get("edu.cmu.cs.lti.feature.pair.package.name")
            ).parseFeatureFunctionSpecs(featureSpec);
            extractor = new PairFeatureExtractor(featureAlphabet, classAlphabet, useBinaryFeatures, config,
                    featureConfig);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);
        List<EventMention> allMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));
        logger.info("Clustering " + allMentions.size() + " mentions.");
        extractor.initWorkspace(aJCas);
        MentionGraph mentionGraph = new MentionGraph(allMentions, useAverage);
        MentionSubGraph predictedTree = decoder.decode(mentionGraph, weights, extractor);

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
//                logger.info("Cluster size is " + predictedChain.size());
                UimaAnnotationUtils.finishTop(event, COMPONENT_ID, 0, aJCas);
                for (EventMention eventMention : predictedChain) {
                    eventMention.setReferringEvent(event);
                }
            }
        }
    }
}
