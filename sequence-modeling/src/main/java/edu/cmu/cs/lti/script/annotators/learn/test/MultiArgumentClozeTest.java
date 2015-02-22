package edu.cmu.cs.lti.script.annotators.learn.test;

import edu.cmu.cs.lti.script.model.*;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
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

    private Integer[] allK;

    private int[] recallCounts;

    private double mrr = 0;

    public double totalCount = 0;

    private File rankListOutputDir;

    private File evalResultFile;

    int numArguments = 3;

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private List<String> evalResults = new ArrayList<>();

    private List<String> evalInfos = new ArrayList<>();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        if (aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ) != null) {
            ignoreLowFreq = (Boolean) aContext.getConfigParameterValue(PARAM_IGNORE_LOW_FREQ);
        } else {
            ignoreLowFreq = true;
        }
        clozeDir = new File((String) aContext.getConfigParameterValue(PARAM_CLOZE_DIR_PATH));

        //prepare evaluation statistics holder
        allK = (Integer[]) aContext.getConfigParameterValue(PARAM_EVAL_RANKS);
        recallCounts = new int[allK.length];

        //prepare paths for output
        String predictorName = initializePredictor(aContext);

        rankListOutputDir = new File((String) aContext.getConfigParameterValue(PARAM_EVAL_RESULT_PATH), predictorName);
        String evalDirPath = (String) aContext.getConfigParameterValue(PARAM_EVAL_LOG_DIR);
        evalResultFile = new File(evalDirPath, "eval_results_" + predictorName);

        try {
            FileUtils.write(evalResultFile, "");
            logEvalInfo(String.format("Rank list output directory : [%s] , eval result file : [%s]",
                    rankListOutputDir.getCanonicalPath(), evalResultFile.getCanonicalPath()));
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
            logger.info("Remove blacklisted file");
            return;
        }

        String clozeFileName = UimaConvenience.getShortDocumentName(aJCas) + ".gz_" + UimaConvenience.getOffsetInSource(aJCas) + clozeExt;

//        List<Triple<List<MooneyEventRepre>, Integer, String>> clozeTasks = getAllPoissibleMooneyStyleClozes(clozeFileName);
        List<Triple<List<MooneyEventRepre>, Integer, String>> clozeTasks = getPreselectedClozeTask(clozeFileName);

        align.loadWord2Stanford(aJCas);
        align.loadFanse2Stanford(aJCas);

        for (Triple<List<MooneyEventRepre>, Integer, String> mooneyClozeTask : clozeTasks) {
            runClozeTask(aJCas, mooneyClozeTask);
        }
    }

    private void runClozeTask(JCas aJCas, Triple<List<MooneyEventRepre>, Integer, String> mooneyClozeTask) {

        //the actual chain is got here
        List<ContextElement> regularChain = getTestingEventMentions(aJCas);
        if (regularChain.size() == 0) {
            return;
        }
        String taskId = mooneyClozeTask.getRight();

        logger.info("Running cloze task : " + taskId);

        int clozeIndex = mooneyClozeTask.getMiddle();
        List<MooneyEventRepre> mooneyChain = mooneyClozeTask.getLeft();

        if (mooneyChain.size() != regularChain.size()) {
            throw new IllegalArgumentException("Test data and document have different size! " + mooneyChain.size() + " " + regularChain.size());
        }

        //use the cloze evaluation file to give the rewritten information to the chain
        for (int i = 0; i < mooneyChain.size(); i++) {
            MooneyEventRepre mooneyEventRepre = mooneyChain.get(i);
            for (int slotId = 0; slotId < mooneyEventRepre.getAllArguments().length; slotId++) {
                LocalArgumentRepre argument = regularChain.get(i).getMention().getArg(slotId);
                if (argument != null) {
                    argument.setRewrittenId((mooneyEventRepre.getAllArguments()[slotId]));
                }
            }
        }

        MooneyEventRepre mooneyStyleAnswer = mooneyChain.get(clozeIndex);

        Set<Integer> entities = getRewritedEntitiesFromChain(regularChain);

        PriorityQueue<Pair<MooneyEventRepre, Double>> results = predict(regularChain, entities, clozeIndex, numArguments);

        int rank = 0;
        boolean oov = true;
        List<String> rankResults = new ArrayList<>();

        while (!results.isEmpty()) {
            rank++;
            Pair<MooneyEventRepre, Double> resulti = results.poll();
            rankResults.add(resulti.getLeft() + "\t" + resulti.getRight());
            if (resulti.getKey().equals(mooneyStyleAnswer)) {
                String evalRecord = String.format("For cloze task : %s, correct answer found at %d", taskId, rank);
                logEvalInfo(evalRecord);
                logEvalResult(evalRecord);
                for (int kPos = 0; kPos < allK.length; kPos++) {
                    if (allK[kPos] >= rank) {
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
            String evalRecord = String.format("For cloze task : %s, correct answer is not found", taskId);
            logEvalResult(evalRecord);
            logEvalInfo(evalRecord);
        }
        totalCount++;

        String rankListOutputName = taskId + "_ranklist";
        String evalInfoOutputName = taskId + "_evalinfo";

        File rankListOutputFile = new File(rankListOutputDir, rankListOutputName);
        File evalInfoFile = new File(rankListOutputDir, evalInfoOutputName);
        try {
            FileUtils.writeLines(rankListOutputFile, rankResults);
            FileUtils.writeLines(evalInfoFile, evalInfos);
            FileUtils.writeLines(evalResultFile, evalResults, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        evalResults = new ArrayList<>();
        evalInfos = new ArrayList<>();
    }


    protected void logEvalResult(String record) {
        evalResults.add(record);
    }

    protected void logEvalInfo(String record) {
        evalInfos.add(record);
    }

    /**
     * Predict the event at the given index of the chain
     *
     * @param chain        The cloze task chain
     * @param testIndex    The position to be test
     * @param numArguments Number of arguments for each event
     * @return A triple indicating the tasks : list of mentions, a target index to guess and the task Id
     */
    protected abstract PriorityQueue<Pair<MooneyEventRepre, Double>> predict(List<ContextElement> chain, Set<Integer> entities, int testIndex, int numArguments);

    private List<Triple<List<MooneyEventRepre>, Integer, String>> getPreselectedClozeTask(String fileName) {
        List<Triple<List<MooneyEventRepre>, Integer, String>> clozeTasks = new ArrayList<>();

        File clozeFile = new File(clozeDir, fileName);
        if (!clozeFile.exists()) {
            logger.info("Cloze file does not exist: " + clozeFile.getPath());
            return clozeTasks;
        }

        List<String> lines = null;

        try {
            lines = FileUtils.readLines(clozeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<MooneyEventRepre> repres = new ArrayList<>();

        List<Integer> blankIndices = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(KmTargetConstants.clozeBlankIndicator)) {
                blankIndices.add(i);
                repres.add(MooneyEventRepre.fromString(line.substring(KmTargetConstants.clozeBlankIndicator.length())));
            } else {
                repres.add(MooneyEventRepre.fromString(line));
            }
        }

        for (int blankIndex : blankIndices) {
            clozeTasks.add(Triple.of(repres, blankIndex, fileName + "_" + blankIndex));
        }

        return clozeTasks;
    }

    /**
     * Ignoring the random selected cloze, use all possible slots for cloze evaluation
     *
     * @param fileName
     * @return
     */
    private List<Triple<List<MooneyEventRepre>, Integer, String>> getAllPoissibleMooneyStyleClozes(String fileName) {
        List<Triple<List<MooneyEventRepre>, Integer, String>> clozeTasks = new ArrayList<>();

        File clozeFile = new File(clozeDir, fileName);
        if (!clozeFile.exists()) {
            logEvalInfo("Cloze file does not exist: " + clozeFile.getPath());
            return clozeTasks;
        }

        List<String> lines = null;

        try {
            lines = FileUtils.readLines(clozeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<MooneyEventRepre> repres = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(KmTargetConstants.clozeBlankIndicator)) {
                repres.add(MooneyEventRepre.fromString(line.substring(KmTargetConstants.clozeBlankIndicator.length())));
            } else {
                repres.add(MooneyEventRepre.fromString(line));
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            clozeTasks.add(Triple.of(repres, i, fileName + "_" + i));
        }

        return clozeTasks;
    }

    private List<ContextElement> getTestingEventMentions(JCas aJCas) {
        List<ContextElement> chain = new ArrayList<>();

        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sent)) {
                if (ignoreLowFreq) {
                    long evmTf = DataPool.getPredicateFreq(align.getLowercaseWordLemma(mention.getHeadWord()));
                    if (Utils.termFrequencyFilter(evmTf)) {
                        logEvalInfo("Mention filtered because of low frequency: " + mention.getCoveredText() + " " + evmTf);
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
                    entities.add(arg.getRewrittenId());
                }
            }
        }
        return entities;
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        for (int kPos = 0; kPos < allK.length; kPos++) {
            logEvalResult(String.format("Recall at %d : %.4f", allK[kPos], recallCounts[kPos] * 1.0 / totalCount));
        }
        logEvalResult(String.format("MRR is : %.4f", mrr / totalCount));

        try {
            FileUtils.writeLines(evalResultFile, evalResults, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}