package edu.cmu.cs.lti.learning.model.graph;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.utils.DebugUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/3/15
 * Time: 2:16 PM
 * <p>
 * A subgraph defines nodes and edges taken from its parent graph (super graph).
 *
 * @author Zhengzhong Liu
 */
public class MentionSubGraph {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    // Gov, Dep, Edge
    private Table<NodeKey, NodeKey, LabelledMentionGraphEdge> edgeTable;

    private Map<LabelledMentionGraphEdge, EdgeType> edgeTypes;

    private int numNodes;

    // The parent graph of this subgraph.
    private MentionGraph parentGraph;

    // Store edges other than the cluster edges.
    private Map<EdgeType, Set<Pair<NodeKey, NodeKey>>> resolvedRelations;

    // Store typed coreference chains, each list represent a chain, where the elements are the
    // <mention id, mention type> representing the mentions in the chain.
    private List<NodeKey>[] typedCorefChains;

    private int totalCorefDistance = 0;

    private ClusterBuilder<Pair<Integer, String>> clusterBuilder;

    /**
     * Initialize a mention subgraph with all the super graph's node. The edges are empty.
     *
     * @param parentGraph The parent graph of this subgraph.
     */
    public MentionSubGraph(MentionGraph parentGraph) {
        edgeTable = HashBasedTable.create();
        numNodes = parentGraph.numNodes();
        this.parentGraph = parentGraph;
        clusterBuilder = new ClusterBuilder<>();
        edgeTypes = new HashMap<>();
    }

    public MentionSubGraph makeCopy() {
        MentionSubGraph subgraph = new MentionSubGraph(parentGraph);
        subgraph.numNodes = numNodes;
        subgraph.totalCorefDistance = totalCorefDistance;

        for (Table.Cell<NodeKey, NodeKey, LabelledMentionGraphEdge> cell : edgeTable.cellSet()) {
            subgraph.edgeTable.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
        }
        subgraph.edgeTypes.putAll(edgeTypes);
        subgraph.clusterBuilder = clusterBuilder.makeCopy();
        return subgraph;
    }

    /**
     * Initialize a mention subgraph with the first {@code numNodes} nodes.
     *
     * @param parentGraph
     * @param numNodes
     */
    public MentionSubGraph(MentionGraph parentGraph, int numNodes) {
        edgeTable = HashBasedTable.create();
        this.numNodes = numNodes;
        this.parentGraph = parentGraph;
        clusterBuilder = new ClusterBuilder<>();
    }

    private SubGraphEdge addEdge(MentionGraphEdge edge) {
        int dep = edge.getDep();
        int gov = edge.getGov();

        SubGraphEdge newEdge = new SubGraphEdge(edge);

        return newEdge;
    }

    public Map<LabelledMentionGraphEdge, EdgeType> getEdgeTypes() {
        return edgeTypes;
    }

    public void addLabelledEdge(LabelledMentionGraphEdge labelledEdge, EdgeType newType) {
        int dep = labelledEdge.getDepKey().getNodeIndex();
        int gov = labelledEdge.getGovKey().getNodeIndex();

        edgeTable.put(labelledEdge.getGovKey(), labelledEdge.getDepKey(), labelledEdge);
        edgeTypes.put(labelledEdge, newType);

        if (gov != 0) {
            if (newType == EdgeType.Coreference) {
                NodeKey govKey = labelledEdge.getGovKey();
                NodeKey depKey = labelledEdge.getDepKey();
                // We consider root to be special, which does not add distance to the tree.
                totalCorefDistance += Math.abs(dep - gov);
                clusterBuilder.addLink(Pair.of(gov, govKey.getMentionType()),
                        Pair.of(dep, depKey.getMentionType()));
            }
        }
    }

    public Map<EdgeType, Set<Pair<NodeKey, NodeKey>>> getResolvedRelations() {
        return resolvedRelations;
    }

    public List<NodeKey>[] getCorefChains() {
        return typedCorefChains;
    }

    /**
     * Compute the loss with regard to the reference graph.
     *
     * @param referenceGraph The graph to be used as a reference for the loss
     * @return The loss of the graph (almost hamming)
     */
    public double getLoss(MentionSubGraph referenceGraph) {
        if (parentGraph != referenceGraph.parentGraph) {
            throw new IllegalArgumentException("To compute loss, both graph should have the same parent.");
        }

        double loss = 0;

        Map<NodeKey, NodeKey> referentCorefAntecedents = new HashMap<>();


        Map<NodeKey, EdgeType> couldBeLinked = couldBeLinkedByInference(referenceGraph);

        // When you link a coreferece link to a wrong position, you will be penalize for 2 things:
        // 1. We cannot find the correct link in the gold tree, loss += 1
        // 2. We find a wrong link in the system tree
        // 2.1 If the wrong link is a normal link, loss += 1
        // 2.2 If the wrong link is a root link, loss += 2
        // That means a link get penalty of 2 if it is a normal error, get 3 if it is a root attachment error.
        // This is the same as using 1 for each node that find the wrong antecedent, 1.5 for each node that incorrect
        // root attachment.

        for (Table.Cell<NodeKey, NodeKey, LabelledMentionGraphEdge> edge : referenceGraph.edgeTable.cellSet()) {
            NodeKey gov = edge.getRowKey();
            NodeKey dep = edge.getColumnKey();
            LabelledMentionGraphEdge referentEdge = edge.getValue();
            EdgeType referentType = referenceGraph.edgeTypes.get(referentEdge);

            if (referentType == EdgeType.Coreference) {
                referentCorefAntecedents.put(dep, gov);
            }

            if (edgeTable.contains(gov, dep)) {
                LabelledMentionGraphEdge thisEdge = edgeTable.get(gov, dep);
                if (edgeTypes.get(thisEdge) != referenceGraph.edgeTypes.get(referentEdge)) {
                    // Wrong type on edge, that actually means one additional link and one missing link.
//                    logger.info("Loss because wrong type on edge " + referentEdge);
                    loss += 2;
                }
            } else {
                // No such link predicted by this system, but some ROOT penalty can be avoided.
                if (referenceGraph.edgeTypes.get(referentEdge).equals(EdgeType.Root)) {
                    EdgeType couldLinkTo = couldBeLinked.get(dep);
                    // If the dep node can actually link to some other node of the specific type, we should not
                    // penalize the ROOT loss.
                    if (couldBeLinked.containsKey(dep) &&
                            referentEdge.getGovKey().getMentionType().equals(couldLinkTo.name())) {
//                        logger.info("Root link not found, but inferred: " + referentEdge);
                    } else {
//                        logger.info("Loss because did not connect to root: " + referentEdge);
                        loss += 1;
                    }
                } else {
//                    logger.info("Loss because missing edge " + referentEdge);
                    loss += 1;
                }
            }
        }

        // We then check for invented links in the prediction graph.
        for (Table.Cell<NodeKey, NodeKey, LabelledMentionGraphEdge> targetEdge : edgeTable.cellSet()) {
            NodeKey from = targetEdge.getRowKey();
            NodeKey to = targetEdge.getColumnKey();
            LabelledMentionGraphEdge labelledTargetEdge = targetEdge.getValue();

            EdgeType type = edgeTypes.get(labelledTargetEdge);

            if (type.equals(EdgeType.Coreference)) {
                // For coreference, check against the antecedent is sufficient.
                NodeKey referentAntecedent = referentCorefAntecedents.get(to);
                if (referentAntecedent == null || !referentAntecedent.equals(from)) {
                    loss += 1;
                }
            } else {
                if (type.equals(EdgeType.Root)) {
                    // Invented root links. Type matching is not required because there is only ROOT type.
                    if (!referenceGraph.edgeTable.contains(from, to)) {
                        if (from.getMentionType().equals(EdgeType.Coreference.name())) {
                            // Adding a new cluster is penalized more.
                            loss += 2;
                        } else {
                            loss += 1;
                        }
                    }
                } else {
                    // Now we see if there are invented "After" or "Subevent" links.
                    if (!referenceGraph.resolvedRelations.containsKey(type) ||
                            !referenceGraph.resolvedRelations.get(type).contains(Pair.of(from, to))) {
                        // Invented edges, which cannot be inferred given the coreference.
                        // logger.info("Loss because invented edge " + labelledTargetEdge);
                        loss += 1;
                    }
                }
            }
        }
        return loss;
    }

    /**
     * Find out nodes that could be linked to be inference. This can be used to avoid penalize wrong root
     * attachment.
     *
     * @param referenceGraph
     * @return
     */
    private Map<NodeKey, EdgeType> couldBeLinkedByInference(MentionSubGraph referenceGraph) {
        Map<NodeKey, EdgeType> couldBeLinked = new HashMap<>();

        if (referenceGraph.getResolvedRelations() != null) {
            for (Map.Entry<EdgeType, Set<Pair<NodeKey, NodeKey>>> edgesByType : referenceGraph.resolvedRelations
                    .entrySet()) {
                EdgeType type = edgesByType.getKey();
                for (Pair<NodeKey, NodeKey> link : edgesByType.getValue()) {
                    NodeKey latterKey = link.getKey();
                    if (link.getValue().compareTo(latterKey) > 0) {
                        latterKey = link.getValue();
                    }
//                logger.info(latterKey + " could be linked as " + type + " by " + link);
                    couldBeLinked.put(latterKey, type);
                }
            }
        }
        return couldBeLinked;
    }

    private double compareEdge(SubGraphEdge referentEdge, SubGraphEdge targetEdge) {
        double loss = 0;
        for (LabelledMentionGraphEdge referentLabelledEdge : referentEdge.getAllLabelledEdge()) {
            if (targetEdge.getLabelledType(referentLabelledEdge) == null) {
                loss += 1;
            } else if (targetEdge.getLabelledType(referentLabelledEdge) !=
                    referentEdge.getLabelledType(referentLabelledEdge)) {
                loss += 1;
            }
        }
        return loss;
    }

    private double computeLabelledLoss(Collection<SubGraphEdge> referentGovEdges, Collection<SubGraphEdge>
            targetGovEdges) {
        double loss = 0;
        if (referentGovEdges.isEmpty()) {
            // Referent node is not connect to anything.
            for (SubGraphEdge targetGovEdge : targetGovEdges) {
                loss += targetGovEdge.numLabelledNonRootLinks();
            }
        } else if (targetGovEdges.isEmpty()) {
            for (SubGraphEdge referentGovEdge : referentGovEdges) {
                loss += referentGovEdge.numLabelledNonRootLinks();
            }
        } else {
            // Because we have a tree, then the edge collection must be single.
            SubGraphEdge referentEdge = referentGovEdges.iterator().next();
            SubGraphEdge targetGovEdge = targetGovEdges.iterator().next();

            if (referentEdge.getGov() != targetGovEdge.getGov()) {
                // First, compare the gov index.
                for (LabelledMentionGraphEdge labelledTargetEdge : targetGovEdge.getAllLabelledEdge()) {
                    if (targetGovEdge.getLabelledType(labelledTargetEdge) == EdgeType.Root) {
                        loss += 1.5;
                    } else {
                        loss += 1;
                    }
                }
            } else {
                // Next, check type.
                for (LabelledMentionGraphEdge labelledTargetEdge : targetGovEdge.getAllLabelledEdge()) {
                    EdgeType targetType = targetGovEdge.getLabelledType(labelledTargetEdge);
                    EdgeType referentType = referentEdge.getLabelledType(labelledTargetEdge);
                    if (targetType != referentType) {
                        loss += 1;
                    }
                }
            }
        }
        return loss;
    }

    /**
     * Compute the difference of features on this graph against another subgraph.
     *
     * @param referentGraph   The other subgraph.
     * @param classAlphabet   The class labels alphabet.
     * @param featureAlphabet The feature alphabet.
     * @param ignoreInferred  Do not penalize inferred links.
     * @return The delta of the features on these graphs.
     */
    public GraphFeatureVector getDelta(MentionSubGraph referentGraph, ClassAlphabet classAlphabet,
                                       FeatureAlphabet featureAlphabet, boolean ignoreInferred,
                                       EdgeType... targetTypes) {
        GraphFeatureVector deltaFeatureVector = new GraphFeatureVector(classAlphabet, featureAlphabet);

        for (EdgeType targetType : targetTypes) {
            TObjectIntMap<Pair<LabelledMentionGraphEdge, String>> deltas =
                    getDelta(referentGraph, targetType, ignoreInferred);
            deltas.forEachEntry((edgeWithType, multiplier) -> {
                LabelledMentionGraphEdge edge = edgeWithType.getKey();
                String className = edgeWithType.getValue();

                if (multiplier != 0) {
//                    logger.info(String.format("Adding features with multiplier %d  for type %s from:", multiplier,
//                            className));
//                    logger.info(edge.toString());
                    deltaFeatureVector.extend(edge.getFeatureVector(), className, multiplier);
                    deltaFeatureVector.extend(edge.getHostingEdge().getNodeAgnosticFeatures(), className, multiplier);
                }
                return true;
            });
        }

        return deltaFeatureVector;
    }

    /**
     * Compute the difference of features on this graph against another subgraph.
     *
     * @param referentGraph  The other subgraph.
     * @param ignoreInferred Do not penalize inferred links.
     * @return The delta of the features on these graphs.
     */
    private TObjectIntMap<Pair<LabelledMentionGraphEdge, String>> getDelta(MentionSubGraph referentGraph,
                                                                           EdgeType targetType,
                                                                           boolean ignoreInferred) {
        if (parentGraph != referentGraph.parentGraph) {
            throw new IllegalArgumentException("To compute delta, both graph should have the same parent.");
        }

        TObjectIntMap<Pair<LabelledMentionGraphEdge, String>> edgeFeatureInclusions = new TObjectIntHashMap<>();

        // Record links link to the targetType ROOT.
        Set<LabelledMentionGraphEdge> thisRoots = new HashSet<>();
        Set<LabelledMentionGraphEdge> referentRoots = new HashSet<>();

        // Record the inferred decedents from the system graph.
        Set<NodeKey> inferredDecedents = new HashSet<>();

        // For edges in the system subgraph. We deduct them.
        for (LabelledMentionGraphEdge edge : edgeTable.values()) {
            EdgeType edgeType = edgeTypes.get(edge);

            if (edgeType == EdgeType.Root) {
                if (edge.getGovKey().getMentionType().equals(targetType.name())) {
                    // This root link links to the targetType ROOT.
                    thisRoots.add(edge);
                }
            }

            if (!(edgeType == targetType)) {
                continue;
            }

            if (ignoreInferred) {
                if (edgeType.equals(EdgeType.After) || edgeType.equals(EdgeType.Subevent)) {
                    Set<Pair<NodeKey, NodeKey>> relations = referentGraph.resolvedRelations.get(edgeType);
                    if (relations != null && relations.contains(Pair.of(edge.getGovKey(), edge.getDepKey()))) {
                        // We make a record of nodes that can link to something.
                        inferredDecedents.add(edge.getDecedent());
                        continue;
                    }
                }
            }
//            logger.info("Deduct features on system graph: ");
//            logger.info(edgeType.name() + "\t" + edge);
            edgeFeatureInclusions.adjustOrPutValue(Pair.of(edge, edgeType.name()), -1, -1);
        }

        // For edges in the referent subgraph. We add them.
        for (LabelledMentionGraphEdge edge : referentGraph.edgeTable.values()) {
            EdgeType edgeType = referentGraph.edgeTypes.get(edge);

            if (edgeType == EdgeType.Root) {
                if (edge.getGovKey().getMentionType().equals(targetType.name())) {
                    // This root link links to the targetType ROOT.
                    referentRoots.add(edge);
                }
            }

            if (!edgeType.equals(targetType)) {
                continue;
            }

//            logger.info("Add features on referent graph: ");
//            logger.info(edgeType.name() + "\t" + edge);
            edgeFeatureInclusions.adjustOrPutValue(Pair.of(edge, edgeType.name()), 1, 1);
        }

        Sets.SetView<LabelledMentionGraphEdge> onlyInThis = Sets.difference(thisRoots, referentRoots);
        Sets.SetView<LabelledMentionGraphEdge> onlyInReferent = Sets.difference(referentRoots, thisRoots);

        // These means invented root keys from the system.
        for (LabelledMentionGraphEdge rootEdgeInThis : onlyInThis) {
//            logger.info("Deduct root features on system graph: ");
//            logger.info(EdgeType.Root + "\t" + rootEdgeInThis);
            edgeFeatureInclusions.adjustOrPutValue(Pair.of(rootEdgeInThis, EdgeType.Root.name()), -1, -1);
        }

        // These are root edges from the referent graph, but we filter out those ROOT link where the decedent can link
        // to somewhere in the resolved graph.
        for (LabelledMentionGraphEdge rootEdgeInReferent : onlyInReferent) {
            if (!inferredDecedents.contains(rootEdgeInReferent.getDecedent())) {
//                logger.info(EdgeType.Root + "\t" + rootEdgeInReferent);
                edgeFeatureInclusions.adjustOrPutValue(Pair.of(rootEdgeInReferent, EdgeType.Root.name()), 1, 1);
            }
        }

        return edgeFeatureInclusions;
    }


    /**
     * Convert the tree to transitive and equivalence resolved graph
     */
    public SetMultimap<Integer, NodeKey> resolveCoreference() {
        return resolveCoreference(numNodes);
    }

    /**
     * Convert the tree to transitive and equivalence resolved graph
     */
    public SetMultimap<Integer, NodeKey> resolveCoreference(int untilNode) {
        SetMultimap<Integer, NodeKey> keyClusters = HashMultimap.create();

        // Collect stuff from the edgeTable, until the limit.
        List<LabelledMentionGraphEdge> allEdges = new ArrayList<>(edgeTable.values());

        allEdges.sort((o1, o2) -> new CompareToBuilder().append(o1.getDepKey(), o2.getDepKey())
                .append(o1.getGovKey(), o2.getGovKey()).build());

        int clusterId = 0;
        for (LabelledMentionGraphEdge labelledEdge : allEdges) {
            EdgeType type = edgeTypes.get(labelledEdge);

            NodeKey depKey = labelledEdge.getDepKey();
            NodeKey govKey = labelledEdge.getGovKey();

            int govIndex = govKey.getNodeIndex();
            int depIndex = depKey.getNodeIndex();

            if (govIndex > untilNode || depIndex > untilNode) {
                // Don't break here since we are not sure that the edges are sorted.
                continue;
            }

            if (type.equals(EdgeType.Root)) {
                // If this link to root, start a new cluster.
                keyClusters.put(clusterId, depKey);
                clusterId++;
            } else if (type.equals(EdgeType.Coreference)) {
                // Add the node to one of the existing cluster.
                for (Integer eventId : keyClusters.keys()) {
                    Set<NodeKey> nodeCluster = keyClusters.get(eventId);
                    if (nodeCluster.contains(govKey)) {
                        nodeCluster.add(depKey);
                        break;
                    }
                }
            }
        }

        // Create a coreference chain.
        typedCorefChains = GraphUtils.createSortedCorefChains(keyClusters);

        return keyClusters;
    }

    public void resolveRelations(SetMultimap<Integer, NodeKey> keyClusters) {
        SetMultimap<EdgeType, Pair<NodeKey, NodeKey>> keyRelations = HashMultimap.create();

        List<LabelledMentionGraphEdge> allEdges = new ArrayList<>(edgeTable.values());

        allEdges.sort((o1, o2) -> new CompareToBuilder().append(o1.getDepKey(), o2.getDepKey())
                .append(o1.getGovKey(), o2.getGovKey()).build());

        for (LabelledMentionGraphEdge labelledEdge : allEdges) {
            EdgeType type = edgeTypes.get(labelledEdge);

            NodeKey depKey = labelledEdge.getDepKey();
            NodeKey govKey = labelledEdge.getGovKey();

            if (type.equals(EdgeType.After) || type.equals(EdgeType.Subevent)) {
                keyRelations.put(type, Pair.of(govKey, depKey));
            }
        }

        TObjectIntMap<NodeKey> node2ClusterId = new TObjectIntHashMap<>();
        for (Map.Entry<Integer, Collection<NodeKey>> cluster : keyClusters.asMap().entrySet()) {
            for (NodeKey element : cluster.getValue()) {
                node2ClusterId.put(element, cluster.getKey());
            }
        }

        SetMultimap<EdgeType, Pair<Integer, Integer>> interRelations = HashMultimap.create();
        // Propagate relations within cluster.
        for (Map.Entry<EdgeType, Pair<NodeKey, NodeKey>> relation :
                keyRelations.entries()) {
            NodeKey govNode = relation.getValue().getLeft();
            NodeKey depNode = relation.getValue().getRight();
            interRelations.put(relation.getKey(),
                    Pair.of(node2ClusterId.get(govNode), node2ClusterId.get(depNode)));
        }

        resolvedRelations = GraphUtils.resolveRelations(interRelations, keyClusters);
        // Create a coreference chain.
        typedCorefChains = GraphUtils.createSortedCorefChains(keyClusters);
    }

    public Map<LabelledMentionGraphEdge, EdgeType> getDecodedEdges() {
        return edgeTypes;
    }


    /**
     * Whether the subgraph matches the gold standard.
     *
     * @return
     */
    public boolean graphMatch() {
        return graphMatch(numNodes);
    }

    /**
     * Whether the subgraph matches the gold standard graph until a particular node.
     *
     * @return
     */
    public boolean graphMatch(int until) {
        SetMultimap<Integer, NodeKey> keyClusters = this.resolveCoreference(until);

        // Variable indicating whether the coreference clusters are matched.
        boolean corefMatch = Arrays.deepEquals(this.getCorefChains(), this.parentGraph.getNodeCorefChains());

        // Use the own cluster to resolve relations.
        this.resolveRelations(keyClusters);

        // Variable indicating whether the other mention links are matched.
        boolean linkMatch = this.getResolvedRelations().equals(this.parentGraph.getResolvedRelations());

        return corefMatch && linkMatch;
    }

    /**
     * Whether the subgraph matches another given subgraph.
     *
     * @return
     */
    public boolean graphMatch(MentionSubGraph referentGraph) {
        return this.clusterBuilder.match(referentGraph.clusterBuilder);
    }

    public String fullTree() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SubGraph of distance %d:\n", totalCorefDistance));

        List<LabelledMentionGraphEdge> edgeValues = new ArrayList<>(edgeTable.values());
        edgeValues.sort((o1, o2)
                -> new CompareToBuilder().append(edgeTypes.get(o1), edgeTypes.get(o2))
                .append(o1.getDep(), o2.getDep()).append(o1.getGov(), o2.getGov()).build());

        for (LabelledMentionGraphEdge edge : edgeValues) {
            sb.append("\t").append(edgeTypes.get(edge)).append(" : ").append(edge.toString()).append("\n");
        }

        sb.append("Cluster view:\n");
        for (TreeSet<Pair<Integer, String>> elements : clusterBuilder.getClusters()) {
            sb.append(Joiner.on("\t").join(elements)).append("\n");
        }

        return sb.toString();
    }

    public boolean hasNonRoot() {
        for (LabelledMentionGraphEdge edge : edgeTable.values()) {
            if (!edge.isRoot()) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SubGraph (non root edges only) of distance %d:\n", totalCorefDistance));

        List<LabelledMentionGraphEdge> edgeValues = new ArrayList<>(edgeTable.values());

        edgeValues.sort((o1, o2)
                -> new CompareToBuilder().append(edgeTypes.get(o1), edgeTypes.get(o2))
                .append(o1.getDep(), o2.getDep()).append(o1.getGov(), o2.getGov()).build());

        for (LabelledMentionGraphEdge edge : edgeValues) {
            if (!edge.isRoot()) {
                sb.append("\t").append(edgeTypes.get(edge)).append(" : ").append(edge.toString()).append("\n");
            }
        }

        sb.append("Cluster view:\n");
        for (TreeSet<Pair<Integer, String>> elements : clusterBuilder.getClusters()) {
            sb.append(Joiner.on("\t").join(elements)).append("\n");
        }

        return sb.toString();
    }

    public int getTotalCorefDistance() {
        return totalCorefDistance;
    }

    /**
     * Update the weights by the difference of the predicted tree and the gold latent tree using Passive-Aggressive
     * algorithm.
     *
     * @param referentTree The gold latent tree.
     */
    public double paUpdate(MentionSubGraph referentTree, GraphWeightVector weights, EdgeType... targetEdgeTypes) {
        GraphFeatureVector delta = this.getDelta(referentTree, weights.getClassAlphabet(),
                weights.getFeatureAlphabet(), true, targetEdgeTypes);

        double loss = this.getLoss(referentTree);
        double l2Sqaure = delta.getFeatureSquare();

        if (l2Sqaure != 0) {
            double tau = loss / l2Sqaure;

            logger.info("Updating weights by: " + tau);
            logger.info(delta.readableNodeVector());

            weights.updateWeightsBy(delta, tau);
            weights.updateAverageWeights();

            logger.info("After update:");
            logger.info(weights.getNodeWeights("Root").toReadableString(weights.getFeatureAlphabet()));
            DebugUtils.pause();

        }
        return loss;
    }
}
