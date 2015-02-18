package limo.exrel.modules.mallet.MalletMaxEnt;

import java.io.Serializable;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;

public class FeatureSequenceValues2FeatureVector  extends Pipe {
	
	public FeatureSequenceValues2FeatureVector() {}
	
	public Instance pipe (Instance carrier)
	{
		FeatureSequenceValues fsv = (FeatureSequenceValues) carrier.getData();
		Object[] objs = fsv.toSortedFeatureIndexValueSequence();
		int[] fets = (int[]) objs[0];
		double[] vals = (double[]) objs[1];
		Alphabet alph = (Alphabet) fsv.getAlphabet();
		//carrier.setData(new FeatureVector (alph, fets, vals, fets.length, fets.length, false, false, true));
		carrier.setData(new FeatureVector (alph, fets, vals));
		return carrier;
	}

}
