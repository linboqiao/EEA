package edu.cmu.cs.lti.event_coref.annotators;

import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/8/15
 * Time: 6:03 PM
 *
 * @author Zhengzhong Liu
 */
public class AllInOneBaselineCorefAnnotator extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Event singleCluster = new Event(aJCas);
        FSArray allMentoinArray = FSCollectionFactory.createFSArray(aJCas, JCasUtil.select(aJCas, EventMention.class));
        singleCluster.setEventMentions(allMentoinArray);
        UimaAnnotationUtils.finishTop(singleCluster, COMPONENT_ID, 0, aJCas);
    }
}
