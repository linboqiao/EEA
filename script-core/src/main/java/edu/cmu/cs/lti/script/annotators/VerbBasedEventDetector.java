package edu.cmu.cs.lti.script.annotators;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/1/18
 * Time: 5:14 PM
 *
 * @author Zhengzhong Liu
 */
public class VerbBasedEventDetector extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Set<Word> usedHeads = new HashSet<>();
        Map<Word, EntityMention> h2Entities = UimaNlpUtils.indexEntityMentions(aJCas);

        int eventId = 0;
        for (StanfordCorenlpToken token : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
            if (!token.getPos().startsWith("V")) {
                continue;
            }

            if (usedHeads.contains(token)) {
                continue;
            }

            List<Word> complements = new ArrayList<>();
            UimaNlpUtils.getPredicate(token, complements);
            usedHeads.addAll(complements);

            EventMention eventMention = new EventMention(aJCas, token.getBegin(), token.getEnd());
            eventMention.setHeadWord(token);

            Map<String, Word> args = getArgs(token);

            List<Annotation> regions = new ArrayList<>();
            regions.add(token);

            List<EventMentionArgumentLink> argumentLinks = new ArrayList<>();

            for (Word complement : complements) {
                regions.add(complement);
                Map<String, Word> complement_args = getArgs(complement);
                for (Map.Entry<String, Word> arg : complement_args.entrySet()) {
                    String role = arg.getKey();
                    if (!args.containsKey(role)) {
                        args.put(role, arg.getValue());
                    }
                }
            }

            eventMention.setRegions(new FSArray(aJCas, regions.size()));
            for (int i = 0; i < regions.size(); i++) {
                eventMention.setRegions(i, regions.get(i));
            }

            for (Map.Entry<String, Word> arg : args.entrySet()) {
                String role = arg.getKey();
                Word argWord = arg.getValue();
                EventMentionArgumentLink argumentLink = new EventMentionArgumentLink(aJCas);
                EntityMention argumentMention = UimaNlpUtils.createNonExistEntityMention(aJCas, h2Entities,
                        argWord.getBegin(), argWord.getEnd(), COMPONENT_ID);

                argumentLink.setArgumentRole(role);
                argumentLink.setArgument(argumentMention);
                argumentLink.setEventMention(eventMention);

                UimaAnnotationUtils.finishTop(argumentLink, COMPONENT_ID, 0, aJCas);
                argumentLinks.add(argumentLink);
            }

            eventMention.setArguments(FSCollectionFactory.createFSList(aJCas, argumentLinks));
            UimaAnnotationUtils.finishAnnotation(eventMention, COMPONENT_ID, eventId++, aJCas);
        }

        UimaNlpUtils.createSingletons(aJCas, new ArrayList<>(JCasUtil.select(aJCas, EntityMention.class)),
                COMPONENT_ID);
    }

    private Map<String, Word> getArgs(Word predicate) {
        Map<String, Word> args = new HashMap<>();
        if (predicate.getChildDependencyRelations() != null) {
            for (StanfordDependencyRelation dep : FSCollectionFactory.create(predicate
                    .getChildDependencyRelations(), StanfordDependencyRelation.class)) {
                Pair<String, Word> child = takeDep(dep);

                if (child == null) {
                    continue;
                }
                Word word = child.getRight();
                String role = child.getLeft();
                args.put(role, word);
            }
        }

        return args;
    }

    private Pair<String, Word> takeDep(StanfordDependencyRelation dep) {
        String depType = dep.getDependencyType();
        Word depWord = dep.getChild();
        if (depType.equals("nsubj") || depType.equals("agent")) {
            return Pair.of("subj", depWord);
        } else if (depType.equals("dobj") || depType.equals("nsubjpass")) {
            return Pair.of("dobj", depWord);
        } else if (depType.equals("iobj")) {
            return Pair.of("iobj", depWord);
        } else if (depType.startsWith("prep_")) {
            return Pair.of(depType, depWord);
        }

        return null;
    }

    private Map<Word, Integer> loadCoref(JCas aJCas) {
        Map<Word, Integer> headToCoref = new HashMap<>();

        int corefId = 0;
        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            Collection<EntityMention> mentions = FSCollectionFactory.create(entity
                    .getEntityMentions(), EntityMention.class);

            if (mentions.size() > 1) {
                for (EntityMention mention : mentions) {
                    StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
                    headToCoref.put(head, corefId);
                }
                corefId += 1;
            }
        }
        return headToCoref;
    }
}
