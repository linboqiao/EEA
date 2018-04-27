package edu.cmu.cs.lti.script.utils;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/27/14
 * Time: 3:50 PM
 */
public class DbManager {
    public static DB getDB(String dbPath, String dbFileName) {
        return getDB(dbPath, dbFileName, false);
    }

    public static DB getDB(String dbPath, String dbFileName, boolean write) {
        DB db;
        if (write) {
            db = DBMaker.newFileDB(new File(dbPath, dbFileName)).transactionDisable().closeOnJvmShutdown().make();
        } else {
            db = DBMaker.newFileDB(new File(dbPath, dbFileName)).readOnly().make();
        }

        return db;
    }

    public static DB[] getDBs(String dbPath, String[] dbFileNames) {
        return getDBs(dbPath, dbFileNames, false);
    }

    public static DB[] getDBs(String dbPath, String[] dbFileNames, boolean write) {
        DB[] allDbs = new DB[dbFileNames.length];

        for (int i = 0; i < dbFileNames.length; i++) {
            allDbs[i] = getDB(dbPath, dbFileNames[i], write);
        }
        return allDbs;
    }

    public static Map[] getMaps(String dbPath, String[] dbFileNames, String mapName) {
        return getMaps(dbPath,  dbFileNames, mapName, false);
    }

    public static Map[] getMaps(String dbPath, String[] dbFileNames, String mapName, boolean write) {
        return getMaps(getDBs(dbPath, dbFileNames, write), mapName);
    }

    public static Map[] getMaps(DB[] dbs, String mapName) {
        Map[] headTfDfMaps = new Map[dbs.length];
        for (int i = 0; i < dbs.length; i++) {
            headTfDfMaps[i] = dbs[i].getHashMap(mapName);
        }
        return headTfDfMaps;
    }


}
