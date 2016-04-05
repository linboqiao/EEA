package edu.cmu.cs.lti.event_coref.model.graph;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.FeatureVector;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MentionCandidate.DecodingResult;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    private static final long serialVersionUID = -4402367559083671750L;

//    private LabelledMentionGraphEdge realLabelledEdge;

    // Features are expensive, only initialized when needed.
    private TObjectDoubleMap<String> rawFeaturesLabelIndependent;

    private Table<DecodingResult, DecodingResult, LabelledMentionGraphEdge> typedEdges;

    private List<LabelledMentionGraphEdge> realLabelledEdges;

    private final MentionGraph hostingGraph;
    private final int govIdx;
    private final int depIdx;

    public enum EdgeType {Root, Coreference}

    private PairFeatureExtractor extractor;

    private boolean averageMode;

//    public MentionGraphEdge(MentionGraph graph, PairFeatureExtractor extractor, int gov, int dep, boolean averageMode) {
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

        realLabelledEdges = new ArrayList<>();
//        if (realGovKey != null && realDepKey != null && edgeType != null) {
//            LabelledMentionGraphEdge edge = createLabelledEdge(candidates, edgeType, realGovKey, realDepKey);
//            edge.setActualEdgeType(edgeType);
//            realLabelledEdges.add(edge);
//        }
    }

    void addRealEdge(List<MentionCandidate> candidates, DecodingResult realGovKey, DecodingResult realDepKey,
                     EdgeType edgeType) {
        LabelledMentionGraphEdge edge = createLabelledEdge(candidates, edgeType, realGovKey, realDepKey);
        edge.setActualEdgeType(edgeType);
        realLabelledEdges.add(edge);
    }

    private void extractNoLabelFeatures(List<MentionCandidate> mentions) {
        int depMentionIndex = hostingGraph.getCandidateIndex(depIdx);
        int govMentionIndex = hostingGraph.getCandidateIndex(govIdx);

        rawFeaturesLabelIndependent = new TObjectDoubleHashMap<>();

        if (isRoot()) {
            MentionCandidate mention = mentions.get(depMentionIndex);
            extractor.extract(mention, rawFeaturesLabelIndependent);
        } else {
            extractor.extract(mentions, govMentionIndex, depMentionIndex, rawFeaturesLabelIndependent);
        }
    }

    /**
     * Some features are independent of the nodes, so we store their shared copy, and extract once for each edge.
     *
     * @param mentions
     * @return
     */
    public TObjectDoubleMap<String> getNodeIndependentFeatures(List<MentionCandidate> mentions) {
        if (rawFeaturesLabelIndependent == null) {
            extractNoLabelFeatures(mentions);
        }
        return rawFeaturesLabelIndependent;
    }

    public TObjectDoubleMap<String> extractNodeDependentFeatures(List<MentionCandidate> mentions) {
        int depMentionIdx = hostingGraph.getCandidateIndex(depIdx);
        int govMentionIdx = hostingGraph.getCandidateIndex(govIdx);

        TObjectDoubleMap<String> rawFeaturesNodeDependent = new TObjectDoubleHashMap<>();

        if (isRoot()) {
            MentionCandidate mention = mentions.get(depMentionIdx);
            extractor.extractCandidateRelated(mention, rawFeaturesNodeDependent);
        } else {
            extractor.extractCandidateRelated(mentions, govMentionIdx, depMentionIdx, rawFeaturesNodeDependent);
        }

        return rawFeaturesNodeDependent;
    }

    public LabelledMentionGraphEdge getLabelledEdge(List<MentionCandidate> mentions, DecodingResult govKey,
                                                    DecodingResult depKey) {
        if (typedEdges.contains(govKey, depKey)) {
            return typedEdges.get(govKey, depKey);
        } else {
            return createLabelledEdge(mentions, getRealEdgeType(), govKey, depKey);
        }
    }

    private LabelledMentionGraphEdge createLabelledEdge(List<MentionCandidate> mentions, EdgeType edgeType,
                                                        DecodingResult govKey, DecodingResult depKey) {
        LabelledMentionGraphEdge newEdge = new LabelledMentionGraphEdge(this, govKey, depKey, averageMode, edgeType);
        typedEdges.put(govKey, depKey, newEdge);

        TObjectDoubleMap<String> rawFeaturesNeedLabel = extractNodeDependentFeatures(mentions);
        TObjectDoubleMap<String> rawFeaturesNoLabel = getNodeIndependentFeatures(mentions);

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
        if (realLabelledEdges.size() == 0) {
            return null;
        }
        return realLabelledEdges.get(0).getEdgeType();
    }

    public List<LabelledMentionGraphEdge> getRealLabelledEdges() {
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
