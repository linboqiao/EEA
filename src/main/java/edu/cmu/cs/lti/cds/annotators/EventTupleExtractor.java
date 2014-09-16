package edu.cmu.cs.lti.cds.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.util.JCasUtil;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.script.type.FanseDependencyRelation;
import edu.cmu.cs.lti.script.type.FanseSemanticRelation;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;

/**
 * An annotator that uses possible resources to detect tuples of event and arguments. Mostly depends
 * on semantic parsing, and with the combination of some rules. For nominal events which lack of
 * semantic role labeling, some rules are use, similar but richer than the Stanford system method.
 * 
 * @author Zhengzhong Liu, Hector
 * 
 */
public class EventTupleExtractor extends JCasAnnotator_ImplBase {
  Set<String> semanticSet = new HashSet<String>();

  Set<String> dependencySet = new HashSet<String>();

  private static final String ANNOTATOR_COMPONENT_ID = EventTupleExtractor.class.getSimpleName();

  private Map<Word, Map<String, Word>> events;

  private Map<Span, EntityMention> head2EntityMention;

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
  }

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    System.out
            .println(String.format("Processing article: %s with [%s]", UimaConvenience
                    .getShortDocumentNameWithOffset(aJCas), this.getClass().getSimpleName()));

    events = new HashMap<Word, Map<String, Word>>();

    head2EntityMention = new HashMap<Span, EntityMention>();
    for (EntityMention mention : JCasUtil.select(aJCas, EntityMention.class)) {
      head2EntityMention.put(toSpan(mention.getHead()), mention);
    }

    // step 1.1: use Fanse Semantic Annotations and Syntactic Annotations to find agent, patient for
    // verb based events
    for (FanseSemanticRelation fsr : JCasUtil.select(aJCas, FanseSemanticRelation.class)) {
      saveArgument(fsr);
    }

    for (Entry<Word, Map<String, Word>> eventEntry : events.entrySet()) {
      System.out.println("Event " + eventEntry.getKey().getCoveredText());
      for (Entry<String, Word> argumentEntry : eventEntry.getValue().entrySet()) {
        System.out.println("Argument " + argumentEntry.getKey() + " : "
                + argumentEntry.getValue().getCoveredText());
      }

      Word eventWord = eventEntry.getKey();
      EventMention evm = new EventMention(aJCas, eventWord.getBegin(), eventWord.getEnd());
      UimaAnnotationUtils.finishAnnotation(evm, ANNOTATOR_COMPONENT_ID, null, aJCas);
      List<EventMentionArgumentLink> argumentLinks = new ArrayList<EventMentionArgumentLink>();

      for (Entry<String, Word> argumentEntry : eventEntry.getValue().entrySet()) {
        System.out.println("Argument " + argumentEntry.getKey() + " : "
                + argumentEntry.getValue().getCoveredText());

        EventMentionArgumentLink link = new EventMentionArgumentLink(aJCas);
        UimaAnnotationUtils.finishTop(link, ANNOTATOR_COMPONENT_ID, null, aJCas);

        link.setArgument(getOrCreateEntityMention(aJCas, argumentEntry.getValue()));
        link.setArgumentRole(argumentEntry.getKey());
        argumentLinks.add(link);
      }
      evm.setArguments(FSCollectionFactory.createFSList(aJCas, argumentLinks));
    }

    // for (EventMention evm : allEvents) {
    // List<FanseToken> tokensInEvent = JCasUtil.selectCovered(FanseToken.class, evm);
    //
    // ArrayList<EntityBasedComponentLink> agentLinks = new ArrayList<EntityBasedComponentLink>();
    // ArrayList<EntityBasedComponentLink> patientLinks = new ArrayList<EntityBasedComponentLink>();
    //
    // for (FanseToken token : tokensInEvent) {
    // Span eventTokenSpan = new Span(token.getBegin(), token.getEnd());
    // if (token2AgentHead.containsKey(eventTokenSpan)) {
    // ComponentAnnotation headWordToken = token2AgentHead.get(eventTokenSpan);
    // Word headWord = anyToken2Word(headWordToken);
    //
    // System.out.println(headWordToken.getCoveredText() + " " + headWordToken.getBegin() + " "
    // + headWordToken.getEnd());
    //
    // if (headWord != null) {
    // System.out.println(headWord.getCoveredText());
    // }
    // Span span = getNonOverlappingNPSpan(headWord, evm, word2NPChunkIndex);
    //
    // EntityBasedComponent agent = getOrCreateEntityComponent(aJCas, span.getBegin(),
    // span.getEnd(), headWord);
    //
    // addWord2EntityIndex(agent);
    // agentLinks.add(APLUtils.createLink(aJCas, evm, agent, APLUtils.AGENT_LINK_TYPE,
    // ANNOTATOR_COMPONENT_ID + "_Fanse"));
    //
    // }
    //
    // if (token2PatientHead.containsKey(eventTokenSpan)) {
    // ComponentAnnotation headWordToken = token2PatientHead.get(eventTokenSpan);
    //
    // Word headWord = anyToken2Word(headWordToken);
    //
    // Span span = getNonOverlappingNPSpan(headWord, evm, word2NPChunkIndex);
    // EntityBasedComponent patientLeftToAnd = getOrCreateEntityComponent(aJCas,
    // span.getBegin(), span.getEnd(), headWord);
    //
    // addWord2EntityIndex(patientLeftToAnd);
    // patientLinks.add(APLUtils.createLink(aJCas, evm, patientLeftToAnd,
    // APLUtils.PATIENT_LINK_TYPE, ANNOTATOR_COMPONENT_ID + "_Fanse"));
    // }
    // // }
    // // }
    // // }
    // evm.setAgentLinks(FSCollectionFactory.createFSList(aJCas, agentLinks));
    // evm.setPatientLinks(FSCollectionFactory.createFSList(aJCas, patientLinks));
    // }
  }

  private Word findObjectConnectedWithPrep(Word agentToken) {
    if (agentToken.getPos().equals("IN")) {
      FSList childDependenciesFS = agentToken.getChildDependencyRelations();

      if (childDependenciesFS == null) {
        return agentToken;
      }

      for (FanseDependencyRelation childDependency : FSCollectionFactory.create(
              childDependenciesFS, FanseDependencyRelation.class)) {
        String relationType = childDependency.getDependencyType();
        if (relationType.equals("pobj")) {
          agentToken = childDependency.getChild();
        }
      }
    }
    return agentToken;
  }

  private Span toSpan(ComponentAnnotation anno) {
    return new Span(anno.getBegin(), anno.getEnd());
  }

  private void addEventArgumentPair(Word eventToken, Word argumentToken, String semanticRole) {
    if (!events.containsKey(eventToken)) {
      events.put(eventToken, new HashMap<String, Word>());
    }
    events.get(eventToken).put(semanticRole, argumentToken);
  }

  private void saveArgument(FanseSemanticRelation fsr) {
    String relation = fsr.getSemanticAnnotation();
    String invertedSign = "-INVERTED";

    Word headToken;
    Word argumentToken;

    if (relation.endsWith(invertedSign)) {
      relation = relation.substring(0, relation.length() - invertedSign.length());
      headToken = fsr.getChild();
      argumentToken = fsr.getHead();
    } else {
      headToken = fsr.getHead();
      argumentToken = fsr.getChild();
    }

    if (relation.equals("ARG0") || relation.equals("ARG1") || relation.equals("ARGM-LOC")
            || relation.equals("ARGM-TMP")) {
      if (argumentToken.getPos().equals("IN")) {
        argumentToken = findObjectConnectedWithPrep(argumentToken);
      }
    }

    if (!Pattern.matches("\\p{Punct}", argumentToken.getCoveredText())) {
      addEventArgumentPair(headToken, argumentToken, relation);
    }
  }

  private EntityMention getOrCreateEntityMention(JCas jcas, Word headWord) {
    EntityMention mention = head2EntityMention.get(toSpan(headWord));
    if (mention == null) {
      mention = new EntityMention(jcas, headWord.getBegin(), headWord.getEnd());
      UimaAnnotationUtils.finishAnnotation(mention, ANNOTATOR_COMPONENT_ID, null, jcas);
    }
    return mention;
  }
}
