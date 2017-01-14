package edu.cmu.cs.lti.after.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.AbstractSimpleTextWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.MentionUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.Collection;
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
        MentionGraph mentionGraph = MentionUtils.createMentionGraph(goldStandardView, candidates, null, false);

        TIntIntMap candidate2EventMap = new TIntIntHashMap();

        ArrayListMultimap<Integer, Integer> event2CandidateMap = ArrayListMultimap.create();

        int eventId = 0;
        for (List<Pair<Integer, String>> corefChain : mentionGraph.getNodeCorefChains()) {
            for (Pair<Integer, String> typedNode : corefChain) {
                int candidateId = MentionGraph.getCandidateIndex(typedNode.getKey());
                candidate2EventMap.put(candidateId, eventId);
                event2CandidateMap.put(eventId, candidateId);
            }
            eventId++;
        }

        for (int candidateId = 0; candidateId < candidates.size(); candidateId++) {
            if (!candidate2EventMap.containsKey(candidateId)) {
                event2CandidateMap.put(eventId, candidateId);
                candidate2EventMap.put(candidateId, eventId);
                eventId++;
            }
        }

        for (EdgeType edgeType : mentionGraph.getResolvedRelations().keySet()) {
            DirectedGraph<Integer, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

            for (Integer event : event2CandidateMap.keys()) {
                graph.addVertex(event);
            }

            for (Map.Entry<Integer, Collection<Integer>> adjacentList : mentionGraph.getResolvedRelations()
                    .get(edgeType).asMap().entrySet()) {
                int fromEvent = candidate2EventMap.get(MentionGraph.getCandidateIndex(adjacentList.getKey()));
                for (Integer toNode : adjacentList.getValue()) {
                    int toCandidate = MentionGraph.getCandidateIndex(toNode);
                    int toEvent = candidate2EventMap.get(toCandidate);
                    graph.addEdge(fromEvent, toEvent);
                }
            }

            CycleDetector<Integer, DefaultEdge> detector = new CycleDetector<>(graph);
            Set<Integer> cycles = detector.findCycles();

            if (cycles.size() > 0) {
                sb.append(UimaConvenience.getDocId(aJCas)).append("\n");
                sb.append("Showing event cycles for type ").append(edgeType).append("\n");
                for (int eventInCycle : cycles) {
                    for (int candidateId : event2CandidateMap.get(eventInCycle)) {
                        MentionCandidate c = candidates.get(candidateId);
                        sb.append(c).append(" ");
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
