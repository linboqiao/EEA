package edu.cmu.cs.lti.cds.runners.script.mooney;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.annotators.script.karlmooney.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.model.KmTargetConstants;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.cds.utils.DbManager;
import edu.cmu.cs.lti.cds.utils.MultiMapUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mapdb.Fun;

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

//    private String[] idHeadMap;

    private String className = this.getClass().getName();

    private Logger logger = Logger.getLogger(className);


    public KarlMooneyPredictor(String dbPath, String[] dbNames, String occName, String cooccName, String[] countingDbFileNames, String headIdMapName) throws Exception {
        logger.setLevel(Level.INFO);

        cooccCountMaps = MultiMapUtils.loadMaps(dbPath, dbNames, cooccName, logger, "Loading coocc");
        occCountMaps = MultiMapUtils.loadMaps(dbPath, dbNames, occName, logger, "Loading occ");
        headIdMaps = MultiMapUtils.loadMaps(dbPath, dbNames, headIdMapName, logger, "Loading head ids");
        logger.info("Loading event head counts : " + dbPath + "/" + Joiner.on(",").join(countingDbFileNames));
        headTfDfMaps = DbManager.getMaps(dbPath, countingDbFileNames, EventMentionHeadCounter.defaultMentionHeadMapName);
    }


//    private void loadReverseIdMap() {
//        idHeadMap = new String[headIdMap.size()];
//        for (TObjectIntIterator it = headIdMap.iterator(); it.hasNext(); ) {
//            it.advance();
//            idHeadMap[it.value()] = (String) it.key();
//        }
//    }

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

    private Pair<List<MooneyEventRepre>, Integer> readNext() throws IOException {
        File clozeFile = allClozeFiles.get(evalPointer++);

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

        return Pair.of(repres, blankIndex);
    }

    private Pair<MooneyEventRepre, MooneyEventRepre> formerBasedTransform(MooneyEventRepre former, MooneyEventRepre latter) {
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

        //add one smoothing
        return Math.log(cooccCountSmoothed / formerOccCountSmoothed);
    }

    // this is correct but slow
    public PriorityQueue<Pair<MooneyEventRepre, Double>> predictTopK(List<MooneyEventRepre> clozeTask, Set<Integer> entities, int missingIndex, Collection<String> allPredicates, double smoothingParameter) {
        PriorityQueue<Pair<MooneyEventRepre, Double>> rankedEvents = new PriorityQueue<>(allPredicates.size(), new DescendingScoredPairComparator());
        MooneyEventRepre answer = clozeTask.get(missingIndex);

        logger.info("Answer is " + answer);
//        logger.info("Candidate head number : " + idHeadMap.length);


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
                    score += precedingScore;
                }

                for (int i = missingIndex + 1; i < clozeTask.size(); i++) {
                    Pair<MooneyEventRepre, MooneyEventRepre> transformedTuples = formerBasedTransform(candidateEvm, clozeTask.get(i));
                    double followingScore = conditionalFollowing(transformedTuples.getLeft(), transformedTuples.getRight(), smoothingParameter, numTotalEvents);
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

    public class DescendingScoredPairComparator implements Comparator<Pair<MooneyEventRepre, Double>> {
        @Override
        public int compare(Pair<MooneyEventRepre, Double> o1, Pair<MooneyEventRepre, Double> o2) {
            return -o1.getValue().compareTo(o2.getValue());
        }
    }

    public void test(String clozeDataDir, int[] allK, double smoothingParameter) throws IOException {
        loadEvalDir(clozeDataDir);

        int[] recallCounts = new int[allK.length];
        int totalCount = 0;
        double mrr = 0;

        int maxK = 0;
        for (int k : allK) {
            if (k > maxK) {
                maxK = k;
            }
        }

        Set<String> allPredicates = new HashSet<>();

        logger.info("Preparing predicates");
        for (Map<String, Fun.Tuple2<Integer, Integer>> map : headTfDfMaps) {
            allPredicates.addAll(map.keySet());
        }
        logger.info("Candidate predicates number : " + allPredicates.size());

        while (hasNext()) {
            Pair<List<MooneyEventRepre>, Integer> clozeTask = readNext();
            List<MooneyEventRepre> chain = clozeTask.getLeft();
            int clozeIndex = clozeTask.getRight();

            if (clozeIndex == -1) {
                logger.info("Ignoring empty file");
                continue;
            }

            Set<Integer> entities = getEntitiesFromChain(chain);
            PriorityQueue<Pair<MooneyEventRepre, Double>> fullResults = predictTopK(chain, entities, clozeIndex, allPredicates, smoothingParameter);

            List<Pair<MooneyEventRepre, Double>> topkResults = new ArrayList<>();

            for (int rank = 0; rank < maxK; rank++) {
                if (fullResults.isEmpty()) {
                    break;
                }
                topkResults.add(fullResults.poll());
            }

            MooneyEventRepre answer = chain.get(clozeIndex);
            logger.info("Working on chain, correct answer is : " + answer);

            logger.info(topkResults.toString());

            int rank = 0;
            boolean oov = true;
            for (Pair<MooneyEventRepre, Double> r : fullResults) {
                rank++;
                if (r.getLeft().equals(answer)) {
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

            if (!oov) {
                mrr += 1 / rank;
            } else {
                logger.info("Answer Predicate is OOV, assigning 0 MRRs");
            }
            totalCount++;
        }

        for (int kPos = 0; kPos < allK.length; kPos++) {
            logger.info(String.format("Recall at %d : %.2f", allK[kPos], recallCounts[kPos] * 1.0 / totalCount));
        }
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
        String subPath = args.length > 1 ? args[1] : "";

        String parentDataDir = config.get("edu.cmu.cs.lti.cds.parent.output"); // "data";
        String clozeDir = config.get("edu.cmu.cs.lti.cds.cloze.base") + "_" + subPath; // "cloze"
        String inputDir = parentDataDir + "/03_" + clozeDir;
        String[] headCountFileNames = config.getList("edu.cmu.cs.lti.cds.headcount.files"); //"headcounts"
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;

        int[] allK = config.getIntList("edu.cmu.cs.lti.cds.eval.rank.k");

        KarlMooneyPredictor kmPredictor = new KarlMooneyPredictor("data/_db", dbNames, KarlMooneyScriptCounter.defaultOccMapName,
                KarlMooneyScriptCounter.defaultCooccMapName, headCountFileNames, KarlMooneyScriptCounter.defaltHeadIdMapName);

        kmPredictor.logger.info("Predictor started, testing ...");
        kmPredictor.test(inputDir, allK, 1);
    }
}