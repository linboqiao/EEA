package edu.cmu.cs.lti.learning.model.decoding;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/1/16
 * Time: 3:59 PM
 *
 * @author Zhengzhong Liu
 */
public class TwoLayerLabelLinkAgenda extends LabelLinkAgenda {
    protected transient final Logger logger = LoggerFactory.getLogger(getClass());

    private List<NodeLinkingState> beamStates;

    private List<NodeLinkingState> nextBeamStates;

    private MinMaxPriorityQueue<InnerBeam> twoLayerDeltas;

//    private PriorityQueue<InnerBeam> twoLayerDeltas;

    private List<MentionCandidate> candidates;

    private int mentionBeamSize;

    private int corefBeamSize;

    public TwoLayerLabelLinkAgenda(int nodeBeamSize, int corefBeamSize, List<MentionCandidate> candidates) {
        this(nodeBeamSize, corefBeamSize, candidates, null);
    }

    public TwoLayerLabelLinkAgenda(int mentionBeamSize, int corefBeamSize, List<MentionCandidate> candidates,
                                   MentionGraph mentionGraph) {
        beamStates = new ArrayList<>();
        nextBeamStates = new ArrayList<>();

        this.mentionBeamSize = mentionBeamSize;
        this.corefBeamSize = corefBeamSize;
        this.candidates = candidates;

        twoLayerDeltas =
                MinMaxPriorityQueue.orderedBy(new Comparator<InnerBeam>() {
                    @Override
                    public int compare(InnerBeam o1, InnerBeam o2) {
                        // Note that we use a reversed comparator here.
                        return new CompareToBuilder().append(o2.baseScore, o1.baseScore).build();
                    }
                }).maximumSize(mentionBeamSize).create();

//        twoLayerDeltas = new PriorityQueue<>(mentionBeamSize);

        beamStates.add(NodeLinkingState.getInitialState(mentionGraph));
    }

    @Override
    public Iterator<NodeLinkingState> iterator() {
        return beamStates.iterator();
    }

    @Override
    public Spliterator<NodeLinkingState> spliterator() {
        return beamStates.spliterator();
    }

    class InnerBeam implements Comparable<InnerBeam>, Iterable<StateDelta> {
        MinMaxPriorityQueue<StateDelta> innerStates;

//        PriorityQueue<StateDelta> innerStates;

        double baseScore;

        long stateHash;

        int innerSize;

        String newestType;

        public InnerBeam(double baseScore, long stateHash, int innerSize, String newestType) {
            innerStates = StateDelta.getReverseHeap(innerSize);
//            innerStates = new PriorityQueue<>(innerSize);
            this.baseScore = baseScore;
            this.stateHash = stateHash;
            this.innerSize = innerSize;
            this.newestType = newestType;
        }

        public StateDelta[] getTops() {
            int numPopped = 0;
            int numRemaining = Math.min(innerSize, innerStates.size());
            StateDelta[] tops = new StateDelta[numRemaining];

            while (numPopped < numRemaining) {
                tops[numPopped] = innerStates.poll();
                numPopped++;
            }
            return tops;
        }

        public void clear() {
            innerStates.clear();
        }

        public void add(StateDelta state) {
            innerStates.offer(state);
        }

        public boolean isEmpty() {
            return innerStates.isEmpty();
        }

        public StateDelta poll() {
            return innerStates.poll();
        }

        @Override
        public int compareTo(InnerBeam o) {
            return new CompareToBuilder().append(this.baseScore, o.baseScore).build();
        }

        @Override
        public Iterator<StateDelta> iterator() {
            return innerStates.iterator();
        }
    }

    public NodeLinkingState getBestBeamState() {
        return beamStates.get(0);
    }

    public void prepareExpand() {
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
        for (NodeLinkingState nodeLinkingState : this) {
            nodeLinkingState.clearFeatures();
        }
    }

    public synchronized void expand(StateDelta delta) {
        boolean addToExisting = false;
        for (InnerBeam innerLayer : twoLayerDeltas) {
            if (innerLayer.stateHash == delta.getCurrentStateHash()) {
                if (innerLayer.newestType.equals(delta.getNodes().getCombinedType())) {
                    addToExisting = true;
                    innerLayer.add(delta);
                }
            }
        }

        if (!addToExisting) {
//            InnerBeam newBeam = new InnerBeam(delta.getStateNodeScore(), delta.getCurrentStateHash(),
//                    corefBeamSize, delta.getNodes().getCombinedType());
//            newBeam.add(delta);
//            twoLayerDeltas.add(newBeam);

            if (twoLayerDeltas.size() < mentionBeamSize ||
                    delta.getStateNodeScore() > twoLayerDeltas.peekLast().baseScore) {
                InnerBeam newBeam = new InnerBeam(delta.getStateNodeScore(), delta.getCurrentStateHash(),
                        corefBeamSize, delta.getNodes().getCombinedType());
                newBeam.add(delta);
                twoLayerDeltas.add(newBeam);
            }
        }
    }

    /**
     * Update the beam states, by converting the deltas to new states, and remove the old ones.
     */
    public void updateStates() {
//        int num2Pop = Math.min(mentionBeamSize, twoLayerDeltas.size());
//        while (num2Pop > 0) {
//            InnerBeam innerLayer = twoLayerDeltas.poll();
//            for (StateDelta stateDelta : innerLayer.getTops()) {
//                NodeLinkingState updatedState = stateDelta.applyUpdate(candidates);
//                nextBeamStates.add(updatedState);
//            }
//            innerLayer.clear();
//            num2Pop--;
//        }
//        twoLayerDeltas.clear();

        while (!twoLayerDeltas.isEmpty()) {
            InnerBeam nextLayer = twoLayerDeltas.poll();
            while (!nextLayer.isEmpty()) {
                StateDelta sd = nextLayer.poll();
                NodeLinkingState updatedState = sd.applyUpdate(candidates);
                nextBeamStates.add(updatedState);
            }
        }
        beamStates = nextBeamStates;
        nextBeamStates = new ArrayList<>();
    }

    @Override
    public boolean contains(LabelLinkAgenda anotherAgenda) {
        return contains((TwoLayerLabelLinkAgenda) anotherAgenda);
    }

    public boolean contains(TwoLayerLabelLinkAgenda anotherAgenda) {
        return beamStates.stream().anyMatch(s -> anotherAgenda.beamStates.stream().anyMatch(s::match));
    }

    @Override
    public void copyFrom(LabelLinkAgenda anotherAgenda) {
        copyFrom((TwoLayerLabelLinkAgenda) anotherAgenda);
    }

    private void copyFrom(TwoLayerLabelLinkAgenda anotherAgenda) {
        this.corefBeamSize = anotherAgenda.corefBeamSize;
        beamStates.clear();
        anotherAgenda.beamStates.iterator().forEachRemaining(s -> beamStates.add(s));
    }

}
