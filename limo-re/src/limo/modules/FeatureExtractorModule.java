package limo.modules;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.w3c.dom.Element;

import limo.exrel.utils.Index;
import limo.exrel.utils.Index.OpenMode;

import limo.exrel.features.AbstractFeature;

import limo.exrel.Exrel;
import limo.exrel.features.RelationExtractionLinearFeaturesGroup;
import limo.exrel.features.RelationExtractionStructuredFeaturesGroup;
import limo.exrel.features.Separator;
//import limo.exrel.features.StructuredFeaturesGroup;
import limo.exrel.utils.XMLConfig;
import limo.exrel.utils.XMLTools;

import limo.exrel.features.FeaturesGroup;

import limo.exrel.slots.OutputFileSlot;
import limo.exrel.slots.XMLConfigSlot;

import limo.core.Corpus;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.interfaces.IAceRelationDocument;
import limo.core.interfaces.IRelationDocument;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.modules.AbstractModule;
import limo.exrel.slots.BooleanSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.slots.IntegerSlot;
import limo.exrel.slots.StringSlot;
import limo.io.IRelationReader;
import limo.cluster.BrownWordCluster;
import limo.cluster.SemKernelDictionary;
import limo.cluster.io.BrownWordClusterReader;
import limo.cluster.io.ScoreReader;
import limo.cluster.io.SemKernelDictionaryReader;

public class FeatureExtractorModule  extends AbstractModule {

	public FileSlot inputFile = new FileSlot(false); //optional: if not given, read from stdin
	public StringSlot readerClass = new StringSlot(true);
		
	public OutputFileSlot outExamplesIdxFile = new OutputFileSlot(true);
	public XMLConfigSlot featuresLayoutXML = new XMLConfigSlot(true);
	public StringSlot featuresDictionary = new StringSlot(true);
	
	public StringSlot mentionOrderMethod = new StringSlot(false); //optional
	
	
	public BooleanSlot directedClassification = new BooleanSlot("true"); //default is true
	public StringSlot granularityClassification = new StringSlot(true); //type or subtype
	
	public FileSlot wordClusterFilePath = new FileSlot(false); 
	
	public FileSlot semanticKernelDictionary = new FileSlot(false);
	
	public BooleanSlot restrictMentionsInBetween = new BooleanSlot("false"); //default is false
	public IntegerSlot maxNumMentions = new IntegerSlot(4);
	
	public static String subtypeDivChar="--";
	
	private LinkedHashMap<String, FeaturesGroup<?>> groups = new LinkedHashMap<String, FeaturesGroup<?>>();	

	
	private boolean addDummy = false; // semod requires this to be true (to have instance even if we don't need it)
	
	public FeatureExtractorModule(String moduleId, String configId) {
		super(moduleId,configId);
	}

	
	@Override
	public void _init(Exrel system, Element e) {
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
				//System.out.println("TODO add strcuturedgroup");
			} else {
				throw new RuntimeException(String.format("Unknown feature group type: %s", elem.getNodeName()));
			}
		}
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
		
		String inFile = null;
		if (inputFile.get() != null)
			inFile = inputFile.get().getAbsolutePath();

		message("Reading parsed data");
		message("InputDir: %s", inFile);

		Index featuresIdx = new Index(outExamplesIdxFile.get(), OpenMode.WRITE);
		
		Corpus corpus;
		
		Class<IRelationReader> classIRelReader;
		IRelationReader reader = null;

		try {
			classIRelReader = (Class<IRelationReader>) Class.forName(readerClass.get());
			@SuppressWarnings("rawtypes")
			Class[] types = new Class[] { String.class };
			Constructor<IRelationReader> cons = classIRelReader.getConstructor(types);
			Object[] args = new Object[] { inFile };
			reader = cons.newInstance(args);
				
			
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
		
		corpus = new Corpus(reader);
		corpus.init();
		message("Number documents read: "+ corpus.getNumDocuments());
		

		try {
			generatePossibleRelationsAndextractFeatures(corpus, featuresIdx);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		featuresIdx.close();
		
		for (FeaturesGroup<?> group : groups.values()) {
			group.cleanup();
		}
	}

	
	



	private void generatePossibleRelationsAndextractFeatures(Corpus corpus, Index featuresIdx) throws Exception {
		
		BrownWordCluster wordCluster = null;
		SemKernelDictionary semKernelDictionary  = null;
		
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
		
		
		// generate possible relations
		message("Generating possible relations.");
		IRelationDocument doc = (IRelationDocument) corpus.getNextDocument();
		int countInstance = 0;
		
		int instanceId = 0;
		
		while (doc != null) {
			message("Extracting features from document: %s", doc.getURI());
			
			if (mentionOrderMethod.get() != null)
				message("Using mention order: "+ mentionOrderMethod.get());
			
			if (restrictMentionsInBetween.get() == true)
				message("Restrict num mentions in between to "+maxNumMentions.get()+ " mentions.");
			
			int countPos = 0;
			int countNeg = 0;
			
			message("Number of sentences in document: "+doc.getSentences().size());
			
			// for every sentence
			for (Sentence s : doc.getSentences()) {
				Relations sentenceRelations = s.getRelations();
				ArrayList<Mention> mentions = s.getMentions();
				
				//message(s.toString());
				//System.out.println("*****"+s.toString());
				/*message("Number of mentions for sentence: " + mentions.size());
				if (mentions.size() < 2) {
					message("Ignoring sentence since it has less than 2 mentions " + s.toString());
					//System.out.println(s.getAnnotatedSentence());
					continue;
				}*/
				
				
				//String qid = "qid:"+s.getSentenceId(); //this is document-level id
				String qid = "qid:"+instanceId;
				instanceId++;
				
				//System.out.println("num mentions: "+s.getMentions().size());
				ParseTree parseTree = (ParseTree) doc.getBestConstituentParseTreeBySentenceId(s.getSentenceId());
				
				
				// only consider a pair once 
				for (int i = 0; i < mentions.size(); i++) {
				
				    for (int j = i+1; j < mentions.size(); j++) {
								
					
						boolean skipInstance = false;
						Mention mention1 = mentions.get(i);
						Mention mention2 = mentions.get(j);
						
						//if (!(mention1.isInRelation() || mention2.isInRelation()))
						//	continue;
						
						
						if (!mention1.equals(mention2)) { 
							if (!equalSpan(mention1.getTokenIds(), mention2
									.getTokenIds())) {
								
								//System.out.println(mention1 + " " + mention2);
								//if we generate all mentions, need to take care of order! //.getRelationInOrder(mention1, mention2);

						

									
									// do not generate inverted pair
									Relation relation = (Relation) sentenceRelations
											.getRelation(mention1,
													mention2);
									
									
									StringBuffer result = new StringBuffer();
									String label = "NONE";
									
									// if we only consider at most three mentions in between (only for negatives! and only in training mode!)
									if (restrictMentionsInBetween.get() == true) {
									    //if (relation==null) {
									    //if (!getConfigId().equals("test") && relation == null) 
									    int maxMen = maxNumMentions.get(); 
									    if ((j-i)>(maxMen+1))
										 break;
									    //}
									}
										
											
									
															
									if (relation != null) {
										countPos++;
										if (granularityClassification.get().equals("type")) {
											label = relation
												.getRelationType();
										} else if (granularityClassification.get().equals("subtype")) {
											label = relation
													.getRelationType() + subtypeDivChar + relation.getRelationSubType();
										}else {
											System.err.println("no valid value given for granularityClassification: type|subtype.");
											System.exit(-1);
										}
									} else {
										countNeg++;
									}
									
									for (String groupName : groups.keySet()) {
										String toAdd;
										if (result.length() > 0) {
											result.append(" ");
										}

										
										try {

						
												toAdd = groups.get(
														groupName).extract(
														parseTree,
														mention1, mention2,
														relation, s, null);
										
											if (toAdd.equals(""))
												skipInstance = true;
											
//											//both semkernel and wordcluster are active
//											if (semKernelDictionary != null && wordCluster != null && groupName.startsWith("wc-")) {
//												if (groupName.equals("wc-3trees")) {
//												    //add both trees to original!!
//													   ParseTree treeWC = new ParseTree(toAdd);
//													   String originalTree = toAdd; 
//
//													   for (LeafNode terminal : treeWC.getTerminals()) {
//														   if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//															   continue; // don't do this for mention and entity type in pethigher
//															   
//														    String word = terminal.getSurface();
//							 
//														    String bitstring = wordCluster.getPrefixClusterId(word,10);
//														  
//														    if (bitstring != null) {
//														    	String firstChild = bitstring;
//														        terminal.setLabel(firstChild);
//															    //terminal.setSurface(secondChild);
//														    }
//													   }
//													   ParseTree tree = new ParseTree(toAdd);
//                                                                                                           for (LeafNode terminal : tree.getTerminals()) {
//													       if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//														   continue; // don't do this for mention and entity type in pethigher                                                                                                                                 
//
//													       String word = terminal.getSurface();
//
//													       String clusterId = semKernelDictionary.getPrefixedWordIndex(word);
//
//													       if (clusterId != null) {
//														   terminal.setSurface(clusterId);
//													       }
//                                                                                                           }
//
//
//	      
//     													toAdd = originalTree +" |BT| " + treeWC.toString() + " |BT| " + tree.toString();
//												} 	
//												
//											}
//											else {
//											// only semkernel is active
//											    if (semKernelDictionary != null && groupName.equals("wc-2trees")) {
//												String originalTree = toAdd; 
//												ParseTree tree = new ParseTree(toAdd);
//												for (LeafNode terminal : tree.getTerminals()) {
//												    if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//													continue; // don't do this for mention and entity type if pethigher                                                                                                                                            
//												    String word = terminal.getSurface();
//												    //String pos = terminal.getLabel();                                                                                                                                                                                
//												    String clusterId = semKernelDictionary.getPrefixedWordIndex(word);
//												    if (clusterId != null) {
//													terminal.setSurface(clusterId);
//												    }
//
//												}
//												toAdd = originalTree + " |BT| " + tree.toString();
//
//											} else if (semKernelDictionary != null && (groupName.equals("str1") || groupName.startsWith("wc-"))) {
//											    ParseTree tree = new ParseTree(toAdd);
//												   for (LeafNode terminal : tree.getTerminals()) {
//													   if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//													    	continue; // don't do this for mention and entity type if pethigher
//													    String word = terminal.getSurface();
//													    //String pos = terminal.getLabel();
//													    String clusterId = semKernelDictionary.getPrefixedWordIndex(word);
//													    if (clusterId != null) {
//														    terminal.setSurface(clusterId);
//													    }
//													    
//													}
//												   toAdd = tree.toString();
//												
//											}
//											
//											// only wordCluster is active
//											if (wordCluster != null) {
//																							
//												if (groupName.equals("wc-2trees")) {
//													// 2 trees
//													ParseTree tree = new ParseTree(toAdd);
//													String originalTree = toAdd; 
//	
//													for (LeafNode terminal : tree.getTerminals()) {
//													    if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//													    	continue; // don't do this for mention and entity type if pethigher
//													    
//														String word = terminal.getSurface();
//														//String bitstring = wordCluster.getFullClusterId(word);
//														String bitstring = wordCluster.getPrefixClusterId(word,10);
//														if (bitstring != null) {
//														    //terminal.setSurface(bitstring);
//														    terminal.setLabel(bitstring);
//														}
//													}
//													toAdd = originalTree + " |BT| " + tree.toString();
//												}
//												else if (groupName.equals("wc-hybridOr")) {
//											    //hybrid tree
//												    ParseTree tree = new ParseTree(toAdd);
//												   for (LeafNode terminal : tree.getTerminals()) {
//													   if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//														   continue; // don't do this for mention and entity type in pethigher
//													    String word = terminal.getSurface();
//													    String pos = terminal.getLabel();
//													    String bitstring = wordCluster.getPrefixClusterId(word,10);
//													    if (bitstring != null) {
//														    String firstChild = pos+" (* "+word+")";
//														    String secondChild = "(* "+bitstring+")";
//														    terminal.setLabel(firstChild);
//														    terminal.setSurface(secondChild);
//													    }
//													    
//													}
//												   toAdd = tree.toString();
//												}
//												else if (groupName.equals("wc-hybridOrOnlyMentions")) {
//												    //hybrid tree
//													    ParseTree tree = new ParseTree(toAdd);
//													   for (LeafNode terminal : tree.getTerminals()) {
//														   if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//															   continue; // don't do this for mention and entity type in pethigher
//															if (terminal.getSurface().equals(mention1.getHead()) ||
//															    	terminal.getSurface().equals(mention2.getHead())) {
//																
//															    String word = terminal.getSurface();
//															    if (mention1.getType().equals("NAM") && terminal.getSurface().equals(mention1.getHead()) ||
//															       (mention2.getType().equals("NAM") && terminal.getSurface().equals(mention2.getHead()))) {
//															    	char firstChar = word.charAt(0);
//															    	if (Character.isLowerCase(firstChar)) {
//															    		word = convertFirstLetterToUpperCase(word);
//															    	}
//															    }
//															    		
//															    String pos = terminal.getLabel();
//															    String bitstring = wordCluster.getPrefixClusterId(word,10);
//															    if (bitstring != null) {
//																    String firstChild = pos+" (* "+word+")";
//																    String secondChild = "(* "+bitstring+")";
//																    terminal.setLabel(firstChild);
//																    terminal.setSurface(secondChild);
//															    }
//														   }
//														}
//													   toAdd = tree.toString();
//													}
//												 //hybrid tree (replace pos with 10bit bitstring)
//												else if (groupName.equals("wc-hybridPos")) {
//												   ParseTree tree = new ParseTree(toAdd);
//												   for (LeafNode terminal : tree.getTerminals()) {
//													   if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//														   continue; // don't do this for mention and entity type in pethigher
//													    String word = terminal.getSurface();
//													   
//													    String bitstring = wordCluster.getPrefixClusterId(word,10);
//													    //String bitstring = wordCluster.getFullClusterId(word);
//													    if (bitstring != null) {
//														    //String firstChild = pos+":"+bitstring;
//													    	String firstChild = bitstring;
//														    //String secondChild = "(* "+bitstring+")";
//														    terminal.setLabel(firstChild);
//														    //terminal.setSurface(secondChild);
//													    }
//													    
//													}
//													
//													toAdd = tree.toString();
//												}
//												else if (groupName.equals("wc-PosOnly")) {
//													
//												    ParseTree tree = new ParseTree(toAdd);
//												    for (LeafNode terminal : tree.getTerminals()) {
//												    	   if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//															   continue; // don't do this for mention and entity type in pethigher
//													String word = terminal.getSurface();
//
//													String bitstring = wordCluster.getPrefixClusterId(word,10);
//													//String bitstring = wordCluster.getFullClusterId(word);                                                                                                                                                           
//													if (bitstring != null) {
//													    //String firstChild = pos+":"+bitstring;                                                                                                                                                                   
//													    String firstChild = bitstring;
//													    terminal.setLabel(firstChild);
//													    terminal.setSurface("*");                                                                                                                                                                        
//													}
//
//												    }
//
//												    toAdd = tree.toString();
//                                                                                                }
//												else if (groupName.equals("wc-ReplaceWord")) {
//												   ParseTree tree = new ParseTree(toAdd);
//												   for (LeafNode terminal : tree.getTerminals()) {
//													   
//													   if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//														   continue; // don't do this for mention and entity type in pethigher
//													   
//													    String word = terminal.getSurface();
//													   
//													    String bitstring = wordCluster.getPrefixClusterId(word,10);
//													    //String bitstring = wordCluster.getFullClusterId(word);
//													    if (bitstring != null) {
//														    //String firstChild = pos+":"+bitstring;
//													    	    //String firstChild = bitstring;
//														    //String secondChild = "(* "+bitstring+")";
//														    //terminal.setLabel(firstChild);
//														    terminal.setSurface(bitstring);
//													    }
//													    
//													}
//													
//													toAdd = tree.toString();
//												}
//												else if (groupName.equals("wc-InsertAbovePos")) {
//												   ParseTree tree = new ParseTree(toAdd);
//												   for (LeafNode terminal : tree.getTerminals()) {
//													   if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//														   continue; // don't do this for mention and entity type in pethigher
//													    String word = terminal.getSurface();
//													    String pos = terminal.getLabel();
//													    String bitstring = wordCluster.getPrefixClusterId(word,10);
//													    //String bitstring = wordCluster.getFullClusterId(word);
//													    if (bitstring != null) {
//														terminal.setLabel(bitstring + " ("+pos);
//														terminal.setSurface(word+")");
//													    }
//													    
//													}
//													
//													toAdd = tree.toString();
//												}
//												if (groupName.equals("wc-hybridPosMentionsOnly")) {
//												    ParseTree tree = new ParseTree(toAdd);
//
//												    for (LeafNode terminal : tree.getTerminals()) {
//													if (terminal.getLabel().equals("MT") || terminal.getLabel().equals("ET"))
//													    continue; // don't do this for mention and entity type in pethigher                                                                                                                                 
//
//													if (mention1.spansOverTerminal(terminal) || mention2.spansOverTerminal(terminal)) {
//													    String word = terminal.getSurface();
//
//													    String bitstring = wordCluster.getPrefixClusterId(word,10);                                                                                                                                      
//													    
//
//													    if (bitstring != null) {
//														//String firstChild = pos+":"+bitstring;                                                                                                                                                   
//														String firstChild = bitstring;
//														//String secondChild = "(* "+bitstring+")";                                                                                                                                                
//														terminal.setLabel(firstChild);
//														//terminal.setSurface(secondChild);                                                                                                                                                        
//													    }
//													}
//												    }
//												    toAdd = tree.toString();
//                                                                                                }
//
//												
//											} // end wordcluster
//											} // end else (from wc + semtk)
											result.append(toAdd);

										} catch (SecurityException e) {
											e.printStackTrace();
										} catch (IllegalArgumentException e) {
											e.printStackTrace();
										}
									} // end group
									
								
									
									
									if (!skipInstance) {


										if (directedClassification.get() == true) {
											if (granularityClassification.get().equals("type")) {
												if (!label.startsWith("NONE")
														&& !label.startsWith("PER-SOC") && relation != null) {
													if (relation.getFirstMention().equals(mention1))
														label+= ".A-B";
													else
														label+= ".B-A";
												}	
											} else if (granularityClassification.get().equals("subtype")) {
												//6 of 23 relation subtypes in ACE 2004 are symmetric:
												//TODO: export this to file, not hard-coding in here
												if (!label.startsWith("NONE")) {
													if (!label.equals("PHYS"+subtypeDivChar+"Near") ||
														!label.startsWith("PER-SOC") || //3 in here
														!label.equals("EMP-ORG"+subtypeDivChar+"Partner") ||
														!label.equals("EMP-ORG"+subtypeDivChar+"Other"))  {
														if (relation.getFirstMention().equals(mention1))
															label+= ".A-B";
														else
															label+= ".B-A";
													}
												}
												
											} else {
												System.err.println("not a valid classification granularity: "+ granularityClassification.get());
												System.exit(-1);
											}
										}
										
										//append qid before result
										result.insert(0, qid + " ");

										// annotatedSentence: mark entities in sentence
										//String annotatedSentence = s.annotateSentence(mention1, mention2);
										String annotatedSentence = s.getAnnotatedMentions(mention1, mention2);
										
										
										featuresIdx.put(label + "\t"
												+ result.toString() 
												+ "\t" + mention1.getEntityReference().getType() + "@"+mention2.getEntityReference().getType()
												+ "\t" + annotatedSentence,
												countInstance);
										countInstance++;

									} else {
										System.err
												.println("Skipping instance: "
														+ result.toString());
									}
								}
									
								
							}
						}
					//} //end else inverted pair
				}
				
				message("Number of mentions for sentence: " + mentions.size());
				
				if (mentions.size() < 2) {
					message("Ignoring sentence since it has less than 2 mentions " + s.getSentenceId());
					if (addDummy) { //for input semod
						//add as DUMMY
						featuresIdx.put("NONE" + "\t"
								+ qid+ " |BT| |ET|" //empty tree
								+ "\t" + "ENTITY@ENTITY"
								+ "\t" + s.getAnnotatedSentence(),
								countInstance);
						countInstance++;
					}
				}
				
				
			}
			
			
			
			
			message("Document: %s has %s positive and %s negative relation instances.",
					doc.getURI(), countPos, countNeg);
			
			if (countPos != doc.getNumRelations()) {
				System.out.println("some relations were ignored! Pos: "+countPos +" vs. "+doc.getNumRelations());
			}
			//get next
			doc = (IAceRelationDocument) corpus.getNextDocument();
		}
		
	}
	


	private String convertFirstLetterToUpperCase(String word) {
		StringBuilder b = new StringBuilder(word);
		int i = 0;
		do {
		  b.replace(i, i + 1, b.substring(i,i + 1).toUpperCase());
		  i =  b.indexOf(" ", i) + 1;
		} while (i > 0 && i < b.length());

		return word;
	}


	private boolean equalSpan(int[] tokenIds, int[] tokenIds2) {
		if (tokenIds.length != tokenIds2.length)
			return false;
		else 
			for (int i = 0; i <= tokenIds.length-1; i++) {
				if (tokenIds[i] != tokenIds2[i])
					return false;
			}
			return true;
	}


/*	private void readDependencyParseData(Corpus corpus) {
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
					ArrayList<ArrayList<DependencyInstance>> dependencyParseTrees = new ArrayList<ArrayList<DependencyInstance>>();
					
					IRelationDocument doc = corpus.getDocumentByURI(file
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
					try {
						DependencyReader depReader = DependencyReader.createDependencyReader("CONLL07",true);
						depReader.startReading(file.getAbsolutePath());
						
						ArrayList<DependencyInstance> parsesOfSentence;
				
						while ((parsesOfSentence = depReader.getNext()) != null) {
							dependencyParseTrees.add(parsesOfSentence);
						}
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					if (doc.getNumSentences() != dependencyParseTrees.size()) {
						System.err
								.println("number of sentences and trees differs! ["
										+ doc.getNumSentences()
										+ " sentences vs. "
										+ dependencyParseTrees.size()
										+ " trees]");
						System.exit(-1);
					} else {
						message(
								"%s parse trees read (num sentences matches num parses)",
								doc.getNumSentences());
					}

					// add parse trees to document
					doc.setDependencyParseTrees(dependencyParseTrees);
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

					IRelationDocument doc = corpus.getDocumentByURI(file
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

					ArrayList<ArrayList<ParseTree>> constituentParseTrees = new ArrayList<ArrayList<ParseTree>>();

					// read parsed data
					try {
						BufferedReader inputReader = new BufferedReader(
								new InputStreamReader(new FileInputStream(file
										.getAbsolutePath()), "UTF-8"));
						String line = inputReader.readLine();
						StringBuilder parseTreeStr = new StringBuilder();
						int currentIndex = 0;
						while (line != null) {
							if (!line.equals(""))
								parseTreeStr.append(line);
							else {

								ParseTree tree = new ParseTree(parseTreeStr
										.toString());
								parseTreeStr = new StringBuilder();

								// System.out.println(tree.getTerminalsAsString());
								ArrayList<ParseTree> parses =new ArrayList<ParseTree>();
								parses.add(tree);
								
								constituentParseTrees.add(parses);

								// check if terminals match
								Sentence s = doc.getSentenceById(currentIndex);

								String terminalsTree = tree
										.getTerminalsAsString();
								String sentence = s.toString();
								if (!terminalsTree.equals(sentence)) {
									System.err
											.println("***** ERROR Terminals differ! "
													+ doc.getURI()
													+ " sentence:"
													+ currentIndex);
									System.err.println(terminalsTree);
									System.err.println(sentence);
								}

								currentIndex += 1;
							}
							line = inputReader.readLine();
							// System.out.println(line);
						}

					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}

					// check

					if (doc.getNumSentences() != constituentParseTrees.size()) {
						System.err
								.println("number of sentences and trees differs! ["
										+ doc.getNumSentences()
										+ " sentences vs. "
										+ constituentParseTrees.size()
										+ " trees]");
						System.exit(-1);
					} else {
						message(
								"%s parse trees read (num sentences matches num parses)",
								doc.getNumSentences());
					}

					// add parse trees to document
					doc.setConstituentParseTrees(constituentParseTrees);
					
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

					IRelationDocument doc = corpus.getDocumentByURI(file
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

					ArrayList<ArrayList<ParseTree>> constituentParseTreesListOfListsParser1 = new ArrayList<ArrayList<ParseTree>>();
					
					ArrayList<ArrayList<ParseTree>> constituentParseTreesListOfListsParser2 = new ArrayList<ArrayList<ParseTree>>();

					// read parsed data
					try {
						//file1
						BufferedReader inputReader = new BufferedReader(
								new InputStreamReader(new FileInputStream(file
										.getAbsolutePath()), "UTF-8"));
						String line = inputReader.readLine();
						StringBuilder parseTreeStr = new StringBuilder();
						int currentIndex = 0;
						while (line != null) {
							if (!line.equals(""))
								parseTreeStr.append(line);
							else {

								ParseTree tree = new ParseTree(parseTreeStr
										.toString());
								parseTreeStr = new StringBuilder();

								ArrayList<ParseTree> parsesOfSentence = new ArrayList<ParseTree>();
								parsesOfSentence.add(tree);
								// System.out.println(tree.getTerminalsAsString());
								constituentParseTreesListOfListsParser1.add(parsesOfSentence);

								// check if terminals match
								Sentence s = doc.getSentenceById(currentIndex);

								String terminalsTree = tree
										.getTerminalsAsString();
								String sentence = s.toString();
								if (!terminalsTree.equals(sentence)) {
									System.err
											.println("***** ERROR Terminals differ! "
													+ doc.getURI()
													+ " sentence:"
													+ currentIndex);
									System.err.println(terminalsTree);
									System.err.println(sentence);
								}

								currentIndex += 1;
							}
							line = inputReader.readLine();
							// System.out.println(line);
						}
						
						BufferedReader inputReader2 = new BufferedReader(
								new InputStreamReader(new FileInputStream(file2
										.getAbsolutePath()), "UTF-8"));
						String line2 = inputReader2.readLine();
						StringBuilder parseTreeStr2 = new StringBuilder();
						int currentIndex2 = 0;
						while (line2 != null) {
							if (!line2.equals(""))
								parseTreeStr2.append(line2);
							else {

								ParseTree tree2 = new ParseTree(parseTreeStr2
										.toString());
								parseTreeStr2 = new StringBuilder();

								ArrayList<ParseTree> parsesOfSentence2 = new ArrayList<ParseTree>();
								parsesOfSentence2.add(tree2);
								// System.out.println(tree.getTerminalsAsString());
								constituentParseTreesListOfListsParser2.add(parsesOfSentence2);

								// check if terminals match
								Sentence s = doc.getSentenceById(currentIndex2);

								String terminalsTree = tree2
										.getTerminalsAsString();
								String sentence = s.toString();
								if (!terminalsTree.equals(sentence)) {
									System.err
											.println("***** ERROR Terminals differ! "
													+ doc.getURI()
													+ " sentence:"
													+ currentIndex);
									System.err.println(terminalsTree);
									System.err.println(sentence);
								}

								currentIndex2 += 1;
							}
							line2 = inputReader2.readLine();
							// System.out.println(line);
						}

					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}

					// check

					if (doc.getNumSentences() != constituentParseTreesListOfListsParser1.size()) {
						System.err
								.println("number of sentences and trees differs! ["
										+ doc.getNumSentences()
										+ " sentences vs. "
										+ constituentParseTreesListOfListsParser1.size()
										+ " trees]");
						System.exit(-1);
					} else {
						message(
								"%s parse trees read (num sentences matches num parses)",
								doc.getNumSentences());
					}
					if (constituentParseTreesListOfListsParser1.size() != constituentParseTreesListOfListsParser2.size())
						System.err.println("parser list length differs!");
					
					// add parse trees to document
					doc.setConstituentParseTrees(constituentParseTreesListOfListsParser1);
					doc.setConstituentParseTrees2(constituentParseTreesListOfListsParser2);
				}

			}
			
		}
		
	}*/

}
