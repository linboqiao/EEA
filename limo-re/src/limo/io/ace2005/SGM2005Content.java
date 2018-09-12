package limo.io.ace2005;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * Class to handle SGM content
 * @author Barbara Plank
 *
 */
public class SGM2005Content {

	private String originalContent;
	private StringBuilder characterEncodingContent;
	private String plainContent;
	
	// map character indices from charEncodingContent to plainText indices
	private HashMap<Integer, Integer> annotation2plainIndices = new HashMap<Integer,Integer>();
	// map plainText indices to charEncodingContent indices
	private HashMap<Integer, Integer> plain2annotationIndices = new HashMap<Integer,Integer>();



	/*** 
	 * Reads SGM file, keeps original content,
	 * gets content without tags (for positioning) as well as
	 * plain content (content without tags and their content) for processing later
	 */
	public SGM2005Content(String url, String encoding) {
		StringBuilder docContent = new StringBuilder();

		try {
			BufferedReader inputReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(url), encoding));

			String line = inputReader.readLine();
			while (line != null) {
				docContent.append(line + " ");
				line = inputReader.readLine();
			}
			inputReader.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// keep original with all tags
		this.originalContent = docContent.toString();
		//one fix for parser: if fraction, add ? 
		this.originalContent = this.originalContent.replaceAll("(\\d+) (\\d+)\\/(\\d+)", "$1?$2/$3");
		// remove only tags to get positions for characters (as in annotation)
		this.characterEncodingContent = stripOffTagsForCharacterCounts(this.originalContent);
		// remove all to get plain text
		this.plainContent = stripOffTagsAndTagContent(this.originalContent);	
		//dictionary that holds index in charEncodingContent (which is annotated in ACE) and index to plain text
		this.createMappingDictionary(this.characterEncodingContent, this.plainContent);
	}

	/***
	 * Gets indices from plain text and from character encoding text
	 * Remember start and end indices of tokens
	 * and indices of all characters in between (because of punctuation not yet stripped off)
	 * @param characterEncodingContent
	 * @param plainText
	 */
	private void createMappingDictionary(
		StringBuilder characterEncodingContent, String plainText) {
		// get copy of charEncodingText
		StringBuilder copyContent = new StringBuilder(characterEncodingContent.toString());
		// charEnc is longer, contains also content of tags
		String[] tokens = plainText.split(" "); //split on whitespace
		//get first non-empty token and replace all up to that in copycontent with X
		String firstToken = "";
		for (String token : tokens) {
			if (token.length()>0) {
				firstToken = token;
				break;
			}
		}
		int indexFirstToken = copyContent.indexOf(firstToken);
		for (int i=0; i< indexFirstToken; i++) {
			copyContent = copyContent.replace(i,i+1,"X");
		}
		int currentIndex = 0; // index for plain text
		for (String token : tokens) {
			if (token.length() > 0) {
				int tokenStartIndex = copyContent.indexOf(token);
				int tokenEndIndex = tokenStartIndex + (token.length()-1);
				String dummy = createDummy(token.length());
				copyContent.replace(tokenStartIndex, tokenEndIndex+1, dummy);
				int plainTokenStartIndex = currentIndex;
				int plainTokenEndIndex = plainTokenStartIndex + (token.length()-1);
				annotation2plainIndices.put(tokenStartIndex, plainTokenStartIndex);
				annotation2plainIndices.put(tokenEndIndex, plainTokenEndIndex);
				plain2annotationIndices.put(plainTokenStartIndex, tokenStartIndex);
				plain2annotationIndices.put(plainTokenEndIndex, tokenEndIndex);
				// add all chars in between token start and end
				int j = plainTokenStartIndex;
				for (int i=tokenStartIndex; i < tokenEndIndex; i++) {
					annotation2plainIndices.put(i, j);
					plain2annotationIndices.put(j, i);
					j+=1;
				}
				currentIndex+=token.length();
			}
			currentIndex+=1;
			
		}
	}

	private String createDummy(int length) {
		StringBuilder b = new StringBuilder();
		for (int i =0; i < length; i++) {
			b.append("X");
		}
		return b.toString();
	}

	// remove all to get plain text
	private String stripOffTagsAndTagContent(String originalContent2) {
		// get all between text tags
		Pattern EXTRACTION_PATTERN = Pattern
			      .compile("<TEXT>(.*?)</TEXT>");
		 Matcher matcher = EXTRACTION_PATTERN.matcher(originalContent);
		 StringBuilder text = new StringBuilder();
		 if (matcher.find()) {
			 
			 for (int i = 1; i <= matcher.groupCount(); i++) {
	             if (matcher.group(i) != null) {
	            	text.append(matcher.group(i));
	             }
			 }
		}
		
		text = new StringBuilder(replaceWithinTagTextWithX(text.toString()));

		// replace <TURN> that and any other tag that may appear in text,
		//like <ANNOTATION> inaudible </ANNOTATION> with nothing (remove them)
		Pattern REMOVE_PATTERN = Pattern
	      .compile("(<ANNOTATION>.*?</ANNOTATION>)|(</?TURN>)|(<POST>)|(</POST>)|"+REPLACETAGSPATTERN+"|(\\(BEGIN VIDEOTAPE\\))|(\\(END VIDEOTAPE\\))"); 
		Matcher matcher_remove = REMOVE_PATTERN.matcher(text);
		String content =  matcher_remove.replaceAll("");
		
		return content.replaceAll("( {2,})", " ");
		
	}

	// remove only tags to get positions for characters (as in annotation)
	// note: replace poster etc with XXX 
	private StringBuilder stripOffTagsForCharacterCounts(String originalContent) {
		//mark beginning of text
		String contentMarkStartText = originalContent.replace("<TEXT>", "&&&&&&&&&&&");
	
		// remove poster's name, postdate, etc. 
		contentMarkStartText = replaceWithinTagTextWithX(contentMarkStartText);
		
		String contentNoTags = contentMarkStartText.replaceAll("<[^<]+>", "");
		StringBuilder sb = new StringBuilder(contentNoTags);
		// replace beginning up to &&&&&& with nothing
		int indexOfTextTag = sb.indexOf("&&&&&&&&&&&");
		for (int i=0; i < indexOfTextTag; i++) {
			sb.replace(i, i+1, "Z"); // replace metadata 
		}
		//remove again mark
		String replace = sb.toString().replace("&&&&&&&&&&&", "");
		replace = replace.replace("(BEGIN VIDEOTAPE)", "XXXXXXXXXXXXXXXXX");
		replace = replace.replace("(END VIDEOTAPE)", "XXXXXXXXXXXXXXX");
		return new StringBuilder(replace);
	}
	
	//private static String REPLACETAGSPATTERN = "(<POSTER>.*?</POSTER>)|(<POSTDATE>.*?</POSTDATE>)|(<SPEAKER>.*?</SPEAKER>)|(<SUBJECT>.*?</SUBJECT>)|(<QUOTE PREVIOUSPOST.*/>)";
	private static String REPLACETAGSPATTERN = "(<POSTER>.*?</POSTER>)|(<POSTDATE>.*?</POSTDATE>)|(<SPEAKER>.*?</SPEAKER>)|(<SUBJECT>.*?</SUBJECT>)|(<QUOTE PREVIOUSPOST[^>]*/>)";
	private static String[] REPLACETAGS = new String[]{"POSTER","POSTDATE","SPEAKER"};
	
	// replace poster's name (or other) with XXX
	private String replaceWithinTagTextWithX(String contentMarkStartText) {
			
		String textReplaced = contentMarkStartText;
		for (String TAGNAME : REPLACETAGS) {
			Pattern REMOVE = Pattern.compile("(<" + TAGNAME + ">)(.*?)(</"+ TAGNAME + ">)");
			Matcher matcher_tagname = REMOVE.matcher(contentMarkStartText);

			StringBuffer stringBuffer = new StringBuffer();
			
			while (matcher_tagname.find()) {
				String tag = matcher_tagname.group(2);
				StringBuffer replacement = new StringBuffer();
				for (int j = 0; j < tag.length(); j++) {
					replacement.append("X");
				}
				String newText = matcher_tagname.group(1)+ replacement.toString()+ matcher_tagname.group(3);
				matcher_tagname.appendReplacement(stringBuffer, newText);
				/*for (int i = 1; i <= matcher_tagname.groupCount(); i++) {
					String posterdate = matcher_tagname.group(i);
					StringBuffer replacement = new StringBuffer();
					for (int j = 0; j < posterdate.length(); j++) {
						replacement.append("X");
					}
					textReplaced = matcher_tagname.replaceAll(replacement.toString());
					//textReplaced = textReplaced.replace(posterdate,
					//		replacement.toString());

				}*/
			}
			matcher_tagname.appendTail(stringBuffer);
			textReplaced = stringBuffer.toString();
		}
		
		return textReplaced;
	}

	@Override
	public String toString() {
		return this.originalContent.toString();
	}
	
	public String getOriginalContent() {
		return originalContent;
	}

	public StringBuilder getCharacterEncodingContent() {
		return characterEncodingContent;
	}

	public String getPlainContent() {
		return plainContent;
	}

	/*** 
	 * Get sequence (from annotation)
	 * @param start
	 * @param end
	 * @return string 
	 */
	public String getSequence(int start, int end) {
		return this.characterEncodingContent.substring(start, end+1);
	} 
	public int getPlainTextIndex(int index) {
		return this.annotation2plainIndices.get(index);
	}
	public int getAnnotationTextIndex(int index) {
		return this.plain2annotationIndices.get(index);
	}

}
