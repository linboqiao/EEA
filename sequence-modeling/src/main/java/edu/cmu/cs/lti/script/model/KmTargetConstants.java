package edu.cmu.cs.lti.script.model;

import edu.cmu.cs.lti.ling.PropBankTagSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/21/14
 * Time: 2:42 PM
 */
public class KmTargetConstants {
    //TODO very ugly class

    public static final Map<String, Integer> targetArguments;

    public static final List<Integer> notNullArguments;

    public static int argMarkerToSlotIndex(int argMarker) {
        return argMarker - anchorArg0Marker;
    }

    public static int slotIndexToArgMarker(int slotIndex) {
        return slotIndex + anchorArg0Marker;
    }

    public static final int nullArgMarker = -1;
    public static final int anchorArg0Marker = 1;
    public static final int anchorArg1Marker = 2;
    public static final int anchorArg2Marker = 3;
    public static final int otherMarker = 0;

    public static final int numSlots = 3;

    static {
        targetArguments = new LinkedHashMap<>();
        targetArguments.put(PropBankTagSet.ARG0, anchorArg0Marker);
        targetArguments.put(PropBankTagSet.ARG1, anchorArg1Marker);
        targetArguments.put(PropBankTagSet.ARG2, anchorArg2Marker);

        notNullArguments = new ArrayList<>();
        notNullArguments.addAll(targetArguments.values());
        notNullArguments.add(otherMarker);
    }

    public static final String clozeBlankIndicator = "##blank##";

}
