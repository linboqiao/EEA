package edu.cmu.cs.lti.script.demo;

import edu.cmu.cs.lti.script.annotators.learn.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.script.model.MooneyEventRepre;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.script.utils.MultiMapUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/21/15
 * Time: 4:35 PM
 */
public class OccCountCheck {

    public static void main(String[] argv) throws Exception {
        Configuration config = new Configuration(new File("settings.properties"));
        DataPool.loadHeadStatistics(config, false);

        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath");
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames");
        String cooccName = KarlMooneyScriptCounter.defaultCooccMapName;
        String occName = KarlMooneyScriptCounter.defaultOccMapName;
        String headIdMapName = KarlMooneyScriptCounter.defaltHeadIdMapName;

        Logger logger = LoggerFactory.getLogger(OccCountCheck.class);


        TObjectIntMap<TIntList>[] cooccCountMaps = MultiMapUtils.loadMaps(dbPath, dbNames, cooccName, logger, "Loading coocc");
        TObjectIntMap<TIntList>[] occCountMaps = MultiMapUtils.loadMaps(dbPath, dbNames, occName, logger, "Loading occ");
        TObjectIntMap<String>[] headIdMaps = MultiMapUtils.loadMaps(dbPath, dbNames, headIdMapName, logger, "Loading head ids");

        Scanner scan = new Scanner(System.in);


        while (true) {
            System.out.print("Mention 1 : ");
            String mention1 = scan.nextLine();

            System.out.print("Mention 2 : ");
            String mention2 = scan.nextLine();

            MooneyEventRepre former = MooneyEventRepre.fromString(mention1);
            MooneyEventRepre latter = MooneyEventRepre.fromString(mention2);

            Pair<Integer, Integer> counts = MultiMapUtils.getCounts(former, latter, cooccCountMaps, occCountMaps, headIdMaps);
            System.out.println(counts);
        }
    }
}
