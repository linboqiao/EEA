package limo.tokenizer.stanford;


import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.LexedTokenFactory;

/**
 * Constructs a Word from a String. This is the default
 * TokenFactory for PTBLexer. It discards the positional information.
 *
 * @author Jenny Finkel
 */
public class WordTokenFactory implements LexedTokenFactory<Word> {

  public Word makeToken(String str, int begin, int length) {
    return new Word(str, begin, begin+length);
  }
}
