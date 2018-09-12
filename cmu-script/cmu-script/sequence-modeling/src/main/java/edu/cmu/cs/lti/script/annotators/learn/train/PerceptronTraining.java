package edu.cmu.cs.lti.script.annotators.learn.train;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.cds.ml.features.CompactFeatureExtractor;
import edu.cmu.cs.lti.collections.TLongIntDoubleHashTable;
import edu.cmu.cs.lti.script.dist.BaseEventDist;
import edu.cmu.cs.lti.script.dist.GlobalUnigrmHwLocalUniformArgumentDist;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.script.model.MooneyEventRepre;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.script.utils.MultiMapUtils;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.BasicConvenience;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.ArrayBasedTwoLevelFeatureTable;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.TwoLevelFeatureTable;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectIntMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.mapdb.Fun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.core.SerializationHelper;

import java.io.File;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/7/15
 * Time: 10:48 PM
 */
public class PerceptronTraining extends AbstractLoggingAnnotator {
    public static final String PARAM_RANK_LIST_SIZE = "rankListSize";

    public static final String PARAM_MINI_BATCH_SIZE = "miniBatchDocuments";

    public static final String PARAM_FEATURE_NAMES = "featureNames";

    public static final String PARAM_MAX_SKIP_GRAM_N = "maxSkippedN";

    public static final String PARAM_TOP_RANK_TO_OPTIMIZE = "topToOptimize";

    public static TwoLevelFeatureTable trainingFeatureTable;

//    public static TwoLevelFeatureTable sumOfFeatures;

    public static long numSamplesProcessed = 0;

    TokenAlignmentHelper align = new TokenAlignmentHelper();
    CompactFeatureExtractor extractor;

    //some defaults, will be changed by parameters anyway
    int miniBatchSize = 1000;
    int rankListSize = 100;

    int maxSkippedN;

    @ConfigurationParameter(name = PARAM_TOP_RANK_TO_OPTIMIZE)
    int topRankToOptimize = 10;

    int topPredictionAsNegative = 1;

    double averageRankPercentage = 0;
    boolean debug = false;

    //can be made as parameters too
    int numArguments = 3;
    BaseEventDist noiseDist = new GlobalUnigrmHwLocalUniformArgumentDist(numArguments);
//    BaseEventDist noiseDist = new TopCappedUnigramEventDist(DataPool.unigramCounts, 500, numArguments);

    //data for the Mooney predictor
    static TObjectIntMap<TIntList>[] cooccCountMaps;
    static TObjectIntMap<TIntList>[] occCountMaps;
    static TObjectIntMap<String>[] headIdMaps;
    static float laplaceSmoothingParameter;

    long numTotalEvents;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        rankListSize = (Integer) aContext.getConfigParameterValue(PARAM_RANK_LIST_SIZE);
        miniBatchSize = (Integer) aContext.getConfigParameterValue(PARAM_MINI_BATCH_SIZE);
        String[] featureImplNames = (String[]) aContext.getConfigParameterValue(PARAM_FEATURE_NAMES);
        maxSkippedN = (Integer) aContext.getConfigParameterValue(PARAM_MAX_SKIP_GRAM_N);

        numSamplesProcessed = 0;

        logger.info(String.format("Perceptron training setup: rank list size [%d], batch size [%d], max skip [%d], " +
                        "optimize rank [%d]",
                rankListSize, miniBatchSize, maxSkippedN, topRankToOptimize));

        try {
            extractor = new CompactFeatureExtractor(trainingFeatureTable, featureImplNames, false);
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

        numTotalEvents = DataPool.predicateTotalCount;

        System.err.println("Number total events " + numTotalEvents);

    }

    public static void initializeParameters() {
        //        trainingFeatureTable = new TLongBasedFeatureHashTable();
        trainingFeatureTable = new ArrayBasedTwoLevelFeatureTable(DataPool.headIdMap.size() + 1);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            logger.debug("Ignored black listed file : " + article.getArticleName());
            return;
        }

        align.loadWord2Stanford(aJCas);
        align.loadFanse2Stanford(aJCas);

        List<ContextElement> chain = new ArrayList<>();
        List<LocalArgumentRepre> arguments = new ArrayList<>();

        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                LocalEventMentionRepre eventRep = LocalEventMentionRepre.fromEventMention(mention, align);
                chain.add(new ContextElement(aJCas, sent, mention, eventRep));
                Collections.addAll(arguments, eventRep.getArgs());
            }
        }

        Map<LocalEventMentionRepre, TLongIntDoubleHashTable> mention2Features = new HashMap<>();

        List<List<TLongIntDoubleHashTable>> batchedNegativeFeatures = new ArrayList<>();
        List<TLongIntDoubleHashTable> batchedCorrectFeatures = new ArrayList<>();

        extractor.prepareGlobalFeatures(chain);

        //for each sample
        for (int sampleIndex = 0; sampleIndex < chain.size(); sampleIndex++) {
            if (debug) {
                System.err.println(String.format("=============Sample %d============", sampleIndex));
            }
            ContextElement realSample = chain.get(sampleIndex);
            TLongIntDoubleHashTable correctFeature = extractor.getFeatures(chain, realSample, sampleIndex, maxSkippedN);
            Sentence sampleSent = realSample.getSent();

            PriorityQueue<Pair<Double, LocalEventMentionRepre>> scores = new PriorityQueue<>(rankListSize,
                    Collections.reverseOrder());

            Set<LocalEventMentionRepre> sampledCandidates = sampleCandidatesWithReal(arguments, realSample.getMention
                    ());

            int originalRank = 0;
            for (LocalEventMentionRepre sample : sampledCandidates) {
                TLongIntDoubleHashTable sampleFeature = extractor.getFeatures(chain,
                        new ContextElement(aJCas, sampleSent, realSample.getOriginalMention(), sample), sampleIndex,
                        maxSkippedN);

                double sampleScore;
                if (debug) {
                    sampleScore = trainingFeatureTable.dotProd(sampleFeature, DataPool.headWords);
                } else {
                    sampleScore = trainingFeatureTable.dotProd(sampleFeature);
                }

                scores.add(Pair.of(sampleScore, sample));
                mention2Features.put(sample, sampleFeature);

                if (debug) {
                    if (sample.mooneyMatch(realSample.getMention())) {
                        System.err.println(sample + " is the actual mention, sampled rank is " + originalRank + " " +
                                "among " + sampledCandidates.size() + ", score is " + sampleScore);
                    } else {
                        if (sampleScore != 0) {
                            System.err.println(sample + " is a noisy mention, sampled rank is " + originalRank + " " +
                                    "score is " + sampleScore);
                        }
                    }
                }
                originalRank++;
            }

            //update when prediction didn't fall into the top list
            Pair<Boolean, List<TLongIntDoubleHashTable>> updateDecision = prepareUpdate(
                    chain, sampleIndex, scores, realSample.getMention(), mention2Features);

            if (updateDecision.getKey()) {
                List<TLongIntDoubleHashTable> pseudoNegativeInstances = updateDecision.getRight();
//                System.err.println("Adding " + pseudoNegativeInstances.size() + " negative features");
                batchedNegativeFeatures.add(pseudoNegativeInstances);
                batchedCorrectFeatures.add(correctFeature);
            }

            numSamplesProcessed++;
            if (numSamplesProcessed % miniBatchSize == 0) {
                logger.info("Processed " + numSamplesProcessed + " samples");
                logger.info("Features lexical pairs just learnt " + trainingFeatureTable.getNumRows());
                logger.info("Average rank position for previous batch is : " + averageRankPercentage / miniBatchSize);
                averageRankPercentage = 0;
                BasicConvenience.printMemInfo(logger);
            }
        }
        perceptronUpdate(batchedCorrectFeatures, batchedNegativeFeatures);
    }

    private Pair<Boolean, List<TLongIntDoubleHashTable>> prepareUpdate(List<ContextElement> chain, int sampleIndex,
                                                                       PriorityQueue<Pair<Double,
                                                                               LocalEventMentionRepre>> scoredItems,
                                                                       LocalEventMentionRepre realMention,
                                                                       Map<LocalEventMentionRepre,
                                                                               TLongIntDoubleHashTable>
                                                                               mention2Features) {
        int realRank = -1;
        int rank = 0;


        List<TLongIntDoubleHashTable> negativeInstancesToTrain = new ArrayList<>();
        List<LocalEventMentionRepre> topPredictions = new ArrayList<>();

        int nonOriginalPredictionCount = 0;
        while (!scoredItems.isEmpty()) {
            Pair<Double, LocalEventMentionRepre> nextScoredItem = scoredItems.poll();
            LocalEventMentionRepre nextMention = nextScoredItem.getRight();

            if (nextMention.equals(realMention)) {
                realRank = rank;
                if (debug) {
                    System.err.println("\t[Real One] " + nextScoredItem + " ranks " + rank);
                }
                break;
            } else {
                if (nonOriginalPredictionCount < topRankToOptimize) {
                    topPredictions.add(nextScoredItem.getRight());
                    nonOriginalPredictionCount++;
                }
            }
            rank++;
        }


        boolean considerCorrect = false;
        if (rank < topRankToOptimize) {
            considerCorrect = true;
            if (debug) {
                System.err.println(String.format("\t[Consider correct] because original rank at %d , within top %d",
                        rank, topRankToOptimize));
            }
        } else {
            for (int i = 0; i < topPredictionAsNegative && i < topPredictions.size(); i++) {
                negativeInstancesToTrain.add(mention2Features.get(topPredictions.get(i)));
            }
        }


        if (debug) {
            System.err.println(String.format("Predicted rank is %d  among %d", realRank, rankListSize));
        }

        averageRankPercentage += realRank * 1.0 / rankListSize;

        //update when we do not consider them as correct
        if (!considerCorrect) {
            return Pair.of(true, negativeInstancesToTrain);
        } else {
            return Pair.of(false, null);
        }
    }

    private Map<LocalEventMentionRepre, Double> getGuidingScores(List<ContextElement> chain, int sampleIndex,
                                                                 LocalEventMentionRepre realMention,
                                                                 List<LocalEventMentionRepre> topPredictions) {
        Map<LocalEventMentionRepre, Double> pseudoNegatives = new HashMap<>();

        double guidedRealScore = guidedScorer(chain, sampleIndex, realMention);

        for (LocalEventMentionRepre topPrediction : topPredictions) {
            double guidedPredictionScore = guidedScorer(chain, sampleIndex, topPrediction);
            if (guidedPredictionScore < guidedRealScore) {
                pseudoNegatives.put(topPrediction, guidedPredictionScore);
            }
        }
        if (debug) {
            System.err.println("Real score is " + guidedRealScore);
            System.err.println(String.format("[Number of top predictions [%d], Choose as negatives [%d]",
                    topPredictions.size(), pseudoNegatives.size()));
            System.err.println("[Pseudo negatives] " + pseudoNegatives);
        }

        return pseudoNegatives;
    }

    private double guidedScorer(List<ContextElement> chain, int sampleIndex, LocalEventMentionRepre candidateEvm) {
        double score = 0;
        for (int i = 0; i < sampleIndex; i++) {
            Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>>
                    substitutedForm = KarlMooneyScriptCounter.
                    firstBasedSubstitution(chain.get(i).getMention(), candidateEvm);
            double precedingScore = conditionalFollowing(MooneyEventRepre.fromTuple(substitutedForm.a),
                    MooneyEventRepre.fromTuple(substitutedForm.b), laplaceSmoothingParameter, numTotalEvents);
            score += precedingScore;
        }

        for (int i = sampleIndex + 1; i < chain.size(); i++) {
            Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>>
                    substitutedForm = KarlMooneyScriptCounter.
                    firstBasedSubstitution(candidateEvm, chain.get(i).getMention());
            double followingScore = conditionalFollowing(MooneyEventRepre.fromTuple(substitutedForm.a),
                    MooneyEventRepre.fromTuple(substitutedForm.b), laplaceSmoothingParameter, numTotalEvents);
            score += followingScore;
        }
        return score;
    }

    private double conditionalFollowing(MooneyEventRepre former, MooneyEventRepre latter, double smoothParameter,
                                        long numTotalEvents) {
        Pair<Integer, Integer> counts = MultiMapUtils.getCounts(former, latter, cooccCountMaps, occCountMaps,
                headIdMaps);

        double cooccCountSmoothed = counts.getRight() + smoothParameter;
        double formerOccCountSmoothed = counts.getLeft() + numTotalEvents * smoothParameter;

        //add one smoothing
        return Math.log(cooccCountSmoothed / formerOccCountSmoothed);
    }


    /**
     * Return a list of generated noise union with the real sample
     *
     * @param arguments  Arguments to be used to generate sample
     * @param realSample Real event example
     * @return A list of noise examples and the real one
     */
    private Set<LocalEventMentionRepre> sampleCandidatesWithReal(List<LocalArgumentRepre> arguments,
                                                                 LocalEventMentionRepre realSample) {
        Set<LocalEventMentionRepre> samples = new HashSet<>();
        boolean containsReal = false;

        for (int i = 0; i < rankListSize; i++) {
            Pair<LocalEventMentionRepre, Double> noise = noiseDist.draw(arguments);
            LocalEventMentionRepre noiseRep = noise.getKey();
            if (noiseRep.mooneyMatch(realSample)) {
                containsReal = true;
            }
            samples.add(noiseRep);
        }

        if (!containsReal) {
            samples.add(realSample);
        }
        return samples;
    }

    private void perceptronUpdate(List<TLongIntDoubleHashTable> listOfPositiveFeatures,
                                  List<List<TLongIntDoubleHashTable>> allPossibleNegativeFeatures) {
        for (int i = 0; i < listOfPositiveFeatures.size(); i++) {
            TLongIntDoubleHashTable positiveFeature = listOfPositiveFeatures.get(i);
            List<TLongIntDoubleHashTable> negativeFeatures = allPossibleNegativeFeatures.get(i);

            updateUniquePositiveFeatures(positiveFeature, negativeFeatures);
        }
    }

    private void vanillaUpdate(TLongIntDoubleHashTable positiveFeature, List<TLongIntDoubleHashTable>
            negativeFeatures) {
        trainingFeatureTable.adjustBy(positiveFeature, negativeFeatures.size());
        for (TLongIntDoubleHashTable currentNegative : negativeFeatures) {
            trainingFeatureTable.adjustBy(currentNegative, -1);
        }
    }

    private void updateUniquePositiveFeatures(TLongIntDoubleHashTable positiveFeature, List<TLongIntDoubleHashTable>
            negativeFeatures) {
        for (TLongObjectIterator<TIntDoubleMap> correctTableIter = positiveFeature.iterator(); correctTableIter
                .hasNext(); ) {
            correctTableIter.advance();

            long rowKey = correctTableIter.key();

            for (TIntDoubleIterator correctCellIter = correctTableIter.value().iterator(); correctCellIter.hasNext();
                    ) {
                correctCellIter.advance();
                int colKey = correctCellIter.key();

                boolean containsInNegative = false;

                for (TLongIntDoubleHashTable negativeFeature : negativeFeatures) {
                    if (negativeFeature.contains(rowKey, colKey)) {
                        containsInNegative = true;
                    }
                }

                if (!containsInNegative) {
                    trainingFeatureTable.adjustOrPutValue(rowKey, colKey, 1, 1);
                }
            }
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Finish one epoch, totally  " + numSamplesProcessed + " samples processed so far");
        logger.info("Processed " + numSamplesProcessed + " samples. Residual size is " + numSamplesProcessed %
                miniBatchSize);
        logger.info("Features lexical pairs learnt " + trainingFeatureTable.getNumRows());
        logger.info("Average rank position for the residual batch: " + averageRankPercentage / (numSamplesProcessed %
                miniBatchSize));
        averageRankPercentage = 0;
        BasicConvenience.printMemInfo(logger);
    }


    public static void main(String[] args) throws Exception {
        Logger logger = LoggerFactory.getLogger(PerceptronTraining.class);
        Configuration config = new Configuration(new File(args[0]));

        int maxIter = config.getInt("edu.cmu.cs.lti.cds.sgd.iter", 5);
        String modelExt = config.get("edu.cmu.cs.lti.cds.model.ext");
        String modelStoragePath = config.get("edu.cmu.cs.lti.cds.perceptron.model.path");

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path");
        int miniBatchNum = config.getInt("edu.cmu.cs.lti.cds.minibatch", 1000);
        String[] featureNames = config.getList("edu.cmu.cs.lti.cds.features");
        String featurePackage = config.get("edu.cmu.cs.lti.cds.features.packagename");
        String semLinkPath = config.get("edu.cmu.cs.lti.cds.db.semlink.path");
        int maxSkipN = config.getInt("edu.cmu.cs.lti.cds.max.n", 9);

        int topRankToOptimize = config.getInt("edu.cmu.cs.lti.cds.perceptron.top.rank.optimize", 10);
        int rankListSize = config.getInt("edu.cmu.cs.lti.cds.perceptron.ranklist.size", 500);

        String blackListFileName = config.get("edu.cmu.cs.lti.cds.blacklist");


        String modelSuffix = Joiner.on("_").join(featureNames);

        logger.info("Model will be stored with suffix : " + modelSuffix);


        //make complete class name
        for (int i = 0; i < featureNames.length; i++) {
            featureNames[i] = featurePackage + "." + featureNames[i];
        }

        String paramTypeSystemDescriptor = "TypeSystem";

        //prepare data
        logger.info("Loading data.");
        DataPool.loadHeadStatistics(config, false);
        DataPool.readBlackList(new File(blackListFileName));
        DataPool.loadSemLinkData(semLinkPath);
        logger.info("Finish data loading.");

        logger.info("# predicates " + DataPool.headIdMap.size());
        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        logger.info("Running " + PerceptronTraining.class.getName());

        AnalysisEngineDescription trainer = AnalysisEngineFactory.createEngineDescription(PerceptronTraining.class,
                typeSystemDescription,
                PerceptronTraining.PARAM_RANK_LIST_SIZE, rankListSize,
                PerceptronTraining.PARAM_MINI_BATCH_SIZE, miniBatchNum,
                PerceptronTraining.PARAM_FEATURE_NAMES, featureNames,
                PerceptronTraining.PARAM_MAX_SKIP_GRAM_N, maxSkipN,
                PerceptronTraining.PARAM_TOP_RANK_TO_OPTIMIZE, topRankToOptimize
        );

        PerceptronTraining.initializeParameters();
        BasicConvenience.printMemInfo(logger, "Beginning memory");

        for (int i = 0; i < maxIter; i++) {
            String modelOutputPath = modelStoragePath + "_" + modelSuffix + "_" + i + modelExt;

            SimplePipeline.runPipeline(reader, trainer);
            File modelDirParent = new File(modelStoragePath).getParentFile();

            if (!modelDirParent.exists()) {
                modelDirParent.mkdirs();
            }

            logger.info("Storing this model to " + modelOutputPath);
            SerializationHelper.write(modelOutputPath, PerceptronTraining.trainingFeatureTable);
        }
    }
}