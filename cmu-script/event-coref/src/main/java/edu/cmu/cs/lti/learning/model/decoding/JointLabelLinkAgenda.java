package edu.cmu.cs.lti.learning.model.decoding;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/1/16
 * Time: 3:59 PM
 *
 * @author Zhengzhong Liu
 */
public class JointLabelLinkAgenda extends LabelLinkAgenda {
    protected transient final Logger logger = LoggerFactory.getLogger(getClass());

    private MinMaxPriorityQueue<NodeLinkingState> beamStates;

    private MinMaxPriorityQueue<NodeLinkingState> nextBeamStates;

    private MinMaxPriorityQueue<StateDelta> topStateDeltas;

    private List<StateDelta> stateDeltas;

    private List<MentionCandidate> candidates;

    private int beamSize;

    // Let it always be true.
    private boolean fastMode = true;

    public JointLabelLinkAgenda(int beamSize, List<MentionCandidate> candidates) {
        this(beamSize, candidates, null);
    }

    public JointLabelLinkAgenda(int beamSize, List<MentionCandidate> candidates, MentionGraph mentionGraph) {
        beamStates = NodeLinkingState.getReverseHeap(beamSize);
        nextBeamStates = NodeLinkingState.getReverseHeap(beamSize);
        topStateDeltas = StateDelta.getReverseHeap(beamSize);

        this.beamSize = beamSize;
        this.candidates = candidates;
        beamStates.add(NodeLinkingState.getInitialState(mentionGraph));
    }

//    public MinMaxPriorityQueue<NodeLinkingState> getBeamStates() {
//        return beamStates;
//    }

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
        topStateDeltas = StateDelta.getReverseHeap(beamSize);
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

    public synchronized void expand(StateDelta delta) {
        if (fastMode) {
            fastExpand(delta);
        } else {
            slowExpand(delta);
        }
    }

    /**
     * Do not use this.
     *
     * @param delta
     * @deprecated
     */
    private int slowExpand(StateDelta delta) {
        stateDeltas.add(delta);
        return stateDeltas.size();
    }

    private int fastExpand(StateDelta delta) {
        topStateDeltas.offer(delta);
        return topStateDeltas.size();
    }

    /**
     * Update the beam states, by converting the deltas to new states, and remove the old ones.
     */
    public void updateStates() {
        if (fastMode) {
            fastUpdate();
        } else {
            slowUpdate();
        }
    }

    @Override
    public boolean contains(LabelLinkAgenda anotherAgenda) {
        return contains((JointLabelLinkAgenda) anotherAgenda);
    }

    @Override
    public void copyFrom(LabelLinkAgenda anotherAgenda) {
        copyFrom((JointLabelLinkAgenda) anotherAgenda);
    }

    private void slowUpdate() {
        for (StateDelta stateDelta : stateDeltas) {
            NodeLinkingState updatedState = stateDelta.applyUpdate(candidates);

//            logger.info("New state ");
//            logger.info(updatedState.toString());

            nextBeamStates.add(updatedState);
        }
        beamStates = nextBeamStates;
        nextBeamStates = NodeLinkingState.getReverseHeap(beamSize);

    }

    private void fastUpdate() {
        while (!topStateDeltas.isEmpty()) {
            StateDelta sd = topStateDeltas.poll();
            NodeLinkingState updatedState = sd.applyUpdate(candidates);
            nextBeamStates.offer(updatedState);
        }
        beamStates = nextBeamStates;
        nextBeamStates = NodeLinkingState.getReverseHeap(beamSize);
    }


    private boolean contains(JointLabelLinkAgenda anotherAgenda) {
        return beamStates.stream().anyMatch(s -> anotherAgenda.beamStates.stream().anyMatch(s::match));
    }

    private void copyFrom(JointLabelLinkAgenda anotherAgenda) {
        this.beamSize = anotherAgenda.beamSize;
        beamStates.clear();
        anotherAgenda.beamStates.iterator().forEachRemaining(s -> beamStates.add(s));
    }

    @Override
    public Iterator<NodeLinkingState> iterator() {
        return beamStates.iterator();
    }

    @Override
    public Spliterator<NodeLinkingState> spliterator() {
        return beamStates.spliterator();
    }
}
