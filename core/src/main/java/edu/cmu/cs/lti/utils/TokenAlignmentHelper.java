package edu.cmu.cs.lti.utils;

import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.FanseToken;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.*;

public class TokenAlignmentHelper {
    Map<FanseToken, StanfordCorenlpToken> f2s;

    Map<StanfordCorenlpToken, FanseToken> s2f;

    Map<Word, StanfordCorenlpToken> w2s;

    Map<StanfordCorenlpToken, Word> s2w;

    Map<Word, FanseToken> w2f;

    Map<FanseToken, Word> f2w;

    private final boolean verbose;

    public TokenAlignmentHelper() {
        this(false);
    }

    public TokenAlignmentHelper(boolean verbose) {
        this.verbose = verbose;
    }

    public void loadFanse2Stanford(JCas aJCas) {
        f2s = getType2TypeMapping(aJCas, FanseToken.class, StanfordCorenlpToken.class);

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
        StanfordCorenlpToken s = w2s.get(token);
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

    public FanseToken getStanfordToken(StanfordCorenlpToken t) {
        return s2f.get(t);
    }


    public StanfordCorenlpToken getStanfordToken(FanseToken t) {
        return f2s.get(t);
    }

    public StanfordCorenlpToken getStanfordToken(Word t) {
        return w2s.get(t);
    }


    public Word getWord(FanseToken t) {
        return f2w.get(t);
    }

    public Word getWord(StanfordCorenlpToken t) {
        return s2w.get(t);
    }

    private <ToType extends ComponentAnnotation, FromType extends ComponentAnnotation> Map<FromType, ToType> getType2TypeMapping(
            JCas aJCas, Class<FromType> clazzFrom, Class<ToType> clazzTo) {
        Map<ToType, Collection<FromType>> tokenCoveredWord = JCasUtil.indexCovered(aJCas, clazzTo,
                clazzFrom);

        Map<ToType, Collection<FromType>> tokenCoveringWord = JCasUtil.indexCovering(aJCas, clazzTo,
                clazzFrom);

        Map<FromType, ToType> word2Token = new HashMap<>();

        for (ToType token : JCasUtil.select(aJCas, clazzTo)) {
            if (token.getBegin() == 0 && token.getEnd() == 0 || token.getBegin() < 0)
                continue;

            Collection<FromType> coveredWords = tokenCoveredWord.get(token);

            if (coveredWords.size() > 0) {
                for (FromType word : coveredWords) {
                    word2Token.put(word, token);
                }
            } else {// in case the token range is larger than the word, use its covering token
                Collection<FromType> coveringToken = tokenCoveringWord.get(token);
                if (coveringToken.size() == 0) {
                    System.err
                            .println(String.format("The word : %s [%d, %d] cannot be associated with a %s",
                                    token.getCoveredText(), token.getBegin(), token.getEnd(),
                                    clazzTo.getSimpleName()));
                } else {
                    System.err.println("Use covering");
                    for (FromType word : coveringToken) {
                        word2Token.put(word, token);
                    }
                }
            }
        }

        return word2Token;
    }

    private <ToType extends ComponentAnnotation, FromType extends ComponentAnnotation> Map<FromType, ToType> getType2TypeMapping(
            JCas aJCas, Class<FromType> clazzFrom, Class<ToType> clazzTo, String targetComponentId) {
        Map<ToType, Collection<FromType>> tokenCoveredWord = JCasUtil.indexCovered(aJCas, clazzTo,
                clazzFrom);

        Map<ToType, Collection<FromType>> tokenCoveringWord = JCasUtil.indexCovering(aJCas, clazzTo,
                clazzFrom);

        Map<FromType, ToType> word2Token = new HashMap<>();

        for (ToType token : JCasUtil.select(aJCas, clazzTo)) {
            if (token.getBegin() == 0 && token.getEnd() == 0 || token.getBegin() < 0)
                continue;

            Collection<FromType> coveredWords = filterByComponentId(tokenCoveredWord.get(token), targetComponentId);

            if (coveredWords.size() > 0) {
                for (FromType word : coveredWords) {
                    word2Token.put(word, token);
                }
            } else {// in case the token range is larger than the word, use its covering token
                Collection<FromType> coveringToken = filterByComponentId(tokenCoveringWord.get(token), targetComponentId);
                if (coveringToken.size() == 0) {
                    if (verbose) {
                        System.err
                                .println(String.format("The word : %s [%d, %d] cannot be associated with a %s",
                                        token.getCoveredText(), token.getBegin(), token.getEnd(),
                                        clazzTo.getSimpleName()));
                    }
                } else {
                    if (verbose) {
                        System.err.println("Use covering for alignment");
                    }
                    for (FromType word : coveringToken) {
                        word2Token.put(word, token);
                    }
                }
            }
        }

        return word2Token;
    }

    private <T extends ComponentAnnotation> List<T> filterByComponentId(Collection<T> origin, String targetComponentId) {
        List<T> targets = new ArrayList<>();
        for (T originToken : origin) {
            if (originToken.getComponentId().equals(targetComponentId)) {
                targets.add(originToken);
            }
        }

        return targets;
    }
}