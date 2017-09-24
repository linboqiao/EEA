package annotation;

/**
 * @author somasw000
 */
public class RelationMention {
    private String id;
    private String refId1;
    private String refId2;
    private String charSeq;
    private int start;
    private int end;
    private String role1;
    private String role2;
    private String arg1Seq;
    private int arg1Start;
    private int arg1End;
    private String arg2Seq;
    private int arg2Start;
    private int arg2End;
    private String printString;
    private String lexcod;

    public String getPrintString() {
        if (printString == null) {
            //System.out.println(id+" ");
            printString = "relationMentionId= " + id + " charSeq: " + charSeq + " start: " + start + " end: " + end + "\targ1= " + refId1 + "\targ1Seq" + arg1Seq + " arg1Start: " + arg1Start + " arg1End: " + arg1End +
                    " role1: " + role1 + "\narg2= " + refId2 + "\targ2Seq" + arg2Seq + " arg2Start: " + arg2Start + " arg2End: " + arg2End + " role1: " + role2 + " lexcod: " + lexcod;
        }
        return printString;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCharSeq() {
        return charSeq;
    }

    public void setCharSeq(String charSeq) {
        this.charSeq = charSeq;
    }

    public int getStart(int start) {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd(int end) {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getRefId1() {
        return refId1;
    }

    public void setRefId1(String refId1) {
        this.refId1 = refId1;
    }

    public String getArg1Seq() {
        return arg1Seq;
    }

    public void setArg1Seq(String arg1Seq) {
        this.arg1Seq = arg1Seq;
    }

    public int getArg1Start() {
        return arg1Start;
    }

    public void setArg1Start(int arg1Start) {
        this.arg1Start = arg1Start;
    }

    public int getArg1End() {
        return arg1End;
    }

    public void setArg1End(int arg1End) {
        this.arg1End = arg1End;
    }

    public String getRole1() {
        return role1;
    }

    public void setRole1(String role1) {
        this.role1 = role1;
    }

    public String getRefId2() {
        return refId2;
    }

    public void setRefId2(String refId2) {
        this.refId2 = refId2;
    }

    public String getArg2Seq() {
        return arg2Seq;
    }

    public void setArg2Seq(String arg2Seq) {
        this.arg2Seq = arg2Seq;
    }

    public int getArg2Start() {
        return arg2Start;
    }

    public void setArg2Start(int arg2Start) {
        this.arg2Start = arg2Start;
    }

    public int getArg2End() {
        return arg2End;
    }

    public void setArg2End(int arg2End) {
        this.arg2End = arg2End;
    }

    public String getRole2() {
        return role2;
    }

    public void setRole2(String role2) {
        this.role2 = role2;
    }

    public String getLexCod() {
        return lexcod;
    }

    public void setLexCod(String lexcod) {
        this.lexcod = lexcod;
    }

}
