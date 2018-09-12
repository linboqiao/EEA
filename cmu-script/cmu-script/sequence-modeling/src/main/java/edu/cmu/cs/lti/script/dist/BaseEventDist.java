package edu.cmu.cs.lti.script.dist;

import edu.cmu.cs.lti.script.model.LocalArgumentRepre;
import edu.cmu.cs.lti.script.model.LocalEventMentionRepre;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/24/15
 * Time: 12:25 AM
 */
public abstract class BaseEventDist {
    int numArguments;

    public BaseEventDist() {

    }

    public BaseEventDist(int numArguments) {
        this.numArguments = numArguments;
    }

    public abstract Pair<LocalEventMentionRepre, Double> draw(List<LocalArgumentRepre> candidateArguments);
}
