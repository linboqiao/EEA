package edu.cmu.cs.lti.cds.annotators.validators;

import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

public class EntityHeadValidator extends AbstractLoggingAnnotator {

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for (EntityMention mention : JCasUtil.select(aJCas, EntityMention.class)) {
            Entity entity = mention.getReferingEntity();

            if (entity == null) {
                logger.info(progressInfo(aJCas));
                logger.info("Null entity for [" + mention.getCoveredText() + "] " + mention.getComponentId() + " " + mention.getBegin() + " " + mention.getEnd());
            } else if (entity.getRepresentativeMention() == null) {
                logger.info("Entity does not have representative mention " + mention.getCoveredText() + " " + mention.getId());
            }
        }
    }
}