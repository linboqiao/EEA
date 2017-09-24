package xmlconversion;

/*
 * @author Dingcheng Li lidi00000
 */

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import annotation.Relation;
import annotation.RelationMention;

public class XmlHelper {
	public static Node getFirstChildByTagName(Node parent, String tagName) {
		NodeList nodeList = parent.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.getNodeName().equalsIgnoreCase(tagName))
				return node;
		}
		return null;
	}

	public static List<Node> getChildrenByTagName(Node parent, String tagName) {
		List<Node> eleList = new ArrayList<Node>();
		NodeList nodeList = parent.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.getNodeName().equalsIgnoreCase(tagName)) {
				eleList.add(node);
			}

		}
		return eleList;
	}
	
	public static int[] extractAceXmlByteSpan(Node menNode){
		int[] aceByteSpan = new int[2];
		Node extNode = XmlHelper.getFirstChildByTagName(menNode,TagTypes.EXTENT);
		Node charNode = XmlHelper.getFirstChildByTagName(extNode,TagTypes.CHARSEQ);
		Node startNode = XmlHelper.getFirstChildByTagName(charNode,TagTypes.START);
		Node endNode = XmlHelper.getFirstChildByTagName(charNode,TagTypes.END);
		String menType = menNode.getAttributes().getNamedItem(TagTypes.TYPE).getNodeValue();
		//System.out.println(startNode.getTextContent()+" "+endNode.getTextContent());
//		if(!menType.equals("PRONOUN")){
//			aceByteSpan[0] = Integer.parseInt(startNode.getTextContent());
//			aceByteSpan[1] = Integer.parseInt(endNode.getTextContent());								
//		}
		aceByteSpan[0] = Integer.parseInt(startNode.getTextContent());
		aceByteSpan[1] = Integer.parseInt(endNode.getTextContent());
		return aceByteSpan;
	}
	
	public static Relation setRelation(Node relNode){
		Relation relation =  new Relation();
		relation.setId(relNode.getAttributes().getNamedItem(TagTypes.ID).getNodeValue());
		relation.setType(relNode.getAttributes().getNamedItem(TagTypes.TYPE).getNodeValue());
		if(relNode.getAttributes().getNamedItem(TagTypes.SUBTYPE)!=null){
			relation.setSubtype(relNode.getAttributes().getNamedItem(TagTypes.SUBTYPE).getNodeValue());
		}
		if(relNode.getAttributes().getNamedItem(TagTypes.TENSE)!=null){
			relation.setTenseType(relNode.getAttributes().getNamedItem(TagTypes.TENSE).getNodeValue());
		}
		
		if(relNode.getAttributes().getNamedItem(TagTypes.MODALITY)!=null){
			relation.setModType(relNode.getAttributes().getNamedItem(TagTypes.MODALITY).getNodeValue());
		}
		List<Node>  relEntities =  XmlHelper.getChildrenByTagName(relNode, TagTypes.ARG);
		for(int j=0; j< relEntities.size(); j++){
			Node relArg = relEntities.get(j);
			String argRole = relArg.getAttributes().getNamedItem(TagTypes.ROLE).getNodeValue();
			if(argRole.contains("Arg") && relation.getEntityArg1()==null){
				relation.setEntityArg1(relArg.getAttributes().getNamedItem(TagTypes.REFID).getNodeValue());
			}else if(argRole.contains("Arg")){
				relation.setEntityArg2(relEntities.get(j).getAttributes().getNamedItem(TagTypes.REFID).getNodeValue());
			}else{
				relation.setTimeArg(relEntities.get(j).getAttributes().getNamedItem(TagTypes.REFID).getNodeValue());
			}
		}
		return relation;
	}
	
	public static RelationMention setRelMention(Node relMenNode){
		RelationMention relMention = new RelationMention();
		Node charSeqNode = XmlHelper.getCharSeqNode(relMenNode);
		String id = relMenNode.getAttributes().getNamedItem(TagTypes.ID).getNodeValue();
		int start = Integer.parseInt(charSeqNode.getAttributes().getNamedItem(TagTypes.START).getNodeValue());
		int end = Integer.parseInt(charSeqNode.getAttributes().getNamedItem(TagTypes.END).getNodeValue());
		String charSeq = charSeqNode.getTextContent();
		relMention.setId(id);	
		relMention.setCharSeq(charSeq);
		relMention.setStart(start);
		relMention.setEnd(end);
		//List<Node>  mentionArgNodes  =  XmlHelper.getChildrenByTagName(relMensList.get(j), TagTypes.RELMENARG);
		List<Node> mentionArgNodes = XmlHelper.getChildrenByTagName(relMenNode, TagTypes.RELMENARG);
	
//		for(int i=0;i<mentionArgNodes.size();i++){
//			Node menArgNode = mentionArgNodes.get(i);
//			String argRole = menArgNode.getAttributes().getNamedItem(TagTypes.REFID).getNodeValue();
//			if(argRole.contains("Arg") && relMention.getId().equals("")){
//				String refId1 = menArgNode.getAttributes().getNamedItem(TagTypes.REFID).getNodeValue();
//				Node arg1SeqNode = XmlHelper.getCharSeqNode(menArgNode);
//				String arg1Seq = arg1SeqNode.getTextContent();
//				int arg1Start = Integer.parseInt(arg1SeqNode.getAttributes().getNamedItem(TagTypes.START).getNodeValue());
//				int arg1End = Integer.parseInt(arg1SeqNode.getAttributes().getNamedItem(TagTypes.END).getNodeValue());
//				relMention.setRefId1(refId1);
//				relMention.setRole1(argRole);
//				relMention.setArg1Seq(arg1Seq);
//				relMention.setArg1Start(arg1Start);
//				relMention.setArg1End(arg1End);
//			}else if(argRole.contains("Arg")){
//				String refId2 = menArgNode.getAttributes().getNamedItem(TagTypes.REFID).getNodeValue();
//				Node arg2SeqNode = XmlHelper.getCharSeqNode(menArgNode);
//				String arg2Seq = arg2SeqNode.getTextContent();
//				int arg2Start = Integer.parseInt(arg2SeqNode.getAttributes().getNamedItem(TagTypes.START).getNodeValue());
//				int arg2End = Integer.parseInt(arg2SeqNode.getAttributes().getNamedItem(TagTypes.END).getNodeValue());
//				relMention.setRefId2(refId2);
//				relMention.setRole2(argRole);
//				relMention.setArg2Seq(arg2Seq);
//				relMention.setArg2Start(arg2Start);
//				relMention.setArg2End(arg2End);
//			}
//		}
		
		
		Node arg1Node = mentionArgNodes.get(0);		
		String refId1 = arg1Node.getAttributes().getNamedItem(TagTypes.REFID).getNodeValue();
		String arg1Role = arg1Node.getAttributes().getNamedItem(TagTypes.ROLE).getNodeValue();
		Node arg1SeqNode = XmlHelper.getCharSeqNode(arg1Node);
		String arg1Seq = arg1SeqNode.getTextContent();
		int arg1Start = Integer.parseInt(arg1SeqNode.getAttributes().getNamedItem(TagTypes.START).getNodeValue());
		int arg1End = Integer.parseInt(arg1SeqNode.getAttributes().getNamedItem(TagTypes.END).getNodeValue());
		if(arg1Role.contains("Arg")){
			relMention.setRefId1(refId1);
			relMention.setRole1(arg1Role);
			relMention.setArg1Seq(arg1Seq);
			relMention.setArg1Start(arg1Start);
			relMention.setArg1End(arg1End);
		}
		Node arg2Node = mentionArgNodes.get(1);
		String refId2 = arg2Node.getAttributes().getNamedItem(TagTypes.REFID).getNodeValue();
		String arg2Role = arg2Node.getAttributes().getNamedItem(TagTypes.ROLE).getNodeValue();
		Node arg2SeqNode = XmlHelper.getCharSeqNode(arg2Node);
		String arg2Seq = arg2SeqNode.getTextContent();
		int arg2Start = Integer.parseInt(arg2SeqNode.getAttributes().getNamedItem(TagTypes.START).getNodeValue());
		int arg2End = Integer.parseInt(arg2SeqNode.getAttributes().getNamedItem(TagTypes.END).getNodeValue());
		//note, arg2 doesn't necessarily means it is arg2 since the relation type doesn't necessarily follow the order
		//but we need to keep the order so that we know that sometimes arg2 is before arg1. Also, we need to check if there
		//are other relation_argument such as time-within. If so, we will ignore them for the moment. 
		if(arg2Role.contains("Arg")){
			relMention.setRefId2(refId2);
			relMention.setRole2(arg2Role);
			relMention.setArg2Seq(arg2Seq);
			relMention.setArg2Start(arg2Start);
			relMention.setArg2End(arg2End);
			System.out.println("relMention.getRefId2 in XmlHelper when arg2role is not null: "+relMention.getRefId2());
		}
		
		if(relMention.getRefId2()==null){
			if(mentionArgNodes.get(2)!=null){
				Node arg3Node = mentionArgNodes.get(2);
				String arg3Role = arg3Node.getAttributes().getNamedItem(TagTypes.ROLE).getNodeValue();
				if(arg3Role.contains("Arg")){
					String refId3 = arg3Node.getAttributes().getNamedItem(TagTypes.REFID).getNodeValue();
					Node arg3SeqNode = XmlHelper.getCharSeqNode(arg3Node);
					String arg3Seq = arg3SeqNode.getTextContent();
					int arg3Start = Integer.parseInt(arg3SeqNode.getAttributes().getNamedItem(TagTypes.START).getNodeValue());
					int arg3End = Integer.parseInt(arg3SeqNode.getAttributes().getNamedItem(TagTypes.END).getNodeValue());
					//System.out.println(refId3);
					relMention.setRefId2(refId3);
					System.out.println("relMention.getRefId2 in XmlHelper: "+relMention.getRefId2());
					relMention.setRole2(arg3Role);
					relMention.setArg2Seq(arg3Seq);
					relMention.setArg2Start(arg3Start);
					relMention.setArg2End(arg3End);
				}
			}
		}
		return relMention;
	}
	
	
	public static Node getCharSeqNode(Node aNode){
		Node extNode = XmlHelper.getFirstChildByTagName(aNode, TagTypes.EXTENT);
		Node charSeqNode = XmlHelper.getFirstChildByTagName(extNode, TagTypes.CHARSEQ);
		return charSeqNode;
	}
	
	public static List<String[]> getNeRelMenArgs(Node relNode){
		List<String[]> listOfRelMenArgs = new ArrayList<String[]>();
		List<Node> relMensList = XmlHelper.getChildrenByTagName(relNode, TagTypes.RELMEN);
		for(int i=0;i<relMensList.size();i++ ){
			Node relMen = relMensList.get(i);
			List<Node> relMenArgNodeList = XmlHelper.getChildrenByTagName(relMen, TagTypes.RELMENARG);
			String relMenArg1 = relMenArgNodeList.get(0).getAttributes().getNamedItem(TagTypes.REFID).getNodeValue();
			String relMenArg2 = relMenArgNodeList.get(1).getAttributes().getNamedItem(TagTypes.REFID).getNodeValue();
			String[] relMenArg = new String[2];
			relMenArg[0] = relMenArg1;
			relMenArg[1] = relMenArg2;
			listOfRelMenArgs.add(relMenArg);
			
		}
		return listOfRelMenArgs;
	}
}
