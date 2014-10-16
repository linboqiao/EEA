package edu.cmu.cs.lti.cds.annotators.validators;

import edu.cmu.cs.lti.script.type.EntityMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import edu.cmu.cs.lti.script.type.Entity;

public class EntityHeadValidator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
     for (EntityMention mention : JCasUtil.select(aJCas, EntityMention.class)){
         Entity entity = mention.getReferingEntity();
         assert entity!= null;
         assert entity.getRepresentativeMention().getCoveredText() != null;
     }
  }
}