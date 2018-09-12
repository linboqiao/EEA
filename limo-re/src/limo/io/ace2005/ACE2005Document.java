package limo.io.ace2005;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import limo.core.Entities;
import limo.core.Entity;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.Token;
import limo.core.interfaces.IAceRelationDocument;
import limo.core.interfaces.IEntity;
import limo.tokenizer.stanford.DocumentPreprocessor;


import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;


public class ACE2005Document extends IAceRelationDocument  {


	private SGM2005Content sgmContent;
	private ACE2005Reader ace2005Reader;
	int count;
	int countIgnored;
	

	public ACE2005Document(File xmlFile, ACE2005Reader ace2005reader) throws Exception {
		this.xmlFile = xmlFile;
		this.ace2005Reader = ace2005reader;
		try {
			if (this.initFileReader(this.xmlFile)) {
				this.entities = this.readEntitiesAndMentionsFromFile();
				this.sentences = this.readTextFromFileAndTokenize();
				this.sentences = this.matchTokensAndMentions();
				this.relations = this.readRelationsFromFileAndAddToSentences();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/***
	 * Read in content from SGM file
	 * - to get real character positions, strip off tags
	 * - to get plain text strip off tags plus what they contain
	 * - to get mapping between character and plain text indices
	 * @param url
	 * @param encoding
	 * @return SGMContent object
	 */
	private SGM2005Content _readSGMcontent(String url, String encoding) {
		return new SGM2005Content(url, encoding);
	}



	/***
	 * read entities and mentions from XML file
	 */
	@SuppressWarnings("unchecked")
	public Entities readEntitiesAndMentionsFromFile() {
		if (this.initFileReader(this.xmlFile)) {
			// the <document> element
			Element docElement = this.docElement;
			
			ArrayList<IEntity> entities = new ArrayList<IEntity>();
			
			Iterator<Element> entityIter = docElement.getChildren("entity").iterator();
			while (entityIter.hasNext()) {
				Element entityElement = entityIter.next();
				String id = entityElement.getAttributeValue("ID");
				String type = entityElement.getAttributeValue("TYPE");
				//String subtype = entityElement.getAttributeValue("SUBTYPE");
				String subtype = "";

				Entity entity = new Entity(id, type);
				entity.setSubtype(subtype);
				addMentions(entity, entityElement);
				entities.add(entity);
			}
			return new Entities(entities);
		}
		//throw new Exception("could not initialize entities and mentions");
		return null;
	}

	@SuppressWarnings("unchecked")
	/**
	 * Read mention information from XML file and add to entity
	 */
	private void addMentions(Entity entity, Element entityElement) {
		List<Element> entity_mentions = entityElement
				.getChildren("entity_mention");
		if (entity_mentions != null) {
			Iterator<Element> mentionIter = entity_mentions.iterator();
			while (mentionIter.hasNext()) {
				Element entity_mention = mentionIter.next();
				String mentionId = entity_mention.getAttributeValue("ID");
				String mentionType = entity_mention.getAttributeValue("TYPE");
				String mentionLDCType = entity_mention.getAttributeValue("LDCTYPE");
				String mentionRole = entity_mention.getAttributeValue("ROLE");
				

				Element extentSeq = entity_mention.getChild("extent").getChild("charseq");
				Element headSeq = entity_mention.getChild("head").getChild("charseq");

				String head = headSeq.getText();
				//System.out.println(head);
				String extend = extentSeq.getText();

				int extendStart = Integer.parseInt(extentSeq.getAttributeValue("START"));
				int extendEnd = Integer.parseInt(extentSeq.getAttributeValue("END"));

				int headStart = Integer.parseInt(headSeq
						.getAttributeValue("START"));
				int headEnd = Integer.parseInt(headSeq.getAttributeValue("END"));
				// System.out.println(extendStart + " " + extendEnd+ " " +
				// headStart+ " " +headEnd);

				Mention mention = new Mention(mentionId, mentionType, extend,
						extendStart, extendEnd, head, headStart, headEnd, entity);
				mention.setMentionLDCType(mentionLDCType);
				mention.setMentionRole(mentionRole);
				entity.addMention(mention);
			}
		}
	}
	

	public Relations readRelationsFromFileAndAddToSentences() throws Exception {
		if (this.initFileReader(this.xmlFile)) {
			
			ArrayList<Relation> relations = new ArrayList<Relation>();
			int numIgnoredRelations = 0;
			int count=0;
			
			@SuppressWarnings("unchecked")
			Iterator<Element> relationIter = this.docElement.getChildren("relation").iterator();
			
	        while (relationIter.hasNext()) {
	        	
	            Element relElement = relationIter.next();            
	            
	            //relation id and type
	            String relationId = relElement.getAttributeValue("ID"); //note that this is not unique, a relation might have several relation_mentions all with the same id, thus add relation mention id here
	            String type = relElement.getAttributeValue("TYPE");
	            String subtype = relElement.getAttributeValue("SUBTYPE");
	            // ACE 2005: relation would also have TENSE and MODALITY
	            
	 	        
	    		@SuppressWarnings("unchecked")
	            Iterator<Element> rel_entity_arg_iter = relElement.getChildren("relation_argument").iterator();
	            
	            String[] entityIds = new String[2];
	            
	            // read in the two entities
	            while (rel_entity_arg_iter.hasNext()) {
	            	
	            	Element rel_entity_arg = rel_entity_arg_iter.next();
	            	//String entityId = rel_entity_arg.getAttributeValue("ENTITYID");
	            	String entityId = rel_entity_arg.getAttributeValue("REFID");
	            	//int entityArgNum = Integer.parseInt(rel_entity_arg.getAttributeValue("ARGNUM"));
	            	String argNum = rel_entity_arg.getAttributeValue("ROLE");
	            	if (!argNum.startsWith("Arg-"))
	            		continue; //skip time annotations in ACE 2005
	            	int entityArgNum = argNum.equals("Arg-1") ? 1 : 2;
	            	
	            	entityIds[entityArgNum-1] = entityId;
	            }
	            
				Entity entity1 = this.getEntities().getEntityById(entityIds[0]);
				Entity entity2 = this.getEntities().getEntityById(entityIds[1]);

				@SuppressWarnings("unchecked")
				Iterator<Element> relIter = relElement.getChildren(
						"relation_mention").iterator();

				if (entity1 == null || entity2 == null) {
					System.err.println("Ignoring relation "+relationId + " since it involves non-existing entities.");
					continue;
				}
				
				while (relIter.hasNext()) {
					
					// for every relation mention
					Element relMention = relIter.next();
					String relMentionId = relMention.getAttributeValue("ID");
					//String uniqueRelationId = relationId + "@" + relMentionId; //add mention id to relationId
					String uniqueRelationId = relMentionId; //add mention id to relationId
					
					//Iterator<Element> rel_mention_argument_iter = relMention.getChildren("rel_mention_arg").iterator();
					@SuppressWarnings("unchecked")
					Iterator<Element> rel_mention_argument_iter = relMention.getChildren("relation_mention_argument").iterator();
					
					String[] mentionIds = new String[2];
					
					while (rel_mention_argument_iter.hasNext()) {
						
						Element relation_mention = rel_mention_argument_iter.next();
						
						String entityMentionId =  relation_mention.getAttributeValue("REFID");
						//int entityArgNum = Integer.parseInt(relation_mention.getAttributeValue("ARGNUM"));
						String entityArg = relation_mention.getAttributeValue("ROLE");
						if (!entityArg.startsWith("Arg-"))
		            		continue; //skip time annotations in ACE 2005
						int entityArgNum = entityArg.equals("Arg-1") ? 1 : 2;
						mentionIds[entityArgNum-1] = entityMentionId;
					}
										
					if (mentionIds[0].equals(mentionIds[1])) {
		            	System.err.println("Relation between the same entity mentions! Annotation error!");
		            	numIgnoredRelations++;
		            	System.err.println(mentionIds[0]);
		            	continue;
		            }
					
					Mention mention1 = entity1.getMentionById(mentionIds[0]);
					Mention mention2 = entity2.getMentionById(mentionIds[1]);
					
					if (mention1 == null) {
						System.err.println("Mention not found! Ignore! Relation: "+uniqueRelationId);
						continue;
					} 	
					if (mention2 == null) {
						System.err.println("Mention not found! Ignore! mention2: "+uniqueRelationId);
						continue;
					}
					if (mention1.isToIgnore()) {
						System.err.println("Ignore relation between mentions to ignore (e.g. outside annotation part!) "+mention1.getHead()); // means that headTokens is empty []
						continue;
					}
					if (mention2.isToIgnore()) {
						System.err.println("Ignore relation between mentions to ignore (e.g. outside annotation part!) "+mention2.getHead());
						continue;
					}
					
					if (mention1.getHead().equals(mention2.getHead()))  {
						System.err.println("relation between mentions with same head! Ignore!");
						numIgnoredRelations++;
						continue;
					}
								
					//check if overlap, for instance [Vancouver]-[area] -- same token but different mentions, ignore!
					if (mention1.sameHeadToken(mention2)) {
						System.err.println("relation in-between same token! Ignore! mention1: "+mention1.getHead() + " mention2: "+mention2.getHead()+ 
								" Relation: "+relationId + " "+ mention1.getExtend() + " " + mention2.getExtend());
						continue;
					}
				
					//check for different sentences before instantiating
					int sentenceId = mention1.getSentenceId();
					int sentenceId2 = mention2.getSentenceId();
					if (sentenceId != sentenceId2) { 
							System.err
									.println("Ingoring relation between mentions of different sentences: "
											+ mention1.getHead()
											+ " (id: "+ sentenceId+ ")"
											+ mention2.getHead()
											+ " (id: " + sentenceId2 + ")");
							System.err.println("Document: " + this.getURI()
									+ " Relation:" + relationId);
							continue;
					}
					
					mention1.setIsInRelation(true);
					mention2.setIsInRelation(true);
				
					
					mention1.setEntityReference(entity1);
					mention2.setEntityReference(entity2);
				
					if (this.ace2005Reader.relationsToIgnoreIds == null
							|| (this.ace2005Reader.relationsToIgnoreIds != null && !this.ace2005Reader.relationsToIgnoreIds
									.contains(relationId))) {
						Relation relation =Relation.createRelation(uniqueRelationId,
								mention1, mention2, type, subtype);
						
						// check if we already have this relation (as relation_mention_ID might be different but relation is annotated twice!)
						if (relations.contains(relation)) {
								System.err.println("Ignoring relation as we already have it (duplicate in annotation or several relations within same token!) "+relationId + " "+relation.toString());
								continue;
						}
						relations.add(relation);
					
						Sentence sentence = this.sentences.get(sentenceId);
						sentence.addRelation(relation);
						count++;
					
					} else if  (this.ace2005Reader.relationsToIgnoreIds != null && this.ace2005Reader.relationsToIgnoreIds
							.contains(relationId)) {
						System.err.println("Ignoring relations as specified in file: "+relationId);
						numIgnoredRelations++;
					
					}
				}
		
			}
	        	        
	        this.count = count;
	        this.countIgnored = numIgnoredRelations;
			return new Relations(relations);
		} else {
			// throw new
			// Exception("could not initialize entities and mentions");
			return null;
		}
	}

	public ArrayList<Sentence> readTextFromFileAndTokenize() {
		// read text
		this.sgmContent = _readSGMcontent(sourceFilename, encoding);
		
		ArrayList<Sentence> sentences = new ArrayList<Sentence>();
		// tokenize
		DocumentPreprocessor docPreprocessor = new DocumentPreprocessor(
				new StringReader(this.sgmContent.getPlainContent()));
		docPreprocessor.setEncoding(this.encoding);

		int sentenceCount = 0;
		for (List<HasWord> docProcSentence : docPreprocessor) {
			Sentence sentence = Sentence.createEmptySentence(sentenceCount);
			int tokenCount = 0;
			for (HasWord iw : docProcSentence) {
				CoreLabel coreLab = (CoreLabel) iw;
				String tokenValue = coreLab.value().replace("\u00A0","_"); // replace non-breaking whitespace (does not work with \\s+)
				//System.out.println(coreLab.value() + "\t" +coreLab.value().replace("\u00A0","_"));
				Token t = new Token(tokenValue, tokenCount, sentence);
				tokenCount += 1;
				//set position before tokenization
				t.setBeginCharPosition(coreLab.beginPosition());
				t.setEndCharPosition(coreLab.endPosition()-1);
				t.setOriginalText(coreLab.originalText());
				//annotation xml
				t.setAnnotationStartIndex(this.sgmContent.getAnnotationTextIndex(t.getCharseq_START()));
				t.setAnnotationEndIndex(this.sgmContent.getAnnotationTextIndex(t.getCharseq_END()));
				sentence.addToken(t);
				
			}
			sentenceCount+=1;
			sentences.add(sentence);
		}
		return sentences;
	}


	/** initialize file reading; set some metainfo **/
	public boolean initFileReader(File xmlFile) {
		try {
			this.xmlFile = xmlFile;
			SAXBuilder builder = new SAXBuilder();
			builder.setValidation(false);
			builder.setFeature("http://xml.org/sax/features/validation", false);
			builder
					.setFeature(
							"http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
							false);
			builder
					.setFeature(
							"http://apache.org/xml/features/nonvalidating/load-external-dtd",
							false);
			Document doc;

			doc = builder.build(xmlFile);
			Element source_file = doc.getRootElement();
			Element docElement = (org.jdom.Element) source_file
					.getChild("document");
			this.docElement = docElement;

			this.URI = source_file.getAttributeValue("URI");
			this.source = source_file.getAttributeValue("SOURCE");
			this.type = source_file.getAttributeValue("TYPE");
			this.version = source_file.getAttributeValue("VERSION");
			this.author = source_file.getAttributeValue("AUTHOR");
			this.encoding = source_file.getAttributeValue("ENCODING");
			this.docId = docElement.getAttributeValue("DOCID");

			String st = xmlFile.getParent() + File.separatorChar;

			URI = URI.replace('.', '_').replaceAll(".sgm", ".SGM");

			String sourceFilename = st + URI;
			this.sourceFilename = sourceFilename;
			return true;
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

}
