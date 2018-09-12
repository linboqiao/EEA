package edu.cmu.cs.lti.uima.util;

import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.FanseToken;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TokenAlignmentHelper {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private Map<FanseToken, StanfordCorenlpToken> f2s;

    private Map<StanfordCorenlpToken, FanseToken> s2f;

    private Map<Word, StanfordCorenlpToken> w2s;

    private Map<StanfordCorenlpToken, Word> s2w;

    private Map<Word, FanseToken> w2f;

    private Map<FanseToken, Word> f2w;

//    private Map<SemaforLabel, StanfordCorenlpToken> semafor2Stanford;

    private final boolean verbose;

    public TokenAlignmentHelper() {
        this(false);
    }

    public TokenAlignmentHelper(boolean verbose) {
        this.verbose = verbose;
    }

    public void loadFanse2Stanford(JCas aJCas) {
        int stanfordSize = JCasUtil.select(aJCas, StanfordCorenlpToken.class).size();
        int fanseSize = JCasUtil.select(aJCas, FanseToken.class).size();

        if (stanfordSize == 0) {
            logger.error("Stanford tokens do not exist, cannot load.");
        } else if (fanseSize == 0) {
            logger.error("Fanse tokens does not exists, cannot load.");
        } else {
            f2s = getType2TypeMapping(aJCas, FanseToken.class, StanfordCorenlpToken.class);
        }
    }

    public void loadStanford2Fanse(JCas aJCas) {
        s2f = getType2TypeMapping(aJCas, StanfordCorenlpToken.class, FanseToken.class);
    }

    public void loadWord2Stanford(JCas aJCas, String targetComponentId) {
        w2s = getType2TypeMapping(aJCas, Word.class, StanfordCorenlpToken.class, targetComponentId);
        s2w = new HashMap<>();
        for (Map.Entry<Word, StanfordCorenlpToken> ws : w2s.entrySet()) {
            s2w.put(ws.getValue(), ws.getKey());
        }
    }

    public void loadWord2Stanford(JCas aJCas) {
        w2s = getType2TypeMapping(aJCas, Word.class, StanfordCorenlpToken.class);
        s2w = new HashMap<>();
        for (Map.Entry<Word, StanfordCorenlpToken> ws : w2s.entrySet()) {
            s2w.put(ws.getValue(), ws.getKey());
        }
    }

    public void loadWord2Fanse(JCas aJCas, String targetComponentId) {
        w2f = getType2TypeMapping(aJCas, Word.class, FanseToken.class, targetComponentId);
        f2w = new HashMap<>();
        for (Map.Entry<Word, FanseToken> wf : w2f.entrySet()) {
            f2w.put(wf.getValue(), wf.getKey());
        }
    }

    public String getLowercaseWordLemma(Word token) {
        StanfordCorenlpToken s;
        if (token instanceof FanseToken) {
            s = f2s.get(token);
        } else {
            s = w2s.get(token);
        }

        if (s != null) {
            return s.getLemma().toLowerCase();
        } else {
            return token.getCoveredText().toLowerCase();
        }
    }

    public FanseToken getFanseToken(Word t) {
        if (t instanceof StanfordCorenlpToken) {
            return s2f.get(t);
        } else {
            return w2f.get(t);
        }
    }

    public FanseToken getFanseToken(StanfordCorenlpToken t) {
        return s2f.get(t);
    }


    public StanfordCorenlpToken getStanfordToken(FanseToken t) {
        return f2s.get(t);
    }

    public StanfordCorenlpToken getStanfordToken(Word t) {
        return w2s.get(t);
    }

//    public StanfordCorenlpToken getStanfordToken(SemaforLabel s) {
//        return semafor2Stanford.get(s);
//    }

    public Word getWord(FanseToken t) {
        return f2w.get(t);
    }

    public Word getWord(StanfordCorenlpToken t) {
        return s2w.get(t);
    }

    private <ToType extends ComponentAnnotation, FromType extends ComponentAnnotation> Map<FromType, ToType>
    getType2TypeMapping(JCas aJCas, Class<FromType> clazzFrom, Class<ToType> clazzTo) {
        return getType2TypeMapping(aJCas, clazzFrom, clazzTo, null);
    }

    private <ToType extends ComponentAnnotation, FromType extends ComponentAnnotation> Map<FromType, ToType>
    getType2TypeMapping(JCas aJCas, Class<FromType> clazzFrom, Class<ToType> clazzTo, String targetComponentId) {
        Map<ToType, Collection<FromType>> tokenCoveredWord = JCasUtil.indexCovered(aJCas, clazzTo,
                clazzFrom);

        Map<ToType, Collection<FromType>> tokenCoveringWord = JCasUtil.indexCovering(aJCas, clazzTo,
                clazzFrom);

        Map<FromType, ToType> word2Token = new HashMap<>();

        for (ToType token : JCasUtil.select(aJCas, clazzTo)) {
            if (token.getBegin() == 0 && token.getEnd() == 0 || token.getBegin() < 0)
                continue;

            Collection<FromType> coveredWords;
            if (targetComponentId != null) {
                coveredWords = filterByComponentId(tokenCoveredWord.get(token), targetComponentId);
            } else {
                coveredWords = tokenCoveredWord.get(token);
            }

            if (coveredWords.size() > 0) {
                for (FromType word : coveredWords) {
                    word2Token.put(word, token);
                }
            } else {// in case the token range is larger than the word, use its covering token
                Collection<FromType> coveringToken = filterByComponentId(tokenCoveringWord.get(token),
                        targetComponentId);
                if (coveringToken.size() == 0) {
                    if (verbose) {
                        logger.warn(String.format("The word : %s [%s] [%d, %d] cannot be associated with a %s",
                                token.getCoveredText(), token.getClass().getSimpleName(), token.getBegin(),
                                token.getEnd(), clazzFrom.getSimpleName()));
                    }
                } else {
                    if (verbose) {
                        logger.warn("Use covering for alignment");
                    }
                    for (FromType word : coveringToken) {
                        word2Token.put(word, token);
                        logger.debug(
                                String.format("Using covering : %s [%s] [%d, %d] covering  %s [%s] [%d, %d]",
                                        word.getCoveredText(), word.getClass().getSimpleName(), word.getBegin(),
                                        word.getEnd(), token.getCoveredText(), token.getClass().getSimpleName(),
                                        token.getBegin(), token.getEnd())
                        );
                    }
                }
            }
        }

        return word2Token;
    }

    private <T extends ComponentAnnotation> List<T> filterByComponentId(Collection<T> origin, String
            targetComponentId) {
        List<T> targets = new ArrayList<>();
        for (T originToken : origin) {
            if (originToken.getComponentId().equals(targetComponentId)) {
                targets.add(originToken);
            }
        }

        return targets;
    }
}