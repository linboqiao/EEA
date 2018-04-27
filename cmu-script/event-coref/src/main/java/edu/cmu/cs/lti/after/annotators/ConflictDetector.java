package edu.cmu.cs.lti.after.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.io.EventDataReader;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.learning.model.graph.MentionGraph;
import edu.cmu.cs.lti.uima.io.writer.AbstractSimpleTextWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.MentionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
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
import org.xml.sax.SAXException;

import java.io.File;
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
        MentionGraph mentionGraph = MentionUtils.createSpanBasedMentionGraph(goldStandardView, candidates, null,
                false, true);

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

            for (Pair<NodeKey, NodeKey> relation : mentionGraph.getResolvedRelations().get(edgeType)) {
                int fromEvent = node2EventMap.get(relation.getKey());
                int toEvent = node2EventMap.get(relation.getValue());
                graph.addEdge(fromEvent, toEvent);
            }

            CycleDetector<Integer, DefaultEdge> detector = new CycleDetector<>(graph);
            Set<Integer> cycles = detector.findCycles();

            if (cycles.size() > 0) {
                sb.append("In Document: ").append(UimaConvenience.getDocId(aJCas)).append("\n");
                sb.append("\tConflicts related to: ").append(edgeType).append("\n");

                sb.append("\tCycle may happend between the following events number:\n");
                for (int eventInCycle : cycles) {
                    sb.append(String.format("\tEvent %d, contains mentions: \t", eventInCycle));
                    for (NodeKey node : event2KeyMap.get(eventInCycle)) {
                        int candidateIndex = MentionGraph.getCandidateIndex(node.getNodeIndex());
                        String repr = String.format("%s:%s [%d:%d]", candidates.get(candidateIndex).getText(),
                                node.getMentionType(), node.getBegin(), node.getEnd());
                        sb.append(repr).append(" ");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static void main(String[] args) throws UIMAException, IOException, CpeDescriptorException, SAXException {
        if (args.length != 1) {
            System.out.println("Usage: this settings.properties");
            System.exit(1);
        }

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TaskEventMentionDetectionTypeSystem");

        Configuration taskConfig = new Configuration(args[0]);

        String datasetSettingDir = taskConfig.get("edu.cmu.cs.lti.dataset.settings.path");
        String[] trainingDatasets = taskConfig.getList("edu.cmu.cs.lti.training.datasets");
        String[] testDatasets = taskConfig.getList("edu.cmu.cs.lti.testing.datasets");

        String trainingWorkingDir = taskConfig.get("edu.cmu.cs.lti.training.working.dir");
        String testingWorkingDir = taskConfig.get("edu.cmu.cs.lti.test.working.dir");

        System.out.println("Reading training data.");
        CollectionReaderDescription trainingReader = readDatasets(datasetSettingDir, trainingDatasets,
                trainingWorkingDir, typeSystemDescription);
        System.out.println("Reading test data.");
        CollectionReaderDescription testReader = readDatasets(datasetSettingDir, testDatasets,
                testingWorkingDir, typeSystemDescription);


        ConflictDetector.findConflicts(typeSystemDescription, trainingReader,
                new File(trainingWorkingDir, "cycles.txt").getPath());
        ConflictDetector.findConflicts(typeSystemDescription, testReader,
                new File(testingWorkingDir, "cycles.txt").getPath());
    }

    public static void findConflicts(TypeSystemDescription typeSystemDescription, CollectionReaderDescription reader,
                                     String outputFile)
            throws UIMAException, IOException {
        AnalysisEngineDescription engine = AnalysisEngineFactory
                .createEngineDescription(
                        ConflictDetector.class, typeSystemDescription,
                        ConflictDetector.PARAM_OUTPUT_PATH, outputFile
                );
        SimplePipeline.runPipeline(reader, engine);
    }

    public static CollectionReaderDescription readDatasets(String datasetConfigPath, String[] datasetNames,
                                                           String parentDir, TypeSystemDescription
                                                                   typeSystemDescription)
            throws IOException, UIMAException, SAXException, CpeDescriptorException {
        EventDataReader reader = new EventDataReader(parentDir, "raw", false);

        for (String datasetName : datasetNames) {
            System.out.println("Reading dataset : " + datasetName);
            Configuration datasetConfig = new Configuration(new File(datasetConfigPath, datasetName + ".properties"));
            reader.readData(datasetConfig, typeSystemDescription);
        }

        return reader.getReader();
    }
}
