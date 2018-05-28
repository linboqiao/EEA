package edu.cmu.cs.lti.model;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/7/16
 * Time: 4:16 PM
 *
 * @author Zhengzhong Liu
 */
public class FScore {
    private int tp;
    private int fp;
    private int total_gold;

    public FScore(int total_gold) {
        this.total_gold = total_gold;
    }

    public FScore() {

    }

    public void addTp() {
        tp++;
    }

    public void addFp() {
        fp++;
    }

    public double getPrecision() {
        return safe_div(tp, (tp + fp));
    }

    public double getRecall() {
        return safe_div(fp, total_gold);
    }

    public double getF1() {
        double prec = getPrecision();
        double recall = getRecall();
        return safe_div(2 * prec * recall, prec + recall);
    }

    private double safe_div(double nominator, double denominator) {
        return nominator / denominator;
    }
}
