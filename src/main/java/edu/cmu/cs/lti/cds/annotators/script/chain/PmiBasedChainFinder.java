package edu.cmu.cs.lti.cds.annotators.script.chain;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/28/14
 * Time: 2:50 PM
 */
public class PmiBasedChainFinder extends AbstractLoggingAnnotator {

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Collection<EventMention> allMentions = JCasUtil.select(aJCas, EventMention.class);


    }


    private void filterMentions(Collection<EventMention> allMentions, JCas aJCas) {
        TokenAlignmentHelper alignmentHelper = new TokenAlignmentHelper();
        alignmentHelper.loadWord2Stanford(aJCas);



        for (EventMention mention : allMentions) {
            String predicate = alignmentHelper.getLowercaseWordLemma(mention.getHeadWord());
        }



    }
}
