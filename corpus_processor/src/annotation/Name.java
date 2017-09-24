package annotation;

/**
 * @author somasw000
 */
public class Name {
    private int start;
    private int end;
    private String coveredText;
    private String printString;


    public String getPrintString() {
        if (printString == null) {
            printString = "name=" + start + "_" + end + "\t" + coveredText;
        }
        return printString;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getCoveredText() {
        return coveredText;
    }

    public void setCoveredText(String coveredText) {
        this.coveredText = coveredText;
    }


}
