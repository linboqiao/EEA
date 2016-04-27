package edu.cmu.cs.lti.learning.model.decoding;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.graph.MentionGraphEdge.EdgeType;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represent a delta on a previous state, which includes change in score, features, and decoding results. These
 * changes are not directly applied to the states because we need to choose the best deltas.
 */
public class StateDelta implements Comparable<StateDelta> {
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    private NodeLinkingState existingState;
    private double newScore;

    private MultiNodeKey nodes;

    private List<EdgeKey> edges;

    private GraphFeatureVector deltaLabelFv;

    // The list of features are corresponding to each possible antecedent decoding, one for each of the possible type
    // on the anaphora node. This is caused by the fact that there can be multiple types on one mention.
    private List<Pair<EdgeType, FeatureVector>> deltaGraphFv;

    public StateDelta(NodeLinkingState existingState) {
        this.existingState = existingState;
        newScore = existingState.getScore();
        edges = new ArrayList<>();
        deltaGraphFv = new ArrayList<>();
    }

    public void setNode(MultiNodeKey nodes, GraphFeatureVector newLabelFv, double nodeScore) {
        this.nodes = nodes;
        this.deltaLabelFv = newLabelFv;
        newScore += nodeScore;
    }

    public void addLink(EdgeType linkType, NodeKey govKey, NodeKey depKey, double linkScore, FeatureVector newCorefFv) {
        edges.add(new EdgeKey(depKey, govKey, linkType));
        newScore += linkScore;
        deltaGraphFv.add(Pair.of(linkType, newCorefFv));
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

    public NodeLinkingState applyUpdate(List<MentionCandidate> candidates) {
        NodeLinkingState state = existingState.makeCopy();

//        logger.info("Before applying : " + state.showNodes());

//        for (int i = 0; i < nodes.size(); i++) {
//            Integer antecedent = antecedents.get(i);
//            EdgeType linkType = linkTypes.get(i);
//            state.addLinkTo(candidates, antecedent, govKeys.get(i), nodes.get(i), linkType);
//        }

        for (EdgeKey edge : edges) {
            state.addLink(candidates, edge);
        }

        state.addNode(nodes);

        state.setScore(newScore);

        state.extendFeatures(deltaLabelFv, deltaGraphFv);

//        logger.info("After applying : " + state.showNodes());
//        DebugUtils.pause();

        return state;
    }

    public String toString() {
        String typeStr = MentionTypeUtils.joinMultipleTypes(nodes.stream()
                .map(NodeKey::getMentionType).collect(Collectors.toSet()));

        StringBuilder sb = new StringBuilder();
        for (NodeKey node : nodes) {
            sb.append("\n");
            sb.append(node.toString());
        }

        return String.format("[StateDelta] [%s] [Links:] %s", typeStr, sb.toString());
    }

    public List<Pair<EdgeType, FeatureVector>> getDeltaGraphFv() {
        return deltaGraphFv;
    }

    public GraphFeatureVector getDeltaLabelFv() {
        return deltaLabelFv;
    }
}