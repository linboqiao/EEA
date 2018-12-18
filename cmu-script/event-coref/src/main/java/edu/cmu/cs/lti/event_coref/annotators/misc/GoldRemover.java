package edu.cmu.cs.lti.event_coref.annotators.misc;

import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/30/16
 * Time: 6:24 PM
 *
 * @author Zhengzhong Liu
 */
public class GoldRemover extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        removeType(aJCas, Event.class);
        removeType(aJCas, EventMention.class);
    }

    private <T extends TOP> void removeType(JCas aJCas, final Class<T> clazz) {

        List<T> items = UimaConvenience.getAnnotationList(aJCas, clazz);

        for (T item : items) {
            item.removeFromIndexes();
        }
    }
}
