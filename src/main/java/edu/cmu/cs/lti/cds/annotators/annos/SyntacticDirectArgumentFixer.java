package edu.cmu.cs.lti.cds.annotators.annos;

import edu.cmu.cs.lti.ling.PennTreeTagSet;
import edu.cmu.cs.lti.ling.PropBankTagSet;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Utils;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.*;

/**
 * <p>Several syntactic relations between events and entities are used to fix the Arg0 and Arg1</p>
 * <p>1. Find a Arg0 from wrong labelled patients by subj
 * <p>2. Find a Arg0/Arg1 using partmod</p>
 * <p/>
 * <p/>
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/15/14
 * Time: 4:48 PM
 */
public class SyntacticDirectArgumentFixer extends AbstractEntityMentionCreator {
    public static final String COMPONENT_ID = SyntacticDirectArgumentFixer.class.getSimpleName();

    @Override
    public String getComponentId() {
        return COMPONENT_ID;
    }

    @Override
    public void subprocess(JCas aJCas) {
        logger.info(progressInfo(aJCas));

//
//        ArrayListMultimap<Span, EntityMention> eventHead2Arg0 = ArrayListMultimap.create();
//        ArrayListMultimap<Span, EntityMention> eventHead2Arg1 = ArrayListMultimap.create();
//
//        Map<Span, EventMention> emptyAgentMentions = new HashMap<>();
//        Map<Span, EventMention> emptyPatientMentions = new HashMap<>();

        for (EventMention evm : JCasUtil.select(aJCas, EventMention.class)) {
            Word evmHead = evm.getHeadWord();

            List<Dependency> evmHeadDependencies = UimaConvenience.convertFSListToList(evmHead.getHeadDependencyRelations(), Dependency.class);
            List<Dependency> evmChildDependencies = UimaConvenience.convertFSListToList(evmHead.getChildDependencyRelations(), Dependency.class);

            Set<EventMentionArgumentLink> arg0s = new HashSet<>();
            Set<EventMentionArgumentLink> arg1s = new HashSet<>();

            for (EventMentionArgumentLink link : FSCollectionFactory.create(evm.getArguments(), EventMentionArgumentLink.class)) {
                if (link.getArgumentRole().equals(PropBankTagSet.ARG0)) {
                    arg0s.add(link);
                } else if (link.getArgumentRole().equals(PropBankTagSet.ARG1)) {
                    arg1s.add(link);
                }
            }

            //fix duplicate arg1 with subj
            if (arg1s.size() > 1 && arg0s.isEmpty()) {
                Map<Span, EventMentionArgumentLink> head2Arg1Links = new HashMap<>();
                for (EventMentionArgumentLink arg1Link : arg1s) {
                    EntityMention arg1 = arg1Link.getArgument();
                    head2Arg1Links.put(Utils.toSpan(arg1.getHead()), arg1Link);
                }

                for (Dependency childDep : evmChildDependencies) {
                    if (childDep.getDependencyType().contains("subj")) {
                        Word depChild = childDep.getChild();
                        Span childSpan = Utils.toSpan(depChild);
//                        System.out.println("Investigating " + evm.getCoveredText());
                        if (head2Arg1Links.containsKey(childSpan)) {
//                            System.out.println("Found wrong arg1 link " + depChild.getCoveredText());
                            EventMentionArgumentLink wrongArg1Link = head2Arg1Links.get(childSpan);
                            arg1s.remove(wrongArg1Link);
                            wrongArg1Link.setArgumentRole("Arg0");
                            wrongArg1Link.setComponentId(COMPONENT_ID);
                            arg0s.add(wrongArg1Link);
                        }
                    }
                }
            }

            for (Dependency dep : evmHeadDependencies) {
                String depType = dep.getDependencyType();
                Word depHead = dep.getHead();

                //TODO investigate whether infmod can be included
                //infmod???, partmod:
                if (depType.equals("partmod")) {
                    if (evmHead.getPos().equals(PennTreeTagSet.VBG)) {
                        if (arg0s.size() == 0) {
                            arg0s.add(addNewArgument(aJCas, evm, depHead, PropBankTagSet.ARG0));
                        }
                    }

                    if (evmHead.getPos().equals(PennTreeTagSet.VBN)) {
                        if (arg1s.size() == 0) {
                            arg1s.add(addNewArgument(aJCas, evm, depHead, PropBankTagSet.ARG1));
                        }
                    }
                }
            }
        }

    }


}
