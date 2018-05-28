package edu.cmu.cs.lti.learning.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/23/15
 * Time: 3:43 PM
 *
 * @author Zhengzhong Liu
 */
public class TrainingStats {
    private int numInstanceProcessed = 0;

    private int averageLossOverN;

    private double recentAccumulatedLoss = 0;

    private int resetableInstanceCount = 0;

    private String name = "Training";

    private double overallLoss = 0;

    public TrainingStats(int averageLossOverN) {
        this.averageLossOverN = averageLossOverN;
        reset();
    }

    public TrainingStats(int averageLossOverN, String name) {
        this.averageLossOverN = averageLossOverN;
        this.name = name;
        reset();
    }


    public void addLoss(Logger logger, double loss) {
        addLoss(loss);

        if (numInstanceProcessed % averageLossOverN == 0) {
            logger.info(String.format("Average loss of previous %d instance is %.3f for %s", averageLossOverN,
                    recentAccumulatedLoss / averageLossOverN, name));
            recentAccumulatedLoss = 0;
        }
    }

    private void addLoss(double loss) {
        recentAccumulatedLoss += loss;
        overallLoss += loss;
        numInstanceProcessed++;
    }

    public void reset() {
        overallLoss = 0;
        resetableInstanceCount = 0;
    }

    public int getNumberOfInstancesProcessed() {
        return numInstanceProcessed;
    }

    public double getAverageOverallLoss() {
        return overallLoss / resetableInstanceCount;
    }

    public static void main(String[] argv) {
        Logger logger = LoggerFactory.getLogger(TrainingStats.class);
        TrainingStats trainingStats = new TrainingStats(5);

        for (double l = 0.1; l < 1.2; l += 0.1) {
            trainingStats.addLoss(logger, l);
        }
    }
}
