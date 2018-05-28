package edu.cmu.cs.lti.learning.utils;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/22/15
 * Time: 5:59 PM
 *
 * @author Zhengzhong Liu
 */
public class DummyCubicLagrangian extends CubicLagrangian {
    public DummyCubicLagrangian() {
        super(0, 0, 0);
    }

    public double getVariable(int i, int j, int t) {
        return 0;
    }

    public double getSumOverTVariable(int i, int j) {
        return 0;
    }

    public double getSumOverJVariable(int i, int t) {
        return 0;
    }

    public double getSumOverIVariable(int j, int t) {
        return 0;
    }

    public void computeSum() {
        sumOverI();
        sumOverT();
        sumOverJ();
    }

    public void sumOverT() {

    }

    public void sumOverI() {

    }

    public void sumOverJ() {

    }

}
