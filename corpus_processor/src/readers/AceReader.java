/**
 * 
 */
package readers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import annotation.Mention;
import annotation.NamedEntity;
import annotation.Relation;
import annotation.RelationMention;
import annotation.SimpleRelMention;
import utils.IntArray;
import xmlconversion.TagTypes;
import xmlconversion.XmlHelper;

/**
 * @author somasw000
 *
 */
public class AceReader { 
	public HashMap<String, NamedEntity> idNeM ;
	public HashMap<String, Mention> idMentionM ;
	//gidMenHm aims at keeping track of idMentionM in an increasing order based on the mention start bytes, it will 
	//be ordered following gidMenList;
	public HashMap<IntArray,String> gidMenHm;
	//gidMenStartList aims at keeping mentions in an increasing order based on the mention start bytes.
	public List<IntArray> gidMenStartList;
	public HashMap<String, Relation>  idRelationM ;
	public HashMap<String,SimpleRelMention> idSimRelMentionM;
	//idSimRelMentionHm is used to store bytes span of relation mention where id is the key and bytes spans of the pair
	//is the value
	public HashMap<SimpleRelMention,String> idSimRelMentionHm;
	public StringBuffer docBuf;
	
	//the following two aims at storing and retrieving relMentionId. Then, it is convinient to print out 
	//instances similar to xml files. It may be easier for debugging.
	//public List<IntArray> rIdIntList;
	//public HashMap<IntArray,String> idOutputHm;

	/**
	 * recursively traverses all the sub directories and process all the files.  
	 * @param dirName
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public void process (String dirName) throws IOException, ParserConfigurationException, SAXException{

		File dFile =  new File (dirName);
		if( dFile.isDirectory()){
			File files[]  =  dFile.listFiles();
			for(int i=0; i< files.length; i++){
				if(files[i].isFile()){
					String filename1 = files[i].getName();
					if(filename1.endsWith(".sgm")){
						System.out.println("sgm file name: "+filename1);
						String comname = filename1.substring(0,filename1.indexOf(".sgm"));
						for(int j=0;j<files.length;j++){
							String filename2=files[j].getName();
							if(filename2.endsWith(".apf.xml") && filename2.contains(comname)){
								System.out.println("apf.xml name: "+filename2);
								processDocument(files[i].getAbsolutePath(),files[j].getAbsolutePath());
							}
							continue;
						}
					}
				} else if(files[i].isDirectory()){
					process(files[i].getAbsolutePath());
				}
			}
		}
	}
	
	/**
	 * 
	 * @param sgmFname
	 * @param annotFname
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void processDocument(String sgmFname,  String annotFname) throws ParserConfigurationException, SAXException, IOException{
		idNeM = new HashMap<String, NamedEntity>();
		idMentionM =  new HashMap<String, Mention>();
		gidMenHm = new HashMap<IntArray,String>();
		gidMenStartList = new ArrayList<IntArray>();
		idRelationM  =  new HashMap<String, Relation>();
		idSimRelMentionM = new HashMap<String, SimpleRelMention>();
		idSimRelMentionHm = new HashMap<SimpleRelMention,String>();
		//rIdIntList = new ArrayList<IntArray>();
		//idOutputHm = new HashMap<IntArray,String>() ;
		docBuf =  SgmReader.readDoc(sgmFname);
		org.w3c.dom.Document document = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// File inputFile = new File(inputString);
		DocumentBuilder builder = factory.newDocumentBuilder();
		document = builder.parse(new File(annotFname));

		processEntities(document);
		processRelations(document);
		//System.out.println("")
		printRelations();
	}

	/**
	 * 
	 * @param st
	 * @param ed
	 * @return
	 */
	public String extractText (int st, int ed ){
		String str = docBuf.substring(st,ed);
		return str;
	}

	/**
	 * 
	 * @param document
	 */
	public void processRelations (org.w3c.dom.Document document){
		boolean debug = false;
		NodeList relList = document.getElementsByTagName(TagTypes.REL);
		for(int i=0;i<relList.getLength();i++){
			Node relNode = relList.item(i);
			Relation relation =  new Relation();
			List<Node> relMensList = XmlHelper.getChildrenByTagName(relNode, TagTypes.RELMEN);
			//System.out.println("relMensList size in processRelations of AceReader: "+relMensList.size());
			
			if(relMensList.size()!=0){
				//System.out.println("relNode in processRelations of AceReader:::::::::::::::::"+relNode.getNodeName() );
				relation=XmlHelper.setRelation(relNode);

				for(int j=0; j< relMensList.size(); j++) {
					RelationMention relMention  =  new RelationMention();
					SimpleRelMention simpleRelMention = new SimpleRelMention();
					Node relMenNode = relMensList.get(j);
					relMention = XmlHelper.setRelMention(relMenNode);
					int[] arg1ByteSpan = new int[2];
					int[] arg2ByteSpan = new int[2];
					arg1ByteSpan[0]=relMention.getArg1Start();
					arg1ByteSpan[1]=relMention.getArg1End();
					arg2ByteSpan[0]=relMention.getArg2Start();
					arg2ByteSpan[1]=relMention.getArg2End();
					simpleRelMention.setMen1Span(arg1ByteSpan);
					simpleRelMention.setMen2Span(arg2ByteSpan);
					//cool, relMentions of a relation may be more than one. But id of relMentions can be used to track their 
					//relations and also not identical with id of relations. So, idSimRelMentionM can store id of relMentions 
					//and two bytes spans of the two mentions of relMentions. Associations of all NEs, relations, relMentions 
					//and Mentions can be set up.
					idSimRelMentionM.put(relMention.getId(), simpleRelMention);
					idSimRelMentionHm.put(simpleRelMention, relMention.getId());
					relation.addRelationMention(relMention);
					if(debug) System.out.println("relation id ==" +relMenNode.getAttributes().getNamedItem(TagTypes.ID).getNodeValue());
					//relation.addRelationMention(relMention);
				}
				idRelationM.put(relation.getId(), relation);
				
				//String relType = relNode.getAttributes().getNamedItem(TagTypes.TYPE).getNodeValue();
				//String relSubType = relNode.getAttributes().getNamedItem(TagTypes.SUBTYPE).getNodeValue();
				//System.out.println("relation type: "+relType+" subtype:  "+relSubType);	
			}

		}
	}

	/**
	 * 
	 * @param document
	 */
	public void processEntities (org.w3c.dom.Document document){
		boolean debug =  false;
		NodeList entList = document.getElementsByTagName(TagTypes.ENT);

		for(int i= 0; i < entList.getLength(); i++){
			Node entityN =  entList.item(i);
			NamedEntity ne =  new NamedEntity ();
			if(debug) System.out.println(entityN.getNodeName() +"  "+entityN.getAttributes().getNamedItem(TagTypes.ID).getNodeValue());
			ne.setId(entityN.getAttributes().getNamedItem(TagTypes.ID).getNodeValue());
			ne.setType(entityN.getAttributes().getNamedItem(TagTypes.TYPE).getNodeValue());
			ne.setSubType(entityN.getAttributes().getNamedItem(TagTypes.SUBTYPE).getNodeValue());
			ne.setClassType(entityN.getAttributes().getNamedItem(TagTypes.CLASS).getNodeValue());
			NodeList mentionList  = entityN.getChildNodes();
			for(int j=0; j< mentionList.getLength(); j++){
				Node childN =  mentionList.item(j);
				if(debug) System.out.println(" \t mention node name == "+ childN.getNodeName());
				if(childN.getNodeName().equals(TagTypes.ENTMEN)) {
					Mention mention  = processMention(childN);
					ne.addMention(mention);
					idMentionM.put(mention.getId(), mention);
					int menStart = mention.getExtentSt();
					int menEnd = mention.getExtentEd();
					IntArray intArray = new IntArray(menStart,menEnd);
					gidMenStartList.add(intArray);
					gidMenHm.put(intArray, mention.getId());
				}
			}
			idNeM.put(ne.getId(), ne);
		}
	}

	/**
	 * 
	 * @param mentionN
	 * @return
	 */
	public Mention processMention (Node mentionN){
		boolean debug  =  false;
		Mention mentionAnnot = new Mention();
		
		if(debug)  System.out.println(mentionN.getAttributes().getNamedItem(TagTypes.TYPE).getNodeValue()+
				" + "+ mentionN.getAttributes().getNamedItem(TagTypes.ID).getNodeValue());
		//System.out.println(mentionN.getNodeName());
		Node extNode = XmlHelper.getFirstChildByTagName(mentionN,TagTypes.EXTENT);
		Node headNode = XmlHelper.getFirstChildByTagName(mentionN,TagTypes.HEAD);

		int extSpan []  =  processExtentHead(extNode);
		int headSpan [] =  processExtentHead(headNode);

		mentionAnnot.setId(mentionN.getAttributes().getNamedItem(TagTypes.ID).getNodeValue());
		mentionAnnot.setType(mentionN.getAttributes().getNamedItem(TagTypes.TYPE).getNodeValue());
		mentionAnnot.setExtentSt(extSpan[0]);
		mentionAnnot.setEntentEd(extSpan[1]);
		
		mentionAnnot.setHeadSt(headSpan[0]);
		mentionAnnot.setHeadEd(headSpan[1]);
		
		mentionAnnot.setExtentCoveredText(extractText(mentionAnnot.getExtentSt(), mentionAnnot.getExtentEd()+1));
		mentionAnnot.setHeadCoveredText(extractText(mentionAnnot.getHeadSt(), mentionAnnot.getHeadEd()+1));
		
		return mentionAnnot;
	}


	/**
	 * 
	 * @param aNode
	 * @return
	 */
	public int [] processExtentHead (Node aNode){
		//System.out.println(aNode.getNodeName());
		int[] aceByteSpan = new int[2];
		Node charNode = XmlHelper.getFirstChildByTagName(aNode,TagTypes.CHARSEQ);
		String start = charNode.getAttributes().getNamedItem(TagTypes.START).getNodeValue();
		String end = charNode.getAttributes().getNamedItem(TagTypes.END).getNodeValue();
		//Node startNode = XmlHelper.getFirstChildByTagName(charNode,TagTypes.START);
		//Node endNode = XmlHelper.getFirstChildByTagName(charNode,TagTypes.END);

		aceByteSpan[0] = Integer.parseInt(start);
		aceByteSpan[1] = Integer.parseInt(end);
		return aceByteSpan;
	}

	/**
	 * 
	 */
	public void printRelations(){
		for(Relation rel: idRelationM.values()){
			//System.out.println(rel.getId());
			System.out.println("\n\nRelation =="+rel.getPrintString()+"\nMENTION="+rel.getRelMentionString());
			
			for(int i=0; i< rel.getRelMentions().size(); i++){
				int arr  []  =  new int [4];
				
				RelationMention rmen  = rel.getRelMentions().get(i);
				System.out.println("\n mention-1 detials = "+idMentionM.get(rmen.getRefId1()).getPrintString());
				System.out.println("rmen.getRefId2 in AceReader : "+rmen.getRefId2()+" "+idMentionM.get(rmen.getRefId2()));
				System.out.println("\n mention-2 detials = "+idMentionM.get(rmen.getRefId2()).getPrintString());
				arr[0] = idMentionM.get(rmen.getRefId1()).getExtentSt();
				arr[1] = idMentionM.get(rmen.getRefId1()).getExtentEd();
				arr[2] = idMentionM.get(rmen.getRefId2()).getExtentSt();
				arr[3] = idMentionM.get(rmen.getRefId2()).getExtentEd();
				Arrays.sort(arr);
				int st =  arr[0]-10;
				int ed  =  arr[3]+10;
				if(st <0) st =0;
				if(ed> docBuf.length()) ed  =  docBuf.length()-1;
				String context =  extractText(st, ed);
				System.out.println("\nCONTEXT "+ st+"_"+ed+" == "+ context.replaceAll("\n", ""));
			}
			//NamedEntities have been set all at once by the method processEntities and are stored into idNeM
			//then, setRelation defined in XmlHelper will set EntityArgs. So, a Relation can easily find their NamedEntities by the following
			//methods. setRelation is called in ACEReader by processRelation
			NamedEntity ne1  =  idNeM.get(rel.getEntityArg1());
			NamedEntity ne2  =  idNeM.get(rel.getEntityArg2());
			//System.out.println("in printRelations of ACEReader: "+rel.getEntityArg1()+" "+rel.getEntityArg2());
			
			System.out.println("\n Enitiy-1 detials = "+ne1.getPrintString());
			System.out.println("\n Enitiy-2 detials = "+ne2.getPrintString());

			//			System.out.println("\n Enitiy-1 detials = "+ne1.getPrintString()+"\n"+ne1.getMentionString());
//			System.out.println("\n Enitiy-2 detials = "+ne2.getPrintString()+"\n"+ne2.getMentionString());
		}
	}
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		if (args.length < 2){
			System.out.println("Missing sgm file and annotaton file");
			return;
		}
		String sgmFname = args[0];
		String annotFname = args[1];

		
		AceReader aread  = new AceReader();
		aread.processDocument(sgmFname, annotFname);
		System.out.println(aread.getDocBuf().toString());

	}

	public HashMap<String, NamedEntity> getIdNeM() {
		return idNeM;
	}

	public HashMap<String, Mention> getIdMentionM() {
		return idMentionM;
	}

	public HashMap<String, Relation> getIdRelationM() {
		return idRelationM;
	}

	public HashMap<String, SimpleRelMention> getIdSimRelMentionM() {
		return idSimRelMentionM;
	}

	public HashMap<SimpleRelMention, String> getIdSimRelMentionHm() {
		return idSimRelMentionHm;
	}

	public StringBuffer getDocBuf() {
		return docBuf;
	}

	public HashMap<IntArray, String> getGidMenHm() {
		return gidMenHm;
	}
	


//	public HashMap<IntArray, String> getIdOutputHm() {
//		return this.idOutputHm;
//	}
//	
//	public void setIdOutputHm(HashMap<IntArray,String> idOutputHm){
//		this.idOutputHm =  idOutputHm;
//	}
//
//	public List<IntArray> getrIdIntList() {
//		return this.rIdIntList;
//	}
//	
//	public void setRidIntList(List<IntArray> rIdIntList){
//		this.rIdIntList = rIdIntList;
//	}
	
	public void setGidMenHm(HashMap<IntArray, String> gidMenHm) {
		this.gidMenHm = gidMenHm;
	}

	public List<IntArray> getGidMenStartList() {
		return gidMenStartList;
	}

	public void setGidMenStartList(List<IntArray> gidMenStartList) {
		this.gidMenStartList = gidMenStartList;
	}

	public void setIdNeM(HashMap<String, NamedEntity> idNeM) {
		this.idNeM = idNeM;
	}

	public void setIdMentionM(HashMap<String, Mention> idMentionM) {
		this.idMentionM = idMentionM;
	}

	public void setIdRelationM(HashMap<String, Relation> idRelationM) {
		this.idRelationM = idRelationM;
	}

	public void setIdSimRelMentionM(
			HashMap<String, SimpleRelMention> idSimRelMentionM) {
		this.idSimRelMentionM = idSimRelMentionM;
	}

	public void setIdSimRelMentionHm(
			HashMap<SimpleRelMention, String> idSimRelMentionHm) {
		this.idSimRelMentionHm = idSimRelMentionHm;
	}

	public void setDocBuf(StringBuffer docBuf) {
		this.docBuf = docBuf;
	}

}
