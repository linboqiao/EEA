package edu.cmu.cs.lti.cds.annotators.script;

import edu.cmu.cs.lti.cds.ml.features.FeatureExtractor;
import edu.cmu.cs.lti.cds.model.ChainElement;
import edu.cmu.cs.lti.cds.model.KmTargetConstants;
import edu.cmu.cs.lti.cds.model.LocalEventMentionRepre;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.cds.utils.DbManager;
import edu.cmu.cs.lti.cds.utils.MultiMapUtils;
import edu.cmu.cs.lti.cds.utils.VectorUtils;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
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
public class CeTester extends AbstractLoggingAnnotator {
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

    private TokenAlignmentHelper align = new TokenAlignmentHelper();

    private FeatureExtractor extractor = new FeatureExtractor();

    private String modelPath;

    private TObjectDoubleMap<String> weights;


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

        try {
            weights = (TObjectDoubleMap<String>) SerializationHelper.read(modelPath);
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
        align.loadWord2Stanford(aJCas);

        List<ChainElement> chain = getHighFreqEventMentions(aJCas);

        if (chain.size() == 0) {
            //empty file
            return;
        }

        int clozeIndex = getClozeIndex(aJCas);

        List<Pair<Integer, String>> arguments = new ArrayList<>();
        for (ChainElement element : chain) {
            for (Pair<Integer, String> arg : element.getMention().getArgs()) {
                arguments.add(arg);
            }
        }

        int numArguments = 3;
        PriorityQueue<Pair<LocalEventMentionRepre, Double>> result = predict(chain, DataPool.headWords, clozeIndex, arguments, numArguments);

        for (int i = 0; i < 10; i++) {
            System.out.println(result.poll());
        }

    }

    private PriorityQueue<Pair<LocalEventMentionRepre, Double>> predict(List<ChainElement> chain, String[] allPredicates, int testIndex, List<Pair<Integer, String>> entities, int numArguments) {
        PriorityQueue<Pair<LocalEventMentionRepre, Double>> rankedEvents = new PriorityQueue<>(allPredicates.length, new DescendingScoredPairComparator());

        //represent some other entity
        entities.add(Pair.of(-1, ""));

        Sentence sent = chain.get(testIndex).getSent();
        for (String head : allPredicates) {
            for (ChainElement candidate : generateCandidateChainElements(sent, head, entities, numArguments)) {
                System.out.println(candidate);
                double score = score(chain, candidate, testIndex);
                rankedEvents.add(Pair.of(candidate.getMention(), score));
            }
        }
        return rankedEvents;
    }

    private List<ChainElement> generateCandidateChainElements(Sentence sent, String head, List<Pair<Integer, String>> entities, int numArguments) {
        List<ChainElement> candidates = new ArrayList<>();

        List<int[]> combs = getCombination(numArguments, entities.size());
        //this is too much
        System.out.println(combs.size());

        for (int[] comb : combs) {
            LocalEventMentionRepre rep = new LocalEventMentionRepre(head, getEntitiesFromMask(entities, comb));
            ChainElement ce = new ChainElement(sent, rep);
            candidates.add(ce);
        }

        return candidates;
    }

    private Pair<Integer, String>[] getEntitiesFromMask(List<Pair<Integer, String>> entities, int[] mask) {
        Pair<Integer, String>[] chosen = new Pair[mask.length];
        for (int i = 0; i < mask.length; i++) {
            chosen[i] = entities.get(mask[i]);
        }
        return chosen;
    }

    private List<int[]> getCombination(int chooseN, int fromM) {
        System.out.println("choose " + chooseN + " from " + fromM);
        byte[] bits = new byte[fromM];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = i < chooseN ? (byte) 1 : (byte) 0;
        }


        List<int[]> combs = new ArrayList<>();

        boolean found = true;
        while (found) {
            int index = 0;
            int[] comb = new int[chooseN];
            for (int j = 0; j < bits.length; j++) {
                if (bits[j] == (byte) 1) {
                    comb[index] = j;
                    index++;
                }
            }
            combs.add(comb);

            found = false;
            for (int i = 0; i < fromM - 1; i++) {
                if (bits[i] == 1 && bits[i + 1] == 0) {
                    found = true;
                    bits[i] = 0;
                    bits[i + 1] = 1;

                    if (bits[0] == 0) {
                        for (int k = 0, j = 0; k < i; k++) {
                            if (bits[k] == 1) {
                                byte temp = bits[k];
                                bits[k] = bits[j];
                                bits[j] = temp;
                                j++;
                            }
                        }
                    }
                    break;
                }
            }
        }


        return combs;
    }

    public class DescendingScoredPairComparator implements Comparator<Pair<LocalEventMentionRepre, Double>> {
        @Override
        public int compare(Pair<LocalEventMentionRepre, Double> o1, Pair<LocalEventMentionRepre, Double> o2) {
            return -o1.getValue().compareTo(o2.getValue());
        }

    }

    private double score(List<ChainElement> chain, ChainElement candidate, int testIndex) {
        TObjectDoubleMap<String> features = extractor.getFeatures(chain, align, candidate, testIndex, skipGramN);
        return VectorUtils.dotProd(features, weights);
    }

    /**
     * Take existing cloze task so we can compare
     */
    private int getClozeIndex(JCas aJCas) {
        String fileName = UimaConvenience.getShortDocumentName(aJCas) + ".gz_" + UimaConvenience.getOffsetInSource(aJCas) + clozeExt;
        List<String> lines = null;
        try {
            lines = FileUtils.readLines(new File(clozeDir, fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int blankIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(KmTargetConstants.clozeBlankIndicator)) {
                blankIndex = i;
            }
        }

        return blankIndex;
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

    public static void main(String[] args) {
        CeTester t = new CeTester();
        int[] array = new int[]{1, 2, 3, 4, 5};
        List<int[]> res = t.getCombination(3, 5);

        for (int i = 0; i < res.size(); i++) {
            for (int j = 0; j < res.get(i).length; j++) {
                System.out.print(" " + array[res.get(i)[j]] + " ");
            }
            System.out.println();
        }
    }

}