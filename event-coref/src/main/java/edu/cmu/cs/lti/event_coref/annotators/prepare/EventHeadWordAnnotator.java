package edu.cmu.cs.lti.event_coref.annotators.prepare;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.DebugUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 2/3/16
 * Time: 8:59 PM
 *
 * @author Zhengzhong Liu
 */
public class EventHeadWordAnnotator extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String language = JCasUtil.selectSingle(aJCas, Article.class).getLanguage();

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            StanfordCorenlpToken headWord = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);

            if (language.equals("zh")) {
                CharacterAnnotation headCharacter = UimaNlpUtils.findHeadCharacterFromZparAnnotation(mention);
                if (headWord == null) {
                    List<StanfordCorenlpToken> headCharacterWords = JCasUtil.selectCovering
                            (StanfordCorenlpToken.class, headCharacter);
                    if (headCharacterWords.size() > 0) {
                        headWord = headCharacterWords.get(0);
                    }
                }
                mention.setHeadCharacter(headCharacter);
            }

            if (headWord == null) {
                logger.debug(String.format("Cannot find head word for annotation [%s]-[%d:%d].",
                        mention.getCoveredText(), mention.getBegin(), mention.getEnd()));
                DebugUtils.pause(logger);
            }

            mention.setHeadWord(headWord);
        }

        for (EventMentionSpan ems : JCasUtil.select(aJCas, EventMentionSpan.class)) {
            for (EventMention eventMention : FSCollectionFactory.create(ems.getEventMentions(), EventMention.class)) {
                ems.setHeadWord(eventMention.getHeadWord());
            }
        }
    }
}
