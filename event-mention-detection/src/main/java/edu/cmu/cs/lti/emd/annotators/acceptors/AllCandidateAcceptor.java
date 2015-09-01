package edu.cmu.cs.lti.emd.annotators.acceptors;

import edu.cmu.cs.lti.script.type.CandidateEventMention;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/1/15
 * Time: 5:31 PM
 *
 * @author Zhengzhong Liu
 */
public class AllCandidateAcceptor extends AbstractCandidateAcceptor {
    @Override
    protected boolean accept(CandidateEventMention candidate) {
        return true;
    }
}
