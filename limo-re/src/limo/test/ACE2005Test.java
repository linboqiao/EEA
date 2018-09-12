package limo.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import limo.core.Relation;
import limo.core.Relations;
import limo.core.interfaces.IRelation;
import limo.core.interfaces.IRelationDocument;
import limo.io.ace2005.ACE2005Reader;

import org.junit.Test;

public class ACE2005Test {

	//public static String DIR = "/home/bplank/project/limosine/tools/limo/";
	public static String DIR = System.getProperty("user.home")+"/projects/limo-re/";
	
	@Test
	public void testReaderReadNumDocuments() throws Exception {
		
		ACE2005Reader reader = new ACE2005Reader(DIR+"data/ace2005/bc");
		ArrayList<IRelationDocument> documents = reader.readDocuments();
		assertEquals(52, documents.size());
	
	}
	
	@Test
	public void testReaderNumRelations() throws Exception {
		
		ACE2005Reader reader = new ACE2005Reader(DIR+"data/ace2005/bc", DIR+"ignoreRelationsACE2005.txt");
		ArrayList<IRelationDocument> documents = reader.readDocuments();
		IRelationDocument doc = documents.get(0);
		assertEquals("CNN_CF_20030303_1900_00.SGM",doc.getURI());
		
		assertEquals(24, doc.getNumRelations());
		
		Relations relations = doc.getRelations();
		for (IRelation ir : relations.get()) {
			Relation rel  = (Relation)ir;
			if (rel.getId().equals("CNN_CF_20030303.1900.00-R7-1")) {
				assertEquals("men", rel.getFirstMention().getHead());
				assertEquals("four heavily armed men", rel.getFirstMention().getExtend());
				assertEquals("boat", rel.getSecondMention().getHead());
				assertEquals("a 30-foot Cuban patrol boat with four heavily armed men",rel.getSecondMention().getExtend());
				assertEquals("ART", rel.getRelationType());
				
			}
		}
	}
	
	


}
