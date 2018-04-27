package edu.cmu.cs.lti.exceptions;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/3/16
 * Time: 5:33 PM
 *
 * @author Zhengzhong Liu
 */
public class NotImplementedException extends RuntimeException {
    private static final long serialVersionUID = -131123902841441104L;

    public NotImplementedException(String message) {
        super(message);
    }
}
