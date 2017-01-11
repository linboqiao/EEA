package edu.cmu.cs.lti.learning.model.graph;

import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.learning.model.NodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/1/15
 * Time: 10:56 PM
 *
 * @author Zhengzhong Liu
 */
public class LabelledMentionGraphEdge implements Serializable {
    private static final long serialVersionUID = -4095257622117532545L;
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    private final MentionGraphEdge hostingEdge;

    private FeatureVector featureVector;

    private NodeKey govKey;

    private NodeKey depKey;

    private boolean averageMode;

    private EdgeType actualEdgeType;

    public LabelledMentionGraphEdge(MentionGraphEdge hostingEdge, NodeKey govKey, NodeKey depKey, boolean averageMode) {
        this.hostingEdge = hostingEdge;
        this.averageMode = averageMode;
        this.govKey = govKey;
        this.depKey = depKey;
//        this.edgeType = edgeType;
    }

    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    public double scoreEdge(EdgeType type, GraphWeightVector weightVector) {
        double nodeDependentScore = averageMode ? weightVector.dotProdAver(featureVector, type.name()) :
                weightVector.dotProd(featureVector, type.name());

        double nodeAgnosticScore = averageMode ?
                weightVector.dotProdAver(hostingEdge.getNodeAgnosticFeatures(), type.name()) :
                weightVector.dotProd(hostingEdge.getNodeAgnosticFeatures(), type.name());

        return nodeDependentScore + nodeAgnosticScore;
    }

    public boolean hasActualEdgeType() {
        return actualEdgeType != null;
    }

    public double getCorrectLabelScore(GraphWeightVector weightVector) {
        if (actualEdgeType == null) {
            return Double.NaN;
        }
        return scoreEdge(actualEdgeType, weightVector);
    }

//
//    public Pair<EdgeType, Double> getCorrectLabelScore(GraphWeightVector weightVector) {
//        EdgeType actualEdgeType = hostingEdge.getRealUnlabelledEdgeType();
//        if (actualEdgeType == null) {
//            return null;
//        }
//        return Pair.of(actualEdgeType, scoreEdge(actualEdgeType, weightVector));
//    }

    public Map<EdgeType, Double> getBestLabelScore(GraphWeightVector weightVector) {
//        double score = Double.NEGATIVE_INFINITY;
//        EdgeType bestLabel = null;

        Map<EdgeType, Double> labelScores = new HashMap<>();

        if (hostingEdge.isRoot()) {
            // The only possible relation with the root node is ROOT.
//            bestLabel = EdgeType.Root;
            labelScores.put(EdgeType.Root, scoreEdge(EdgeType.Root, weightVector));
        } else {
            for (EdgeType label : EdgeType.values()) {
                if (label.equals(EdgeType.Root)) {
                    // If the edge is not a root edge, we will not test for Root edge type.
                    continue;
                }
                double typeScore = scoreEdge(label, weightVector);

                labelScores.put(label, typeScore);

//                if (typeScore > score) {
//                    score = typeScore;
//                    bestLabel = label;
//                }
            }
        }
        return labelScores;
    }

    public Map<EdgeType, Double> getAllLabelScore(GraphWeightVector weightVector) {
        Map<EdgeType, Double> allLabelScores = new HashMap<>();

        if (hostingEdge.isRoot()) {
            // The only possible relation with the root node is ROOT.
            allLabelScores.put(EdgeType.Root, scoreEdge(EdgeType.Root, weightVector));
        } else {
            for (EdgeType label : EdgeType.values()) {
                if (label.equals(EdgeType.Root)) {
                    // If the edge is not a root edge, we will not test for Root edge type.
                    continue;
                }
                double typeScore = scoreEdge(label, weightVector);
                allLabelScores.put(label, typeScore);
            }
        }
        return allLabelScores;
    }


    public int getGov() {
        return hostingEdge.getGov();
    }

    public int getDep() {
        return hostingEdge.getDep();
    }

    public static final Comparator<LabelledMentionGraphEdge> edgeDepComparator = (o1, o2) -> o1.getDep() - o2.getDep();

    public void setFeatureVector(FeatureVector featureVector) {
        this.featureVector = featureVector;
    }

//    public EdgeType getEdgeType() {
//        return edgeType;
//    }

    public NodeKey getDepKey() {
        return depKey;
    }

    public NodeKey getGovKey() {
        return govKey;
    }

    public String toString() {
        return String.format("Label edge: [T:%s,R:%s]-[T:%s,R:%s], Gold Type: %s",
                govKey.getMentionType(), govKey.getRealis(), depKey.getMentionType(), depKey.getRealis(),
                actualEdgeType);
    }

    public void setActualEdgeType(EdgeType actualEdgeType) {
        this.actualEdgeType = actualEdgeType;
    }

    public EdgeType getActualEdgeType() {
        return actualEdgeType;
    }

    public boolean notNoneType() {
        return actualEdgeType != null;
    }

    public MentionGraphEdge getHostingEdge() {
        return hostingEdge;
    }

}
