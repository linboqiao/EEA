package edu.cmu.cs.lti.event_coref.model.graph;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/3/15
 * Time: 2:28 PM
 *
 * @author Zhengzhong Liu
 */
public class SubGraphEdge {
    public final int govIdx;
    public final int depIdx;
    private Edge.EdgeType edgeType;

//    private double score;

    public SubGraphEdge(int govIdx, int depIdx, Edge.EdgeType edgeType) {
        this.govIdx = govIdx;
        this.depIdx = depIdx;
        this.edgeType = edgeType;
    }

    public Edge.EdgeType getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(Edge.EdgeType edgeType) {
        this.edgeType = edgeType;
    }

    public static final Comparator<SubGraphEdge> subgraphComparator = new Comparator<SubGraphEdge>() {
        @Override
        public int compare(SubGraphEdge o1, SubGraphEdge o2) {
            return o1.depIdx - o2.depIdx;
        }
    };

    public String toString() {
        return "SubGraphEdge: (" + govIdx + ',' + depIdx + ")" + " [" + edgeType + "]";
    }
}
