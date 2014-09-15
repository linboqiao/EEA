package edu.cmu.cs.lti.uima.util;

/**
 * Constants for strings.
 * 
 * @author Jun Araki
 */
public class StringConstants {

  public enum BasicStringConstant {
    EMPTY_STRING      (""),
    WHITE_SPACE       (" "),
    TAB               ("\t"),
    LINE_BREAK        ("\n"),
    COMMA             (","),
    PERIOD            ("."),
    QUESTION_MARK     ("?"),
    EXCLAMATION_MARK  ("!"),
    SINGLE_QUOTATION  ("'"),
    DOUBLE_QUOTATION  ("\""),
    SLASH             ("/"),
    BACK_SLASH        ("\\"),
    COLON             (":"),
    SEMICOLON         (";"),
    UNDERSCORE        ("_"),
    HYPHEN            ("-"),
    PLUS              ("+"),   // summation
    MINUS             ("-"),   // subtraction
    TIMES             ("*"),   // multiplication
    DIVIDED           ("/"),   // division
    LEFT_PARENTHESIS  ("("),
    RIGHT_PARENTHESIS (")"),
    SHARP             ("#"),
    NULL_STRING       ("(null)");

    /** The actual value of the string */
    private final String value;

    /**
     * Constructor.
     * 
     * @param value
     */
    BasicStringConstant(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

}
