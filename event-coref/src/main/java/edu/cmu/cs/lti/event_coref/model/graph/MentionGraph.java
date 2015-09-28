package edu.cmu.cs.lti.event_coref.model.graph;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.model.GraphWeightVector;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import org.apache.uima.jcas.JCas;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(MentionGraph.class.getName());

    // Each mention is represented as a mention node in the graph. This array is sorted by node comparison, i.e.
    // sorted by their discourse sequence.
    private MentionNode[] mentionNodes;

    // Edge represent a relation from a node to the other indexed from the dependent node to the governing node.
    // For coreference, dependent is always the anaphora (later), governer is its antecedent (earlier).
    // For relations, this dependents on the link direction. Link come form the governer and end at the dependent.
    private MentionGraphEdge[][] mentionGraphEdges;

    // Each cluster is represented as a mapping from the event id to the event mention id list.
    ArrayListMultimap<Integer, Integer> event2Clusters;

    // Represent each cluster as one array. Each chain is sorted by id within cluster. Chains are sorted by their
    // first element.
    private int[][] corefChains;

    // Represent each relation with a adjacent list.
    private Map<MentionGraphEdge.EdgeType, int[][]> nonEquivalentEdges;

    private FeatureAlphabet featureAlphabet;
    private ClassAlphabet classAlphabet;
    private PairFeatureExtractor extractor;
    private JCas context;

    public MentionGraph(JCas context, List<EventMention> mentions, List<EventMentionRelation> relations, FeatureAlphabet
            featureAlphabet, ClassAlphabet classAlphabet, PairFeatureExtractor extractor) {
        this.featureAlphabet = featureAlphabet;
        this.classAlphabet = classAlphabet;
        this.extractor = extractor;
        this.context = context;

        mentionNodes = new MentionNode[mentions.size() + 1];
        // A virtual root, the id should always be 0.
        mentionNodes[0] = new MentionNode(0);
        for (int i = 0; i < mentions.size(); i++) {
            mentionNodes[i] = new MentionNode(i + 1, mentions.get(i));
        }

        // Read gold standard cluster information.
        event2Clusters = groupEventClusters(mentions);
        corefChains = GraphUtils.createSortedCorefChains(event2Clusters);
        logger.debug(String.format("Number of full clusters : %d", event2Clusters.size()));
        logger.debug(String.format("Number of non-singleton clusters : %d", corefChains.length));

        // Edges is a 2-d array, from current to antecedent. The first node (root), does not have any antecedent,
        // is a empty array. Node 0 has no edges.
        mentionGraphEdges = new MentionGraphEdge[mentionNodes.length][];
        for (int curr = 1; curr < mentionNodes.length; curr++) {
            mentionGraphEdges[curr] = new MentionGraphEdge[curr];
        }
        storeCoreferenceEdges();

        // This will store all other relations, which are propagated using the gold clusters.
        nonEquivalentEdges = GraphUtils.resolveRelations(convertToEventRelation(relations), event2Clusters,
                mentionNodes.length);
        storeNonEquivalentEdges();
        linkToRoot();
    }

    public MentionGraph(JCas context, List<EventMention> mentions, FeatureAlphabet featureAlphabet, ClassAlphabet
            classAlphabet, PairFeatureExtractor extractor) {
        this(context, mentions, new ArrayList<>(), featureAlphabet, classAlphabet, extractor);
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
                    mentionGraphEdges[anaphoraId][antecedentId] = new MentionGraphEdge(this, anaphoraId, antecedentId);
                    mentionGraphEdges[anaphoraId][antecedentId].edgeType = MentionGraphEdge.EdgeType.Coreference;
                }
            }
        }
    }

    /**
     * Store other non equivalent relations as edges too.
     */
    private void storeNonEquivalentEdges() {
        for (Map.Entry<MentionGraphEdge.EdgeType, int[][]> edgeRelations : nonEquivalentEdges.entrySet()) {
            MentionGraphEdge.EdgeType type = edgeRelations.getKey();
            int[][] adjacentList = edgeRelations.getValue();
            for (int govNodeId = 0; govNodeId < adjacentList.length; govNodeId++) {
                for (int depNodeId : adjacentList[govNodeId]) {
                    mentionGraphEdges[depNodeId][govNodeId] = new MentionGraphEdge(this, govNodeId, depNodeId);
                    mentionGraphEdges[depNodeId][govNodeId].edgeType = type;
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
                antecedentEdges[0] = new MentionGraphEdge(this, nodeIndex, 0);
                antecedentEdges[0].edgeType = MentionGraphEdge.EdgeType.Root;
            }
        }
    }

    /**
     * Read the stored event cluster information, stored as a map from event index (cluster index) to event mention
     * index, where event mention indices are based on their index in the input list.
     *
     * @param mentions The list of event mentions.
     * @return Map from the event index to mention indices it contains.
     */
    private ArrayListMultimap<Integer, Integer> groupEventClusters(List<EventMention> mentions) {
        ArrayListMultimap<Integer, Integer> event2Clusters = ArrayListMultimap.create();
        for (int i = 1; i < mentionNodes.length; i++) {
            EventMention mention = mentions.get(i - 1);
            mentionNodes[i] = new MentionNode(i, mention);
            Event event = mention.getReferringEvent();
            // This will store all clusters, including singleton clusters.
            event2Clusters.put(event.getEventIndex(), i);
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
            allRelations.put(type, Pair.with(govMention.getReferringEvent().getEventIndex(), depMention
                    .getReferringEvent().getEventIndex()));
        }
        return allRelations;
    }

    public MentionNode getNode(int index) {
        return mentionNodes[index];
    }

    public MentionNode[] getMentionNodes() {
        return mentionNodes;
    }

    public MentionGraphEdge[][] getMentionGraphEdges() {
        return mentionGraphEdges;
    }

    public int numNodes() {
        return mentionNodes.length;
    }

    public int[][] getCorefChains() {
        return corefChains;
    }

    public Map<MentionGraphEdge.EdgeType, int[][]> getNonEquivalentEdges() {
        return nonEquivalentEdges;
    }

    public MentionSubGraph getLatentTree(GraphWeightVector weights) {
        MentionSubGraph latentTree = new MentionSubGraph(this);
        MentionNode[] mentionNodes = getMentionNodes();

        logger.debug("Decoding the gold latent tree");

        for (int curr = 1; curr < mentionNodes.length; curr++) {
            Pair<MentionGraphEdge, MentionGraphEdge.EdgeType> bestEdge = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int ant = 0; ant < curr; ant++) {
                MentionGraphEdge mentionGraphEdge = mentionGraphEdges[curr][ant];
                Pair<MentionGraphEdge.EdgeType, Double> correctLabelScore =
                        mentionGraphEdge.getCorrectLabelScore(weights, extractor);
                if (correctLabelScore == null) {
                    // There is no edge here.
                    continue;
                }
                MentionGraphEdge.EdgeType label = correctLabelScore.getValue0();
                double score = correctLabelScore.getValue1();

                if (score > bestScore) {
                    bestEdge = Pair.with(mentionGraphEdge, label);
                    bestScore = score;
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
            for (MentionGraphEdge mentionGraphEdge : mentionGraphEdgeArray) {
                sb.append("\t").append(mentionGraphEdge).append("\n");
            }
        }
        return sb.toString();
    }

    public FeatureAlphabet getFeatureAlphabet() {
        return featureAlphabet;
    }

    public ClassAlphabet getClassAlphabet() {
        return classAlphabet;
    }

    public PairFeatureExtractor getExtractor() {
        return extractor;
    }

    public JCas getContext() {
        return context;
    }
}
