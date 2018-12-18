package edu.cmu.cs.lti.salience.utils;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.ArticleComponent;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import org.apache.uima.fit.util.JCasUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/18/17
 * Time: 6:30 PM
 *
 * @author Zhengzhong Liu
 */
public class TextUtils {
    /**
     * Output as space tokenized text.
     *
     * @param component
     * @return
     */
    public static String asTokenized(ComponentAnnotation component) {
        List<String> words = new ArrayList<>();
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, component)) {
            words.add(token.getCoveredText());
        }
        return Joiner.on(" ").join(words);
    }

    public static Map<StanfordCorenlpToken, Span> getTokenSpacedOffset(ArticleComponent articleComponent){
        Map<StanfordCorenlpToken, Span> tokenOffsets = new HashMap<>();
        int offset = 0;
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, articleComponent)) {
            int begin = offset;
            int tokenSize = asTokenized(token).split(" ").length;
            int end = begin + tokenSize;
            tokenOffsets.put(token, Span.of(begin, end));
            offset += tokenSize;
        }
        return tokenOffsets;
    }

    /**
     * Get token based offset of the annotation, based on the corresponding article. The tokens are based on space
     * delimiters.
     *
     * @param articleComponent
     * @param annotation
     * @return
     */
    public static Span getSpaceTokenOffset(ArticleComponent articleComponent, ComponentAnnotation annotation) {
        int tokensBefore = 0;
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, articleComponent)) {
            if (token.getEnd() - 1 < annotation.getBegin()) {
                tokensBefore += asTokenized(token).split(" ").length;
            }
        }

        int begin = tokensBefore;
        int annoLength = 0;
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, annotation)) {
            annoLength += asTokenized(token).split(" ").length;
        }
        int end = begin + annoLength;
        return Span.of(begin, end);
    }
}
