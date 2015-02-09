//package limo.test;
//
//import static org.junit.Assert.*;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//import limo.core.trees.constituency.ParseTree;
//import limo.core.trees.dependency.DependencyInstance;
//import limo.core.trees.dependency.io.CONLL07Reader;
//
//import org.junit.Test;
//
//
//import edu.stanford.nlp.ling.CoreLabel;
//import edu.stanford.nlp.ling.IndexedWord;
//import edu.stanford.nlp.process.CoreLabelTokenFactory;
//import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
//import edu.stanford.nlp.trees.GrammaticalStructure;
//import edu.stanford.nlp.trees.TypedDependency;
//import edu.stanford.nlp.trees.semgraph.SemanticGraph;
//import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;
//import edu.stanford.nlp.trees.semgraph.SemanticGraphFactory;
//
//
//public class DepTreeTest {
//
//	
//	@Test
//	public void testCONLL07ReaderAndNestedTree() throws IOException {
//		CONLL07Reader reader = new CONLL07Reader(false);
//		reader.startReading( ACE2004Test.DIR +"data/ACE2004Test_BNEWS_NWIRE/two/process/parsed_bohnet/ABC20001001_1830_0973.SGM.txt.bohnet");
//			
//		ArrayList<DependencyInstance> instances = reader.getNext();
//		DependencyInstance depTree = instances.get(0);
//		assertEquals(22,depTree.getNumTypedDependencies());
//		assertEquals(19,depTree.getNumTypedDependenciesCollapsed());
//		System.out.println(depTree.getNestedTree());
//		String expect = "(ROOT (had (prep (like (pobj (states (amod (many))(nn (heartland))))))(punct (,))(nsubj (iowa))(aux (has))(dobj (trouble))(xcomp (keeping (dobj (people (amod (young))))(advmod (down (prep (on (pobj (farm (det (the))))(cc (or))(conj (within (advmod (anywhere))(pobj (lines (nn (state))))))))))))(punct (.))))";
//		assertEquals(expect,depTree.getNestedTree());
//		String expectDWT = "(had (like (states (many)(heartland)))(,)(iowa)(has)(trouble)(keeping (people (young))(down (on (farm (the))(or)(within (anywhere)(lines (state))))))(.))";
//		assertEquals(expectDWT,depTree.getDependencyWordTree());
//	}
//	
//	
//	
//	@Test
//	public void testOkNestedTree() throws IOException {
//		CONLL07Reader reader = new CONLL07Reader(false);
//		reader.startReading(ACE2004Test.DIR +"data/tt.conll");
//			
//		ArrayList<DependencyInstance> instances = reader.getNext();
//		DependencyInstance depTree = instances.get(0);
//		assertEquals(2,depTree.getNumTypedDependencies());
//		assertEquals(2,depTree.getNumTypedDependenciesCollapsed());
//		System.out.println(depTree.getNestedTree());
//		String expect = "(ROOT (Ok (punct (.))))";
//		assertEquals(expect,depTree.getNestedTree());
//		
//		instances = reader.getNext();
//		depTree = instances.get(0);
//		assertEquals(10,depTree.getNumTypedDependencies());
//		expect = "(ROOT (know (nsubj (We (det (all))))(ccomp (served (complm (that))(nsubj (Kerry (nn (John))))(prep (in (pobj (Vietnam))))))(punct (.))))";
//		assertEquals(expect,depTree.getNestedTree());
//		
//		instances = reader.getNext();
//		depTree = instances.get(0);
//		expect = "(ROOT (acd (punct (-))(punct (]))))";
//		assertEquals(3,depTree.getNumTypedDependencies());
//		assertEquals(expect,depTree.getNestedTree());
//		
//		instances = reader.getNext();
//		depTree = instances.get(0); //tree with only 2 roots!
//		expect ="(TOP (ROOT (Ok ))(ROOT (. )))";
//		assertEquals(2,depTree.getNumTypedDependencies());
//		assertEquals(expect,depTree.getNestedTree());
//		
//		instances = reader.getNext();
//		depTree = instances.get(0); //tree with only 2 roots!
//		expect = "(ROOT (] (punct (acd))(punct (-))))";
//		assertEquals(3,depTree.getNumTypedDependencies());
//		
//		instances = reader.getNext();
//		depTree = instances.get(0); //tree with no "root" label and no "0" head
//		expect = "(ROOT (this (dep (is (punct (extreme))))))";
//		assertEquals(expect, depTree.getNestedTree());
//	}
//	
//	@Test
//	public void testGettingErrorInvalidParentId() throws IOException {
//		
//		CONLL07Reader reader = new CONLL07Reader(false);
//		reader.startReading( ACE2004Test.DIR +"run/data/BNEWS_NWIRE_train1234/process/parsed_bohnet/CNN20001010_1400_0358.SGM.txt.bohnet");
//		
//		
//		ArrayList<DependencyInstance> instances;
//		while ((instances = reader.getNext()) != null) {
//			DependencyInstance depTree = instances.get(0);
//			System.out.println(depTree.toString());
//			System.out.println(depTree.getNestedTree());
//		}
//	}
//	
//
//	@Test
//	public void testGetNextedTree() throws IOException {
//		String file = ACE2004Test.DIR +"data/ACE2004Test_BNEWS_NWIRE/two/process/parsed_bohnet/ABC20001001_1830_0973.SGM.txt.bohnet";
//		List<GrammaticalStructure> parses = EnglishGrammaticalStructure.readCoNLLXGrammaticStructureCollection(file);
//		
//		assertEquals(28,parses.size());
//		
//		//nested trees
//		String expected = "(ROOT (built (nsubjpass (apartments (amod (new))(cc (and))(conj (houses))))(aux (are))(auxpass (being))(punct (.))))";
//		String expectedPos = "(ROOT (VBN (built (nsubjpass (NNS (apartments (amod (JJ (new)))(cc (CC (and)))(conj (NNS (houses))))))(aux (VBP (are)))(auxpass (VBG (being)))(punct (. (.))))))";
//		
//		CONLL07Reader reader = new CONLL07Reader(false);
//		reader.startReading( ACE2004Test.DIR +"data/ACE2004Test_BNEWS_NWIRE/two/process/parsed_bohnet/ABC20001001_1830_0973.SGM.txt.bohnet");
//			
//		ArrayList<DependencyInstance> instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		DependencyInstance depTree = instances.get(0);
//		
//		assertEquals(expected, depTree.getNestedTree());
//		assertEquals(expectedPos, depTree.getNestedTree(true));
//	
//	}
//	
//	@Test
//	public void testGRCT() throws IOException {
//		String file = ACE2004Test.DIR +"data/ACE2004Test_BNEWS_NWIRE/two/process/parsed_bohnet/ABC20001001_1830_0973.SGM.txt.bohnet";
//		List<GrammaticalStructure> parses = EnglishGrammaticalStructure.readCoNLLXGrammaticStructureCollection(file);
//		
//		assertEquals(28,parses.size());
//		
//		CONLL07Reader reader = new CONLL07Reader(false);
//		reader.startReading( ACE2004Test.DIR +"data/ACE2004Test_BNEWS_NWIRE/two/process/parsed_bohnet/ABC20001001_1830_0973.SGM.txt.bohnet");
//			
//		ArrayList<DependencyInstance> instances = reader.getNext();
//		DependencyInstance firstDepTree = instances.get(0);
//		instances = reader.getNext();
//		DependencyInstance nextDepTree = instances.get(0);
//		instances = reader.getNext();
//		DependencyInstance nextnextDepTree = instances.get(0);
//		instances = reader.getNext();
//		DependencyInstance nextnextnextDepTree = instances.get(0);
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		instances = reader.getNext();
//		DependencyInstance depTree = instances.get(0);
//		
//	
//		//GRCT: grammatical role centered tree
//		String expectedGRCT_noPos = "(ROOT (nsubjpass (amod (new))(apartments)(cc (and))(conj (houses)))(aux (are))(auxpass (being))(built)(punct (.)))";
//		assertEquals(expectedGRCT_noPos,depTree.getGRCT());
//		
//		
//		//test if leaves are in order!
//		String grct_as_tree = depTree.getGRCT(true);
//		System.out.println("tree: "+grct_as_tree);
//		String grct_withPos = "(ROOT (nsubjpass (amod (JJ new))(NNS apartments)(cc (CC and))(conj (NNS houses)))(aux (VBP are))(auxpass (VBG being))(VBN built)(punct (. .)))";
//		assertEquals(grct_withPos,depTree.getGRCT(true));
//		
//		ParseTree tree = new ParseTree(grct_as_tree);
//		String expectedSent = "new apartments and houses are being built .";
//		System.out.println("terminals: "+ tree.getTerminalsAsString());
//		assertEquals(expectedSent, tree.getTerminalsAsString());
//	
//		String expectDepWords =  "(built (apartments (new)(and)(houses))(are)(being)(.))";
//		assertEquals(expectDepWords, depTree.getDependencyWordTree());
//		
//		String expectDeps = "(ROOT (nsubjpass (amod)(cc)(conj))(aux)(auxpass)(punct))";
//		assertEquals(expectDeps, depTree.getDependencyTree());
//		
//		//first depTree
//		System.out.println("GRCT: "+firstDepTree.getGRCT(true));
//		tree = new ParseTree(firstDepTree.getGRCT(true));
//		System.out.println(firstDepTree.getGRCT(true));
//		String sent = "like many heartland states , iowa has had trouble keeping young people down on the farm or anywhere within state lines .";
//		//assertEquals(sent, tree.getTerminalsAsString());
//		
//		tree = new ParseTree(nextDepTree.getGRCT(true));
//		System.out.println(nextDepTree.getGRCT(true));
//		String sentNext = "with population waning , the state is looking beyond its borders for newcomers .";
//		//assertEquals(sentNext,tree.getTerminalsAsString());
//		
//		tree = new ParseTree(nextnextDepTree.getGRCT(true));
//		System.out.println(nextnextDepTree.getGRCT(true));
//		String sentNextnext = "as abc 's jim sciutto reports , one little town may provide a big lesson .";
//		assertEquals(sentNextnext,tree.getTerminalsAsString());
//		
//		tree = new ParseTree(nextnextnextDepTree.getGRCT(true));
//		System.out.println("***");
//		System.out.println(nextnextnextDepTree.toString());
//		System.out.println(nextnextnextDepTree.getGRCT(true));
//		String sentNextnextnext = "on homecoming night postville feels like hometown , usa , but a look around this town of 2,000 shows it 's become a miniature ellis island .";
//		assertEquals(sentNextnextnext,tree.getTerminalsAsString());
//		
//	}
//	
//	
//	
//	@Test
//	public void testSemGraphIndexNodes() throws IOException {
//		/*String tLPP = "edu.stanford.nlp.parser.lexparser.EnglishTreebankParserParams";
//	    TreebankLangParserParams params = ReflectionLoading.loadByReflection(tLPP);
//		String conllXFileName = "/home/bplank/test.conll";
//		List<GrammaticalStructure> listGs = params.readGrammaticalStructureFromFile(conllXFileName);*/	
//
//		List<GrammaticalStructure> listGs = EnglishGrammaticalStructure.readCoNLLXGrammaticStructureCollection( ACE2004Test.DIR +"data/test.conll");
//			
//		assertEquals(1,listGs.size());
//		
//		GrammaticalStructure gs = listGs.get(0);
//				
//		Collection<TypedDependency> all = gs.typedDependencies();
//		int allDeps = all.size();
//		Collection<TypedDependency> coll = gs.typedDependenciesCollapsed(); //modifies all in-place!	
//		assertNotSame(allDeps,coll.size());
//	
//		String docId = "d1";
//		int sentIndex=0;
//		SemanticGraph semgraph = SemanticGraphFactory.makeFromTree(gs, docId, sentIndex);
//		
//		assertEquals("The",semgraph.getNodeByIndex(1).value());
//		assertEquals("luxury",semgraph.getNodeByIndex(2).value());
//		assertEquals("U.S.",semgraph.getNodeByIndex(12).value());
//	}
//	
//	@Test
//	public void testGetPathSingleTokens() throws IOException {
//	
//		CONLL07Reader reader = new CONLL07Reader(false);
//		reader.startReading( ACE2004Test.DIR +"data/ACE2004Test_BNEWS_NWIRE/two/process/parsed_bohnet/ABC20001001_1830_0973.SGM.txt.bohnet");
//			
//		ArrayList<DependencyInstance> instances = reader.getNext();
//		DependencyInstance depTree = instances.get(0);
//		
//		//heartland states
//		int[] tokenIds1 = new int[]{2};
//		int[] tokenIds2 = new int[]{3};
//		depTree.decorateDependency(tokenIds1, "E1");
//		depTree.decorateDependency(tokenIds2, "E2");
//		depTree.getPath(tokenIds1, tokenIds2); //alters semgraph!
//		System.out.println(depTree.getNestedTree());
//		
//	}
//	
//	@Test
//	public void testGetPathMultipleTokens() throws IOException {
//	
//		CONLL07Reader reader = new CONLL07Reader(false);
//		reader.startReading( ACE2004Test.DIR +"data/ACE2004Test_BNEWS_NWIRE/two/process/parsed_bohnet/ABC20001001_1830_0973.SGM.txt.bohnet");
//			
//		ArrayList<DependencyInstance> instances = reader.getNext();
//		DependencyInstance depTree = instances.get(0);
//		
//		//heartland states
//		int[] tokenIds1 = new int[]{2,3};
//		int[] tokenIds2 = new int[]{5};
//		depTree.decorateDependency(tokenIds1, "E1");
//		depTree.decorateDependency(tokenIds2, "E2");
//		depTree.getPath(tokenIds1, tokenIds2); //alters semgraph!
//		System.out.println(depTree.getNestedTree());
//		String expected = "(ROOT (had (prep (like (pobj-E1 (states (nn (heartland))))))(nsubj-E2 (iowa))))";
//		String expectedPos = "(ROOT (VBN (had (prep (IN (like (pobj-E1 (NNS (states (nn (NN (heartland)))))))))(nsubj-E2 (NNP (iowa))))))";
//		assertEquals(expected,depTree.getNestedTree());
//		assertEquals(expectedPos,depTree.getNestedTree(true));
//		
//	}
//	
//	@Test
//	public void testGetShortestPath() throws IOException {
//		List<GrammaticalStructure> listGs = EnglishGrammaticalStructure.readCoNLLXGrammaticStructureCollection( ACE2004Test.DIR +"data/test.conll");
//		GrammaticalStructure gs = listGs.get(0);
//		SemanticGraph semgraph = SemanticGraphFactory.makeFromTree(gs, "d2", 0);
//		List<SemanticGraphEdge> path = semgraph.getShortestUndirectedPathEdges(semgraph.getNodeByIndex(4), semgraph.getNodeByIndex(12));
//		for (SemanticGraphEdge e : path)
//			System.out.println(e);
//		
//		SemanticGraph semGraphPath = SemanticGraphFactory.makeFromEdges(path);
//		semGraphPath.prettyPrint();
//		IndexedWord word1 = semGraphPath.getNodeByIndex(4);
//		IndexedWord parentWord1 = semGraphPath.getParent(word1);
//		SemanticGraphEdge edgeWord1 = semGraphPath.getEdge(parentWord1, word1);
//		//edgeWord1.setRelation(new GrammaticalRelation(Language.English,edgeWord1.getRelation().getShortName()+"-E1"));
//		System.out.println(parentWord1 + " " + word1 + " :" +edgeWord1);
//		
//		CoreLabelTokenFactory fac = new CoreLabelTokenFactory();
//		CoreLabel cl1 = fac.makeToken("E1",-1,2);
//		cl1.setIndex(1001);
//		IndexedWord e1 = new IndexedWord(cl1);
//		
//		semGraphPath.prettyPrint();
//		
//		semGraphPath.addVertex(e1);
//		semGraphPath.addEdge(parentWord1, e1, edgeWord1.getRelation(), 1);
//		semGraphPath.addEdge(e1, word1, edgeWord1.getRelation(),1);
//		semGraphPath.removeEdge(edgeWord1);
//		
//		semGraphPath.prettyPrint();
//		
//		System.out.println();
//		DependencyInstance depInst = new DependencyInstance();
//		depInst.setSemanticGraph(semGraphPath);
//		depInst.getNestedTree();
//		System.out.println(depInst.getNestedTree(true));
//	}
//	
///*	@Test
//	public void testGetShortestPathFromDependencyInstanceAndDecoratedDependency() throws IOException {
//		CONLL07Reader reader = new CONLL07Reader(false);
//		reader.startReading( ACE2004Test.DIR +"data/ACE2004Test_BNEWS_NWIRE/two/process/parsed_bohnet/ABC20001001_1830_0973.SGM.txt.bohnet");
//			
//		ArrayList<DependencyInstance> instances = reader.getNext();
//		DependencyInstance depTree = instances.get(0);
//				
//		int[] tokenIds1 = new int[] {2};
//		int[] tokenIds2 = new int[] {3};
//		depTree.decorateDependency(tokenIds1, "E1");
//		depTree.decorateDependency(tokenIds2, "E2");
//		
//		String expected = "(ROOT (had (prep (like (pobj-E2 (states (amod (many))(nn-E1 (heartland))))))(punct (,))(nsubj (iowa))(aux (has))(dobj (trouble))(xcomp (keeping (dobj (people (amod (young))))(advmod (down (prep (on (pobj (farm (det (the))))(cc (or))(conj (within (advmod (anywhere))(pobj (lines (nn (state))))))))))))(punct (.))))";
//		assertEquals(expected,depTree.getNestedTree());
//		
//		depTree.getPath(tokenIds1,tokenIds2);
//		System.out.println(depTree.getNestedTree());
//		
//	}*/
///*	
//	@Test
//	public void testGetShortestPathFromDependencyInstanceAndDecoratedWords() throws IOException {
//		CONLL07Reader reader = new CONLL07Reader(false);
//		reader.startReading( ACE2004Test.DIR +"data/ACE2004Test_BNEWS_NWIRE/two/process/parsed_bohnet/ABC20001001_1830_0973.SGM.txt.bohnet");
//			
//		ArrayList<DependencyInstance> instances = reader.getNext();
//		DependencyInstance depTree = instances.get(0);
//				
//		int[] tokenIds1 = new int[] {2};
//		int[] tokenIds2 = new int[] {3};
//		depTree.decorateTokens(tokenIds1, "E1");
//		depTree.prettyPrintSemGraph();
//		System.out.println(depTree.getNestedTree());
//		depTree.decorateTokens(tokenIds2, "E2");
//		depTree.prettyPrintSemGraph();
//		System.out.println(depTree.getNestedTree());
//		
//		String expected = "(ROOT (had (prep (like (pobj (states-E2 (amod (many))(nn (heartland-E1))))))(punct (,))(nsubj (iowa))(aux (has))(dobj (trouble))(xcomp (keeping (dobj (people (amod (young))))(advmod (down (prep (on (pobj (farm (det (the))))(cc (or))(conj (within (advmod (anywhere))(pobj (lines (nn (state))))))))))))(punct (.))))";
//		assertEquals(expected,depTree.getNestedTree());
//	
//		
//	}*/
//	
//	@Test(expected=IllegalArgumentException.class)
//	public void testAccessInvalidIndex(){
//		List<GrammaticalStructure> listGs;
//		try {
//			listGs = EnglishGrammaticalStructure.readCoNLLXGrammaticStructureCollection( ACE2004Test.DIR +"data/test.conll");
//
//			assertEquals(1,listGs.size());
//			
//			GrammaticalStructure gs = listGs.get(0);
//			SemanticGraph semgraph = SemanticGraphFactory.makeFromTree(gs, "d1", 0);
//			
//			semgraph.getNodeByIndex(100);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//	}
//
//}
