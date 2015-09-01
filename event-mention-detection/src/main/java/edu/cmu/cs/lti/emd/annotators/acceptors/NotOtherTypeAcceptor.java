package edu.cmu.cs.lti.emd.annotators.acceptors;

import edu.cmu.cs.lti.emd.annotators.acceptors.AbstractCandidateAcceptor;
import edu.cmu.cs.lti.emd.annotators.twostep.EventMentionTypeLearner;
import edu.cmu.cs.lti.script.type.CandidateEventMention;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/1/15
 * Time: 5:10 PM
 *
 * @author Zhengzhong Liu
 */
public class NotOtherTypeAcceptor extends AbstractCandidateAcceptor {
    @Override
    protected boolean accept(CandidateEventMention candidate) {
        return candidate.getPredictedType() != null && !candidate.getPredictedType().equals(EventMentionTypeLearner
                .OTHER_TYPE);
    }
}
