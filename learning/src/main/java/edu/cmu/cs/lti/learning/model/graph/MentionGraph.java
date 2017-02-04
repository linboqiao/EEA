package edu.cmu.cs.lti.learning.model.graph;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.learning.model.NodeKey;
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

//    // Extractor must be passed in if deserialized.
//    private transient PairFeatureExtractor extractor;

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
    private List<NodeKey>[] keyCorefChains;

    // Represent each relation with a adjacent list. Each vertex in the relation is a NodeKey,
    // which uniquely point to one mention in the node.
    private Map<EdgeType, Set<Pair<NodeKey, NodeKey>>> resolvedRelations;

    private Map<EdgeType, Map<Integer, List<Integer>>> closureEventRelations;
    private Map<EdgeType, Map<Integer, List<Integer>>> reducedEventRelations;

    private SetMultimap<Integer, NodeKey> event2NodeKeys;

    private boolean useAverage = false;

    private final int rootIndex = 0;

    private final int numNodes;

    // Store gold mention types.
    private String[] mentionTypes;

    /**
     * Provide to the graph with only a list of mentions, no coreference information.
     */
    public MentionGraph(List<MentionCandidate> mentions, PairFeatureExtractor extractor, boolean useAverage) {
        this(mentions, new int[0][], new int[0], HashBasedTable.create(), extractor, false, false);
        this.useAverage = useAverage;
    }

    /**
     * Provide to the graph a list of mentions and predefined event relations. Nodes without these relations will be
     * link to root implicitly.
     *
     * @param candidates         List of candidates for linking. Each candidate corresponds to a unique span.
     * @param candidate2Mentions Map from candidate to its labelled version, one candidate can have multiple
     *                           labelled counter part.
     * @param mention2EventIndex Event for each labelled candidate.
     * @param relations          Relation between mentions (labelled candidate).
     * @param extractor          The feature extractor.
     */
    public MentionGraph(List<MentionCandidate> candidates, int[][] candidate2Mentions,
                        int[] mention2EventIndex, Table<Integer, Integer, String> relations,
                        PairFeatureExtractor extractor, boolean isTraining, boolean hasGold) {
//        this.extractor = extractor;
        numNodes = candidates.size() + 1;

        // Initialize the edges.
        // Edges are 2-d arrays, from current to antecedent. The first node (root), does not have any antecedent,
        // nor link to any other relations. So it is a empty array: Node 0 has no edges.
        graphEdges = new MentionGraphEdge[numNodes][];
        for (int curr = 1; curr < numNodes; curr++) {
            graphEdges[curr] = new MentionGraphEdge[numNodes];
            for (int ant = 0; ant <= curr; ant++) {
                graphEdges[curr][ant] = new MentionGraphEdge(this, extractor, ant, curr, useAverage);
            }
        }

        // Each cluster is represented as a mapping from the event id to the node keys.
        event2NodeKeys = groupEventClusters(mention2EventIndex, candidate2Mentions, candidates);

        // Group mention nodes into clusters, the first is the event id, the second is the node id.
        keyCorefChains = GraphUtils.createSortedCorefChains(event2NodeKeys);

        // This will store all other relations, which are propagated using the gold clusters.
        HashMultimap<EdgeType, Pair<Integer, Integer>> eventRelations = asEventRelations(relations,
                mention2EventIndex);
        reducedEventRelations = new HashMap<>();
        closureEventRelations = new HashMap<>();

        for (Map.Entry<EdgeType, Collection<Pair<Integer, Integer>>> relationsByType : eventRelations
                .asMap().entrySet()) {
            EdgeType type = relationsByType.getKey();

            Map<Integer, List<Integer>> closure = GraphUtils.transitiveClosure(relationsByType.getValue());
            closureEventRelations.put(type, closure);

            Map<Integer, List<Integer>> reduction = GraphUtils.transitiveReduction(closure);
            reducedEventRelations.put(type, reduction);
        }
        resolvedRelations = GraphUtils.resolveRelations(eventRelations, event2NodeKeys);

        if (isTraining) {
            if (extractor == null) {
                throw new IllegalArgumentException("The feature extractor is not initialized.");
            }

            this.useAverage = false;

            Map<EdgeType, Set<NodeKey>> allKeysWithAntecedents = new HashMap<>();

            for (EdgeType edgeType : EdgeType.getNormalTypes()) {
                allKeysWithAntecedents.put(edgeType, new HashSet<>());
            }

            storeCoreferenceEdges(candidates, allKeysWithAntecedents);

            storeRelations(candidates, allKeysWithAntecedents);

            // Link lingering nodes to root.
            // We consider nodes that do not directly connecting to another via an unlabelled edge, or sub node keys
            // that do not connect to other node keys as lingering.
            linkToRoot(candidates, allKeysWithAntecedents);
        } else {
            this.useAverage = true;
        }
    }

    public Map<EdgeType, Map<Integer, List<Integer>>> getClosureEventRelations() {
        return closureEventRelations;
    }

    public Map<EdgeType, Map<Integer, List<Integer>>> getReducedEventRelations() {
        return reducedEventRelations;
    }

    public SetMultimap<Integer, NodeKey> getEvent2NodeKeys() {
        return event2NodeKeys;
    }

    public MentionGraphEdge getEdge(int gov, int dep) {
        int precedent = Math.min(dep, gov);
        int succedent = Math.max(dep, gov);

        if (succedent == 0) {
            // No link available for the first node.
            return null;
        } else {
            return graphEdges[succedent][precedent];
        }
    }


    private NodeKey getKeyByIndex(List<MentionCandidate> candidates, int index, int nthKey) {
        return candidates.get(index).asKey().getKeys().get(nthKey);
    }

    private MentionGraphEdge createLabelledGoldEdge(NodeKey realGovKey, NodeKey realDepKey,
                                                    List<MentionCandidate> candidates, EdgeType edgeType) {
        int gov = realGovKey.getNodeIndex();
        int dep = realDepKey.getNodeIndex();
        MentionGraphEdge goldEdge = getEdge(gov, dep);

        goldEdge.addRealLabelledEdge(candidates, realGovKey, realDepKey, edgeType);
        return goldEdge;
    }

    /**
     * Store unlabelled edges, such as After and Subevent.
     */
    private void storeRelations(List<MentionCandidate> candidates, Map<EdgeType, Set<NodeKey>> allKeysWithAntecedents) {
        for (Map.Entry<EdgeType, Set<Pair<NodeKey, NodeKey>>> typedRelations : resolvedRelations.entrySet()) {
            EdgeType type = typedRelations.getKey();
            Set<Pair<NodeKey, NodeKey>> adjacentLists = typedRelations.getValue();
            for (Pair<NodeKey, NodeKey> relation : adjacentLists) {
                NodeKey inKey = relation.getKey();
                NodeKey outKey = relation.getValue();
                MentionGraphEdge e = createLabelledGoldEdge(inKey, outKey, candidates, type);

                NodeKey latterKey = outKey.compareTo(inKey) > 0 ? outKey : inKey;
                allKeysWithAntecedents.get(type).add(latterKey);
            }
        }
    }

    /**
     * Store coreference information as graph edges.
     */
    private void storeCoreferenceEdges(List<MentionCandidate> candidates,
                                       Map<EdgeType, Set<NodeKey>> allKeysWithAntecedents) {

        for (List<NodeKey> keyCorefChain : keyCorefChains) {
            // Within the cluster, link each antecedent with all its anaphora.
            for (int i = 0; i < keyCorefChain.size() - 1; i++) {
                NodeKey actualAntecedent = keyCorefChain.get(i);
                for (int j = i + 1; j < keyCorefChain.size(); j++) {
                    NodeKey actualAnaphora = keyCorefChain.get(j);
                    createLabelledGoldEdge(actualAntecedent, actualAnaphora, candidates, EdgeType.Coreference);
                    allKeysWithAntecedents.get(EdgeType.Coreference).add(actualAnaphora);
                }
            }
        }
    }

    /**
     * If a node is link to nowhere, link it to root.
     */
    private void linkToRoot(List<MentionCandidate> candidates, Map<EdgeType, Set<NodeKey>> allKeysWithAntecedents) {
        // Loop starts from 1, because node 0 is the root itself.
        for (int curr = 1; curr < numNodes(); curr++) {
            MentionKey depKeys = candidates.get(getCandidateIndex(curr)).asKey();

            // We assume no two mention contains the same depKey in input.
            for (NodeKey depKey : depKeys) {
                for (EdgeType edgeType : EdgeType.getNormalTypes()) {
                    if (!allKeysWithAntecedents.get(edgeType).contains(depKey)) {
                        createLabelledGoldEdge(NodeKey.getRootKey(edgeType), depKey, candidates, EdgeType.Root);
                    }
                }
            }
        }
    }

    /**
     * Read the stored event cluster information, stored as a map from event index (cluster index) to mention node
     * index, where event mention indices are based on their index in the input list. Note that mention node index is
     * not the same to mention index because it include artificial nodes (e.g. Root).
     */
    private SetMultimap<Integer, NodeKey> groupEventClusters(int[] mention2EventIndex,
                                                             int[][] candidate2MentionIndex,
                                                             List<MentionCandidate> candidates) {
        SetMultimap<Integer, NodeKey> nodeClusters = HashMultimap.create();

        if (mention2EventIndex.length > 0) {
            for (int nodeIndex = 0; nodeIndex < numNodes(); nodeIndex++) {
                if (!isRoot(nodeIndex)) {
                    int candidateIndex = getCandidateIndex(nodeIndex);
                    List<NodeKey> keys = candidates.get(candidateIndex).asKey().getKeys();

                    // A candidate can be mapped to multiple mentions (due to multi-tagging).
                    // To handle multi-tagging, we represent each element as a pair of node and the type.
                    for (int nthMention = 0; nthMention < candidate2MentionIndex[candidateIndex].length; nthMention++) {
                        int mentionIndex = candidate2MentionIndex[candidateIndex][nthMention];
                        int eventIndex = mention2EventIndex[mentionIndex];
                        nodeClusters.put(eventIndex, keys.get(nthMention));
                    }
                }
            }
        }
        return nodeClusters;
    }

    /**
     * Convert event mention relation to event relations. Input relations must be transferable to its corresponding
     * events.
     *
     * @param mentionRelations Relations between event mentions.
     * @return Map from edge type to event-event relation.
     */
    private HashMultimap<EdgeType, Pair<Integer, Integer>> asEventRelations(
            Table<Integer, Integer, String> mentionRelations, int[] mention2EventIndex) {
        HashMultimap<EdgeType, Pair<Integer, Integer>> allRelations = HashMultimap.create();

        for (Table.Cell<Integer, Integer, String> relation : mentionRelations.cellSet()) {
            EdgeType type = EdgeType.valueOf(relation.getValue());
            int gov = mention2EventIndex[relation.getRowKey()];
            int dep = mention2EventIndex[relation.getColumnKey()];
            allRelations.put(type, Pair.of(gov, dep));
        }
        return allRelations;
    }

    public synchronized LabelledMentionGraphEdge getLabelledEdge(List<MentionCandidate> mentions, NodeKey govKey,
                                                                 NodeKey depKey) {
        return getEdge(govKey.getNodeIndex(), depKey.getNodeIndex()).getLabelledEdge(mentions, govKey, depKey);
    }

    public int numNodes() {
        return numNodes;
    }

    public List<NodeKey>[] getNodeCorefChains() {
        return keyCorefChains;
    }

    public Map<EdgeType, Set<Pair<NodeKey, NodeKey>>> getResolvedRelations() {
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
