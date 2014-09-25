package edu.cmu.cs.lti.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.FanseToken;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;

public class TokenAlignmentHelper {
  Map<FanseToken, StanfordCorenlpToken> f2s;

  Map<Word, StanfordCorenlpToken> w2s;

  public void loadFanse2Stanford(JCas aJCas) {
    f2s = getType2TypeMapping(aJCas, FanseToken.class, StanfordCorenlpToken.class);
  }

  public void loadWord2Stanford(JCas aJCas) {
    w2s = getType2TypeMapping(aJCas, Word.class, StanfordCorenlpToken.class);
  }

  public StanfordCorenlpToken getStanfordToken(FanseToken t) {
    return f2s.get(t);
  }

  public StanfordCorenlpToken getStanfordToken(Word t) {
    return w2s.get(t);
  }

  private <ToType extends ComponentAnnotation, FromType extends ComponentAnnotation> Map<FromType, ToType> getType2TypeMapping(
          JCas aJCas, Class<FromType> clazzFrom, Class<ToType> clazzTo) {
    Map<ToType, Collection<FromType>> tokenCoveredWord = JCasUtil.indexCovered(aJCas, clazzTo,
            clazzFrom);

    Map<ToType, Collection<FromType>> tokenCoveringWord = JCasUtil.indexCovering(aJCas, clazzTo,
            clazzFrom);

    Map<FromType, ToType> word2Token = new HashMap<FromType, ToType>();

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
          System.out.println("Use covering");
          for (FromType word : coveringToken) {
            word2Token.put(word, token);
          }
        }
      }
    }

    return word2Token;
  }

}
