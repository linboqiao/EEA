package annotation;

/**
 * @author somasw000
 */
public class Mention {
    private String id;
    private String type;
    private int extentSt;
    private int extentEd;
    private int headSt;
    private int headEd;
    private String extentCoveredText;
    private String headCoveredText;
    private String printString;


    public String getPrintString() {
        if (printString == null) {
            printString = "mentionID= " + id + "  type= " + type + "\textent=" + extentSt + "_" + extentEd + " =" +
                    extentCoveredText + "=\t head=" + headSt + "_" + headEd + " =" + headCoveredText + "=";
        }
        return printString;
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

    public int getExtentSt() {
        return extentSt;
    }

    public void setExtentSt(int extentSt) {
        this.extentSt = extentSt;
    }

    public int getExtentEd() {
        return extentEd;
    }

    public void setEntentEd(int ententEd) {
        this.extentEd = ententEd;
    }

    public int getHeadSt() {
        return headSt;
    }

    public void setHeadSt(int headSt) {
        this.headSt = headSt;
    }

    public int getHeadEd() {
        return headEd;
    }

    public void setHeadEd(int headEd) {
        this.headEd = headEd;
    }

    public String getExtentCoveredText() {
        return extentCoveredText;
    }

    public void setExtentCoveredText(String extentCoveredText) {
        this.extentCoveredText = extentCoveredText;
    }

    public String getHeadCoveredText() {
        return headCoveredText;
    }

    public void setHeadCoveredText(String headCoveredText) {
        this.headCoveredText = headCoveredText;
    }


}
