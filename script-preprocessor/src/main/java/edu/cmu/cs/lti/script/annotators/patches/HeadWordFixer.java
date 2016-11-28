package edu.cmu.cs.lti.script.annotators.patches;

import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.StanfordCoreNlpUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/16/14
 * Time: 5:09 PM
 */
public class HeadWordFixer extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        for (EntityMention mention : JCasUtil.select(aJCas, EntityMention.class)) {
            mention.setHead(StanfordCoreNlpUtils.fixByDependencyHead(mention, mention.getHead()));
        }
    }
}
