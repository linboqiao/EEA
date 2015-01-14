package edu.cmu.cs.lti.script.annotators.learn.test;

import edu.cmu.cs.lti.script.model.*;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
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

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/4/14
 * Time: 3:03 PM
 */
public abstract class MultiArgumentClozeTest extends AbstractLoggingAnnotator {
    public static final String PARAM_IGNORE_LOW_FREQ = "ignoreLowFreq";

    public static final String PARAM_CLOZE_DIR_PATH = "clozePath";

    public static final String PARAM_EVAL_RESULT_PATH = "evalPath";

    public static final String PARAM_EVAL_RANKS = "evalRanks";

    public static final String PARAM_EVAL_LOG_DIR = "evalLogDir";

    private boolean ignoreLowFreq;

    private String clozeExt = ".txt";

    private File clozeDir;

    private List<Integer> allK;

    private int[] recallCounts;

    private double mrr = 0;

    public double totalCount = 0;

    private File rankListOutputDir;

    private File evalLogFile;

    int numArguments = 3;

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private List<String> evalResults;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        if (aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ) != null) {
            ignoreLowFreq = (Boolean) aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ);
        } else {
            ignoreLowFreq = true;
        }
        clozeDir = new File((String) aContext.getConfigParameterValue(PARAM_CLOZE_DIR_PATH));
        allK = (List<Integer>) aContext.getConfigParameterValue(PARAM_EVAL_RANKS);

        //prepare paths for output
        String predictorName = initializePredictor(aContext);

        rankListOutputDir = new File((String) aContext.getConfigParameterValue(PARAM_EVAL_RESULT_PATH), predictorName);
        evalLogFile = new File((String) aContext.getConfigParameterValue(PARAM_EVAL_LOG_DIR), predictorName);

        try {
            //ensure file is empty
            FileUtils.write(evalLogFile, "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initailize the predictor, get the predictor name
     *
     * @param aContext Uima Context
     * @return Name of the predictor, use to distinguish different evaluation output
     */
    protected abstract String initializePredictor(UimaContext aContext);

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        if (DataPool.blackListedArticleId.contains(article.getArticleName())) {
            //ignore this blacklisted file;
            logger.info("Ignored black listed file");
            return;
        }

        evalResults = new ArrayList<>();

        String clozeFileName = UimaConvenience.getShortDocumentName(aJCas) + ".gz_" + UimaConvenience.getOffsetInSource(aJCas) + clozeExt;
        Triple<List<MooneyEventRepre>, Integer, String> mooneyClozeTask = getMooneyStyleCloze(clozeFileName);
        if (mooneyClozeTask == null) {
            logger.info("Cloze file removed due to duplication or empty");
            return;
        }

        align.loadWord2Stanford(aJCas);

        //the actual chain is got here
        List<ContextElement> regularChain = getTestingEventMentions(aJCas);
        if (regularChain.size() == 0) {
            return;
        }

        String rankListOutputName = mooneyClozeTask.getRight() + "_ranklist";

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

        List<ContextElement> entities = getTestingEventMentions(aJCas);

        PriorityQueue<Pair<MooneyEventRepre, Double>> results = predict(regularChain, entities, clozeIndex, numArguments);

        int rank = 0;
        boolean oov = true;
        List<String> rankResults = new ArrayList<>();

        while (!results.isEmpty()) {
            rank++;
            Pair<MooneyEventRepre, Double> resulti = results.poll();
            rankResults.add(resulti.getLeft() + "\t" + resulti.getRight());
            if (resulti.getKey().equals(mooneyStyleAnswer)) {
                String evalRecord = String.format("For cloze task : %s, correct answer found at %d", clozeFileName, rank);
                logger.info(evalRecord);
                evalResults.add(evalRecord);
                for (int kPos = 0; kPos < allK.size(); kPos++) {
                    if (allK.get(kPos) >= rank) {
                        recallCounts[kPos]++;
                    }
                }
                oov = false;
                break;
            }
        }

        if (!oov) {
            mrr += 1.0 / rank;
        } else {
            String evalRecord = String.format("For cloze task : %s, correct answer is not found", clozeFileName);
            evalResults.add(evalRecord);
            logger.info(evalRecord);
        }
        totalCount++;

        File rankListOutputFile = new File(rankListOutputDir, rankListOutputName);
        try {
            FileUtils.writeLines(rankListOutputFile, rankResults);
            FileUtils.writeLines(evalLogFile, evalResults, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void evalLog(String record) {
        evalResults.add(record);
    }

    /**
     * Predict the event at the given index of the chain
     *
     * @param chain        The cloze task chain
     * @param testIndex    The position to be test
     * @param numArguments Number of arguments for each event
     * @return
     */
    protected abstract PriorityQueue<Pair<MooneyEventRepre, Double>> predict(List<ContextElement> chain, List entities, int testIndex, int numArguments);

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

    private List<ContextElement> getTestingEventMentions(JCas aJCas) {
        List<ContextElement> chain = new ArrayList<>();

        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                if (ignoreLowFreq) {
                    long evmTf = DataPool.getPredicateFreq(align.getLowercaseWordLemma(mention.getHeadWord()));
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

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        recallCounts = new int[allK.size()];
        for (int kPos = 0; kPos < allK.size(); kPos++) {
            logger.info(String.format("Recall at %d : %.4f", allK.get(kPos), recallCounts[kPos] * 1.0 / totalCount));
        }
        logger.info(String.format("MRR is : %.4f", mrr / totalCount));
        logger.info("Completed.");
    }


}