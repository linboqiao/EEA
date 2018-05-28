package edu.cmu.cs.lti.model;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/13/14
 * Time: 12:22 AM
 */
public class MutableDouble implements Serializable {
    private static final long serialVersionUID = -3206704535452338294L;
    double value = 0;

    public MutableDouble(double init) {
        value = init;
    }

    public double increment(double amount) {
        value += amount;
        return value;
    }

    public double multiply(double by) {
        value *= by;
        return by;
    }

    public double get() {
        return value;
    }

    public String toString() {
        return Double.toString(value);
    }

    public String format(String format) {
        return String.format(format, value);
    }
}