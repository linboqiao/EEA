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
import java.util.HashMap;
import java.util.Map;

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

        Map<StanfordCorenlpToken, String> token2Frame = new HashMap<>();
        for (SemaforAnnotationSet annoSet : JCasUtil.select(aJCas, SemaforAnnotationSet.class)) {
            String frameName = annoSet.getFrameName();
            for (SemaforLayer layer : FSCollectionFactory.create(annoSet.getLayers(), SemaforLayer.class)) {
                if (layer.getName().equals("Target")) {
                    token2Frame.put(UimaNlpUtils.findHeadFromStanfordAnnotation(layer), frameName);
                }
            }
        }

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            StanfordCorenlpToken headWord = (StanfordCorenlpToken) mention.getHeadWord();
            FSList headArgsFS = headWord.getChildSemanticRelations();

            if (mention.getFrameName() == null) {
                if (token2Frame.containsKey(headWord)) {
                    mention.setFrameName(token2Frame.get(headWord));
                }
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