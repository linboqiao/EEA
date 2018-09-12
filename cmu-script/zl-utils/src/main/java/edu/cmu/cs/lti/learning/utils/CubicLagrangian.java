package edu.cmu.cs.lti.learning.utils;

/**
 * A 3-dimension lagrangian variable.
 *
 * @author Zhengzhong Liu
 */
public class CubicLagrangian {
    // Shape : i, j, t
    private double[][][] variables;

    // Shape : i, j
    private double[][] varSumOverT;

    // Shape : j, t
    private double[][] varSumOverI;

    // Shape : i, t
    private double[][] varSumOverJ;

    private int dI;
    private int dJ;
    private int dT;

    public CubicLagrangian(int dimensionI, int dimensionJ, int dimensionT) {
        variables = new double[dimensionI][dimensionJ][dimensionT];
        this.dI = dimensionI;
        this.dJ = dimensionJ;
        this.dT = dimensionT;
    }

    public double getVariable(int i, int j, int t) {
        return variables[i][j][t];
    }

    public double getSumOverTVariable(int i, int j) {
        return varSumOverT[i][j];
    }

    public double getSumOverJVariable(int i, int t) {
        return varSumOverJ[i][t];
    }

    public double getSumOverIVariable(int j, int t) {
        return varSumOverI[j][t];
    }

    public void update(int i, int j, int t, double updateValue) {
        variables[i][j][t] += updateValue;
    }

    public void projectedUpdate(int i, int j, int t, double updateValue) {
        double oldValue = variables[i][j][t];
        double newValue = oldValue + updateValue;
        variables[i][j][t] = newValue < 0 ? newValue : 0;
    }

    public void computeSum() {
        sumOverI();
        sumOverT();
        sumOverJ();
    }

    public void sumOverT() {
        varSumOverT = new double[dI][dJ];
        for (int i = 0; i < dI; i++) {
            for (int j = 0; j < dJ; j++) {
                for (int t = 0; t < dT; t++) {
                    varSumOverT[i][j] += variables[i][j][t];
                }
            }
        }
    }

    public void sumOverI() {
        varSumOverI = new double[dJ][dT];
        for (int i = 0; i < dI; i++) {
            for (int j = 0; j < dJ; j++) {
                for (int t = 0; t < dT; t++) {
                    varSumOverI[j][t] += variables[i][j][t];
                }
            }
        }
    }

    public void sumOverJ() {
        varSumOverJ = new double[dI][dT];
        for (int i = 0; i < dI; i++) {
            for (int j = 0; j < dJ; j++) {
                for (int t = 0; t < dT; t++) {
                    varSumOverJ[i][t] += variables[i][j][t];
                }
            }
        }
    }
}
