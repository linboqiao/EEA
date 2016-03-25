package edu.cmu.cs.lti.event_coref.decoding.model;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge.EdgeType;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.GraphFeatureVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MentionCandidate.DecodingResult;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.javatuples.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class StateDelta implements Comparable<StateDelta> {
//    private final MentionGraphEdge.EdgeType linkType;
//    private DecodingResult node;
//    private int antecedent;

    private NodeLinkingState existingState;
    private double newScore;

    private List<DecodingResult> nodes;
    private List<Integer> antecedents;
    private List<MentionGraphEdge.EdgeType> linkTypes;
    private List<DecodingResult> govKeys;
    private List<DecodingResult> depKeys;

    private GraphFeatureVector deltaLabelFv;

    // The list of features are corresponding to each possible antecedent decoding, one for each of the possible type
    // on the anaphora node. This is caused by the fact that there can be multiple types on one mention.
    private List<Pair<EdgeType, FeatureVector>> deltaGraphFv;

    public StateDelta(NodeLinkingState existingState, List<Integer> antecedents,
                      List<MentionGraphEdge.EdgeType> linkTypes, List<DecodingResult> govKeys,
                      List<DecodingResult> depKeys, double additionalScore, GraphFeatureVector newLabelFv,
                      List<Pair<EdgeType, FeatureVector>> newCorefFv) {
        this.existingState = existingState;
        this.antecedents = antecedents;
        this.linkTypes = linkTypes;
        this.newScore = existingState.getScore() + additionalScore;
        this.deltaLabelFv = newLabelFv;
        this.deltaGraphFv = newCorefFv;
        this.govKeys = govKeys;
        this.depKeys = depKeys;
        this.nodes = depKeys;
    }

    @Override
    public int compareTo(StateDelta o) {
        return new CompareToBuilder().append(newScore, o.newScore).build();
    }

    public static Comparator<StateDelta> reverseComparator() {
        return Comparator.reverseOrder();
    }

    public static MinMaxPriorityQueue<StateDelta> getReverseHeap(int maxSize) {
        return MinMaxPriorityQueue.orderedBy(reverseComparator()).maximumSize(maxSize).create();
    }

    public NodeLinkingState getUpdatedState(List<MentionCandidate> candidates) {
        NodeLinkingState state = existingState.makeCopy();

//        System.out.println("Before applying : " + state.showNodes());

        for (int i = 0; i < nodes.size(); i++) {
            Integer antecedent = antecedents.get(i);
            EdgeType linkType = linkTypes.get(i);
            state.addLinkTo(candidates, antecedent, govKeys.get(i), depKeys.get(i), linkType);
        }

        state.addNode(nodes);

        state.setScore(newScore);

//        System.out.println("After applying : " + state.showNodes());
//        DebugUtils.pause();

        return state;
    }

    public String toString() {
        String typeStr = MentionTypeUtils.joinMultipleTypes(nodes.stream()
                .map(MentionCandidate.DecodingResult::getMentionType).collect(Collectors.toSet()));
        return String.format("[StateDelta] [%s] --%s--> [%d] ", nodes.get(0).toString(), typeStr, antecedents.get(0));
    }

    public List<Pair<EdgeType, FeatureVector>> getDeltaGraphFv() {
        return deltaGraphFv;
    }

    public GraphFeatureVector getDeltaLabelFv() {
        return deltaLabelFv;
    }
}