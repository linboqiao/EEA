package edu.cmu.cs.lti.event_coref.model.graph;

import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import org.javatuples.Pair;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/1/15
 * Time: 10:56 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionGraphEdge {
    public final int govIdx;
    public final int depIdx;

    private final MentionGraph hostingGraph;

    // This variable store the gold type during training. The subgraph will store the predicted type.
    public EdgeType edgeType = null;

    // A score for each label type.
    private double[] labelScores;

    private boolean isScored;

    private boolean featureExtracted;

    private FeatureVector labelledFeatures;

    public enum EdgeType {Root, Coreference, Subevent, After}

    public MentionGraphEdge(MentionGraph graph, int gov, int dep) {
        this.govIdx = gov;
        this.depIdx = dep;
        this.labelScores = new double[EdgeType.values().length];
        this.isScored = false; // Both features and scores started as 0.
        this.featureExtracted = false;
        this.hostingGraph = graph;
    }

    public double getLabelScore(EdgeType edgeType, GraphWeightVector weightVector, PairFeatureExtractor extractor) {
        if (!isScored) {
            scoreEdge(weightVector, extractor);
        }
        return labelScores[edgeType.ordinal()];
    }

    public void scoreEdge(GraphWeightVector weightVector, PairFeatureExtractor extractor) {
        for (int i = 0; i < EdgeType.values().length; i++) {
            double typeScore = weightVector.dotProd(getLabelledFeatures(extractor), EdgeType.values()[i].name());
            labelScores[i] = typeScore;
        }
        isScored = true;
    }

    private void extractFeatures(PairFeatureExtractor extractor) {
        labelledFeatures = extractor.extract(hostingGraph.getContext(), hostingGraph.getNode(govIdx)
                .getMention(), hostingGraph.getNode(depIdx).getMention());
        featureExtracted = true;
    }

    public Pair<EdgeType, Double> getCorrectLabelScore(GraphWeightVector weightVector, PairFeatureExtractor extractor) {
        if (edgeType == null) {
            return null;
        }
        return Pair.with(edgeType, getLabelScore(edgeType, weightVector, extractor));
    }

    public Pair<EdgeType, Double> getBestLabelScore(GraphWeightVector weightVector, PairFeatureExtractor extractor) {
        double score = Double.NEGATIVE_INFINITY;
        boolean isRootEdge = govIdx == 0;
        EdgeType bestLabel = null;

        if (isRootEdge) {
            // The only possible relation with the root node is ROOT.
            score = getLabelScore(EdgeType.Root, weightVector, extractor);
            bestLabel = EdgeType.Root;
        } else {
            for (EdgeType label : EdgeType.values()) {
                if (label.equals(EdgeType.Root)) {
                    // If the edge is not a root edge, we will not test for Root edge type.
                    continue;
                }
                double typeScore = getLabelScore(label, weightVector, extractor);
                if (typeScore > score) {
                    score = typeScore;
                    bestLabel = label;
                }
            }
        }
        return Pair.with(bestLabel, score);
    }

    public FeatureVector getLabelledFeatures(PairFeatureExtractor extractor) {
        if (!featureExtracted) {
            extractFeatures(extractor);
        }
        return labelledFeatures;
    }


    public MentionGraph getHostingGraph() {
        return hostingGraph;
    }

    public boolean equals(MentionGraphEdge other) {
        return govIdx == other.govIdx && depIdx == other.depIdx;
    }

    public String toString() {
        return "Edge: (" + govIdx + ',' + depIdx + ")" + " [" + edgeType + "]";
    }

    public static final Comparator<MentionGraphEdge> edgeDepComparator = new Comparator<MentionGraphEdge>() {
        @Override
        public int compare(MentionGraphEdge o1, MentionGraphEdge o2) {
            return o1.depIdx - o2.depIdx;
        }
    };
}
