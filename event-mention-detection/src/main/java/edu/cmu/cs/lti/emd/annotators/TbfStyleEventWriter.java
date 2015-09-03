package edu.cmu.cs.lti.emd.annotators;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.io.writer.AbstractSimpleTextWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.javatuples.Pair;

import java.util.*;

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

    public static final String goldCompontnentId = TbfEventDataReader.COMPONENT_ID;

    @Override
    public String getTextToPrint(JCas aJCas) {
        UimaConvenience.printProcessLog(aJCas, logger);

        StringBuilder sb = new StringBuilder();

        Article article = JCasUtil.selectSingle(aJCas, Article.class);

        String articleName = article.getArticleName();

        sb.append("#BeginOfDocument ").append(articleName).append("\n");

        TokenAlignmentHelper align = new TokenAlignmentHelper();
        align.loadWord2Stanford(aJCas, goldCompontnentId);

        int eventId = 1;
        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            Pair<String, String> wordInfo = getWords(mention, align);
            if (wordInfo == null) {
                continue;
            }
            List<String> parts = new ArrayList<>();
            parts.add(systemId);
            parts.add(articleName);
            String eid = "E" + eventId++;
            parts.add(eid);
            parts.add(wordInfo.getValue0());
            parts.add(wordInfo.getValue1());
            parts.add(mention.getEventType());
            parts.add(mention.getRealisType() == null ? "Actual" : mention.getRealisType());
            sb.append(Joiner.on("\t").join(parts)).append("\n");
            mention.setId(eid);
        }
        sb.append("#EndOfDocument\n");

        return sb.toString();
    }

    private List<Word> mapToGoldWordsTest(ComponentAnnotation candidate, TokenAlignmentHelper align) {

        List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, candidate);

        List<Word> tokenBasedMapping = new ArrayList<>();

        for (StanfordCorenlpToken token : tokens) {
            Word word = align.getWord(token);
            if (word == null) {
//                System.err.println(token.getCoveredText() + " cannot map");
            } else {
                tokenBasedMapping.add(word);
            }
        }
        return tokenBasedMapping;
    }

    private Set<Word> mapToGoldWords(ComponentAnnotation candidate, TokenAlignmentHelper align) {
        List<Word> allWords = JCasUtil.selectCovered(Word.class, candidate);
        Set<Word> allUnderlying = new HashSet<>();

//        logger.info(candidate.getBegin() + " " + candidate.getEnd());

        for (Word word : allWords) {
            if (word instanceof StanfordCorenlpToken) {
                // Converting from stanford token to word might have subtle differences.
                Word alignedWord = align.getWord((StanfordCorenlpToken) word);
                if (alignedWord != null) {
                    allUnderlying.add(alignedWord);
//                    logger.info("Adding from stanford " + alignedWord.getCoveredText() + " " + alignedWord.getId());
                }
            } else {
                if (word.getComponentId().equals(goldCompontnentId)) {
                    allUnderlying.add(word);
//                    logger.info("Add directly " + word.getCoveredText() + " " + word.getId() + " " + word.getBegin()
//                            + " " + +word.getEnd());
                }
            }
        }

//        logger.info(candidate.getCoveredText());
//        DebugUtils.pause();

        return allUnderlying;
    }

    private Pair<String, String> getWords(ComponentAnnotation mention, TokenAlignmentHelper align) {
        List<String> wordIds = new ArrayList<>();
        List<String> surface = new ArrayList<>();

//        List<Word> words = JCasUtil.selectCovered(Word.class, candidate);

        List<Word> words = new ArrayList<>(mapToGoldWords(mention, align));

        if (words.size() == 0) {
            logger.warn("Candidate event mention is " + mention.getCoveredText() + " " + mention.getBegin() + " " +
                    mention.getEnd());
            logger.warn("Candidate cannot be mapped to a word, this candidate will be discarded in output.");
//            DebugUtils.pause();
            return null;
        }

        Collections.sort(words, (w1, w2) -> w1.getBegin() - w2.getBegin());

        for (Word word : words) {
            wordIds.add(word.getId());
            surface.add(word.getCoveredText());
        }

        return Pair.with(Joiner.on(",").join(wordIds), Joiner.on(" ").join(surface));
    }

}

