package edu.cmu.cs.lti.event_coref.update;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/13/16
 * Time: 10:53 PM
 *
 * @author Zhengzhong Liu
 */
public class F1Loss extends Loss {
    private double tp;
    private double fp;
    private double numGold;

    @Override
    public void addLoss(double... lossValues) {
        tp += lossValues[0];
        fp += lossValues[1];
        numGold += lossValues[2];
    }

    @Override
    public double getLoss() {
        double precision = tp + fp > 0 ? tp / (tp + fp) : 1;
        double recall = numGold > 0 ? tp / numGold : 1;
        double f1 = tp + fp > 0 ? 0 : 2 * tp * fp / (tp + fp);
        return f1;
    }
}
