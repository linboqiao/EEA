package edu.cmu.cs.lti.cds.model;

import edu.cmu.cs.lti.ling.PropBankTagSet;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/21/14
 * Time: 2:42 PM
 */
public class KmTargetConstants {
    public static final Map<String, Integer> targetArguments;


    public static final int nullArgMarker = -1;
    public static final int firstArg0Marker = 1;
    public static final int firstArg1Marker = 2;
    public static final int firstArg2Marker = 3;
    public static final int otherMarker = 0;

    static {
        targetArguments = new LinkedHashMap<>();
        targetArguments.put(PropBankTagSet.ARG0, firstArg0Marker);
        targetArguments.put(PropBankTagSet.ARG1, firstArg1Marker);
        targetArguments.put(PropBankTagSet.ARG2, firstArg2Marker);
    }

    public static final String clozeBlankIndicator = "##blank##";

}
