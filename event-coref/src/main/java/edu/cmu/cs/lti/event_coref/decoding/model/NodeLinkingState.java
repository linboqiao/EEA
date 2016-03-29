package edu.cmu.cs.lti.event_coref.decoding.model;

import com.google.common.collect.MinMaxPriorityQueue;
import edu.cmu.cs.lti.event_coref.model.graph.LabelledMentionGraphEdge;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraph;
import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge.EdgeType;
import edu.cmu.cs.lti.event_coref.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.GraphFeatureVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MentionCandidate.DecodingResult;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represent both the node state and the linking state of the elements.
 *
 * @author Zhengzhong Liu
 */
public class NodeLinkingState implements Comparable<NodeLinkingState> {
    protected transient final Logger logger = LoggerFactory.getLogger(getClass());

    private double score;

    private MentionGraph graph;

    // Node index is the index the record how many actual nodes are added to the decoding state.
    // So the value of this index can be used to retrieve the corresponding graph node from the MentionGraph.
    private int nodeIndex;

    // Here is the node decoding results for each token. There are multiple possible types for a token, so each token
    // is corresponding multiple decoding results.
    private List<List<DecodingResult>> nodeResults;

    private MentionSubGraph decodingTree;

    private GraphFeatureVector labelFv;

    private Map<EdgeType, FeatureVector> corefFv;

    private NodeLinkingState(MentionGraph graph) {
        this.graph = graph;
        this.nodeResults = new ArrayList<>();
        this.nodeIndex = 0;
        decodingTree = new MentionSubGraph(graph);
        corefFv = new HashMap<>();
    }

    public static NodeLinkingState getInitialState(MentionGraph graph) {
        NodeLinkingState s = new NodeLinkingState(graph);
        List<DecodingResult> rootNode = MentionCandidate.getRootKey();
        s.nodeResults.add(rootNode);
        s.nodeIndex += 1; // The first node is added, which is the root.
        return s;
    }

    public void clearFeatures() {
        labelFv = null;
        corefFv.clear();
    }

    public String toString() {
        return "[Node Linking State]\n" +
                "Score: " + score +
                "\n<Nodes>\n" +
                showNodes() +
                "\n" +
                "<Coref Tree>\n" +
                decodingTree.toString();
    }

    public String showTree() {
        return decodingTree.toString();
    }

    public String showNodes() {
        StringBuilder nodes = new StringBuilder();
        nodes.append("[State Nodes] ");
        for (int i = 0; i < nodeResults.size(); i++) {
            for (DecodingResult nodeResult : nodeResults.get(i)) {
                nodes.append(i).append(":").append(nodeResult.getMentionType()).append(" ");
            }
            nodes.append(";");
        }
        return nodes.toString();
    }

    public int compareTo(NodeLinkingState s) {
        return new CompareToBuilder().append(score, s.score).build();
    }

    public static Comparator<NodeLinkingState> reverseComparator() {
        return Comparator.reverseOrder();
    }

    public List<DecodingResult> getLastNode() {
        return getNode(nodeIndex - 1);
    }

    public List<DecodingResult> getNode(int index) {
        return nodeResults.get(index);
    }

    public List<List<DecodingResult>> getNodeResults() {
        return nodeResults;
    }

    public void extendFeatures(GraphFeatureVector newLabelFv, List<Pair<EdgeType, FeatureVector>> newCorefFvs) {
        if (this.labelFv == null) {
            this.labelFv = newLabelFv.newGraphFeatureVector();
        }

        this.labelFv.extend(newLabelFv);

        for (Pair<EdgeType, FeatureVector> newCorefFv : newCorefFvs) {
            EdgeType fvType = newCorefFv.getValue0();
            FeatureVector fv = newCorefFv.getValue1();
            if (this.corefFv.containsKey(fvType)) {
                corefFv.get(fvType).extend(fv);
            } else {
                corefFv.put(fvType, fv);
            }
        }
    }

    public void addLinkTo(List<MentionCandidate> candidates, int antecedent, DecodingResult govKey,
                          DecodingResult depKey, EdgeType type) {
        addLink(candidates, antecedent, nodeIndex, govKey, depKey, type);
    }

    public void addLink(List<MentionCandidate> mentionCandidates, int gov, int dep, DecodingResult govKey,
                        DecodingResult depKey, EdgeType type) {
//        System.out.println("Adding link from " + dep);
//        System.out.println(govKey.toString());
//        System.out.println(depKey.toString());

        LabelledMentionGraphEdge edge = graph.getMentionGraphEdge(dep, gov).getLabelledEdge(mentionCandidates,
                govKey, depKey);
        decodingTree.addEdge(edge, type);
    }

    public void addNode(List<DecodingResult> newNode) {
        nodeResults.add(newNode);
        nodeIndex++;
    }

    public Pair<Double, Double> loss(NodeLinkingState referenceState) {
        double labelLoss = computeHammingLoss(referenceState.getNodeResults());

        double graphLoss;

        if (!decodingTree.graphMatch(referenceState.decodingTree)) {
            graphLoss = decodingTree.getLoss(referenceState.decodingTree);
        } else {
            graphLoss = 0;
        }

        return Pair.with(labelLoss, graphLoss);
    }

    private double computeHammingLoss(List<List<DecodingResult>> referenceNodes) {
        double loss = 0;

        // Root node is ignored.
        for (int i = 1; i < nodeIndex; i++) {
            double matches = 0;

            List<DecodingResult> decodingResult = nodeResults.get(i);
            List<DecodingResult> referentReslt = referenceNodes.get(i);

            Set<String> decodingTypes = decodingResult.stream().map(DecodingResult::getMentionType)
                    .collect(Collectors.toSet());

            Set<String> referentTypes = referentReslt.stream().map(DecodingResult::getMentionType)
                    .collect(Collectors.toSet());

            for (String decodedType : decodingTypes) {
                if (referentTypes.contains(decodedType)) {
                    matches += 1;
                }
            }

            double dice = 2 * matches / (decodingTypes.size() + referentTypes.size());

            loss += 1 - dice;
        }
        return loss;
    }

    public String getCombinedLastNodeType() {
        return getCombinedMentionType(nodeIndex - 1);
    }

    public String getCombinedMentionType(int nodeIndex) {
        return MentionTypeUtils.joinMultipleTypes(getMentionType(nodeIndex));
    }

    public Set<String> getMentionType(int nodeIndex) {
        return nodeResults.get(nodeIndex).stream().map(DecodingResult::getMentionType).collect(Collectors.toSet());
    }

    public static MinMaxPriorityQueue<NodeLinkingState> getReverseHeap(int maxSize) {
        return MinMaxPriorityQueue.orderedBy(NodeLinkingState.reverseComparator()).maximumSize(maxSize).create();
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public MentionSubGraph getDecodingTree() {
        return decodingTree;
    }

    public boolean match(NodeLinkingState otherState) {
        boolean corefMatch = otherState.decodingTree.graphMatch(decodingTree, nodeIndex);

        boolean labelMatch = true;
        for (int i = 1; i < nodeIndex; i++) {
            Set<String> otherTypes = otherState.getMentionType(i);
            if (!getMentionType(i).equals(otherTypes)) {
                labelMatch = false;
                break;
            }
        }

//        if (corefMatch && labelMatch) {
//            logger.debug("State match between the following.");
//            logger.debug(this.toString());
//            logger.debug(otherState.toString());
//        }

        return corefMatch && labelMatch;
    }

    public GraphFeatureVector getLabelFv() {
        return labelFv;
    }

    public Map<EdgeType, FeatureVector> getCorefFv() {
        return corefFv;
    }

    public NodeLinkingState makeCopy() {
        NodeLinkingState state = new NodeLinkingState(graph);
        state.score = score;
        state.decodingTree = decodingTree.makeCopy();
        state.nodeIndex = nodeIndex;
        for (List<DecodingResult> node : nodeResults) {
            state.nodeResults.add(node);
        }

        // TODO making full copy of the existing feature vector, might be faster way of doing this.
        if (this.labelFv != null) {
            state.labelFv = this.labelFv.newGraphFeatureVector();
            state.labelFv.extend(this.labelFv);
        }

        state.corefFv = new HashMap<>();
        for (Map.Entry<EdgeType, FeatureVector> corefFv : this.corefFv.entrySet()) {
            FeatureVector newFv = corefFv.getValue().newFeatureVector();
            newFv.extend(corefFv.getValue());
            state.corefFv.put(corefFv.getKey(), newFv);
        }

        return state;
    }
}
