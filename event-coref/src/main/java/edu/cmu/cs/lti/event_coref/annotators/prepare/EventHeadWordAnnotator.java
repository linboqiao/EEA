package edu.cmu.cs.lti.event_coref.annotators.prepare;

import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.CharacterAnnotation;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
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

        UimaConvenience.printProcessLog(aJCas, logger, true);
        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {

            StanfordCorenlpToken headWord = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);

            if (language.equals("zh")) {
                CharacterAnnotation headCharacter = UimaNlpUtils.findHeadCharacterFromZparAnnotatoin(mention);
                if (headWord == null) {
                    List<StanfordCorenlpToken> headCharacterWords = JCasUtil.selectCovered
                            (StanfordCorenlpToken.class, headCharacter);
                    if (headCharacterWords.size() > 0) {
                        headWord = headCharacterWords.get(0);
                    }
                }
                mention.setHeadCharacter(headCharacter);
            }

            mention.setHeadWord(headWord);
        }
    }
}
