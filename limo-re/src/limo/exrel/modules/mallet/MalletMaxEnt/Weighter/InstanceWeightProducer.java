package limo.exrel.modules.mallet.MalletMaxEnt.Weighter;

import cc.mallet.types.Instance;

public interface InstanceWeightProducer {
	
	//public double getWeight(Instance carrier);
	public double getWeight(String predicates);

}
