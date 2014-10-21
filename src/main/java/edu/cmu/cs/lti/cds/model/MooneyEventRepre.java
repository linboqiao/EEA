package edu.cmu.cs.lti.cds.model;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/20/14
 * Time: 5:02 PM
 */
public class MooneyEventRepre implements Serializable {
    final String predicate;
    final int arg0;
    final int arg1;
    final int arg2;

    public static final int nullArgMarker = -1;
    public static final int firstArg0Marker = 1;
    public static final int firstArg1Marker = 2;
    public static final int firstArg2Marker = 3;
    public static final int otherMarker = 0;

    public MooneyEventRepre(String p, int a0, int a1, int a2) {
        super();
        this.predicate = p;
        this.arg0 = a0;
        this.arg1 = a1;
        this.arg2 = a2;
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
}
