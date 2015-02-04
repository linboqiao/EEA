package edu.cmu.cs.lti.script.annotators.learn.train;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.cds.ml.features.CompactFeatureExtractor;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import edu.cmu.cs.lti.script.dist.GlobalUnigrmHwLocalUniformArgumentDist;
import edu.cmu.cs.lti.script.model.ContextElement;
import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.BasicConvenience;
import edu.cmu.cs.lti.utils.ArrayBasedTwoLevelFeatureTable;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.TwoLevelFeatureTable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import weka.core.SerializationHelper;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

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

    public static final String PARAM_SKIP_GRAM_N = "skipgramn";

    public static TwoLevelFeatureTable trainingFeatureTable;

//    public static TwoLevelFeatureTable sumOfFeatures;

    public static long numSamplesProcessed = 0;

    TokenAlignmentHelper align = new TokenAlignmentHelper();
    CompactFeatureExtractor extractor;

    //some defaults, will be changed by parameters anyway
    int miniBatchSize = 100;
    int rankListSize = 100;
    int numArguments = 3;

    int skipGramN = 2;

    int topNegatives = 5;

    double averageRankPercentage = 0;

    Random randomGenerator = new Random();

    GlobalUnigrmHwLocalUniformArgumentDist noiseDist = new GlobalUnigrmHwLocalUniformArgumentDist();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        rankListSize = (Integer) aContext.getConfigParameterValue(PARAM_RANK_LIST_SIZE);
        miniBatchSize = (Integer) aContext.getConfigParameterValue(PARAM_MINI_BATCH_SIZE);
        String[] featureImplNames = (String[]) aContext.getConfigParameterValue(PARAM_FEATURE_NAMES);
        skipGramN = (Integer) aContext.getConfigParameterValue(PARAM_SKIP_GRAM_N);

//        trainingFeatureTable = new TLongBasedFeatureHashTable();
        trainingFeatureTable = new ArrayBasedTwoLevelFeatureTable(DataPool.headIdMap.size());
        numSamplesProcessed = 0;

        try {
            extractor = new CompactFeatureExtractor(trainingFeatureTable, featureImplNames);
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            logger.fine("Ignored black listed file : " + article.getArticleName());
            return;
        }

        align.loadWord2Stanford(aJCas);
        align.loadFanse2Stanford(aJCas);

        List<ContextElement> chain = new ArrayList<>();
        List<LocalArgumentRepre> arguments = new ArrayList<>();

        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                LocalEventMentionRepre eventRep = LocalEventMentionRepre.fromEventMention(mention, align);
                chain.add(new ContextElement(aJCas, sent, mention.getHeadWord(), eventRep));
                Collections.addAll(arguments, eventRep.getArgs());
            }
        }

        Map<LocalEventMentionRepre, TLongShortDoubleHashTable> mention2Features = new HashMap<>();

        //for each sample
        for (int sampleIndex = 0; sampleIndex < chain.size(); sampleIndex++) {
            ContextElement realSample = chain.get(sampleIndex);
            TLongShortDoubleHashTable correctFeatures = extractor.getFeatures(chain, realSample, sampleIndex, skipGramN, false);
            Sentence sampleSent = realSample.getSent();

            PriorityQueue<Pair<Double, LocalEventMentionRepre>> scores = new PriorityQueue<>(rankListSize, Collections.reverseOrder());

            int originalRank = 0;
            for (LocalEventMentionRepre sample : sampleCandidatesWithReal(arguments, realSample.getMention())) {
                TLongShortDoubleHashTable sampleFeature = extractor.getFeatures(chain, new ContextElement(aJCas, sampleSent, realSample.getHead(), sample), sampleIndex, skipGramN, false);
                double sampleScore = trainingFeatureTable.dotProd(sampleFeature, DataPool.headWords);
                scores.add(Pair.of(sampleScore, sample));
                mention2Features.put(sample, sampleFeature);

                if (sample.mooneyMatch(realSample.getMention())) {
                    System.err.println(sample + " is correct, Original rank is " + originalRank + " score is " + sampleScore);
                } else {
                    if (sampleScore != 0) {
                        System.err.println(sample + " is wrong, Original rank is " + originalRank + " score is " + sampleScore);
                    }
                }

                originalRank++;
            }

            int realRank = -1;
            int rank = 0;

            int negativeCount = 0;

            List<TLongShortDoubleHashTable> currentBestSampleFeatures = new ArrayList<>();
            while (!scores.isEmpty()) {
                Pair<Double, LocalEventMentionRepre> nextScoredItem = scores.poll();
                if (nextScoredItem.getRight().mooneyMatch(realSample.getMention())) {
                    realRank = rank;
                    break;
                } else {
                    if (negativeCount < topNegatives) {
                        currentBestSampleFeatures.add(mention2Features.get(nextScoredItem.getRight()));
                        negativeCount++;
                    }
                }
                rank++;
            }

            System.err.println(String.format("Real rank is %d  among %d", realRank, rankListSize));
            if (realRank != 0) {
                perceptronUpdate(correctFeatures, currentBestSampleFeatures);
            }

            averageRankPercentage += realRank * 1.0 / rankListSize;
            numSamplesProcessed++;

            if (numSamplesProcessed % miniBatchSize == 0) {
                logger.info("Features lexical pairs just learnt " + trainingFeatureTable.getNumRows());
                logger.info("Processed " + numSamplesProcessed + " samples");
                logger.info("Average rank position for previous batch is : " + averageRankPercentage / miniBatchSize);
                averageRankPercentage = 0;
                BasicConvenience.printMemInfo(logger);
            }
        }
    }

    /**
     * Return a list of generated noise union with the real sample
     *
     * @param arguments
     * @param realSample
     * @return
     */
    private List<LocalEventMentionRepre> sampleCandidatesWithReal(List<LocalArgumentRepre> arguments, LocalEventMentionRepre realSample) {
        List<LocalEventMentionRepre> samples = new ArrayList<>();
        boolean containsReal = false;

        for (int i = 0; i < rankListSize; i++) {
            Pair<LocalEventMentionRepre, Double> noise = noiseDist.draw(arguments, numArguments);
            LocalEventMentionRepre noiseRep = noise.getKey();

            if (noiseRep.mooneyMatch(realSample)) {
                containsReal = true;
            }

            samples.add(noiseRep);
        }

        if (!containsReal) {
            //replace the last sample with the real one
            samples.remove(samples.size() - 1);
            samples.add(randomGenerator.nextInt(samples.size()), realSample);
        }
        return samples;
    }


    private void perceptronUpdate(TLongShortDoubleHashTable correctFeatures, List<TLongShortDoubleHashTable> currentTops) {
        trainingFeatureTable.adjustBy(correctFeatures, 1);

        for (TLongShortDoubleHashTable currentTop : currentTops) {
            trainingFeatureTable.adjustBy(currentTop, -1);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Finish one epoch, totally  " + numSamplesProcessed + " samples processed so far");
        logger.info("Processed " + numSamplesProcessed + " samples. Residual size is " + numSamplesProcessed % miniBatchSize);
        logger.info("Features lexical pairs learnt " + trainingFeatureTable.getNumRows());
        logger.info("Average rank position for the residual batch: " + averageRankPercentage / (numSamplesProcessed % miniBatchSize));
        averageRankPercentage = 0;
        BasicConvenience.printMemInfo(logger);
    }

    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger(PerceptronTraining.class.getName());

        Configuration config = new Configuration(new File(args[0]));
        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path");
        int maxIter = config.getInt("edu.cmu.cs.lti.cds.sgd.iter");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String blackListFileName = config.get("edu.cmu.cs.lti.cds.blacklist");
        String modelStoragePath = config.get("edu.cmu.cs.lti.cds.perceptron.model.path");
        int miniBatchNum = config.getInt("edu.cmu.cs.lti.cds.minibatch");
        String modelExt = config.get("edu.cmu.cs.lti.cds.model.ext");
        String[] featureNames = config.getList("edu.cmu.cs.lti.cds.features");
        String featurePackage = config.get("edu.cmu.cs.lti.cds.features.packagename");
        int skipgramN = config.getInt("edu.cmu.cs.lti.cds.skipgram.n");

        int rankListSize = config.getInt("edu.cmu.cs.lti.cds.perceptron.ranklist.size");

        String modelSuffix = Joiner.on("_").join(featureNames);

        //make complete class name
        for (int i = 0; i < featureNames.length; i++) {
            featureNames[i] = featurePackage + "." + featureNames[i];
        }

        String paramTypeSystemDescriptor = "TypeSystem";

        //prepare data
        logger.info("Loading data");
        DataPool.loadHeadStatistics(config, false);
        DataPool.readBlackList(new File(blackListFileName));
        DataPool.loadKmCooccMap(dbPath, dbNames[0], KarlMooneyScriptCounter.defaultCooccMapName);
        DataPool.loadEventUnigramCounts(config);

        logger.info("# predicates " + DataPool.headIdMap.size());

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        logger.info("Running " + CompactGlobalNegativeTrainer.class.getName());

        AnalysisEngineDescription trainer = CustomAnalysisEngineFactory.createAnalysisEngine(PerceptronTraining.class, typeSystemDescription,
                PerceptronTraining.PARAM_RANK_LIST_SIZE, rankListSize,
                PerceptronTraining.PARAM_MINI_BATCH_SIZE, miniBatchNum,
                PerceptronTraining.PARAM_FEATURE_NAMES, featureNames,
                PerceptronTraining.PARAM_SKIP_GRAM_N, skipgramN);

        BasicConvenience.printMemInfo(logger, "Beginning memory");

        for (int i = 0; i < maxIter; i++) {
            String modelOutputPath = modelStoragePath + "_" + modelSuffix + "_" + i + modelExt;
            String averageModelOutputPath = modelOutputPath + "_average";

            SimplePipeline.runPipeline(reader, trainer);
            File modelDirParent = new File(modelStoragePath).getParentFile();

            if (!modelDirParent.exists()) {
                modelDirParent.mkdirs();
            }

            logger.info("Storing this model to " + modelOutputPath);
            SerializationHelper.write(modelOutputPath, PerceptronTraining.trainingFeatureTable);

//            TLongShortDoubleHashTable averageParameters = PerceptronTraining.sumOfFeatures;
//            averageParameters.multiplyBy(1.0 / PerceptronTraining.numSamplesProcessed);
//            logger.info("Storing the averaged model to " + modelOutputPath);
//            SerializationHelper.write(averageModelOutputPath, averageParameters);
        }
    }
}