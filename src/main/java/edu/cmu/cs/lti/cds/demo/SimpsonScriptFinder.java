package edu.cmu.cs.lti.cds.demo;

import java.util.Collection;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;

public class SimpsonScriptFinder extends JCasAnnotator_ImplBase {

  public static final String PARAM_TARGET_NAME = "targetName";

  @ConfigurationParameter(name = PARAM_TARGET_NAME)
  private String targetName;

  @Override
  public void initialize(final UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    targetName = (String) context.getConfigParameterValue(PARAM_TARGET_NAME);
  }

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    for (EventMention evm : JCasUtil.select(aJCas, EventMention.class)) {
      Collection<EventMentionArgumentLink> argumentLinks = FSCollectionFactory.create(
              evm.getArguments(), EventMentionArgumentLink.class);
      for (EventMentionArgumentLink argument : argumentLinks) {
        EntityMention entityMention = argument.getArgument();
        String role = argument.getArgumentRole();
        if (entityMention.getCoveredText().toLowerCase().contains(targetName)) {
          ScriptCollector.addObservation(aJCas, evm.getReferringEvent(),
                  entityMention.getReferingEntity(), role);
        }
      }
    }
  }
}
