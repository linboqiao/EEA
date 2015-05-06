package edu.cmu.lti.event_coref.model.graph;

import gnu.trove.map.TObjectDoubleMap;
import org.javatuples.Pair;

import java.util.Comparator;
import java.util.EnumMap;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/1/15
 * Time: 10:56 PM
 * <p/>
 * Partially borrowed from HotCoref
 * <p/>
 * http://www.ims.uni-stuttgart.de/forschung/ressourcen/werkzeuge/HOTCoref.en.html
 *
 * @author Zhengzhong Liu
 */
public class Edge {
    public final int govIdx;
    public final int depIdx;
    public EdgeType edgeType;

    //a score for each label type
    private double[] labelScores;
    //the score on the arc without the label
    private double arcScore;


    private TObjectDoubleMap<String> arcFeatures;
    private EnumMap<EdgeType, TObjectDoubleMap<String>> labelledFeatures;

    public enum EdgeType {Root, Coreference, Subevent, After}

    public Edge(int gov, int dep) {
        this.govIdx = gov;
        this.depIdx = dep;
        this.labelScores = new double[EdgeType.values().length];
    }

    public void setLabelScore(EdgeType edgeType, double score) {
        labelScores[edgeType.ordinal()] = score;
    }

    public double getLabelScore(EdgeType edgeType) {
        return labelScores[edgeType.ordinal()];
    }

    public double getArcScore() {
        return arcScore;
    }

    public void setArcScore(double arcScore) {
        this.arcScore = arcScore;
    }

    public Pair<EdgeType, Double> getCorretLabelScore() {
        if (edgeType != null) {
            return Pair.with(edgeType, labelScores[edgeType.ordinal()]);
        } else {
            return null;
        }
    }

    public Pair<EdgeType, Double> getBestLabelScore() {
        double score = Double.NEGATIVE_INFINITY;
        EdgeType bestLabel = null;
        for (EdgeType label : EdgeType.values()) {
            double typeScore = getLabelScore(label);
            if (typeScore > score) {
                score = typeScore;
                bestLabel = label;
            }
        }
        return Pair.with(bestLabel, score);
    }

    public double getLabelScore(int edgeType) {
        return labelScores[edgeType];
    }

    public TObjectDoubleMap<String> getArcFeatures() {
        return arcFeatures;
    }

    public void setArcFeatures(TObjectDoubleMap<String> arcFeatures) {
        this.arcFeatures = arcFeatures;
    }

    public EnumMap<EdgeType, TObjectDoubleMap<String>> getLabelledFeatures() {
        return labelledFeatures;
    }

    public void setLabelledFeatures(EnumMap<EdgeType, TObjectDoubleMap<String>> labelledFeatures) {
        this.labelledFeatures = labelledFeatures;
    }

    public boolean equals(Edge other) {
        return govIdx == other.govIdx && depIdx == other.depIdx;
    }

    public String toString() {
        return "e: (" + govIdx + ',' + depIdx + ")";
    }

    public static final Comparator<Edge> edgeDepComparator = new Comparator<Edge>() {
        @Override
        public int compare(Edge o1, Edge o2) {
            return o1.depIdx - o2.depIdx;
        }
    };
}
