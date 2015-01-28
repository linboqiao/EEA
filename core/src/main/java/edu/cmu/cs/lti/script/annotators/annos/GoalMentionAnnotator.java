package edu.cmu.cs.lti.script.annotators.annos;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/17/14
 * Time: 4:54 PM
 */
public class GoalMentionAnnotator extends AbstractLoggingAnnotator {
    private Set<String> goalIndicatingDep = new HashSet<>(Arrays.asList("purpcl"));

    public final String COMPONENT_ID = GoalMentionAnnotator.class.getSimpleName();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        ArrayListMultimap<Word, Pair<Word, String>> verbGoalPairs = findVerbGoalPairs(aJCas);

//        System.out.println("Found pairs "+verbGoalPairs.size());

        Map<Span, StanfordTreeAnnotation> head2MaxTrees = getHeadSpan2MaxTrees(aJCas);
        for (EventMention evm : JCasUtil.select(aJCas, EventMention.class)) {
            Word evmHead = evm.getHeadWord();
            if (verbGoalPairs.containsKey(evmHead)) {
//                System.out.println("Find goal for event "+evmHead.getCoveredText());
                for (Pair<Word, String> goalHeadDepPair : verbGoalPairs.get(evmHead)) {
                    Word goalHead = goalHeadDepPair.getKey();
                    Span goalHeadSpan = UimaAnnotationUtils.toSpan(goalHead);
                    if (head2MaxTrees.containsKey(goalHeadSpan)) {
                        StanfordTreeAnnotation maxTree = head2MaxTrees.get(goalHeadSpan);
                        createGoalMention(aJCas, evm, maxTree.getBegin(), maxTree.getEnd(), goalHead);
                    } else {
                        createGoalMention(aJCas, evm, goalHead.getBegin(), goalHead.getEnd(), goalHead);
                    }
                }
            }
        }
    }

    private void createGoalMention(JCas aJCas, EventMention sourceEvm, int goalBegin, int goalEnd, Word goalHead) {
        GoalMention goalMention = new GoalMention(aJCas);
        UimaAnnotationUtils.finishAnnotation(goalMention, goalBegin, goalEnd, COMPONENT_ID, null, aJCas);
        goalMention.setHead(goalHead);

        EventMentionGoalLink link = new EventMentionGoalLink(aJCas);
        link.setEventMention(sourceEvm);
        link.setGoalMention(goalMention);
        UimaAnnotationUtils.finishTop(link, COMPONENT_ID, null, aJCas);

        goalMention.setEventMentionLinks(UimaConvenience.appendFSList(aJCas, goalMention.getEventMentionLinks(), link, EventMentionGoalLink.class));
        sourceEvm.setGoalLinks(UimaConvenience.appendFSList(aJCas, sourceEvm.getGoalLinks(), link, EventMentionGoalLink.class));
    }

    private Map<Span, StanfordTreeAnnotation> getHeadSpan2MaxTrees(JCas aJCas) {
        Map<Span, StanfordTreeAnnotation> headSpan2MaxTree = new HashMap<>();
        for (StanfordTreeAnnotation treeAnno : JCasUtil.select(aJCas, StanfordTreeAnnotation.class)) {
            Span headSpan = UimaAnnotationUtils.toSpan(treeAnno.getHead());

            if (headSpan2MaxTree.containsKey(headSpan)) {
                StanfordTreeAnnotation oldTreeAnno = headSpan2MaxTree.get(headSpan);
                if (oldTreeAnno.getEnd() - oldTreeAnno.getBegin() < treeAnno.getEnd() - treeAnno.getBegin()) {
                    headSpan2MaxTree.put(headSpan, treeAnno);
                }
            } else {
                headSpan2MaxTree.put(headSpan, treeAnno);
            }
        }

        return headSpan2MaxTree;
    }

    private ArrayListMultimap<Word, Pair<Word, String>> findVerbGoalPairs(JCas aJCas) {
        ArrayListMultimap<Word, Pair<Word, String>> verbGoalPairs = ArrayListMultimap.create();
        //only fanse dependencies have Purpcl
        for (FanseDependencyRelation dep : JCasUtil.select(aJCas, FanseDependencyRelation.class)) {
            if (goalIndicatingDep.contains(dep.getDependencyType())) {
                Word head = dep.getHead();
                Word child = dep.getChild();

                verbGoalPairs.put(head, Pair.of(child, dep.getDependencyType()));
            }
        }
        return verbGoalPairs;
    }

}