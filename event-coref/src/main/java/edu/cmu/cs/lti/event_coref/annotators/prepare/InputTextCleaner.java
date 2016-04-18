package edu.cmu.cs.lti.event_coref.annotators.prepare;

import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.util.NoiseTextFormatter;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Only work if the InputView really exists and contains document text. （Gonna be replaced by the direct reader
 * integrated version)
 * <p>
 * Date: 4/21/15
 * Time: 12:38 AM
 *
 * @author Zhengzhong Liu
 */
public class InputTextCleaner extends AbstractAnnotator {
    public static final String PARAM_INPUT_VIEW_NAME = "InputViewName";

    @ConfigurationParameter(name = PARAM_INPUT_VIEW_NAME)
    private String inputViewName;


    private String moreThan2ConsecutiveNewLines = "[\\p{Punct}|\\s]*[\\n]{2,}[\\p{Punct}|\\s]*";
    private String moreThan2NewLinesMixedWithWhiteSpaces =
            "[\\p{Punct}|\\s]*[\\n]+[\\p{Punct}|\\s]*\\n+[\\p{Punct}|\\s]*";

    private String whiteSpacePunctPattern = moreThan2NewLinesMixedWithWhiteSpaces + "|" + moreThan2ConsecutiveNewLines;

    //A pattern match string with only whitespaces or new lines, and must contain one \n
//    private String whiteSpacePunctPattern = "[\\p{Punct}|\\s]*[\\n]{2,
// }[\\p{Punct}|\\s]*|[\\p{Punct}|\\s]*[\\n]+[\\p{Punct}|\\s]*\\n+[\\p{Punct}|\\s]*";

    private Pattern p = Pattern.compile("(" + whiteSpacePunctPattern + ")", Pattern.DOTALL);
    private Pattern hasPunct = Pattern.compile("\\p{Punct}");


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas inputView = null;
        try {
            inputView = aJCas.getView(inputViewName);
        } catch (CASException e) {
            throw new AnalysisEngineProcessException(e);
        }
        String originalText = inputView.getDocumentText();
        String cleanedText = new NoiseTextFormatter(originalText).cleanForum().cleanNews().getText();
        //we only want to fix punctuation, but we wanna keep index exactly the same.
        assert (originalText.length() == cleanedText.length());

        aJCas.setDocumentText(puncFix(cleanedText));
    }

    private String puncFix(String text) {

        Matcher m = p.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String found = m.group(1);

            String replacementStr;
            if (containsPunct(found) || m.start() == 0) {
                replacementStr = found;
            } else {
                replacementStr = found.substring(0, 1) + "." + found.substring(2);
            }
            replacementStr = Matcher.quoteReplacement(replacementStr);
            m.appendReplacement(sb, replacementStr);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private boolean containsPunct(String str) {
        return hasPunct.matcher(str).find();
    }
}