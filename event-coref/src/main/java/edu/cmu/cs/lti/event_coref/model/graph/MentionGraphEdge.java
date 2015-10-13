package edu.cmu.cs.lti.event_coref.model.graph;

import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import org.javatuples.Pair;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/1/15
 * Time: 10:56 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionGraphEdge implements Serializable {
    private static final long serialVersionUID = -4095257622117532545L;

    public final int govIdx;
    public final int depIdx;

    private final MentionGraph hostingGraph;

    // This variable store the gold type during training.
    public EdgeType edgeType = null;

    private boolean featureExtracted;

    private FeatureVector labelledFeatures;

    public enum EdgeType {Root, Coreference}

    private boolean testingMode;

    public MentionGraphEdge(MentionGraph graph, int gov, int dep, boolean testingMode) {
        this.govIdx = gov;
        this.depIdx = dep;
        this.featureExtracted = false;
        this.hostingGraph = graph;
        this.testingMode = testingMode;
    }

    public double scoreEdge(EdgeType type, GraphWeightVector weightVector, PairFeatureExtractor extractor) {
        return weightVector.dotProd(getLabelledFeatures(extractor), type.name());
    }

    private void extractFeatures(PairFeatureExtractor extractor) {
        MentionNode govNode = hostingGraph.getNode(govIdx);
        MentionNode depNode = hostingGraph.getNode(depIdx);

        if (govNode.isRoot()) {
            labelledFeatures = extractor.extract(depNode.getMentionIndex());
        } else {
            labelledFeatures = extractor.extract(govNode.getMentionIndex(), depNode.getMentionIndex());
        }
        featureExtracted = true;
    }

    public Pair<EdgeType, Double> getCorrectLabelScore(GraphWeightVector weightVector, PairFeatureExtractor extractor) {
        if (edgeType == null) {
            return null;
        }
        return Pair.with(edgeType, scoreEdge(edgeType, weightVector, extractor));
    }

    public Pair<EdgeType, Double> getBestLabelScore(GraphWeightVector weightVector, PairFeatureExtractor extractor) {
        double score = Double.NEGATIVE_INFINITY;
        boolean isRootEdge = govIdx == 0;
        EdgeType bestLabel = null;

        if (isRootEdge) {
            // The only possible relation with the root node is ROOT.
            score = scoreEdge(EdgeType.Root, weightVector, extractor);
            bestLabel = EdgeType.Root;
        } else {
            for (EdgeType label : EdgeType.values()) {
                if (label.equals(EdgeType.Root)) {
                    // If the edge is not a root edge, we will not test for Root edge type.
                    continue;
                }
                double typeScore = scoreEdge(label, weightVector, extractor);

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
