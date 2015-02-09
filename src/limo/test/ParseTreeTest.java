package limo.test;

import static org.junit.Assert.*;


import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.re.structured.RelationExtractionStructuredFeature;

import org.junit.Test;

import edu.stanford.nlp.ling.LabelFactory;
import edu.stanford.nlp.trees.PennTreeReaderFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;

public class ParseTreeTest {

	@Test
	public void testCreateParseTree() throws IOException {
		String parsed = "(S1 (S (NP (JJ Palestinian) (NNS leaders)) (VP (VBD accused) (NP (NNP Israel)) (PP (IN of) (S (VP (VBG choosing) (NP (NP (DT a) (NN path)) (PP (IN of) (NP (NN war)))) (PP (IN by) (S (VP (VP (VBG launching) (NP (DT the) (NNS strikes))) (CC and) (VP (VBD called) (PP (IN for) (NP (JJ international) (NN intervention))))))))))) (. .)))";
		ParseTree parseTree = new ParseTree(parsed);
		//String expected = "(S1 (S (NP (JJ Palestinian)(NNS leaders))(VP (VBD accused)(NP (NNP Israel))(PP (IN of)(S (VP (VBG choosing)(NP (NP (DT a)(NN path))(PP (IN of)(NP (NN war))))(PP (IN by)(S (VP (VP (VBG launching)(NP (DT the)(NNS strikes)))(CC and)(VP (VBD called)(PP (IN for)(NP (JJ international)(NN intervention)))))))))))(. .)))";
		String expected = "(S1 (S (NP (JJ Palestinian) (NNS leaders)) (VP (VBD accused) (NP (NNP Israel)) (PP (IN of) (S (VP (VBG choosing) (NP (NP (DT a) (NN path)) (PP (IN of) (NP (NN war)))) (PP (IN by) (S (VP (VP (VBG launching) (NP (DT the) (NNS strikes))) (CC and) (VP (VBD called) (PP (IN for) (NP (JJ international) (NN intervention))))))))))) (. .)))";
		assertEquals(expected, parseTree.toString());
		
		parsed = "   (S1      (S (NP (JJ Palestinian) 	(NNS leaders)) (VP (VBD accused) (NP (NNP Israel)) (PP (IN of) (S (VP (VBG choosing) (NP (NP (DT a) (NN path)) (PP (IN of) (NP (NN war)))) (PP (IN by) (S (VP (VP (VBG launching) (NP (DT the) (NNS strikes))) (CC and) (VP (VBD called) (PP (IN for) (NP (JJ international) (NN intervention))))))))))) (. .)))";
		parseTree = new ParseTree(parsed);
		//expected = "(S1 (S (NP (JJ Palestinian)(NNS leaders))(VP (VBD accused)(NP (NNP Israel))(PP (IN of)(S (VP (VBG choosing)(NP (NP (DT a)(NN path))(PP (IN of)(NP (NN war))))(PP (IN by)(S (VP (VP (VBG launching)(NP (DT the)(NNS strikes)))(CC and)(VP (VBD called)(PP (IN for)(NP (JJ international)(NN intervention)))))))))))(. .)))";
		expected = "(S1 (S (NP (JJ Palestinian) (NNS leaders)) (VP (VBD accused) (NP (NNP Israel)) (PP (IN of) (S (VP (VBG choosing) (NP (NP (DT a) (NN path)) (PP (IN of) (NP (NN war)))) (PP (IN by) (S (VP (VP (VBG launching) (NP (DT the) (NNS strikes))) (CC and) (VP (VBD called) (PP (IN for) (NP (JJ international) (NN intervention))))))))))) (. .)))";
		assertEquals(expected, parseTree.toString());
		
		try {
			ParseTree invalid = new ParseTree("(");
			assertNull(invalid);
		} catch (RuntimeException e) {
			System.err.println("ok");
		}
	}
	
//	@Test
//	public void testReadParseTrees() {
//		String parsed = "(NP (The) (house))";
//		ParseTree parseTree = new ParseTree(parsed);
//		//String expected = "(NP (The The)(house house))"; //parse tree doubles "emtpy" leaf nodes
//		String expected = "(NP (The)(house))"; //parse tree doubles "emtpy" leaf nodes
//		assertEquals(expected, parseTree.toString());
//		
//		parsed = "(NP (DT The) (NN house))";
//		parseTree = new ParseTree(parsed);
//		expected = "(NP (DT The)(NN house))"; //parse tree doubles "emtpy" leaf nodes
//		assertEquals(expected, parseTree.toString());
//	}
	
	@Test
	public void testGetTerminals() throws IOException {
		String parsed = "(S1 (S (NP (JJ Palestinian) (NNS leaders)) (VP (VBD accused) (NP (NNP Israel)) (PP (IN of) (S (VP (VBG choosing) (NP (NP (DT a) (NN path)) (PP (IN of) (NP (NN war)))) (PP (IN by) (S (VP (VP (VBG launching) (NP (DT the) (NNS strikes))) (CC and) (VP (VBD called) (PP (IN for) (NP (JJ international) (NN intervention))))))))))) (. .)))";
		ParseTree parseTree = new ParseTree(parsed);
		assertEquals(20,parseTree.getTerminals().size()); //includes puntuation
		assertEquals("intervention",parseTree.getTerminalSurface(18));
		assertEquals("leaders",parseTree.getTerminalSurface(1));
	}
	
	@Test
	public void testInsertNodes() throws IOException {
		String parsed = "(S1 (S (NP (JJ Palestinian) (NNS leaders)) (VP (VBD accused) (NP (NNP Israel)) (PP (IN of) (S (VP (VBG choosing) (NP (NP (DT a) (NN path)) (PP (IN of) (NP (NN war)))) (PP (IN by) (S (VP (VP (VBG launching) (NP (DT the) (NNS strikes))) (CC and) (VP (VBD called) (PP (IN for) (NP (JJ international) (NN intervention))))))))))) (. .)))";
		ParseTree parseTree = new ParseTree(parsed);
		int[] tokenIds1 = new int[] {0,1};
		int[] tokenIds2 = new int[]{3};
		String decoratedStr = parseTree.insertNodes(tokenIds1, "E1", tokenIds2, "E2");
		ParseTree decorated = new ParseTree(decoratedStr);
		//String expected = "(S1 (S (NP (E1 (JJ Palestinian)(NNS leaders)))(VP (VBD accused)(NP (E2 (NNP Israel)))(PP (IN of)(S (VP (VBG choosing)(NP (NP (DT a)(NN path))(PP (IN of)(NP (NN war))))(PP (IN by)(S (VP (VP (VBG launching)(NP (DT the)(NNS strikes)))(CC and)(VP (VBD called)(PP (IN for)(NP (JJ international)(NN intervention)))))))))))(. .)))";
		String expected = "(S1 (S (NP (E1 (JJ Palestinian) (NNS leaders))) (VP (VBD accused) (NP (E2 (NNP Israel))) (PP (IN of) (S (VP (VBG choosing) (NP (NP (DT a) (NN path)) (PP (IN of) (NP (NN war)))) (PP (IN by) (S (VP (VP (VBG launching) (NP (DT the) (NNS strikes))) (CC and) (VP (VBD called) (PP (IN for) (NP (JJ international) (NN intervention))))))))))) (. .)))";
		
		assertEquals(expected,decorated.toString());
		
		//if it's longer it will be ignored (same as before)
		tokenIds1 = new int[]{0,1,2};//ignores verb
		decoratedStr = parseTree.insertNodes(tokenIds1, "E1", tokenIds2, "E2");
		decorated = new ParseTree(decoratedStr);
		
		//TODO
		assertEquals(expected,decorated.toString());
		
		
	}
	
	@Test
	public void testStanfordTreebankReader() {
		/* Treebank tb = new MemoryTreebank();
		 
		 String treeFileName = "/home/bplank/project/limosine/tools/limo/run/data/BNEWS_NWIRE_test0/process/parsed_charniak/ABC20001121_1830_0865.SGM.txt.charniak.penn";

		if (treeFileName != null) {
			try {
				TreeReader tr = new PennTreeReader(
						new BufferedReader(new InputStreamReader(
								new FileInputStream(treeFileName))),
						new LabeledScoredTreeFactory());
				Tree t;
				while ((t = tr.readTree()) != null) {
					tb.add(t);
				}
			} catch (IOException e) {
				throw new RuntimeException("File problem: " + e);
			}

		}*/

		String parsed = "(S1 (S (NP (JJ Palestinian) (NNS leaders)) (VP (VBD accused) (NP (NNP Israel)) (PP (IN of) (S (VP (VBG choosing) (NP (NP (DT a) (NN path)) (PP (IN of) (NP (NN war)))) (PP (IN by) (S (VP (VP (VBG launching) (NP (DT the) (NNS strikes))) (CC and) (VP (VBD called) (PP (IN for) (NP (JJ international) (NN intervention))))))))))) (. .)))";

		Tree tree = Tree.valueOf(parsed);
		assertEquals(parsed, tree.toString());
		
		List<Tree> leaves = tree.getLeaves();
		
		Tree terminal = leaves.get(0);
		assertEquals("Palestinian",terminal.value());
		Tree parent = terminal.parent(tree).parent(tree);
		
		assertEquals("NP",parent.value());
	    //System.out.println(tree.toString());
	    
		TreeFactory tf = tree.treeFactory();
	    LabelFactory lf = tree.label().labelFactory();
	    List<Tree> children = parent.getChildrenAsList();
	    //create the new node, add node as child
	    Tree left = tf.newTreeNode(lf.newLabel("E1"), null);
	    for (int i = 0; i < children.size(); i++) {
	      left.addChild(children.get(i));
	    }

	    // remove all the two first children of t before
	    for (int i = 0; i < children.size(); i++) {
	      parent.removeChild(0);
	    }

	    // add XS as the first child
	    parent.addChild(0, left);
	    
	    //System.out.println(tree.toString());
		
	}
	
	@Test
	public void testInsertNodes1() throws IOException {
		String out = "(S1 (S (NP (NAC (NNP FIFE)(NNP LAKE)(, ,)(NNP Mich.))(NNP _)(EX There))(VP (AUX are)(NP (NP (DT no)(NNS teams))(PP (IN of)(NP (NNS lawyers))))(PP (IN in)(NP (DT this)(JJ sleepy)(JJ little)(NN hamlet))))(. .)))";
		int[] tokenIds1 = new int[] {0,1};
	
		ParseTree tree = new ParseTree(out);
		String result = tree.insertNode(tokenIds1, "*****E1*****");
		String expect =  "(S1 (S (NP (NAC (*****E1***** (NNP FIFE) (NNP LAKE)) (, ,) (NNP Mich.)) (NNP _) (EX There)) (VP (AUX are) (NP (NP (DT no) (NNS teams)) (PP (IN of) (NP (NNS lawyers)))) (PP (IN in) (NP (DT this) (JJ sleepy) (JJ little) (NN hamlet)))) (. .)))";
		assertEquals(expect, result);
		
	}
	
	@Test
	public void testInsertNodes2() throws IOException {
		String out = "(S1 (S (NP (NAC (NNP FIFE)(NNP LAKE)(, ,)(NNP Mich.))(NNP _)(EX There))(VP (AUX are)(NP (NP (DT no)(NNS teams))(PP (IN of)(NP (NNS lawyers))))(PP (IN in)(NP (DT this)(JJ sleepy)(JJ little)(NN hamlet))))(. .)))";
		int[] tokenIds1 = new int[] {0,1};
		int[] tokenIds2 = new int[]{3};
		ParseTree tree = new ParseTree(out);
		String result = tree.insertNodes(tokenIds1, "*****E1*****", tokenIds2, "*****E2*****");
		String expect =  "(S1 (S (NP (NAC (*****E1***** (NNP FIFE) (NNP LAKE)) (, ,) (*****E2***** (NNP Mich.))) (NNP _) (EX There)) (VP (AUX are) (NP (NP (DT no) (NNS teams)) (PP (IN of) (NP (NNS lawyers)))) (PP (IN in) (NP (DT this) (JJ sleepy) (JJ little) (NN hamlet)))) (. .)))";
		assertEquals(expect, result);
		
		
	}
	
	@Test
	public void testInsertNodes3() throws IOException {
		String out = "(S1 (S (NP (NAC (NNP FIFE)(NNP LAKE)(, ,)(NNP Mich.))(NNP _)(EX There))(VP (AUX are)(NP (NP (DT no)(NNS teams))(PP (IN of)(NP (NNS lawyers))))(PP (IN in)(NP (DT this)(JJ sleepy)(JJ little)(NN hamlet))))(. .)))";
			
		int[] tokenIds1 = new int[] {0,1};
		int[] tokenIds2 = new int[]{3};
		ParseTree tree = new ParseTree(out);
		String result = tree.insertNodes(tokenIds1, "ENTITY-INFOT1", tokenIds2, "ENTITY-INFOT2");
		String expect =  "(S1 (S (NP (NAC (ENTITY-INFOT1 (NNP FIFE) (NNP LAKE)) (, ,) (ENTITY-INFOT2 (NNP Mich.))) (NNP _) (EX There)) (VP (AUX are) (NP (NP (DT no) (NNS teams)) (PP (IN of) (NP (NNS lawyers)))) (PP (IN in) (NP (DT this) (JJ sleepy) (JJ little) (NN hamlet)))) (. .)))";
		assertEquals(expect, result);
		
		
	}
	
	@Test
	public void testParseTreeConstructor() throws IOException {
		String out = "(S (NP-XX (NNP John)) (VP-YY (VBZ walks)))";
		ParseTree t = new ParseTree(out);
		assertEquals(out, t.toString());
		
		String outStanford = "(S (NP-XX (NNP John)) (VP-YY (VBZ walks)))";
		Tree tree = Tree.valueOf(outStanford, new PennTreeReaderFactory());
		assertEquals(outStanford, tree.toString());
		
		tree = Tree.valueOf(outStanford); // if you don't use PennTreeReader it removes -XX
		assertNotSame(outStanford, tree.toString());
		
	}
	
	@Test
	public void testParseInsertNode24() throws IOException  {
		String in = "(S1 (S (NP (NP (DT The)(NN race))(PP (IN for)(NP (NN township)(NN supervisor))))(VP (AUX was)(NP (DT an)(JJ engaging)(CD one))(PP (IN from)(NP (DT the)(NN beginning)))(, ,)(SBAR (IN since)(S (NP (DT both)(NNP Stremlow)(CC and)(NNP Larson))(ADVP (RB already))(VP (VBP sit)(PP (IN on)(NP (NP (NP (DT the)(NN township)(POS 's))(NNP Board))(PP (IN of)(NP (NP (NNS Trustees))(VP (VBG _)(NP (NNP Stremlow))(PP (IN as)(NP (NP (NN supervisor))(CC and)(NP (NP (NNP Larson))(PP (IN as)(NP (NN township)(NN clerk)))))))))))))))(. .)))";
		int[] tokenIds1 = new int[] {4,5,6};
		int[] tokenIds2 = new int[] {0};
		ParseTree tree = new ParseTree(in);
		String out = "(S1 (S (NP (NP (ENTITY-INFOT2 (DT The)) (NN race)) (PP (IN for) (NP (NN township) (ENTITY-INFOT1 (NN supervisor))))) (VP (AUX was) (NP (DT an) (JJ engaging) (CD one)) (PP (IN from) (NP (DT the) (NN beginning))) (, ,) (SBAR (IN since) (S (NP (DT both) (NNP Stremlow) (CC and) (NNP Larson)) (ADVP (RB already)) (VP (VBP sit) (PP (IN on) (NP (NP (NP (DT the) (NN township) (POS 's)) (NNP Board)) (PP (IN of) (NP (NP (NNS Trustees)) (VP (VBG _) (NP (NNP Stremlow)) (PP (IN as) (NP (NP (NN supervisor)) (CC and) (NP (NP (NNP Larson)) (PP (IN as) (NP (NN township) (NN clerk))))))))))))))) (. .)))";
		String result = tree.insertNodes(tokenIds1, "ENTITY-INFOT1", tokenIds2, "ENTITY-INFOT2");
		assertEquals(out,result);
	}
	
	@Test
	public void testParseInsertNodeSmall() throws IOException {
		String in = "(S (NP (DT The)(NN race)))";
		int[] tokenIds1 = new int[] {0,1};
		ParseTree tree = new ParseTree(in);
		String out = "(S (NP (ENTITY (DT The) (NN race))))";
		String result = tree.insertNode(tokenIds1, "ENTITY");
		assertEquals(out,result);
	}
	
	@Test
	public void testParseInsertNodesSmallBoth() throws IOException  {
		String in = "(S (NP (DT The)(NN race)))";
		int[] tokenIds1 = new int[] {0};
		int[] tokenIds2 = new int[] {1};
		ParseTree tree = new ParseTree(in);
		String out = "(S (NP (ENTITY1 (DT The)) (ENTITY2 (NN race))))";
		String result = tree.insertNodes(tokenIds1, "ENTITY1", tokenIds2, "ENTITY2");
		assertEquals(out,result);
	}
	
	
	@Test
	public void testHideNodes() throws IOException  {
		
		Set<String> s  = new HashSet<String>();
		s.add("The");
		
		String p = "(S (NP (DT The) (NN race))(VP (VBZ begins)))";
		ParseTree tree = new ParseTree(p);
		
		for (Tree terminal : tree.getTerminals()) {
		
			if (s.contains(terminal.label().value())) {
				//terminal.hide();
				tree.hide(terminal);
				
			}
		}
		assertEquals("(S (NP (NN race)) (VP (VBZ begins)))", tree.toString());
		//assertEquals("(S (NP (DT The) (NN race)) (VP (VBZ begins)))",tree.toString());
		
		//remove np
		Tree np = tree.getNode(2);
		//assertEquals("(NP (DT The) (NN race))",np.toString());
		tree.hide(np);
		assertEquals("(S (VP (VBZ begins)))",tree.toString());
	}
	
	@Test
	public void testAncestor()  throws IOException {
		
		
		String p = "(S (NP (DT The) (NN race))(VP (VBZ begins)))";
		ParseTree tree = new ParseTree(p);
		Iterator<Tree> iter = tree.getIterator();
		Tree terminal = tree.getTerminals().get(1);
		
		while (iter.hasNext()) {
			Tree node = iter.next();
			if (node.toString().contains(terminal.toString())) {
				//System.out.println("ancester ? " +node + " -> " +terminal + " "+tree.anyAncestorOf(node, terminal));
				assertTrue(tree.anyAncestorOf(node, terminal));
			} else {
				assertFalse(tree.anyAncestorOf(node, terminal));
			}
		}
		
	}
	
	@Test
	public void testGetMCT() throws IOException  {
		
		
		String p = "(S (NP (DT The)(NN race)) (VP (VBZ begins)))";
		ParseTree tree = new ParseTree(p);
		String path = tree.MCT(0,1);
		assertEquals("(NP (DT The) (NN race))",path);
		
		path = tree.MCT(0,2);
		assertEquals("(S (NP (DT The) (NN race)) (VP (VBZ begins)))",path);
		
		path = tree.MCT(1,2);
		assertEquals("(S (NP (DT The) (NN race)) (VP (VBZ begins)))",path);
		
		p = "(S (NP (NP (DT The)(NN race)) (NP (DT The)(NN house))) (VP (VBZ begins)))";
		tree = new ParseTree(p);
		path = tree.MCT(0,2);
		assertEquals("(NP (NP (DT The) (NN race)) (NP (DT The) (NN house)))",path);
		
		path = tree.MCT(2,4);
		assertEquals("(S (NP (NP (DT The) (NN race)) (NP (DT The) (NN house))) (VP (VBZ begins)))",path);
		
	}	
	
	@Test
	public void testMCTsamesurface() throws IOException  {
		String p ="(S1 (S (NP (E-VEH (PRP it))) (VP (AUX was) (ADVP (RB hand)) (VP (VBN delivered) (PP (TO to) (NP (ENTITY-INFOT1 (JJ harrod)) (ENTITY-INFOT2 (NN harrod)) (POS 's))) (PP (IN by) (NP (NP (DT the) (JJ new) (JJ chief) (E-PER (JJ executive)) (E-PER (NNP nick) (NN paxton))) (SBAR (WHNP (E-PER (WP who))) (S (VP (MD would) (RB n't) (VP (VB trust) (NP (E-VEH (PRP$ its)) (NN futcher)) (PP (TO to) (NP (E-PER (NN anyone)) (RB else))))))))))) (. .)))";
		ParseTree tree = new ParseTree(p);
		String mct = tree.MCT(5,6);
		assertEquals("(NP (ENTITY-INFOT1 (JJ harrod)) (ENTITY-INFOT2 (NN harrod)) (POS 's))", mct);
		
	}
		
	@Test
	public void testGetPET()throws IOException  {
		String p = "(S (NP (DT The)(NN race)) (VP (VBZ begins)))";
		ParseTree tree = new ParseTree(p);
		
		String path = tree.PET(0,1);
		assertEquals("(NP (DT The) (NN race))",path);
	
		path = tree.PET(1,2);
		assertEquals("(S (NP (NN race)) (VP (VBZ begins)))",path);
		
		path = tree.PET(2,1);
		assertEquals("(S (NP (NN race)) (VP (VBZ begins)))",path);
		
		path = tree.PET(2,2);
		assertEquals("(VBZ begins)",path);
	}
	
	@Test
	public void testTheirSon()throws IOException  {
		String p = "(S (NP (PRP$ their)(NN son)) (VP (AUX was) (VP (VBN allowed) (S (VP (TO to) (VP (VB conduct) (PP (IN with) (`` ``) (NP (DT the) (E2 (NNP washington) (NN post))))))))))";
		ParseTree tree = new ParseTree(p);
		String path = tree.PET(0,1);
		assertEquals("(NP (PRP$ their) (NN son))",path);
		

		int[] tokenIds1 = new int[] {0};
		int[] tokenIds2 = new int[] {1};
		
		String decoratedStr = tree.insertNodes(tokenIds1, "E1", tokenIds2, "E2");
		ParseTree parseTreeDecorated = new ParseTree(decoratedStr);
		
		System.out.println(parseTreeDecorated);
		
	}
	
	@Test
	public void testPETwc() throws IOException {
		String t = "(S1 (S (ADVP (RB now)) (NP (PRP they)) (VP (AUX are) (VP (VBG learning) (NP (JJR more) (NNS details)) (SBAR (SBAR (IN that) (S (NP (NP (PRP$ their) (NN son)) (PP (IN in) (NP (NN fact)))) (VP (AUX was) (VP (VBN kicked) (CC and) (VBN beaten) (PP (IN after) (NP (PRP$ his) (NN capture))))))) (, ,) (CC and) (SBAR (IN that) (S (NP (PRP$ his) (JJ fellow) (NN pilot) (NNP david) (NNP williams)) (VP (AUX had) (NP (NP (DT a) (NN knife)) (VP (VBN held) (PP (TO to) (NP (PRP$ his) (NN throat))) (PP (ADVP (RB immediately)) (IN after) (NP (DT an) (NN tur))))) (, ,) (PP (VBG according) (PP (TO to) (NP (NP (DT an) (NN interview)) (SBAR (S (NP (PRP$ their) (NN son)) (VP (AUX was) (VP (VBN allowed) (S (VP (TO to) (VP (VB conduct) (PP (IN with) (`` ``) (NP (DT the) (NNP washington) (NN post)) ('' ''))))))))) (, ,) (SBAR (WHNP (WP who)) (S (VP (VBD flew) (PP (IN with) (NP (PRP him))) (PP (IN into) (NP (NNP kuwait))))))))))))))) (. .)))";
		
		String tmpFirstTarget = "ENTITY-INFOT1";
		String tmpSecondTarget = "ENTITY-INFOT2";
		
		int[] tokenIds1 = new int[] {42};
		int[] tokenIds2 = new int[] {43};
		
		ParseTree decorated = new ParseTree(t);
		String decoratedStr = decorated.insertNodes(tokenIds1, tmpFirstTarget, tokenIds2, tmpSecondTarget);
		ParseTree parseTreeDecorated = new ParseTree(decoratedStr);
		
		int spanTokenIdStart = RelationExtractionStructuredFeature.min(tokenIds1, tokenIds2);
		int spanTokenIdEnd = RelationExtractionStructuredFeature.max(tokenIds1, tokenIds2);
	
		String pet = parseTreeDecorated.getPathEnclosedTree(spanTokenIdStart,spanTokenIdEnd);
		
		
		String firstTarget = "E1";
		String secondTarget = "E2";
				
		pet = pet.replaceAll(tmpFirstTarget, firstTarget);
		pet = pet.replaceAll(tmpSecondTarget, secondTarget);
	
		ParseTree tree = new ParseTree(pet);
		
		for (Tree terminal : tree.getTerminals()) {
			
			String bitstring = "10101";
			Tree pos = terminal.parent(tree.getRootNode());
			Tree posParent = pos.parent(tree.getRootNode());
			if (bitstring != null) {
			    	String firstChild = bitstring;
			    	
					if (posParent.label().value().startsWith("E1") ||
						posParent.label().value().startsWith("E2")) { //smooth also E entities
				
						pos.setValue(firstChild); //for now add dummy pos
					}
			    	
			
			    }
			    
			}
			
		pet = tree.toString();
		ParseTree out = new ParseTree(pet);
		assertEquals("(NP (E1 (10101 their)) (E2 (10101 son)))",out.toString());
		
	}
	
	
	@Test
	public void testGetPETmore() throws IOException {
		String p ="(S1 (S (NP (PRP he)) (VP (AUX was) (VP (VBN arrested) (PP (IN in) (NP (NNP June))) (SBAR (IN after) (S (NP (NP (PRP he)) (NP (PRP he)) (: --)) (VP (VBD jumped) (NP (NN trial)) (PP (IN on) (NP (NN bail)))))))) (. .)))";
		ParseTree tree = new ParseTree(p);
		String expect = "(NP (NP (PRP he)) (NP (PRP he)) (: --))";
		String mct = tree.MCT(6,7);
		assertEquals(expect,mct);
		
		String pet = tree.PET(6,7);
		assertEquals("(NP (NP (PRP he)) (NP (PRP he)))",pet);
		
	}
	
	
	@Test
	public void testGetPETlarger() throws IOException  {
		String p = "(S (NP (NP (DT The)(NN race)) (NP (DT The)(NN house))) (VP (VBZ begins)))";
		ParseTree tree = new ParseTree(p);
		
		String path = tree.PET(0,1);
		assertEquals("(NP (DT The) (NN race))",path);
	
		path = tree.PET(1,2);
		assertEquals("(NP (NP (NN race)) (NP (DT The)))",path);
		
		path = tree.PET(2,1);
		assertEquals("(NP (NP (NN race)) (NP (DT The)))",path);
	}
	
	@Test
	public void testGetPETsameMentions() throws IOException  {
		ParseTree tree = new ParseTree("(S1 (S (NP (NNP Governor) (NNP Mark)) (, ,) (NP (NN pudding) (CC and) (NN pie)) (VP (VP (NN Tax) (NP (DT those) (NNS citizens))) (CC and) (VP (VB make) (S (NP (PRP them)) (VP (VB cry) (SBAR (WHADVP (WRB When)) (S (NP (DT the) (NNS anti-taxers)) (VP (VBD came) (PRT (RP out)) (S (VP (TO to) (VP (VB play) (SBAR (S (NP (NNP Governor) (NNP Mark)) (VP (VBD ran) (PRT (RP away))))))))))))))) (. .)))");
		
		
		String path = tree.PET(0,1);
		assertEquals("(NP (NNP Governor) (NNP Mark))",path);
	
		path = tree.PET(20,21);
		assertEquals("(NP (NNP Governor) (NNP Mark))",path);
		
	}
	
	@Test
	public void testInsertNodesAndGetPET() throws IOException {
		String out = "(S1 (S (NP (NAC (NNP FIFE)(NNP LAKE)(, ,)(NNP Mich.))(NNP _)(EX There))(VP (AUX are)(NP (NP (DT no)(NNS teams))(PP (IN of)(NP (NNS lawyers))))(PP (IN in)(NP (DT this)(JJ sleepy)(JJ little)(NN hamlet))))(. .)))";
		
		int[] tokenIds1 = new int[] {0,1};
		int[] tokenIds2 = new int[]{3};
		ParseTree tree = new ParseTree(out);
		String result = tree.insertNodes(tokenIds1, "ENTITY-INFOT1", tokenIds2, "ENTITY-INFOT2");
		String expect =  "(S1 (S (NP (NAC (ENTITY-INFOT1 (NNP FIFE) (NNP LAKE)) (, ,) (ENTITY-INFOT2 (NNP Mich.))) (NNP _) (EX There)) (VP (AUX are) (NP (NP (DT no) (NNS teams)) (PP (IN of) (NP (NNS lawyers)))) (PP (IN in) (NP (DT this) (JJ sleepy) (JJ little) (NN hamlet)))) (. .)))";
		assertEquals(expect, result);
		
		ParseTree parseTreeDecorated = new ParseTree(result);
		
		int spanTokenIdStart = RelationExtractionStructuredFeature.min(tokenIds1, tokenIds2);
		int spanTokenIdEnd = RelationExtractionStructuredFeature.max(tokenIds1, tokenIds2);
		
			
		String pet = parseTreeDecorated.getPathEnclosedTree(spanTokenIdStart,spanTokenIdEnd);
		//System.out.println(pet);
		assertEquals("(NAC (ENTITY-INFOT1 (NNP FIFE) (NNP LAKE)) (, ,) (ENTITY-INFOT2 (NNP Mich.)))", pet);
		
		tokenIds2 = new int[]{8};
		result = tree.insertNodes(tokenIds1, "ENTITY-INFOT1", tokenIds2, "ENTITY-INFOT2");
		expect =  "(S1 (S (NP (NAC (ENTITY-INFOT1 (NNP FIFE) (NNP LAKE)) (, ,) (NNP Mich.)) (NNP _) (EX There)) (VP (AUX are) (NP (NP (DT no) (ENTITY-INFOT2 (NNS teams))) (PP (IN of) (NP (NNS lawyers)))) (PP (IN in) (NP (DT this) (JJ sleepy) (JJ little) (NN hamlet)))) (. .)))";
		assertEquals(expect, result);
		
		parseTreeDecorated = new ParseTree(result);
		
		spanTokenIdStart = RelationExtractionStructuredFeature.min(tokenIds1, tokenIds2);
		spanTokenIdEnd = RelationExtractionStructuredFeature.max(tokenIds1, tokenIds2);
		
		//String mct = parseTreeDecorated.MCT(spanTokenIdStart,spanTokenIdEnd);
		pet = parseTreeDecorated.getPathEnclosedTree(spanTokenIdStart,spanTokenIdEnd);
		//System.out.println(mct);
		//System.out.println(pet);
		assertEquals("(S (NP (NAC (ENTITY-INFOT1 (NNP FIFE) (NNP LAKE)) (, ,) (NNP Mich.)) (NNP _) (EX There)) (VP (AUX are) (NP (NP (DT no) (ENTITY-INFOT2 (NNS teams))))))",pet);
	}
	
	@Test
	public void testInsertNodesAndGetPETIndexmoved() throws IOException {
		String out = "(S1 (S (NP (NAC (NNP FIFE)(NNP LAKE)(, ,)(NNP Mich.))(NNP _)(EX There))(VP (AUX are)(NP (NP (DT no)(NNS teams))(PP (IN of)(NP (NNS lawyers))))(PP (IN in)(NP (DT this)(JJ sleepy)(JJ little)(NN hamlet))))(. .)))";
		
		int[] tokenIds1 = new int[] {8};
		int[] tokenIds2 = new int[]{10};
		ParseTree tree = new ParseTree(out);
		String result = tree.insertNodes(tokenIds1, "ENTITY-INFOT1", tokenIds2, "ENTITY-INFOT2");
		
		ParseTree parseTreeDecorated = new ParseTree(result);
		
		int spanTokenIdStart = RelationExtractionStructuredFeature.min(tokenIds1, tokenIds2);
		int spanTokenIdEnd = RelationExtractionStructuredFeature.max(tokenIds1, tokenIds2);
		
			
		String pet = parseTreeDecorated.getPathEnclosedTree(spanTokenIdStart,spanTokenIdEnd);
		//System.out.println(pet);
		assertEquals("(NP (NP (ENTITY-INFOT1 (NNS teams))) (PP (IN of) (NP (ENTITY-INFOT2 (NNS lawyers)))))", pet);
		
		
	}
	
	@Test
	 public void testPETdecoratedZhangIMonlyETexample() throws IOException  {
		String p = "(S1 (S (NP (DT The) (`` `) (`` `) (NN poison) (NN pill)) (PRN (, ,) ('' ') ('' ') (S (VP (VBN ruled) (ADJP (JJ illegal)) (PP (IN in) (NP (NNP November))) (PP (IN by) (NP (NNP U.S.) (NNP District) (NNP G.) (NNP Ernest) (NNP Tidwell))))) (, ,)) (VP (MD would) (VP (VB become) (ADJP (JJ effective)) (SBAR (IN after) (S (NP (DT a) (NN shareholder)) (VP (AUX had) (VP (VBN acquired) (NP (NP (CD 10) (NN percent)) (PP (IN of) (NP (DT the) (JJ outstanding) (NN stock)))))))))) (. .)))";
		int[] tokenIds1 = new int[] {13};
		int[] tokenIds2 = new int[]{27,28};
		ParseTree tree = new ParseTree(p);
		String result = tree.insertNodes(tokenIds1, "E1", tokenIds2, "E2");
		System.out.println(tree.getTerminalsAsString());
		assertEquals("(S1 (S (NP (DT The) (`` `) (`` `) (NN poison) (NN pill)) (PRN (, ,) ('' ') ('' ') (S (VP (VBN ruled) (ADJP (JJ illegal)) (PP (IN in) (NP (NNP November))) (PP (IN by) (NP (E1 (NNP U.S.)) (NNP District) (NNP G.) (NNP Ernest) (NNP Tidwell))))) (, ,)) (VP (MD would) (VP (VB become) (ADJP (JJ effective)) (SBAR (IN after) (S (NP (DT a) (NN shareholder)) (VP (AUX had) (VP (VBN acquired) (NP (NP (E2 (CD 10) (NN percent))) (PP (IN of) (NP (DT the) (JJ outstanding) (NN stock)))))))))) (. .)))", result);
		
		int spanTokenIdStart = RelationExtractionStructuredFeature.min(tokenIds1, tokenIds2);
		int spanTokenIdEnd = RelationExtractionStructuredFeature.max(tokenIds1, tokenIds2);
		
		ParseTree parseTreeDecorated = new ParseTree(result);
		String pet = parseTreeDecorated.getPathEnclosedTree(spanTokenIdStart,spanTokenIdEnd);
		System.out.println(pet);
		
		
	}
	
	
}
