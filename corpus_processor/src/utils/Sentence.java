package utils;


public class Sentence {

	String sent;
	int start;
	int end;	
	public void setSent(String sent){
		this.sent=sent;
	}
	
	public String getSent(){
		return this.sent;
	}
	
	public void setStart(int start){
		this.start=start;
	}
	public int getStart(){
		return this.start;
	}
	
	public void setEnd(int end){
		this.end=end;
	}
	public int getEnd(){
		return this.end;
	}
	
}
