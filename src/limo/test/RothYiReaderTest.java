package limo.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import limo.cluster.BrownWordCluster;
import limo.cluster.io.BrownWordClusterReader;
import limo.cluster.io.ScoreReader;
import limo.core.Mention;
import limo.core.Sentence;
import limo.core.interfaces.IRelationDocument;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.re.structured.BOW_M;
import limo.exrel.features.re.structured.BOW_Mwc;
import limo.io.ry.RothYihConll2004Document;
import limo.io.ry.RothYihConll2004Reader;

public class RothYiReaderTest {
	
	@Test
	public void testReadCorpusSkipSentencesWithRelations() throws IOException {
		String path = System.getProperty("user.home") + "/projects/limo-re/data/small_roth_yi.corp";
		
		RothYihConll2004Reader reader = new RothYihConll2004Reader(path);
		
		boolean skipSentencesWithoutRelation = true;
		ArrayList<IRelationDocument> documents = reader.readDocuments(skipSentencesWithoutRelation);
		assertEquals(1, documents.size());
		
		RothYihConll2004Document doc =  (RothYihConll2004Document) documents.get(0);
		assertEquals(2,doc.getNumRelations());
		
		assertEquals(2,doc.getNumSentences());

		assertEquals(8, doc.getNumMentions());
		
		for (Sentence s : doc.getSentences()) {
			System.out.println(s);
			System.out.println(s.getMentions().size());
			for (Mention m : s.getMentions()) {
				System.out.println(m);
			}
		}
		
	}
	
	@Test
	public void testReadCorpusDoNotSkipSentencesWithRelations() throws IOException {
		String path = System.getProperty("user.home") + "/projects/limo-re/data/small_roth_yi.corp";
		
		RothYihConll2004Reader reader = new RothYihConll2004Reader(path);
		
		boolean skipSentencesWithoutRelation = false;
		ArrayList<IRelationDocument> documents = reader.readDocuments(skipSentencesWithoutRelation);
		assertEquals(1, documents.size());
		
		RothYihConll2004Document doc =  (RothYihConll2004Document) documents.get(0);
		assertEquals(2,doc.getNumRelations());
		
		assertEquals(5,doc.getNumSentences());

		assertEquals(10, doc.getNumMentions());
		
		
	}

	@Test
	public void testBOWmarked() throws IOException {
		String path = System.getProperty("user.home") + "/projects/limo-re/data/small_roth_yi.corp";
		
		RothYihConll2004Reader reader = new RothYihConll2004Reader(path);
		
		boolean skipSentencesWithoutRelation = true;
		ArrayList<IRelationDocument> documents = reader.readDocuments(skipSentencesWithoutRelation);
		assertEquals(1, documents.size());
		
		RothYihConll2004Document doc =  (RothYihConll2004Document) documents.get(0);
		assertEquals(2,doc.getNumRelations());
		
		assertEquals(2,doc.getNumSentences());

		assertEquals(8, doc.getNumMentions());
		
		Sentence s = doc.getSentences().get(0);
		Mention mention1 = s.getMentions().get(0);
		Mention mention2 = s.getMentions().get(1);
		ParseTree tree = new ParseTree("(S1 (S (NP (PRP It)) (VP (VBZ describes) (SBAR (WHADVP (WRB how)) (S (NP (DT the) (JJ European) (NNS Governments)) (VP (VBP look) (S (VP (TO to) (VP (AUX be) (VP (VBG changing) (NP (NP (PRP$ their) (NN tune)) (PP (IN in) (NP (VBZ regards)))) (PP (TO to) (NP (NNP President) (NNP Bush))))))))))) (. .)))");
		BOW_M bb = new BOW_M();
		String bow = bb.extract(tree,mention1,mention2,null,s,null);
		assertEquals("(BOX (E1 European) (E2 Governments))",bow);
		
		mention2 = s.getMentions().get(3);
		bow = bb.extract(tree,mention1,mention2,null,s,null);
		assertEquals("(BOX (E1 European) (W Governments) (W look) (W to) (W be) (W changing) (W their) (W tune) (W in) (W regards) (W to) (E2 President))",bow);
	}
	
	
	@Test
	public void testBOW_BOXwc() throws IOException {
		String path = System.getProperty("user.home") + "/projects/limo-re/data/small_roth_yi2.corp";
		
		RothYihConll2004Reader reader = new RothYihConll2004Reader(path);
		
		boolean skipSentencesWithoutRelation = false;
		ArrayList<IRelationDocument> documents = reader.readDocuments(skipSentencesWithoutRelation);
		assertEquals(1, documents.size());
		
		RothYihConll2004Document doc =  (RothYihConll2004Document) documents.get(0);
		assertEquals(2,doc.getNumRelations());
		
		assertEquals(6,doc.getNumSentences());

		assertEquals(17, doc.getNumMentions());
		
		Sentence s = doc.getSentences().get(1);
		Mention mention1 = s.getMentions().get(0);
		Mention mention2 = s.getMentions().get(1);
		ParseTree tree = new ParseTree("(S1 (S (NP (PRP It)) (VP (VBZ describes) (SBAR (WHADVP (WRB how)) (S (NP (DT the) (JJ European) (NNS Governments)) (VP (VBP look) (S (VP (TO to) (VP (AUX be) (VP (VBG changing) (NP (NP (PRP$ their) (NN tune)) (PP (IN in) (NP (VBZ regards)))) (PP (TO to) (NP (NNP President) (NNP Bush))))))))))) (. .)))");
		BOW_M bb = new BOW_M();
		String bow = bb.extract(tree,mention1,mention2,null,s,null);
		assertEquals("(BOX (E1 European) (E2 Governments))",bow);
		
		mention2 = s.getMentions().get(3);
		bow = bb.extract(tree,mention1,mention2,null,s,null);
		assertEquals("(BOX (E1 European) (W Governments) (W look) (W to) (W be) (W changing) (W their) (W tune) (W in) (W regards) (W to) (E2 President))",bow);
	
		String clusterFile = System.getProperty("user.home") + "/projects/limo-re/wordcluster/ukwac_all_ace2005vocabOnly-c1000-p1.out/paths";
		
		BrownWordClusterReader wordClusterReader = (BrownWordClusterReader) ScoreReader.createScoreReader("Brown");
		
		wordClusterReader.startReading(clusterFile);
		
		BrownWordCluster wordCluster = (BrownWordCluster) wordClusterReader.createWordCluster();
		ArrayList<Object> resources = new ArrayList<Object>();
		resources.add(wordCluster);
		
		//BOWwc bbwc = new BOWwc();
		//String bowc = bbwc.extract(tree,mention1,mention2,null,s,resources);
		//assertEquals("(BOX (E1 1111101101) (E 1111101101) (W 1010101101) (W 100110) (W 1010010) (W 1010111101) (E 0011111) (W 1101011111) (W 10001110) (W 1010101011) (W 100110) (E2 1111101110))",bowc);
		
		
		s = doc.getSentences().get(5);
		mention1 = s.getMentions().get(0);
		mention2 = s.getMentions().get(1);
		tree = new ParseTree("(S1 (S (NP (NNP Governor) (NNP Mark)) (, ,) (NP (NN pudding) (CC and) (NN pie)) (VP (VP (NN Tax) (NP (DT those) (NNS citizens))) (CC and) (VP (VB make) (S (NP (PRP them)) (VP (VB cry) (SBAR (WHADVP (WRB When)) (S (NP (DT the) (NNS anti-taxers)) (VP (VBD came) (PRT (RP out)) (S (VP (TO to) (VP (VB play) (SBAR (S (NP (NNP Governor) (NNP Mark)) (VP (VBD ran) (PRT (RP away))))))))))))))) (. .)))");
			
		BOW_Mwc bbwc = new BOW_Mwc();
		String bowc = bbwc.extract(tree,mention1,mention2,null,s,resources);
		assertEquals("(BOX (E1 1111101110) (E2 111110001))",bowc);
			
		mention1 = s.getMentions().get(5);
		mention2 = s.getMentions().get(6);
		
		bowc = bbwc.extract(tree,mention1,mention2,null,s,resources);
		assertEquals("(BOX (E1 1111101110) (E2 111110001))",bowc);
	}
}
