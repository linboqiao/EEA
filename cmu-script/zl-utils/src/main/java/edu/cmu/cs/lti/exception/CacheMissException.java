package edu.cmu.cs.lti.exception;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/19/15
 * Time: 4:02 PM
 *
 * @author Zhengzhong Liu
 */
public class CacheMissException extends Exception {
    private static final long serialVersionUID = -3601520863321982660L;

    public CacheMissException(String message) {
        super(message);
    }
}
