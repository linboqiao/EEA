package limo.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import limo.core.trees.constituency.ParseTree;
import limo.core.trees.dependency.DependencyInstance;
import limo.core.trees.dependency.io.CONLL07Reader;

import org.junit.Test;

import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;


public class DependencyInstanceTest {

	@Test 
	public void testReadFile() throws IOException {
		String file = ACE2004Test.DIR +"data/test.boh.bohnet";
		List<GrammaticalStructure> parses = EnglishGrammaticalStructure.readCoNLLXGrammaticStructureCollection(file);
		
		assertEquals(2,parses.size());
	}
	
	@Test 
	public void testGetSentences() throws IOException {

		CONLL07Reader reader = new CONLL07Reader(false);
		reader.startReading( ACE2004Test.DIR +"data/test.boh.bohnet");
			
		ArrayList<DependencyInstance> instances = reader.getNext();
		DependencyInstance first = instances.get(0);
		assertEquals("In Washington , U.S. officials are working overtime .",first.getSentenceSurface());
		
		instances = reader.getNext();
		DependencyInstance second = instances.get(0);
		assertEquals("Leaders of the European Union meet in Zagreb today .", second.getSentenceSurface());
	}
	
	/*	@Test
	public void testGetEntireNestedTree() throws IOException {

		CONLL07Reader reader = new CONLL07Reader(false);
		reader.startReading( ACE2004Test.DIR +"data/test.boh.bohnet");
			
		ArrayList<DependencyInstance> instances = reader.getNext();
		DependencyInstance first = instances.get(0);
		
		String entireTreeNested = "(ROOT (working) (prep (In) (pobj (Washington)))(punct (,))(nsubj (officials) (nn (U.S.)))(aux (are))(dobj (overtime))(punct (.)))";
		//assertEquals(entireTreeNested,first.getNestedTree(first.getSemanticGraph()));
		System.out.println(first.getNestedTree(first.getSemanticGraph()));
		
		
		instances = reader.getNext();
		DependencyInstance second = instances.get(0);
		
		entireTreeNested = "(ROOT (meet) (nsubj (Leaders) (prep (of) (pobj (Union) (det (the))(nn (European)))))(prep (in) (pobj (Zagreb)))(tmod (today))(punct (.)))";
		//assertEquals(entireTreeNested,second.getNestedTree(second.getSemanticGraph()));
		System.out.println(second.getNestedTree(second.getSemanticGraph()));
		
	}	*/
	
	@Test
	public void testNestedTree() throws IOException {
		CONLL07Reader reader = new CONLL07Reader(false);
		reader.startReading( ACE2004Test.DIR +"data/test.boh.bohnet");
			
		ArrayList<DependencyInstance> instances = reader.getNext();
		DependencyInstance depTree = instances.get(0);
		assertEquals("In Washington , U.S. officials are working overtime .",depTree.getSentenceSurface());
		
		int[] tokenIds1 = new int[]{3};
		int[] tokenIds2 = new int[] {4};
		SemanticGraph semGraph = depTree.getPath(tokenIds1, tokenIds2, depTree.getCollapsedSemanticGraph()); 
		semGraph = depTree.decorateDependency(tokenIds1, "target", "E1", semGraph);
		semGraph = depTree.decorateDependency(tokenIds2, "target", "E2", semGraph);
		
		
		String path = depTree.getNestedTree(semGraph,DependencyInstance.Output.WORD_BELOW_DEP);		
		System.out.println(path);
		ParseTree p = new ParseTree(path);
		
		for (Tree l : p.getTerminals())
			System.out.println(l.label().value());
		
	}	
	
	/*@Test
	public void testGetEntireNestedTreeCollapsed() throws IOException {

		CONLL07Reader reader = new CONLL07Reader(false);
		reader.startReading( ACE2004Test.DIR +"data/test.boh.bohnet");
			
		ArrayList<DependencyInstance> instances = reader.getNext();
		DependencyInstance first = instances.get(0);
		
		String entireTreeNested = "(ROOT (working) (prep_in (Washington))(punct (,))(nsubj (officials) (nn (U.S.)))(aux (are))(dobj (overtime))(punct (.)))";
		assertEquals(entireTreeNested,first.getNestedTree(first.getCollapsedSemanticGraph()));
		
		instances = reader.getNext();
		DependencyInstance second = instances.get(0);
		
		entireTreeNested = "(ROOT (meet) (nsubj (Leaders) (prep_of (Union) (det (the))(nn (European))))(prep_in (Zagreb))(tmod (today))(punct (.)))";
		assertEquals(entireTreeNested,second.getNestedTree(second.getCollapsedSemanticGraph()));
		
	}	*/
	
	@Test
	public void testGetNestedTreeOnlyPath() throws IOException {
		CONLL07Reader reader = new CONLL07Reader(false);
		reader.startReading( ACE2004Test.DIR +"data/test.boh.bohnet");
			
		ArrayList<DependencyInstance> instances = reader.getNext();
		DependencyInstance depTree = instances.get(0);
		
		int[] tokenIds1 = new int[]{1};//Washington
		int[] tokenIds2 = new int[]{3,4};//U.S. officials
		SemanticGraph semGraph = depTree.getPath(tokenIds1, tokenIds2, depTree.getSemanticGraph()); 
		semGraph = depTree.decorateDependency(tokenIds1, "target", "E1", semGraph);
		semGraph = depTree.decorateDependency(tokenIds2, "target", "E2", semGraph);
		
		String path = depTree.getNestedTree(semGraph);
		System.out.println(">>>>> "+path);
		System.out.println();
			
	}
	
	@Test
	public void testGetNestedTreeEntireButMarkPath() throws IOException {
		CONLL07Reader reader = new CONLL07Reader(false);
		reader.startReading( ACE2004Test.DIR +"data/test.boh.bohnet");
			
		ArrayList<DependencyInstance> instances = reader.getNext();
		DependencyInstance depTree = instances.get(0);
		
		int[] tokenIds1 = new int[]{1};//Washington
		int[] tokenIds2 = new int[]{3,4};//U.S. officials
		SemanticGraph pathSemGraph = depTree.getPath(tokenIds1, tokenIds2, depTree.getSemanticGraph()); 
		pathSemGraph = depTree.decorateDependency(tokenIds1, "target", "E1", pathSemGraph);
		pathSemGraph = depTree.decorateDependency(tokenIds2, "target", "E2", pathSemGraph);
		
		//SemanticGraph entireSemGraph = depTree.getSemanticGraph();
		
		
		//String path = depTree.getNestedTree(entireSemGraph, semGraph);
		
			
	}
	
	
	/*@Test
	public void testGetEntireDependencyWordTree() throws IOException {

		CONLL07Reader reader = new CONLL07Reader(false);
		reader.startReading( ACE2004Test.DIR +"data/test.boh.bohnet");
			
		ArrayList<DependencyInstance> instances = reader.getNext();
		DependencyInstance first = instances.get(0);
		
		String expected = "(working (In (Washington))(,)(officials (U.S.))(are)(overtime)(.))";
		assertEquals(expected,first.getDependencyWordTree());
		
		instances = reader.getNext();
		DependencyInstance second = instances.get(0);
		
		expected = "(meet (Leaders (of (Union (the)(European))))(in (Zagreb))(today)(.))";
		assertEquals(expected,second.getDependencyWordTree());
		
	}	
	
	@Test
	public void testGetEntireDependencyWordTreeAndDecorate() throws IOException {

		CONLL07Reader reader = new CONLL07Reader(false);
		reader.startReading( ACE2004Test.DIR +"data/test.boh.bohnet");
			
		ArrayList<DependencyInstance> instances = reader.getNext();
		DependencyInstance first = instances.get(0);
		
		String undecorated = first.getDependencyWordTree();
		String expected = "(working (In (Washington))(,)(officials (U.S.))(are)(overtime)(.))";
		assertEquals(expected, undecorated);
		int[] t1 = new int[]{1};
		int[] t2 = new int[]{4};
		first.decorateTokens(t2, "E1");
		first.decorateTokens(t1, "E2");
		
		expected = "(working (In (Washington=E2))(,)(officials=E1 (U.S.))(are)(overtime)(.))";
		assertEquals(expected,first.getDependencyWordTree());
	    
		instances = reader.getNext();
		DependencyInstance second = instances.get(0);
		
		t1 = new int[]{3,4};
		t2 = new int[]{7};
		second.decorateTokens(t1, "E1",true);
		second.decorateTokens(t2, "E2",true);
		
		expected = "(meet (Leaders (of (Union=E1 (the)(European))))(in (Zagreb=E2))(today)(.))";
		
		assertEquals(expected,second.getNestedTree(true));
		
	}	

*/	
	/*@Test
	public void testGRCT() throws IOException {
		String file = ACE2004Test.DIR +"data/ACE2004Test_BNEWS_NWIRE/two/process/parsed_bohnet/ABC20001001_1830_0973.SGM.txt.bohnet";
		List<GrammaticalStructure> parses = EnglishGrammaticalStructure.readCoNLLXGrammaticStructureCollection(file);
		
		assertEquals(28,parses.size());
		
		CONLL07Reader reader = new CONLL07Reader(false);
		reader.startReading( ACE2004Test.DIR +"data/ACE2004Test_BNEWS_NWIRE/two/process/parsed_bohnet/ABC20001001_1830_0973.SGM.txt.bohnet");
			
		ArrayList<DependencyInstance> instances = reader.getNext();
		DependencyInstance firstDepTree = instances.get(0);
		instances = reader.getNext();
		DependencyInstance nextDepTree = instances.get(0);
		instances = reader.getNext();
		DependencyInstance nextnextDepTree = instances.get(0);
		instances = reader.getNext();
		DependencyInstance nextnextnextDepTree = instances.get(0);
		instances = reader.getNext();
		instances = reader.getNext();
		instances = reader.getNext();
		instances = reader.getNext();
		instances = reader.getNext();
		instances = reader.getNext();
		instances = reader.getNext();
		
		
		instances = reader.getNext();
		
		DependencyInstance dep = instances.get(0);
		System.out.println(dep);
		
		assertEquals("stores are expanding .",dep.getSentenceSurface());
		String expectedGRCT = "(ROOT (nsubj (stores))(aux (are))(expanding)(punct (.)))";
		assertEquals(expectedGRCT,dep.getGRCT(false));
		String expectedGRCTwPos = "(ROOT (nsubj (NNS stores))(aux (VBP are))(VBG expanding)(punct (. .)))";
		assertEquals(expectedGRCTwPos,dep.getGRCT(true));
		
		String c = dep.getGRCT(true);
		System.out.println(c);
		//System.out.println(c.replaceAll("\\(", "[.").replaceAll("\\)", " ] "));
		ParseTree cTree = new ParseTree(c);
		assertEquals("stores are expanding .",cTree.getTerminalsAsString());
		
		dep.printConll();
		
		instances = reader.getNext();
		DependencyInstance depTree = instances.get(0);
		
		
	
		//GRCT: grammatical role centered tree
		String expectedGRCT_noPos = "(ROOT (nsubjpass (amod (new))(apartments)(cc (and))(conj (houses)))(aux (are))(auxpass (being))(built)(punct (.)))";
		assertEquals(expectedGRCT_noPos,depTree.getGRCT());
		
		
		//test if leaves are in order!
		String grct_as_tree = depTree.getGRCT(true);
		System.out.println("tree: "+grct_as_tree);
		String grct_withPos = "(ROOT (nsubjpass (amod (JJ new))(NNS apartments)(cc (CC and))(conj (NNS houses)))(aux (VBP are))(auxpass (VBG being))(VBN built)(punct (. .)))";
		assertEquals(grct_withPos,depTree.getGRCT(true));
		
		ParseTree tree = new ParseTree(grct_as_tree);
		String expectedSent = "new apartments and houses are being built .";
		System.out.println("terminals: "+ tree.getTerminalsAsString());
		assertEquals(expectedSent, tree.getTerminalsAsString());
	
		String expectDepWords =  "(built (apartments (new)(and)(houses))(are)(being)(.))";
		assertEquals(expectDepWords, depTree.getDependencyWordTree());
		
		String expectDeps = "(ROOT (nsubjpass (amod)(cc)(conj))(aux)(auxpass)(punct))";
		assertEquals(expectDeps, depTree.getDependencyTree());
		
		//first depTree
		System.out.println("GRCT: "+firstDepTree.getGRCT(true));
		tree = new ParseTree(firstDepTree.getGRCT(true));
		System.out.println(firstDepTree.getGRCT(true));
		String sent = "like many heartland states , iowa has had trouble keeping young people down on the farm or anywhere within state lines .";
		//assertEquals(sent, tree.getTerminalsAsString());
		
		tree = new ParseTree(nextDepTree.getGRCT(true));
		System.out.println(nextDepTree.getGRCT(true));
		String sentNext = "with population waning , the state is looking beyond its borders for newcomers .";
		//assertEquals(sentNext,tree.getTerminalsAsString());
		
		tree = new ParseTree(nextnextDepTree.getGRCT(true));
		System.out.println(nextnextDepTree.getGRCT(true));
		String sentNextnext = "as abc 's jim sciutto reports , one little town may provide a big lesson .";
		assertEquals(sentNextnext,tree.getTerminalsAsString());
		
		tree = new ParseTree(nextnextnextDepTree.getGRCT(true));
		System.out.println("***");
		System.out.println(nextnextnextDepTree.toString());
		System.out.println(nextnextnextDepTree.getGRCT(true));
		String sentNextnextnext = "on homecoming night postville feels like hometown , usa , but a look around this town of 2,000 shows it 's become a miniature ellis island .";
		assertEquals(sentNextnextnext,tree.getTerminalsAsString());
		
	}*/

}
