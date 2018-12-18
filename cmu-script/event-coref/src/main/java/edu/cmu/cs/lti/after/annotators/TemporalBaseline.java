package edu.cmu.cs.lti.after.annotators;

import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.timeml.MentionLink;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

/**
 * A baseline that directly copy temporal relations.
 *
 * @author Zhengzhong Liu
 */
public class TemporalBaseline extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        List<EventMentionRelation> originalRelations = UimaConvenience.getAnnotationList(aJCas,
                EventMentionRelation.class);

        for (EventMentionRelation originalRelation : originalRelations) {
            originalRelation.removeFromIndexes();
        }

        for (MentionLink link : JCasUtil.select(aJCas, MentionLink.class)) {
            createLink(aJCas, link.getSource(), link.getTarget(), link.getRelationType());
        }
    }

    private void createLink(JCas aJCas, EventMention from, EventMention to, String timemlType) {
        if (timemlType.equals("AFTER")) {
            createAfterLink(aJCas, to, from);
        } else if (timemlType.equals("BEFORE")) {
            createAfterLink(aJCas, from, to);
        }
    }

    private void createAfterLink(JCas aJCas, EventMention from, EventMention to) {
        EventMentionRelation afterLink = new EventMentionRelation(aJCas);
        afterLink.setHead(from);
        afterLink.setChild(to);
        afterLink.setRelationType("After");
        UimaAnnotationUtils.finishTop(afterLink, COMPONENT_ID, 0, aJCas);
    }

    public static CollectionReaderDescription run(CollectionReaderDescription reader,
                                                  TypeSystemDescription typeSystemDescription,
                                                  String mainDir, String baseOutput)
            throws SAXException, UIMAException, CpeDescriptorException, IOException {
        AnalysisEngineDescription annotator = AnalysisEngineFactory.createEngineDescription(TemporalBaseline
                .class, typeSystemDescription);
        return new BasicPipeline(reader, mainDir, baseOutput, annotator).run().getOutput();
    }
}
