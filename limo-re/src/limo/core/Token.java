package limo.core;

import edu.stanford.nlp.ling.HasWord;

/**
 * A single token, implements Stanford's HasWord
 * since we use their tokenizer
 * 
 * @author Barbara Plank
 *
 */
public class Token implements HasWord {
	
	private static final long serialVersionUID = 1L;

	private int tokenId; //index of token in sentence
		
	private String value;
	
	// before tokenization info
	private Integer charseq_START;
	private Integer charseq_END;
	
	private String originalText;
	
	private String iob2 = "O";
	
	// annotation info (from XML files)
	private Integer annotation_charseq_START;
	private Integer annotation_charseq_END;
	
	private Sentence sentenceReference;

	public Token(String token, int index, Sentence sentence) {
		this.tokenId = index;
		this.value = token;
		this.sentenceReference = sentence;
	}

	// info from tokenizer
	public void setBeginCharPosition(int beginPosition) {
		this.charseq_START = beginPosition; 
	}
	
	public void setEndCharPosition(int endPosition) {
		this.charseq_END = endPosition;
	}
	
	public void setOriginalText(String originalText) {
		this.originalText = originalText; 
	}
	
	@Override
	public String toString() {
		if (charseq_START != null && annotation_charseq_START != null)
			return "["+tokenId+" "+value+ " start:"+ charseq_START + " end:"+ charseq_END + "| annotationStart: "+ annotation_charseq_START + " annotationEnd: "+annotation_charseq_END +" org:"+originalText +"]";
		else if (charseq_START != null)
			return "["+tokenId+" "+value+ " start:"+ charseq_START + " end:"+ charseq_END + " org:"+originalText +"]";
		else 
			return "["+tokenId+" "+value+"]";
	}

	public void setIndex(int index) {
		this.tokenId = index;
	}

	public String getValue() {
		return this.value;
	}

	public int getTokenId() {
		return tokenId;
	}

	public Integer getCharseq_START() {
		return charseq_START;
	}

	public Integer getCharseq_END() {
		return charseq_END;
	}

	public void setAnnotationStartIndex(int annotationTextIndex) {
		this.annotation_charseq_START = annotationTextIndex;
	}
	
	public void setAnnotationEndIndex(int annotationTextIndex) {
		this.annotation_charseq_END = annotationTextIndex;
	}

	public Integer getAnnotationStartIndex() {
		return annotation_charseq_START;
	}

	public Integer getAnnotationEndIndex() {
		return annotation_charseq_END;
	}

	public Sentence getSentenceReference() {
		return sentenceReference;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((annotation_charseq_END == null) ? 0
						: annotation_charseq_END.hashCode());
		result = prime
				* result
				+ ((annotation_charseq_START == null) ? 0
						: annotation_charseq_START.hashCode());
		result = prime * result
				+ ((charseq_END == null) ? 0 : charseq_END.hashCode());
		result = prime * result
				+ ((charseq_START == null) ? 0 : charseq_START.hashCode());
		result = prime * result
				+ ((originalText == null) ? 0 : originalText.hashCode());
		result = prime * result + tokenId;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Token other = (Token) obj;
		if (annotation_charseq_END == null) {
			if (other.annotation_charseq_END != null)
				return false;
		} else if (!annotation_charseq_END.equals(other.annotation_charseq_END))
			return false;
		if (annotation_charseq_START == null) {
			if (other.annotation_charseq_START != null)
				return false;
		} else if (!annotation_charseq_START
				.equals(other.annotation_charseq_START))
			return false;
		if (charseq_END == null) {
			if (other.charseq_END != null)
				return false;
		} else if (!charseq_END.equals(other.charseq_END))
			return false;
		if (charseq_START == null) {
			if (other.charseq_START != null)
				return false;
		} else if (!charseq_START.equals(other.charseq_START))
			return false;
		if (originalText == null) {
			if (other.originalText != null)
				return false;
		} else if (!originalText.equals(other.originalText))
			return false;
		if (tokenId != other.tokenId)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	//iob2 tagging setter/getters
	public void setBegin() {
        this.iob2 = "B";
	}

	public void setInside() {
		this.iob2 = "I";
	}

	public boolean isBegin() {
		if (iob2.equals("B"))
			return true;
		else
			return false;
	}

	public boolean isInside() {
		if (iob2.equals("I"))
			return true;
		else
			return false;
	}

	public boolean isOutside() {
		if (iob2.equals("O"))
			return true;
		else
			return false;
	}

	@Override
	public void setWord(String token) {
		this.value = token;
		
	}

	@Override
	public String word() {
		return this.value;
	}
	
	
}
