package edu.cmu.cs.lti.learning.model.decoding;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.GraphFeatureVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
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
public class TwoLayerLabelLinkAgenda extends LabelLinkAgenda {
    protected transient final Logger logger = LoggerFactory.getLogger(getClass());

    private MinMaxPriorityQueue<NodeLinkingState> beamStates;

    private MinMaxPriorityQueue<NodeLinkingState> nextBeamStates;

    private MinMaxPriorityQueue<StateDelta> topStateDeltas;

    private List<StateDelta> stateDeltas;

    private List<MentionCandidate> candidates;

    private int mentionBeamSize;

    private int corefBeamSize;

    public TwoLayerLabelLinkAgenda(int nodeBeamSize, int corefBeamSize, List<MentionCandidate> candidates) {
        this(nodeBeamSize, corefBeamSize, candidates, null);
    }

    public TwoLayerLabelLinkAgenda(int mentionBeamSize, int corefBeamSize, List<MentionCandidate> candidates,
                                   MentionGraph mentionGraph) {
        beamStates = NodeLinkingState.getReverseHeap(mentionBeamSize);
        nextBeamStates = NodeLinkingState.getReverseHeap(mentionBeamSize);
        topStateDeltas = StateDelta.getReverseHeap(mentionBeamSize);

        this.mentionBeamSize = mentionBeamSize;
        this.corefBeamSize = corefBeamSize;
        this.candidates = candidates;
        beamStates.add(NodeLinkingState.getInitialState(mentionGraph));
    }

    public MinMaxPriorityQueue<NodeLinkingState> getBeamStates() {
        return beamStates;
    }

    public NodeLinkingState getBestBeamState() {
        return beamStates.peek();
    }

    /**
     * Take the states out in order as a list. This requires one full creation of a Queue and a list.
     *
     * @return The ordered states in a list.
     */
    public List<NodeLinkingState> getOrderedStates() {
        MinMaxPriorityQueue<NodeLinkingState> tempStates = NodeLinkingState.getReverseHeap(beamStates.size());
        tempStates.addAll(beamStates);
        List<NodeLinkingState> orderedStates = new ArrayList<>();
        while (!tempStates.isEmpty()) {
            orderedStates.add(tempStates.poll());
        }
        return orderedStates;
    }

    public void prepareExpand() {
        stateDeltas = new ArrayList<>();
    }

    public String toString() {
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

    public void expand(StateDelta delta) {
        topStateDeltas.offer(delta);
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
        while (!topStateDeltas.isEmpty()) {
            StateDelta sd = topStateDeltas.poll();
            NodeLinkingState updatedState = sd.applyUpdate(candidates);
            nextBeamStates.offer(updatedState);
        }
        beamStates = nextBeamStates;
        nextBeamStates = NodeLinkingState.getReverseHeap(mentionBeamSize);
    }

    @Override
    public boolean contains(LabelLinkAgenda anotherAgenda) {
        return false;
    }

    @Override
    public void copyFrom(LabelLinkAgenda anotherAgenda) {

    }

}
