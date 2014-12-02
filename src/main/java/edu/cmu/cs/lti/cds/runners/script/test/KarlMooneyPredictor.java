package edu.cmu.cs.lti.cds.runners.script.test;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.model.KmTargetConstants;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.cds.utils.DbManager;
import edu.cmu.cs.lti.cds.utils.MultiMapUtils;
import edu.cmu.cs.lti.utils.Comparators;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/21/14
 * Time: 1:31 PM
 */
public class KarlMooneyPredictor {
    private List<File> allClozeFiles;

    private int evalPointer = 0;

    public static final String clozeFileSuffix = ".txt";

    private TObjectIntMap<TIntList>[] cooccCountMaps;

    private TObjectIntMap<TIntList>[] occCountMaps;

    private Map[] headTfDfMaps;

    private TObjectIntMap<String>[] headIdMaps;

    private String className = this.getClass().getName();

    private Logger logger = Logger.getLogger(className);

    public KarlMooneyPredictor(String dbPath, String[] dbNames, String occName, String cooccName, String[] countingDbFileNames, String headIdMapName) throws Exception {
        logger.setLevel(Level.INFO);
        Utils.printMemInfo(logger, "Initial memory information ");

        cooccCountMaps = MultiMapUtils.loadMaps(dbPath, dbNames, cooccName, logger, "Loading coocc");

        occCountMaps = MultiMapUtils.loadMaps(dbPath, dbNames, occName, logger, "Loading occ");

        headIdMaps = MultiMapUtils.loadMaps(dbPath, dbNames, headIdMapName, logger, "Loading head ids");

        logger.info("Loading event head counts : " + dbPath + "/" + Joiner.on(",").join(countingDbFileNames));

        headTfDfMaps = DbManager.getMaps(dbPath, countingDbFileNames, EventMentionHeadCounter.defaultMentionHeadMapName);

        Utils.printMemInfo(logger, "Memory info after all loading");
    }

    private void loadEvalDir(String clozeDataDirPath) throws IOException {
        File clozeDataDir = new File(clozeDataDirPath);

        allClozeFiles = new ArrayList<>();
        if (clozeDataDir.isDirectory()) {
            for (File clozeFile : clozeDataDir.listFiles()) {
                if (!clozeFile.isDirectory() && clozeFile.getName().endsWith(clozeFileSuffix)) {
                    allClozeFiles.add(clozeFile);
                }
            }
        } else {
            throw new IOException("Cannot find given directory " + clozeDataDirPath);
        }
    }

    private int clozeTaskSize() {
        return allClozeFiles.size();
    }

    private boolean hasNext() {
        return evalPointer < allClozeFiles.size();
    }

    private File getNextCloze() {
        return allClozeFiles.get(evalPointer++);
    }

    private Triple<List<MooneyEventRepre>, Integer, String> parseCloze(File clozeFile) throws IOException {
        logger.info("Predicting cloze task : " + clozeFile.getName());

        List<String> lines = FileUtils.readLines(clozeFile);

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

        return Triple.of(repres, blankIndex, clozeFile.getName());
    }

    public static Pair<MooneyEventRepre, MooneyEventRepre> formerBasedTransform(MooneyEventRepre former, MooneyEventRepre latter) {
        TIntIntMap transformMap = new TIntIntHashMap();

        //TODO change representation to general array
        int[] formerArguments = former.getAllArguments();
        int[] latterArguments = latter.getAllArguments();

        MooneyEventRepre transformedFormer = new MooneyEventRepre();
        MooneyEventRepre transformedLatter = new MooneyEventRepre();

        transformedFormer.setPredicate(former.getPredicate());
        transformedLatter.setPredicate(latter.getPredicate());

        for (int formerSlotId = 0; formerSlotId < formerArguments.length; formerSlotId++) {
            int originFormerId = formerArguments[formerSlotId];
            if (originFormerId == 0 || originFormerId == -1) {
                transformedFormer.setArgument(formerSlotId, originFormerId);
            } else {
                int formerBasedArgumentId = KmTargetConstants.slotIndexToArgMarker(formerSlotId);
                transformMap.put(originFormerId, formerBasedArgumentId);
                transformedFormer.setArgument(formerSlotId, formerBasedArgumentId);
            }
        }

        TIntSet mask = new TIntHashSet();
        for (int latterSlotId = 0; latterSlotId < latterArguments.length; latterSlotId++) {
            int latterArgumentId = latterArguments[latterSlotId];
            if (latterArgumentId == 0 || latterArgumentId == -1) {
                transformedLatter.setArgument(latterSlotId, latterArgumentId);
            } else {
                int transformedId = transformMap.get(latterArgumentId);
                transformedLatter.setArgument(latterSlotId, transformedId);
                mask.add(transformedId);
            }
        }

        int[] transformedFormerArgument = transformedFormer.getAllArguments();
        for (int formerSlotId = 0; formerSlotId < transformedFormerArgument.length; formerSlotId++) {
            int formerArgumentId = transformedFormerArgument[formerSlotId];
            if (formerArgumentId != 0 && formerArgumentId != -1) {
                if (!mask.contains(formerArgumentId)) {
                    transformedFormer.setArgument(formerSlotId, 0);
                }
            }
        }
        return Pair.of(transformedFormer, transformedLatter);
    }

    //this is correct but slow.
    private double conditionalFollowing(MooneyEventRepre former, MooneyEventRepre latter, double laplacianSmoothingParameter, int numTotalEvents) {
        Pair<Integer, Integer> counts = MultiMapUtils.getCounts(former, latter, cooccCountMaps, occCountMaps, headIdMaps);

        double cooccCountSmoothed = counts.getRight() + laplacianSmoothingParameter;
        double formerOccCountSmoothed = counts.getLeft() + numTotalEvents * laplacianSmoothingParameter;

        if (cooccCountSmoothed > laplacianSmoothingParameter) {
            logger.fine("Probability of seeing " + former + " before " + latter);
            logger.fine(cooccCountSmoothed / formerOccCountSmoothed + " " + counts.getRight() + "/" + counts.getLeft());
        }

        //add one smoothing
        return Math.log(cooccCountSmoothed / formerOccCountSmoothed);
    }

    public PriorityQueue<Pair<MooneyEventRepre, Double>> predict(List<MooneyEventRepre> clozeTask, Set<Integer> entities, int missingIndex, Collection<String> allPredicates, double smoothingParameter) {
        PriorityQueue<Pair<MooneyEventRepre, Double>> rankedEvents = new PriorityQueue<>(allPredicates.size(), new Comparators.DescendingScoredPairComparator<MooneyEventRepre, Double>());
        MooneyEventRepre answer = clozeTask.get(missingIndex);

        logger.info("Answer is " + answer);

        for (String head : allPredicates) {
            List<MooneyEventRepre> candidateEvms = MooneyEventRepre.generateTuples(head, entities);
            int numTotalEvents = candidateEvms.size() * allPredicates.size();

            for (MooneyEventRepre candidateEvm : candidateEvms) {
                boolean sawAnswer = false;
                if (answer.equals(candidateEvm)) {
                    sawAnswer = true;
                    logger.info("Answer candidate appears: " + candidateEvm);
                }

                double score = 0;
                for (int i = 0; i < missingIndex; i++) {
                    Pair<MooneyEventRepre, MooneyEventRepre> transformedTuples = formerBasedTransform(clozeTask.get(i), candidateEvm);
                    double precedingScore = conditionalFollowing(transformedTuples.getLeft(), transformedTuples.getRight(), smoothingParameter, numTotalEvents);

//                    if (transformedTuples.getRight().equals(answer)) {
//                        System.out.println("Following " + transformedTuples.getLeft() + " " + precedingScore);
//                    }
//
//                    if (transformedTuples.getRight().toString().equals("say(0,0,-1)")) {
//                        System.out.println("Best following " + transformedTuples.getLeft() + " " + precedingScore);
//                    }

                    score += precedingScore;
                }

                for (int i = missingIndex + 1; i < clozeTask.size(); i++) {
                    Pair<MooneyEventRepre, MooneyEventRepre> transformedTuples = formerBasedTransform(candidateEvm, clozeTask.get(i));
                    double followingScore = conditionalFollowing(transformedTuples.getLeft(), transformedTuples.getRight(), smoothingParameter, numTotalEvents);

//                    if (transformedTuples.getLeft().equals(answer)) {
//                        System.out.println("Before " + transformedTuples.getRight() + " " + followingScore);
//                    }
//
//                    if (transformedTuples.getLeft().toString().equals("say(0,0,-1)")) {
//                        System.out.println("Best before " + transformedTuples.getLeft() + " " + followingScore);
//                    }


                    score += followingScore;
                }

                if (sawAnswer) {
                    logger.info(String.format("Answer score for %s is %.2f", candidateEvm, score));
                }
                rankedEvents.add(Pair.of(candidateEvm, score));
            }
        }
        return rankedEvents;
    }

    public void test(String clozeDataDir, String outputDirPath, int[] allK, double smoothingParameter, boolean doFilter) throws IOException {
        loadEvalDir(clozeDataDir);

        File outputParentDir = new File(outputDirPath);
        if (!outputParentDir.exists()) {
            outputParentDir.mkdirs();
        }

        int[] recallCounts = new int[allK.length];
        int totalCount = 0;
        double mrr = 0;

        List<String> allPredicates = Arrays.asList(DataPool.headWords);

        logger.info("Candidate predicates number : " + allPredicates.size());

        while (hasNext()) {
            File clozeFile = getNextCloze();
            Triple<List<MooneyEventRepre>, Integer, String> clozeTask = parseCloze(clozeFile);

            List<MooneyEventRepre> chain = clozeTask.getLeft();
            int clozeIndex = clozeTask.getMiddle();
            String outputBase = clozeTask.getRight() + "_res";

            if (clozeIndex == -1) {
                logger.info("Ignoring empty file");
                continue;
            }

            Set<Integer> entities = getEntitiesFromChain(chain);
            PriorityQueue<Pair<MooneyEventRepre, Double>> fullResults = predict(chain, entities, clozeIndex, allPredicates, smoothingParameter);

            MooneyEventRepre answer = chain.get(clozeIndex);
            logger.info("Working on chain, correct answer is : " + answer);

            //write and check all results until you see the correct one
            int rank;
            boolean oov = true;
            List<String> lines = new ArrayList<>();
            for (rank = 1; rank <= fullResults.size(); rank++) {
                Pair<MooneyEventRepre, Double> r = fullResults.poll();
                lines.add(r.getLeft() + "\t" + r.getRight());
                if (r.getLeft().equals(answer)) {
                    logger.info(String.format("For cloze task : %s, correct answer found at %d", clozeFile.getName(), rank));
                    logger.info("Correct answer found at " + rank);
                    for (int kPos = 0; kPos < allK.length; kPos++) {
                        if (allK[kPos] >= rank) {
                            recallCounts[kPos]++;
                        }
                    }
                    oov = false;
                    break;
                }
            }

            File outputFile = new File(outputDirPath, outputBase);
            FileUtils.writeLines(outputFile, lines);

            if (!oov) {
                mrr += 1.0 / rank;
            } else {
                logger.info("Answer Predicate is OOV, assigning 0 MRRs");
            }
            totalCount++;
        }

        for (int kPos = 0; kPos < allK.length; kPos++) {
            logger.info(String.format("Recall at %d : %.4f", allK[kPos], recallCounts[kPos] * 1.0 / totalCount));
        }

        logger.info(String.format("MRR is : %.4f", mrr / totalCount));
    }


    private Set<Integer> getEntitiesFromChain(List<MooneyEventRepre> chain) {
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

    public static void main(String[] args) throws Exception {
        System.out.println("Starting the predictor ...");

        Configuration config = new Configuration(new File(args[0]));
        String subPath = args.length > 1 ? "_" + args[1] : "_km";

        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String inputDir = config.get("edu.cmu.cs.lti.cds.cloze.path");
        String[] headCountFileNames = config.getList("edu.cmu.cs.lti.cds.headcount.files"); //"headcounts"
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;

        int[] allK = config.getIntList("edu.cmu.cs.lti.cds.eval.rank.k");

        String outputPath = config.get("edu.cmu.cs.lti.cds.eval.result.path") + subPath;

        boolean filter = config.getBoolean("edu.cmu.cs.lti.cds.filter.lowfreq");

        DataPool.loadHeadCounts(dbPath, dbNames[0], KarlMooneyScriptCounter.defaltHeadIdMapName, headCountFileNames);

        KarlMooneyPredictor kmPredictor = new KarlMooneyPredictor(dbPath, dbNames, KarlMooneyScriptCounter.defaultOccMapName,
                KarlMooneyScriptCounter.defaultCooccMapName, headCountFileNames, KarlMooneyScriptCounter.defaltHeadIdMapName);

        kmPredictor.logger.info("Predictor started, testing ...");
        kmPredictor.test(inputDir, outputPath, allK, 1, filter);
    }
}