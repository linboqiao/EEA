package edu.cmu.cs.lti.ark.pipeline.parsing;

import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/24/15
 * Time: 1:16 PM
 */
public interface ParserImpl {
    public Sentence parse(Sentence input) throws ParsingException;
}
