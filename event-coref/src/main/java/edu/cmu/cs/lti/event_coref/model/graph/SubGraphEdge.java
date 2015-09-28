package edu.cmu.cs.lti.event_coref.model.graph;

import edu.cmu.cs.lti.learning.model.FeatureVector;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/3/15
 * Time: 2:28 PM
 *
 * @author Zhengzhong Liu
 */
public class SubGraphEdge {
    private MentionGraphEdge.EdgeType edgeType;

    private final MentionGraphEdge superGraphEdge;

    public SubGraphEdge(MentionGraphEdge edge, MentionGraphEdge.EdgeType edgeType) {
        this.edgeType = edgeType;
        this.superGraphEdge = edge;
    }

    public MentionGraphEdge.EdgeType getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(MentionGraphEdge.EdgeType edgeType) {
        this.edgeType = edgeType;
    }

    public static final Comparator<SubGraphEdge> subgraphComparator = new Comparator<SubGraphEdge>() {
        @Override
        public int compare(SubGraphEdge o1, SubGraphEdge o2) {
            return MentionGraphEdge.edgeDepComparator.compare(o1.superGraphEdge, o2.superGraphEdge);
        }
    };

    public int getGov() {
        return superGraphEdge.govIdx;
    }

    public int getDep() {
        return superGraphEdge.depIdx;
    }

    public String toString() {
        return "SubGraphEdge: (" + getGov() + ',' + getDep() + ")" + " [" + edgeType + "]";
    }

    public MentionGraphEdge getSuperGraphEdge() {
        return superGraphEdge;
    }

    public FeatureVector getEdgeFeatures() {
        return superGraphEdge.getLabelledFeatures(superGraphEdge.getHostingGraph().getExtractor());
    }
}