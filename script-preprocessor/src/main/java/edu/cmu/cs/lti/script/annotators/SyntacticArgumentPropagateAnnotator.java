package edu.cmu.cs.lti.script.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.ling.PennTreeTagSet;
import edu.cmu.cs.lti.ling.PropBankTagSet;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.*;

/**
 * Propagate appropriate argument with several syntactic relations between events and verbs
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/17/14
 * Time: 9:38 PM
 */
public class SyntacticArgumentPropagateAnnotator extends AbstractEntityMentionCreator {
    public static final String COMPONENT_ID = SyntacticArgumentPropagateAnnotator.class.getSimpleName();

    private Set<String> shareSubjDeps = new HashSet<>(Arrays.asList("xcomp", "advcl", "purpcl"));

    private Set<String> completeMarker = new HashSet<>(Arrays.asList("have", "had", "has"));

    //using span to communicate probably is the safest way when two types of tokens exists
    private ArrayListMultimap<Span, Pair<Span, String>> shareSubjVerbPairs;

    //TODO: There is a couple of problems on sharing, 1) all stuff need to be shared at once 2) sharing conflicts should be used to correct old stuff 3)conj might help share more than only Arg0 and Arg1

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        super.process(aJCas);

        logger.info(progressInfo(aJCas));
        findShareSubjVerbs(aJCas);

        ArrayListMultimap<Span, EntityMention> eventHead2Arg0 = ArrayListMultimap.create();
        ArrayListMultimap<Span, EntityMention> eventHead2Arg1 = ArrayListMultimap.create();
        Map<Span, EventMention> emptyAgentMentions = new HashMap<>();
        Map<Span, EventMention> emptyPatientMentions = new HashMap<>();

        for (EventMention evm : JCasUtil.select(aJCas, EventMention.class)) {
            Word evmHead = evm.getHeadWord();

            Set<EventMentionArgumentLink> arg0s = new HashSet<>();
            Set<EventMentionArgumentLink> arg1s = new HashSet<>();

            for (EventMentionArgumentLink link : FSCollectionFactory.create(evm.getArguments(), EventMentionArgumentLink.class)) {
                if (link.getArgumentRole().equals(PropBankTagSet.ARG0)) {
                    arg0s.add(link);
                } else if (link.getArgumentRole().equals(PropBankTagSet.ARG1)) {
                    arg1s.add(link);
                }
            }

            if (arg0s.size() == 0) {
                emptyAgentMentions.put(UimaAnnotationUtils.toSpan(evmHead), evm);
            } else {
                for (EventMentionArgumentLink arg0Link : arg0s) {
                    eventHead2Arg0.put(UimaAnnotationUtils.toSpan(evmHead), arg0Link.getArgument());
                }
            }


            if (arg1s.size() == 0) {
                emptyPatientMentions.put(UimaAnnotationUtils.toSpan(evmHead), evm);
            } else {
                for (EventMentionArgumentLink arg1Link : arg1s) {
                    eventHead2Arg1.put(UimaAnnotationUtils.toSpan(evmHead), arg1Link.getArgument());
                }
            }
        }

        //xcomp:
        //purpcl:
        //advcl: subject of the clause might be omitted
        //vch??

        for (Map.Entry<Span, Collection<Pair<Span, String>>> subClausePairs : shareSubjVerbPairs.asMap().entrySet()) {
            Span headSpan = subClausePairs.getKey();
            List<EntityMention> arg0s = eventHead2Arg0.get(headSpan);
            List<EntityMention> arg1s = eventHead2Arg1.get(headSpan);

            Word head = JCasUtil.selectCovered(aJCas, Word.class, headSpan.getBegin(), headSpan.getEnd()).get(0);

            for (Pair<Span, String> childDepPair : subClausePairs.getValue()) {
                Span childSpan = childDepPair.getKey();
                Word child = JCasUtil.selectCovered(aJCas, Word.class, childSpan.getBegin(), childSpan.getEnd()).get(0);

//                String dep = childDepPair.getValue();
//                boolean complementTransfer = completeMarker.contains(head.getCoveredText()) && dep.equals("vch");

                //share agent
                if (!child.getPos().equals(PennTreeTagSet.VBN)) {
                    if (emptyAgentMentions.containsKey(childSpan)) {
                        EventMention emptyAgentMention = emptyAgentMentions.get(childSpan);
                        if (arg0s != null && arg0s.size() > 0) {
                            for (EntityMention arg0 : arg0s) {
                                EventMentionArgumentLink newArgument = addNewArgument(aJCas, emptyAgentMention, arg0, PropBankTagSet.ARG0);
                                eventHead2Arg0.put(childSpan, newArgument.getArgument());
                            }
                            emptyAgentMentions.remove(childSpan);
                        } else {
                            Pair<Word, String> headSubjDepPair = getSubj(head);
                            if (headSubjDepPair != null) {
                                Word headSubj = headSubjDepPair.getKey();
                                EventMentionArgumentLink newArgument = addNewArgument(aJCas, emptyAgentMention, headSubj, PropBankTagSet.ARG0);
                                if (childDepPair.getValue().equals("vch")) {
                                    System.out.println(String.format("Sharing agent [%s] from [%s] to [%s]", newArgument.getArgument().getCoveredText(), head.getCoveredText(), emptyAgentMention.getCoveredText()));
                                    System.out.println(JCasUtil.selectCovering(Sentence.class, head).get(0).getCoveredText());
                                }
                                eventHead2Arg0.put(childSpan, newArgument.getArgument());
                            }
                            emptyAgentMentions.remove(childSpan);
                        }
                    }
                } else {
                    //share patient
                    if (emptyPatientMentions.containsKey(childSpan)) {
                        if (arg1s != null && arg1s.size() > 0) {
                            EventMention emptyPatientMention = emptyPatientMentions.get(childSpan);
                            for (EntityMention arg1 : arg1s) {
                                EventMentionArgumentLink newArgument = addNewArgument(aJCas, emptyPatientMention, arg1, PropBankTagSet.ARG1);
                                eventHead2Arg1.put(childSpan, newArgument.getArgument());
                            }
                        }
                        emptyPatientMentions.remove(childSpan);
                    }
                }
            }
        }

        List<Set<Word>> conjVerbLists = findConjVerbs(aJCas);


        for (Set<Word> conjVerbs : conjVerbLists) {
            Span agentSharerSpan = null;
            Span patientSharerSpan = null;

            List<Span> agentShareeSpans = new ArrayList<>();
            List<Span> patientShareeSpans = new ArrayList<>();

            for (Word verb : conjVerbs) {
                Span verbSpan = UimaAnnotationUtils.toSpan(verb);

                if (emptyAgentMentions.containsKey(verbSpan)) {
                    agentShareeSpans.add(verbSpan);
                } else {
                    agentSharerSpan = verbSpan;
                }

                if (emptyPatientMentions.containsKey(verbSpan)) {
                    patientShareeSpans.add(verbSpan);
                } else {
                    patientSharerSpan = verbSpan;
                }
            }


            if (agentShareeSpans.size() == conjVerbs.size() - 1 && agentSharerSpan != null) {
//                Word agentSharer = JCasUtil.selectCovered(aJCas, Word.class, agentSharerSpan.getBegin(), agentSharerSpan.getEnd()).get(0);

                if (eventHead2Arg0.containsKey(agentSharerSpan))
                    for (EntityMention sharedArg0 : eventHead2Arg0.get(agentSharerSpan)) {
                        for (Span agentShareeSpan : agentShareeSpans) {
                            addNewArgument(aJCas, emptyAgentMentions.get(agentShareeSpan), sharedArg0, PropBankTagSet.ARG0);
//                            System.out.println(String.format("Sharing agent [%s] from [%s] to [%s]", sharedArg0.getCoveredText(), agentSharer.getCoveredText(), emptyAgentMentions.get(agentShareeSpan).getCoveredText()));
//                            System.out.println(JCasUtil.selectCovering(Sentence.class, agentSharer).get(0).getCoveredText());
                        }
                    }
            }

            if (patientShareeSpans.size() == conjVerbs.size() - 1 && patientSharerSpan != null) {
//                Word patientSharer = JCasUtil.selectCovered(aJCas, Word.class, patientSharerSpan.getBegin(), patientSharerSpan.getEnd()).get(0);

                if (eventHead2Arg1.containsKey(patientSharerSpan))
                    for (EntityMention sharedArg1 : eventHead2Arg1.get(patientSharerSpan)) {
                        for (Span patientShareeSpan : patientShareeSpans) {
                            addNewArgument(aJCas, emptyPatientMentions.get(patientShareeSpan), sharedArg1, PropBankTagSet.ARG1);
//                            System.out.println(String.format("Sharing patient [%s] from [%s] to [%s]", sharedArg1.getCoveredText(), patientSharer.getCoveredText(), emptyPatientMentions.get(patientShareeSpan).getCoveredText()));
//                            System.out.println(JCasUtil.selectCovering(Sentence.class, patientSharer).get(0).getCoveredText());
                        }
                    }
            }
        }
    }


    private Pair<Word, String> getSubj(Word word) {
        FSList depsFS = word.getChildDependencyRelations();
        if (depsFS != null) {
            for (Dependency dep : FSCollectionFactory.create(depsFS, Dependency.class)) {
                if (dep.getDependencyType().contains("subj")) {
                    return Pair.of(dep.getChild(), dep.getDependencyType());
                }
            }
        }
        return null;
    }


    private void addToChain(List<Set<Word>> conjVerbChains, Word word1, Word word2) {
        boolean existCluster = false;
        for (Set<Word> conjChain : conjVerbChains) {
            if (conjChain.contains(word1) || conjChain.contains(word2)) {
                conjChain.add(word1);
                conjChain.add(word2);
                existCluster = true;
            }
        }
        if (!existCluster) {
            Set<Word> newChain = new HashSet<>();
            newChain.add(word1);
            newChain.add(word2);
            conjVerbChains.add(newChain);
        }
    }

    private List<Set<Word>> findConjVerbs(JCas aJCas) {
        List<Set<Word>> conjVerbChains = new ArrayList<>();

        for (StanfordDependencyRelation dep : JCasUtil.select(aJCas, StanfordDependencyRelation.class)) {

            if (dep.getDependencyType().equals("conj_and")) {
                if (dep.getHead().getPos().startsWith(PennTreeTagSet.VERB_PREFIX)) {
                    addToChain(conjVerbChains, dep.getHead(), dep.getChild());
                }
            }
        }

        // check conjVerbs to make it POS consistent
        for (Set<Word> conjVerbs : conjVerbChains) {
            TObjectIntMap<String> posCounts = new TObjectIntHashMap<>();
            for (Word v : conjVerbs) {
                posCounts.adjustOrPutValue(v.getPos(), 1, 1);
            }
            if (posCounts.size() != 1) {
                int maxPosCount = -1;
                String maxPos = null;
                for (String keyPos : posCounts.keySet()) {
                    int count = posCounts.get(keyPos);
                    if (count > maxPosCount) {
                        maxPosCount = count;
                        maxPos = keyPos;
                    }
                }

                List<Word> verbsToRemove = new ArrayList<>();
                for (Word verb : conjVerbs) {
                    if (!verb.getPos().equals(maxPos)) {
                        verbsToRemove.add(verb);
                    }
                }

                for (Word verbToRemove : verbsToRemove) {
                    conjVerbs.remove(verbToRemove);
                }
            }
        }

        return conjVerbChains;
    }


    private void findShareSubjVerbs(JCas aJCas) {
        shareSubjVerbPairs = ArrayListMultimap.create();

        for (StanfordDependencyRelation dep : JCasUtil.select(aJCas, StanfordDependencyRelation.class)) {
            if (shareSubjDeps.contains(dep.getDependencyType())) {
                Word head = dep.getHead();
                Word child = dep.getChild();
                shareSubjVerbPairs.put(UimaAnnotationUtils.toSpan(head), Pair.of(UimaAnnotationUtils.toSpan(child), dep.getDependencyType()));
            }
        }

        // this will replace some Stanford dependency generated above, which is what we want
        for (FanseDependencyRelation dep : JCasUtil.select(aJCas, FanseDependencyRelation.class)) {
            if (shareSubjDeps.contains(dep.getDependencyType())) {
                Word head = dep.getHead();
                Word child = dep.getChild();

                Span headSpan = UimaAnnotationUtils.toSpan(head);
                Span childSpan = UimaAnnotationUtils.toSpan(child);

//                if (dep.getDependencyType().equals("vch")){
//                    System.out.println("vch "+head.getCoveredText()+" "+child.getCoveredText());
//                }

                //remove stanford one if duplicated
                if (shareSubjVerbPairs.containsKey(headSpan)) {
                    Pair<Span, String> duplicatedPair = null;
                    for (Pair<Span, String> stanfordPair : shareSubjVerbPairs.get(headSpan)) {
                        if (stanfordPair.getKey().equals(childSpan)) {
                            duplicatedPair = stanfordPair;
                            break;
                        }
                    }

                    if (duplicatedPair != null) {
                        shareSubjVerbPairs.remove(headSpan, duplicatedPair);
                    }
                }

                shareSubjVerbPairs.put(headSpan, Pair.of(childSpan, dep.getDependencyType()));
            }
        }
    }

    @Override
    public String getComponentId() {
        return COMPONENT_ID;
    }


}
