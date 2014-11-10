package edu.cmu.cs.lti.cds.demo;

import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import weka.core.SerializationHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/5/14
 * Time: 2:46 PM
 */
public class ModelChecker {

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration(new File(args[0]));

        String modelStoragePath = config.get("edu.cmu.cs.lti.cds.negative.model.path");

        TObjectDoubleMap<String> weights = (TObjectDoubleHashMap<String>) SerializationHelper.read(modelStoragePath + config.get("edu.cmu.cs.lti.cds.testing.model"));

        File out = new File("model_out");

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));

        for (TObjectDoubleIterator<String> iter = weights.iterator(); iter.hasNext(); ) {
            iter.advance();
//            System.out.println(iter.key() + " " + iter.value());
            writer.write(iter.key() + " " + iter.value() + "\n");
        }
    }
}