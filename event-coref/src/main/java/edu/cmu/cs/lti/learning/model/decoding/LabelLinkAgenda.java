package edu.cmu.cs.lti.learning.model.decoding;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.GraphFeatureVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MultiNodeKey;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/1/16
 * Time: 3:59 PM
 *
 * @author Zhengzhong Liu
 */
public class LabelLinkAgenda {
    protected transient final Logger logger = LoggerFactory.getLogger(getClass());

    private List<NodeLinkingState> beamStates;

    private List<NodeLinkingState> nextBeamStates;

    private  MinMaxPriorityQueue<StateDelta> stateDeltas;

    private List<MentionCandidate> candidates;

//    private StateDelta currentDelta;

    private int beamSize;

    public LabelLinkAgenda(int beamSize, List<MentionCandidate> candidates) {
        this(beamSize, candidates, null);
    }

    public LabelLinkAgenda(int beamSize, List<MentionCandidate> candidates, MentionGraph mentionGraph) {
        beamStates = new ArrayList<>(beamSize);
        nextBeamStates = new ArrayList<>(beamSize);
        this.beamSize = beamSize;
        this.candidates = candidates;
        beamStates.add(NodeLinkingState.getInitialState(mentionGraph));
    }

    public List<NodeLinkingState> getBeamStates() {
        return beamStates;
    }

    public void prepareExpand() {
        stateDeltas = StateDelta.getReverseHeap(beamSize);
    }

    public String showAgendaItems() {
        StringBuilder sb = new StringBuilder();

        sb.append("Showing beam states:\n");
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

    public StateDelta expand(NodeLinkingState expandingState, MultiNodeKey node, double nodeScore,
                             GraphFeatureVector newLabelFv) {
        StateDelta delta = new StateDelta(expandingState);
        delta.setNode(node, newLabelFv, nodeScore);

        stateDeltas.offer(delta);

        return delta;
    }

    public Map<EdgeType, FeatureVector> getBestDeltaCorefVectors() {
        return beamStates.get(0).getCorefFv();
    }

    public GraphFeatureVector getBestDeltaLabelFv() {
        return beamStates.get(0).getLabelFv();
    }

    /**
     * Update the beam states, by converting the deltas to new states, and remove the old ones.
     */
    public void updateStates() {
//        logger.debug("Update states with " + stateDeltas.size() + " deltas.");

        while (!stateDeltas.isEmpty()) {
            StateDelta delta = stateDeltas.poll();
            NodeLinkingState updatedState = delta.applyUpdate(candidates);
            nextBeamStates.add(updatedState);
        }
        beamStates = nextBeamStates;
        nextBeamStates = new ArrayList<>(beamSize);
    }


    public boolean contains(LabelLinkAgenda anotherAgenda) {
        return beamStates.stream().anyMatch(s -> anotherAgenda.beamStates.stream().anyMatch(s::match));
    }

    public void copyFrom(LabelLinkAgenda anotherAgenda) {
        this.beamSize = anotherAgenda.beamSize;
        beamStates.clear();
        anotherAgenda.beamStates.iterator().forEachRemaining(s -> beamStates.add(s));
    }

}
