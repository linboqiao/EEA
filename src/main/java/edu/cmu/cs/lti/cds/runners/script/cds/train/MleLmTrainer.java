package edu.cmu.cs.lti.cds.runners.script.cds.train;

import edu.cmu.cs.lti.utils.Configuration;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/2/14
 * Time: 10:12 PM
 */
public class MleLmTrainer {
    //implements a super slow MLE trainer, simply to see if it works

    public static void main(String[] args) throws IOException {
        Configuration config = new Configuration(new File(args[0]));

        String[] headCountFileNames = config.getList("edu.cmu.cs.lti.cds.headcount.files"); //"headcounts"
        int skipK = config.getInt("edu.cmu.cs.lti.cds.skipgram.n"); //"skip"

    }

}
