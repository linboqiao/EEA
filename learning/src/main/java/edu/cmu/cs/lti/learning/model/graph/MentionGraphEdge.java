package edu.cmu.cs.lti.learning.model.graph;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/2/16
 * Time: 11:31 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionGraphEdge implements Serializable {
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    private static final long serialVersionUID = -4402367559083671750L;

//    private LabelledMentionGraphEdge realLabelledEdge;

    // Features are expensive, only initialized when needed.
    private TObjectDoubleMap<String> rawFeaturesLabelIndependent;

    private Table<NodeKey, NodeKey, LabelledMentionGraphEdge> typedEdges;

    // Gov key, dep Key, the actual edge.
    private Table<NodeKey, NodeKey, LabelledMentionGraphEdge> realLabelledEdges;

    private final MentionGraph hostingGraph;
    private final int govIdx;
    private final int depIdx;

    private PairFeatureExtractor extractor;

    private boolean averageMode;

    private EdgeType actualEdgeType;

//    public MentionGraphEdge(MentionGraph graph, PairFeatureExtractor extractor, int gov, int dep, boolean
// averageMode) {
//        this(graph, extractor, gov, dep, null, null, null, new ArrayList<>(), averageMode);
//    }

    public MentionGraphEdge(MentionGraph graph, PairFeatureExtractor extractor, int gov, int dep,
                            boolean averageMode) {
        this.hostingGraph = graph;
        this.averageMode = averageMode;
        this.typedEdges = HashBasedTable.create();
        this.extractor = extractor;

        this.govIdx = gov;
        this.depIdx = dep;

        realLabelledEdges = HashBasedTable.create();
    }

    void addRealEdge(List<MentionCandidate> candidates, NodeKey realGovKey, NodeKey realDepKey, EdgeType edgeType) {
        LabelledMentionGraphEdge edge = createLabelledEdge(candidates, realGovKey, realDepKey);
        this.actualEdgeType = edgeType;
//        edge.setActualEdgeType(edgeType);
        realLabelledEdges.put(edge.getGovKey(), edge.getDepKey(), edge);
    }

    private void extractNoLabelFeatures(List<MentionCandidate> mentions, NodeKey govKey, NodeKey depKey) {
        int depMentionIndex = hostingGraph.getCandidateIndex(depIdx);

        rawFeaturesLabelIndependent = new TObjectDoubleHashMap<>();

        if (isRoot()) {
            MentionCandidate mention = mentions.get(depMentionIndex);
            extractor.extract(mention, rawFeaturesLabelIndependent, depKey);
        } else {
            extractor.extract(mentions, govKey, depKey, rawFeaturesLabelIndependent);
        }
    }

    /**
     * Some features are independent of the nodes, so we store their shared copy, and extract once for each edge.
     *
     * @param mentions
     * @return
     */
    public TObjectDoubleMap<String> getNodeIndependentFeatures(List<MentionCandidate> mentions, NodeKey govKey,
                                                               NodeKey depKey) {
        if (rawFeaturesLabelIndependent == null) {
            extractNoLabelFeatures(mentions, govKey, depKey);
        }
        return rawFeaturesLabelIndependent;
    }

    /**
     * Some features are dependent of the node, specifically, depends on the type or realis of the node. Such
     * features should be extracted when needed.
     *
     * @param mentions
     * @param govKey
     * @param depKey
     * @return
     */
    public TObjectDoubleMap<String> extractNodeDependentFeatures(List<MentionCandidate> mentions, NodeKey govKey,
                                                                 NodeKey depKey) {
        int depMentionIdx = hostingGraph.getCandidateIndex(depIdx);

        TObjectDoubleMap<String> rawFeaturesNodeDependent = new TObjectDoubleHashMap<>();

        if (isRoot()) {
            MentionCandidate mention = mentions.get(depMentionIdx);
            extractor.extractLabelRelated(mention, rawFeaturesNodeDependent, depKey);
        } else {
            extractor.extractLabelRelated(mentions, govKey, depKey, rawFeaturesNodeDependent);
        }

        return rawFeaturesNodeDependent;
    }

    public LabelledMentionGraphEdge getLabelledEdge(List<MentionCandidate> mentions, NodeKey govKey, NodeKey depKey) {
        if (typedEdges.contains(govKey, depKey)) {
            return typedEdges.get(govKey, depKey);
        } else {
            return createLabelledEdge(mentions, govKey, depKey);
        }
    }

    public LabelledMentionGraphEdge getExistingLabelledEdge(NodeKey govKey, NodeKey depKey) {
//        logger.info("getting existing at " + govKey + " " + depKey);
        return typedEdges.get(govKey, depKey);
    }

    public void createLabelledEdgeWithFeatures(NodeKey govKey, NodeKey depKey, FeatureVector featureVector) {
        LabelledMentionGraphEdge newEdge = new LabelledMentionGraphEdge(this, govKey, depKey, averageMode);
        newEdge.setFeatureVector(featureVector);
        typedEdges.put(govKey, depKey, newEdge);
    }

    private LabelledMentionGraphEdge createLabelledEdge(List<MentionCandidate> mentions,
                                                        NodeKey govKey, NodeKey depKey) {
//        logger.info("Created labelled edge at " + govKey + " " + depKey);
        LabelledMentionGraphEdge newEdge = new LabelledMentionGraphEdge(this, govKey, depKey, averageMode);
        typedEdges.put(govKey, depKey, newEdge);

        TObjectDoubleMap<String> rawFeaturesNeedLabel = extractNodeDependentFeatures(mentions, govKey, depKey);
        TObjectDoubleMap<String> rawFeaturesNoLabel = getNodeIndependentFeatures(mentions, govKey, depKey);

        FeatureVector featureVector = extractor.newFeatureVector();

        rawFeaturesNoLabel.forEachEntry((featureName, featureValue) -> {
            featureVector.addFeature(featureName, featureValue);
            return true;
        });

        rawFeaturesNeedLabel.forEachEntry((featureName, featureValue) -> {
            featureVector.addFeature(featureName, featureValue);
            return true;
        });

        newEdge.setFeatureVector(featureVector);

        return newEdge;
    }

    public EdgeType getRealEdgeType() {
//        EdgeType type = null;
//        for (Table.Cell<NodeKey, NodeKey, LabelledMentionGraphEdge> edgeCell : realLabelledEdges.cellSet()) {
//            type = edgeCell.getValue().getActualEdgeType();
//        }
        return actualEdgeType;
    }

    public Table<NodeKey, NodeKey, LabelledMentionGraphEdge> getRealLabelledEdges() {
        return realLabelledEdges;
    }


    public String toString() {
        return "Edge: (" + govIdx + ',' + depIdx + ")" + " [Actual Edge: " + getRealEdgeType() + "]";
    }

//    public void addRealLabelledEdge(DecodingResult govKey, DecodingResult depKey, EdgeType edgeType) {
//        realEdge = addLabelledEdge(edgeType, govKey, depKey);
//        typedEdges.put(govKey, depKey, realEdge);
//    }

    public boolean isRoot() {
        return hostingGraph.isRoot(govIdx);
    }

    public int getGovIdx() {
        return govIdx;
    }

    public int getDepIdx() {
        return depIdx;
    }
}
