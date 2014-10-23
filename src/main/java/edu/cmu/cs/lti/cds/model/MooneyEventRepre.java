package edu.cmu.cs.lti.cds.model;

import gnu.trove.set.TIntSet;
import org.mapdb.Fun;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/20/14
 * Time: 5:02 PM
 */
public class MooneyEventRepre {
    String predicate;
    int arg0;
    int arg1;
    int arg2;
    //to be predict, consider missing
    boolean toPredict;

    public MooneyEventRepre() {

    }

    public MooneyEventRepre(String p, int a0, int a1, int a2) {
        super();
        this.predicate = p.replace("\n", " ").replace("(", "_").replace(")", "_");
        this.arg0 = a0;
        this.arg1 = a1;
        this.arg2 = a2;
        toPredict = false;
    }

    public MooneyEventRepre(int a0, int a1, int a2) {
        super();
        this.predicate = "";
        this.arg0 = a0;
        this.arg1 = a1;
        this.arg2 = a2;
        this.toPredict = true;
    }

    public int[] getAllArguments() {
        return new int[]{arg0, arg1, arg2};
    }

    //TODO make this elegant
    public void setArgument(int slotId, int value) {
        if (slotId == 0) {
            arg0 = value;
        } else if (slotId == 1) {
            arg1 = value;
        } else if (slotId == 2) {
            arg2 = value;
        }
    }


    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public void setArg0(int arg0) {
        this.arg0 = arg0;
    }

    public void setArg1(int arg1) {
        this.arg1 = arg1;
    }

    public void setArg2(int arg2) {
        this.arg2 = arg2;
    }

    public int getArg2() {
        return arg2;
    }

    public String getPredicate() {
        return predicate;
    }

    public int getArg0() {
        return arg0;
    }

    public int getArg1() {
        return arg1;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MooneyEventRepre repre = (MooneyEventRepre) o;

        if (predicate != null ? !predicate.equals(repre.predicate) : repre.predicate != null) return false;
        if (arg0 != repre.arg0) return false;
        if (arg1 != repre.arg1) return false;
        if (arg2 != repre.arg2) return false;

        return true;
    }

    @Override
    public String toString() {
        return String.format("%s(%d,%d,%d)", predicate, arg0, arg1, arg2);
    }

    public String toEmptyPredicateString() {
        return String.format("%s(%d,%d,%d)", KmTargetConstants.clozeBlankIndicator, arg0, arg1, arg2);
    }

    public String toStringWithEmptyIndicator(TIntSet mask) {
        return String.format("%s%s(%d,%d,%d)",
                KmTargetConstants.clozeBlankIndicator,
                predicate,
                maskedSlot(mask, arg0, KmTargetConstants.firstArg0Marker),
                maskedSlot(mask, arg1, KmTargetConstants.firstArg1Marker),
                maskedSlot(mask, arg2, KmTargetConstants.firstArg2Marker)
        );
    }

    private int maskedSlot(TIntSet mask, int value, int slot) {
        if (value == KmTargetConstants.nullArgMarker) {
            return value;
        }
        return mask.contains(slot) ? value : KmTargetConstants.otherMarker;
    }

    public static MooneyEventRepre fromString(String s) {
        String[] groups = s.split("\\(");
        String predicate = groups[0];
        String[] arg = groups[1].substring(0, groups[1].length() - 1).split(",");


        if (predicate.equals(KmTargetConstants.clozeBlankIndicator)) {
            return new MooneyEventRepre(Integer.parseInt(arg[0]), Integer.parseInt(arg[1]), Integer.parseInt(arg[2]));
        } else {
            return new MooneyEventRepre(predicate, Integer.parseInt(arg[0]), Integer.parseInt(arg[1]), Integer.parseInt(arg[2]));
        }
    }

    /**
     * Note that the toPredicte is not preserved
     *
     * @return
     */
    public Fun.Tuple4<String, Integer, Integer, Integer> toTuple() {
        return new Fun.Tuple4<>(predicate, arg0, arg1, arg2);
    }

    public static MooneyEventRepre fromTuple(Fun.Tuple4<String, Integer, Integer, Integer> tuple) {
        return new MooneyEventRepre(tuple.a, tuple.b, tuple.c, tuple.d);
    }

}
