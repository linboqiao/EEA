package edu.cmu.cs.lti.after.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.event_coref.decoding.BFAfterLatentTreeDecoder;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.LabelledMentionGraphEdge;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.utils.LearningUtils;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MentionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

import static edu.cmu.cs.lti.learning.model.ModelConstants.AFTER_MODEL_NAME;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/12/16
 * Time: 3:49 PM
 *
 * @author Zhengzhong Liu
 */
public class LatentTreeAfterAnnotator extends AbstractLoggingAnnotator {
    private PairFeatureExtractor extractor;

    private BFAfterLatentTreeDecoder decoder;

    private GraphWeightVector weights;

    public static final String PARAM_MODEL_DIRECTORY = "modelDirectory";
    @ConfigurationParameter(name = PARAM_MODEL_DIRECTORY)
    private File modelDirectory;

    public static final String PARAM_CONFIG = "configuration";
    @ConfigurationParameter(name = PARAM_CONFIG)
    private Configuration config;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Latent Tree After Annotator initializing.");

        String featureSpecKey = "edu.cmu.cs.lti.features.after.spec";

        try {
            File modelFile = new File(modelDirectory, AFTER_MODEL_NAME);
            logger.info("Loading weights from " + modelFile);
            weights = SerializationUtils.deserialize(new FileInputStream(modelFile));
            specWarning(weights.getFeatureSpec(), config.get(featureSpecKey));
            extractor = LearningUtils.initializeMentionPairExtractor(config, featureSpecKey, weights
                    .getFeatureAlphabet());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        decoder = new BFAfterLatentTreeDecoder();
        logger.info("After link decoder initialized.");

//        BFAfterLatentTreeDecoder.startDebug();
    }

    private void specWarning(String oldSpec, String newSpec) {
        if (!oldSpec.equals(newSpec)) {
            logger.warn("Current feature specification is not the same with the trained model.");
            logger.warn("Will use the stored specification, it might create unexpected errors");
            logger.warn("New spec is: " + newSpec);
            logger.warn("Using Spec:" + oldSpec);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        UimaConvenience.printProcessLog(aJCas, logger);

        if (UimaConvenience.getDocId(aJCas).contains("bolt-eng-DF-170-181125-9125545")) {
            BFAfterLatentTreeDecoder.startDebug();
        } else {
            BFAfterLatentTreeDecoder.stopDebug();
        }

        extractor.initWorkspace(aJCas);
        List<MentionCandidate> candidates = MentionUtils.getSpanBasedCandidates(aJCas);

        List<EventMention> allMentions = MentionUtils.clearDuplicates(
                new ArrayList<>(JCasUtil.select(aJCas, EventMention.class))
        );

        Map<Span, List<EventMention>> groupedMentions = groupMentions(allMentions);

        MentionGraph mentionGraph = MentionUtils.createSpanBasedMentionGraph(aJCas, candidates, extractor, false);

        MentionSubGraph predictedTree = decoder.decode(mentionGraph, candidates, weights, false);

        for (Map.Entry<LabelledMentionGraphEdge, EdgeType> decodedEdge : predictedTree
                .getDecodedEdges().entrySet()) {
            if (decodedEdge.getKey().isRoot()){
                continue;
            }

            NodeKey fromKey = decodedEdge.getKey().getGovKey();
            NodeKey toKey = decodedEdge.getKey().getDepKey();
            EdgeType type = decodedEdge.getValue();

            EventMention fromMention = groupedMentions.get(Span.of(fromKey.getBegin(), fromKey.getEnd())).get
                    (fromKey.getKeyIndex());

            EventMention toMention = groupedMentions.get(Span.of(toKey.getBegin(), toKey.getEnd())).get(toKey
                    .getKeyIndex());
            EventMentionRelation relation = new EventMentionRelation(aJCas);
            relation.setRelationType(type.name());
            relation.setHead(fromMention);
            relation.setChild(toMention);
            UimaAnnotationUtils.finishTop(relation, COMPONENT_ID, 0, aJCas);
        }
    }

    private Map<Span, List<EventMention>> groupMentions(List<EventMention> allMentions) {
        ArrayListMultimap<Span, EventMention> groupedMentions = ArrayListMultimap.create();
        for (EventMention mention : allMentions) {
            groupedMentions.put(Span.of(mention.getBegin(), mention.getEnd()), mention);
        }

        Map<Span, List<EventMention>> sortedGroups = new HashMap<>();

        for (Map.Entry<Span, Collection<EventMention>> mentionGroup : groupedMentions.asMap().entrySet()) {
            ArrayList<EventMention> mentions = new ArrayList<>(mentionGroup.getValue());
            mentions.sort(Comparator.comparing(EventMention::getEventType));
            sortedGroups.put(mentionGroup.getKey(), mentions);
        }

        return sortedGroups;
    }
}
