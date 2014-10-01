package edu.cmu.cs.lti.cds.annotators.validators;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import edu.cmu.cs.lti.script.type.Entity;

public class EntityHeadValidator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
      System.out.println("Entity " + entity.getId() );
        String headMentionStr = entity.getRepresentativeMention().getCoveredText().replace("\n", "");
        System.out.println(" is represented by " + headMentionStr);
    }
  }
}