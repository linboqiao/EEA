package edu.cmu.cs.lti.ark.pipeline.parsing;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/24/15
 * Time: 1:30 PM
 */
public class ParsingException extends Exception {
    public ParsingException(String message) {
        super(message);
    }

    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }

}
