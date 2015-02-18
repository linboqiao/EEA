package limo.exrel.modules.mallet.MalletMaxEnt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;

public class TokenSequence2FeatureSequenceValues extends Pipe  implements Serializable  {
	
	double binaryFeatureValue = 1.0;
	
	public TokenSequence2FeatureSequenceValues (Alphabet dataDict, double binaryFeatureValue)
	{
		super (dataDict, null);
		if (binaryFeatureValue > 0)
			this.binaryFeatureValue = binaryFeatureValue;
	}

	public TokenSequence2FeatureSequenceValues (double binaryFeatureValue)
	{
		super(new Alphabet(), null);
		if (binaryFeatureValue > 0)
			this.binaryFeatureValue = binaryFeatureValue;
	}
	
	public TokenSequence2FeatureSequenceValues ()
	{
		super(new Alphabet(), null);
	}
	
	public Instance pipe (Instance carrier)
	{
		String feature = "", val = "";
		double value = 0.0;
		TokenSequence ts = (TokenSequence) carrier.getData();
		FeatureSequenceValues ret =
			new FeatureSequenceValues ((Alphabet)getDataAlphabet(), ts.size());
		for (int i = 0; i < ts.size(); i++) {
			feature = ts.get(i).getText();
			if (feature.contains("==")) {
				val = feature.substring(feature.lastIndexOf("==")+2);
				if (val != null && isNumeric(val)) {
					value = Double.parseDouble(val);
					feature = feature.substring(0, feature.lastIndexOf("=="));
				}
				else {
					value = binaryFeatureValue;
				}
			}
			else {
				value = binaryFeatureValue;
			}
			ret.add (feature, value);
		}
		carrier.setData(ret);
		return carrier;
	}
	
	private static boolean isNumeric(String str)  
	{  
	  try  
	  {  
	    double d = Double.parseDouble(str);  
	  }  
	  catch(NumberFormatException nfe)  
	  {  
	    return false;  
	  }  
	  return true;  
	}
	
	// Serialization 
	
			private static final long serialVersionUID = 1;
			private static final int CURRENT_SERIAL_VERSION = 1;
			
			private void writeObject (ObjectOutputStream out) throws IOException {
				out.writeInt (CURRENT_SERIAL_VERSION);
				out.writeDouble(binaryFeatureValue);
			}
			
			private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
				int version = in.readInt ();
				if (version > 0) {
					binaryFeatureValue = in.readDouble();
				}
			}

}
