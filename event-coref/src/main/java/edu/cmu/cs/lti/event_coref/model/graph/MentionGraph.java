package edu.cmu.cs.lti.event_coref.model.graph;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import org.javatuples.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/1/15
 * Time: 11:30 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionGraph implements Serializable {
    private static final long serialVersionUID = -3451529942657683816L;

    // Each mention is represented as a mention node in the graph. This array is sorted by node comparison, i.e.
    // sorted by their discourse sequence.
    private MentionNode[] mentionNodes;

    // Edge represent a relation from a node to the other indexed from the dependent node to the governing node.
    // For coreference, dependent is always the anaphora (later), governer is its antecedent (earlier).
    // For relations, this dependents on the link direction. Link come form the governer and end at the dependent.
    //
    // This edge can be used to store edge features and labelled scores, and the gold type. For edges that are not in
    // gold standard, the gold type will be null.
    private MentionGraphEdge[][] mentionGraphEdges;

    // Represent each cluster as one array. Each chain is sorted by id within cluster. Chains are sorted by their
    // first element.
    private int[][] corefChains;

    // Represent each relation with a adjacent list.
    private Map<MentionGraphEdge.EdgeType, int[][]> resolvedRelations;

    private boolean useAverage = false;

    /**
     * Now if you only provide a list of mentions, it will automatically enter the testing mode (e.g. using average
     * weights)
     *
     * @param mentions List of event mentions.
     */
    public MentionGraph(List<EventMention> mentions, boolean useAverage) {
        this(mentions, new ArrayList<>());
        this.useAverage = useAverage;
    }

    public MentionGraph(List<EventMention> mentions, List<EventMentionRelation> relations) {
        this.useAverage = false;

        mentionNodes = new MentionNode[mentions.size() + 1];
        // A virtual root, the id should always be 0.
        mentionNodes[0] = new MentionNode(0);
        for (int i = 0; i < mentions.size(); i++) {
            mentionNodes[i + 1] = new MentionNode(i + 1);
        }

        // Read gold standard cluster information.
        // Each cluster is represented as a mapping from the event id to the event mention id list.
        ArrayListMultimap<Integer, Integer> event2Clusters;

        // Group mention nodes into clusters, the first is the event id, the second is the node id.
        event2Clusters = groupEventClusters(mentions);

        corefChains = GraphUtils.createSortedCorefChains(event2Clusters);

        //TODO why this really happen?
//        if (event2Clusters.keySet().size() != corefChains.length) {
//            throw new IllegalStateException("Conversion to chains should retain the number of clusters!");
//        }

        // Edges are 2-d arrays, from current to antecedent. The first node (root), does not have any antecedent, nor
        // link to any other relations. So it is a empty array. Node 0 has no edges.
        mentionGraphEdges = new MentionGraphEdge[mentionNodes.length][];
        for (int curr = 1; curr < mentionNodes.length; curr++) {
            mentionGraphEdges[curr] = new MentionGraphEdge[mentionNodes.length];
        }
        storeCoreferenceEdges();

        // This will store all other relations, which are propagated using the gold clusters.
        resolvedRelations = GraphUtils.resolveRelations(convertToEventRelation(relations), event2Clusters,
                mentionNodes.length);
        storeNonEquivalentEdges();
        linkToRoot();
    }

    /**
     * Store coreference information as graph edges.
     */
    private void storeCoreferenceEdges() {
        for (int[] chain : corefChains) {
            // Within the cluster, link each antecedent with all its anaphora.
            for (int i = 0; i < chain.length - 1; i++) {
                int antecedentId = chain[i];
                for (int j = i + 1; j < chain.length; j++) {
                    int anaphoraId = chain[j];
                    mentionGraphEdges[anaphoraId][antecedentId] = new MentionGraphEdge(this, antecedentId,
                            anaphoraId, useAverage);
                    mentionGraphEdges[anaphoraId][antecedentId].setEdgeType(MentionGraphEdge.EdgeType.Coreference);
                }
            }
        }
    }

    /**
     * Store other non equivalent relations as edges too.
     */
    private void storeNonEquivalentEdges() {
        for (Map.Entry<MentionGraphEdge.EdgeType, int[][]> edgeRelations : resolvedRelations.entrySet()) {
            MentionGraphEdge.EdgeType type = edgeRelations.getKey();
            int[][] adjacentList = edgeRelations.getValue();
            for (int govNodeId = 0; govNodeId < adjacentList.length; govNodeId++) {
                for (int depNodeId : adjacentList[govNodeId]) {
                    mentionGraphEdges[depNodeId][govNodeId] = new MentionGraphEdge(this, govNodeId, depNodeId,
                            useAverage);
                    mentionGraphEdges[depNodeId][govNodeId].setEdgeType(type);
                }
            }
        }
    }

    /**
     * If a node is link to nowhere, link it to root.
     */
    private void linkToRoot() {
        // Loop starts from 1, because node 0 is the root itself.
        for (int nodeIndex = 1; nodeIndex < mentionGraphEdges.length; nodeIndex++) {
            MentionGraphEdge[] antecedentEdges = mentionGraphEdges[nodeIndex];
            boolean hasEdge = false;
            for (MentionGraphEdge antMentionGraphEdge : antecedentEdges) {
                if (antMentionGraphEdge != null) {
                    hasEdge = true;
                }
            }

            if (!hasEdge) {
                antecedentEdges[0] = new MentionGraphEdge(this, 0, nodeIndex, useAverage);
                antecedentEdges[0].setEdgeType(MentionGraphEdge.EdgeType.Root);
            }
        }
    }

    /**
     * Read the stored event cluster information, stored as a map from event index (cluster index) to event mention
     * index, where event mention indices are based on their index in the input list.
     *
     * @return Map from the event index to mention indices it contains.
     */
    private ArrayListMultimap<Integer, Integer> groupEventClusters(List<EventMention> mentions) {
        ArrayListMultimap<Integer, Integer> event2Clusters = ArrayListMultimap.create();
//        for (int i = 1; i < mentionNodes.length; i++) {
        for (MentionNode mentionNode : mentionNodes) {
            if (!mentionNode.isRoot()) {
                EventMention mention = mentions.get(mentionNode.getMentionIndex());
                Event event = mention.getReferringEvent();
                if (event != null) {
                    // This will store all clusters, including singleton clusters.
                    event2Clusters.put(event.getIndex(), mentionNode.getId());
                }
            }
        }
        return event2Clusters;
    }

    /**
     * Convert event mention relation to event relations. Input relations must be transferable ot its corresponding
     * events.
     *
     * @param relations Relations between event mentions.
     * @return Map from edge type to event-event relation.
     */
    private ArrayListMultimap<MentionGraphEdge.EdgeType, Pair<Integer, Integer>> convertToEventRelation(
            List<EventMentionRelation> relations) {
        ArrayListMultimap<MentionGraphEdge.EdgeType, Pair<Integer, Integer>> allRelations = ArrayListMultimap.create();
        for (EventMentionRelation relation : relations) {
            EventMention govMention = relation.getHead();
            EventMention depMention = relation.getChild();
            MentionGraphEdge.EdgeType type = MentionGraphEdge.EdgeType.valueOf(relation.getRelationType());
            allRelations.put(type, Pair.with(govMention.getReferringEvent().getIndex(), depMention
                    .getReferringEvent().getIndex()));
        }
        return allRelations;
    }

    public MentionNode getNode(int index) {
        return mentionNodes[index];
    }

    public MentionNode[] getMentionNodes() {
        return mentionNodes;
    }

    public MentionGraphEdge getMentionGraphEdge(int dep, int gov) {
        if (dep == 0) {
            return null;
        }

        if (mentionGraphEdges[dep][gov] == null) {
            mentionGraphEdges[dep][gov] = new MentionGraphEdge(this, gov, dep, useAverage);
        }

        return mentionGraphEdges[dep][gov];
    }

    public int numNodes() {
        return mentionNodes.length;
    }

    public int[][] getCorefChains() {
        return corefChains;
    }

    public Map<MentionGraphEdge.EdgeType, int[][]> getResolvedRelations() {
        return resolvedRelations;
    }

    public MentionSubGraph getLatentTree(GraphWeightVector weights, PairFeatureExtractor extractor) {
        MentionSubGraph latentTree = new MentionSubGraph(this);
        MentionNode[] mentionNodes = getMentionNodes();

        for (int curr = 1; curr < mentionNodes.length; curr++) {
            Pair<MentionGraphEdge, MentionGraphEdge.EdgeType> bestEdge = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int ant = 0; ant < curr; ant++) {
                MentionGraphEdge goldEdge = mentionGraphEdges[curr][ant];
                if (goldEdge != null) {
                    Pair<MentionGraphEdge.EdgeType, Double> correctLabelScore =
                            goldEdge.getCorrectLabelScore(weights, extractor);
                    if (correctLabelScore == null) {
                        // This is not a gold standard edge.
                        continue;
                    }
                    MentionGraphEdge.EdgeType label = correctLabelScore.getValue0();
                    double score = correctLabelScore.getValue1();

                    if (score > bestScore) {
                        bestEdge = Pair.with(goldEdge, label);
                        bestScore = score;
                    }
                }
            }

            latentTree.addEdge(bestEdge.getValue0(), bestEdge.getValue1());
        }
        return latentTree;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph : \n");

        for (MentionGraphEdge[] mentionGraphEdgeArray : mentionGraphEdges) {
            if (mentionGraphEdgeArray != null) {
                for (MentionGraphEdge mentionGraphEdge : mentionGraphEdgeArray) {
                    if (mentionGraphEdge != null) {
                        sb.append("\t").append(mentionGraphEdge.toString()).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }
}
