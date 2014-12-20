package edu.cmu.cs.lti.cds.runners.misc;

import edu.cmu.cs.lti.cds.annotators.stats.EventMentionHeadCounter;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.HTreeMap;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/25/14
 * Time: 5:35 PM
 */
public class HeadWordFrequency {
    private final DB db;

    private HTreeMap<String, Fun.Tuple2<Integer, Integer>> headCounts;

    public HeadWordFrequency(String dbPath, String dbName, String countsName) {
        DBMaker dbm = DBMaker.newFileDB(new File(dbPath, dbName)).readOnly();
        db = dbm.make();

        headCounts = db.getHashMap(countsName);
    }

    public static void main(String[] args) throws IOException {
        HeadWordFrequency freq = new HeadWordFrequency("data/_db",
                EventMentionHeadCounter.defaultDBName, EventMentionHeadCounter.defaultMentionHeadMapName);

        for (Map.Entry<String, Fun.Tuple2<Integer, Integer>> st : freq.headCounts.entrySet()) {
            System.out.println(st.getKey() + " " + st.getValue().a + " " + st.getValue().b);
        }

    }

}
