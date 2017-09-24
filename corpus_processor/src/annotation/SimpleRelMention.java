package annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import readers.AceReader;

/**
 * @author somasw000
 *
 */
public class SimpleRelMention {
	private int[] men1Span;
	private int[] men2Span;
	// Lazily initialized, cached hashCode
	private volatile int hashCode = 0; // (See Item 48)
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SimpleRelMention))
			return false;
		SimpleRelMention pn = (SimpleRelMention)o;
		return pn.getMen1Span()[0] == men1Span[0] &&
		pn.getMen1Span()[1] == men1Span[1] &&
		pn.getMen2Span()[0] == men2Span[0] &&
		pn.getMen2Span()[1] == men2Span[1];
	}

	public int hashCode() {
		if (hashCode == 0) {
			int result = 17;
			result = 37*result + men1Span[0];
			result = 37*result + men1Span[1];
			result = 37*result + men2Span[0];
			result = 37*result + men2Span[1];
			hashCode = result;
		}
		return hashCode;
	}

	public int[] getMen1Span() {
		return men1Span;
	}
	public void setMen1Span(int[] men1Span) {
		this.men1Span = men1Span;
	}
	public int[] getMen2Span() {
		return men2Span;
	}
	public void setMen2Span(int[] men2Span) {
		this.men2Span = men2Span;
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {

		HashMap<SimpleRelMention,Integer> hmTest = new HashMap<SimpleRelMention,Integer>();
		int[] intArr1 = new int[2];
		intArr1[0] = 102;
		intArr1[1] = 205;
		int[] intArr2 = new int[2];
		intArr2[0] = 226;
		intArr2[1] = 404;
		int[] intArr3 = new int[2];
		intArr3[0] = 12;
		intArr3[1] = 20;
		int[] intArr4 = new int[2];
		intArr4[0] = 32;
		intArr4[1] = 40;
		int[] intArr5 = new int[2];
		intArr5[0] = 12;
		intArr5[1] = 20;
		int[] intArr6 = new int[2];
		intArr6[0] = 32;
		intArr6[1] = 50;
		int[] intArr7 = new int[2];
		intArr7[0] = 52;
		intArr7[1] = 70;
		int[] intArr8 = new int[2];
		intArr8[0] = 12;
		intArr8[1] = 20;
		int[] intArr9 = new int[2];
		intArr9[0] = 12;
		intArr9[1] = 20;
		int[] intArr10 = new int[2];
		intArr10[0] = 82;
		intArr10[1] = 90;

		//		SimpleRelMention srm1 = new SimpleRelMention(intArr1,intArr2);
		//		SimpleRelMention srm2 = new SimpleRelMention(intArr3,intArr6);
		//		SimpleRelMention srm3 = new SimpleRelMention(intArr5,intArr8);
		//		SimpleRelMention srm4 = new SimpleRelMention(intArr8,intArr9);
		//		SimpleRelMention srm5 = new SimpleRelMention(intArr9,intArr1);
		//		SimpleRelMention srm6 = new SimpleRelMention(intArr10,intArr8);
		//		SimpleRelMention srm7 = new SimpleRelMention(intArr1,intArr3);
		//		SimpleRelMention srm8 = new SimpleRelMention(intArr1,intArr5);

		SimpleRelMention srm1 = new SimpleRelMention();
		srm1.setMen1Span(intArr1);
		srm1.setMen2Span(intArr2);
		SimpleRelMention srm2 = new SimpleRelMention();
		srm2.setMen1Span(intArr3);
		srm2.setMen2Span(intArr6);
		SimpleRelMention srm3 = new SimpleRelMention();
		srm3.setMen1Span(intArr5);
		srm3.setMen2Span(intArr8);
		SimpleRelMention srm4 = new SimpleRelMention();
		srm4.setMen1Span(intArr8);
		srm4.setMen2Span(intArr9);
		SimpleRelMention srm5 = new SimpleRelMention();
		srm5.setMen1Span(intArr9);
		srm5.setMen2Span(intArr1);
		SimpleRelMention srm6 = new SimpleRelMention();
		srm6.setMen1Span(intArr10);
		srm6.setMen2Span(intArr8);
		SimpleRelMention srm7 = new SimpleRelMention();
		srm7.setMen1Span(intArr1);
		srm7.setMen2Span(intArr3);
		SimpleRelMention srm8 = new SimpleRelMention();
		srm8.setMen1Span(intArr1);
		srm8.setMen2Span(intArr5);
		SimpleRelMention srm9 = new SimpleRelMention();
		srm9.setMen1Span(intArr1);
		srm9.setMen2Span(intArr5);

		List<SimpleRelMention> srmList = new ArrayList<SimpleRelMention>();
		srmList.add(srm1);
		srmList.add(srm2);
		srmList.add(srm3);
		srmList.add(srm4);
		srmList.add(srm5);
		srmList.add(srm6);
		srmList.add(srm7);
		srmList.add(srm8);
		srmList.add(srm1);
		srmList.add(srm2);
		srmList.add(srm3);
		srmList.add(srm4);
		srmList.add(srm5);
		srmList.add(srm6);
		srmList.add(srm7);
		srmList.add(srm8);
		srmList.add(srm1);
		srmList.add(srm2);
		srmList.add(srm3);
		srmList.add(srm4);
		srmList.add(srm5);
		srmList.add(srm6);
		srmList.add(srm7);
		srmList.add(srm8);


		System.out.println(srmList.size());
		for(int i=0;i<srmList.size();i++){
			SimpleRelMention srm = srmList.get(i);
			if(!hmTest.containsKey(srm)){
				hmTest.put(srm, 1);
				System.out.println("right here funnyyyyyyyyyyyyyyy ======================== "+1+" "+srm.getMen1Span()[0]+" "+srm.getMen1Span()[1]+" "+srm.getMen2Span()[0]+" "+srm.getMen2Span()[1]);
			}else{
				int count = hmTest.get(srm)+1;
				hmTest.put(srm, count);
				System.out.println("right here? ======================== "+count+" "+srm.getMen1Span()[0]+" "+srm.getMen1Span()[1]+" "+srm.getMen2Span()[0]+" "+srm.getMen2Span()[1]);
			}
		}

		System.out.println(srm9.equals(srm8)+" "+hmTest.containsKey(srm8)+" "+hmTest.containsKey(srm9));
		//System.out.println(hmTest.containsKey(srm8)+" "+hmTest.containsKey(srm9));
		Iterator<SimpleRelMention> iterator = hmTest.keySet().iterator();
		while(iterator.hasNext()){
			SimpleRelMention srm = iterator.next();
			int count = hmTest.get(srm);
			System.out.println(srm+" "+count);
		}
	}
}
