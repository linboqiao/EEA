package limo.test;

import static org.junit.Assert.*;

import limo.core.Mention;
import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IRelation;

import org.junit.Test;

public class SentenceTest {

	@Test
	public void testCreateSentenceFromPlainInput() {
		Sentence sentence = Sentence.createSentenceFromPlainInput(0, "Run your test via Eclipse");
		assertEquals(0, sentence.getSentenceId());
		assertEquals("Run/O your/O test/O via/O Eclipse/O",sentence.getAnnotatedSentence());
		assertEquals("Run your test via Eclipse",sentence.getPlainSentence());
		
		sentence = Sentence.createSentenceFromPlainInput(1, "   Run   your test 	via Eclipse   ");
		assertEquals("Run your test via Eclipse",sentence.getPlainSentence());
		
		assertEquals(4,sentence.getTokens().get(4).getTokenId());
	}

	@Test
	public void testCreateSentenceFromNERtaggedInput() {
		//test simple annotation
		String sentenceNER = "John/PER from/O   Trento/LOC   ";
		Sentence sentence = Sentence.createSentenceFromNERtaggedInput(2, sentenceNER);
		assertEquals("John from Trento", sentence.getPlainSentence());
		
		assertEquals("John", sentence.getTokens().get(0).getValue());
		assertEquals("Trento", sentence.getTokens().get(2).getValue());
		
		assertEquals(sentence.getMentions().get(0).getHead(), "John");
		assertEquals("PER", sentence.getMentions().get(0).getEntityReference().getType());
		int[] tokenIds = new int[]{0};
		assertArrayEquals(sentence.getMentions().get(0).getTokenIds(), tokenIds);
		
		tokenIds = new int[]{2};
		assertArrayEquals(sentence.getMentions().get(1).getTokenIds(), tokenIds);
		assertEquals(sentence.getMentions().get(1).getHead(), "Trento");
		
		//test IOB annotation
		String sentenceNER_IOB = "John/B-PER from/O   New/B-LOC York/I-LOC US/B-LOC ";
		sentence = Sentence.createSentenceFromNERtaggedInput(2, sentenceNER_IOB);
		assertEquals("John from New York US", sentence.getPlainSentence());
		
		assertEquals(3,sentence.getMentions().size());
		assertEquals("John", sentence.getMentions().get(0).getHead());
		assertEquals("New York", sentence.getMentions().get(1).getHead());
		tokenIds = new int[]{2,3};
		assertArrayEquals(tokenIds, sentence.getMentions().get(1).getTokenIds());
		assertEquals("US", sentence.getMentions().get(2).getHead());
		
		//test simple but with consecutive NERs (get merged)
		sentenceNER_IOB = "John/PER from/O New/LOC York/LOC US/LOC ";
		sentence = Sentence.createSentenceFromNERtaggedInput(2, sentenceNER_IOB);
		assertEquals("John from New York US", sentence.getPlainSentence());
		assertEquals(2,sentence.getMentions().size());
		
		assertEquals("John", sentence.getMentions().get(0).getHead());
		assertNotSame("New York", sentence.getMentions().get(1).getHead());
		assertEquals("New York US", sentence.getMentions().get(1).getHead());
	}
	
	@Test
	public void testCreateSentenceFromNERtaggedInputIncludingMention() {
		//test simple annotation
		String sentenceNER = "John/PER_NAM from/O   Trento/LOC_NOM   ";
		Sentence sentence = Sentence.createSentenceFromNERtaggedInput(2, sentenceNER);
		assertEquals("John from Trento", sentence.getPlainSentence());
		
		assertEquals("John", sentence.getTokens().get(0).getValue());
		assertEquals("Trento", sentence.getTokens().get(2).getValue());
		
		assertEquals(sentence.getMentions().get(0).getHead(), "John");
		assertEquals("PER", sentence.getMentions().get(0).getEntityReference().getType());
		assertEquals("NAM", sentence.getMentions().get(0).getType());
		int[] tokenIds = new int[]{0};
		assertArrayEquals(sentence.getMentions().get(0).getTokenIds(), tokenIds);
		
		tokenIds = new int[]{2};
		assertArrayEquals(sentence.getMentions().get(1).getTokenIds(), tokenIds);
		assertEquals(sentence.getMentions().get(1).getHead(), "Trento");
		
		//test IOB annotation
		String sentenceNER_IOB = "John/B-PER_NAM from/O   New/B-LOC_NOM York/I-LOC_NOM US/B-LOC ";
		sentence = Sentence.createSentenceFromNERtaggedInput(2, sentenceNER_IOB);
		assertEquals("John from New York US", sentence.getPlainSentence());
		
		assertEquals(3,sentence.getMentions().size());
		assertEquals("John", sentence.getMentions().get(0).getHead());
		assertEquals("New York", sentence.getMentions().get(1).getHead());
		assertEquals("NOM", sentence.getMentions().get(1).getType());
		assertEquals("LOC", sentence.getMentions().get(1).getEntityReference().getType());
		tokenIds = new int[]{2,3};
		assertArrayEquals(tokenIds, sentence.getMentions().get(1).getTokenIds());
		assertEquals("US", sentence.getMentions().get(2).getHead());
		

	}

	@Test
	public void testCreateRelationsFromClassifierOutput() throws Exception {
		String s1 = "John/O Smith/B-PERSON from/O Boulder/O ,/O Colorado/B-LOCATION ./O";
		String s2 = "John/O Smith/B-PERSON@R1_GPE-AFF_ARG1 from/O Boulder/B-LOCATION@R1_GPE-AFF_ARG2 ,/O Colorado/O ./O";
		String s3 = "John/O Smith/O from/O Boulder/B-LOCATION@R1_PHYS_ARG1 ,/O Colorado/B-LOCATION@R1_PHYS_ARG2 ./O";
		
		Sentence s = Sentence.createSentenceFromRelationTaggedInput(0, s1);
		s = Sentence.createSentenceFromRelationTaggedInput(0, s2);
		s = Sentence.createSentenceFromRelationTaggedInput(0, s3);
		
		for (Relation r : s.getRelationsAsList()) {
			System.out.println(r);
			
		}
	}
	
/*	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testCreateSentenceFromNERtaggedInputAccessNonExistingMention() throws IndexOutOfBoundsException {
	    String sentenceNER_IOB = "John/PER from/O New/LOC York/LOC US/LOC ";
		Sentence sentence = Sentence.createSentenceFromNERtaggedInput(2, sentenceNER_IOB);
		sentence.getMentions().get(2);
		
		//thrown.expectMessage(JUnitMatchers.containsString("index"));
	}
*/	
	@Test
	public void testCreateSentenceFromRelationTaggedInput() throws Exception {
		
		String sentenceStr = "The/O Okogbe/O road/O tanker/O explosion/O occurred/O on/O 12/O July/O 2012/O when/O a/O road/O tanker/LOCATION@R1_PHYS_ARG1 in/O Okobie/O ,/O Nigeria/LOCATION@R1_PHYS_ARG2 fell/O into/O a/O ditch/O and/O exploded/O ./O";
		Sentence s = Sentence.createSentenceFromRelationTaggedInput(0, sentenceStr);
		
		assertEquals(1, s.getRelations().size());
		for (IRelation ir : s.getRelationsAsList()) {
			Relation relation = (Relation)ir;
			assertEquals("tanker", relation.getFirstMention().getHead());
			assertEquals("Nigeria", relation.getSecondMention().getHead());
		}
	}
	
	@Test
	public void testGetAnnotatedSentence() throws Exception {
		String taggedSentence = "The/O Okogbe/O road/O tanker/O explosion/O occurred/O on/O 12/O July/O 2012/O when/O a/O road/O tanker/B-LOCATION@R1_PHYS_ARG1 in/O Okobie/O ,/O Nigeria/B-LOCATION@R1_PHYS_ARG2 fell/O into/O a/O ditch/O and/O exploded/O ./O";
		Sentence s = Sentence.createSentenceFromRelationTaggedInput(0, taggedSentence);
		assertEquals(taggedSentence,s.getAnnotatedSentence());
		Relations relations = s.getRelations();
		assertEquals(1, relations.size());
		Relation rel = (Relation)s.getRelationsAsList().get(0);
		assertEquals("PHYS",rel.getRelationType());
		assertEquals("tanker",rel.getFirstMention().getHead());
		assertEquals("Nigeria", rel.getSecondMention().getHead());
	}
	
	@Test
	public void testGetAnnotatedSentenceWithMoreThanOneRelation() throws Exception {
		String sentenceRel = "John/B-PER@R1_PHYS_ARG1 from/O   New/B-LOC@R1_PHYS_ARG2|R2_PHYS_ARG1 York/I-LOC@R1_PHYS_ARG2|R2_PHYS_ARG1 US/B-LOC@R2_PHYS_ARG2 ";
		Sentence s = Sentence.createSentenceFromRelationTaggedInput(1, sentenceRel);
		Relations relations = s.getRelations();
		assertEquals(2, relations.size());
		Relation rel = (Relation)s.getRelationsAsList().get(1);
		assertEquals("PHYS",rel.getRelationType());
		assertEquals("John",rel.getFirstMention().getHead());
		assertEquals("New York", rel.getSecondMention().getHead());
		
		rel = (Relation)s.getRelationsAsList().get(0);
		assertEquals("PHYS",rel.getRelationType());
		assertEquals("New York",rel.getFirstMention().getHead());
		assertEquals("US", rel.getSecondMention().getHead());

	}
	
	@Test
	public void testGetNumMentionsInBetween() throws Exception {
		String out = "Dick/B-PER@R1_EMP-ORG_ARG1|R2_PHYS_ARG1 Semion/I-PER@R1_EMP-ORG_ARG1|R2_PHYS_ARG1 ,/O VOA/B-ORG@R1_EMP-ORG_ARG2 News/I-ORG@R1_EMP-ORG_ARG2 ,/O Tallahassee/B-GPE@R2_PHYS_ARG2 ./O";
		Sentence s = Sentence.createSentenceFromRelationTaggedInput(0, out);
		assertEquals(2,s.getRelations().size());
		Relation rel = (Relation)s.getRelationsAsList().get(1); 
		assertEquals("EMP-ORG",rel.getRelationType());
		assertEquals("Dick Semion",rel.getFirstMention().getHead());
		assertEquals("VOA News", rel.getSecondMention().getHead());
		assertEquals(0,s.getNumMentionsInBetween(rel.getFirstMention(), rel.getSecondMention()));
		
		rel = (Relation)s.getRelationsAsList().get(0);
		assertEquals("PHYS",rel.getRelationType());
		assertEquals("Dick Semion",rel.getFirstMention().getHead());
		assertEquals("Tallahassee", rel.getSecondMention().getHead());
		
		assertEquals(1,s.getNumMentionsInBetween(rel.getFirstMention(), rel.getSecondMention()));
		
		out ="John/B-PER@R1_EMP-ORG_ARG1 and/O Mary/B-PER who/O Jim/B-PER and/O Peter/B-PER know/O works/O at/O Microsoft/B-ORG@R1_EMP-ORG_ARG2";
		s = Sentence.createSentenceFromRelationTaggedInput(0, out);
		assertEquals(1,s.getRelations().size());
		rel = (Relation)s.getRelationsAsList().get(0);
		assertEquals("EMP-ORG",rel.getRelationType());
		assertEquals("John",rel.getFirstMention().getHead());
		assertEquals("Microsoft",rel.getSecondMention().getHead());
		
		assertEquals(3,s.getNumMentionsInBetween(rel.getSecondMention(),rel.getFirstMention()));
		
	}
	
	@Test
	public void testReadRelation() throws Exception {
		String input = "state/B-GPE.State-or-Province@3-2_EMP-ORG.Employ-Staff_ARG2 legislators/B-PER@3-2_EMP-ORG.Employ-Staff_ARG1|5-1_PHYS.Located_ARG1 in/O florida/B-GPE.State-or-Province@1-2_PHYS.Part-Whole_ARG2 's/O capitol/B-GPE.Population-Center@1-2_PHYS.Part-Whole_ARG1|5-1_PHYS.Located_ARG2 are/O discussing/O if/O a/O special/O session/O is/O needed/O to/O decide/O which/O 25/O electors/B-PER will/O represent/O florida/B-GPE.State-or-Province in/O the/O electoral/B-ORG.Government college/I-ORG.Government --/O a/O democratic/B-ORG.Other slate/O ?/O";
		Sentence s = Sentence.createSentenceFromRelationTaggedInput(6, input);
		assertEquals(3,s.getRelations().size());
		
		input = "Indeed/O ,/O even/O as/O his/B-PER@5-2_PER-SOC.Business_ARG1 aides/B-PER@5-2_PER-SOC.Business_ARG2 were/O finalizing/O plans/O for/O their/B-PER assault/O ,/O Gore/B-PER@7-1_PHYS.Located_ARG1 discussed/O his/B-PER education/O proposals/O on/O Sunday/O afternoon/O with/O teachers/B-PER@8-1_PHYS.Located_ARG1 ,/O students/B-PER@9-1_PHYS.Located_ARG1 and/O parents/B-PER@10-1_PHYS.Located_ARG1 during/O a/O photo/O opportunity/O at/O his/B-PER@11-1_ART.User-or-Owner_ARG1 residence/B-FAC.Building@7-1_PHYS.Located_ARG2|8-1_PHYS.Located_ARG2|9-1_PHYS.Located_ARG2|10-1_PHYS.Located_ARG2|11-1_ART.User-or-Owner_ARG2|12-1_PHYS.Located_ARG1 in/O Washington/B-GPE.Population-Center@12-1_PHYS.Located_ARG2 ./O";
		s = Sentence.createSentenceFromRelationTaggedInput(105, input);
		assertEquals(7, s.getRelations().size());
		
		input = " in/O leon/B-GPE county/I-GPE ,/O which/B-GPE@R1_PHYS_ARG1 includes/O tallahassee/B-GPE@R1_PHYS_ARG2 ,/O circuit/O court/B-ORG@R2_EMP-ORG_ARG2 judge/B-PER@R2_EMP-ORG_ARG1 n./O sanders/O sauls/O ordered/O 13,000/O contested/O ballots/O from/O miami-dade/O and/O palm/B-GPE@R3_DISC_ARG1 beach/I-GPE@R3_DISC_ARG1 counties/B-GPE@R3_DISC_ARG2 to/O be/O brought/O to/O tallahassee/O by/O Saturday/O ./O";
		s = Sentence.createSentenceFromRelationTaggedInput(12, input);
		assertEquals(3,s.getRelations().size());
	}
	
	@Test
	public void testGetAnnotatedMentions() {
		//this method is used to write the out.idx file, thus test it!
		Sentence s = Sentence.createSentenceFromNERtaggedInput(0, "``/O The/O manufacturers/B-ORG are/O scapegoats/B-ORG ,/O ''/O said/O Robert/B-PER Labatt/I-PER ,/O a/O new/O media/O analyst/B-PER at/O research/O group/B-ORG Gartner/B-ORG ./O");
		
		Mention mention1=s.findMentionWithHead("analyst");
		Mention mention2=s.findMentionWithHead("Gartner");
		Relation r = Relation.createRelation("3-1", mention1, mention2, "EMP-ORG", null);
		s.addRelation(r);
		Mention mention11 = s.findMentionWithHead("manufacturers");
		Mention mention22 = s.findMentionWithHead("scapegoats");
		Relation r2 = Relation.createRelation("3-3", mention11,mention22, "XXX", null);
		s.addRelation(r2);
		s.addRelation(r);
		String expect = "``/O The/O manufacturers/B-ORG@3-3_XXX_ARG1 are/O scapegoats/B-ORG@3-3_XXX_ARG2 ,/O ''/O said/O Robert/B-PER Labatt/I-PER ,/O a/O new/O media/O analyst/B-PER@3-1_EMP-ORG_ARG1 at/O research/O group/B-ORG Gartner/B-ORG@3-1_EMP-ORG_ARG2 ./O";
		assertEquals(expect, s.getAnnotatedSentence());
	}
	
	@Test
	public void testCreateSentenceForEval() throws Exception {
		int tp=0, fp=0,fn=0;
		int cross=0;
		String annotatedSentence = "WHITE/B-GPE@1-1_PHYS_ARG1 SULPHUR/I-GPE@1-1_PHYS_ARG1 SPRINGS/I-GPE@1-1_PHYS_ARG1 ,/O W./B-GPE@1-1_PHYS_ARG2 Va./I-GPE@1-1_PHYS_ARG2 -LRB-/O AP/B-ORG -RRB-/O _/O Republican/B-ORG@2-1_EMP-ORG_ARG2 Rep./B-PER J.C./B-PER@2-1_EMP-ORG_ARG1 Watts/I-PER@2-1_EMP-ORG_ARG1 joked/O Saturday/O night/O that/O he/B-PER had/O revised/O returns/O on/O the/O presidential/O election/O :/O ``/O Al/B-PER Gore/I-PER ,/O 9,834/O lawsuits/O ./O";
		Sentence s = Sentence.createSentenceFromRelationTaggedInput(0, annotatedSentence);
		assertEquals(2, s.getRelations().size());
		for (Relation r : s.getRelationsAsList()) {
			System.out.println(r);
		}
		
		String predictedsentence = "WHITE/B-GPE@R1_PHYS_ARG1 SULPHUR/I-GPE@R1_PHYS_ARG1 SPRINGS/I-GPE@R1_PHYS_ARG1 ,/O W./B-GPE@R1_PHYS_ARG2 Va./I-GPE@R1_PHYS_ARG2 -LRB-/O AP/O -RRB-/O _/O Republican/B-ORG@R2_EMP-ORG_ARG2 Rep./B-PER@R2_EMP-ORG_ARG1 J.C./O Watts/O joked/O Saturday/O night/O that/O he/O had/O revised/O returns/O on/O the/O presidential/O election/O :/O ``/O Al/O Gore/O ,/O 9,834/O lawsuits/O ./O";
		Sentence ps = Sentence.createSentenceFromRelationTaggedInput(0, predictedsentence);
		assertEquals(2,ps.getRelations().size());
		
	
		
		for (Relation p: ps.getRelationsAsList()) {
			
			Relation toCheck = (Relation) s.getRelations().getRelation(p.getFirstMention(), p.getSecondMention());
			System.out.println(p + " " + toCheck);
			if (toCheck == null)
				fp++;
			else {
				if (toCheck.getRelationType().equals(p.getRelationType())) {
					if (toCheck.isSymmetric())
						tp++;
					else {	
						if (toCheck.getFirstMention().equals(p.getFirstMention())) {
							tp++;	
						}
						else {
							fp++;//inverted! (same as cross-type)
							fn++;
							cross++;
							//System.out.println(p + " " + toCheck);
						}
					}
				} else {
					fp++;
					fn++;
					cross++;
				}
					
			}
			
		}
		for (Relation g: s.getRelationsAsList()) {
			if (ps.getRelations().getRelation(g.getFirstMention(),g.getSecondMention())==null) {
				fn++;
				System.out.println("--->"+ g);
			}
		}
		
		assertEquals(1, tp);
		assertEquals(1, fp);
		assertEquals(1, fn);
		assertEquals(0, cross);
	}
 }
