package edu.cmu.cs.lti.learning.model.graph;

import com.google.common.base.Joiner;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/2/16
 * Time: 11:31 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionGraphEdge implements Serializable {
    private static final long serialVersionUID = -4402367559083671750L;

    // Features are expensive, only initialized when needed.
    private FeatureVector nodeAgnosticFeatures;

    // Labelled edges consider also the type of the node of the edge. One edge can have multiple labelled edges.
    private Table<NodeKey, NodeKey, LabelledMentionGraphEdge> labelledEdges;

    // Gov key, dep Key, the actual edge.
    private Table<NodeKey, NodeKey, LabelledMentionGraphEdge> realLabelledEdges;

    private final MentionGraph hostingGraph;
    private final int govIdx;
    private final int depIdx;

    private PairFeatureExtractor extractor;

    private boolean averageMode;

    private boolean hasRealLabelledEdge = false;

    // Currently using NULL to represent a NONE type.
    private EdgeType realUnlabelledType;

    public MentionGraphEdge(MentionGraph graph, PairFeatureExtractor extractor,
                            int gov, int dep, boolean averageMode) {
        this.hostingGraph = graph;
        this.averageMode = averageMode;
        this.labelledEdges = HashBasedTable.create();
        this.extractor = extractor;

        this.govIdx = gov;
        this.depIdx = dep;

        realLabelledEdges = HashBasedTable.create();
    }

    public void setRealUnlabelledType(EdgeType realUnlabelledType) {
        this.realUnlabelledType = realUnlabelledType;
    }

    public boolean hasGoldUnlabelledType() {
        return realUnlabelledType != null;
    }

    public boolean hasRealLabelledEdge() {
        return hasRealLabelledEdge;
    }

    public void addRealLabelledEdge(List<MentionCandidate> candidates, NodeKey realGovKey, NodeKey realDepKey,
                                    EdgeType edgeType) {
        LabelledMentionGraphEdge edge = createLabelledEdge(candidates, realGovKey, realDepKey);
        edge.setActualEdgeType(edgeType);
        realLabelledEdges.put(edge.getGovKey(), edge.getDepKey(), edge);
        hasRealLabelledEdge = true;
    }

    /**
     * Call this to extract features. This must be done actively by the caller.
     *
     * @param mentions
     */
    public void extractNodeAgnosticFeatures(List<MentionCandidate> mentions) {
        if (nodeAgnosticFeatures != null) {
            return;
        }

        int depCandidateIndex = MentionGraph.getCandidateIndex(depIdx);

        TObjectDoubleMap<String> rawFeaturesNodeAgnostic = new TObjectDoubleHashMap<>();

        if (isRoot()) {
            MentionCandidate mention = mentions.get(depCandidateIndex);
            extractor.extract(mention, rawFeaturesNodeAgnostic);
        } else {
            int govCandidateId = MentionGraph.getCandidateIndex(govIdx);

            extractor.extract(mentions, govCandidateId, depCandidateIndex, rawFeaturesNodeAgnostic);
        }

        nodeAgnosticFeatures = extractor.newFeatureVector();

        rawFeaturesNodeAgnostic.forEachEntry((featureName, featureValue) -> {
            nodeAgnosticFeatures.addFeature(featureName, featureValue);
            return true;
        });
    }

    /**
     * Some features are independent of the nodes, so we store their shared copy, and extract once for each edge.
     *
     * @return Extracted feature vector.
     */
    public FeatureVector getNodeAgnosticFeatures() {
        return nodeAgnosticFeatures;
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
            extractor.extractLabelRelated(mention, depKey, rawFeaturesNodeDependent);
        } else {
            extractor.extractLabelRelated(mentions, govKey, depKey, rawFeaturesNodeDependent);
        }

        return rawFeaturesNodeDependent;
    }

    public LabelledMentionGraphEdge getLabelledEdge(List<MentionCandidate> mentions, NodeKey govKey, NodeKey depKey) {
        if (labelledEdges.contains(govKey, depKey)) {
            return labelledEdges.get(govKey, depKey);
        } else {
            return createLabelledEdge(mentions, govKey, depKey);
        }
    }

    public LabelledMentionGraphEdge getExistingLabelledEdge(NodeKey govKey, NodeKey depKey) {
//        logger.info("getting existing at " + govKey + " " + depKey);
        return labelledEdges.get(govKey, depKey);
    }

    private LabelledMentionGraphEdge createLabelledEdge(List<MentionCandidate> mentions,
                                                        NodeKey govKey, NodeKey depKey) {
        LabelledMentionGraphEdge newEdge = new LabelledMentionGraphEdge(this, govKey, depKey, averageMode);
        labelledEdges.put(govKey, depKey, newEdge);

        TObjectDoubleMap<String> rawFeaturesNodeRelated = extractNodeDependentFeatures(mentions, govKey, depKey);

        FeatureVector featureVector = extractor.newFeatureVector();

        rawFeaturesNodeRelated.forEachEntry((featureName, featureValue) -> {
            featureVector.addFeature(featureName, featureValue);
            return true;
        });

        newEdge.setFeatureVector(featureVector);

        return newEdge;
    }

    public EdgeType getRealUnlabelledEdgeType() {
        return realUnlabelledType;
    }

    public Table<NodeKey, NodeKey, LabelledMentionGraphEdge> getRealLabelledEdges() {
        return realLabelledEdges;
    }


    public String toString() {
        return String.format("Edge: (%d->%d), [Gold Type: %s], Gold Labelled Edges: %s.", govIdx, depIdx,
                realUnlabelledType, Joiner.on(" ; ").join(realLabelledEdges.values()));
    }

    public boolean isRoot() {
        return hostingGraph.isRoot(govIdx);
    }

    public int getGov() {
        return govIdx;
    }

    public int getDep() {
        return depIdx;
    }

    public List<LabelledMentionGraphEdge> getAllLabelledEdges(List<MentionCandidate> candidates) {
        return getAllLabelledEdges(candidates, false);
    }

    public List<LabelledMentionGraphEdge> getAllLabelledEdges(List<MentionCandidate> candidates,
                                                                   boolean reverse) {
        int currentCandidateId = MentionGraph.getCandidateIndex(depIdx);
        int antCandidateId = MentionGraph.getCandidateIndex(govIdx);

        if (reverse) {
            currentCandidateId = MentionGraph.getCandidateIndex(govIdx);
            antCandidateId = MentionGraph.getCandidateIndex(depIdx);
        }

        List<LabelledMentionGraphEdge> labelledEdges = new ArrayList<>();

        for (NodeKey govKey : candidates.get(antCandidateId).asKey()) {
            for (NodeKey depKey : candidates.get(currentCandidateId).asKey()) {
                labelledEdges.add(getLabelledEdge(candidates, govKey, depKey));
            }
        }
        return labelledEdges;

    }
}
