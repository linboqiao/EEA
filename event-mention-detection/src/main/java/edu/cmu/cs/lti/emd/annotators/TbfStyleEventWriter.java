package edu.cmu.cs.lti.emd.annotators;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.io.writer.AbstractSimpleTextWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/1/15
 * Time: 9:15 PM
 */
public class TbfStyleEventWriter extends AbstractSimpleTextWriterAnalysisEngine {
    public static final String PARAM_SYSTEM_ID = "systemId";

    @ConfigurationParameter(name = PARAM_SYSTEM_ID)
    private String systemId;

    @Override
    public String getTextToPrint(JCas aJCas) {
        StringBuilder sb = new StringBuilder();

        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        String articleName = article.getArticleName();

        sb.append("#BeginOfDocument ").append(articleName).append("\n");

        TokenAlignmentHelper align = new TokenAlignmentHelper();
        align.loadWord2Stanford(aJCas, TbfEventDataReader.COMPONENT_ID);

        int eventId = 1;
        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            List<String> parts = new ArrayList<>();
            parts.add(systemId);
            parts.add(articleName);
            String eid = "E" + eventId++;
            parts.add(eid);
            Pair<String, String> wordInfo = getWords(mention, align);
            parts.add(wordInfo.getValue0());
            parts.add(wordInfo.getValue1());
            parts.add(mention.getEventType());
            parts.add(mention.getRealisType() == null ? "Actual" : mention.getRealisType());
//            parts.add("1");
            sb.append(Joiner.on("\t").join(parts)).append("\n");
            mention.setId(eid);
        }
        sb.append("#EndOfDocument\n");

        return sb.toString();
    }

    private List<Word> getSubWords(ComponentAnnotation candidate, TokenAlignmentHelper align) {
        List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, candidate);

        List<Word> words = new ArrayList<>();

        for (StanfordCorenlpToken token : tokens) {
            Word word = getWord(token);
            if (word == null) {
//                System.err.println(token.getCoveredText() + " cannot map");
            } else {
                words.add(word);
            }
        }
        return words;
    }


    private Word getWord(StanfordCorenlpToken token) {
        for (Word word : JCasUtil.selectCovered(Word.class, token)) {
            if (word.getComponentId().equals(TbfEventDataReader.COMPONENT_ID)) {
                return word;
            }
        }

        for (Word word : JCasUtil.selectCovering(Word.class, token)) {
            if (word.getComponentId().equals(TbfEventDataReader.COMPONENT_ID)) {
                return word;
            }
        }
        return null;
    }

    private Pair<String, String> getWords(ComponentAnnotation mention, TokenAlignmentHelper align) {
        List<String> wordIds = new ArrayList<>();
        List<String> surface = new ArrayList<>();

//        List<Word> words = JCasUtil.selectCovered(Word.class, candidate);

        List<Word> words = getSubWords(mention, align);

        if (words.size() == 0) {
            System.out.println(mention.getCoveredText() + " " + mention.getBegin() + " " + mention.getEnd());
            Utils.pause();
        }

        for (Word word : words) {
            if (word.getComponentId().equals(TbfEventDataReader.COMPONENT_ID)) {
                wordIds.add(word.getId());
                surface.add(word.getCoveredText());
            }
        }

        return Pair.with(Joiner.on(",").join(wordIds), Joiner.on(" ").join(surface));
    }

}

