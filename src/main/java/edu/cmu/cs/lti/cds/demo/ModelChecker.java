package edu.cmu.cs.lti.cds.demo;

import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import weka.core.SerializationHelper;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/5/14
 * Time: 2:46 PM
 */
public class ModelChecker {

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration(new File(args[0]));

        String modelStoragePath = config.get("edu.cmu.cs.lti.cds.nce.model.path");

        TObjectDoubleMap<String> weights = (TObjectDoubleHashMap<String>) SerializationHelper.read(modelStoragePath + "59.ser");

        for (TObjectDoubleIterator<String> iter = weights.iterator(); iter.hasNext(); ) {
            iter.advance();
            System.out.println(iter.key() + " " + iter.value());
        }
    }
}