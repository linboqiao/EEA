package edu.cmu.cs.lti.cds.annotators;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * An annotator that uses possible resources to detect tuples of event and arguments. Mostly depends
 * on semantic parsing, and with the combination of some rules. For nominal eventWithSemanticRolesAndPrep which lack of
 * semantic role labeling, some rules are use, similar but richer than the Stanford system method.
 *
 * @author Zhengzhong Liu, Hector
 */
public class EventMentionTupleExtractor extends AbstractLoggingAnnotator {
    Set<String> semanticSet = new HashSet<String>();

    Set<String> dependencySet = new HashSet<String>();

    private static final String ANNOTATOR_COMPONENT_ID = EventMentionTupleExtractor.class
            .getSimpleName();

    private Map<Word, Map<String, Pair<Word, Word>>> eventWithSemanticRolesAndPrep;

    private Map<Span, EntityMention> head2EntityMention;

    private int numEntityMentions;

    private int numEntities;

    //TODO: More need to be done to find the actual role from prep
    public static final Set<String> nonPrepRoles = new HashSet<String>(Arrays.asList("ARG0", "ARG1",
            "ARGM-LOC", "ARGM-TMP", "ARGM-PNC"));

    public static final Set<String> nonEntityModifiers = new HashSet<>(Arrays.asList(
            "ARGM-MNR",
            "ARGM-ADV",
            "ARGM-DIS",
            "ARGM-MOD",
            "ARGM-NEG",
            "ARGM-ADJ"));


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        eventWithSemanticRolesAndPrep = new HashMap<Word, Map<String, Pair<Word, Word>>>();

        head2EntityMention = new HashMap<Span, EntityMention>();
        Collection<EntityMention> entityMentions = JCasUtil.select(aJCas, EntityMention.class);
        for (EntityMention mention : entityMentions) {
            head2EntityMention.put(toSpan(mention.getHead()), mention);
        }
        numEntityMentions = entityMentions.size();
        numEntities = JCasUtil.select(aJCas, Entity.class).size();

        // step 1.1: use Fanse Semantic Annotations and Syntactic Annotations to find agent, patient for
        // verb based eventWithSemanticRolesAndPrep
        for (FanseSemanticRelation fsr : JCasUtil.select(aJCas, FanseSemanticRelation.class)) {
            saveArgument(fsr);
        }

        for (Entry<Word, Map<String, Pair<Word, Word>>> eventEntry : eventWithSemanticRolesAndPrep.entrySet()) {
            Word eventWord = eventEntry.getKey();
            EventMention evm = new EventMention(aJCas, eventWord.getBegin(), eventWord.getEnd());
            evm.setHeadWord(eventWord);
            UimaAnnotationUtils.finishAnnotation(evm, ANNOTATOR_COMPONENT_ID, null, aJCas);
            List<EventMentionArgumentLink> argumentLinks = new ArrayList<EventMentionArgumentLink>();

            for (Entry<String, Pair<Word, Word>> argumentEntry : eventEntry.getValue().entrySet()) {
                EventMentionArgumentLink link = new EventMentionArgumentLink(aJCas);
                UimaAnnotationUtils.finishTop(link, ANNOTATOR_COMPONENT_ID, null, aJCas);

                Pair<Word, Word> argumentWithPrep = argumentEntry.getValue();

                link.setVerbPreposition(argumentWithPrep.getValue());
                link.setArgument(getOrCreateEntityMention(aJCas, argumentWithPrep.getKey()));
                link.setArgumentRole(argumentEntry.getKey());
                link.setEventMention(evm);
                argumentLinks.add(link);
            }
            evm.setArguments(FSCollectionFactory.createFSList(aJCas, argumentLinks));
        }
    }

    private Word findObjectConnectedWithPrep(Word argumentToken) {
        FSList childDependenciesFS = argumentToken.getChildDependencyRelations();

        if (childDependenciesFS == null) {
            return argumentToken;
        }

        for (FanseDependencyRelation childDependency : FSCollectionFactory.create(
                childDependenciesFS, FanseDependencyRelation.class)) {
            String relationType = childDependency.getDependencyType();
            if (relationType.equals("pobj")) {
                argumentToken = childDependency.getChild();
                break;
            }
            argumentToken = childDependency.getChild();
        }

        return argumentToken;
    }

    private Span toSpan(ComponentAnnotation anno) {
        return new Span(anno.getBegin(), anno.getEnd());
    }

    private void addEventArgumentPair(Word eventToken, Word argumentToken, String semanticRole, Word prepToken) {
        if (!eventWithSemanticRolesAndPrep.containsKey(eventToken)) {
            eventWithSemanticRolesAndPrep.put(eventToken, new HashMap<String, Pair<Word,Word>>());
        }
        eventWithSemanticRolesAndPrep.get(eventToken).put(semanticRole, Pair.of(argumentToken,prepToken));
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


        if (nonEntityModifiers.contains(relation)){
            //do not add non entity ArgM s as real arguments.
            return;
        }


            Word verbPrep = null;

        //check if we need to transfer to the pobj through preposition
        if (argumentToken.getPos().equals("IN") || argumentToken.getPos().equals("TO")) {
            verbPrep = argumentToken;
            argumentToken = findObjectConnectedWithPrep(argumentToken);
        }

        if (!Pattern.matches("\\p{Punct}", argumentToken.getCoveredText())) {
            addEventArgumentPair(headToken, argumentToken, relation, verbPrep);
        }
    }

    private EntityMention getOrCreateEntityMention(JCas jcas, Word headWord) {
        EntityMention mention = head2EntityMention.get(toSpan(headWord));
        if (mention == null) {
            mention = UimaNlpUtils.createEntityMention(jcas, headWord.getBegin(), headWord.getEnd(),
                    ANNOTATOR_COMPONENT_ID);
            head2EntityMention.put(toSpan(headWord), mention);
        }
        return mention;
    }
}