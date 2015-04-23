package edu.cmu.cs.lti.script;

import edu.cmu.cs.lti.script.annotators.AbstractEntityMentionCreator;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/22/15
 * Time: 6:23 PM
 *
 * @author Zhengzhong Liu
 */
public class SyntacticBasedEventMentionTupleExtractor extends AbstractEntityMentionCreator{
    @Override
    public String getComponentId() {
        return SyntacticBasedEventMentionTupleExtractor.class.getSimpleName();
    }

    @Override
    public void subprocess(JCas aJCas) {

    }
}
