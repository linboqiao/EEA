package edu.cmu.cs.lti.event_coref.update;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/13/16
 * Time: 10:52 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class Loss {
    public abstract void addLoss(double... lossValues);

    public abstract double getLoss();
}
