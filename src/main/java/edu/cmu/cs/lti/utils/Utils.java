package edu.cmu.cs.lti.utils;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/14/14
 * Time: 9:03 PM
 */
public class Utils {
    public static Span toSpan(ComponentAnnotation anno) {
        return new Span(anno.getBegin(), anno.getEnd());
    }
}
