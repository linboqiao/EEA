package edu.cmu.cs.lti.learning.model.decoding;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represent a delta on a previous state, which includes change in score, features, and decoding results. These
 * changes are not directly applied to the states because we need to choose the best deltas.
 */
public class StateDelta implements Comparable<StateDelta> {
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    private NodeLinkingState existingState;
    private double updatedScore;

    private MultiNodeKey nodes;

    private List<EdgeKey> edges;

    private GraphFeatureVector deltaLabelFv;

    // The list of features are corresponding to each possible antecedent decoding, one for each of the possible type
    // on the anaphora node. This is caused by the fact that there can be multiple types on one mention.
    private List<Pair<EdgeType, FeatureVector>> deltaGraphFv;

    public StateDelta(NodeLinkingState existingState) {
        this.existingState = existingState;
        updatedScore = existingState.getScore();
        edges = new ArrayList<>();
        deltaGraphFv = new ArrayList<>();
    }

    public void addNode(MultiNodeKey nodes, GraphFeatureVector newLabelFv, double nodeScore) {
        this.nodes = nodes;
        this.deltaLabelFv = newLabelFv;
        updatedScore += nodeScore;
    }

    public void addLink(EdgeType linkType, NodeKey govKey, NodeKey depKey, double linkScore, FeatureVector newCorefFv) {
        EdgeKey edge = new EdgeKey(depKey, govKey, linkType);
        edges.add(edge);
        updatedScore += linkScore;
        deltaGraphFv.add(Pair.of(linkType, newCorefFv));
    }

    @Override
    public int compareTo(StateDelta o) {
        return new CompareToBuilder().append(updatedScore, o.updatedScore).build();
    }

    public static Comparator<StateDelta> reverseComparator() {
        return Comparator.reverseOrder();
    }

    public static MinMaxPriorityQueue<StateDelta> getReverseHeap(int maxSize) {
        return MinMaxPriorityQueue.orderedBy(reverseComparator()).maximumSize(maxSize).create();
    }

    public NodeLinkingState applyUpdate(List<MentionCandidate> candidates) {
        NodeLinkingState state = existingState.makeCopy();

        for (EdgeKey edge : edges) {
            state.addLink(candidates, edge);
        }

        state.addNode(nodes);

        state.setScore(updatedScore);

        state.extendFeatures(deltaLabelFv, deltaGraphFv);

        return state;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (NodeKey node : nodes) {
            sb.append("\n");
            sb.append(node.toString());
        }

        return String.format("[StateDelta]\n [Nodes] %s\n [Links]\n [Score] %.4f", sb.toString(), updatedScore);
    }

    public List<Pair<EdgeType, FeatureVector>> getDeltaGraphFv() {
        return deltaGraphFv;
    }

    public GraphFeatureVector getDeltaLabelFv() {
        return deltaLabelFv;
    }
}