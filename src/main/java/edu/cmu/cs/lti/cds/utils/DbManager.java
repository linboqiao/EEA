package edu.cmu.cs.lti.cds.utils;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;

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



}
