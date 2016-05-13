package edu.cmu.cs.lti.event_coref.decoding;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.decoding.LabelLinkAgenda;
import edu.cmu.cs.lti.learning.model.decoding.NodeLinkingState;
import edu.cmu.cs.lti.learning.model.decoding.StateDelta;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.LabelledMentionGraphEdge;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.update.DiscriminativeUpdater;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static edu.cmu.cs.lti.learning.model.ModelConstants.COREF_MODEL_NAME;

/**
 * Created with IntelliJ IDEA.
 * Date: 2/28/16
 * Time: 4:22 PM
 *
 * @author Zhengzhong Liu
 */
public class BeamLatentTreeDecoder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final GraphWeightVector corefWeights;
    private final PairFeatureExtractor mentionPairExtractor;

    private DiscriminativeUpdater updater;
    private final boolean isTraining;

    private TrainingStats corefTrainingStats;

    private int beamSize = 5;

    private final GraphFeatureVector dummyMentionFv;

    private final boolean delayUpdate;

    /**
     * Training constructor
     *
     * @param corefWeights          Weight vector for coreference.
     * @param interMentionExtractor Extractor for inter event features.
     * @param updater               Updater that controls the update.
     * @param delayUpdate           Whether to do delayed LaSO update.
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public BeamLatentTreeDecoder(GraphWeightVector corefWeights, PairFeatureExtractor interMentionExtractor,
                                 DiscriminativeUpdater updater, boolean delayUpdate)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        this(corefWeights, interMentionExtractor, delayUpdate, true);
        this.updater = updater;
        corefTrainingStats = delayUpdate ? new TrainingStats(5, "coref") : new TrainingStats(250, "coref");

        logger.info("Starting the Beam Decoder with joint training.");
    }

    public BeamLatentTreeDecoder(GraphWeightVector corefWeights, PairFeatureExtractor mentionPairExtractor)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        this(corefWeights, mentionPairExtractor, false, false);
        logger.info("Starting the Beam Decoder with joint testing.");
    }

    private BeamLatentTreeDecoder(GraphWeightVector corefWeights, PairFeatureExtractor mentionPairExtractor,
                                  boolean delayUpdate, boolean isTraining)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {

        this.corefWeights = corefWeights;
        this.mentionPairExtractor = mentionPairExtractor;

        this.isTraining = isTraining;
        this.delayUpdate = delayUpdate;

        // The alphabet here are not so correct.
        dummyMentionFv = new GraphFeatureVector(corefWeights.getClassAlphabet(), corefWeights.getFeatureAlphabet());
    }

    /**
     * Decode method.
     *
     * @param aJCas        The JCas container.
     * @param mentionGraph The mention graph.
     * @param candidates   Candidates containing gold standard annotation.
     * @return The final decoding state.
     */
    public NodeLinkingState decode(JCas aJCas, MentionGraph mentionGraph, List<MentionCandidate> candidates) {
        // Prepare a gold agenda and a decoding agenda.
        LabelLinkAgenda goldAgenda = new LabelLinkAgenda(beamSize, candidates, mentionGraph);
        LabelLinkAgenda decodingAgenda = new LabelLinkAgenda(beamSize, candidates, mentionGraph);

        mentionPairExtractor.initWorkspace(aJCas);

        for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            decodingAgenda.prepareExpand();
            if (isTraining) {
                goldAgenda.prepareExpand();
                expandGoldLink(mentionGraph, candidates, candidateIndex, goldAgenda);
            }

            expandCorefLinks(mentionGraph, candidates, candidateIndex, decodingAgenda);

//            logger.info("Update states for decoding.");
            decodingAgenda.updateStates();

            if (isTraining) {
                // Expand for gold.
//                logger.info("Update states for gold.");
                goldAgenda.updateStates();
                // Record updates;
                updater.recordLaSOUpdate(decodingAgenda, goldAgenda);
                if (!delayUpdate) {
                    TObjectDoubleMap<String> losses = updater.update();
                    corefTrainingStats.addLoss(logger, losses.get(COREF_MODEL_NAME));
                }
            }
//            logger.info("Gold agenda.");
//
//            logger.info(goldAgenda.toString());
//
//            DebugUtils.pause();
        }

        if (isTraining) {
            // The final check matches the first item in the agendas, while the searching check only ensure containment.
            updater.recordFinalUpdate(decodingAgenda, goldAgenda);
            // Update based on cumulative errors.
            TObjectDoubleMap<String> losses = updater.update();
            corefTrainingStats.addLoss(logger, losses.get(COREF_MODEL_NAME) / mentionGraph.numNodes());
        }


        return decodingAgenda.getBeamStates().get(0);
    }

    private void expandCorefLinks(MentionGraph mentionGraph, List<MentionCandidate> candidates, int candidateIndex,
                                  LabelLinkAgenda agenda) {
        MentionCandidate candidate = candidates.get(candidateIndex);
        MultiNodeKey currNode = candidate.asKey();

        for (int ant = 0; ant < MentionGraph.getNodeIndex(candidateIndex); ant++) {
            for (NodeLinkingState nodeLinkingState : agenda.getBeamStates()) {
                MultiNodeKey antNodeKeys = nodeLinkingState.getNode(ant);
                for (NodeKey currNodeKey : currNode) {
                    for (NodeKey antNodeKey : antNodeKeys) {
                        StateDelta decision = new StateDelta(nodeLinkingState);
                        decision.addNode(currNode, dummyMentionFv, 0);

                        LabelledMentionGraphEdge edge = mentionGraph
                                .getLabelledEdge(candidates, antNodeKey, currNodeKey);

//                        logger.debug("Edge is " + edge);

                        for (Map.Entry<EdgeType, Double> labelScore : edge.getAllLabelScore(corefWeights).entrySet()) {

//                            logger.debug("Extending " + labelScore.getKey());
//                            logger.debug(edge.getFeatureVector().readableString());

                            decision.addLink(labelScore.getKey(), antNodeKey, currNodeKey, labelScore.getValue(),
                                    edge.getFeatureVector());

//                            DebugUtils.pause();
                        }
                        agenda.expand(decision);
                    }
                }
            }
        }
    }

    private void expandGoldLink(MentionGraph mentionGraph, List<MentionCandidate> candidates, int candidateIndex,
                                LabelLinkAgenda agenda) {
        MentionCandidate candidate = candidates.get(candidateIndex);
        MultiNodeKey currNode = candidate.asKey();

        int currentNodeIndex = MentionGraph.getNodeIndex(candidateIndex);

        // Expand each possible beam state.
        for (NodeLinkingState nodeLinkingState : agenda.getBeamStates()) {
            // Decide the link for each node key.
            for (NodeKey currNodeKey : currNode) {
                int numPossibleCorrectLinks = 0;
                for (int ant = 0; ant < MentionGraph.getNodeIndex(candidateIndex); ant++) {
                    Table<NodeKey, NodeKey, LabelledMentionGraphEdge> realGraphEdges = mentionGraph
                            .getMentionGraphEdge(currentNodeIndex, ant).getRealLabelledEdges();

                    Map<NodeKey, LabelledMentionGraphEdge> correctAntEdges = realGraphEdges.column(currNodeKey);
                    numPossibleCorrectLinks += correctAntEdges.size();

                    for (Map.Entry<NodeKey, LabelledMentionGraphEdge> correctAntEdge : correctAntEdges.entrySet()) {
                        NodeKey correctAnt = correctAntEdge.getKey();
                        LabelledMentionGraphEdge realGraphEdge = realGraphEdges.get(correctAnt, currNodeKey);
                        Pair<EdgeType, Double> correctLabelScore = realGraphEdge.getCorrectLabelScore(corefWeights);
                        double linkScore = correctLabelScore.getRight();

                        // Expand for each possible link.
                        StateDelta decision = new StateDelta(nodeLinkingState);
                        decision.addNode(currNode, dummyMentionFv, 0);
                        decision.addLink(correctLabelScore.getLeft(), realGraphEdge.getGovKey(), currNodeKey,
                                linkScore, realGraphEdge.getFeatureVector());
                        agenda.expand(decision);
                    }
                }

                if (numPossibleCorrectLinks == 0) {
                    throw new IllegalStateException("No possible links for for " + currNodeKey);
                }
            }
        }
    }

}
