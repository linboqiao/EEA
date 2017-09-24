package utils;

import annotation.SimpleRelMention;

public class PrintMethods {

	public static String setText(StringBuffer dBuf,SimpleRelMention simRelMen){
		String text="";
		int[] men1Span = simRelMen.getMen1Span();
		int[] men2Span = simRelMen.getMen2Span();
		if(men1Span[0]<=men2Span[0]&&men1Span[1]<=men2Span[1]){
			text = dBuf.substring(men1Span[0],men2Span[1]+1);
			//System.out.println("case 1: men1Span[0] "+men1Span[0]+" men2Span[1] "+men2Span[1]);
		}else if(men1Span[0]>=men2Span[0]&&men1Span[1]<=men2Span[1]){
			text = dBuf.substring(men2Span[0],men2Span[1]+1);
			//System.out.println("case 2: men2Span[0] "+men2Span[0]+" men2Span[1] "+men2Span[1]);
		}else if(men1Span[0]>=men2Span[0]&&men1Span[1]>=men2Span[1]){
			text = dBuf.substring(men2Span[0],men1Span[1]+1);
			//System.out.println("case 3: men2Span[0] "+men2Span[0]+" men1Span[1] "+men1Span[1]);
		}else if(men1Span[0]>=men2Span[0]&&men1Span[1]>=men2Span[1]){
			text = dBuf.substring(men2Span[0],men1Span[1]+1);
			//System.out.println("case 4: men2Span[0] "+men2Span[0]+" men1Span[1] "+men1Span[1]);
		}else if(men1Span[0]>men2Span[0]&&men1Span[1]>men2Span[1]){
			text = dBuf.substring(men2Span[0],men1Span[1]+1);
			//System.out.println("case 5: men2Span[0] "+men2Span[0]+" men1Span[1] "+men1Span[1]);
		}else if(men1Span[0]<=men2Span[0]&&men1Span[1]>=men2Span[1]){
			text = dBuf.substring(men1Span[0],men1Span[1]+1);
			//System.out.println("case 6: men1Span[0] "+men1Span[0]+" men1Span[1] "+men1Span[1]+" men2Span[0] "+men2Span[0]+" men2Span[1] "+men2Span[1]);
		}
		return text;
	}
}
