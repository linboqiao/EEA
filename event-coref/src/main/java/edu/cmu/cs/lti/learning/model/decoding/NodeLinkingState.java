package edu.cmu.cs.lti.learning.model.decoding;

import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.LabelledMentionGraphEdge;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.MentionSubGraph;
import edu.cmu.cs.lti.learning.update.SeqLoss;
import edu.cmu.cs.lti.learning.utils.MentionTypeUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Pair;
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

    // Here is the node decoding results. Each item correspond to a node in the graph (which includes the root node
    // as well).
    private List<MultiNodeKey> nodeResults;

    private MentionSubGraph decodingTree;

    private GraphFeatureVector labelFv;

    private Map<EdgeType, FeatureVector> corefFv;

    private NodeLinkingState(MentionGraph graph) {
        this.graph = graph;
        this.nodeResults = new ArrayList<>();
        this.nodeIndex = 0;
        if (graph != null) {
            decodingTree = new MentionSubGraph(graph);
        }
        corefFv = new HashMap<>();
    }

    public static NodeLinkingState getInitialState(MentionGraph graph) {
        NodeLinkingState s = new NodeLinkingState(graph);
        MultiNodeKey rootNode = MultiNodeKey.rootKey();
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
                ((decodingTree == null) ? "[No coreference]" :
                        "[Coref Tree]\n" + decodingTree.toString());

    }

    public String showTree() {
        if (decodingTree == null) {
            return "[No coreference]";
        }
        return decodingTree.toString();
    }

    public String showNodes() {
        StringBuilder nodes = new StringBuilder();
        nodes.append("[State Nodes] ");
        for (int i = 0; i < nodeResults.size(); i++) {
            for (NodeKey nodeResult : nodeResults.get(i)) {
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

    public MultiNodeKey getLastNode() {
        return getNode(nodeIndex - 1);
    }

    public MultiNodeKey getNode(int index) {
        return nodeResults.get(index);
    }

    public List<MultiNodeKey> getNodeResults() {
        return nodeResults;
    }

    public void extendFeatures(GraphFeatureVector newLabelFv, List<Pair<EdgeType, FeatureVector>> newCorefFvs) {
        if (this.labelFv == null) {
            this.labelFv = newLabelFv.newGraphFeatureVector();
        }

        this.labelFv.extend(newLabelFv);


        for (Pair<EdgeType, FeatureVector> newCorefFv : newCorefFvs) {
            EdgeType fvType = newCorefFv.getLeft();
            FeatureVector fv = newCorefFv.getRight();
            if (this.corefFv.containsKey(fvType)) {
                corefFv.get(fvType).extend(fv);
            } else {
                corefFv.put(fvType, fv);
            }
        }
    }

    public void addLinkTo(List<MentionCandidate> candidates, int antecedent, NodeKey govKey,
                          NodeKey depKey, EdgeType type) {
        addLink(candidates, antecedent, nodeIndex, govKey, depKey, type);
    }

    public void addLink(List<MentionCandidate> mentionCandidates, int gov, int dep, NodeKey govKey,
                        NodeKey depKey, EdgeType type) {
//        System.out.println("Adding link from " + dep);
//        System.out.println(govKey.toString());
//        System.out.println(depKey.toString());

        LabelledMentionGraphEdge edge = graph.getMentionGraphEdge(dep, gov).getLabelledEdge(mentionCandidates,
                govKey, depKey);
        decodingTree.addEdge(edge, type);
    }

    public void addLink(List<MentionCandidate> mentionCandidates, EdgeKey edgeKey) {
        LabelledMentionGraphEdge edge = graph.getLabelledEdge(mentionCandidates,
                edgeKey.getGovNode(), edgeKey.getDepNode());
        decodingTree.addEdge(edge, edgeKey.getType());
    }

    public void addNode(MultiNodeKey newNode) {
        nodeResults.add(newNode);
        nodeIndex++;
    }

    public Pair<Double, Double> loss(NodeLinkingState referenceState, SeqLoss labelLosser) {
        double labelLoss = computeLabelLoss(referenceState.getNodeResults(), labelLosser);

//        logger.info("Label loss is " + labelLoss);
//        DebugUtils.pause();

        double graphLoss = 0;

        if (decodingTree != null) {
            if (!decodingTree.graphMatch(referenceState.decodingTree)) {
                graphLoss = decodingTree.getLoss(referenceState.decodingTree);
            }
        }

        return Pair.of(labelLoss, graphLoss);
    }

    private double computeLabelLoss(List<MultiNodeKey> referenceNodes, SeqLoss labelLosser) {
        String[] reference = new String[referenceNodes.size() - 1];
        String[] prediction = new String[nodeResults.size() - 1];

        // The ROOT node is not counted.
        for (int i = 1; i < referenceNodes.size(); i++) {
            reference[i - 1] = MentionTypeUtils.joinMultipleTypes(referenceNodes.get(i).stream()
                    .map(NodeKey::getMentionType).collect(Collectors.toList()));
            prediction[i - 1] = MentionTypeUtils.joinMultipleTypes(nodeResults.get(i).stream()
                    .map(NodeKey::getMentionType).collect(Collectors.toList()));
        }

        return labelLosser.compute(reference, prediction, ClassAlphabet.noneOfTheAboveClass);
    }

    public String getCombinedLastNodeType() {
        return getCombinedMentionType(nodeIndex - 1);
    }

    public String getCombinedMentionType(int nodeIndex) {
        return nodeResults.get(nodeIndex).getCombinedType();
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
        boolean corefMatch = decodingTree == null || otherState.decodingTree.graphMatch(decodingTree, nodeIndex);

        boolean labelMatch = true;
        for (int i = 1; i < nodeIndex; i++) {
            String otherType = otherState.getCombinedMentionType(i);
            if (!getCombinedMentionType(i).equals(otherType)) {
                labelMatch = false;
                break;
            }
        }

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
        if (decodingTree != null) {
            state.decodingTree = decodingTree.makeCopy();
        }
        state.nodeIndex = nodeIndex;
        for (MultiNodeKey node : nodeResults) {
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
//
//    public Map<Integer, String> getAvailableNodeLabels() {
//        Map<Integer, String> nodeTypes = new HashMap<>();
//        for (int i = 0; i < nodeResults.size(); i++) {
//            String joinedType = getCombinedMentionType(i);
//            nodeTypes.put(i, joinedType);
//        }
//        return nodeTypes;
//    }
}
