package edu.cmu.cs.lti.event_coref.model.graph;

import edu.cmu.cs.lti.event_coref.model.graph.MentionGraphEdge.EdgeType;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.NodeKey;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/3/15
 * Time: 2:28 PM
 *
 * @author Zhengzhong Liu
 */
public class SubGraphEdge {
    private EdgeType edgeType;

    private final LabelledMentionGraphEdge superGraphEdge;

    public SubGraphEdge(LabelledMentionGraphEdge edge, EdgeType edgeType) {
        this.edgeType = edgeType;
        this.superGraphEdge = edge;
    }

    public EdgeType getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(EdgeType edgeType) {
        this.edgeType = edgeType;
    }

    public NodeKey getDepKey() {
        return superGraphEdge.getDepKey();
    }

    public NodeKey getGovKey() {
        return superGraphEdge.getGovKey();
    }

    public static final Comparator<SubGraphEdge> subgraphComparator = new Comparator<SubGraphEdge>() {
        @Override
        public int compare(SubGraphEdge o1, SubGraphEdge o2) {
            return LabelledMentionGraphEdge.edgeDepComparator.compare(o1.superGraphEdge, o2.superGraphEdge);
        }
    };

    public int getGov() {
        return superGraphEdge.getGov();
    }

    public int getDep() {
        return superGraphEdge.getDep();
    }

    public String toString() {
        return String.format("SubGraphEdge: (%d:%s,%d:%s) [%s]", getGov(), getGovKey().getMentionType(),
                getDep(), getDepKey().getMentionType(), edgeType);
    }

    public LabelledMentionGraphEdge getSuperGraphEdge() {
        return superGraphEdge;
    }

    public FeatureVector getEdgeFeatures() {
        return superGraphEdge.getFeatureVector();
    }
}