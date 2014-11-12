package edu.cmu.cs.lti.cds.demo;

import edu.cmu.cs.lti.cds.annotators.script.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.utils.Configuration;

import java.io.File;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/10/14
 * Time: 10:21 PM
 */
public class CompactFeatureDemo {
    private static Logger logger = Logger.getLogger(CompactFeatureDemo.class.getName());

    public static void main(String args[]) throws Exception {
        Configuration config = new Configuration(new File(args[0]));

        String dbPath = config.get("edu.cmu.cs.lti.cds.dbpath"); //"dbpath"
        String[] dbNames = config.getList("edu.cmu.cs.lti.cds.db.basenames"); //db names;
        String headIdMapName = KarlMooneyScriptCounter.defaltHeadIdMapName;

        DataPool.loadHeadIds(dbPath,dbNames[0],headIdMapName);


    }

}
