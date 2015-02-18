package limo.modules;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;

import limo.cluster.BrownWordCluster;
import limo.cluster.ClassificationScores;
import limo.cluster.SemKernelDictionary;
import limo.cluster.Word2VecSemKernelDictionary;
import limo.cluster.WordEmbeddingDictionary;
import limo.cluster.io.BrownWordClusterReader;
import limo.cluster.io.ScoreReader;
import limo.cluster.io.SemKernelDictionaryReader;
import limo.cluster.io.TextClassifierReader;
import limo.cluster.io.Word2VecSemKernelDictionaryReader;
import limo.cluster.io.WordEmbeddingDictionaryReader;
import limo.core.Corpus;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IAceRelationDocument;
import limo.core.interfaces.IRelation;
import limo.core.interfaces.IRelationDocument;
import limo.core.trees.AbstractTree;
import limo.core.trees.constituency.ParseTree;
import limo.core.trees.dependency.DependencyInstance;
import limo.exrel.Exrel;
import limo.exrel.features.AbstractFeature;
import limo.exrel.features.FeaturesGroup;
import limo.exrel.features.RelationExtractionLinearFeaturesGroup;
import limo.exrel.features.RelationExtractionStructuredFeaturesGroup;
import limo.exrel.features.Separator;
import limo.exrel.features.re.structured.VEChm;
import limo.exrel.modules.AbstractModule;
import limo.exrel.slots.BooleanSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.slots.InputDirSlot;
import limo.exrel.slots.IntegerSlot;
import limo.exrel.slots.OutputFileSlot;
import limo.exrel.slots.StringSlot;
import limo.exrel.slots.XMLConfigSlot;
import limo.exrel.utils.Index;
import limo.exrel.utils.Index.OpenMode;
import limo.exrel.utils.XMLConfig;
import limo.exrel.utils.XMLTools;
import limo.io.IRelationReader;
import limo.io.filter.ParsedDataFilter;

import org.w3c.dom.Element;

/***
 * Module that reads parsed data and generates training/test instances
 * @author Barbara Plank
 *
 */
public class ParsedDataReaderModule  extends AbstractModule {

	public InputDirSlot inputDir = new InputDirSlot(true);
	public InputDirSlot inputDir2 = new InputDirSlot(false); //optional. Parses of second parser
		
	public StringSlot fileEndingConstituency = new StringSlot(true);
	
	public StringSlot readerClass = new StringSlot(true);
	
	public InputDirSlot inputDirDep = new InputDirSlot(false); //optional. Dependency parses
	public StringSlot fileEndingDependency = new StringSlot(false);
	
	public InputDirSlot aceDataDir = new InputDirSlot(true);
	public FileSlot ignoreRelationsFile = new FileSlot(false); //optional
	public FileSlot mappingRelationsFile = new FileSlot(false); //optional
	
	public OutputFileSlot outExamplesIdxFile = new OutputFileSlot(true);
	public XMLConfigSlot featuresLayoutXML = new XMLConfigSlot(true);
	public StringSlot featuresDictionary = new StringSlot(true);
	
	public BooleanSlot directedClassification = new BooleanSlot("true"); //default is true
	public StringSlot granularityClassification = new StringSlot(true); //type or subtype
	
	public FileSlot wordClusterFilePath = new FileSlot(false); 
	
	public FileSlot semanticKernelDictionary = new FileSlot(false);
	
	public FileSlot word2vecSemanticKernelDictionary = new FileSlot(false); //thien's addition for word2vec
	public FileSlot wordEmbeddingDictionary = new FileSlot(false); //thien's addition for word embeddings
	
	public BooleanSlot restrictMentionsInBetween = new BooleanSlot("false"); //default is false
	public BooleanSlot restrictOnlyNegative = new BooleanSlot("false"); //to replicate Zhang on ACE 2004
	public BooleanSlot skipSentences = new BooleanSlot("false"); //to replicate Zhang on ACE 2004
	public IntegerSlot maxNumMentions = new IntegerSlot(4);
	public BooleanSlot restrictNegativesTraining = new BooleanSlot("false"); //default is false
	
	public BooleanSlot addEmtpyEntity = new BooleanSlot("false");
	
	public static String subtypeDivChar="--";
	
	public FileSlot mappingEntitiesFile = new FileSlot(false); //optional
	public FileSlot trainRelationsFile = new FileSlot(false); //optional
	
	public BooleanSlot trainReranker = new BooleanSlot("false"); //default is false

	public StringSlot sentenceWeightsEnding = new StringSlot(false); //default is false 
	
	private LinkedHashMap<String, FeaturesGroup<?>> groups = new LinkedHashMap<String, FeaturesGroup<?>>();	

	
	public static HashMap<String,String> posMapping = new HashMap<String,String>();
	
	private String skipString = ""; //thien's addition to contain only separators in skip instances
	
	
	public ParsedDataReaderModule(String moduleId, String configId) {
		super(moduleId,configId);
	}

	
	@Override
	public void _init(Exrel system, Element e) {
		skipString = "";
		
		// load dictionary
		File dictionary = new File(featuresDictionary.get());
		
		XMLConfig featuresXML = featuresLayoutXML.get();
		Element root = featuresXML.getExtractorLayoutElement();
		message("Found features layout for Extractor: %s", root.getNodeName());
		Vector<Element> children = XMLTools.getChildren(root);
		for (Element elem : children) {
			String groupId = elem.getAttribute("id");
			if (elem.getNodeName().equals("separator")) {
				addGroup(groupId, new Separator(groupId, elem.getTextContent()));
				
				if (elem.getTextContent().contains("BV"))
					skipString += elem.getTextContent() + " 1:0.0 ";
				else if (elem.getTextContent().contains("EV"))
					skipString += "2:0.0 " + elem.getTextContent() + " ";
				else
					skipString += elem.getTextContent() + " ";
			} else if (elem.getNodeName().equals("lineargroup")) {
				//!!!!! String dictionary = elem.getAttribute("dictionary");
				//RelationExtractionLinearFeaturesGroup linear = new RelationExtractionLinearFeaturesGroup(groupId, new File(dictionary));
				RelationExtractionLinearFeaturesGroup linear = new RelationExtractionLinearFeaturesGroup(groupId, dictionary);
				addGroup(groupId, linear);
				addFeatures(linear, XMLTools.getChildren(elem));				
			} else if (elem.getNodeName().equals("structuredgroup")) {
				RelationExtractionStructuredFeaturesGroup structured = new RelationExtractionStructuredFeaturesGroup(groupId);
				addGroup(groupId, structured);
				addFeatures(structured, XMLTools.getChildren(elem));	
			} else {
				throw new RuntimeException(String.format("Unknown feature group type: %s", elem.getNodeName()));
			}
		}
		
		// init posMapping (universal)
		posMapping.put(".", ".");
		posMapping.put("ADJ", "j");
		posMapping.put("ADP", "i");
		posMapping.put("ADV", "a");
		posMapping.put("CONJ", "c");
		posMapping.put("DET", "d");
		posMapping.put("NOUN", "n");
		posMapping.put("NUM", "u");
		posMapping.put("PRON", "p");
		posMapping.put("PRT", "r");
		posMapping.put("VERB", "v");
		posMapping.put("X", "x");
		// / ENGLISH PTB
		posMapping.put("!", ".");
		posMapping.put("#", ".");
		posMapping.put("$", ".");
		posMapping.put("''", ".");
		posMapping.put("(", ".");
		posMapping.put(")", ".");
		posMapping.put(",", ".");
		posMapping.put("-LRB-", ".");
		posMapping.put("-RRB-", ".");
		posMapping.put(".", ".");
		posMapping.put(":", ".");
		posMapping.put("?", ".");
		posMapping.put("CC", "c");
		posMapping.put("CD", "u");
		posMapping.put("CD|RB", "x");
		posMapping.put("DT", "d");
		posMapping.put("EX", "d");
		posMapping.put("FW", "x");
		posMapping.put("IN", "i");
		posMapping.put("IN|RP", "i");
		posMapping.put("JJ", "j");
		posMapping.put("JJR", "j");
		posMapping.put("JJRJR", "j");
		posMapping.put("JJS", "j");
		posMapping.put("JJ|RB", "j");
		posMapping.put("JJ|VBG", "j");
		posMapping.put("LS", "x");
		posMapping.put("MD", "v");
		posMapping.put("NN", "n");
		posMapping.put("NNP", "n");
		posMapping.put("NNPS", "n");
		posMapping.put("NNS", "n");
		posMapping.put("NN|NNS", "n");
		posMapping.put("NN|SYM", "n");
		posMapping.put("NN|VBG", "n");
		posMapping.put("NP", "n");
		posMapping.put("PDT", "d");
		posMapping.put("POS", "r");
		posMapping.put("PRP", "p");
		posMapping.put("PRP$", "p");
		posMapping.put("PRP|VBP", "p");
		posMapping.put("PRT", "r");
		posMapping.put("RB", "a");
		posMapping.put("RBR", "a");
		posMapping.put("RBS", "a");
		posMapping.put("RB|RP", "a");
		posMapping.put("RB|VBG", "a");
		posMapping.put("RN", "x");
		posMapping.put("RP", "r");
		posMapping.put("SYM", "x");
		posMapping.put("TO", "r");
		posMapping.put("UH", "x");
		posMapping.put("VB", "v");
		posMapping.put("VBD", "v");
		posMapping.put("VBD|VBN", "v");
		posMapping.put("VBG", "v");
		posMapping.put("VBG|NN", "v");
		posMapping.put("VBN", "v");
		posMapping.put("VBP", "v");
		posMapping.put("VBP|TO", "v");
		posMapping.put("VBZ", "v");
		posMapping.put("VP", "v");
		posMapping.put("WDT", "d");
		posMapping.put("WH", "x");
		posMapping.put("WP", "p");
		posMapping.put("WP$", "p");
		posMapping.put("WRB", "a");
		posMapping.put("``", ".");
		
		skipString = skipString.trim();
	}
	
	/*** mapping pos (universal pos) to short pos ***/
	public static String getPosMapping(String pos) {
		String mapping = posMapping.get(pos);
		if (mapping != null)
			return mapping;
		else
			return "n"; //no pos found, assume noun by default
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addFeatures(FeaturesGroup group, Vector<Element> featureElements) {
		for (Element featureElem : featureElements) {
			String className = featureElem.getAttribute("class"); 
			try {
				Class<?> featureClass = Class.forName(className);
				AbstractFeature feature = (AbstractFeature)featureClass.newInstance();
				feature.init(featureElem);
				message("Adding feature %s to feature group %s",
						feature.toString(),
						group.getGroupId());
				group.addFeature(feature);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(String.format("Could initialize feature for class %s", className));
			}
		}
	}
	
	public void addGroup(String groupName, FeaturesGroup<?> group) {
		if (groups.get(groupName) != null) {
			throw new RuntimeException(String.format("Duplicate feature group: %s", groupName));
		}
		message("Adding feature group \"%s\" (%s)", groupName, group.getClass().getSimpleName());
		groups.put(groupName, group);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	protected void _run() {
		String inDir = inputDir.get().getAbsolutePath();

		message("Reading parsed data");
		message("InputDir: %s", inDir);

		Index featuresIdx = new Index(outExamplesIdxFile.get(), OpenMode.WRITE);
		
		Corpus corpus;
		
		Class<IRelationReader> classIRelReader;
		IRelationReader reader = null;

		try {
			classIRelReader = (Class<IRelationReader>) Class.forName(readerClass.get());

			if (ignoreRelationsFile.get() != null && mappingEntitiesFile.get() == null) {
				@SuppressWarnings("rawtypes")
				Class[] types = new Class[] { String.class, String.class };
				Constructor<IRelationReader> cons = classIRelReader
						.getConstructor(types);
				Object[] args = new Object[] {
						aceDataDir.get().getAbsolutePath(),
						ignoreRelationsFile.get().getAbsolutePath() };
				reader = cons.newInstance(args);
			} else if (mappingEntitiesFile.get() != null && trainRelationsFile.get()!= null) {
				@SuppressWarnings("rawtypes")
				Class[] types = new Class[] { String.class, String.class, String.class };
				Constructor<IRelationReader> cons = classIRelReader
						.getConstructor(types);
				Object[] args = new Object[] {
						aceDataDir.get().getAbsolutePath(),
						mappingEntitiesFile.get().getAbsolutePath(),
						trainRelationsFile.get().getAbsolutePath()};
				reader = cons.newInstance(args);
				
			} else {
				@SuppressWarnings("rawtypes")
				Class[] types = new Class[] { String.class };
				Constructor<IRelationReader> cons = classIRelReader
						.getConstructor(types);
				Object[] args = new Object[] { aceDataDir.get()
						.getAbsolutePath() };
				reader = cons.newInstance(args);
				
			}

		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		message("Reading in documents.");
		
		corpus = new Corpus(reader, skipSentences.get());
		corpus.init();
		message("Number documents read: "+ corpus.getNumDocuments());
		
		// populates constituentParseTrees
		readParsedConstituencyData(corpus);
		
		if (inputDirDep.get()!= null)
			readDependencyParseData(corpus);
		
		try {
			if (trainReranker.get() == false)
				generatePossibleRelationsAndextractFeatures(corpus, featuresIdx);
			//else 
				//generateRerankingData(corpus,featuresIdx);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		featuresIdx.close();
		
		for (FeaturesGroup<?> group : groups.values()) {
			group.cleanup();
		}
	}

	


//	private void generateRerankingData(Corpus corpus, Index featuresIdx) {
//		IRelationDocument doc = corpus.getNextDocument();
//		int countInstance = 0;
//		
//		int instanceId = 0;
//		
//		while (doc != null) {
//			message("Extracting features from document: %s", doc.getURI());
//			
//			
//			// for every sentence
//			for (Sentence s : doc.getSentences()) {
//				Relations sentenceRelations = s.getRelations();
//				ArrayList<Mention> mentions = s.getMentions();
//							
//				int numRelations = sentenceRelations.size();
//				System.out.println(numRelations + " relations; "+mentions.size() + " num mentions");
//				String qid = "qid:"+s.getSentenceId(); //this is document-level id
//				
//				instanceId++;
//			}				
//			// get next
//			doc = (IAceRelationDocument) corpus.getNextDocument();
//		}
//	}


	private void generatePossibleRelationsAndextractFeatures(Corpus corpus, Index featuresIdx) throws Exception {
		BrownWordCluster wordCluster = null;
		SemKernelDictionary semKernelDictionary  = null;
		//thien's addition
		Word2VecSemKernelDictionary word2vecSemKernelDictionary = null;
		WordEmbeddingDictionary wordEmbeddingDict = null;
		//end thien
		ClassificationScores sentWeights = null;
		
		if (wordClusterFilePath.get() != null) {
			try {
			message("Loading word clusters.. " + wordClusterFilePath.get().getAbsolutePath());
			BrownWordClusterReader wordClusterReader = (BrownWordClusterReader) ScoreReader.createScoreReader("Brown");
			
			wordClusterReader.startReading(wordClusterFilePath.get().getAbsolutePath());
			
			wordCluster = (BrownWordCluster) wordClusterReader.createWordCluster();
				
			
			//Try one out
			String bit = wordCluster.getFullClusterId("the");
			System.out.println(bit);
			System.out.println("the 4:" + wordCluster.getPrefixClusterId("the", 4));
			System.out.println("the 6:" + wordCluster.getPrefixClusterId("the", 6));
			System.out.println("the 100:" + wordCluster.getPrefixClusterId("the", 100));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		if (semanticKernelDictionary.get() != null) {
			try {
				message("Loading semKernelDictionary.. " + semanticKernelDictionary.get().getAbsolutePath());
			
				SemKernelDictionaryReader dictionaryReader = (SemKernelDictionaryReader) ScoreReader.createScoreReader("SemKernelDictionary");
			
				dictionaryReader.startReading(semanticKernelDictionary.get().getAbsolutePath());
				semKernelDictionary = (SemKernelDictionary) dictionaryReader.createWordCluster();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//thien's addition
		if (word2vecSemanticKernelDictionary.get() != null) {
			try {
				message("Loading word2vecSemKernelDictionary.. " + word2vecSemanticKernelDictionary.get().getAbsolutePath());
				
				Word2VecSemKernelDictionaryReader word2vecDictionaryReader = (Word2VecSemKernelDictionaryReader) ScoreReader.createScoreReader("Word2VecSemKernelDictionary");
				
				word2vecDictionaryReader.startReading(word2vecSemanticKernelDictionary.get().getAbsolutePath());
				word2vecSemKernelDictionary = (Word2VecSemKernelDictionary) word2vecDictionaryReader.createWordCluster();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (wordEmbeddingDictionary.get() != null) {
			try {
				message("Loading wordEmbeddingDictionary.. " + wordEmbeddingDictionary.get().getAbsolutePath());
				
				WordEmbeddingDictionaryReader wordEmbeddingDicReader = (WordEmbeddingDictionaryReader) ScoreReader.createScoreReader("WordEmbeddingDictionary");
				
				wordEmbeddingDicReader.startReading(wordEmbeddingDictionary.get().getAbsolutePath());
				wordEmbeddingDict = (WordEmbeddingDictionary) wordEmbeddingDicReader.createWordEmbedding();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//end thien
		
		
		// generate possible relations
		message("Generating possible relations.");
		IRelationDocument doc = corpus.getNextDocument();
		
		
		if (sentenceWeightsEnding.get() != null) {
			String inputfileWeights = aceDataDir.get().getAbsolutePath() + "/" +doc.getURI()+"."+sentenceWeightsEnding.get();
			TextClassifierReader sentenceScoreReader = (TextClassifierReader) ScoreReader.createScoreReader("TextClassifier");
			sentenceScoreReader.startReading(inputfileWeights);
			message("reading file: ", inputfileWeights);
			
			sentWeights = sentenceScoreReader.readClassificationScores();
		}
		
		int countInstance = 0;
		
		int instanceId = 0;
		
		while (doc != null) {
			message("Extracting features from document: %s", doc.getURI());
			
			if (restrictMentionsInBetween.get() == true)
				message("Restrict num mentions in between to "+maxNumMentions.get()+ " mentions.");
			
			int countPos = 0;
			int countNeg = 0;
			
			int countIgnoredRelBecauseOfMentionDistance = 0;
			
			// for every sentence
			for (Sentence s : doc.getSentences()) {
				
				if (sentWeights != null) {
					double sentWeight = sentWeights.getNextScore();
					s.setWeight(sentWeight);
				}
				
				Relations sentenceRelations = s.getRelations();
				ArrayList<Mention> mentions = s.getMentions();
				
				//String qid = "qid:"+s.getSentenceId(); //this is document-level id
				String qid = "qid:"+instanceId;
				instanceId++;
				
				
				if (skipSentences.get() == true) {
					//just to test (get less negatives): use only sentences where a relation exists
					if (sentenceRelations.size()==0)
						continue;
				}
				
				int num_expected_relations_in_sentence = sentenceRelations.size();
				int num_used_relations_in_sentence = 0; 
				
				// only consider a pair once 
				for (int i = 0; i < mentions.size(); i++) {
				
				    for (int j = i+1; j < mentions.size(); j++) {
								
					
						Mention mention1 = mentions.get(i);
						Mention mention2 = mentions.get(j);

						// do not generate inverted pair
						Relation relation = (Relation) sentenceRelations
								.getRelation(mention1, mention2);

						// StringBuffer result = new StringBuffer();
						String label = "NONE";

						// if we only consider at most three mentions in between
						// (only for negatives! and only in training mode!)
						if (restrictMentionsInBetween.get() == true) {
							if (restrictOnlyNegative.get() == true) {
								if (relation == null) { // Zhang restricts only
														// negative
									// if (!getConfigId().equals("test") &&
									// relation == null)
									int maxMen = maxNumMentions.get();
									if ((j - i) > (maxMen + 1)) {
										break;
									}
								}
							} else {
								// restrict all
								int maxMen = maxNumMentions.get();
								if ((j - i) > (maxMen + 1)) {
									if (relation != null)
										countIgnoredRelBecauseOfMentionDistance++;
									break;
								}
							}

						}
						
						//skip negatives during training like Gosh & Muresan (2012): Thus,
						//to balance the distribution of the positive and the negative examples in the training set, we
						//selected only those negative entity sequences where at least one of the two entities is a gold
						//standard entity. ==> which means, participates in a relation
						if (relation == null) {
							if (restrictNegativesTraining.get() == true) {
								if (mention1.isInRelation() == false && mention2.isInRelation() == false)
									break; //skip it!
							}
						}
						
									
						if (relation != null) {
							// System.out.println(relation.getId());
							countPos++;
							num_used_relations_in_sentence++;
							if (granularityClassification.get().equals("type")) {
								label = relation.getRelationType();
							} else if (granularityClassification.get().equals(
									"subtype")) {
								label = relation.getRelationType()
										+ subtypeDivChar
										+ relation.getRelationSubType();
							} else {
								System.err
										.println("no valid value given for granularityClassification: type|subtype.");
								System.exit(-1);
							}
						} else {
							countNeg++;
						}
									
						// return null if it is a skipInstance
						StringBuilder result = generateInstance(groups,
								mention1, mention2, relation, s, doc,
								semKernelDictionary, word2vecSemKernelDictionary, wordEmbeddingDict, wordCluster, sentWeights);

						if (result != null) {
							// System.out.println(result.toString());
							if (directedClassification.get() == true) {
								if (granularityClassification.get().equals(
										"type")) {
									if (!label.startsWith("NONE")
											&& !label.startsWith("PER-SOC")
											&& relation != null) {
										if (relation.getFirstMention().equals(
												mention1))
											label += ".A-B";
										else
											label += ".B-A";
									}
								} else if (granularityClassification.get()
										.equals("subtype")) {
									// 6 of 23 relation subtypes in ACE 2004 are
									// symmetric:
									// TODO: export this to file, not
									// hard-coding in here
									if (!label.startsWith("NONE")) {
										if (!label.equals("PHYS"
												+ subtypeDivChar + "Near")
												|| !label.startsWith("PER-SOC")
												|| // 3 in here
												!label.equals("EMP-ORG"
														+ subtypeDivChar
														+ "Partner")
												|| !label.equals("EMP-ORG"
														+ subtypeDivChar
														+ "Other")) {
											if (relation.getFirstMention()
													.equals(mention1))
												label += ".A-B";
											else
												label += ".B-A";
										}
									}

								} else {
									System.err
											.println("not a valid classification granularity: "
													+ granularityClassification
															.get());
									System.exit(-1);
								}
							}

							// append qid before result
							result.insert(0, qid + " ");

							// annotatedSentence: mark entities in sentence
							// do not output gold relation here, because
							// Disambiguator needs only the target entities!
							String annotatedSentence = s.getAnnotatedMentions(
									mention1, mention2);
										
							featuresIdx.put(label + "\t" + result.toString()
									+ "\t"
									+ mention1.getEntityReference().getType()
									+ "@"
									+ mention2.getEntityReference().getType()
									+ "\t" + annotatedSentence, countInstance);
							countInstance++;

							if (addEmtpyEntity.get() == true) {
								// replace entity type with "ENTITY"
								String entity1type = mention1
										.getEntityReference().getType();
								String entity2type = mention2
										.getEntityReference().getType();

								// temporarily replace entity
								mention1.getEntityReference().setType("ENTITY");
								mention2.getEntityReference().setType("ENTITY");

								StringBuilder alteredResult = generateInstance(
										groups, mention1, mention2, relation,
										s, doc, semKernelDictionary, word2vecSemKernelDictionary, wordEmbeddingDict,
										wordCluster, sentWeights);
								// append qid before result
								alteredResult.insert(0, qid + " ");
								String alteredAnnotatedSentence = s
										.getAnnotatedMentions(mention1,
												mention2);

								featuresIdx.put(label
										+ "\t"
										+ alteredResult
										+ "\t"
										+ mention1.getEntityReference()
												.getType()
										+ "@"
										+ mention2.getEntityReference()
												.getType() + "\t"
										+ alteredAnnotatedSentence,
										countInstance);
								countInstance++;

								// reset entity type
								mention1.getEntityReference().setType(
										entity1type);
								mention2.getEntityReference().setType(
										entity2type);
							}
										
						} else {
							message("skipping instance...");
							System.err.println("Skipping instance.. ");
							//add as DUMMY
							featuresIdx.put("NONE" + "\t"
									//+ qid+ " |BT| |ET|" //empty tree
									+ qid + " " + (skipString.isEmpty() ? "EMPTY" : skipString) //thien's addition
									+ "\t" + "ENTITY@ENTITY"
									+ "\t" + s.getAnnotatedSentence(),
									countInstance);
							countInstance++;
						}
					}

				}

				if (num_expected_relations_in_sentence
						- num_used_relations_in_sentence != 0) {
					message("Ignored %s relations (because mentions are far apart)",
							num_expected_relations_in_sentence
									- num_used_relations_in_sentence);
					for (IRelation r : s.getRelationsAsList()) {
						int numBetween = s.getNumMentionsInBetween(
								r.getFirstMention(), r.getSecondMention());
						if (numBetween > maxNumMentions.get()) {
							message(numBetween + " " + r);
						}
					}
				}

				// add even if we don't want it
				if (mentions.size() < 2) {
					message("Ignoring sentence since it has less than 2 mentions " + s.getSentenceId());
					boolean addDummy = true;
					if (addDummy) { //for input semod
						//add as DUMMY
						featuresIdx.put("NONE" + "\t"
								//+ qid+ " |BT| |ET|" //empty tree
								+ qid + " " + (skipString.isEmpty() ? "EMPTY" : skipString) //thien's addition
								+ "\t" + "ENTITY@ENTITY"
								+ "\t" + s.getAnnotatedSentence(),
								countInstance);
						countInstance++;
					}
				}
			} // end for each sentence

			// } //end else inverted pair

			message("Document: %s has %s positive and %s negative relation instances.",
					doc.getURI(), countPos, countNeg);
			if (countIgnoredRelBecauseOfMentionDistance > 0)
				message("Number relations ignored (because of Mention distance): "
						+ countIgnoredRelBecauseOfMentionDistance);

			if (countPos != doc.getNumRelations()) {
				message("some relations were ignored! Pos: " + countPos
						+ " vs. " + doc.getNumRelations());
			}

			// get next
			doc = (IAceRelationDocument) corpus.getNextDocument();
		}

	}
	
	


	private StringBuilder generateInstance(
			LinkedHashMap<String, FeaturesGroup<?>> groups2, Mention mention1,
			Mention mention2, Relation relation, Sentence s,
			IRelationDocument doc, SemKernelDictionary semKernelDictionary, Word2VecSemKernelDictionary word2vecSemKernelDictionary, WordEmbeddingDictionary wordEmbeddingDict,
			BrownWordCluster wordCluster, ClassificationScores sentWeights)
			throws Exception {

		ParseTree parseTreeParser1 = (ParseTree) doc
				.getBestConstituentParseTreeBySentenceId(s.getSentenceId());

		ParseTree parseTreeParser2 = null;
		if (inputDir2.get() != null) {
			parseTreeParser2 = (ParseTree) doc
					.getBestConstituentParseTreeSecondParserBySentenceId(s
							.getSentenceId());
		}

		DependencyInstance depTree = null;
		if (inputDirDep.get() != null) {
			depTree = doc.getBestDependencyTreeBySentenceId(s.getSentenceId());
		}

		StringBuilder result = new StringBuilder();
		boolean skipInstance = false;

		for (String groupName : groups.keySet()) {
			String toAdd;
			if (result.length() > 0) {
				result.append(" ");
			}

			AbstractTree parseTree;
			if (groupName.equals("parser2")) {
				parseTree = parseTreeParser2;
			} else {
				parseTree = parseTreeParser1;
			}
			if (groupName.equals("parserDep")) {
				parseTree = depTree;
			}

			try {

				toAdd = groups.get(groupName).extract(parseTree, mention1,
						mention2, relation, s, wordCluster, sentWeights, semKernelDictionary, word2vecSemKernelDictionary, wordEmbeddingDict);

				
				
				//FeaturesGroup<?> g = groups.get(groupName);
				if (toAdd.equals("") && !(groups.get(groupName) instanceof RelationExtractionLinearFeaturesGroup))
					skipInstance = true;

				result.append(toAdd);

			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		} // end group
		if (skipInstance)
			return null;
		else
			return result;
	}


//	private String convertFirstLetterToUpperCase(String word) {
//		StringBuilder b = new StringBuilder(word);
//		int i = 0;
//		do {
//		  b.replace(i, i + 1, b.substring(i,i + 1).toUpperCase());
//		  i =  b.indexOf(" ", i) + 1;
//		} while (i > 0 && i < b.length());
//
//		return word;
//	}


	private void readDependencyParseData(Corpus corpus) {
			//read dependency parsed data
		if (inputDirDep.get() != null) {
			if (fileEndingDependency.get() == null) {
				System.err.println("Must specify fileEndingDependency in XML config");
				System.exit(-1);
			}
			String inDir = inputDirDep.get().getAbsolutePath();
			File dir = new File(inDir);
			if (dir.isDirectory()) {

				File[] files = dir.listFiles(new ParsedDataFilter(fileEndingDependency.get()));

				if (files.length != corpus.getNumDocuments()) {
					System.err
							.println("Number of documents in corpus is different from number of files in parsed data!");
					System.exit(-1);
				}
				
				
				// for every document
				for (int i = 0; i < files.length; i++) {
					File file = files[i];
					message("Reading file: " + file.getName());
									
					IRelationDocument doc = (IRelationDocument) corpus.getDocumentByURI(file.getName());
					
					if (doc == null) {
						System.err.println("Document not found: "
								+ file.getName());
						System.exit(-1);
					} else {
						//message("Sentences in file: %s", doc.getSentences()
						//		.size());
						message("Entities: %s, Mentions: %s, Relations: %s",
								doc.getNumEntities(), doc.getNumMentions(), doc
										.getNumRelations());
					}
				
					boolean nbestMode=false;
						
					doc.addDependencyData(file.getAbsolutePath(),nbestMode);
					
					if (doc.getNumSentences() != doc.getNumDependencyTrees()) {
						System.err
								.println("number of sentences and trees differs! ["
										+ doc.getNumSentences()
										+ " sentences vs. "
										+ doc.getNumDependencyTrees()
										+ " trees]");
						System.exit(-1);
					} else {
						message(
								"%s parse trees read (num sentences matches num parses)",
								doc.getNumSentences());
					}

				
				} // end for every doc
			}
		}
	}


	private void readParsedConstituencyData(Corpus corpus) {
				
		// READ ONLY ONE PARSE DATA
		if (inputDir2.get() == null) {

			String inDir = inputDir.get().getAbsolutePath();
			File dir = new File(inDir);
			if (dir.isDirectory()) {

				File[] files = dir.listFiles(new ParsedDataFilter(fileEndingConstituency.get()));

				if (files.length != corpus.getNumDocuments()) {
					System.err
							.println("Number of documents in corpus is different from number of files in parsed data!");
					System.exit(-1);
				}

				// for every document
				for (int i = 0; i < files.length; i++) {
					File file = files[i];
					message("Reading file: " + file.getName());

					IRelationDocument doc = (IRelationDocument) corpus.getDocumentByURI(file
							.getName());
					if (doc == null) {
						System.err.println("Document not found: "
								+ file.getName());
						System.exit(-1);
					} else {
						//message("Sentences in file: %s", doc.getSentences()
						//		.size());
						message("Entities: %s, Mentions: %s, Relations: %s",
								doc.getNumEntities(), doc.getNumMentions(), doc
										.getNumRelations());
					}

					doc.addConstituencyData(file.getAbsolutePath());
					
					// check

					if (doc.getNumSentences() != doc.getNumParseTrees()) {
						System.err
								.println("number of sentences and trees differs! ["
										+ doc.getNumSentences()
										+ " sentences vs. "
										+ doc.getNumParseTrees()
										+ " trees]");
						System.exit(-1);
					} else {
						message(
								"%s parse trees read (num sentences matches num parses)",
								doc.getNumSentences());
					}		
				}

			}

		} // READ DATA ALSO FROM SECOND PARSER 
		else {

			String inDir = inputDir.get().getAbsolutePath();
			String inDir2 = inputDir2.get().getAbsolutePath();
			File dir = new File(inDir);
			File dir2 = new File(inDir2);
			if (dir.isDirectory() && dir2.isDirectory()) {
				File[] files = dir.listFiles(new ParsedDataFilter(fileEndingConstituency
						.get()));
				File[] files2 = dir2.listFiles(new ParsedDataFilter(fileEndingConstituency
						.get()));

				if (files.length != corpus.getNumDocuments() || files2.length != corpus.getNumDocuments() || files.length != files2.length) {
					System.err
							.println("Number of documents in corpus is different from number of files in parsed data!");
					System.exit(-1);
				}

				// for every document
				for (int i = 0; i < files.length; i++) {
					File file = files[i];
					File file2 = files2[i];
					message("Reading file: " + file.getName());
					message("Reading file: " + file2.getName());

					IAceRelationDocument doc = (IAceRelationDocument) corpus.getDocumentByURI(file
							.getName());
					if (doc == null) {
						System.err.println("Document not found: "
								+ file.getName());
						System.exit(-1);
					} else {
						message("Sentences in file: %s", doc.getSentences()
								.size());
						message("Entities: %s, Mentions: %s, Relations: %s",
								doc.getNumEntities(), doc.getNumMentions(), doc
										.getNumRelations());
					}

					doc.addConstituencyData(inputDir.get().getAbsolutePath());
					doc.addConstituencyData(inputDir2.get().getAbsolutePath());
					
					// check

					if (doc.getNumSentences() != doc.getNumParseTrees() || doc.getNumSentences() != doc.getNumParseTreesSecondParser()) {
						System.err
								.println("number of sentences and trees differs! ["
										+ doc.getNumSentences()
										+ " sentences vs. "
										+ doc.getNumParseTrees()
										+ " trees]");
						System.exit(-1);
					} else {
						message("%s parse trees read (num sentences matches num parses)",
								doc.getNumSentences());
					}

				}
			}
		}
	}
}