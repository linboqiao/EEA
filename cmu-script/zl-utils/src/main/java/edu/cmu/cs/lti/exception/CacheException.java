package edu.cmu.cs.lti.exception;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/19/15
 * Time: 9:21 PM
 *
 * @author Zhengzhong Liu
 */
public class CacheException extends RuntimeException {
    private static final long serialVersionUID = -391149591722485602L;

    public CacheException(String message) {
        super(message);
    }
}
