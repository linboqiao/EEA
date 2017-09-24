package annotation;

import java.util.ArrayList;

/**
 * @author somasw000
 *
 */
public class NamedEntity {
	private String  id ;
	private String type;
	private String subtype;
	private ArrayList<Mention> mentions; 
	private ArrayList<Name> names;
	private String printString;
	private String mentionString;
	private String nameString;
	private String classType;
	
	public  void addMention(Mention aMen){
		if(mentions ==  null )  mentions =  new ArrayList<Mention>();
		mentions.add(aMen);
	}
	
	public  void addNames(Name aName){
		if(names ==  null )  names =  new ArrayList<Name>();
		names.add(aName);
	}
	
	public String getPrintString() {
		if(printString == null){
			printString = "NeID="+ id +" type="+ type+" subtype="+subtype+" class="+classType;
		}
		return printString;
	}

	public String getMentionString() {
		if(mentionString ==  null){
			mentionString=mentions.get(0).getPrintString();
			for(int i=1; i<  mentions.size(); i++){
				mentionString+="\n"+mentions.get(i).getPrintString();
			}
		}
		return mentionString;
	}

	public String getNameString() {
		if(nameString ==  null){
			nameString=names.get(0).getPrintString();
			for(int i=1; i<  names.size(); i++){
				nameString+="\n"+names.get(i).getPrintString();
			}
		}
		return nameString;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getClassType() {
		return classType;
	}
	public void setClassType(String classType) {
		this.classType = classType;
	}
	
	public String getSubType() {
		return subtype;
	}
	public void setSubType(String subtype) {
		this.subtype = subtype;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public ArrayList<Mention> getMentions() {
		return mentions;
	}
	public void setMentions(ArrayList<Mention> mentions) {
		this.mentions = mentions;
	}
	public ArrayList<Name> getNames() {
		return names;
	}
	public void setNames(ArrayList<Name> names) {
		this.names = names;
	}
	
	
}
