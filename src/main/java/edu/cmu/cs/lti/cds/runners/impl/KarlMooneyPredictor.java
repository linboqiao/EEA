package edu.cmu.cs.lti.cds.runners.impl;

import edu.cmu.cs.lti.cds.annotators.script.karlmooney.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple4;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;

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

    public KarlMooneyPredictor(String dbPath, String dbName) {
        DBMaker dbm = DBMaker.newFileDB(new File(dbPath, dbName)).readOnly();
        db = dbm.make();
        evalPointer = 0;
    }

    private ConcurrentNavigableMap<Tuple2<Tuple4<String, Integer, Integer, Integer>, Tuple4<String, Integer, Integer, Integer>>, Integer> readCooccCounts(String mapName) {
        return db.getTreeMap(mapName);
    }

    private ConcurrentNavigableMap<Tuple4<String, Integer, Integer, Integer>, Integer> readOccCounts(String mapName) {
        return db.getTreeMap(mapName);
    }

    private void loadEvalDir(String clozeDataDir) throws IOException {
        for (File clozeFile : new File(clozeDataDir).listFiles()) {
            if (!clozeFile.isDirectory() && clozeFile.getName().endsWith(clozeFileSuffix)) {
                allClozeFiles.add(clozeFile);
            }
        }
    }

    private boolean hasNext() {
        return evalPointer < allClozeFiles.size();
    }

    private Triple<List<MooneyEventRepre>, Integer, String> readNext() throws IOException {
        File clozeFile = allClozeFiles.get(evalPointer++);

        List<String> lines = FileUtils.readLines(clozeFile);

        List<MooneyEventRepre> repres = new ArrayList<>();

        String missed = null;
        int blankIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String[] parts = lines.get(i).split("\t");
            repres.add(MooneyEventRepre.fromString(parts[0]));

            if (parts.length == 2) {
                missed = parts[1];
                blankIndex = i;
            }
        }

        return Triple.of(repres, blankIndex, missed);
    }

    public void predicate(List<MooneyEventRepre> clozeTask, int missingIndex, ConcurrentNavigableMap<Tuple2<Tuple4<String, Integer, Integer, Integer>,
            Tuple4<String, Integer, Integer, Integer>>, Integer> cooccs, ConcurrentNavigableMap<Tuple4<String, Integer, Integer, Integer>, Integer> occs) {
        for (int i  = 0 ; i < missingIndex; i ++){

        }
    }

    public void eval(String clozeDataDir, String dbDirectory, String coocName, String occName) throws IOException {
        loadEvalDir(clozeDataDir);

        ConcurrentNavigableMap<Tuple2<Tuple4<String, Integer, Integer, Integer>, Tuple4<String, Integer, Integer, Integer>>, Integer> cooccCounts = readCooccCounts(coocName);
        ConcurrentNavigableMap<Tuple4<String, Integer, Integer, Integer>, Integer> occCounts = readOccCounts(occName);

        while (hasNext()) {
            Triple<List<MooneyEventRepre>, Integer, String> clozeTask = readNext();
            predicate(clozeTask.getLeft(), clozeTask.getMiddle(), cooccCounts, occCounts);
        }
    }

    public static void main(String[] args) {
        KarlMooneyPredictor kmPredictor = new KarlMooneyPredictor("data/_db", KarlMooneyScriptCounter.defaultDBName);
        ConcurrentNavigableMap<Tuple2<Tuple4<String, Integer, Integer, Integer>, Tuple4<String, Integer, Integer, Integer>>, Integer> cooccCounts
                = kmPredictor.readCooccCounts(KarlMooneyScriptCounter.defaultCooccMapName);
        ConcurrentNavigableMap<Tuple4<String, Integer, Integer, Integer>, Integer> occCounts = kmPredictor.readOccCounts(KarlMooneyScriptCounter.defaultOccMapName);

        System.out.println("Coocc size " + cooccCounts.size());
        System.out.println("Occ size " + occCounts.size());


        int count = 10;

        for (Map.Entry<Tuple2<Tuple4<String, Integer, Integer, Integer>, Tuple4<String, Integer, Integer, Integer>>, Integer> entry : cooccCounts.entrySet()) {
            System.out.println(entry.getKey().a);
            System.out.println(entry.getKey().b);
            System.out.println(entry.getValue());

            count--;
            if (count <= 0) {
                break;
            }
        }

        System.out.println("Done");
    }
}
