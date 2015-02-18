package limo.exrel.modules.mallet.MalletMaxEnt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.FeatureSequenceWithBigrams;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TokenSequenceSimpleRemoveRareTokens extends Pipe implements Serializable {
	
	HashMap<String, Double> frequency;
	boolean binary = true;
	int cutoff = 4;
	
	public TokenSequenceSimpleRemoveRareTokens(HashMap<String, Double> frequency, int cutoff, boolean binary) {
		this.frequency = frequency;
		this.cutoff = cutoff;
		this.binary = binary;
		if (this.frequency == null)
			this.frequency = new HashMap<String, Double>();
	}
	
	public TokenSequenceSimpleRemoveRareTokens() {
		this.frequency = new HashMap<String, Double>();
		this.cutoff = 4;
		this.binary = true;
	}
	
	public Instance pipe (Instance carrier)
	{
		TokenSequence ts = (TokenSequence) carrier.getData();
		
		TokenSequence ret = new TokenSequence ();
		for (int i = 0; i < ts.size(); i++) {
			String t = ts.get(i).getText();
			String test = t;
			if (!binary && t.contains("=="))
				test = t.substring(0, t.lastIndexOf("=="));
			if (frequency.containsKey(test) && (frequency.get(test).doubleValue() >= cutoff))
				ret.add(t);
		}
//		if (ret.size() == 0)
//			carrier.setTarget(null); //remove this instance from the list
		carrier.setData(ret);
		return carrier;
	}
	
	// Serialization 
	
		private static final long serialVersionUID = 1;
		private static final int CURRENT_SERIAL_VERSION = 1;
		
		private void writeObject (ObjectOutputStream out) throws IOException {
			out.writeInt (CURRENT_SERIAL_VERSION);
			out.writeBoolean(binary);
			out.writeInt(cutoff);
			out.writeObject(frequency);
		}
		
		private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
			int version = in.readInt ();
			if (version > 0) {
				binary = in.readBoolean();
				cutoff = in.readInt();
				frequency = (HashMap<String, Double>) in.readObject();
			}
		}

}
