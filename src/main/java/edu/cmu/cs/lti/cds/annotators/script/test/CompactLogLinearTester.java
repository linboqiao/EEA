package edu.cmu.cs.lti.cds.annotators.script.test;

import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.ml.features.CompactFeatureExtractor;
import edu.cmu.cs.lti.cds.model.ChainElement;
import edu.cmu.cs.lti.cds.model.KmTargetConstants;
import edu.cmu.cs.lti.cds.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.cds.runners.script.cds.test.CompactLogLinearTestRunner;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.cds.utils.DbManager;
import edu.cmu.cs.lti.cds.utils.MultiMapUtils;
import edu.cmu.cs.lti.collections.TLongShortDoubleHashTable;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
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
import org.mapdb.Fun;
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
public class CompactLogLinearTester extends AbstractLoggingAnnotator {
    public static final String PARAM_HEAD_COUNT_DB_NAMES = "headCountDbFileNames";

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String PARAM_IGNORE_LOW_FREQ = "ignoreLowFreq";

    public static final String PARAM_CLOZE_DIR_PATH = "clozePath";

    public static final String PARAM_MODEL_PATH = "modelPath";

    private boolean ignoreLowFreq;

    private Map<String, Fun.Tuple2<Integer, Integer>>[] headTfDfMaps;

    private String clozeExt = ".txt";

    private File clozeDir;

    //TODO make it parameter
    private int skipGramN = 2;

    //make it parameter
    int numArguments = 3;

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private CompactFeatureExtractor extractor;

    private String modelPath;
    private TLongBasedFeatureTable compactWeights;


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);

        if (aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ) != null) {
            ignoreLowFreq = (Boolean) aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ);
        } else {
            ignoreLowFreq = true;
        }

        if (ignoreLowFreq) {
            String[] countingDbFileNames = (String[]) aContext.getConfigParameterValue(PARAM_HEAD_COUNT_DB_NAMES);
            headTfDfMaps = DbManager.getMaps(dbPath, countingDbFileNames, EventMentionHeadCounter.defaultMentionHeadMapName);
        }

        modelPath = (String) aContext.getConfigParameterValue(PARAM_MODEL_PATH);

        logger.info("Loading from " + modelPath);

        try {
            compactWeights = (TLongBasedFeatureTable) SerializationHelper.read(modelPath);
            extractor = new CompactFeatureExtractor(compactWeights);
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

        Triple<List<MooneyEventRepre>, Integer, String> mooneyClozeTask = getMooneyStyleCloze(aJCas);
        if (mooneyClozeTask == null) {
            logger.info("Cloze file removed due to duplication or empty");
            return;
        }

        align.loadWord2Stanford(aJCas);

        List<ChainElement> regularChain = getHighFreqEventMentions(aJCas);

        if (regularChain.size() == 0) {
            return;
        }

        String outputBase = mooneyClozeTask.getRight() + "_res";

        int clozeIndex = mooneyClozeTask.getMiddle();
        List<MooneyEventRepre> mooneyChain = mooneyClozeTask.getLeft();

        if (mooneyChain.size() != regularChain.size()) {
//            System.out.println(regularChain);
            throw new IllegalArgumentException("Test data and document have different size! " + mooneyChain.size() + " " + regularChain.size());
        }

        MooneyEventRepre mooneyStyleAnswer = mooneyChain.get(clozeIndex);

        PriorityQueue<Pair<MooneyEventRepre, Double>> results = predictMooneyStyle(regularChain, mooneyClozeTask.getLeft(), clozeIndex, numArguments, DataPool.headWords);

        int rank;
        boolean oov = true;
        List<String> lines = new ArrayList<>();
        for (rank = 1; rank <= results.size(); rank++) {
            Pair<MooneyEventRepre, Double> resulti = results.poll();
            lines.add(resulti.getLeft() + "\t" + resulti.getRight());
            if (resulti.getKey().equals(mooneyStyleAnswer)) {
                logger.info("Correct answer found at " + rank);
                for (int kPos = 0; kPos < CompactLogLinearTestRunner.allK.length; kPos++) {
                    if (CompactLogLinearTestRunner.allK[kPos] >= rank) {
                        CompactLogLinearTestRunner.recallCounts[kPos]++;
                    }
                }
                oov = false;
                break;
            }
//            System.out.println(resulti);
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

    private PriorityQueue<Pair<MooneyEventRepre, Double>> predictMooneyStyle(
            List<ChainElement> regularChain, List<MooneyEventRepre> mooneyChain, int testIndex, int numArguments, String[] allPredicates) {
        MooneyEventRepre answer = mooneyChain.get(testIndex);
        logger.info("Answer is " + answer);

        PriorityQueue<Pair<MooneyEventRepre, Double>> rankedEvents = new PriorityQueue<>(allPredicates.length,
                new Comparators.DescendingScoredPairComparator<MooneyEventRepre, Double>());

        //extract full entities
        List<Pair<Integer, String>> entities = new ArrayList<>();
        for (ChainElement element : regularChain) {
            Collections.addAll(entities, element.getMention().getArgs());
        }
        //represent 'other' unseen entity
        entities.add(Pair.of(-1, ""));
        Set<Integer> mooneyEntities = getRewritedEntitiesFromChain(mooneyChain);

        Sentence sent = regularChain.get(testIndex).getSent();
        boolean debug = true;
        for (String head : allPredicates) {
            List<MooneyEventRepre> candidateMooeyEvms = MooneyEventRepre.generateTuples(head, mooneyEntities);
            for (MooneyEventRepre candidateEvm : candidateMooeyEvms) {
//                skipGramN = 50;
                TLongShortDoubleHashTable features = extractor.getFeatures(mooneyChain, candidateEvm, testIndex, skipGramN);
                double score = compactWeights.dotProd(features);

                if (candidateEvm.equals(answer)) {
//                    logger.info(features.toString());
//                    for (TLongObjectIterator<TShortDoubleMap> firstLevelIter = features.iterator(); firstLevelIter.hasNext(); ) {
//                        firstLevelIter.advance();
//                        long featureRowKey = firstLevelIter.key();
//                        if (compactWeights.containsRow(featureRowKey)) {
//                            System.out.println("Finding feature match under " + featureRowKey);
//                            TShortDoubleMap weightsRow = compactWeights.getRow(featureRowKey);
//                            TShortDoubleMap secondLevelFeatures = firstLevelIter.value();
//                            for (TShortDoubleIterator secondLevelIter = secondLevelFeatures.iterator(); secondLevelIter.hasNext(); ) {
//                                secondLevelIter.advance();
//                                System.out.println("Answer feature " + secondLevelIter.key());
//                                if (weightsRow.containsKey(secondLevelIter.key())) {
//                                    System.out.println("Feature matched " + weightsRow.get(secondLevelIter.key()));
//                                }
//                            }
//                        }
//                    }

                    logger.info("Answer candidate appears: " + candidateEvm);
                    logger.info("Feature score " + score);
                }

                rankedEvents.add(Pair.of(candidateEvm, score));
            }
        }
        return rankedEvents;
    }

    private Pair<Integer, String>[] getEntitiesFromMask(List<Pair<Integer, String>> entities, int[] mask) {
        Pair<Integer, String>[] chosen = new Pair[mask.length];
        for (int i = 0; i < mask.length; i++) {
            chosen[i] = entities.get(mask[i]);
        }
        return chosen;
    }

    private Triple<List<MooneyEventRepre>, Integer, String> getMooneyStyleCloze(JCas aJCas) {
        String fileName = UimaConvenience.getShortDocumentName(aJCas) + ".gz_" + UimaConvenience.getOffsetInSource(aJCas) + clozeExt;
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

    private Set<Integer> getRewritedEntitiesFromChain(List<MooneyEventRepre> chain) {
        Set<Integer> entities = new HashSet<>();
        for (MooneyEventRepre rep : chain) {
            for (int arg : rep.getAllArguments()) {
                if (arg != KmTargetConstants.nullArgMarker && arg != KmTargetConstants.otherMarker) {
                    entities.add(arg);
                }
            }
        }
        return entities;
    }

    private List<ChainElement> getHighFreqEventMentions(JCas aJCas) {
        List<ChainElement> chain = new ArrayList<>();

        //in principle, this iteration will get the same ordering as iterating event mention
        //hopefully this is valid
        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                if (ignoreLowFreq) {
                    int evmTf = MultiMapUtils.getTf(headTfDfMaps, align.getLowercaseWordLemma(mention.getHeadWord()));
                    //filter by low tf df counts
                    if (Utils.termFrequencyFilter(evmTf)) {
                        logger.info("Mention filtered because of low frequency: " + mention.getCoveredText() + " " + evmTf);
                        continue;
                    }
                }
                LocalEventMentionRepre eventRep = LocalEventMentionRepre.fromEventMention(mention, align);
                chain.add(new ChainElement(sent, eventRep));
            }
        }
        return chain;
    }
}