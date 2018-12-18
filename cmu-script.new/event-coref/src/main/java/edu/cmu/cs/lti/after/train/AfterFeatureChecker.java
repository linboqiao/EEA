package edu.cmu.cs.lti.after.train;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.mention_pair.extractor.PairFeatureExtractor;
import edu.cmu.cs.lti.learning.model.*;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.LabelledMentionGraphEdge;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.learning.model.graph.MentionGraphEdge;
import edu.cmu.cs.lti.learning.utils.LearningUtils;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MentionUtils;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/27/17
 * Time: 3:09 PM
 *
 * @author Zhengzhong Liu
 */
public class AfterFeatureChecker extends AbstractLoggingAnnotator {
    private PairFeatureExtractor extractor;

    public static final String PARAM_CONFIG_PATH = "configPath";
    @ConfigurationParameter(name = PARAM_CONFIG_PATH)
    private Configuration config;

    public static final String PARAM_OUTPUT_DIR = "outputDir";
    @ConfigurationParameter(name = PARAM_OUTPUT_DIR)
    private File outputDir;

    private FeatureAlphabet featureAlphabet;

    private TIntDoubleMap noneLinkFeatureCounts;
    private Map<String, TIntDoubleMap> featureCounts;

    private Map<String, ArrayListMultimap<Integer, String>> featureAppearances;
    private ArrayListMultimap<Integer, String> noneAppearances;

    private ArrayListMultimap<EdgeType, String> nothingFires;

    private String docId = "";

    private List<EdgeType> typeSet;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        String afterFeatureSpec = "edu.cmu.cs.lti.features.after.spec";

        boolean readable = config.getBoolean("edu.cmu.cs.lti.readableModel", false);
        int featureBits = config.getInt("edu.cmu.cs.lti.feature.alphabet_bits", 22);

        featureAlphabet = new HashAlphabet(featureBits, readable);
        extractor = LearningUtils.initializeMentionPairExtractor(config, afterFeatureSpec, featureAlphabet);

        noneLinkFeatureCounts = new TIntDoubleHashMap();

        featureCounts = new HashMap<>();
        featureAppearances = new HashMap<>();
        for (EdgeType edgeType : EdgeType.values()) {
            if (edgeType.equals(EdgeType.Coreference) || edgeType.equals(EdgeType.Root)) {
                continue;
            }
            featureCounts.put(edgeType.name(), new TIntDoubleHashMap());
            featureAppearances.put(edgeType.name(), ArrayListMultimap.create());

            featureCounts.put(edgeType.name() + "_Root", new TIntDoubleHashMap());
            featureAppearances.put(edgeType.name() + "_Root", ArrayListMultimap.create());
        }

        noneAppearances = ArrayListMultimap.create();

        nothingFires = ArrayListMultimap.create();

        typeSet = new ArrayList<>();
        typeSet.add(EdgeType.After);
        typeSet.add(EdgeType.Subevent);

        logger.info("Initialized after feature checker.");
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();

        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        Set<Integer> featureIndices = new HashSet<>();

        ArrayList<String> sortedHeader = new ArrayList<>(featureCounts.keySet());
        Collections.sort(sortedHeader);

        // Write counts for each edge type.
        try {
            for (String t : sortedHeader) {
                TIntDoubleMap counts = featureCounts.get(t);
                ArrayListMultimap<Integer, String> featureContexts = featureAppearances.get(t);
                File featureCountOut = new File(outputDir, t + "_trigger.txt");
                File featureContextOut = new File(outputDir, t + "_context.txt");
                writeCounts(featureCountOut, counts, featureIndices);
                writeContext(featureContextOut, featureContexts);
            }
            File noneCountOut = new File(outputDir, "None_trigger.txt");
            File noneContextOut = new File(outputDir, "None_context.txt");

            writeCounts(noneCountOut, noneLinkFeatureCounts, featureIndices);
            writeContext(noneContextOut, noneAppearances);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }

        // Write cases that no feature fires.
        for (Map.Entry<EdgeType, Collection<String>> typeAndContext : nothingFires.asMap().entrySet()) {
            File noFireFile = new File(outputDir, "No_Fire_" + typeAndContext.getKey());
            try {
                Writer writer = new BufferedWriter(new FileWriter(noFireFile));
                for (String c : typeAndContext.getValue()) {
                    writer.write(c);
                    writer.write("\n");
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Write summary counts.
        List<Integer> allFeatures = new ArrayList<>(featureIndices);
        List<Pair<Double, String>> lines = new ArrayList<>();

        List<String> headers = new ArrayList<>();
        headers.add("ID");
        headers.add("Feature");
        for (String t : sortedHeader) {
            headers.add(t);
        }
        headers.add("None");

        for (int f : allFeatures) {
            StringBuilder sb = new StringBuilder();
            sb.append(f).append("\t").append(featureAlphabet.getFeatureNameRepre(f)).append("\t");

            double afterCount = 0;
            for (String t : sortedHeader) {
                TIntDoubleMap counts = featureCounts.get(t);
                double v = counts.get(f);
                sb.append(v).append("\t");
                if (t.equals(EdgeType.After)) {
                    afterCount = v;
                }
            }
            sb.append(noneLinkFeatureCounts.get(f)).append("\n");
            lines.add(Pair.of(afterCount, sb.toString()));
        }

        lines.sort(Collections.reverseOrder());

        File summary = new File(outputDir, "summary.txt");
        try {
            Writer summaryWriter = new BufferedWriter(new FileWriter(summary));
            summaryWriter.write(Joiner.on("\t").join(headers));
            summaryWriter.write("\n");
            for (Pair<Double, String> line : lines) {
                summaryWriter.write(line.getValue());
            }
            summaryWriter.close();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void writeContext(File outputFile, ArrayListMultimap<Integer, String> appearance) throws IOException {
        Writer writer = new BufferedWriter(new FileWriter(outputFile));

        for (Map.Entry<Integer, Collection<String>> featureAppearance : appearance.asMap().entrySet()) {
            int f = featureAppearance.getKey();
            writer.write(String.format("%d\t%s\t:\n", f, featureAlphabet.getFeatureNameRepre(f)));

            Collection<String> sents = featureAppearance.getValue();
            for (String sent : sents) {
                writer.write("\t- ");
                writer.write(sent);
                writer.write("\n");
            }
        }

        writer.close();
    }

    private void writeCounts(File outputFile, TIntDoubleMap counts, Set<Integer> featureIndices) throws IOException {
        Writer writer = new BufferedWriter(new FileWriter(outputFile));

        for (TIntDoubleIterator iter = counts.iterator(); iter.hasNext(); ) {
            iter.advance();
            int index = iter.key();
            double v = iter.value();

            featureIndices.add(index);
            writer.write(String.format("%d\t%s\t%.4f\n", index, featureAlphabet.getFeatureNameRepre(index), v));
        }
        writer.close();
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        docId = UimaConvenience.getDocId(aJCas);
        List<MentionCandidate> candidates = MentionUtils.getSpanBasedCandidates(aJCas);

        extractor.initWorkspace(aJCas);
        MentionGraph mentionGraph = MentionUtils.createSpanBasedMentionGraph(aJCas, candidates, extractor, true);
        check(mentionGraph, candidates);
    }

    private void check(MentionGraph mentionGraph, List<MentionCandidate> candidates) {
        Table<NodeKey, NodeKey, String> nodeRelations = getClosureRelations(mentionGraph);

        Map<NodeKey, String> linkedToSomewhere = new HashMap<>();

        nodeRelations.cellSet().forEach(cell -> {
            NodeKey gov = cell.getRowKey();
            NodeKey dep = cell.getColumnKey();
            if (gov.compareTo(dep) > 0) {
                linkedToSomewhere.put(gov, cell.getValue());
            } else {
                linkedToSomewhere.put(dep, cell.getValue());
            }
        });

        SetMultimap<Integer, NodeKey> event2NodeKeys = mentionGraph.getEvent2NodeKeys();
        TObjectIntMap<NodeKey> nodeToEvent = getNodeToEvent(event2NodeKeys);

        List<Pair<Integer, Integer>> firedEvents = new ArrayList<>();

        for (int curr = 1; curr < mentionGraph.numNodes(); curr++) {
            for (int ant = 0; ant < curr; ant++) {
                MentionGraphEdge edge = mentionGraph.getEdge(ant, curr);
                edge.extractNodeAgnosticFeatures(candidates);

                for (NodeKey descendant : edge.getSuccKey(candidates)) {
                    if (edge.isRoot()) {
                        for (EdgeType edgeType : typeSet) {
                            LabelledMentionGraphEdge labelledRootEdge = edge.getLabelledEdge(candidates,
                                    NodeKey.getRootKey(edgeType), descendant);

                            String actualType = edgeType.name() + "_Root";
                            if (linkedToSomewhere.containsKey(descendant)) {
                                if (linkedToSomewhere.get(descendant).equals(edgeType.name())) {
                                    actualType = null;
                                }
                            }

                            checkEdge(labelledRootEdge, actualType, candidates);
                        }
                        continue;
                    }

                    for (NodeKey antecedent : edge.getAntKey(candidates)) {
                        LabelledMentionGraphEdge forwardEdge = edge.getLabelledEdge(candidates, antecedent, descendant);
                        LabelledMentionGraphEdge backwardEdge = edge.getLabelledEdge(candidates, descendant,
                                antecedent);

                        String forwardType = nodeRelations.get(antecedent, descendant);
                        String backwardType = nodeRelations.get(descendant, antecedent);

                        if (checkEdge(forwardEdge, forwardType, candidates)) {
                            firedEvents.add(Pair.of(nodeToEvent.get(antecedent), nodeToEvent.get(descendant)));
                        }

                        if (checkEdge(backwardEdge, backwardType, candidates)) {
                            firedEvents.add(Pair.of(nodeToEvent.get(descendant), nodeToEvent.get(antecedent)));
                        }
                    }
                }
            }
        }

        for (Map.Entry<EdgeType, Map<Integer, List<Integer>>> adjacentByType :
                mentionGraph.getClosureEventRelations().entrySet()) {
            EdgeType t = adjacentByType.getKey();
            for (Map.Entry<Integer, List<Integer>> adjacent : adjacentByType.getValue().entrySet()) {
                int fromEvent = adjacent.getKey();
                for (Integer toEvent : adjacent.getValue()) {
                    if (!firedEvents.contains(Pair.of(fromEvent, toEvent))) {
                        for (NodeKey fromKey : event2NodeKeys.get(fromEvent)) {
                            String fromContext = getCandidateInContext(candidates.get(
                                    MentionGraph.getCandidateIndex(fromKey.getNodeIndex())));
                            for (NodeKey toKey : event2NodeKeys.get(toEvent)) {
                                String toContext = getCandidateInContext(candidates.get(
                                        MentionGraph.getCandidateIndex(toKey.getNodeIndex())));
                                String context = docId + " : " + fromContext + "  |  " + toContext;
                                nothingFires.put(t, context);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addAll(TIntDoubleMap mainCount, TIntDoubleMap addCount) {
        for (TIntDoubleIterator iter = addCount.iterator(); iter.hasNext(); ) {
            iter.advance();
            mainCount.adjustOrPutValue(iter.key(), iter.value(), iter.value());
        }
    }

    private boolean checkEdge(LabelledMentionGraphEdge edge, String actualType,
                              List<MentionCandidate> candidates) {
        MentionCandidate depCand = candidates.get(MentionGraph.getCandidateIndex(edge.getDep()));
        String depCandContext = getCandidateInContext(depCand);

        String govCandContext;
        if (edge.isRoot()) {
            govCandContext = "ROOT";
        } else {
            MentionCandidate govCand = candidates.get(MentionGraph.getCandidateIndex(edge.getGov()));
            govCandContext = getCandidateInContext(govCand);
        }

        TIntDoubleMap counts = countFeatureFromEdge(edge);

        if (actualType == null) {
            addAll(noneLinkFeatureCounts, counts);

            for (TIntDoubleIterator iter = counts.iterator(); iter.hasNext(); ) {
                iter.advance();
                noneAppearances.put(iter.key(), docId + " : " + govCandContext + "  |  " + depCandContext);
            }
            return false;
        }

        addAll(featureCounts.get(actualType), counts);

        String context = docId + " : " + govCandContext + "  |  " + depCandContext;

        for (TIntDoubleIterator iter = counts.iterator(); iter.hasNext(); ) {
            iter.advance();
            featureAppearances.get(actualType).put(iter.key(), context);
        }

        return !counts.isEmpty();
    }

    private String getCandidateInContext(MentionCandidate candidate) {
        Sentence sentence = candidate.getContainedSentence();

        int begin = candidate.getBegin() - sentence.getBegin();
        int end = begin + candidate.getEnd() - candidate.getBegin();

        String sentText = sentence.getCoveredText();

        String candidateInContext = sentText.substring(0, begin) + "[" + candidate.getText() + "]" +
                sentText.substring(end, sentText.length());

        return candidateInContext.replaceAll("\n", " ");
    }

    private TObjectIntMap<NodeKey> getNodeToEvent(SetMultimap<Integer, NodeKey> evnet2NodeKeys) {
        TObjectIntMap<NodeKey> node2Event = new TObjectIntHashMap<>();
        for (Map.Entry<Integer, Collection<NodeKey>> event2Node : evnet2NodeKeys.asMap().entrySet()) {
            for (NodeKey nodeKey : event2Node.getValue()) {
                node2Event.put(nodeKey, event2Node.getKey());
            }
        }
        return node2Event;
    }

    private Table<NodeKey, NodeKey, String> getClosureRelations(MentionGraph graph) {
        Table<NodeKey, NodeKey, String> nodeRelations = HashBasedTable.create();

        Map<EdgeType, Map<Integer, List<Integer>>> closureRelations = graph.getClosureEventRelations();

        for (Map.Entry<EdgeType, Map<Integer, List<Integer>>> relationByType : closureRelations.entrySet()) {
            EdgeType t = relationByType.getKey();

            for (Map.Entry<Integer, List<Integer>> relationAjacent : relationByType.getValue().entrySet()) {
                int fromEvent = relationAjacent.getKey();
                Set<NodeKey> fromNodes = graph.getEvent2NodeKeys().get(fromEvent);

                for (Integer toEvent : relationAjacent.getValue()) {
//                    logger.info("relation between event " + fromEvent + " and " + toEvent + " is " + t);

                    Set<NodeKey> toNodes = graph.getEvent2NodeKeys().get(toEvent);

                    for (NodeKey fromNode : fromNodes) {
                        for (NodeKey toNode : toNodes) {
//                            logger.info("This means a link from  " + fromNode + " to " + toNode);
                            nodeRelations.put(fromNode, toNode, t.name());
                        }
                    }
                }
            }
        }

        return nodeRelations;
    }

    private TIntDoubleMap countFeatureFromEdge(LabelledMentionGraphEdge labelledEdge) {
        TIntDoubleMap counts = new TIntDoubleHashMap();
        FeatureVector nodeIndepedent = labelledEdge.getHostingEdge().getNodeAgnosticFeatures();
        FeatureVector nodeDependent = labelledEdge.getFeatureVector();

        countFeatures(nodeIndepedent, counts);
        countFeatures(nodeDependent, counts);

        return counts;
    }

    private void countFeatures(FeatureVector fv, TIntDoubleMap featureCount) {
        FeatureVector.FeatureIterator iter = fv.featureIterator();

        while (iter.hasNext()) {
            iter.next();
            double featureValue = iter.featureValue();
            featureCount.adjustOrPutValue(iter.featureIndex(), featureValue, featureValue);
        }
    }
}
