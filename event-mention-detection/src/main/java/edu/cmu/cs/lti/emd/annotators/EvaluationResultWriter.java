package edu.cmu.cs.lti.emd.annotators;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.io.writer.AbstractSimpleTextWriterAnalsysisEngine;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
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
public class EvaluationResultWriter extends AbstractSimpleTextWriterAnalsysisEngine {

    public static final String SYSTEM_ID = "CMU-TWO-STEP";

    @Override
    public String getTextToPrint(JCas aJCas) {
        StringBuilder sb = new StringBuilder();

        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        String articleName = article.getArticleName();

        sb.append("#BeginOfDocument ").append(articleName).append("\n");

        TokenAlignmentHelper align = new TokenAlignmentHelper();
        align.loadWord2Stanford(aJCas, TbfEventDataReader.COMPONENT_ID);

        int eventId = 1;
        for (CandidateEventMention candidate : JCasUtil.select(aJCas, CandidateEventMention.class)) {
            if (candidate.getPredictedType() != null && !candidate.getPredictedType().equals(EventMentionTypeLearner.OTHER_TYPE)) {
                List<String> parts = new ArrayList<>();
                parts.add(SYSTEM_ID);
                parts.add(articleName);
                String eid = "E" + eventId++;
                parts.add(eid);
                Pair<String, String> wordInfo = getWords(candidate, align);
                parts.add(wordInfo.getValue0());
                parts.add(wordInfo.getValue1());
                parts.add(candidate.getPredictedType());
                parts.add(candidate.getPredictedRealis() == null ? "Actual" : candidate.getPredictedRealis());
                parts.add("1");
                sb.append(Joiner.on("\t").join(parts)).append("\n");

                candidate.setId(eid);
            }
        }

        sb.append("#EndOfDocument\n");

        return sb.toString();
    }

    private List<Word> getSubWords(CandidateEventMention candidate, TokenAlignmentHelper align) {
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

    private Pair<String, String> getWords(CandidateEventMention candidate, TokenAlignmentHelper align) {
        List<String> wordIds = new ArrayList<>();
        List<String> surface = new ArrayList<>();

//        List<Word> words = JCasUtil.selectCovered(Word.class, candidate);

        List<Word> words = getSubWords(candidate, align);

        if (words.size() == 0) {
            System.out.println(candidate.getCoveredText() + " " + candidate.getBegin() + " " + candidate.getEnd());
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

