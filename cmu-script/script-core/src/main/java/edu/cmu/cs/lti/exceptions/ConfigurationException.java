package edu.cmu.cs.lti.exceptions;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/25/16
 * Time: 9:29 PM
 *
 * @author Zhengzhong Liu
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = -8983042627642889548L;

    public ConfigurationException(String message) {
        super(message);
    }
}
