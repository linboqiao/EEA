package edu.cmu.cs.lti.script.annotators.learn.test;

import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import edu.cmu.cs.lti.script.features.CompactFeatureExtractor;
import edu.cmu.cs.lti.script.model.*;
import edu.cmu.cs.lti.script.runners.learn.test.CompactLogLinearTestRunner;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Comparators;
import edu.cmu.cs.lti.utils.TLongBasedFeatureTable;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/4/14
 * Time: 3:03 PM
 */
public class MultiArgumentClozeTester extends AbstractLoggingAnnotator {
    public static final String PARAM_HEAD_COUNT_DB_NAMES = "headCountDbFileNames";

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String PARAM_IGNORE_LOW_FREQ = "ignoreLowFreq";

    public static final String PARAM_CLOZE_DIR_PATH = "clozePath";

    public static final String PARAM_MODEL_PATH = "modelPath";

    public static final String PARAM_FEATURE_NAMES = "featureNames";

    public static final String PARAM_SKIP_GRAM_N = "skipgramn";

    private boolean ignoreLowFreq;

//    private Map<String, Fun.Tuple2<Integer, Integer>>[] headTfDfMaps;

    private String clozeExt = ".txt";

    private File clozeDir;

    private int skipGramN = 2;

    //make it parameter
    int numArguments = 3;

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private CompactFeatureExtractor extractor;

    private String modelPath;
    private TLongBasedFeatureTable compactWeights;

//    private TShortObjectMap<String> featureNames;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);

        skipGramN = (Integer) aContext.getConfigParameterValue(PARAM_SKIP_GRAM_N);


        if (aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ) != null) {
            ignoreLowFreq = (Boolean) aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ);
        } else {
            ignoreLowFreq = true;
        }

        if (ignoreLowFreq) {
            String[] countingDbFileNames = (String[]) aContext.getConfigParameterValue(PARAM_HEAD_COUNT_DB_NAMES);
//            headTfDfMaps = DbManager.getMaps(dbPath, countingDbFileNames, EventMentionHeadCounter.defaultMentionHeadMapName);
        }

        modelPath = (String) aContext.getConfigParameterValue(PARAM_MODEL_PATH);

        logger.info("Loading from " + modelPath);

        try {
            compactWeights = (TLongBasedFeatureTable) SerializationHelper.read(modelPath);
            String[] featureImplNames = (String[]) aContext.getConfigParameterValue(PARAM_FEATURE_NAMES);
            try {
                extractor = new CompactFeatureExtractor(compactWeights, featureImplNames);
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        clozeDir = new File((String) aContext.getConfigParameterValue(PARAM_CLOZE_DIR_PATH));
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            //ignore this blacklisted file;
            logger.info("Ignored black listed file");
            return;
        }

        String clozeFileName = UimaConvenience.getShortDocumentName(aJCas) + ".gz_" + UimaConvenience.getOffsetInSource(aJCas) + clozeExt;
        Triple<List<MooneyEventRepre>, Integer, String> mooneyClozeTask = getMooneyStyleCloze(clozeFileName);
        if (mooneyClozeTask == null) {
            logger.info("Cloze file removed due to duplication or empty");
            return;
        }

        align.loadWord2Stanford(aJCas);

        //the actual chain is got here
        List<ContextElement> regularChain = getHighFreqEventMentions(aJCas);

        if (regularChain.size() == 0) {
            return;
        }

        String outputBase = mooneyClozeTask.getRight() + "_res";

        int clozeIndex = mooneyClozeTask.getMiddle();
        List<MooneyEventRepre> mooneyChain = mooneyClozeTask.getLeft();

        if (mooneyChain.size() != regularChain.size()) {
            throw new IllegalArgumentException("Test data and document have different size! " + mooneyChain.size() + " " + regularChain.size());
        }

        for (int i = 0; i < mooneyChain.size(); i++) {
            MooneyEventRepre mooneyEventRepre = mooneyChain.get(i);
            for (int slotId = 0; slotId < mooneyEventRepre.getAllArguments().length; slotId++) {
                LocalArgumentRepre argument = regularChain.get(i).getMention().getArg(slotId);
                if (argument != null) {
                    argument.setRewritedId((mooneyEventRepre.getAllArguments()[slotId]));
                }
            }
        }

        MooneyEventRepre mooneyStyleAnswer = mooneyChain.get(clozeIndex);
        PriorityQueue<Pair<MooneyEventRepre, Double>> results = predictMooneyStyle(regularChain, clozeIndex, numArguments, DataPool.headWords);

        int rank = 0;
        boolean oov = true;
        List<String> lines = new ArrayList<>();
        while (!results.isEmpty()) {
            rank++;
            Pair<MooneyEventRepre, Double> resulti = results.poll();
            lines.add(resulti.getLeft() + "\t" + resulti.getRight());
            if (resulti.getKey().equals(mooneyStyleAnswer)) {
                logger.info(String.format("For cloze task : %s, correct answer found at %d", clozeFileName, rank));
                for (int kPos = 0; kPos < CompactLogLinearTestRunner.allK.length; kPos++) {
                    if (CompactLogLinearTestRunner.allK[kPos] >= rank) {
                        CompactLogLinearTestRunner.recallCounts[kPos]++;
                    }
                }
                oov = false;
                break;
            }
        }

        if (!oov) {
            CompactLogLinearTestRunner.mrr += 1.0 / rank;
        } else {
            logger.info("Answer Predicate is OOV, contributing 0 MRRs");
        }
        CompactLogLinearTestRunner.totalCount++;
        File outputFile = new File(CompactLogLinearTestRunner.outputPath, outputBase);
        try {
            FileUtils.writeLines(outputFile, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private PriorityQueue<Pair<MooneyEventRepre, Double>> predictMooneyStyle(List<ContextElement> chain, int testIndex, int numArguments, String[] allPredicates) {
        ContextElement answer = chain.get(testIndex);
        logger.info("Answer is " + answer.getMention());

        PriorityQueue<Pair<MooneyEventRepre, Double>> rankedEvents = new PriorityQueue<>(allPredicates.length,
                new Comparators.DescendingScoredPairComparator<MooneyEventRepre, Double>());

        Set<Integer> mooneyEntities = getRewritedEntitiesFromChain(chain);

        ContextElement realElement = chain.get(testIndex);

        for (String head : allPredicates) {
            List<MooneyEventRepre> candidateMooeyEvms = MooneyEventRepre.generateTuples(head, mooneyEntities);
            for (MooneyEventRepre candidateEvm : candidateMooeyEvms) {
                ContextElement candidate = ContextElement.fromMooney(realElement.getJcas(), realElement.getSent(), realElement.getHead(), candidateEvm);
                TLongShortDoubleHashTable features = extractor.getFeatures(chain, candidate, testIndex, skipGramN, false);

                double score = compactWeights.dotProd(features);
                if (score > 0) {
                    logger.info("Candidate is " + candidate.getMention());
                    logger.info("Feature score " + score);
                    compactWeights.dotProd(features, extractor.getFeatureNamesByIndex(), DataPool.headWords);
                }
//                if (score > 0) {
//                    System.out.println("candidate is " + candidate.getMention());
//                    System.out.println("Score is " + score);
//                }

                if (candidate.getMention().mooneyMatch(answer.getMention())) {
                    logger.info("Answer candidate appears: " + candidate.getMention());
                    logger.info("Feature score " + score);
                    logger.info("Answer features : ");
                    logger.info(features.dump(DataPool.headWords, extractor.getFeatureNamesByIndex()));
                }
                rankedEvents.add(Pair.of(candidateEvm, score));
            }
        }
        return rankedEvents;
    }

    private Triple<List<MooneyEventRepre>, Integer, String> getMooneyStyleCloze(String fileName) {
        File clozeFile = new File(clozeDir, fileName);

        if (!clozeFile.exists()) {
            logger.info("Cloze file does not exist: " + clozeFile.getPath());
            return null;
        }

        List<String> lines = null;

        try {
            lines = FileUtils.readLines(clozeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<MooneyEventRepre> repres = new ArrayList<>();

        int blankIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(KmTargetConstants.clozeBlankIndicator)) {
                blankIndex = i;
                repres.add(MooneyEventRepre.fromString(line.substring(KmTargetConstants.clozeBlankIndicator.length())));
            } else {
                repres.add(MooneyEventRepre.fromString(line));
            }
        }
        return Triple.of(repres, blankIndex, fileName);
    }


    private List<ContextElement> getHighFreqEventMentions(JCas aJCas) {
        List<ContextElement> chain = new ArrayList<>();
        //in principle, this iteration will get the same ordering as iterating event mention
        //hopefully this is valid
        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                if (ignoreLowFreq) {
//                    int evmTf = MultiMapUtils.getTf(headTfDfMaps, align.getLowercaseWordLemma(mention.getHeadWord()));
                    long evmTf = DataPool.getPredicateFreq(align.getLowercaseWordLemma(mention.getHeadWord()));
                    //filter by low tf df counts
                    if (Utils.termFrequencyFilter(evmTf)) {
                        logger.info("Mention filtered because of low frequency: " + mention.getCoveredText() + " " + evmTf);
                        continue;
                    }
                }
                LocalEventMentionRepre eventRep = LocalEventMentionRepre.fromEventMention(mention, align);
                chain.add(new ContextElement(aJCas, sent, mention.getHeadWord(), eventRep));
            }
        }
        return chain;
    }

    public static Set<Integer> getRewritedEntitiesFromChain(List<ContextElement> chain) {
        Set<Integer> entities = new HashSet<>();
        for (ContextElement rep : chain) {
            for (LocalArgumentRepre arg : rep.getMention().getArgs()) {
                if (arg != null && !arg.isOther()) {
                    entities.add(arg.getRewritedId());
                }
            }
        }
        return entities;
    }
}