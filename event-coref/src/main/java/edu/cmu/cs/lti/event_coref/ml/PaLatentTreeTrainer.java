package edu.cmu.cs.lti.event_coref.ml;

import edu.cmu.cs.lti.event_coref.model.graph.*;
import edu.cmu.cs.lti.pipeline.AbstractProcessorBuilder;
import edu.cmu.cs.lti.pipeline.BasicLoopyPipeline;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.pipeline.LoopPipeline;
import edu.cmu.cs.lti.event_coref.model.graph.Edge.EdgeType;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Pair;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/29/15
 * Time: 7:27 PM
 *
 * @author Zhengzhong Liu
 */
public class PaLatentTreeTrainer extends AbstractLoggingAnnotator {
    //    EnumMap<Edge.EdgeType, MapBasedFeatureContainer> labelledWeights;
//    MapBasedFeatureContainer unlabelledWeights;
    MentionPairFeatureExtractor extractor;
    private StructWeights weights;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        weights = new StructWeights();
        logger.info("Initialize perceptron trainer");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        printProcessInfo(aJCas, logger);
        List<EventMention> allMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));
        List<EventMentionRelation> allMentionRelations = new ArrayList<>(JCasUtil.select(aJCas, EventMentionRelation.class));
        //feed the extractor with document information
        extractor = new MentionPairFeatureExtractor(aJCas);
        // a graph to be filled
        Graph mentionGraph = new Graph(allMentions, allMentionRelations);
        mentionGraph.fillGraph(extractor, weights);

//        System.out.println("Graph is ");
//        System.out.println(mentionGraph);

        //  decoding
        SubGraph predictedTree = bestFirstDecoding(mentionGraph);

        System.out.println("Predicted Tree is ");
        System.out.println(predictedTree);

        System.out.println("Labelled feature for the predicted Tree");
        System.out.println(predictedTree.getAllLabelledFeatures());

        if (!graphMatch(predictedTree, mentionGraph)) {
            SubGraph latentTree = getLatentTree(mentionGraph);

            System.out.println("Labelled feature for the latent Tree");
            System.out.println(latentTree);

            System.out.println("Latent Tree is ");
            System.out.println(latentTree.getAllLabelledFeatures());

            System.out.println("Labelled feature for the predicted Tree is now");
            System.out.println(predictedTree.getAllLabelledFeatures());

            StructDelta delta = predictedTree.getDelta(latentTree);
            System.out.println(delta);
            double loss = predictedTree.getLoss(latentTree);
            double tau = getUpdateWeight(delta, weights, loss);
            weights.update(delta, tau);

//            Utils.pause();
        }
    }

    public double getUpdateWeight(StructDelta delta, StructWeights weights, double loss) {
        return (delta.getScore(weights) + loss) / delta.getL2();
    }

    private boolean graphMatch(SubGraph predictedTree, Graph mentionGraph) {
        predictedTree.resolveTree();
        boolean corefMatch = Arrays.deepEquals(predictedTree.getCorefChains(), mentionGraph.getCorefChains());
        boolean linkMatch = true;

        for (Map.Entry<Edge.EdgeType, int[][]> predictEdgesWithType : predictedTree.getEdgeAdjacentList().entrySet()) {
            int[][] actualEdges = mentionGraph.getEdgeAdjacentList().get(predictEdgesWithType.getKey());
            if (!Arrays.deepEquals(predictEdgesWithType.getValue(), actualEdges)) {
                linkMatch = false;
            }
        }
        return corefMatch && linkMatch;
    }

    private SubGraph getLatentTree(Graph mentionGraph) {
        SubGraph latentTree = new SubGraph(mentionGraph.numNodes());
        Node[] nodes = mentionGraph.getNodes();

        mentionGraph.getCorefChains();

        logger.debug("Decoding the latent tree");

        for (int curr = 1; curr < nodes.length; curr++) {
            Pair<Edge, EdgeType> bestEdge = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int ant = 0; ant < curr; ant++) {
                Edge edge = mentionGraph.getEdges()[curr][ant];
                Pair<Edge.EdgeType, Double> bestCorrectLabelScore = edge.getCorrectLabelScore();
                if (bestCorrectLabelScore == null) {
                    continue;
                }
                Edge.EdgeType label = bestCorrectLabelScore.getValue0();
                double score = bestCorrectLabelScore.getValue1();
//                System.out.println(ant + " " + curr + " " + bestCorrectLabelScore);
                if (bestEdge == null) {
                    bestEdge = Pair.with(edge, label);
                } else {
                    if (score > bestScore) {
                        bestEdge = Pair.with(edge, label);
                        bestScore = score;
                    }
                }
            }

//            System.out.println(bestEdge);

            latentTree.addEdge(bestEdge.getValue0(), bestEdge.getValue1());
        }
        return latentTree;
    }

    private SubGraph bestFirstDecoding(Graph mentionGraph) {
        SubGraph bestFirstTree = new SubGraph(mentionGraph.numNodes());
        Node[] nodes = mentionGraph.getNodes();

        logger.debug("Decoding with best first");

        for (int curr = 1; curr < nodes.length; curr++) {
            Pair<Edge, Edge.EdgeType> bestEdge = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int ant = 0; ant < curr; ant++) {
                Edge edge = mentionGraph.getEdges()[curr][ant];
                Pair<Edge.EdgeType, Double> bestLabelScore = edge.getBestLabelScore(ant == 0);
                double score = bestLabelScore.getValue1();
                Edge.EdgeType label = bestLabelScore.getValue0();

                if (bestEdge == null) {
                    bestEdge = Pair.with(edge, label);
                    bestScore = score;
                } else if (score > bestScore) {
                    bestEdge = Pair.with(edge, label);
                    bestScore = score;
                }
            }

            bestFirstTree.addEdge(bestEdge.getValue0(), bestEdge.getValue1());
        }
        return bestFirstTree;
    }

    class PerceptronLooper extends LoopPipeline {
        final int maxIter;
        private int numIter = 0;
        private String outputPath;

        public PerceptronLooper(int maxIter, String outputPath) {
            this.maxIter = maxIter;
            this.outputPath = outputPath;
        }

        @Override
        protected boolean checkStopCriteria() {
            logger.info("Iteration : " + numIter);
            return numIter++ >= maxIter;
        }

        @Override
        protected void stopActions() {
            try {
                File outputFile = new File(outputPath);
                File outputFolder = outputFile.getParentFile();

                if (!outputFolder.exists()) {
                    outputFolder.mkdirs();
                }

                SerializationUtils.serialize(weights, new ObjectOutputStream(new FileOutputStream(new File(outputPath))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void run(String typeSystemName, final String parentDir, final String baseInputDir, int maxIter, String modelOutput) throws IOException, UIMAException {
        final TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);

        BasicLoopyPipeline blp = new BasicLoopyPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, parentDir, baseInputDir);
            }

            @Override
            public AnalysisEngineDescription[] buildPreprocessors() throws ResourceInitializationException {
                return new AnalysisEngineDescription[0];
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription trainer = AnalysisEngineFactory.createEngineDescription(
                        PaLatentTreeTrainer.class, typeSystemDescription
                );
                return new AnalysisEngineDescription[]{trainer};
            }

            @Override
            public AnalysisEngineDescription[] buildPostProcessors() throws ResourceInitializationException {
                return new AnalysisEngineDescription[0];
            }
        }, new PerceptronLooper(maxIter, modelOutput)
        );
        blp.run();
    }


    public static void main(String[] args) throws IOException, UIMAException {
        PaLatentTreeTrainer trainer = new PaLatentTreeTrainer();
        String typeSystemName = "TypeSystem";
        String parentDir = args[0];
        String modelOutput = args[1];
        String baseInputDir = "argument_extracted";
        int maxIter = 5;
        trainer.run(typeSystemName, parentDir, baseInputDir, maxIter, modelOutput);
    }
}
