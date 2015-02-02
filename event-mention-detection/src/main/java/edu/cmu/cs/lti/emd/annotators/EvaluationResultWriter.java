package edu.cmu.cs.lti.emd.annotators;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.collection_reader.EventMentionDetectionDataReader;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.CandidateEventMention;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.io.writer.AbstractSimpleTextWriterAnalsysisEngine;
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

    public static final String SYSTEM_ID = "CMU_TWO_STEP";

    @Override
    public String getTextToPrint(JCas aJCas) {
        StringBuilder sb = new StringBuilder();

        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        String articleName = article.getArticleName();

        sb.append("#BeginOfDocument ").append(articleName).append("\n");

        int eventId = 1;
        for (CandidateEventMention candidate : JCasUtil.select(aJCas, CandidateEventMention.class)) {
            if (candidate.getPredictedType() != null && !candidate.getPredictedType().equals(EventMentionCandidateFeatureGenerator.OTHER_TYPE)) {
                List<String> parts = new ArrayList<>();
                parts.add(SYSTEM_ID);
                parts.add(articleName);
                parts.add("E" + eventId++);
                Pair<String, String> wordInfo = getWords(candidate);
                parts.add(wordInfo.getValue0());
                parts.add(wordInfo.getValue1());
                parts.add(candidate.getPredictedType());
                parts.add("Actual");
                parts.add("1");
                sb.append(Joiner.on("\t").join(parts)).append("\n");
            }
        }

        sb.append("#EndOfDocument\n");

        return sb.toString();
    }

    private Pair<String, String> getWords(CandidateEventMention candidate) {
        List<String> wordIds = new ArrayList<>();
        List<String> surface = new ArrayList<>();
        for (Word word : JCasUtil.selectCovered(Word.class, candidate)) {
            if (word.getComponentId().equals(EventMentionDetectionDataReader.componentId)) {
                wordIds.add(word.getId());
                surface.add(word.getCoveredText());
            }
        }
        return Pair.with(Joiner.on(",").join(wordIds), Joiner.on(" ").join(surface));
    }

}

