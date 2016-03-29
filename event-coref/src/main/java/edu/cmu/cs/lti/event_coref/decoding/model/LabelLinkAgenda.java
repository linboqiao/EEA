package edu.cmu.cs.lti.event_coref.decoding.model;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge.EdgeType;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.GraphFeatureVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MentionCandidate.DecodingResult;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/1/16
 * Time: 3:59 PM
 *
 * @author Zhengzhong Liu
 */
public class LabelLinkAgenda {
    protected transient final Logger logger = LoggerFactory.getLogger(getClass());

    private MinMaxPriorityQueue<NodeLinkingState> beamStates;

    private MinMaxPriorityQueue<NodeLinkingState> nextBeamStates;

    private MinMaxPriorityQueue<StateDelta> stateDeltas;

    private List<MentionCandidate> candidates;

//    // Only one set of the features is recorded: which is the features for the best scored new states.
//    // These features are delta over the previous vectors, i.e. they are not the features from starting the decoding
//    // to date, but only the new features introduced in this state update.
//    private GraphFeatureVector bestDeltaLabelFv;
//
//    private List<Pair<EdgeType, FeatureVector>> bestDeltaCorefVectors;

    private int beamSize;

    private List<List<MentionCandidate.DecodingResult>> actualDecodingSequence = new ArrayList<>();

    public LabelLinkAgenda(int beamSize, List<MentionCandidate> candidates, MentionGraph mentionGraph) {
        beamStates = NodeLinkingState.getReverseHeap(beamSize);
        nextBeamStates = NodeLinkingState.getReverseHeap(beamSize);
        this.beamSize = beamSize;
        this.candidates = candidates;

        beamStates.offer(NodeLinkingState.getInitialState(mentionGraph));
    }

    public MinMaxPriorityQueue<NodeLinkingState> getBeamStates() {
        return beamStates;
    }

    public void prepareExpand() {
        stateDeltas = StateDelta.getReverseHeap(beamSize);
    }

    public String showAgendaItems() {
        StringBuilder sb = new StringBuilder();

        sb.append("Showing beam states without order:\n");
        for (NodeLinkingState beamState : beamStates) {
            sb.append(beamState.toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    public void clearFeatures() {
        for (NodeLinkingState beamState : beamStates) {
            beamState.clearFeatures();
        }
    }


    /**
     * Record the TOP K deltas, based on the scores after the delta.
     *
     * @param expandingState  The state to be expanded.
     * @param antecedent      The antecedent to be linked to.
     * @param linkType        The linking type.
     * @param additionalScore Additional score for the expanded state.
     * @param newLabelFv      New features for the expanded state.
     * @param newCorefFv      New coref features for the expanded state.
     */
    public void expand(NodeLinkingState expandingState, List<Integer> antecedent,
                       List<EdgeType> linkType, List<DecodingResult> govKeys, List<DecodingResult> depKeys,
                       double additionalScore, GraphFeatureVector newLabelFv,
                       List<Pair<EdgeType, FeatureVector>> newCorefFv) {

        if (linkType.size() != govKeys.size() || antecedent.size() != linkType.size() ||
                govKeys.size() != antecedent.size() || govKeys.size() != depKeys.size()) {
            throw new IllegalArgumentException(String.format("Wrong state input. gov key size : %d,  dep key size : " +
                            "%d, link size : %d, antecedent size %d", govKeys.size(), depKeys.size(), linkType.size(),
                    antecedent.size()));
        }

        if (govKeys.stream().anyMatch(p -> p == null)) {
            logger.debug("Gov key contains null");
            throw new IllegalArgumentException("Null key in gov");
        }

        if (depKeys.stream().anyMatch(p -> p == null)) {
            logger.debug("Dep key contains null");
            throw new IllegalArgumentException("Null key in dep");
        }


        StateDelta delta = new StateDelta(expandingState, antecedent, linkType, govKeys, depKeys, additionalScore,
                newLabelFv, newCorefFv);
        boolean accepted = stateDeltas.offer(delta);
//        if (accepted) {
//            logger.debug("Accepted the following delta.");
//            logger.debug(delta.toString());
//            logger.debug(String.valueOf(stateDeltas.size()));
//
//            logger.debug("Remaining ...");
//            stateDeltas.forEach(d -> logger.debug(d.toString()));
//        }
    }

    public Map<EdgeType, FeatureVector> getBestDeltaCorefVectors() {
        return beamStates.peek().getCorefFv();
    }

    public GraphFeatureVector getBestDeltaLabelFv() {
        return beamStates.peek().getLabelFv();
    }

    /**
     * Update the beam states, by converting the deltas to new states, and remove the old ones.
     */
    public void updateStates() {
        // First take the best features from the delta
//        boolean isFirst = true;

//        logger.debug("Update states with " + stateDeltas.size() + " deltas.");

        while (!stateDeltas.isEmpty()) {
            StateDelta delta = stateDeltas.poll();
            NodeLinkingState updatedState = delta.applyUpdate(candidates);
            nextBeamStates.add(updatedState);
//            if (isFirst) {
//                bestDeltaLabelFv = delta.getDeltaLabelFv();
//                bestDeltaCorefVectors = delta.getDeltaGraphFv();
//
//                isFirst = false;
//
//                actualDecodingSequence.add(updatedState.getLastNode());
//
////                logger.debug("Update states with best label features");
////                logger.debug(bestDeltaLabelFv.readableNodeVector());
////                logger.debug("Update with best graph features for each antecedents.");
////
////                for (Pair<EdgeType, FeatureVector> typeFeatureVector : bestDeltaCorefVectors) {
////                    logger.debug("Edge type is " + typeFeatureVector.getValue0());
////                    logger.debug(typeFeatureVector.getValue1().readableString());
////                }
////                DebugUtils.pause(logger);
//            }
        }
        beamStates = nextBeamStates;
        nextBeamStates = NodeLinkingState.getReverseHeap(beamSize);
    }

    public void showActualSequence() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < actualDecodingSequence.size(); i++) {
            List<MentionCandidate.DecodingResult> decodingResult = actualDecodingSequence.get(i);
            String type = MentionTypeUtils.joinMultipleTypes(decodingResult.stream()
                    .map(MentionCandidate.DecodingResult::getMentionType).collect(Collectors.toSet()));
            sb.append(i).append(":").append(type).append(" ");
        }
        logger.debug(sb.toString());
        actualDecodingSequence = new ArrayList<>();
    }

    public boolean contains(LabelLinkAgenda anotherAgenda) {
        return beamStates.stream().anyMatch(s -> anotherAgenda.beamStates.stream().anyMatch(g -> s.match(g)));
    }

    public void copyFrom(LabelLinkAgenda anotherAgenda) {
        this.beamSize = anotherAgenda.beamSize;
        beamStates.clear();
        anotherAgenda.beamStates.iterator().forEachRemaining(s -> beamStates.add(s));
    }

}
