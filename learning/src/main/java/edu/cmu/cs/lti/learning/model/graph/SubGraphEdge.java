package edu.cmu.cs.lti.learning.model.graph;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.NodeKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/3/15
 * Time: 2:28 PM
 *
 * @author Zhengzhong Liu
 */
public class SubGraphEdge {
    private final MentionGraphEdge graphEdge;

    private EdgeType unlabelledType;

    private Table<NodeKey, NodeKey, LabelledMentionGraphEdge> labelledEdges;

    private Map<LabelledMentionGraphEdge, EdgeType> labelledTypes;

    private boolean hasUnlabelledType;

    private int labelledNonRootLinks;

    public SubGraphEdge(MentionGraphEdge edge) {
        graphEdge = edge;
        labelledEdges = HashBasedTable.create();
        labelledTypes = new HashMap<>();
        hasUnlabelledType = false;
        labelledNonRootLinks = 0;
    }

    public void addLabelledEdge(LabelledMentionGraphEdge labelledEdge, EdgeType type) {
        // Unlabelled types and labelled ones are mutually exclusive.
        this.unlabelledType = null;
        hasUnlabelledType = false;

        labelledEdges.put(labelledEdge.getGovKey(), labelledEdge.getDepKey(), labelledEdge);
        labelledTypes.put(labelledEdge, type);

        if (type != EdgeType.Root) {
            labelledNonRootLinks += 1;
        }
    }

    public LabelledMentionGraphEdge getLabelledEdge(NodeKey govKey, NodeKey depKey) {
        return labelledEdges.get(govKey, depKey);
    }

    public Collection<LabelledMentionGraphEdge> getAllLabelledEdge() {
        return labelledEdges.values();
    }

    public int numLabelledNonRootLinks() {
        return labelledNonRootLinks;
    }


    public EdgeType getUnlabelledType() {
        return unlabelledType;
    }

    public void setUnlabelledType(EdgeType unlabelledType) {
        this.unlabelledType = unlabelledType;
        hasUnlabelledType = true;
        labelledTypes = new HashMap<>();
        labelledEdges = HashBasedTable.create();
    }

    public EdgeType getLabelledType(NodeKey govKey, NodeKey depKey) {
        return labelledTypes.get(getLabelledEdge(govKey, depKey));
    }

    public EdgeType getLabelledType(LabelledMentionGraphEdge labelledEge) {
        return labelledTypes.get(labelledEge);
    }

    public boolean hasUnlabelledType() {
        return hasUnlabelledType;
    }

    public int getGov() {
        return graphEdge.getGov();
    }

    public int getDep() {
        return graphEdge.getDep();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SubGraphEdge: (%d,%d) [%s]", getGov(), getDep(), unlabelledType));
        for (Map.Entry<LabelledMentionGraphEdge, EdgeType> edgeWithType : labelledTypes.entrySet()) {
            sb.append("\t");
            sb.append(String.format("%s, predicted as: %s.", edgeWithType.getKey(), edgeWithType.getValue()));
        }

        return sb.toString();
    }

    public FeatureVector getEdgeFeatures() {
        return graphEdge.getNodeAgnosticFeatures();
    }
}