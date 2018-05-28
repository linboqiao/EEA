package edu.cmu.cs.lti.uima.util;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ExtractorBase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/26/15
 * Time: 5:36 PM
 */
public class NoiseTextFormatter {
    private static ExtractorBase extractor = ArticleExtractor.getInstance();

    private static String[] forumPatterns = {"<post[^<]*>", "<quote[^<]*>", "<\\s?/\\s?quote>", "<\\s?/\\s?post>",
            "<img[^<]*>",
            "<a\\s?href=[^>]*>", "<\\s?/\\s?a>"};

    private static String[] newsDiscardPattern = {
            "<DOCID>[^<]*<\\s/\\sDOCID>", "<DOCTYPE[^>]*>[^<]*<\\s?/\\sDOCTYPE>", "<KEYWORD>", "<\\s?/\\s?KEYWORD>",
            "<BODY>", "<TEXT>", "<\\s?/\\s?TEXT>", "<\\s?/\\s?BODY>", "<DOC>", "<\\s?/\\s?DOC>?", "<P>", "<\\s?/\\s?P>",
            "<DATETIME>[^<]*<\\s/\\sDATETIME>", "<DATELINE>[^<]*<\\s?/\\s*DATELINE>", "<DOC[^>]*>", "<HEADLINE>",
            "<\\s?/\\s?HEADLINE>"
    };

    private static String sentenceEndFixer = "[^\\p{Punct}](\\n)[\\s|\\n]*\\n";

    private String text;

    public NoiseTextFormatter(String input) {
        this.text = input;
    }

    public NoiseTextFormatter cleanWithPattern(String... patterns) {
        cleanTextWithPatterns(patterns);
        return this;
    }

    public NoiseTextFormatter cleanForum() {
        cleanTextWithPatterns(forumPatterns);
        return this;
    }

    public NoiseTextFormatter cleanNews() {
        cleanTextWithPatterns(newsDiscardPattern);
        return this;
    }

    public String getText() {
        return text;
    }

    public void cleanTextWithPatterns(String[] patterns) {
        replaceMatchedWithChar(patterns, ' ');
    }

    public void replaceMatchedWithChar(String[] patterns, char c) {
        if (text.isEmpty()) {
            return;
        }

        for (String pattern : patterns) {
            Pattern p = Pattern.compile("(" + pattern + ")", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                String found = m.group(1);
//                System.out.println(found);
                String replacementStr = StringUtils.repeat(c, found.length());
                m.appendReplacement(sb, replacementStr);
            }
            m.appendTail(sb);
            if (sb.length() != 0) {
                text = sb.toString();
            }
        }
    }

    /**
     * For multiple consecutive new lines, replace first one with a period. Only one new line will be replaced by a
     * period, which ensure the offset of the rest won't change.
     */
    public NoiseTextFormatter multiNewLineBreaker() {
        return multiNewLineBreaker("en");
    }


    /**
     * For multiple consecutive new lines, replace first one with a period. Only one new line will be replaced by a
     * period, which ensure the offset of the rest won't change.
     */
    public NoiseTextFormatter multiNewLineBreaker(String language) {
        String stopSymbol = ".";
        String pattern = "([^\\p{Punct}\\s]\\h*)(\\n)(\\s*\\n+)";

        if (language.equals("zh")) {
            stopSymbol = "。";
            pattern = "([^，。！？\\s]\\h*)(\\n)(\\s*\\n+)";
        }

        text = text.replaceAll(pattern, "$1" + stopSymbol + "$3");
        return this;
    }

    public static void main(String[] args) throws BoilerpipeProcessingException, IOException, SAXException,
            TikaException {
        String noisyText = FileUtils.readFileToString(new File("/Users/zhengzhongliu/Documents/projects/cmu-script/data/mention/LDC/LDC2015E78_DEFT_Rich_ERE_Chinese_and_English_Parallel_Annotation_V2/data/cmn/source/020f7dc5023a3dcdff18cb12b621d2a8.mp.txt"));

        String language = "zh";

        NoiseTextFormatter formatter = new NoiseTextFormatter(noisyText);
        String ruleCleaned = formatter.cleanForum().cleanNews().multiNewLineBreaker(language).getText();

        System.out.println("=== Rule results ===");
        System.out.println(ruleCleaned);

        System.out.println("Original length " + noisyText.length());
        System.out.println("Rule length " + ruleCleaned.length());
    }
}
