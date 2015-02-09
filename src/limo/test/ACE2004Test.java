package limo.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;

import limo.core.Relation;
import limo.core.Relations;
import limo.core.interfaces.IRelation;
import limo.core.interfaces.IRelationDocument;
import limo.io.ace2004.ACE2004Reader;

import org.junit.Test;

public class ACE2004Test {

	//PLEASE SET IT TO THE HOME DIR WHERE YOU INSTALLED LIMO
	//public static String DIR = "/home/bplank/project/limosine/tools/limo/";
	//public static String DIR = System.getenv("LIMODIR"); //"/home/bplank/projects/limo-re/";
	public static String DIR = System.getProperty("user.home") +"/projects/limo-re/";
	
	
	@Test
	public void testCheckIfDirExists() {
		
		File dir = new File(DIR);
		assertEquals(true,dir.exists());
		
		if (dir.exists()==false)
			System.err.println("Directory does not exist! "+DIR);
	
	}
	
	@Test
	public void testReaderReadNumDocuments() {
		
		ACE2004Reader reader = new ACE2004Reader(DIR+"data/ACE2004Test_BNEWS_NWIRE/one");
		ArrayList<IRelationDocument> documents = reader.readDocuments();
		assertEquals(1, documents.size());
	
	}
	
	@Test
	public void testReaderNumRelations() {
		
		ACE2004Reader reader = new ACE2004Reader(DIR+"data/ACE2004Test_BNEWS_NWIRE/one",DIR+"ignoreRelations.txt");
		ArrayList<IRelationDocument> documents = reader.readDocuments();
		IRelationDocument doc = documents.get(0);
		assertEquals(22, doc.getNumRelations());
		
		Relations relations = doc.getRelations();
		for (IRelation ir : relations.get()) {
			Relation rel  = (Relation)ir;
			if (rel.getId().equals("ABC20001001.1830.0973-R4")) {
				assertEquals("jim sciutto", rel.getFirstMention().getHead());
				assertEquals("abc", rel.getSecondMention().getHead());
				assertEquals("EMP-ORG", rel.getRelationType());
			}
		}
	}
	
	@Test
	public void testReaderNumRelationsWithIgnoreFile() {
		
		ACE2004Reader reader = new ACE2004Reader(DIR+"data/ACE2004Test_BNEWS_NWIRE/two",DIR+"ignoreRelations.txt");
		ArrayList<IRelationDocument> documents = reader.readDocuments();
		IRelationDocument doc = documents.get(0);
		assertEquals(22, doc.getNumRelations());
		
		IRelationDocument doc2 = documents.get(1);
		assertEquals(53-4, doc2.getNumRelations()); //since 4 are ignored
		
		Relations relations = doc.getRelations();
		for (IRelation ir : relations.get()) {
			Relation rel  = (Relation)ir;
			//if (rel.getId().equals("ABC20001001.1830.0973-R4")) {
			if (rel.getId().equals("4-1")) {
				assertEquals("jim sciutto", rel.getFirstMention().getHead());
				assertEquals("abc", rel.getSecondMention().getHead());
				assertEquals("EMP-ORG", rel.getRelationType());
			}
		}
		
		relations = doc2.getRelations();
		for (IRelation ir : relations.get()) {
			Relation rel  = (Relation)ir;
			//if (rel.getId().equals("NYT20001127.2103.0306-R5")) {
			if (rel.getId().equals("5-1")) {
				assertEquals("Bush", rel.getFirstMention().getHead());
				assertEquals("the younger Bush", rel.getFirstMention().getExtend());
				assertEquals("chief-of-staff-in-waiting", rel.getSecondMention().getHead());
				assertEquals("PER-SOC", rel.getRelationType());
			}
		}
		
	
	}


}
