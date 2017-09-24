package annotation;

import java.util.ArrayList;

/**
 * @author somasw000
 */
public class Relation {
    private String id;
    private String type;
    private String subtype;
    private String tenseType;
    private String modType;
    private String entityArg1;
    private String entityArg2;
    private String timeArg;
    private ArrayList<RelationMention> relMentions;
    private String printString;
    private String relPrintString;


    public void addRelationMention(RelationMention relMention) {
        if (relMentions == null) relMentions = new ArrayList<RelationMention>();
        //System.out.println(relMention.getId());
        relMentions.add(relMention);
    }

    public String getPrintString() {
        if (printString == null) {
            printString = "RelationId= " + id + " type= " + type + " _ " + subtype + " tenseType= " + tenseType
                    + "\t entities= " + entityArg1 + " == " + entityArg2 + " timeArg: " + timeArg;
        }
        return printString;
    }

    public String getRelMentionString() {
        if (relPrintString == null) {
            //System.out.println(relMentions.size());
            relPrintString = relMentions.get(0).getPrintString();
            for (int i = 1; i < relMentions.size(); i++) {
                relPrintString += "\n" + relMentions.get(i).getPrintString();
            }
        }
        return relPrintString;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public String getTenseType() {
        return tenseType;
    }

    public void setTenseType(String tenseType) {
        this.tenseType = tenseType;
    }

    public String getModType() {
        return modType;
    }

    public void setModType(String modType) {
        this.modType = modType;
    }

    public String getEntityArg1() {
        return entityArg1;
    }

    public void setEntityArg1(String entityArg1) {
        this.entityArg1 = entityArg1;
    }

    public String getEntityArg2() {
        return entityArg2;
    }

    public void setEntityArg2(String entityArg2) {
        this.entityArg2 = entityArg2;
    }

    public String getTimeArg() {
        return timeArg;
    }

    public void setTimeArg(String timeArg) {
        this.timeArg = timeArg;
    }

    public ArrayList<RelationMention> getRelMentions() {
        return relMentions;
    }

    public void setRelMentions(ArrayList<RelationMention> relMentions) {
        this.relMentions = relMentions;
    }


}
