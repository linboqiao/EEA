package edu.cmu.cs.lti.learning.model.graph;

import com.google.common.collect.*;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.learning.model.NodeKey;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/1/15
 * Time: 11:30 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionGraph implements Serializable {
    private static final long serialVersionUID = -3451529942657683816L;

    protected transient final Logger logger = LoggerFactory.getLogger(getClass());

    // Extractor must be passed in if deserialized.
    private transient PairFeatureExtractor extractor;

    // Edge represent a relation from a node to the other indexed from the dependent node to the governing node.
    // For coreference, dependent is always the anaphora (later), governer is its antecedent (earlier).
    // For relations, this dependents on the link direction. Link come form the governer and end at the dependent.
    //
    // This edge can be used to store edge features and labelled scores, and the gold type. For edges that are not in
    // gold standard, the gold type will be null.
    private MentionGraphEdge[][] graphEdges;

    // Represent each cluster as one array. Each chain is sorted by id within cluster. Chains are sorted by their
    // first element.
    // This chain is used to store the gold standard coreference chains during training. Decoding chains are obtained
    // via a similar field in the subgraph.
    private List<Pair<Integer, String>>[] typedCorefChains;

    // Represent each relation with a adjacent list, relations are for unlabelled links between graph nodes.
    private Map<EdgeType, ListMultimap<Integer, Integer>> resolvedRelations;

    private boolean useAverage = false;

    private final int rootIndex = 0;

    private final int numNodes;

    /**
     * Provide to the graph with only a list of mentions, no coreference information.
     */
    public MentionGraph(List<MentionCandidate> mentions, PairFeatureExtractor extractor, boolean useAverage) {
        this(mentions, new int[0][], new String[0], new int[0], HashBasedTable.create(), extractor, false);
        this.useAverage = useAverage;
    }

    /**
     * Provide to the graph a list of mentions and predefined event relations. Nodes without these relations will be
     * link to root implicitly.
     *
     * @param candidates         List of candidates for linking. Each candidate corresponds to a unique span.
     * @param candidate2Mentions Map from candidate to its labelled version, one candidate can have multiple
     *                           labelled counter part.
     * @param mentionTypes       Type for each labelled candidate.
     * @param mention2EventIndex Event for each labelled candidate.
     * @param spanRelations      Relation between candidates (unlabelled).
     * @param extractor          The feature extractor.
     */
    public MentionGraph(List<MentionCandidate> candidates, int[][] candidate2Mentions, String[] mentionTypes,
                        int[] mention2EventIndex, Table<Integer, Integer, String> spanRelations,
                        PairFeatureExtractor extractor, boolean isTraining) {
        if (extractor == null) {
            logger.error("The extractor is not initialized.");
        }

        this.extractor = extractor;
        numNodes = candidates.size() + 1;

        // Initialize the edges.
        // Edges are 2-d arrays, from current to antecedent. The first node (root), does not have any antecedent,
        // nor link to any other relations. So it is a empty array: Node 0 has no edges.
        graphEdges = new MentionGraphEdge[numNodes][];
        for (int curr = 1; curr < numNodes; curr++) {
            graphEdges[curr] = new MentionGraphEdge[numNodes];
            for (int ant = 0; ant < curr; ant++) {
                graphEdges[curr][ant] = new MentionGraphEdge(this, extractor, ant, curr, useAverage);
            }
        }

        // Each cluster is represented as a mapping from the event id to the event mention id list.
        SetMultimap<Integer, Pair<Integer, String>> typedNodeClusters =
                groupEventClusters(mention2EventIndex, candidate2Mentions, mentionTypes);

        // Group mention nodes into clusters, the first is the event id, the second is the node id.
        typedCorefChains = GraphUtils.createSortedCorefChains(typedNodeClusters);

        // This step consider use each span as the minimum unit. So it puts spans into clusters. Roughly, this
        // clustering actually represents "simultaneous" relation. Since if a span corresponds to two mentions,
        // these two mentions are mostly happening simultaneously (at least according to the current annotation
        // scheme).
        Multimap<Integer, Integer> nodeClusters = relaxCoreference(mention2EventIndex, candidate2Mentions);

        // This will store all other relations, which are propagated using the gold clusters.
        resolvedRelations = GraphUtils.resolveRelations(
                convertToEventRelation(spanRelations, candidate2Mentions, mention2EventIndex), nodeClusters);


        if (isTraining) {
            this.useAverage = false;

            Set<NodeKey> keysWithAntecedents = storeCoreferenceEdges(candidates);
            TIntObjectMap<EdgeType> nodesWithAntecedents = storeUnlabelledEdges();
            cleanupEdges(candidates);

            // Link lingering nodes to root.
            // We consider nodes that do not directly connecting to another via an unlabelled edge, or sub node keys
            // that do not connect to other node keys as lingering.
            linkToRoot(candidates, keysWithAntecedents, nodesWithAntecedents);
        } else {
            this.useAverage = true;
        }
    }

    private Multimap<Integer, Integer> relaxCoreference(int[] mention2EventIndex, int[][] candidate2Mentions) {
        Multimap<Integer, Integer> event2NodeId = ArrayListMultimap.create();
        // Put candidates into cluster first.
        for (int candidateId = 0; candidateId < candidate2Mentions.length; candidateId++) {
            for (int mentionId : candidate2Mentions[candidateId]) {
                int eventId = mention2EventIndex[mentionId];
                event2NodeId.put(eventId, getNodeIndex(candidateId));
            }
        }
        return event2NodeId;
    }

    private MentionGraphEdge getEdge(int curr, int ant) {
        MentionGraphEdge edge;
        if (graphEdges[curr][ant] == null) {
            edge = new MentionGraphEdge(this, extractor, ant, curr, useAverage);
            graphEdges[curr][ant] = edge;
        } else {
            edge = graphEdges[curr][ant];
        }
        return edge;
    }

    private MentionGraphEdge createUnlabelledGoldEdge(int gov, int dep, EdgeType edgeType) {
        MentionGraphEdge goldEdge = getEdge(dep, gov);
        goldEdge.setRealUnlabelledType(edgeType);
        return goldEdge;
    }

    private MentionGraphEdge createLabelledGoldEdge(int gov, int dep, NodeKey realGovKey,
                                                    NodeKey realDepKey, List<MentionCandidate> candidates,
                                                    EdgeType edgeType) {
        MentionGraphEdge goldEdge = getEdge(dep, gov);
        goldEdge.addRealLabelledEdge(candidates, realGovKey, realDepKey, edgeType);
        return goldEdge;
    }

    /**
     * One problem in this multi type edge graph is that we don't want edges to conflict each other. However, even
     * the annotations contains errors: some coreference links are actually subevents. In this function we clear up
     * such conflicts.
     */
    private void cleanupEdges(List<MentionCandidate> candidates) {
        for (int dep = 2; dep < graphEdges.length; dep++) {
            MentionKey depKeys = candidates.get(getCandidateIndex(dep)).asKey();
            for (int gov = 1; gov < graphEdges[dep].length; gov++) {
                MentionKey govKeys = candidates.get(getCandidateIndex(gov)).asKey();
                MentionGraphEdge edge = getMentionGraphEdge(dep, gov);

                // If there is a unlabelled type for this link, clear all the labelled links. (unlabelled overwrites)
                if (edge.hasGoldUnlabelledType()) {
                    for (NodeKey depKey : depKeys) {
                        for (NodeKey govKey : govKeys) {
                            LabelledMentionGraphEdge labelledEdge = edge.getLabelledEdge(candidates, govKey, depKey);
                            if (labelledEdge.notNoneType()) {
                                labelledEdge.setActualEdgeType(null);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Store unlabelled edges, such as After and Subevent.
     */
    private TIntObjectMap<EdgeType> storeUnlabelledEdges() {
        TIntObjectMap<EdgeType> nodesWithAntecedents = new TIntObjectHashMap<>();

        for (Map.Entry<EdgeType, ListMultimap<Integer, Integer>> typedRelations : resolvedRelations.entrySet()) {
            EdgeType type = typedRelations.getKey();
            ListMultimap<Integer, Integer> adjacentLists = typedRelations.getValue();
            for (Map.Entry<Integer, Collection<Integer>> adjList : adjacentLists.asMap().entrySet()) {
                int in = adjList.getKey();
                for (int out : adjList.getValue()) {
                    createUnlabelledGoldEdge(in, out, type);
                    nodesWithAntecedents.put(out, type);
                }
            }
        }

        return nodesWithAntecedents;
    }

    /**
     * Store coreference information as graph edges.
     */
    private Set<NodeKey> storeCoreferenceEdges(List<MentionCandidate> candidates) {
        Set<NodeKey> keysWithAntecedents = new HashSet<>();
        for (List<Pair<Integer, String>> typedCorefChain : typedCorefChains) {
            // Go through the chain, and find out which NodeKeys are in the chain after considering the type.
            List<NodeKey> actualCorefNodes = new ArrayList<>();

            for (int i = 0; i < typedCorefChain.size(); i++) {
                Pair<Integer, String> element = typedCorefChain.get(i);
                int nodeId = element.getLeft();
                int candidateIndex = getCandidateIndex(nodeId);
                String mentionType = element.getRight();

                MentionKey candidateKeys = candidates.get(candidateIndex).asKey();

                NodeKey actualNode = null;
                for (NodeKey key : candidateKeys) {
                    if (key.getMentionType().equals(mentionType)) {
                        actualNode = key;
                        break;
                    }
                }
                actualCorefNodes.add(actualNode);
            }

            // Within the cluster, link each antecedent with all its anaphora.
            for (int i = 0; i < typedCorefChain.size() - 1; i++) {
                int antecedentNodeId = typedCorefChain.get(i).getLeft();
                NodeKey actualAntecedent = actualCorefNodes.get(i);
                for (int j = i + 1; j < typedCorefChain.size(); j++) {
                    int anaphoraNodeId = typedCorefChain.get(j).getLeft();
                    NodeKey actualAnaphora = actualCorefNodes.get(j);

                    createLabelledGoldEdge(antecedentNodeId, anaphoraNodeId, actualAntecedent, actualAnaphora,
                            candidates, EdgeType.Coreference);

                    keysWithAntecedents.add(actualAnaphora);
                }
            }
        }
        return keysWithAntecedents;
    }

    /**
     * If a node is link to nowhere, link it to root.
     */
    private void linkToRoot(List<MentionCandidate> candidates, Set<NodeKey> keysWithAntecedents,
                            TIntObjectMap<EdgeType> nodesWithAntecedents) {
        // Loop starts from 1, because node 0 is the root itself.
        for (int curr = 1; curr < numNodes(); curr++) {
            if (nodesWithAntecedents.containsKey(curr)) {
                continue;
            }

            MentionKey depKeys = candidates.get(getCandidateIndex(curr)).asKey();
            NodeKey rootKey = MentionKey.rootKey().takeFirst();

            // We assume no two mention contains the same depKey in input.
            for (NodeKey depKey : depKeys) {
                if (!keysWithAntecedents.contains(depKey)) {
                    createLabelledGoldEdge(0, curr, rootKey, depKey, candidates, EdgeType.Coref_Root);
                }
            }
        }
    }

    /**
     * Read the stored event cluster information, stored as a map from event index (cluster index) to mention node
     * index, where event mention indices are based on their index in the input list. Note that mention node index is
     * not the same to mention index because it include artificial nodes (e.g. Root).
     */
    private SetMultimap<Integer, Pair<Integer, String>> groupEventClusters(int[] mention2EventIndex,
                                                                           int[][] candidate2MentionIndex,
                                                                           String[] mentionTypes) {
        SetMultimap<Integer, Pair<Integer, String>> nodeClusters = HashMultimap.create();

        for (int nodeIndex = 0; nodeIndex < numNodes(); nodeIndex++) {
            if (!isRoot(nodeIndex)) {
                int candidateIndex = getCandidateIndex(nodeIndex);
                // A candidate can be mapped to multiple mentions (due to multi-tagging).
                // To handle multi-tagging, we represent each element as a pair of node and the type.
                for (int mentionIndex : candidate2MentionIndex[candidateIndex]) {
                    int eventIndex = mention2EventIndex[mentionIndex];
                    nodeClusters.put(eventIndex, Pair.of(nodeIndex, mentionTypes[mentionIndex]));
                }
            }
        }
        return nodeClusters;
    }

    /**
     * Convert event mention relation to event relations. Input relations must be transferable to its corresponding
     * events.
     *
     * @param spanRelations Relations between event spans.
     * @return Map from edge type to event-event relation.
     */
    private HashMultimap<EdgeType, Pair<Integer, Integer>> convertToEventRelation(
            Table<Integer, Integer, String> spanRelations, int[][] span2mention, int[] mention2EventIndex) {
        HashMultimap<EdgeType, Pair<Integer, Integer>> allRelations = HashMultimap.create();

        for (Table.Cell<Integer, Integer, String> relation : spanRelations.cellSet()) {
            int[] govMentions = span2mention[relation.getRowKey()];
            int[] depMentions = span2mention[relation.getColumnKey()];
            EdgeType type = EdgeType.valueOf(relation.getValue());

            for (int govMention : govMentions) {
                for (int depMention : depMentions) {
                    allRelations.put(type, Pair.of(mention2EventIndex[govMention], mention2EventIndex[depMention]));
                }
            }
        }
        return allRelations;
    }

    public MentionGraphEdge getMentionGraphEdge(int dep, int gov) {
        if (dep == 0) {
            return null;
        }
        return getEdge(dep, gov);
    }

    public synchronized LabelledMentionGraphEdge getLabelledEdge(List<MentionCandidate> mentions, NodeKey govKey,
                                                                 NodeKey depKey) {
        return getEdge(getNodeIndex(depKey.getCandidateIndex()), getNodeIndex(govKey.getCandidateIndex()))
                .getLabelledEdge(mentions, govKey, depKey);
    }

    public int numNodes() {
        return numNodes;
    }

    public List<Pair<Integer, String>>[] getNodeCorefChains() {
        return typedCorefChains;
    }

    public Map<EdgeType, ListMultimap<Integer, Integer>> getResolvedRelations() {
        return resolvedRelations;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph : \n");

        for (MentionGraphEdge[] mentionGraphEdgeArray : graphEdges) {
            if (mentionGraphEdgeArray != null) {
                for (MentionGraphEdge mentionGraphEdge : mentionGraphEdgeArray) {
                    if (mentionGraphEdge != null) {
                        if (mentionGraphEdge.hasGoldUnlabelledType() || mentionGraphEdge.hasRealLabelledEdge()) {
                            sb.append("\t").append(mentionGraphEdge).append("\n");
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public void setExtractor(PairFeatureExtractor extractor) {
        this.extractor = extractor;
    }

    public static int getCandidateIndex(int nodeIndex) {
        // Under the current implementation, we only have an additional root node.
        return nodeIndex - 1;
    }

    public static int getNodeIndex(int candidateIndex) {
        return candidateIndex + 1;
    }

    public boolean isRoot(int nodeIndex) {
        return nodeIndex == rootIndex;
    }

//    public boolean hasAntecedent(NodeKey anaphoraNode) {
//        return keysWithAntecedents.contains(anaphoraNode);
//    }
}
