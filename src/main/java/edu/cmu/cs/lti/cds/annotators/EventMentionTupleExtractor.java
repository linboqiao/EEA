package edu.cmu.cs.lti.cds.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

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
public class EventMentionTupleExtractor extends AbstractEntityMentionCreator {
    Set<String> semanticSet = new HashSet<String>();

    Set<String> dependencySet = new HashSet<String>();

    private static final String COMPONENT_ID = EventMentionTupleExtractor.class
            .getSimpleName();

    private Map<Word, ArrayListMultimap<String, Pair<Word, Word>>> eventWithSemanticRolesAndPrep;

//    private Map<Span, EntityMention> head2EntityMention;
//
//    private int numEntityMentions;
//
//    private int numEntities;

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
    public String getComponentId() {
        return COMPONENT_ID;
    }

    @Override
    public void subprocess(JCas aJCas) {
        logger.info(progressInfo(aJCas));

        eventWithSemanticRolesAndPrep = new HashMap<>();

//        head2EntityMention = new HashMap<>();
//        Collection<EntityMention> entityMentions = JCasUtil.select(aJCas, EntityMention.class);
//        for (EntityMention mention : entityMentions) {
//            head2EntityMention.put(Utils.toSpan(mention.getHead()), mention);
//        }
//        numEntityMentions = entityMentions.size();
//        numEntities = JCasUtil.select(aJCas, Entity.class).size();

        for (FanseSemanticRelation fsr : JCasUtil.select(aJCas, FanseSemanticRelation.class)) {
            saveArgument(fsr);
        }

        for (Entry<Word, ArrayListMultimap<String, Pair<Word, Word>>> eventEntry : eventWithSemanticRolesAndPrep.entrySet()) {
            Word eventWord = eventEntry.getKey();
            EventMention evm = new EventMention(aJCas, eventWord.getBegin(), eventWord.getEnd());
            evm.setHeadWord(eventWord);
            UimaAnnotationUtils.finishAnnotation(evm, COMPONENT_ID, null, aJCas);
            List<EventMentionArgumentLink> argumentLinks = new ArrayList<>();

            ArrayListMultimap<String, Pair<Word, Word>> role2Aruguments = eventEntry.getValue();
            for (String argumentRole : role2Aruguments.keySet()) {
                //do not add arguments that is not about entities
                if (!nonEntityModifiers.contains(argumentRole)) {
                    List<Pair<Word, Word>> potentialLinks = role2Aruguments.get(argumentRole);
                    for (Pair<Word, Word> potentialLink : potentialLinks) {
                        argumentLinks.add(createArgumentLink(aJCas, evm, argumentRole, potentialLink.getLeft(), potentialLink.getRight()));
                    }
                }
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

    private void addEventArgumentPair(Word eventToken, Word argumentToken, String semanticRole, Word prepToken) {
        if (!eventWithSemanticRolesAndPrep.containsKey(eventToken)) {
            ArrayListMultimap<String, Pair<Word, Word>> multimap = ArrayListMultimap.create();
            eventWithSemanticRolesAndPrep.put(eventToken, multimap);
        }
        eventWithSemanticRolesAndPrep.get(eventToken).put(semanticRole, Pair.of(argumentToken, prepToken));
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

            //TODO Fanse parser sometimes messed up VBN and VBD, which leads to incorrect semantic parsing here

        } else {
            headToken = fsr.getHead();
            argumentToken = fsr.getChild();
        }

//        if (nonEntityModifiers.contains(relation)) {
//            return;
//        }

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

//    private EventMentionArgumentLink createArgumentLink(JCas aJCas, EventMention evm, String roleName, Word argument, Word prep) {
//        EventMentionArgumentLink link = new EventMentionArgumentLink(aJCas);
//        EntityMention argumentEntity = getOrCreateSingletonEntityMention(aJCas, argument);
//        link.setVerbPreposition(prep);
//        link.setArgument(argumentEntity);
//        link.setArgumentRole(roleName);
//        link.setEventMention(evm);
//        UimaAnnotationUtils.finishTop(link, COMPONENT_ID, null, aJCas);
//        argumentEntity.setArgumentLinks(UimaConvenience.appendFSList(aJCas, argumentEntity.getArgumentLinks(), link, EventMentionArgumentLink.class));
//        return link;
//    }
//
//    private EntityMention getOrCreateSingletonEntityMention(JCas jcas, Word headWord) {
//        EntityMention mention = head2EntityMention.get(Utils.toSpan(headWord));
//        if (mention == null) {
//            mention = UimaNlpUtils.createEntityMention(jcas, headWord.getBegin(), headWord.getEnd(),
//                    COMPONENT_ID);
//            Entity entity = new Entity(jcas);
//            entity.setEntityMentions(new FSArray(jcas, 1));
//            entity.setEntityMentions(0, mention);
//            entity.setRepresentativeMention(mention);
//            mention.setReferingEntity(entity);
//            UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, null, jcas);
//            head2EntityMention.put(Utils.toSpan(headWord), mention);
//        }
//        return mention;
//    }
}