package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.script.type.QuotedContent;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordTreeAnnotation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/13/15
 * Time: 5:08 PM
 *
 * @author Zhengzhong Liu
 */
public class QuoteAnnotator extends AbstractLoggingAnnotator {
    private int maxTokensInBetween = 60;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        int start = -1;
        int numWordsInBetween = 0;
        for (StanfordCorenlpToken token : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
//            System.out.println(token.getLemma());
            if (token.getLemma().equals("``")) {
                start = token.getBegin();
                numWordsInBetween = 0;
            } else if (token.getLemma().equals("''") && start > 0) {
                if (numWordsInBetween < maxTokensInBetween) {
                    QuotedContent quotedContent = new QuotedContent(aJCas);
                    UimaAnnotationUtils.finishAnnotation(quotedContent, start, token.getEnd(), COMPONENT_ID, 0, aJCas);
                    if (checkSentenceConstruct(quotedContent)) {
                        quotedContent.setPhraseQuote(false);
                    } else {
                        quotedContent.setPhraseQuote(true);
                    }
                }
                start = -1;
            } else if (start != -1) {
                numWordsInBetween++;
            }
        }
    }

    private boolean checkSentenceConstruct(QuotedContent content) {
        StanfordTreeAnnotation tree = UimaNlpUtils.findLargestContainingTree(content);
        if (tree.getPennTreeLabel().equals("S") || tree.getPennTreeLabel().equals("ROOT")) {
            return true;
        }
        return false;
    }
}