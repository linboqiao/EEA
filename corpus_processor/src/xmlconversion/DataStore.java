package xmlconversion;

import java.util.ArrayList;
import java.util.HashMap;

public class DataStore {

	HashMap<Integer,String> spanEntHm;
	HashMap<Integer,String> spanSentHm;
	ArrayList<Integer> entStartList;
	ArrayList<Integer> sentStartList;
	
	public DataStore (HashMap<Integer,String>[] spanHm, ArrayList<Integer>[] startList){
		spanEntHm = spanHm[0];
		spanSentHm = spanHm[1];
		entStartList = startList[0];
		sentStartList = startList[1];
	}
	
	public HashMap<Integer,String> getEntHm() {
		return spanEntHm;
	}

	public void setEntHm(HashMap<Integer,String> spanEntHm) {
		this.spanEntHm = spanEntHm;
	}

	public HashMap<Integer,String> getSentHm() {
		return spanSentHm;
	}

	public void setSentHm(HashMap<Integer,String> spanSentHm) {
		this.spanSentHm = spanSentHm;
	}
	
	public ArrayList<Integer> getEntStartList() {
		return entStartList;
	}

	public void setEntStartList(ArrayList<Integer> entStartList) {
		this.entStartList = entStartList;
	}

	public ArrayList<Integer> getSentStartList() {
		return sentStartList;
	}

	public void setSentStartList(ArrayList<Integer> sentStartList) {
		this.sentStartList = sentStartList;
	}

}
