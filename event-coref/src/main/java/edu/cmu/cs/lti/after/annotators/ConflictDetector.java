package edu.cmu.cs.lti.after.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.AbstractSimpleTextWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.MentionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/13/17
 * Time: 5:14 PM
 *
 * @author Zhengzhong Liu
 */
public class ConflictDetector extends AbstractSimpleTextWriterAnalysisEngine {

    @Override
    public String getTextToPrint(JCas aJCas) {
        StringBuilder sb = new StringBuilder();

        JCas goldStandardView = JCasUtil.getView(aJCas, goldStandardViewName, aJCas);

        List<MentionCandidate> candidates = MentionUtils.getSpanBasedCandidates(goldStandardView);
        MentionGraph mentionGraph = MentionUtils.createSpanBasedMentionGraph(goldStandardView, candidates, null, false, true);

        Map<NodeKey, Integer> node2EventMap = new HashMap<>();
        ArrayListMultimap<Integer, NodeKey> event2KeyMap = ArrayListMultimap.create();

        int eventId = 0;
        for (List<NodeKey> corefChain : mentionGraph.getNodeCorefChains()) {
            for (NodeKey key : corefChain) {
                node2EventMap.put(key, eventId);
                event2KeyMap.put(eventId, key);
            }
            eventId++;
        }



        for (MentionCandidate candidate : candidates) {
            for (NodeKey nodeKey : candidate.asKey()) {
                if (!node2EventMap.containsKey(nodeKey)) {
                    event2KeyMap.put(eventId, nodeKey);
                    node2EventMap.put(nodeKey, eventId);
                    eventId++;
                }
            }
        }

        for (EdgeType edgeType : mentionGraph.getResolvedRelations().keySet()) {
            DirectedGraph<Integer, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

            for (Integer event : event2KeyMap.keys()) {
                graph.addVertex(event);
            }

            for (Map.Entry<NodeKey, List<NodeKey>> adjacentList : mentionGraph.getResolvedRelations()
                    .get(edgeType).entrySet()) {
                int fromEvent = node2EventMap.get(adjacentList.getKey());

                for (NodeKey toNode : adjacentList.getValue()) {
                    int toEvent = node2EventMap.get(toNode);
                    graph.addEdge(fromEvent, toEvent);
                }
            }

            CycleDetector<Integer, DefaultEdge> detector = new CycleDetector<>(graph);
            Set<Integer> cycles = detector.findCycles();

            if (cycles.size() > 0) {
                sb.append(UimaConvenience.getDocId(aJCas)).append("\n");
                sb.append("Showing event cycles for type ").append(edgeType).append("\n");
                for (int eventInCycle : cycles) {
                    sb.append(String.format("In event %d\t", eventInCycle));
                    for (NodeKey node : event2KeyMap.get(eventInCycle)) {
                        int candidateIndex = MentionGraph.getCandidateIndex(node.getNodeIndex());
                        String repr = String.format("%s [%d:%d] %s", candidates.get(candidateIndex).getText(),
                                node.getBegin(), node.getEnd(), node.getMentionType());
                        sb.append(repr).append(" ");
                    }
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static void main(String[] args) throws UIMAException, IOException {

        if (args.length != 2) {
            System.out.println("Usage: this [input] [output]");
            System.exit(1);
        }

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TaskEventMentionDetectionTypeSystem");

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, args[0]);

        AnalysisEngineDescription engine = AnalysisEngineFactory
                .createEngineDescription(
                        ConflictDetector.class, typeSystemDescription,
                        ConflictDetector.PARAM_OUTPUT_PATH, args[1]
                );

        SimplePipeline.runPipeline(reader, engine);
    }
}
