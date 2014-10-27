package edu.cmu.cs.lti.cds.runners.script.mooney;

import edu.cmu.cs.lti.cds.annotators.script.EventMentionHeadCounter;
import edu.cmu.cs.lti.cds.annotators.script.karlmooney.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.model.KmTargetConstants;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.cds.utils.DbManager;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple4;

import java.io.File;
import java.io.IOException;
import java.util.*;

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

    private Map<Tuple2<Tuple4<String, Integer, Integer, Integer>, Tuple4<String, Integer, Integer, Integer>>, Integer> cooccCounts;
    private Map<Tuple4<String, Integer, Integer, Integer>, Integer> occCounts;
    private Map<String, Fun.Tuple2<Integer, Integer>> headCountMap;

    public KarlMooneyPredictor(String dbPath, String dbName, String occName, String cooccName, String countingDbFileName) {
        DBMaker dbm = DBMaker.newFileDB(new File(dbPath, dbName)).readOnly();
        db = dbm.make();
        evalPointer = 0;

        cooccCounts = db.getTreeMap(cooccName);
        occCounts = db.getTreeMap(occName);

        if (countingDbFileName != null) {
            DB headCountDb = DbManager.getDB(dbPath, countingDbFileName);
            headCountMap = headCountDb.getHashMap(EventMentionHeadCounter.defaultMentionHeadMapName);
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

        List<String> lines = FileUtils.readLines(clozeFile);

        List<MooneyEventRepre> repres = new ArrayList<>();

        int blankIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.startsWith(KmTargetConstants.clozeBlankIndicator)) {
                blankIndex = i;
                repres.add(MooneyEventRepre.fromString(StringUtils.stripStart(line, KmTargetConstants.clozeBlankIndicator)));
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
            int formerArgumentId = formerArguments[formerSlotId];
            if (formerArgumentId == 0 || formerArgumentId == -1) {
                transformedFormer.setArgument(formerSlotId, formerArgumentId);
            } else {
                transformMap.put(formerArgumentId, formerSlotId);
                transformedFormer.setArgument(formerSlotId, formerSlotId);
            }
        }

        TIntSet mask = new TIntHashSet();
        for (int latterSlotId = 0; latterSlotId < latterArguments.length; latterSlotId++) {
            int latterArgumentId = latterArguments[latterSlotId];
            if (latterArgumentId == 0 || latterArgumentId == -1) {
                transformedLatter.setArgument(latterSlotId, latterArgumentId);
            } else {
                int tranformedId = transformMap.get(latterArgumentId);
                transformedLatter.setArgument(latterSlotId, tranformedId);
                mask.add(tranformedId);
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
    private double conditionalFollowing(MooneyEventRepre former, MooneyEventRepre latter,
                                        Map<Tuple2<Tuple4<String, Integer, Integer, Integer>, Tuple4<String, Integer, Integer, Integer>>, Integer> cooccs,
                                        Map<Tuple4<String, Integer, Integer, Integer>, Integer> occs, double laplacianSmoothingParameter) {
        Tuple4<String, Integer, Integer, Integer> formerTuple = former.toTuple();
        Tuple4<String, Integer, Integer, Integer> latterTuple = latter.toTuple();

        Integer cooccCount = cooccs.get(new Tuple2<>(formerTuple, latterTuple));
        Integer formerOccCount = occs.get(formerTuple);

        double numTotalEvents = occs.size();

        double cooccCountSmoothed = cooccCount == null ? laplacianSmoothingParameter : cooccCount + laplacianSmoothingParameter;
        double formerOccCountSmoothed = formerOccCount == null ? numTotalEvents * laplacianSmoothingParameter : formerOccCount + numTotalEvents * laplacianSmoothingParameter;

//        System.out.println(String.format("%s -- %s : %.2f %.2f %.5f", former, latter, cooccCountSmoothed, formerOccCountSmoothed, cooccCountSmoothed / formerOccCountSmoothed));

        //add one smoothing
        return Math.log(cooccCountSmoothed / formerOccCountSmoothed);
    }


    // this is correct but slow
    public List<Pair<MooneyEventRepre, Double>> predicateTopK(List<MooneyEventRepre> clozeTask, int missingIndex,
                                                              Map<Tuple2<Tuple4<String, Integer, Integer, Integer>,
                                                                      Tuple4<String, Integer, Integer, Integer>>, Integer> cooccs,
                                                              Map<Tuple4<String, Integer, Integer, Integer>, Integer> occs, int k, double smoothingParameter) {

        PriorityQueue<Pair<MooneyEventRepre, Double>> rankedEvents = new PriorityQueue<>(occs.size(), new DescendingScoredPairComparator());


        MooneyEventRepre answer = clozeTask.get(missingIndex);

//        Map<Tuple4<String, Integer, Integer, Integer>, Integer> correctCandidate = new HashMap<>();
//        correctCandidate.put(answer.toTuple(), 1);
//        correctCandidate.put(MooneyEventRepre.fromString("say(0,0,-1)").toTuple(), 1);
//
//        for (Map.Entry<Tuple4<String, Integer, Integer, Integer>, Integer> candidate : correctCandidate.entrySet()) {

        for (Map.Entry<Tuple4<String, Integer, Integer, Integer>, Integer> candidate : occs.entrySet()) {
            double score = 0;
            ////This is predicate only
//            String candidatePredicate = candidate.getKey().a;
//            MooneyEventRepre candidateEvm = new MooneyEventRepre(candidatePredicate, targetEventRepre.getArg0(), targetEventRepre.getArg1(), targetEventRepre.getArg2());

            MooneyEventRepre candidateEvm = MooneyEventRepre.fromTuple(candidate.getKey());

            for (int i = 0; i < missingIndex; i++) {
                Pair<MooneyEventRepre, MooneyEventRepre> transformedTuples = formerBasedTransform(clozeTask.get(i), candidateEvm);
                score += conditionalFollowing(transformedTuples.getLeft(), transformedTuples.getRight(), cooccs, occs, smoothingParameter);
            }

            for (int i = missingIndex + 1; i < clozeTask.size(); i++) {
                Pair<MooneyEventRepre, MooneyEventRepre> transformedTuples = formerBasedTransform(candidateEvm, clozeTask.get(i));
                score += conditionalFollowing(transformedTuples.getLeft(), transformedTuples.getRight(), cooccs, occs, smoothingParameter);
            }

            rankedEvents.add(Pair.of(candidateEvm, score));
        }

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

            List<Pair<MooneyEventRepre, Double>> topkResults = predicateTopK(chain, clozeIndex, cooccCounts, occCounts, k, smoothingParameter);

            MooneyEventRepre answer = chain.get(clozeIndex);
            System.out.println("Correct answer is : " + answer);

            System.out.println(topkResults);
            for (Pair<MooneyEventRepre, Double> r : topkResults) {
                if (r.getLeft().equals(answer)) {
                    System.out.println("Correct answer found");
                    recallCount++;
                }
            }
        }

        return recallCount * 1.0 / clozeTaskSize();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting the predictor ...");

        KarlMooneyPredictor kmPredictor = new KarlMooneyPredictor("data/_db",
                "occs_94-96", KarlMooneyScriptCounter.defaultOccMapName,
                KarlMooneyScriptCounter.defaultCooccMapName, "headcounts_94-96");

//        ConcurrentNavigableMap<Tuple2<Tuple4<String, Integer, Integer, Integer>, Tuple4<String, Integer, Integer, Integer>>, Integer> cooccCounts
//                = kmPredictor.readCooccCounts(KarlMooneyScriptCounter.defaultMentionHeadMapName);
//        ConcurrentNavigableMap<Tuple4<String, Integer, Integer, Integer>, Integer> occCounts = kmPredictor.readOccCounts(KarlMooneyScriptCounter.defaultOccMapName);

//        System.out.println("Coocc size " + kmPredictor.cooccCounts.size());
//        System.out.println("Occ size " + kmPredictor.occCounts.size());
//
//
//        int count = 10;
//        for (Map.Entry<Tuple2<Tuple4<String, Integer, Integer, Integer>, Tuple4<String, Integer, Integer, Integer>>, Integer> entry : kmPredictor.cooccCounts.entrySet()) {
//            System.out.println(entry.getKey().a);
//            System.out.println(entry.getKey().b);
//            System.out.println(entry.getValue());
//
//            count--;
//            if (count <= 0) {
//                break;
//            }
//        }
//
//        count = 10;
//        for (Map.Entry<Tuple4<String, Integer, Integer, Integer>, Integer> entry : kmPredictor.occCounts.entrySet()) {
//            System.out.println(entry.getKey());
//            System.out.println(entry.getValue());
//
//            count--;
//            if (count <= 0) {
//                break;
//            }
//        }


        System.out.println("Predictor started, testing ...");

//        double recallAt10 = kmPredictor.test("data/03_cloze_files", 10, 0.01);
//        System.out.println("Recall at 10 " + recallAt10);
    }
}
