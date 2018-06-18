package edu.cmu.cs.lti.event_coref.annotators.prepare;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.ArrayList;

/**
 * Quick and dirty argument extractor based on Semafor and Fanse parsers.
 *
 * @author Zhengzhong Liu
 */
public class MergedArgumentAnnotator extends AbstractLoggingAnnotator {
    public static final String ANNOTATOR_COMPONENT_ID = MergedArgumentAnnotator.class.getSimpleName();

//    TokenAlignmentHelper helper = new TokenAlignmentHelper();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        helper.loadStanford2Fanse(aJCas);
//        helper.loadFanse2Stanford(aJCas);

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            StanfordCorenlpToken headWord = (StanfordCorenlpToken) mention.getHeadWord();
            FSList headArgsFS = headWord.getChildSemanticRelations();

            if (mention.getFrameName() == null) {
                mention.setFrameName(headWord.getFrameName());
            }

            if (headArgsFS != null) {
                for (SemanticRelation relation : FSCollectionFactory.create(headArgsFS, SemanticRelation.class)) {
                    EventMentionArgumentLink argumentLink = new EventMentionArgumentLink((aJCas));
                    SemanticArgument argument = relation.getChild();
                    EntityMention argumentEntityMention = UimaNlpUtils.createArgMention(aJCas, argument
                            .getBegin(), argument.getEnd(), ANNOTATOR_COMPONENT_ID);
                    argumentLink.setArgument(argumentEntityMention);

                    if (relation.getPropbankRoleName() != null) {
                        argumentLink.setPropbankRoleName(relation.getPropbankRoleName());
                    }

                    if (relation.getFrameElementName() != null) {
                        argumentLink.setFrameElementName(relation.getFrameElementName());
                    }
                    mention.setArguments(UimaConvenience.appendFSList(aJCas, mention.getArguments(), argumentLink,
                            EventMentionArgumentLink.class));
                    UimaAnnotationUtils.finishTop(argumentLink, ANNOTATOR_COMPONENT_ID, 0, aJCas);
                }
            } else {
                // An empty argument list.
                mention.setArguments(FSCollectionFactory.createFSList(aJCas, new ArrayList<>()));
            }
        }
    }
}