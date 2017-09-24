package utils;


import java.util.List;

//import edu.stanford.nlp.ling.CoreLabel;

public class StringMethods {
	/**
	 * 
	 * @param text1
	 * @return the substring (the smaller of the two texts) or null if there is no substring match
	 */
	public static String substringMatch(String text1, String text2){
		boolean debug  = false;
		if(text1.length() > text2.length()){
			if( text1.indexOf(text2) != -1){
				return text2;
			}
		}
		if(text2.length() > text1.length()){
			if( text2.indexOf(text1) != -1){
				return text1;
			}
		}
		return null;
	}

//	public static String sentenceString(List<CoreLabel> sentence){
//		StringBuffer sb = new StringBuffer();;
//		for (CoreLabel word : sentence){
//			sb.append(" "+word.current());
//		}
//		return sb.toString().trim();
//	}
}
