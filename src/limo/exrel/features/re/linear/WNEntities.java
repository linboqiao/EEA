//package limo.exrel.features.re.linear;
//
//import java.util.regex.Pattern;
//
//import rita.wordnet.RiWordnet;
//
//import edu.stanford.nlp.process.Morphology;
//import edu.stanford.nlp.trees.Tree;
//import limo.core.Sentence;
//import limo.core.Mention;
//import limo.core.Relation;
//import limo.core.trees.constituency.ParseTree;
//
//// local context
//public class WNEntities extends RelationExtractionLinearFeature {
///*
//	hypernyms of entities 
//	*/
//	
//	@Override
//	protected String _extract(ParseTree parseTree, Mention mention1, Mention mention2,
//			Relation relation, Sentence sentence, String groupId) {
//		
//		int[] tokens1 = mention1.getTokenIds();
//		int[] tokens2 = mention2.getTokenIds();
//		
//		RiWordnet wordnet = new RiWordnet();
//			
//		StringBuilder sb = new StringBuilder();
//		
//		int idx=0;
//		for (int i : tokens1) {
//			Tree terminal = parseTree.getTerminal(i);
//			
//			String word = sentence.getTokens().get(i).getValue();
//			String tag = terminal.parent(parseTree.getRootNode()).value();
//			
//			addFeatures(1, idx, word,tag,sb,wordnet);
//			idx++;
//		} 
//		idx=0; //index within entity
//		for (int i : tokens2) {
//			Tree terminal = parseTree.getTerminal(i);
//			
//			String word = sentence.getTokens().get(i).getValue();
//			String tag = terminal.parent(parseTree.getRootNode()).value();
//			
//			addFeatures(2, idx, word,tag,sb,wordnet);
//			idx++;
//		} 
//		return sb.toString();
//		
//	}
//	/*RiWordnet.NOUN  ->NN*
//    RiWordnet.VERB  -> VB*
//    RiWordnet.ADJ   -> JJ*
//    RiWordnet.ADV   -> RB*
//     Valid wordnet parts-of-speech include (noun="n",verb="v",adj="a", and adverb="r"). */
//	private void addFeatures(int t,int i, String word, String tag,StringBuilder sb, RiWordnet wordnet) {
//		String pos;
//		if (tag.startsWith("VB"))
//			pos = "v";
//		else if (tag.startsWith("NN"))
//			pos = "n";
//		else if (tag.startsWith("JJ"))
//			pos = "a";
//		else if (tag.startsWith("RB"))
//			pos = "r";
//		else
//			return; //don't consider others
//		
//		
//		String[] sim = wordnet.getAllHypernyms(word, pos);
//		if (sim!=null)
//			for (String hypernym : sim) {
//				sb.append("E"+t+"h:"+hypernym);
//				sb.append(RelationExtractionLinearFeature.BOWseparator);
//			}
//	}
//
//}
