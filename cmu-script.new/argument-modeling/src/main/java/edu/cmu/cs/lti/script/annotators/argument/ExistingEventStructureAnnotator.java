package edu.cmu.cs.lti.script.annotators.argument;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.HashMap;
import java.util.Map;

/**
 * To work with pre-annotated dataset, we need to combine the automatic annotations and gold annotations. This
 * inludes the following steps:
 * <p>
 * Prerequisite:
 * 1. SRL annotation (Semafor and ArgumentMerger).
 * 2. Stanford CoreNLP annotation.
 * <p>
 * 1. Combine the gold entity mentions with the stanford clusters to create appropriate entities.
 * 2. Add frame labels to events.
 * 3. Make sure headwords are attached.
 * <p>
 * Date: 10/23/18
 * Time: 11:24 AM
 *
 * @author Zhengzhong Liu
 */
public class ExistingEventStructureAnnotator extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, true);
        handleEntities(aJCas, goldView);
        handleEvents(aJCas, goldView);
    }

    private void handleEvents(JCas aJCas, JCas goldView) {
        for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
            EventMention sysMention = new EventMention(aJCas);
            StanfordCorenlpToken headword = UimaNlpUtils.findHeadFromStanfordAnnotation(sysMention);
            sysMention.setFrameName(headword.getFrameName());
            sysMention.setHeadWord(headword);
            UimaAnnotationUtils.finishAnnotation(sysMention, mention.getBegin(), mention.getEnd(), COMPONENT_ID,
                    mention.getId(), aJCas);
        }
    }

    private void handleEntities(JCas aJCas, JCas goldView) {
        Map<Span, EntityMention> span2SystemEnt = new HashMap<>();

        for (EntityMention mention : JCasUtil.select(aJCas, EntityMention.class)) {
            span2SystemEnt.put(Span.of(mention.getBegin(), mention.getEnd()), mention);
        }

        for (EntityMention goldEntityMention : JCasUtil.select(goldView, EntityMention.class)) {
            Span goldSpan = Span.of(goldEntityMention.getBegin(), goldEntityMention.getEnd());

            Map<String, String> goldMeta = UimaAnnotationUtils.readMeta(goldEntityMention);

            if (span2SystemEnt.containsKey(goldSpan)) {
                EntityMention systemEnt = span2SystemEnt.get(goldSpan);
                UimaAnnotationUtils.addMeta(aJCas, systemEnt, "isGold", "true");

                for (Map.Entry<String, String> meta : goldMeta.entrySet()) {
                    UimaAnnotationUtils.addMeta(aJCas, systemEnt, meta.getKey(), meta.getValue());
                }
            } else {
                EntityMention mention = new EntityMention(aJCas);
                UimaAnnotationUtils.addMeta(aJCas, mention, "isGold", "true");
                for (Map.Entry<String, String> meta : goldMeta.entrySet()) {
                    UimaAnnotationUtils.addMeta(aJCas, mention, meta.getKey(), meta.getValue());
                }
                mention.setHead(UimaNlpUtils.findHeadFromStanfordAnnotation(mention));

                UimaAnnotationUtils.finishAnnotation(mention, goldEntityMention.getBegin(),
                        goldEntityMention.getEnd(), COMPONENT_ID, mention.getId(), aJCas);
            }
        }
    }
}
