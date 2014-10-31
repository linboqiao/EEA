package edu.cmu.cs.lti.cds.runners.script.mooney;

import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.annotators.script.karlmooney.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.model.KmTargetConstants;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.cds.utils.DbManager;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mapdb.DB;
import org.mapdb.Fun;
import weka.core.SerializationHelper;

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
    private DB db;

    private List<File> allClozeFiles;

    private int evalPointer = 0;

    public static final String clozeFileSuffix = ".txt";

    private TObjectIntMap<TIntList> cooccCounts;

    private TObjectIntMap<TIntList> occCounts;

    private Map<String, Fun.Tuple2<Integer, Integer>> headCountMap;

    private TObjectIntMap<String> headIdMap = new TObjectIntHashMap<>();

    private String[] idHeadMap;

    private String className = this.getClass().getName();

    private Logger logger = Logger.getLogger(className);


    public KarlMooneyPredictor(String dbPath, String dbName, String occName, String cooccName, String countingDbFileName, String headIdMapName) throws Exception {
        logger.setLevel(Level.INFO);

        logger.info("Loading cooccs " + new File(dbPath, dbName + "_" + cooccName).getAbsolutePath());
        cooccCounts = (TObjectIntMap<TIntList>) SerializationHelper.read(new File(dbPath, dbName + "_" + cooccName).getAbsolutePath());
        logger.info("Loading occ " + new File(dbPath, dbName + "_" + occName).getAbsolutePath());
        occCounts = (TObjectIntMap<TIntList>) SerializationHelper.read(new File(dbPath, dbName + "_" + occName).getAbsolutePath());
        logger.info("Loading head ids: " + new File(dbPath, dbName + "_" + headIdMapName).getAbsolutePath());
        headIdMap = (TObjectIntMap<String>) SerializationHelper.read(new File(dbPath, dbName + "_" + headIdMapName).getAbsolutePath());
        logger.info("Loading reverse head ids");
        loadReverseIdMap();

        logger.info("Loading event head counts : " + dbPath + "/" + countingDbFileName);
        if (countingDbFileName != null) {
            DB headCountDb = DbManager.getDB(dbPath, countingDbFileName);
            headCountMap = headCountDb.getHashMap(EventMentionHeadCounter.defaultMentionHeadMapName);
        }
    }

    private void loadReverseIdMap() {
        idHeadMap = new String[headIdMap.size()];
        for (TObjectIntIterator it = headIdMap.iterator(); it.hasNext(); ) {
            it.advance();
            idHeadMap[it.value()] = (String) it.key();
        }
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
//        if (former.getPredicate().equals("form")) {
//            System.out.println("transforming");
//            System.out.println(former);
//            System.out.println(latter);
//        }

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

//        if (former.getPredicate().equals("have") && former.getArg0() == 1) {
//            System.out.println("Transformed results");
//            System.out.println(transformedFormer);
//            System.out.println(transformedLatter);
//            Utils.pause();
//        }
        return Pair.of(transformedFormer, transformedLatter);
    }


    //this is correct but slow.
    private double conditionalFollowing(MooneyEventRepre former, MooneyEventRepre latter, double laplacianSmoothingParameter) {
        TIntLinkedList formerTuple = former.toCompactForm(headIdMap);
        TIntLinkedList latterTuple = latter.toCompactForm(headIdMap);

        TIntLinkedList joinedPair = MooneyEventRepre.joinCompactForm(formerTuple, latterTuple);

        double numTotalEvents = occCounts.size();

        double cooccCountSmoothed;
        if (cooccCounts.containsKey(joinedPair)) {
            cooccCountSmoothed = cooccCounts.get(joinedPair) + laplacianSmoothingParameter;
        } else {
            cooccCountSmoothed = laplacianSmoothingParameter;
        }

        double formerOccCountSmoothed;
        if (occCounts.containsKey(formerTuple)) {
            formerOccCountSmoothed = occCounts.get(formerTuple) + numTotalEvents * laplacianSmoothingParameter;
        } else {
            formerOccCountSmoothed = numTotalEvents * laplacianSmoothingParameter;
        }

//        if (former.getPredicate().equals("have")) {
//            System.out.println("Joint pair is " + joinedPair);
//            System.out.println(String.format("%s -- %s : %.2f %.2f %.5f", former, latter, cooccCountSmoothed, formerOccCountSmoothed, cooccCountSmoothed / formerOccCountSmoothed));
//            System.out.println("Coocc count is " + cooccCounts.get(joinedPair));
//        }

        //add one smoothing
        return Math.log(cooccCountSmoothed / formerOccCountSmoothed);
    }


    // this is correct but slow
    public List<Pair<MooneyEventRepre, Double>> predictTopK(List<MooneyEventRepre> clozeTask, Set<Integer> entities, int missingIndex, int k, double smoothingParameter) {

        // the queue size is not right
        PriorityQueue<Pair<MooneyEventRepre, Double>> rankedEvents = new PriorityQueue<>(headCountMap.size(), new DescendingScoredPairComparator());

        MooneyEventRepre answer = clozeTask.get(missingIndex);

        System.out.println("Answer is " + answer);

        System.out.println("Candidate head number : " + idHeadMap.length);


        for (String head : idHeadMap) {
//            System.out.println(head);
//            System.out.println(entities);

            List<MooneyEventRepre> candidateEvms = MooneyEventRepre.generateTuples(head, entities);

//            System.out.println(candidateEvms);


//        }
//
//        for (TIntList candidate : occCounts.keySet()) {
//
//            List<MooneyEventRepre> candidateEvms = MooneyEventRepre.generateTuples(candidate, idHeadMap);


            for (MooneyEventRepre candidateEvm : candidateEvms) {
                boolean sawAnswer = false;
                if (answer.equals(candidateEvm)) {
                    sawAnswer = true;
                    System.out.println("Answer candidate is " + candidateEvm);
                }

                double score = 0;
                for (int i = 0; i < missingIndex; i++) {
                    Pair<MooneyEventRepre, MooneyEventRepre> transformedTuples = formerBasedTransform(clozeTask.get(i), candidateEvm);

                    double precedingScore = conditionalFollowing(transformedTuples.getLeft(), transformedTuples.getRight(), smoothingParameter);
                    if (sawAnswer) {
                        System.out.println("Preceding transforms");
                        System.out.println(clozeTask.get(i) + " - " + candidateEvm + "  to " + transformedTuples);
                        System.out.println(String.format("Preceding score for Answer %s - %s is %.2f ", transformedTuples.getLeft(), transformedTuples.getRight(), precedingScore));

                    }
                    score += precedingScore;
                }

                for (int i = missingIndex + 1; i < clozeTask.size(); i++) {
                    Pair<MooneyEventRepre, MooneyEventRepre> transformedTuples = formerBasedTransform(candidateEvm, clozeTask.get(i));

                    double followingScore = conditionalFollowing(transformedTuples.getLeft(), transformedTuples.getRight(), smoothingParameter);

                    if (sawAnswer) {
                        System.out.println("Succeeding transforms");
                        System.out.println(candidateEvm + " " + clozeTask.get(i) + " to " + transformedTuples);
                        System.out.println(String.format("Following score for Answer %s - %s is %.2f", transformedTuples.getLeft(), transformedTuples.getRight(), followingScore));
                    }

//                    if (sawAnswer) {
//                        System.out.println(String.format("Following score for answer %s - %s is %.2f ", transformedTuples.getLeft(), transformedTuples.getRight(), followingScore));
//                    } else {
//                        System.out.println(String.format("Following score for candidate %s - %s is %.2f ", transformedTuples.getLeft(), transformedTuples.getRight(), followingScore));
//                    }
                    score += followingScore;
                }


                if (sawAnswer) {
                    System.out.println(String.format("Answer score for %s is %.2f", candidateEvm, score));
//                    Utils.pause();
                } else {
//                    if (score > -1000)
//                        System.out.println(String.format("Candidate score for %s is %.2f", candidateEvm, score));
                }

                rankedEvents.add(Pair.of(candidateEvm, score));
            }
        }
//        Utils.pause();

//        System.out.println("Best one is " + rankedEvents.peek().getLeft());

        List<Pair<MooneyEventRepre, Double>> topKEvents = new ArrayList<>();

        for (int rank = 0; rank < k; rank++) {
            if (rankedEvents.isEmpty()) {
                break;
            }
            topKEvents.add(rankedEvents.poll());
        }

        return topKEvents;
    }

    public class DescendingScoredPairComparator implements Comparator<Pair<MooneyEventRepre, Double>> {
        @Override
        public int compare(Pair<MooneyEventRepre, Double> o1, Pair<MooneyEventRepre, Double> o2) {
            return -o1.getValue().compareTo(o2.getValue());
        }
    }


    public double test(String clozeDataDir, int k, double smoothingParameter) throws IOException {
        loadEvalDir(clozeDataDir);

        int recallCount = 0;
        while (hasNext()) {
            Pair<List<MooneyEventRepre>, Integer> clozeTask = readNext();
            List<MooneyEventRepre> chain = clozeTask.getLeft();
            int clozeIndex = clozeTask.getRight();

            if (clozeIndex == -1) {
                logger.info("Ignoring empty file");
                continue;
            }

            Set<Integer> entities = getEntitiesFromChain(chain);
            List<Pair<MooneyEventRepre, Double>> topkResults = predictTopK(chain, entities, clozeIndex, k, smoothingParameter);

            System.out.println(topkResults);

            MooneyEventRepre answer = chain.get(clozeIndex);
            logger.info("Working on chain, correct answer is : " + answer);

            logger.info(topkResults.toString());
            for (Pair<MooneyEventRepre, Double> r : topkResults) {
                if (r.getLeft().equals(answer)) {
                    logger.info("Correct answer found");
                    recallCount++;
                }
            }

//            Utils.pause();
        }

        return recallCount * 1.0 / clozeTaskSize();
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

    private void check() {
        System.out.println("Coocc size " + cooccCounts.size());
        System.out.println("Occ size " + occCounts.size());

        int count = 10;

        for (TObjectIntIterator<TIntList> it = cooccCounts.iterator(); it.hasNext(); ) {
            it.advance();
            TIntList pairList = it.key();
            System.out.println(MooneyEventRepre.fromCompactForm(pairList.subList(0, 4), idHeadMap));
            System.out.println(MooneyEventRepre.fromCompactForm(pairList.subList(4, pairList.size()), idHeadMap));
            System.out.println(it.value());

            count--;
            if (count <= 0) {
                break;
            }
        }

        count = 10;
        for (TObjectIntIterator<TIntList> it = occCounts.iterator(); it.hasNext(); ) {
            it.advance();
            System.out.println(MooneyEventRepre.fromCompactForm(it.key(), idHeadMap));
            System.out.println(it.value());

            count--;
            if (count <= 0) {
                break;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting the predictor ...");

        String dbName = args[0]; //occs_94-96
        String headCountingFileName = args[1]; //"headcounts_94-96"
        String evalDataPath = args[2]; //"data/03_cloze_dev"
        int k = Integer.parseInt(args[3]); //the top "k" used for evaluation

        KarlMooneyPredictor kmPredictor = new KarlMooneyPredictor("data/_db", dbName, KarlMooneyScriptCounter.defaultOccMapName,
                KarlMooneyScriptCounter.defaultCooccMapName, headCountingFileName, KarlMooneyScriptCounter.defaltHeadIdMapName);

        kmPredictor.logger.info("Predictor started, testing ...");
        double recallAtK = kmPredictor.test(evalDataPath, k, 1);
        kmPredictor.logger.info(String.format("Recall at %d : %.2f", k, recallAtK));
    }
}