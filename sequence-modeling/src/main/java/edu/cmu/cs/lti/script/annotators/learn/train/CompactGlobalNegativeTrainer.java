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
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.TLongBasedFeatureTable;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.TwoLevelFeatureTable;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.iterator.TShortDoubleIterator;
import gnu.trove.map.TShortDoubleMap;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * The Negative Sampling Training:
 * 1. The objective is to discriminate real samples with generated samples
 * 2. It is not asymptotic consistent
 * 3. A trick is used to help training: some samples are rejected if they consist of one correct feature being seen
 * 4. The problem of the trick is: more and more will be rejected and the training will be slow; it requires to
 * <p/>
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/31/14
 * Time: 5:52 PM
 */
public class CompactGlobalNegativeTrainer extends AbstractLoggingAnnotator {
    public static final String PARAM_NEGATIVE_NUMBERS = "negativeNumbers";

    public static final String PARAM_MINI_BATCH_SIZE = "miniBatchDocuments";

    public static final String PARAM_FEATURE_NAMES = "featureNames";

    public static final String PARAM_SKIP_GRAM_N = "skipgramn";

    TokenAlignmentHelper align = new TokenAlignmentHelper();
    CompactFeatureExtractor extractor;

    //a more compact form in storing such parameters
    public static final TwoLevelFeatureTable trainingUsedCompactWeights = new TLongBasedFeatureTable();
    //ada grad memory
    public static final TLongShortDoubleHashTable compactAdaGradMemory = new TLongShortDoubleHashTable();
    //sample counter
    public static long numSampleProcessed = 0;


    //some defaults
    int miniBatchSize = 100;
    int numNoise = 25;
    int numArguments = 3;

    int skipGramN = 2;

    //    UnigramEventDist noiseDist = new UnigramEventDist(DataPool.unigramCounts, DataPool.eventUnigramTotalCount);
    GlobalUnigrmHwLocalUniformArgumentDist noiseDist = new GlobalUnigrmHwLocalUniformArgumentDist();

    TLongShortDoubleHashTable cumulativeGradient = new TLongShortDoubleHashTable();

    double cumulativeObjective = 0;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        numNoise = (Integer) aContext.getConfigParameterValue(PARAM_NEGATIVE_NUMBERS);
        miniBatchSize = (Integer) aContext.getConfigParameterValue(PARAM_MINI_BATCH_SIZE);
        String[] featureImplNames = (String[]) aContext.getConfigParameterValue(PARAM_FEATURE_NAMES);
        skipGramN = (Integer) aContext.getConfigParameterValue(PARAM_SKIP_GRAM_N);
        try {
            extractor = new CompactFeatureExtractor(trainingUsedCompactWeights, featureImplNames);
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        numSampleProcessed = 0;
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            logger.fine("Ignored black listed file : " + article.getArticleName());
            return;
        }

        align.loadWord2Stanford(aJCas);
        List<ContextElement> chain = new ArrayList<>();
        List<LocalArgumentRepre> arguments = new ArrayList<>();

        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                LocalEventMentionRepre eventRep = LocalEventMentionRepre.fromEventMention(mention, align);
                chain.add(new ContextElement(aJCas, sent, mention.getHeadWord(), eventRep));
                Collections.addAll(arguments, eventRep.getArgs());
            }
        }

        //for each sample
        for (int sampleIndex = 0; sampleIndex < chain.size(); sampleIndex++) {
            ContextElement realSample = chain.get(sampleIndex);
            TLongShortDoubleHashTable features = extractor.getFeatures(chain, realSample, sampleIndex, skipGramN, false);
            Sentence sampleSent = realSample.getSent();
            //generate noise samples
            List<TLongShortDoubleHashTable> noiseSamples = new ArrayList<>();
            for (int i = 0; i < numNoise; i++) {
                Pair<LocalEventMentionRepre, Double> noise = noiseDist.draw(arguments, numArguments);
                LocalEventMentionRepre noiseRep = noise.getKey();
                TLongShortDoubleHashTable noiseFeature = extractor.getFeatures(chain, new ContextElement(aJCas, sampleSent, realSample.getHead(), noiseRep), sampleIndex, skipGramN, true);
                if (noiseFeature != null) {
                    noiseSamples.add(noiseFeature);
                }
            }
            //cumulative the gradient so far, and compute sample cost
            this.cumulativeObjective += gradientAscent(noiseSamples, features);
        }

        //treat one chain as one sample
        numSampleProcessed++;

        if (numSampleProcessed % miniBatchSize == 0) {
            logger.info("Features lexical pairs just learnt " + trainingUsedCompactWeights.getNumRows());
            logger.info("Processed " + numSampleProcessed + " samples");
            logger.info("Average gain for previous batch is : " + cumulativeObjective / miniBatchSize);
            logger.info("Committing " + cumulativeGradient.getNumRows() + " lexical pairs");
            update();
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        logger.info("Finish one epoch, totally  " + numSampleProcessed + " samples processed so far");
        logger.info("Processed " + numSampleProcessed + " samples. Residual size is " + numSampleProcessed % miniBatchSize);
        logger.info("Features lexical pairs learnt " + trainingUsedCompactWeights.getNumRows());
        logger.info("Average cumulative gain for the residual batch: " + cumulativeObjective / (numSampleProcessed % miniBatchSize));
        update();
//        trainingUsedCompactWeights.dump(new PrintWriter(System.err));
    }

    private double gradientAscent(List<TLongShortDoubleHashTable> noiseSamples, TLongShortDoubleHashTable dataSample) {
        //start by assigning gradient as x_w
        // g = x_w
        TLongShortDoubleHashTable gradient = dataSample;

//        double scoreTrue = DataPool.trainingUsedCompactWeights.dotProd(dataSample, extractor.getFeatureNamesByIndex());
        double scoreTrue = trainingUsedCompactWeights.dotProd(dataSample);

        double sigmoidTrue = sigmoid(scoreTrue);

        //multiple x_w by (1- sigmoid)
        // g = ( Lw(u) - sigmoid(\theta * x_w) ) x_w, where Lw(u) = 1
        gradient.multiplyBy((1 - sigmoidTrue));

        //calculate local objective
        // log (sigmoid( score of true))
        double localObjective = Math.log(sigmoidTrue);

//        if (scoreTrue != 0) {
//            System.err.println("True scores " + scoreTrue + " " + sigmoidTrue + " ");
//            trainingUsedCompactWeights.dotProd(dataSample, extractor.getFeatureNamesByIndex());
//            System.err.println(dataSample.dump(headWords, extractor.getFeatureNamesByIndex()));
//        }

        for (TLongShortDoubleHashTable noiseSample : noiseSamples) {
//            double scoreNoise = trainingUsedCompactWeights.dotProd(dataSample, extractor.getFeatureNamesByIndex());
            double scoreNoise = trainingUsedCompactWeights.dotProd(noiseSample);
            double sigmoidNoise = sigmoid(scoreNoise);

//            if (scoreNoise != 0) {
//                System.err.println("Noise scores " + scoreTrue + " " + sigmoidTrue + " ");
//                trainingUsedCompactWeights.dotProd(dataSample, extractor.getFeatureNamesByIndex());
//            }

            // log (sigmoid( - score of noise))
            // i.e  log ( 1 - sigmoid(score of noise))
            localObjective += Math.log(1 - sigmoidNoise);

            //multiple sigmiod_noise by noise features x_w'
            // g += ( Lw'(u) - sigmoid(\theta * x_w') ) x_w', where Lw(u) = 0
            //note that this directly change the noise feature itself
            noiseSample.multiplyBy(sigmoidNoise);
            gradient.minusBy(noiseSample);

//            if (scoreNoise < 0) {
//            System.err.println(scoreNoise + " " + sigmoidNoise);
//            System.err.println(noiseSample.dump(DataPool.headWords, extractor.getFeatureNamesByIndex()));
//            }
        }

        //update the cumulative gradient;
        for (TLongObjectIterator<TShortDoubleMap> rowIter = gradient.iterator(); rowIter.hasNext(); ) {
            rowIter.advance();
            for (TShortDoubleIterator cellIter = rowIter.value().iterator(); cellIter.hasNext(); ) {
                cellIter.advance();
                cumulativeGradient.adjustOrPutValue(rowIter.key(), cellIter.key(), cellIter.value(), cellIter.value());
            }
        }
        //return the objective
        return localObjective;
    }

    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private void update() {
        cumulativeObjective = 0;
        BasicConvenience.printMemInfo(logger);
        logger.info("Updating");
        adaGradUpdate(0.1);
        logger.info("Update Done");
    }

    private void adaGradUpdate(double eta) {
        for (TLongObjectIterator<TShortDoubleMap> rowIter = cumulativeGradient.iterator(); rowIter.hasNext(); ) {
            rowIter.advance();

            for (TShortDoubleIterator cellIter = rowIter.value().iterator(); cellIter.hasNext(); ) {
                cellIter.advance();
                double g = cellIter.value();
                if (g != 0) {
                    double gSq = g * g;
                    double cumulativeGsq = compactAdaGradMemory.adjustOrPutValue(rowIter.key(), cellIter.key(), gSq, gSq);
                    double update = eta * g / Math.sqrt(cumulativeGsq);
                    if (Double.isNaN(g)) {
                        System.out.println(rowIter.key() + " " + rowIter.value() + update);
                    }
                    trainingUsedCompactWeights.adjustOrPutValue(rowIter.key(), cellIter.key(), update, update);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger(CompactGlobalNegativeTrainer.class.getName());

        Configuration config = new Configuration(new File(args[0]));
        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path");
        int maxIter = config.getInt("edu.cmu.cs.lti.cds.sgd.iter");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String[] countingDbFileNames = config.getList("edu.cmu.cs.lti.cds.headcount.files");
        String blackListFileName = config.get("edu.cmu.cs.lti.cds.blacklist");
        String modelStoragePath = config.get("edu.cmu.cs.lti.cds.negative.model.path");
        int noiseNum = config.getInt("edu.cmu.cs.lti.cds.negative.noisenum");
        int miniBatchNum = config.getInt("edu.cmu.cs.lti.cds.minibatch");
        String modelExt = config.get("edu.cmu.cs.lti.cds.model.ext");
        String[] featureNames = config.getList("edu.cmu.cs.lti.cds.features");
        String featurePackage = config.get("edu.cmu.cs.lti.cds.features.packagename");
        int skipgramN = config.getInt("edu.cmu.cs.lti.cds.skipgram.n");

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

        AnalysisEngineDescription trainer = CustomAnalysisEngineFactory.createAnalysisEngine(CompactGlobalNegativeTrainer.class, typeSystemDescription,
                CompactGlobalNegativeTrainer.PARAM_NEGATIVE_NUMBERS, noiseNum,
                CompactGlobalNegativeTrainer.PARAM_MINI_BATCH_SIZE, miniBatchNum,
                CompactGlobalNegativeTrainer.PARAM_FEATURE_NAMES, featureNames,
                CompactGlobalNegativeTrainer.PARAM_SKIP_GRAM_N, skipgramN);

        BasicConvenience.printMemInfo(logger, "Beginning memory");

        for (int i = 0; i < maxIter; i++) {
            String modelOutputPath = modelStoragePath + "_" + modelSuffix + "_" + i + modelExt;
            logger.info("Storing this model to " + modelOutputPath);

            SimplePipeline.runPipeline(reader, trainer);
            File modelDirParent = new File(modelStoragePath).getParentFile();

            if (!modelDirParent.exists()) {
                modelDirParent.mkdirs();
            }

            SerializationHelper.write(modelOutputPath, CompactGlobalNegativeTrainer.trainingUsedCompactWeights);
        }
    }
}