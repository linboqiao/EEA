package limo.exrel.features.re.linear;

import java.util.regex.Pattern;

import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.trees.Tree;
import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.constituency.ParseTree;

// local context
public class LC extends RelationExtractionLinearFeature {
/*
	Token. The token itself.
	Lemma. The lemma of the token.
	PoS. The PoS tag of the token.
	Stem. The stem of the token.
	Orthographic. This feature maps each token into equivalence classes that
	encode features such as capitalization, punctuation, numerals and so on.*/
	
	private static final String regexUpper ="^[A-Z0-9]"; //alpha-numeric uppercase
	public static boolean isUpperCase(String str){
	    return Pattern.compile(regexUpper).matcher(str).find();
	}
	
	private static final boolean startsWithUpperCase(String s) {
		char c = s.toCharArray()[0];
		if (Character.isLetter(c) && Character.isLowerCase(c)) {
			return false;

		}
		return true;
	}
	
	private static final String regexNumber ="^[0-9.]"; //numeric
	public static boolean isNumber(String str){
	    return Pattern.compile(regexNumber).matcher(str).find();
	}
	
	@Override
	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
			Relation relation, Sentence sentence, String groupId) {
		
		int[] tokens1 = mention1.getTokenIds();
		int[] tokens2 = mention2.getTokenIds();
		
			
		StringBuilder sb = new StringBuilder();
		
		int idx=0;
		for (int i : tokens1) {
			Tree terminal = parseTree.getTerminal(i);
			
			String word = sentence.getTokens().get(i).getValue();
			String tag = terminal.parent(parseTree.getRootNode()).value();
			
			addFeatures(1, idx, word,tag,sb);
			idx++;
		} 
		idx=0; //index within entity
		for (int i : tokens2) {
			Tree terminal = parseTree.getTerminal(i);
			
			String word = sentence.getTokens().get(i).getValue();
			String tag = terminal.parent(parseTree.getRootNode()).value();
			
			addFeatures(2, idx, word,tag,sb);
			idx++;
		} 
		return sb.toString();
		
	}

	private void addFeatures(int t,int i, String word, String tag,StringBuilder sb) {
		sb.append("E"+t+i+"w:"+word);
		sb.append(RelationExtractionLinearFeature.BOWseparator);
		
		sb.append("E"+t+i+"s:"+Morphology.stemStatic(word, tag));
		sb.append(RelationExtractionLinearFeature.BOWseparator);
		
		boolean lowercase=false;
		sb.append("E"+t+i+"l:"+Morphology.lemmaStatic(word, tag, lowercase));
		sb.append(RelationExtractionLinearFeature.BOWseparator);
		
		sb.append("E"+t+i+"p:"+tag);
		sb.append(RelationExtractionLinearFeature.BOWseparator);
		
		sb.append("E"+t+i+"allU"+isUpperCase(word));
		sb.append(RelationExtractionLinearFeature.BOWseparator);
		
		sb.append("E"+t+i+"startU"+startsWithUpperCase(word));
		sb.append(RelationExtractionLinearFeature.BOWseparator);
		
		sb.append("E"+t+i+"NUM"+isNumber(word));
		sb.append(RelationExtractionLinearFeature.BOWseparator);
		
	}

}
